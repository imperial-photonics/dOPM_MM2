/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dopm_mm2.Runnables;

import dopm_mm2.GUI.dOPM_hostframe;
import dopm_mm2.Devices.TangoXYStage;
import dopm_mm2.util.FileMM;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import org.micromanager.PropertyMap;
import org.micromanager.PropertyMaps;
import org.micromanager.data.Datastore;
import org.micromanager.data.SummaryMetadata;
import org.micromanager.display.DisplayWindow;

/**
 *
 * @author OPMuser
 */
public class TangoXYscanRunnableInherited extends AbstractAcquisitionRunnable{
    public TangoXYscanRunnableInherited(dOPM_hostframe frame_ref){
        super(frame_ref);
    }
    @Override
    public void runSingleView(double opmAngle) throws Exception{
        // First, get scan length
        double scanLengthXY = deviceSettings.getXyStageScanLength();
        String scanAxis = deviceSettings.getXyStageScanAxis();
        double triggerDistanceUm = deviceSettings.getXyStageTriggerDistance();
        double scanSpeed = deviceSettings.getXyStageScanSpeed();

        
        runnableLogger.info(String.format(
                "%s stage scan--target scan length: %.1f; "
                        + "trigger distance: %.1f um",
                scanAxis, scanLengthXY, triggerDistanceUm));
        // 
        // undershoot so it can reach constant speed
        double scanUndershoot = 10;  // um
        double scanOvershoot = scanUndershoot; 
        DisplayWindow display;
        
        try {
            TangoXYStage.setTangoTriggerAxis(XYStagePort, scanAxis);
        } catch (Exception e){
            throw new Exception("Failed to set tango trigger axis with " + 
                    e.getMessage());
        }
        try {
            TangoXYStage.setTangoTriggerDistance(XYStagePort, scanAxis,
                    triggerDistanceUm*1e-3);
        } catch (Exception e){
            throw new Exception("Failed to set tango trigger distance with " + 
                    e.getMessage());
        }
        
        // work out range so that an integer number of triggers has the correct
        // trigger distance (the tango trigger range is a bit like numpy 
        // linspace)
        double startingScanPosition;
        switch (scanAxis){
            case "x":
                startingScanPosition = startingXPosition;
                break;
            case "y":
                startingScanPosition = startingYPosition;
                break;
            default:
                throw new Exception("scanAxis should be x or y");
        }  

        double triggerScanStartUm = startingScanPosition - scanLengthXY/2;
        double targetTriggerScanEndUm = startingScanPosition + scanLengthXY/2;
        double[] triggerRangeMillim = new double[]{
            triggerScanStartUm*1e-3, targetTriggerScanEndUm*1e-3};
        
        double actualTriggerScanEndMillim = targetTriggerScanEndUm*1e-3;
        double actualTriggerScanEndUm = targetTriggerScanEndUm;  // initial val
        
        // Set trigger range for tango to define volume scan length. My function
        // returns the actual scan length for an integer number of triggers
        try {
            actualTriggerScanEndMillim = TangoXYStage.setTangoTriggerRange(
                    XYStagePort, scanAxis, triggerRangeMillim)[1];
        } catch (Exception e){
            throw new Exception(String.format("Failed to set Tango %s "
                    + "trigger range to [%.4f, ~%.4f] mm with exception %s", 
                    scanAxis, triggerRangeMillim[0], triggerRangeMillim[1],
                    e.getMessage()));
        }
        double actualScanLength = actualTriggerScanEndUm - triggerScanStartUm;
        double scanStartUm = triggerScanStartUm - scanUndershoot;
        double scanEndUm = actualTriggerScanEndMillim*1e3 + scanOvershoot;
        int nFrames = (int)(actualScanLength/triggerDistanceUm);
        
        // move to start of scan (a little before trigger range start)
        try {
            TangoXYStage.setAxisPosition(XYStage, scanStartUm, scanAxis);
        } catch (Exception e){
            throw new Exception(String.format(
                    "Failed to move to start %s scan position %.1f um",
                    scanAxis, scanStartUm));
        }
        
        try {
            TangoXYStage.setTangoTriggerEnable(XYStagePort, scanAxis, 1);
        } catch (Exception e){
            throw new Exception(String.format("Failed to enable triggering "
                    + "%s axis of tango with exception %s", 
                    scanAxis, e.getMessage()));
        }
        
        // Create datastore
        Datastore store;
        if (frame_.isSaveImgToDisk()){
            try {
                PropertyMap myPropertyMap = PropertyMaps.builder().
                    putString("scan type", "stage scanning").
                    putString("scan axis", scanAxis).
                    putDouble("trigger distance um", triggerDistanceUm).
                    putDouble("scan length um", actualScanLength).
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
                
        // start stage motion for triggered acquisition
        runnableLogger.info(String.format("Starting XY (%s) stage scanning "
                + "[start: %.2f um, frames: %d, end %.2f um]",
                scanAxis, scanStartUm, nFrames, scanEndUm));
        try {
            core_.setProperty(XYStage, "Velocity", scanSpeed);
            TangoXYStage.setAxisPosition(XYStage, scanEndUm, scanAxis);
        } catch (Exception e){
            throw new Exception(String.format(
                    "Failed to move stage to %s end scan position %.1f um",
                    scanAxis, scanEndUm));
        }
        // Acquire volume in the trigger loop
        try{
            acquireTriggeredDataset(store, scanEndUm, nFrames);
        } catch (TimeoutException e){
            throw e;
        } catch (Exception e2){
            throw new Exception("Unknown error occured in triggered sequence"
                    + "acquisition: " + e2.getMessage());
        } finally {
            // Freeze and close datastore
            store.freeze();
            if(frame_.isSaveImgToDisk()) store.close();
        }
        
        // Disable triggering, stop sequence
        try {
            TangoXYStage.setTangoTriggerEnable(XYStagePort, scanAxis, 0);
        } catch (Exception e){
            throw new Exception(String.format("Failed to disable triggering "
                    + "%s axis of tango with exception %s", 
                    scanAxis, e.getMessage()));
        }
        core_.stopSequenceAcquisition(camName);
        
    }
}
