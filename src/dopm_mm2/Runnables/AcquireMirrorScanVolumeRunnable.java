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
import java.util.logging.Logger;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;
import mmcorej.TaggedImage;
import org.micromanager.display.DisplayWindow;
import org.micromanager.data.Coordinates;
import org.micromanager.data.Coords;
import org.micromanager.data.Image;
import dopm_mm2.util.FileMM;

/**
 *
 * @author lnr19
 */
public class AcquireMirrorScanVolumeRunnable {

    private final dOPM_hostframe frame_;
    private final CMMCore core_;
    private final Studio mm_;
    private DisplayManager displayManager;
    private int PIDeviceID;
    private static final Logger runnableLogger = 
            Logger.getLogger(AcquireMirrorScanVolumeRunnable.class.getName());
    private DeviceManager deviceSettings;
    private PIStage control;
    private DisplayWindow display;
    
    public AcquireMirrorScanVolumeRunnable(dOPM_hostframe frame_ref) {
        frame_ = frame_ref;
        mm_ = dOPM_hostframe.mm_;
        core_ = mm_.getCMMCore();
        deviceSettings = frame_.getDeviceSettings();
        PIDeviceID = 1;
    }

    @SuppressWarnings("UnusedAssignment")
    public void run() throws Exception {

        String settingsOutDir = frame_.getSettingsFolderDir();
        // FileWriter logFile = new FileWriter(settingsOutDir + "log.txt");
        // BufferedWriter runLog = new BufferedWriter(logFile);
        
        // just print log for this runnable for debugging directly
        FileHandler fh = new FileHandler(settingsOutDir + "log.txt");  
        runnableLogger.addHandler(fh);
        SimpleFormatter formatter = new SimpleFormatter();  
        fh.setFormatter(formatter);  
        
        ScriptController sc = mm_.getScriptController();
        
        sc.message("Running Mirror-scan volume acquisition!");

        String camName = deviceSettings.getCameraDeviceName();
        String mirrorStage = deviceSettings.getMirrorStageDeviceName();
        String XYStage = deviceSettings.getXYStageDeviceName();
        String ZStage = deviceSettings.getZStageDeviceName();
        
        // Set serial command
        String port = deviceSettings.getMirrorStageComPort();
        String commandTerminator = "\n"; 

        // disable triggering, set trigger to low
        try {
            control.setupPITriggering(port, PIDeviceID);
        } catch (Exception e){
            runnableLogger.severe("Failed to setup triggering with " + e.getMessage());
            throw (e);
        }

        // Setup in um
        double x_pos;
        double y_pos;
        double z_pos;
        double mirror_pos;
        if (!XYStage.equals("")){
            x_pos = core_.getXPosition(XYStage);
            y_pos = core_.getYPosition(XYStage);
        }
        if (!ZStage.equals("")) z_pos = core_.getPosition(ZStage);
        if (!mirrorStage.equals("")){
            mirror_pos = core_.getPosition(mirrorStage);
        } else {
            throw new Exception("No mirror position found, check device is connected");
        }

        double startMirrorScan = mirror_pos;  // scan start in um
        double endMirrorTarget = mirror_pos + deviceSettings.getScanLength();  // target scan end in um
        double scanSpeed = deviceSettings.getMirrorStageSpeed();  // scan speed in mm/s or um/ms
        double trigDist = deviceSettings.getTriggerDistance();  // trigger distance in um
        String trgDistStr = Double.toString(trigDist/1000); // in mm
        double exposure = deviceSettings.getExposureTime();

        // Make sure camera is in internal triggering first before moving stages etc.
        core_.setProperty(camName, "TRIGGER SOURCE","INTERNAL");
        
        // based on settings, e.g. trigger mode and exposure, get readout time
        double readoutTimeMs = deviceSettings.getCameraReadoutTime();

        double maxFPS = 1000/readoutTimeMs;

        // undershoot and overshoot for ramp up and down
        double undershoot = 5;  // 5 um
        double overshoot = 5; 

        // actual end point of scan is a multiple of trigger distances:
        double endMirrorScan = trigDist*Math.floor((endMirrorTarget-startMirrorScan)/trigDist);
        runnableLogger.info(
                "Target scan end is " + endMirrorTarget + ", actual scan end is " + endMirrorScan);

        double effectiveFPS = (1000/(trigDist/scanSpeed));

        // should never happen v, this is controlled in DeviceManager in setters
        if (readoutTimeMs > trigDist/scanSpeed){
                throw new Exception("Trigger intervals too fast for camera readout time, frames will be dropped");
        }

        int nFramesTotal = (int)Math.floor((endMirrorScan-startMirrorScan)/trigDist);  // number of frames

        sc.message("Now running with hardware triggering");
        runnableLogger.info("Now running with hardware triggering");
        
        
        Datastore store = mm_.data().createRAMDatastore();
        display = mm_.displays().createDisplay(store);
        Coords.Builder cb = Coordinates.builder();

        // TODO have a look at this...
        // note -- getCoordsBuilder is deprecated , so using the new version
        cb = mm_.data().coordsBuilder().z(0).channel(0).stagePosition(0);
        // coordsBuilder_reflected = coordsBuilder_reflected.p(counter_reflected);
        // img = img.copyAtCoords(coordsBuilder_reflected.build());

        int frameTimeout = 2000; // 2s

        double stopLastAcqStart = System.currentTimeMillis();
        try{
                core_.stopSequenceAcquisition();
                //store.freeze();
                //store.close();
        } catch (Exception e){
                runnableLogger.info(e.getMessage());
        }
        double stopLastAcqTime = System.currentTimeMillis() - stopLastAcqStart;
        runnableLogger.info("Stopping sequence acquisition took " + stopLastAcqTime + " ms");

        double acqStart = System.currentTimeMillis();
        core_.clearCircularBuffer();
        core_.setProperty(camName, "TriggerPolarity","POSITIVE");  
        core_.setProperty(camName, "TRIGGER SOURCE","EXTERNAL");  

        // starts frame grabber
        core_.prepareSequenceAcquisition(camName);
        core_.startSequenceAcquisition(nFramesTotal, 0, true);

        int nFrames=0;
        double frameTimeTotal = 0;
        
        // answerTerminator = "ok"; //reprap gcode terminates with this
        // answer = core_.getSerialPortAnswer(port, answerTerminator); 
        // print(answer);

        // CTO 1 2 1 (set axis to be moved as 1)
        // CTO 1 3 0 (set trigger mode to PositionDistance)
        // CTO 1 1 0.01 (set to send trigger every 0.01 mm travelled)
        // TRO 1 1 (enable triggering)

        // set to beginning
        core_.waitForDevice(mirrorStage);
        core_.setProperty(mirrorStage, "Velocity", 100);
        double currentMirrorScanspeed = 
                Double.parseDouble(core_.getProperty(mirrorStage, "Velocity"));

        core_.setPosition(mirrorStage, startMirrorScan-undershoot);

        core_.setProperty(camName, "Exposure", exposure);

        // wait until reached start position
        // replaced with command that polls moving state #5
        String moveState;
        double waitStart = System.currentTimeMillis();
        double waitTimeout = 10e3;  // ms
        while(!control.checkPIMotion(port).equals("0") && 
                System.currentTimeMillis()-waitStart < waitTimeout){
            core_.setProperty(mirrorStage, "Velocity", 100);  // ensure is full speed
            Thread.sleep(100);
        }
        if (!control.checkPIMotion(port).equals("0")){
            throw new Exception("Timed out moving stage to start");
        }

        core_.setProperty(mirrorStage, "Velocity", scanSpeed);
        
        // do I need this? below
        // core_.setSerialPortCommand(port, "", commandTerminator);

        // set start and end points for triggering (so we can ramp up without triggers)

        double startScan_mm = (double)startMirrorScan/1000;
        double endScan_mm = (double)endMirrorScan/1000;
        double trigDist_mm = (double)trigDist/1000;

        control.stopPIStage(port);  // need this command so PI stage plays ball
        control.setPITriggerDistance(port, PIDeviceID, trigDist_mm);
        control.setPITriggerRange(port, PIDeviceID, new double[]{startScan_mm, endScan_mm});
        
        String startScanMsg = "Starting scan: exp_time=" + core_.getExposure() + "ms, min_z=" + 
                startMirrorScan + " um, max_z=" + endMirrorScan + " um, scan speed=" + scanSpeed + 
                "mm/s ("+ nFramesTotal + " frames)"; 

        runnableLogger.info(startScanMsg);
        sc.message(startScanMsg);

        //core_.setPosition(PIDevice, (max_z+1));  // start movement
        boolean timeout = false;
        double acqTimeStart = System.currentTimeMillis();

        core_.waitForDevice(mirrorStage);
        core_.waitForSystem();
        boolean grabbed = false;
        // throw new Exception("blba balbal");
        while (nFrames < nFramesTotal && !timeout){

                // print(pos);
                // start movement, end at trig dist/2 so trigger goes down again (might not work with start and stop trigger set)
                //core_.setPosition(PIDevice, max_z+(trig_dist/2));  
                core_.setPosition(mirrorStage, endMirrorScan+overshoot);  
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
        
        // command to wait until stopped moving and throw exception if times out doing this?
        
        runnableLogger.info("Acq time: " + acqTimeStop + "ms");
        runnableLogger.info("end position: " + core_.getPosition(mirrorStage));

        control.setPITriggerEnable(port, 0);
                
        core_.setSerialPortCommand(port, "TRO 1 0", commandTerminator); // disable triggering
        core_.setSerialPortCommand(port, "STP", commandTerminator);
        core_.setSerialPortCommand(port, "ERR?", commandTerminator);
        String err = core_.getSerialPortAnswer(port, commandTerminator);
        core_.setSerialPortCommand(port, "DIO 1 0", commandTerminator);

        runnableLogger.info("nframes: " + nFrames);
        runnableLogger.info("frames dropped? : " + (nFramesTotal-nFrames));
        runnableLogger.info("total acq time: " + frameTimeTotal);
        //runnableLogger.info("average frame time: " + frame_time_total/nframes);
        core_.stopSequenceAcquisition();
        store.freeze();
        store.close();
        double acqEnd = System.currentTimeMillis();


        core_.setProperty(camName, "TRIGGER SOURCE","INTERNAL");  

        double elapsed = acqEnd-acqStart;
        runnableLogger.info("time elapsed (cam and stage) " + elapsed + " ms");
    }
}
