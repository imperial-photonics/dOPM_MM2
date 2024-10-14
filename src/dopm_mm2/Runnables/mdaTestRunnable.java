/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dopm_mm2.Runnables;

import java.util.logging.Logger;
import mmcorej.CMMCore;
import org.micromanager.Studio;
import org.micromanager.acquisition.AcquisitionManager;
import org.micromanager.data.Datastore;
import org.micromanager.data.Image;
import org.micromanager.display.DisplayManager;
import org.micromanager.display.DisplayWindow;
import dopm_mm2.Runnables.doNothingRunnable;


/**
 *
 * @author lnr19
 */
public class mdaTestRunnable implements Runnable {
    private final CMMCore core_;
    private final Studio mm_;
    private DisplayManager displayManager;
    private static final Logger testRunnableLogger = 
            Logger.getLogger(PIScanRunnable.class.getName());
    private DisplayWindow display;
    private Datastore storeMDA;
    private AcquisitionManager acq_;
    private doNothingRunnable testRunnable = new doNothingRunnable();


    public mdaTestRunnable(CMMCore core_, Studio mm_) {
        this.core_ = core_;
        this.mm_ = mm_;
        acq_ = mm_.getAcquisitionManager();


    }
    
    @Override
    public void run(){
        testRunnableLogger.info("In test mda runnable");
        try {
            
            acq_.clearRunnables();
            acq_.attachRunnable(-1, -1, -1, -1, testRunnable);
            storeMDA = acq_.runAcquisitionNonblocking();

            testRunnableLogger.info("datastore " + storeMDA.getName());

            
        } catch (Exception e){
            testRunnableLogger.severe(e.getMessage());
        }
    }
    
}

