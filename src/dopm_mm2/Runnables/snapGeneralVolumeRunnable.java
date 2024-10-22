/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dopm_mm2.Runnables;

import dopm_mm2.GUI.dOPM_hostframe;
import java.util.logging.Logger;
import mmcorej.CMMCore;
import org.micromanager.Studio;
import org.micromanager.acquisition.AcquisitionManager;
import org.micromanager.data.Datastore;
import org.micromanager.data.SummaryMetadata;


/**
 *
 * @author lnr19
 */
public class snapGeneralVolumeRunnable implements Runnable {
    private final CMMCore core_;
    private final Studio mm_;
    private static final Logger mdaRunnableLogger = 
            Logger.getLogger(PIScanRunnable.class.getName());
    private AcquisitionManager acq_;
    private Runnable snapRunnable;


    public snapGeneralVolumeRunnable(dOPM_hostframe frame_ref, int scanType) {
        this.core_ = frame_ref.core_;
        this.mm_ = frame_ref.mm_;
        acq_ = mm_.getAcquisitionManager();
        
        
        switch(scanType){
            case 0:
                snapRunnable = new PIScanRunnableInherited(frame_ref);
                break;
            case 1:
                snapRunnable = new TangoXYscanRunnableInherited(frame_ref);
                break;
            case 2:
                snapRunnable = new TangoXYscanRunnableInherited(frame_ref);
            default:
                mdaRunnableLogger.severe("Unknown volume scantype");
            
        }
    }
    
    @Override
    public void run(){
        if (snapRunnable != null){
            try {
                acq_.clearRunnables();
                acq_.attachRunnable(-1, -1, -1, -1, snapRunnable);
                Datastore snapStore = acq_.runAcquisitionNonblocking();
                
                SummaryMetadata snapMetadata = snapStore.getSummaryMetadata();
                mdaRunnableLogger.info("snap metadata:" + snapMetadata.toString());

                mdaRunnableLogger.info("Running " + snapRunnable.getClass().getName());

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

