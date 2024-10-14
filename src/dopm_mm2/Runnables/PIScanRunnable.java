/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dopm_mm2.Runnables;

import dopm_mm2.Devices.PIStage;
import dopm_mm2.GUI.dOPM_hostframe;
import dopm_mm2.Devices.DeviceManager;
import dopm_mm2.util.FileMM;

import mmcorej.CMMCore;
import org.micromanager.ScriptController;
import org.micromanager.Studio;
import org.micromanager.data.Datastore;
import org.micromanager.display.DisplayManager;
import mmcorej.TaggedImage;
import org.micromanager.display.DisplayWindow;
import org.micromanager.data.Coords;
import org.micromanager.data.Image;

import java.util.logging.Logger;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;

import java.io.IOException;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.micromanager.data.SummaryMetadata;

/* stuff for future
import dopmMillim2.util.FileMM;
import dopmMillim2.util.MMStudioInstance;
import dopmMillim2.util.RunnableExceptionHandler;
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
    
    private double scanLengthMillim;
    private double scanSpeed;
    private double trigDistMillim;
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
        runnableLogger.info("Entered PIScanRunnable runnable");
        frame_ = frame_ref;
        mm_ = dOPM_hostframe.mm_;
        core_ = mm_.getCMMCore();
        deviceSettings = frame_.getDeviceSettings();
        PIDeviceID = 1;
        
        saveImgToDisk = frame_.isSaveImgToDisk();
        
        settingsOutDir = frame_.getSettingsFolderDir().getAbsolutePath();
        dataOutDir = frame_.getDataFolderDir().getAbsolutePath();
        baseDir = frame_.getBaseFolderDir().getAbsolutePath();
        
        runnableLogger.info("dataOutDir: " + dataOutDir);
        runnableLogger.info("baseDir: " + baseDir);
        runnableLogger.info("settingsOutDir: " + settingsOutDir);
        
        // FileWriter logFile = new FileWriter(settingsOutDir + "log.txt");
        // BufferedWriter runLog = new BufferedWriter(logFile);
                
        sc = mm_.getScriptController();
        
        camName = deviceSettings.getLeftCameraName();
        mirrorStage = deviceSettings.getMirrorStageName();
        XYStage = deviceSettings.getXyStageName();
        ZStage = deviceSettings.getZStageName();
        
        // Set serial command
        port = deviceSettings.getMirrorStageComPort();
        runnableLogger.info("For PIMag, using port: " + port);
        commandTerminator = "\n"; 
        
        scanLengthMillim = deviceSettings.getMirrorScanLength()*1e-3;  // target scan end in mm
        scanSpeed = deviceSettings.getMirrorStageScanSpeed();  // scan speed in mm/s or um/ms
        trigDistMillim = deviceSettings.getMirrorTriggerDistance()*1e-3;  // trigger distance in um
        exposure = deviceSettings.getExposureTime();
        
        runnableLogger.info(String.format("Got device settings from hostframe: "
                + "scan length[mm] %.5f; "
                + "scanSspeed[mm/s] %.5f; "
                + "trigger distance[mm] %.5f;"
                + "exposure time[ms] %.5f", 
                scanLengthMillim, scanSpeed, trigDistMillim, exposure));
    }
    
    public int makeDirsAndLog(){
        try { 
            new File(baseDir).mkdirs();
            new File(settingsOutDir).mkdirs();
            new File(dataOutDir).mkdirs();
            
            LocalDateTime date = LocalDateTime.now(); // Create a date object
            DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern(
                    "yyyyMMddhhmmss");

            String formattedDate = date.format(myFormatObj);
            
            // just print log for this runnable for debugging directly
            FileHandler fh = new FileHandler(new File(
                    settingsOutDir, 
                    String.format("acqLog%s.txt", formattedDate)).toString());
 
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
        
        // TODO find what might be causing extra commands sitting in buffer
        sc.message("Running Mirror-scan volume acquisition!");
        runnableLogger.info("Running Mirror-scan volume acquisition!");
        double theVeryBeginning = System.currentTimeMillis();

        
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
        
        String currentScanSpeed;
        
        // check isn't zero like the PI device adapter likes to do
        try {
            currentScanSpeed = core_.getProperty(mirrorStage, "Velocity");
        } catch(Exception e){
            throw new RuntimeException(e);
        }
        runnableLogger.info("Initial currentScanSpeed " + currentScanSpeed);
        
        
        // Setup in um
        double currentXPos;
        double currentYPos;
        double currentZPos;
        double mirror_posMillim;
        try {
            if (!XYStage.equals("")){
                currentXPos = core_.getXPosition(XYStage);
                currentYPos = core_.getYPosition(XYStage);
            }
            if (!ZStage.equals("")) currentZPos = core_.getPosition(ZStage);
            if (!mirrorStage.equals("")){
                mirror_posMillim = core_.getPosition(mirrorStage)*1e-3;
            } else {
                throw new Exception("No mirror position found, check device is connected");
            }
        } catch (Exception e){
            throw new RuntimeException(e.getMessage());
        }

        runnableLogger.info("scanLength (mm) is "  + scanLengthMillim);
        double startMirrorScanMillim = mirror_posMillim;  // scan start in um
        double endMirrorTargetMillim = mirror_posMillim + scanLengthMillim;  // target scan end in um

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
        runnableLogger.info("readout time (ms) " + readoutTimeMs);

        double maxFPS = 1000/readoutTimeMs;

        // undershoot and overshoot for ramp up and down
        double undershootMillim = 7*1e-3;  // 7 um
        double overshootMillim = 7*1e-3;

        // actual end point of scan is a multiple of trigger distances:
        double endMirrorScanMillim = startMirrorScanMillim + 
                trigDistMillim*Math.floor(
                (endMirrorTargetMillim-startMirrorScanMillim)/trigDistMillim);
        runnableLogger.info(
                "Target scan end is " + endMirrorTargetMillim +
                        ", actual scan end is " + endMirrorScanMillim);

        double effectiveFPS = (1/(trigDistMillim/scanSpeed));
        runnableLogger.info(
                    "Effective FPS: " + effectiveFPS);

        // should never happen v, this is controlled in DeviceManager in setters
        if (readoutTimeMs > 1e3*trigDistMillim/scanSpeed){
            runnableLogger.warning(
                    "Trigger intervals too fast for camera readout time, frames will be dropped");
        }

        int nFramesTotal = (int)Math.floor(
                (endMirrorScanMillim-startMirrorScanMillim)/trigDistMillim);  // number of frames

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
            core_.setProperty(camName, "OUTPUT TRIGGER KIND[0]","EXPOSURE");
            core_.setProperty(camName, "OUTPUT TRIGGER POLARITY[0]","POSITIVE");
            core_.setProperty(camName, "OUTPUT TRIGGER SOURCE[0]","TRIGGER");
            core_.setProperty(camName, "TRIGGER GLOBAL EXPOSURE","GLOBAL RESET");
            
            // prepares to grab frames from cam buffer
            core_.prepareSequenceAcquisition(camName);
            core_.startSequenceAcquisition(nFramesTotal, 0, true);
            
        } catch (Exception e){
            runnableLogger.severe(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
        
        // set start and end points for triggering (we ramp up without triggers outside this range)
        
        double startScanMillim = startMirrorScanMillim-undershootMillim;
        double endScanMillim = endMirrorScanMillim+overshootMillim;
        
        runnableLogger.info("Trigger range: " + startMirrorScanMillim + " to " + endMirrorScanMillim);
        runnableLogger.info("Trigger distance: " + trigDistMillim);
        
        try {
            PIStage.stopPIStage(port);  // need this command so PI stage plays ball
            PIStage.setPITriggerDistance(port, PIDeviceID, trigDistMillim);
            PIStage.setPITriggerRange(port, PIDeviceID, new double[]{startMirrorScanMillim, endMirrorScanMillim});
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
            runnableLogger.info("currentMirrorScanspeed after 100 " + currentMirrorScanspeed);
            sc.message("currentMirrorScanspeed after 100 " + currentMirrorScanspeed);

            PIStage.setPositionMillim(mirrorStage, startScanMillim);

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
            while(PIStage.checkPIMotion(port) && 
                    (System.currentTimeMillis()-waitStart) < waitTimeout){
                // core_.setProperty(mirrorStage, "Velocity", 100);  // ensure is full speed
                Thread.sleep(200);
                runnableLogger.info("Still moving, waiting...");
            }
            if (PIStage.checkPIMotion(port)){
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
            currentScanSpeed = core_.getProperty(mirrorStage, "Velocity");
        } catch(Exception e){
            throw new RuntimeException(e);
        }
        runnableLogger.info("Scan speed after check motion " + currentScanSpeed);
        

        try {
            runnableLogger.info("Setting " + mirrorStage + " velocity to " + scanSpeed);
            core_.setProperty(mirrorStage, "Velocity", scanSpeed);
        } catch (Exception e) {
            runnableLogger.severe("Failed to set scan speed with " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
                
        try {
            currentScanSpeed = core_.getProperty(mirrorStage, "Velocity");
        } catch(Exception e){
            throw new RuntimeException(e);
        }
        runnableLogger.info("Scan speed: " + scanSpeed + 
                " (from core " + currentScanSpeed + ")");
        
        // do I need this? below
        // core_.setSerialPortCommand(port, "", commandTerminator);
        
        String startScanMsg = "Starting scan: exp_time=" + exposure + "ms, min_z=" + 
                startMirrorScanMillim + " mm, max_z=" + endMirrorScanMillim + " mm, scan speed=" + scanSpeed + 
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
        
        double beforeFileCreation = System.currentTimeMillis();
        
        runnableLogger.info(String.format(
                "SETUP TIME: %.2f ms", (beforeFileCreation-theVeryBeginning)));
        
        Datastore store;
        if (saveImgToDisk){
            dataSavePath = (new File(dataOutDir, "MMStack")).getAbsolutePath();
            try {
                store = FileMM.createDatastore(camName, dataSavePath, true);                
                // these days you need to create metadata with these TIFF datastores...
                SummaryMetadata metaData = mm_.data().summaryMetadataBuilder().build();
                store.setSummaryMetadata(metaData);
            } catch (IOException ie){
                runnableLogger.severe("Failed to create datastore in " + dataSavePath +
                        " with " + ie.getMessage());
                return;
            }
        } else {
            store = mm_.data().createRAMDatastore();
            display = mm_.displays().createDisplay(store);
        }
        double afterFileCreation = System.currentTimeMillis();
        runnableLogger.info(String.format(
                "DATASTORE FILE CREATION/RAM ALLOCATION TIME: %.2f ms", 
                (afterFileCreation-theVeryBeginning)));
        
        // TODO: THIS IS A TEST, REMOVE HARD CODING 
        // Maybe move this to the start
        // String daqDODevice = deviceSettings.getDaqDOPortDeviceName();
        String daqDODevice = "NIDAQDO-Dev2/port0";
        String lineName = "line2";  // 515
        // String lineName = deviceSettings.getLaserBlankingLines().get(
            // deviceSettings.getCurrentAcqChannel());
        
        // Laser triggering on!
        try {
            core_.setProperty(daqDODevice, "Blanking", "On");  // blank on
            core_.waitForDevice(daqDODevice);  // so that the line doesn't get set before blanking is on
            Thread.sleep(160); // seems that the blanking lags a bit so that
                               // setting the line happens ~160 ms before 
                               // very silly. Happens in intervals of 40ms...
            
            core_.setProperty(daqDODevice, lineName, 1);  // laser digital on
        } catch (Exception e) {
            runnableLogger.severe("Failed to intitialise laser blanking " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
        
        try {
            store = acquireTriggeredDataset(store, endScanMillim, nFramesTotal);
        } catch (Exception e){
            runnableLogger.severe("Triggered acquisition failed with " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
                
        
        try{
            cleanUpAcq(store);
            core_.setProperty(daqDODevice, lineName, 0);  // laser digital on
            core_.setProperty(daqDODevice, "Blanking", "Off");  // blank on


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
            if(saveImgToDisk) store.close();
        }
    }
    
    public Datastore acquireTriggeredDataset(Datastore store, double scanEnd, int nFramesTotal)
            throws Exception {
        // Coords.Builder cb = Coordinates.builder();
        boolean timeout = false;
        double acqTimeStart = System.currentTimeMillis();
    
        // Coords.Builder cb = mm_.data().coordsBuilder().z(0).channel(0).stagePosition(0);
        Coords.Builder cb = mm_.data().coordsBuilder().z(0);

        boolean grabbed = false;
        int nFrames = 0;

        double frameTimeTotal = 0;
        int frameTimeout = 2000; // if no frame received for 2s, time out

        try {
            PIStage.setPITriggerEnable(port, 1);
            PIStage.setPositionMillim(mirrorStage, scanEnd);  

        } catch (Exception e) {
            runnableLogger.severe(
                    "Failed to enable triggering in acq loop with " + e.getMessage());
            throw new Exception("Failed to enable triggering in acq loop with " + e.getMessage());
        }
        while (nFrames < nFramesTotal && !timeout){

                // print(pos);
                // start movement, end at trig dist/2 so trigger goes down again (might not work with start and stop trigger set)
                //core_.setPosition(PIDevice, max_z+(trig_dist/2));  
                double tic=System.currentTimeMillis();
                double toc=tic;

                grabbed = false;
                while(toc-tic < frameTimeout && !grabbed){
                        // wait for an image in the circular buffer
                        if (core_.getRemainingImageCount() > 0){
                            TaggedImage img = core_.popNextTaggedImage();	// TaggedImage
                            // runnableLogger.info("Got tagged image:" + nFrames);
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
        try {
            PIStage.setPITriggerEnable(port, 0);
        } catch (Exception e) {
            runnableLogger.severe(
                    "Failed to disable triggering in acq loop with " + e.getMessage());
        }        
        double acqTimeStop = System.currentTimeMillis() - acqTimeStart; 
        runnableLogger.info("Acq time in acquireTriggeredDataset: [ms]" + acqTimeStop);
        runnableLogger.info("End position: " + core_.getPosition(mirrorStage));
        
        runnableLogger.info("nFrames: " + nFrames);
        runnableLogger.info(String.format("Actual effective FPS: %.2f", 1e3*nFrames/frameTimeTotal));

        runnableLogger.info("Frames dropped: " + (nFramesTotal-nFrames));
        runnableLogger.info("Total time in frame grabbing loop [ms]: " + frameTimeTotal);
        
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
