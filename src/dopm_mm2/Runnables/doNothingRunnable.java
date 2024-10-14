/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dopm_mm2.Runnables;

import java.util.logging.Logger;

/**
 *
 * @author OPMuser
 */
public class doNothingRunnable implements Runnable{
    
    private static final Logger testNothingRunnableLogger = 
        Logger.getLogger(PIScanRunnable.class.getName());
    
    public doNothingRunnable(){}
    
    @Override
    public void run(){
        testNothingRunnableLogger.info("Literally do nothing runnable");
    }
}
