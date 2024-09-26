/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dopm_mm2.Runnables;

import java.util.logging.Logger;
import mmcorej.CMMCore;
import org.micromanager.display.DisplayManager;
import org.micromanager.Studio;
import org.micromanager.data.Datastore;
import org.micromanager.data.Image;
import org.micromanager.display.DisplayWindow;


/**
 *
 * @author lnr19
 */
public class snapTestRunnable implements Runnable {
    private final CMMCore core_;
    private final Studio mm_;
    private DisplayManager displayManager;
    private static final Logger testRunnableLogger = 
            Logger.getLogger(PIScanRunnable.class.getName());
    private DisplayWindow display;
    private Datastore store;


    public snapTestRunnable(CMMCore core_, Studio mm_) {
        this.core_ = core_;
        this.mm_ = mm_;
    }
    
    @Override
    public void run(){
        testRunnableLogger.info("In test snap runnable");
        try {
            testRunnableLogger.info("studio object: " + mm_);
            testRunnableLogger.info("core object: " + core_);

            store = mm_.data().createRAMDatastore();
            display = mm_.displays().createDisplay(store);
            String camdev = core_.getCameraDevice();
            testRunnableLogger.info("snap with demo cam " + camdev);
            mm_.live().snap(false);
            testRunnableLogger.info("snapped without assigning");
            Image img = mm_.live().snap(false).get(0);
            mm_.live().displayImage(img);
            testRunnableLogger.info("snapped with assigning");
            img = mm_.live().snap(true).get(0);
            testRunnableLogger.info("snapped with showing");


            
        } catch (Exception e){
            testRunnableLogger.severe(e.getMessage());
        }
    }
    
}
