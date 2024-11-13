/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dopm_mm2.Runnables;

import dopm_mm2.GUI.dOPM_hostframe;
import dopm_mm2.Devices.TangoXYStage;
import dopm_mm2.acquisition.MDAProgressManager;
import dopm_mm2.util.FileMM;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import org.micromanager.PropertyMap;
import org.micromanager.PropertyMaps;
import org.micromanager.data.Datastore;
import org.micromanager.data.SummaryMetadata;
import org.micromanager.display.DisplayWindow;

/** Runnable for Tango XY stage scanning acquisition.
 * Note that all units are in um here because default precision too low to
 * set trigger distance to order 1um, and also the triggering for the OPM 
 * in 712 was also done in micron
 * 
 *
 * @author lnr19
 */
public class TangoXYscanRunnableInherited extends AbstractAcquisitionRunnable{
    public TangoXYscanRunnableInherited(dOPM_hostframe frame_ref, 
            MDAProgressManager acqProgressMgr){
        super(frame_ref, acqProgressMgr);
        
        // init dimenions of tango
        try {
            TangoXYStage.setTangoXyUnitsToUm(XYStagePort);
        } catch (Exception e){
            logErrorWithWindow(e);
        }
    }
    @Override
    public void runSingleView(double opmAngle) throws Exception{
        // First, get scan length
        double scanLengthXyUm = deviceSettings.getXyStageScanLength();
        String scanAxis = deviceSettings.getXyStageScanAxis();
        double triggerDistanceUm = deviceSettings.getXyStageTriggerDistance();
        double scanSpeed = deviceSettings.getXyStageCurrentScanSpeed();

        
        runnableLogger.info(String.format("%s stage scan \n "
                        + "target scan length: %.1f; "
                        + "trigger distance: %.1f um; "
                        + "scan speed %.4f mm/s; "
                        + "",
                scanAxis, scanLengthXyUm, triggerDistanceUm, scanSpeed));
        // 
        // undershoot so it can reach constant speed
        double scanUndershoot = 10;  // um
        double scanOvershoot = scanUndershoot; 
        DisplayWindow display;
        long start_;
        
        start_ = System.currentTimeMillis();
        try {
            TangoXYStage.setTangoTriggerAxis(XYStagePort, scanAxis);
        } catch (Exception e){
            throw new Exception("Failed to set tango trigger axis with " + 
                    e.getMessage());
        }
        try {
            TangoXYStage.setTangoTriggerDistance(XYStagePort, scanAxis,
                    triggerDistanceUm);
        } catch (Exception e){
            throw new Exception("Failed to set tango trigger distance with " + 
                    e.getMessage());
        }
        runnableLogger.info(String.format("trigger axis and distance setup time %d ms", 
                System.currentTimeMillis()-start_));
        // work out range so that an integer number of triggers has the correct
        // trigger distance (the tango trigger range is a bit like numpy 
        // linspace)
        double startingScanPosition;
        switch (scanAxis){
            case "x":
                startingScanPosition = startingXPositionUm;
                break;
            case "y":
                startingScanPosition = startingYPositionUm;
                break;
            default:
                throw new Exception("scanAxis should be x or y");
        }  

        double triggerScanStartUm = startingScanPosition - scanLengthXyUm/2;
        double targetTriggerScanEndUm = startingScanPosition + scanLengthXyUm/2;
        
        double[] triggerRangeUm = new double[]{
            triggerScanStartUm, targetTriggerScanEndUm};
        
        double[] triggerRangeMillim = new double[]{
            triggerScanStartUm*1e-3, targetTriggerScanEndUm*1e-3};
        
        double actualTriggerScanEndMillim;
        double actualTriggerScanEndUm;
        
        // Set trigger range for tango to define volume scan length. My function
        // returns the actual scan length for an integer number of triggers
        
        ////////////////////////////////////////////////////////////////////////
        /////// I set dim earlier to 10, which means microns are used !! ///////
         
        start_ = System.currentTimeMillis();
        try {
            // set triggers and gets end point of trigger range
            actualTriggerScanEndUm = 
                   TangoXYStage.setTangoTriggerRange(
                    XYStagePort, scanAxis, triggerRangeUm)[1];
            actualTriggerScanEndMillim = actualTriggerScanEndUm*1e-3;
        } catch (Exception e){
            throw new Exception(String.format("Failed to set Tango %s "
                    + "trigger range to [%.4f, ~%.4f] um with exception %s", 
                    scanAxis, triggerRangeUm[0], triggerRangeUm[1],
                    e.getMessage()));
        }
        double actualScanLength = actualTriggerScanEndUm - triggerScanStartUm;
        double scanStartUm = triggerScanStartUm - scanUndershoot;
        double scanEndUm = actualTriggerScanEndUm + scanOvershoot;
        int nFrames = (int)(actualScanLength/triggerDistanceUm);
        
        runnableLogger.info(String.format("Trigger interval set time %d ms", 
                System.currentTimeMillis()-start_));
        
        start_ = System.currentTimeMillis();
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
        runnableLogger.info(String.format("trigger enable and move to start time %d ms", 
                System.currentTimeMillis()-start_));
        
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

                // feed datastore extra metadata info specific to stage scan
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
        start_ = System.currentTimeMillis();
        core_.prepareSequenceAcquisition(camName);
        core_.startSequenceAcquisition(nFrames, 0, true);
        runnableLogger.info(String.format("Sequence setup time %d ms", 
            System.currentTimeMillis()-start_));
                
        // start stage motion for triggered acquisition
        runnableLogger.info(String.format("Starting XY (%s) stage scanning "
                + "[start: %.2f um, frames: %d, end %.2f um]",
                scanAxis, scanStartUm, nFrames, scanEndUm));
        
        start_ = System.currentTimeMillis();
        try {
            TangoXYStage.setTangoAxisSpeed(XYStage, scanAxis, scanSpeed);
            TangoXYStage.setAxisPosition(XYStage, scanEndUm, scanAxis);
        } catch (Exception e){
            throw new Exception(String.format(
                    "Failed to move stage to %s end scan position %.1f um",
                    scanAxis, scanEndUm));
        }
        runnableLogger.info(String.format("Move to start of scan time %d ms", 
            System.currentTimeMillis()-start_));
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
            // apparently the metadata is not saved 
            // unless sequence acquisition is stopped!
            double acqstopStart = System.currentTimeMillis();
            core_.stopSequenceAcquisition(camName);
            runnableLogger.info(String.format("stopSequenceAcquisition time %.2f ms",
                    System.currentTimeMillis()-acqstopStart));
            if (store != null){
                double freezeStart = System.currentTimeMillis();
                store.freeze();
                if(frame_.isSaveImgToDisk()) store.close();
                runnableLogger.info(String.format("DS freezing time %.2f ms",
                        System.currentTimeMillis()-freezeStart));
            } else {
                runnableLogger.severe("Can't freeze/close empty datastore");
            }
        }
        
        // Disable triggering, stop sequence
        start_ = System.currentTimeMillis();
        try {
            TangoXYStage.setTangoTriggerEnable(XYStagePort, scanAxis, 0);
            runnableLogger.info("Tango Error? " + 
                    TangoXYStage.getTangoErrorMsg(XYStagePort));

        } catch (Exception e){
            throw new Exception(String.format("Failed to disable triggering "
                    + "%s axis of tango with exception %s", 
                    scanAxis, e.getMessage()));
        }
        runnableLogger.info(String.format("trigger disable time %d ms", 
            System.currentTimeMillis()-start_));
        
    }
}
