/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dopm_mm2.Runnables;

import dopm_mm2.Devices.PIStage;
import dopm_mm2.GUI.dOPM_hostframe;
import static dopm_mm2.Runnables.AbstractAcquisitionRunnable.runnableLogger;
import dopm_mm2.acquisition.MDAListener;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import org.micromanager.PropertyMap;
import org.micromanager.PropertyMaps;
import org.micromanager.data.Datastore;
import org.micromanager.display.DisplayWindow;

/**
 *
 * @author OPMuser
 */
public class PIScanRunnableInherited extends AbstractAcquisitionRunnable{
        private final int PIDeviceID;
        
        public PIScanRunnableInherited(dOPM_hostframe frame_ref, 
                MDAListener acqMgr){
        super(frame_ref, acqMgr);
        PIDeviceID = 1;
    }
    
    @Override
    public void runSingleView(double opmAngle) throws Exception{
        
        DisplayWindow display;  // if saved to RAM
        
        // set trigger output DO pin on PI controller to low, 
        // set trigger mode, etc.
        try {
            PIStage.setupPITriggering(mirrorStagePort, PIDeviceID);
        } catch (Exception e){
            runnableLogger.severe("Failed in initial triggering setup with: " 
                    + e.getMessage());
            throw new Exception(e.getMessage());
        }

        double scanLengthUm = deviceSettings.getMirrorScanLength();
        double triggerDistanceUm = deviceSettings.getMirrorTriggerDistance();
        double triggerDistanceMillim = triggerDistanceUm*1e-3;
        double scanSpeed = deviceSettings.getMirrorStageCurrentScanSpeed();
        
        runnableLogger.info(String.format("mirror scan: target scan length; %.1f, "
                        + "trigger distance; %.1f um, scan speed; %.4f, "
                        + "starting mirror position; %.4f",
                scanLengthUm, triggerDistanceUm, 
                scanSpeed, startingMirrorPositionUm));
        // 
        // undershoot so it can reach constant speed
        double scanUndershoot = 10;  // um
        double scanOvershoot = scanUndershoot; 
        
        double triggerScanStartUm = startingMirrorPositionUm - scanLengthUm/2;
        double targetTriggerScanEndUm = startingMirrorPositionUm + scanLengthUm/2;
        
        // actual scan end with integer number of triggers
        double triggerScanEndUm = triggerScanStartUm + 
                triggerDistanceUm*Math.floor(
                (targetTriggerScanEndUm-triggerScanStartUm)/triggerDistanceUm);
        double triggerScanEndMillim = triggerScanEndUm*1e-3;
        double triggerScanStartMillum = triggerScanStartUm*1e-3;
        
        double scanStartUm = triggerScanStartUm - scanUndershoot;
        double scanEndUm = triggerScanEndUm + scanOvershoot;
        double acquiredScanLengthUm = triggerScanEndUm - triggerScanStartUm;
        
        runnableLogger.info(
                "Target scan end is " + targetTriggerScanEndUm +
                        ", actual scan end is " + triggerScanEndUm);

        double effectiveFPS = (1/(triggerDistanceMillim/scanSpeed));
        runnableLogger.info(
                    "Effective FPS: " + effectiveFPS);

        int nFrames = (int)((triggerScanEndUm-triggerScanStartUm)/triggerDistanceUm);  
 
        try {
            PIStage.stopPIStage(mirrorStagePort);  // need this command so PI stage plays ball
            PIStage.setPITriggerDistance(mirrorStagePort, PIDeviceID, triggerDistanceMillim);
            PIStage.setPITriggerRange(mirrorStagePort, PIDeviceID, 
                    new double[]{triggerScanStartMillum, triggerScanEndMillim});
        } catch (Exception e){
            runnableLogger.severe("Failed setting PI trigger settings with " + e.getMessage());
            throw e;
        }
        
        // Move to start
        try {
            core_.setPosition(mirrorStage, scanStartUm);
        } catch (Exception e){
            runnableLogger.severe("Failed to move to PI stage to scan start position");
            throw e;
        }
        
        // Create datastore
        Datastore store;
        if (frame_.isSaveImgToDisk()){
            try {
                PropertyMap myPropertyMap = PropertyMaps.builder().
                    putString("scan type", "PI mirror scanning").
                    putDouble("trigger distance", triggerDistanceUm).
                    putDouble("scan length", acquiredScanLengthUm).
                        build();

                store = createDatastore(myPropertyMap);
       
            } catch (IOException ie){
                throw ie;
            } catch (Exception e){
                throw new Exception ("Unknown error creating datastore: " 
                        + e.getMessage());
            }
        } else {
            store = mm_.data().createRAMDatastore();
            display = mm_.displays().createDisplay(store);
        }
        
        // Enable laser blanking with trigger
        core_.setProperty(DAQDOPort, "Blanking", "On");
        
        // Prepare to grab frames from cam buffer
        core_.prepareSequenceAcquisition(camName);
        core_.startSequenceAcquisition(nFrames, 0, true);
        
        try {
            PIStage.setPITriggerEnable(mirrorStagePort, 1);
        } catch (Exception e) {
            runnableLogger.severe(
                    "Failed to enable triggering with: " + e.getMessage());
            throw new Exception("Failed to enable triggering with: " + e.getMessage());
        }
                
        // Start stage motion for triggered acquisition
        runnableLogger.info(String.format("Starting PI stage scanning "
                + "[start: %.2f um, frames: %d, end %.2f um], scan speed: %.4f",
                scanStartUm, nFrames, scanEndUm, scanSpeed));
        try {
            core_.setProperty(mirrorStage, "Velocity", scanSpeed);
            core_.setPosition(mirrorStage, scanEndUm);
        } catch (Exception e){
            throw new Exception(String.format(
                    "Failed to move PI stage to %s end scan position %.1f um",
                    scanEndUm));
        }
        // Acquire volume in the trigger loop
        // Maybe we can handle all of this in the abstract class instead
        try{
            acquireTriggeredDataset(store, scanEndUm, nFrames);
        } catch (TimeoutException e){
            throw e;
        } catch (Exception e2){
            throw new Exception("Unknown error occured in triggered sequence"
                    + "acquisition: " + e2.getMessage());
        } finally {
            // Freeze and close datastore
            if (store != null){
                store.freeze();
                // keep open if RAM datastore
                if(frame_.isSaveImgToDisk()) store.close();
            }
        }
        
        // Disable triggering, stop sequence
        try {
            PIStage.setPITriggerEnable(mirrorStagePort, 1);
        } catch (Exception e){
            throw new Exception(String.format("Failed to disable PI triggering "
                    + "with exception %s", e.getMessage()));
        }
        core_.stopSequenceAcquisition(camName);
    }
}
