/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dopm_mm2.Runnables;

import dopm_mm2.Devices.PIStage;
import dopm_mm2.GUI.dOPM_hostframe;
import dopm_mm2.Devices.DeviceManager;
import java.io.BufferedWriter;
import java.io.FileWriter;

import mmcorej.CMMCore;
import org.micromanager.ScriptController;
import org.micromanager.Studio;
import org.micromanager.data.Datastore;
import org.micromanager.display.DisplayManager;
import mmcorej.TaggedImage;
import org.micromanager.display.DisplayWindow;
import org.micromanager.data.Coordinates;
import org.micromanager.data.Coords;
import org.micromanager.data.Image;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;

import java.io.IOException;
import java.io.File;
import javax.swing.JOptionPane;

/* stuff for future
import dopm_mm2.util.FileMM;
import dopm_mm2.util.MMStudioInstance;
import dopm_mm2.util.RunnableExceptionHandler;
*/

/** Runnable to acquire a single PI-scan-triggered acquisition in RAM
 *
 * @author lnr19
 */
public class PIScanRunnable implements Runnable {

    private final dOPM_hostframe frame_;
    private final CMMCore core_;
    private final Studio mm_;
    private DisplayManager displayManager;
    private int PIDeviceID;
    private static final Logger runnableLogger = 
            Logger.getLogger(PIScanRunnable.class.getName());
    
    private ScriptController sc;

    private DeviceManager deviceSettings;
    private PIStage control;
    private DisplayWindow display;
    
    private String camName;
    private String mirrorStage;
    private String XYStage;
    private String ZStage;
    
    private double scanLength;
    private double scanSpeed;
    private double trigDist;
    private double exposure;
    
    // Set serial command
    private String port;
    private String commandTerminator = "\n"; 
    
    private String settingsOutDir;
    private String dataOutDir;
    private String baseDir;
    private String dataSavePath;
    private FileHandler fh;
    
    boolean saveImgToDisk;
    
    public PIScanRunnable(dOPM_hostframe frame_ref) {
        frame_ = frame_ref;
        mm_ = dOPM_hostframe.mm_;
        core_ = mm_.getCMMCore();
        deviceSettings = frame_.getDeviceSettings();
        PIDeviceID = 1;
        
        saveImgToDisk = frame_.isSaveImgToDisk();
        
        settingsOutDir = frame_.getSettingsFolderDir().getAbsolutePath();
        dataOutDir = frame_.getDataFolderDir().getAbsolutePath();
        baseDir = frame_.getBaseFolderDir().getAbsolutePath();
        // FileWriter logFile = new FileWriter(settingsOutDir + "log.txt");
        // BufferedWriter runLog = new BufferedWriter(logFile);
                
        sc = mm_.getScriptController();
        
        camName = deviceSettings.getCameraDeviceName();
        mirrorStage = deviceSettings.getMirrorStageDeviceName();
        XYStage = deviceSettings.getXYStageDeviceName();
        ZStage = deviceSettings.getZStageDeviceName();
        
        // Set serial command
        port = deviceSettings.getMirrorStageComPort();
        commandTerminator = "\n"; 
        
        scanLength = deviceSettings.getScanLength();  // target scan end in um
        scanSpeed = deviceSettings.getMirrorStageSpeed();  // scan speed in mm/s or um/ms
        trigDist = deviceSettings.getTriggerDistance();  // trigger distance in um
        exposure = deviceSettings.getExposureTime();
    }
    
    public int makeDirsAndLog(){
        try { 
            new File(baseDir).mkdirs();
            new File(settingsOutDir).mkdirs();
            new File(dataOutDir).mkdirs();
            
            // just print log for this runnable for debugging directly
            fh = new FileHandler(new File(settingsOutDir, "log.txt").getAbsolutePath());
 
            runnableLogger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();  
            fh.setFormatter(formatter);  
        } catch (IOException ioe){
            runnableLogger.severe("Failed to create log with " + ioe.getMessage());
            return 1;
        } catch (SecurityException se){
            runnableLogger.severe("Failed to create dirs " + se.getMessage());
            return 1;
        }
        return 0;
    }
    
    @Override
    @SuppressWarnings("UnusedAssignment")
    public void run(){
        sc.message("Running Mirror-scan volume acquisition!");
        
        makeDirsAndLog();  // make dirs for saving volume and logs

        // this is here in case the runnable is called in a loop, for example
        if (frame_.getInterruptFlag()) {
            runnableLogger.info("Acquisition terminated manually!");
            return;
        }
        
        try {
            PIStage.setupPITriggering(port, PIDeviceID);
        } catch (Exception e){
            runnableLogger.severe("Failed to setup triggering with " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
        
        // Setup in um
        double currentXPos;
        double currentYPos;
        double currentZPos;
        double mirror_pos;
        try {
            if (!XYStage.equals("")){
                currentXPos = core_.getXPosition(XYStage);
                currentYPos = core_.getYPosition(XYStage);
            }
            if (!ZStage.equals("")) currentZPos = core_.getPosition(ZStage);
            if (!mirrorStage.equals("")){
                mirror_pos = core_.getPosition(mirrorStage);
            } else {
                throw new Exception("No mirror position found, check device is connected");
            }
        } catch (Exception e){
            throw new RuntimeException(e.getMessage());
        }

        double startMirrorScan = mirror_pos;  // scan start in um
        double endMirrorTarget = mirror_pos + scanLength;  // target scan end in um

        // Make sure camera is in internal triggering first before moving stages etc.
        try {
            core_.setProperty(camName, "TRIGGER SOURCE","INTERNAL");
        } catch (Exception e){
            runnableLogger.severe(
                    "Failed to set camera to internal triggering with " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
        
        // based on settings, e.g. trigger mode and exposure, get readout time
        double readoutTimeMs = deviceSettings.getCameraReadoutTime();

        double maxFPS = 1000/readoutTimeMs;

        // undershoot and overshoot for ramp up and down
        double undershoot_mm = 5;  // 5 mm
        double overshoot_mm = 5; 

        // actual end point of scan is a multiple of trigger distances:
        double endMirrorScan = trigDist*Math.floor((endMirrorTarget-startMirrorScan)/trigDist);
        runnableLogger.info(
                "Target scan end is " + endMirrorTarget + ", actual scan end is " + endMirrorScan);

        double effectiveFPS = (1000/(trigDist/scanSpeed));

        // should never happen v, this is controlled in DeviceManager in setters
        if (readoutTimeMs > trigDist/scanSpeed){
            runnableLogger.warning(
                    "Trigger intervals too fast for camera readout time, frames will be dropped");
        }

        int nFramesTotal = (int)Math.floor((endMirrorScan-startMirrorScan)/trigDist);  // number of frames

        sc.message("Now running with hardware triggering");
        runnableLogger.info("Now running with hardware triggering");
        
        double stopLastAcqStart = System.currentTimeMillis();
        try {
            core_.stopSequenceAcquisition();
            runnableLogger.info("Stopped previous acquisition");
            double stopLastAcqTime = System.currentTimeMillis() - stopLastAcqStart;
            runnableLogger.info(
                    "Stopping previous sequence acquisition took " + stopLastAcqTime + " ms");
        } catch (Exception e){
                
        }

        double acqStart = System.currentTimeMillis();
        
        try{
            core_.setProperty(camName, "TriggerPolarity","POSITIVE");
            core_.setProperty(camName, "TRIGGER SOURCE","EXTERNAL");
        
            // prepares to grab frames from cam buffer
            core_.prepareSequenceAcquisition(camName);
            core_.startSequenceAcquisition(nFramesTotal, 0, true);
            
        } catch (Exception e){
            runnableLogger.severe(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
        
        // set start and end points for triggering (we ramp up without triggers outside this range)
        double startTrigger_mm = (double)(startMirrorScan)/1000;
        double endTrigger_mm = (double)(endMirrorScan)/1000;
        double trigDist_mm = (double)trigDist/1000;
        
        double startScan_mm = startTrigger_mm-undershoot_mm;
        double endScan_mm = endMirrorScan+overshoot_mm;
        
        try {
            PIStage.stopPIStage(port);  // need this command so PI stage plays ball
            PIStage.setPITriggerDistance(port, PIDeviceID, trigDist_mm);
            PIStage.setPITriggerRange(port, PIDeviceID, new double[]{startTrigger_mm, endTrigger_mm});
        } catch (Exception e){
            runnableLogger.severe("Failed setting PI trigger settings with " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
        
        // set to beginning
        try {
            core_.waitForDevice(mirrorStage);
            core_.setProperty(mirrorStage, "Velocity", 100);
            double currentMirrorScanspeed = 
                    Double.parseDouble(core_.getProperty(mirrorStage, "Velocity"));

            core_.setPosition(mirrorStage, startScan_mm);

            core_.setProperty(camName, "Exposure", exposure);
        } catch (Exception e) {
            runnableLogger.severe(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }

        // wait until reached start position
        // replaced with command that polls moving state #5
        String moveState;
        double waitStart = System.currentTimeMillis();
        double waitTimeout = 10e3;  // ms
        
        try {
            while(!(PIStage.checkPIMotion(port).equals("0")) && 
                    (System.currentTimeMillis()-waitStart) < waitTimeout){
                core_.setProperty(mirrorStage, "Velocity", 100);  // ensure is full speed
                Thread.sleep(100);
            }
            if (!control.checkPIMotion(port).equals("0")){
                throw new Exception("Timed out moving stage to start");
            }
        } catch (InterruptedException ie){
            runnableLogger.severe("Stage moving to start somehow interrupted " + ie.getMessage());
            throw new RuntimeException(ie.getMessage());
        } catch (Exception e){
            runnableLogger.severe(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }

        try {
            core_.setProperty(mirrorStage, "Velocity", scanSpeed);
        } catch (Exception e) {
            runnableLogger.severe("Failed to set scan speed with " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
        
        // do I need this? below
        // core_.setSerialPortCommand(port, "", commandTerminator);
        
        String startScanMsg = "Starting scan: exp_time=" + exposure + "ms, min_z=" + 
                startMirrorScan + " um, max_z=" + endMirrorScan + " um, scan speed=" + scanSpeed + 
                "mm/s ("+ nFramesTotal + " frames)"; 

        runnableLogger.info(startScanMsg);
        sc.message(startScanMsg);

        //core_.setPosition(PIDevice, (max_z+1));  // start movement

        try {
            core_.waitForDevice(mirrorStage);
            core_.waitForSystem();
        } catch (Exception e){
            runnableLogger.severe("Failed to wait for system with " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
        
        Datastore store;
        if (saveImgToDisk){
            dataSavePath = (new File(dataOutDir, "MMStack.tiff")).getAbsolutePath();
            try {
                store = mm_.data().createMultipageTIFFDatastore(dataSavePath, false, false);
            } catch (IOException ie){
                runnableLogger.severe("Failed to create datastore in " + dataSavePath +
                        " with " + ie.getMessage());
                return;
            }
        } else {
            store = mm_.data().createRAMDatastore();
            display = mm_.displays().createDisplay(store);
        }
        
        try{
            store = acquireTriggeredDataset(store, endScan_mm, nFramesTotal);
        } catch (Exception e){
            runnableLogger.severe("Triggered acquisition failed with " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
                
        try{
            cleanUpAcq(store);
        } catch (Exception e){
            runnableLogger.severe("Acquisition cleanup failed with " + e.getMessage());
        }
        
        double acqEnd = System.currentTimeMillis();
        double elapsed = acqEnd-acqStart;
        runnableLogger.info("time elapsed (cam and stage) " + elapsed + " ms");
    }
    
    public void cleanUpAcq(Datastore store) throws Exception {
        // cleaning up
        //runnableLogger.info("average frame time: " + frame_time_total/nframes);
        try {
            core_.stopSequenceAcquisition();
            core_.setProperty(camName, "TRIGGER SOURCE","INTERNAL");  
            PIStage.setPITriggerEnable(port, 0);  // disable triggering
            PIStage.setPIDigitalOut(port, 0);  // set trigger to low (just in case)
            PIStage.haltPIStage(port);  // halt stage if still moving
        } catch (Exception e){
            throw e;
        } finally {
            store.freeze();
            store.close();
        }
    }
    
    public Datastore acquireTriggeredDataset(Datastore store, double scanEnd, int nFramesTotal)
            throws Exception {
        // Coords.Builder cb = Coordinates.builder();
        boolean timeout = false;
        double acqTimeStart = System.currentTimeMillis();
    
        Coords.Builder cb = mm_.data().coordsBuilder().z(0).channel(0).stagePosition(0);
        boolean grabbed = false;
        int nFrames = 0;

        double frameTimeTotal = 0;
        int frameTimeout = 2000; // if no frame received for 2s, time out


        while (nFrames < nFramesTotal && !timeout){

                // print(pos);
                // start movement, end at trig dist/2 so trigger goes down again (might not work with start and stop trigger set)
                //core_.setPosition(PIDevice, max_z+(trig_dist/2));  
                core_.setPosition(mirrorStage, scanEnd);  
                double tic=System.currentTimeMillis();
                double toc=tic;

                grabbed = false;
                while(toc-tic < frameTimeout && !grabbed){
                        // wait for an image in the circular buffer
                        if (core_.getRemainingImageCount() > 0){
                            TaggedImage img = core_.popNextTaggedImage();	// TaggedImage
                            Image tmp = mm_.data().convertTaggedImage(img);  // Image 
                            // does this copy in memory? inefficient?
                            Image cbImg = tmp.copyAtCoords(cb.z(nFrames).build());
                            store.putImage(cbImg);
                            grabbed = true;
                            nFrames++;
                        }
                        toc = System.currentTimeMillis(); 
                }
                if (toc-tic >= frameTimeout){
                        runnableLogger.severe((nFramesTotal-nFrames)+" FRAMES DROPPED");
                        timeout=true;
                }
                frameTimeTotal += (toc-tic);

        }
        double acqTimeStop = System.currentTimeMillis() - acqTimeStart; 
        runnableLogger.info("Acq time: " + acqTimeStop + "ms");
        runnableLogger.info("end position: " + core_.getPosition(mirrorStage));
        
        runnableLogger.info("nframes: " + nFrames);
        runnableLogger.info("frames dropped? : " + (nFramesTotal-nFrames));
        runnableLogger.info("total acq time: " + frameTimeTotal);
        
        return store;
    }
}   
    
    /* These were written to improve repetitive try catch blocks,
       but ive left them out for readability
    */
/*
    public void setPropertyAndLog(String device, String property, String value){
        setPropertyAndLog(device, property, value, runnableLogger, Level.SEVERE);
    }
    
    public void setPropertyAndLog(String device, String property, String value,
                                  Level level){
        setPropertyAndLog(device, property, value, runnableLogger, level);
    }
    
    public void setPropertyAndLog(String device, String property, String value,
                                  Logger logger, Level level){
        try{
            core_.setProperty(device, property, value);
        } catch(Exception e){
            logger.log(level, e.getMessage());
        }
    }

    public String getPropertyAndLog(String device, String property){
        return getPropertyAndLog(device, property, runnableLogger, Level.SEVERE);
    }
    
    public String getPropertyAndLog(String device, String property, Level level){
        return getPropertyAndLog(device, property, runnableLogger, level);
    }
    
    public String getPropertyAndLog(String device, String property, Logger logger, Level level){
        try{
            return core_.getProperty(device, property);
        } catch(Exception e){
            logger.log(level, e.getMessage());
            return "";
        }
    }
}
*/
