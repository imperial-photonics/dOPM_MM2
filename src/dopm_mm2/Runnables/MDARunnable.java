/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dopm_mm2.Runnables;

import dopm_mm2.GUI.dOPM_hostframe;
import static dopm_mm2.Runnables.AbstractAcquisitionRunnable.runnableLogger;
import dopm_mm2.acquisition.MDAProgressManager;
import dopm_mm2.util.errorTools;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javax.swing.JOptionPane;
// import javax.swing.JOptionPane;
import mmcorej.CMMCore;
import org.micromanager.Studio;
// import org.micromanager.acqj.main.AcquisitionEvent;
import org.micromanager.acquisition.AcquisitionManager;
import org.micromanager.data.Datastore;
import org.micromanager.data.SummaryMetadata;

 // can access acquisition engine here
// import org.micromanager.internal.MMStudio;  
// import org.micromanager.acquisition.internal.AcquisitionEngine;


/**
 *
 * @author lnr19
 */
public class MDARunnable implements Runnable {
    private final CMMCore core_;
    private final Studio mm_;
    private final dOPM_hostframe frame_;
    private static final Logger mdaRunnableLogger = 
            Logger.getLogger(MDARunnable.class.getName());
    private AcquisitionManager acq_;
    private MDAProgressManager mdaMgr;
    private AbstractAcquisitionRunnable snapRunnable;
    protected String acqTimestamp;
    protected String scanTypeLabel;

    public MDARunnable(dOPM_hostframe frame_ref, int scanType) {
        this.core_ = frame_ref.core_;
        this.mm_ = frame_ref.mm_;
        this.frame_ = frame_ref;
        acq_ = mm_.getAcquisitionManager();
        
        
        // get aquisition progress manager--retrieves current index of each dim
        try {
            mdaMgr = new MDAProgressManager();
        } catch (Exception e){
            String err = "Failed to create the "
                    + "dOPM MDA acqusition manager (used to find "
                    + "acquisition indices): " + e.getMessage();
            mdaRunnableLogger.severe(err);
            errorTools.acquisitionErrorWindow(err);
        }
        
        // do something like this? to get the position index
        // acqJ_.addHook(new AcquisitionHook(), AFTER_HARDWARE_HOOK);
        
        switch(scanType){
            case 0:
                snapRunnable = new PIScanRunnableInherited(
                        frame_ref, mdaMgr);
                scanTypeLabel = "mirror_scan";
                break;
            case 1:
                snapRunnable = new TangoXYscanRunnableInherited(
                        frame_ref, mdaMgr);
                scanTypeLabel = "y_stage_scan";
                break;
            case 2:  // x scan isnt actually going to exist
                snapRunnable = new TangoXYscanRunnableInherited(
                        frame_ref, mdaMgr);
                scanTypeLabel = "x_stage_scan";
                break;
            default:
                mdaRunnableLogger.severe("Unknown volume scantype");
        }
    }
    
    private void createLog(String logOutDir){
        try { 
            // Just print log for this runnable for debugging directly
            new File(logOutDir).mkdirs(); 
            FileHandler fh = new FileHandler(new File(
                    logOutDir, 
                    String.format("acqLog%s.txt", acqTimestamp)).toString());
 
            runnableLogger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();  
            fh.setFormatter(formatter);  
            runnableLogger.info(String.format("Started log for %s at %s", 
                    this.getClass().getName(), logOutDir));
        } catch (IOException ioe){
            runnableLogger.severe("Failed to create log with " + 
                    ioe.getMessage());
        } catch (SecurityException se){
            runnableLogger.severe("Failed to create dirs " + se.getMessage());
        }     
    }
    
    @Override
    public void run(){
        if (snapRunnable != null){
            // Set up timestamp and save dirs
            LocalDateTime date = LocalDateTime.now(); // Create a date object
            DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern(
                        "yyyyMMdd-HHmmss");

            String formattedDate = date.format(myFormatObj);
            acqTimestamp = formattedDate;
            String saveDirName = String.format("%s_%s", scanTypeLabel, formattedDate);

            File dataOutRootDir = frame_.getDataFolderDir();//.getAbsolutePath();
            File logOutFile = frame_.getDataFolderDir();
            String logOutDir = logOutFile.getAbsolutePath();

            // make the dirs in a timestamped subdir so it doesn't overwrite
            snapRunnable.dataOutDir = new File(
                    dataOutRootDir, saveDirName).getAbsolutePath();

            createLog(logOutDir);
                        
            try {
                acq_.clearRunnables();
                acq_.attachRunnable(-1, -1, -1, -1, snapRunnable);
                
                // we dont use this 
                // Datastore snapStore = acq_.runAcquisitionNonblocking();
                // not sure what blocking helps with really, we dont need the
                // snap datastore desperately
                Datastore snapStore = acq_.runAcquisition();

                // SummaryMetadata snapMetadata = snapStore.getSummaryMetadata();
                // mdaRunnableLogger.info("snap metadata:" + snapMetadata.toString());

                mdaRunnableLogger.info("Running " + snapRunnable.getClass().getName());
                acq_.clearRunnables();  // remove runnable

                

            } catch (Exception e){  // look into using micromanager exceptions
                mdaRunnableLogger.severe("Failed in volume acquisition:" + 
                        e.getMessage());
            }
        } else {
            mdaRunnableLogger.severe("Runnable is null; wasn't successfully "
                    + "created (is scantype correct?)");
        }
    }
    
}

