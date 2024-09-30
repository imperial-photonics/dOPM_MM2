/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dopm_mm2.Runnables;

import dopm_mm2.Devices.PIStage;
import java.io.File;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import mmcorej.CMMCore;
import org.micromanager.Studio;
import org.micromanager.data.Datastore;
import org.micromanager.display.DisplayManager;
import org.micromanager.display.DisplayWindow;
import java.time.*; // import the LocalDate class
import java.time.format.DateTimeFormatter;


/**
 *
 * @author OPMuser
 */
public class PITriggerTest implements Runnable {
    
    private final CMMCore core_;
    private final Studio mm_;
    private DisplayManager displayManager;
    private static final Logger pitestRunnableLogger = 
            Logger.getLogger(PIvelocityBugTestRunnable.class.getName());
    private DisplayWindow display;
    private Datastore store;
    private String port;
    private int PIDeviceID;


    public PITriggerTest(CMMCore core_, Studio mm_) {
        this.core_ = core_;
        this.mm_ = mm_;
        PIDeviceID = 1;
        port = "COM3";
    }
    
    @Override
    public void run(){
        String currentScanSpeed;
        String mirrorStage = "PIZStage";

        pitestRunnableLogger.info("In test snap runnable");
        try {
            LocalDateTime date = LocalDateTime.now(); // Create a date object
            DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("dd-MM-yyyy_HH-mm-ss");

            String formattedDate = date.format(myFormatObj);
            
            String logdir = "C:\\Users\\CRICKOPMuser\\Documents\\Leo\\micromanager\\testsettings";
            new File(logdir).mkdirs();
            FileHandler fh = new FileHandler(new File(
                    logdir, String.format("trigger_log_%s.txt", formattedDate.toString())).getAbsolutePath());
            
            
            pitestRunnableLogger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();  
            fh.setFormatter(formatter);  
            
            String errans;
            
            pitestRunnableLogger.info("studio object: " + mm_);
            pitestRunnableLogger.info("core object: " + core_);
            
            core_.setProperty(mirrorStage, "Velocity", 100);
            
            // check isn't zero like the PI device adapter likes to do
            currentScanSpeed = core_.getProperty(mirrorStage, "Velocity");
            pitestRunnableLogger.info("Initial currentScanSpeed " + currentScanSpeed);
            
            
            PIStage.setupPITriggering(port, PIDeviceID);

            // check isn't zero like the PI device adapter likes to do
            currentScanSpeed = core_.getProperty(mirrorStage, "Velocity");
            pitestRunnableLogger.info("currentScanSpeed after setupPITriggering " + currentScanSpeed);
            
            PIStage.stopPIStage(port);  // need this command so PI stage plays ball
            PIStage.setPITriggerDistance(port, PIDeviceID, 0.001);
            PIStage.setPITriggerRange(port, PIDeviceID, new double[]{0, 0.5});
            PIStage.setPositionMillim(mirrorStage, -0.1);
            // check isn't zero like the PI device adapter likes to do
            currentScanSpeed = core_.getProperty(mirrorStage, "Velocity");
            pitestRunnableLogger.info("currentScanSpeed after PI stop, trig dist, trig range " + currentScanSpeed);
            
            boolean checkmotion;
            do{
                checkmotion = PIStage.checkPIMotion(port);
                Thread.sleep(100);
            } while (checkmotion);
            
            pitestRunnableLogger.info("motion? " + checkmotion);
            
            core_.setProperty(mirrorStage, "Velocity", 0.01);
            
            currentScanSpeed = core_.getProperty(mirrorStage, "Velocity");

            pitestRunnableLogger.info("Scan speed after check motion " + currentScanSpeed);
            //trigger on
            PIStage.setPITriggerEnable(port, 1);
            PIStage.setPositionMillim(mirrorStage, 0.5);
            /*
            core_.setSerialPortCommand(port, "VEL 1 100", "\n");
            core_.setSerialPortCommand(port, "ERR?", "\n");
            errans = core_.getSerialPortAnswer(port, "\n");            
            currentScanSpeed = core_.getProperty(mirrorStage, "Velocity");
            pitestRunnableLogger.info("Scan speed after set '100' with VEL: " + currentScanSpeed); 
            */
            PIStage.setPITriggerEnable(port, 0);

            
        } catch (Exception e){
            pitestRunnableLogger.severe(e.getMessage());
        }
    }
    
}
