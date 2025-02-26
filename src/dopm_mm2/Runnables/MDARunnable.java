/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dopm_mm2.Runnables;
import dopm_mm2.Devices.DeviceSettingsManager;
import dopm_mm2.GUI.dOPM_hostframe;
import dopm_mm2.acquisition.MdaBridge;
import dopm_mm2.util.dialogBoxes;
// forward logs from runnables to MDARunnable
import static dopm_mm2.Runnables.AbstractAcquisitionRunnable.runnableLogger;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import mmcorej.CMMCore;
import org.micromanager.Studio;
import org.micromanager.acquisition.AcquisitionManager;
import org.micromanager.data.Datastore;

 // can access acquisition engine here
// from messing around:
// import org.micromanager.internal.MMStudio;  
// import org.micromanager.acquisition.internal.AcquisitionEngine;
// import org.micromanager.acqj.main.AcquisitionEvent;

/**
 * Runnable that attaches the desired dOPM imaging modality to the MicroManager
 * MDA, creates folders, and launches the MDA
 * @author Leo Rowe-Brown
 */
public class MDARunnable implements Runnable {
    private final CMMCore core_;
    private final Studio mm_;
    private final DeviceSettingsManager deviceSettings;
    private static final Logger mdaRunnableLogger = 
            Logger.getLogger(MDARunnable.class.getName());
    private AcquisitionManager acq_;
    private MdaBridge mdaMgr;
    private AbstractAcquisitionRunnable dopmRunnable;
    protected String acqTimestamp;
    protected String scanTypeLabel;
    protected String dataOutRootDir;
    protected boolean saveToDisk;

    public MDARunnable(Studio mm, 
            DeviceSettingsManager deviceSettings, 
            int scanType,
            String dataOutRootDir,
            boolean saveToDisk) {
        this.core_ = mm.getCMMCore();
        this.mm_ = mm;
        this.acq_ = mm_.getAcquisitionManager();
        this.deviceSettings = deviceSettings;
        this.dataOutRootDir = dataOutRootDir;
        this.saveToDisk = saveToDisk;
        
        // Need this MdaBridge class to keep track of the indices in the MDA.
        // maybe move to run()
        try {
            mdaMgr = new MdaBridge();
        } catch (Exception e){
            String err = "Failed to create the "
                    + "dOPM MDA acqusition manager (used to find "
                    + "acquisition indices): " + e.getMessage();
            mdaRunnableLogger.severe(err);
            dialogBoxes.acquisitionErrorWindow(err);
        }
        
        // ASIDE: Maybe the new acqJ allows one to get the indicies from the
        // acquisition engine?
        // acqJ_.addHook(new AcquisitionHook(), AFTER_HARDWARE_HOOK);
        
        switch(scanType){
            case 0:  // PI scan runnable
                dopmRunnable = new PIScanRunnableInherited(
                        mm, deviceSettings, mdaMgr);
                scanTypeLabel = "mirror_scan";
                break;
            case 1:  // (y) stage scan runnable
                dopmRunnable = new TangoXYScanRunnableInherited(
                        mm, deviceSettings, mdaMgr);
                scanTypeLabel = "y_stage_scan";
                break;
            default:
                mdaRunnableLogger.severe("Unknown volume scantype");
        }
    }
        
    /**
     * create log file for (custom) MDA.
     * @return 
     * @param logOutDir directory in which log file is saved
     */
    private FileHandler createLogFile(String logOutDir){
        FileHandler fh = null;
        try { 
            // Just print log for this runnable for debugging directly
            new File(logOutDir).mkdirs(); 
            fh = new FileHandler(new File(
                    logOutDir, 
                    String.format("acqLog%s.txt", acqTimestamp)).toString());
 
            runnableLogger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();  
            fh.setFormatter(formatter);  
            runnableLogger.info(String.format("Started log for %s at %s", 
                    this.getClass().getName(), logOutDir));
            return fh;
        } catch (IOException ioe){
            runnableLogger.severe("Failed to create log with " + 
                    ioe.getMessage());
            return fh;
        } catch (SecurityException se){
            runnableLogger.severe("Failed to create dirs " + se.getMessage());
            return fh;
        }
    }
    
    @Override
    public void run(){
        if (dopmRunnable != null){
            // Set up timestamp and save dirs
            LocalDateTime date = LocalDateTime.now(); // Create a date object
            DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern(
                        "yyyyMMdd-HHmmss");

            String formattedDate = date.format(myFormatObj);
            acqTimestamp = formattedDate;
            String saveDirName = String.format("%s_%s", scanTypeLabel, formattedDate);

            // make the dirs in a timestamped subdir so it doesn't overwrite
            dopmRunnable.dataOutDir = new File(
                    dataOutRootDir, saveDirName).getAbsolutePath();
            
            // tell runnable whether if it's saving to disk
            dopmRunnable.saveToDisk = saveToDisk;
            
            // save mdaRunnableLogger to file
            FileHandler logFileHandler = 
                    createLogFile(dopmRunnable.dataOutDir); 
            
            // info
            String mdaInfo =
                String.format("\n%d channels: ", mdaMgr.getnChannelPts()) 
                    + mdaMgr.getChannelNames() +
                String.format("\n%d positions: ", mdaMgr.getnPositionPts())
                    + mdaMgr.getPositionLabels() +
                String.format("\n%d z slices: ", mdaMgr.getnZPts())
                    + mdaMgr.getzSlices() + "(um)" +
                String.format("\n%d timepoints: ", mdaMgr.getnTimePts())
                    + mdaMgr.getTimepointsMs() + "(ms)" +
                "\nView 1? "
                    + (deviceSettings.isView1Imaged() ? "Yes" : "No") +
                "\nView 2? " 
                    + (deviceSettings.isView2Imaged() ? "Yes" : "No");
            
            mdaRunnableLogger.info("Starting dOPM MDA with:" + mdaInfo);
            
                                                
            try {
                // clear, otherwise we end up just adding more and more 
                // dopm acqs on 
                acq_.clearRunnables();
                // -1 indicates that runnable is executed at each point for a
                // given dimenions (there are 4)
                acq_.attachRunnable(-1, -1, -1, -1, dopmRunnable);
                
                // we dont use the snap datastore really 
                // not sure what blocking helps with really, we dont need the
                // snap datastore desperately
                Datastore snapStore = acq_.runAcquisition();

                // SummaryMetadata snapMetadata = snapStore.getSummaryMetadata();
                // mdaRunnableLogger.info("snap metadata:" + snapMetadata.toString());

                mdaRunnableLogger.info("Running " + dopmRunnable.getClass().getName());
                acq_.clearRunnables();  // remove runnable

                core_.setProperty(deviceSettings.getLaserBlankingDOport(), "Blanking", "On");
                
                
                if (dopmRunnable.acquisitionFailed){
                    dialogBoxes.acquisitionErrorWindow(dopmRunnable.exception);
                } else {
                    dialogBoxes.acquisitionComplete();
                }

            } catch (Exception e){  // look into using micromanager exceptions
                mdaRunnableLogger.severe("Failed in volume acquisition:" + 
                        e.getMessage());
            } finally {
                logFileHandler.close();
            }
        } else {
            mdaRunnableLogger.severe("Runnable is null; wasn't successfully "
                    + "created (is scantype correct?)");
        }
    }
    
}

