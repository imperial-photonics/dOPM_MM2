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
public class PIvelocityBugTestRunnable implements Runnable {
    
    private final CMMCore core_;
    private final Studio mm_;
    private DisplayManager displayManager;
    private static final Logger pitestRunnableLogger = 
            Logger.getLogger(PIvelocityBugTestRunnable.class.getName());
    private DisplayWindow display;
    private Datastore store;
    private String port;
    private int PIDeviceID;


    public PIvelocityBugTestRunnable(CMMCore core_, Studio mm_) {
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
                    logdir, String.format("log_%s.txt", formattedDate.toString())).getAbsolutePath());
            
            
            pitestRunnableLogger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();  
            fh.setFormatter(formatter);  
            
            String errans;
            
            pitestRunnableLogger.info("studio object: " + mm_);
            pitestRunnableLogger.info("core object: " + core_);
            
            // check isn't zero like the PI device adapter likes to do
            currentScanSpeed = core_.getProperty(mirrorStage, "Velocity");
            pitestRunnableLogger.info("Initial currentScanSpeed " + currentScanSpeed);
            
            /*
            PIStage.setupPITriggering(port, PIDeviceID);

            // check isn't zero like the PI device adapter likes to do
            currentScanSpeed = core_.getProperty(mirrorStage, "Velocity");
            pitestRunnableLogger.info("currentScanSpeed after setupPITriggering " + currentScanSpeed);
            
            PIStage.stopPIStage(port);  // need this command so PI stage plays ball
            PIStage.setPITriggerDistance(port, PIDeviceID, 0.001);
            PIStage.setPITriggerRange(port, PIDeviceID, new double[]{0, 0.1});
            
            // check isn't zero like the PI device adapter likes to do
            currentScanSpeed = core_.getProperty(mirrorStage, "Velocity");
            pitestRunnableLogger.info("currentScanSpeed after PI stop, trig dist, trig range " + currentScanSpeed);
            */
            boolean checkmotion = PIStage.checkPIMotion(port);
            pitestRunnableLogger.info("motion? " + checkmotion);
            
            // check if readout is waiting
            String test;
            try {
                test = core_.getSerialPortAnswer(port, "\n");
                pitestRunnableLogger.info("there was an output waiting to be read!! "+ test);
            } catch (Exception e){
                
            }
            currentScanSpeed = core_.getProperty(mirrorStage, "Velocity");

            pitestRunnableLogger.info("Scan speed after check motion " + currentScanSpeed);
            /*
            core_.setSerialPortCommand(port, "VEL 1 100", "\n");
            core_.setSerialPortCommand(port, "ERR?", "\n");
            errans = core_.getSerialPortAnswer(port, "\n");            
            currentScanSpeed = core_.getProperty(mirrorStage, "Velocity");
            pitestRunnableLogger.info("Scan speed after set '100' with VEL: " + currentScanSpeed); 
            */
            try {
                test = core_.getSerialPortAnswer(port, "\n");
                pitestRunnableLogger.info("there was an output waiting to be read!! after get speed "+ test);
            } catch (Exception e){
                
            }
            
            core_.setProperty(mirrorStage, "Velocity", 100);
            currentScanSpeed = core_.getProperty(mirrorStage, "Velocity");
            pitestRunnableLogger.info("Scan speed after set '100': " + currentScanSpeed);  

            
            core_.setSerialPortCommand(port, "VEL 1 0.01", "\n");
            core_.setSerialPortCommand(port, "ERR?", "\n");
            errans = core_.getSerialPortAnswer(port, "\n");

            currentScanSpeed = core_.getProperty(mirrorStage, "Velocity");
            pitestRunnableLogger.info("Scan speed after set 0.01 with VEL: " + currentScanSpeed);
            
            core_.setProperty(mirrorStage, "Velocity", 10.0);
            currentScanSpeed = core_.getProperty(mirrorStage, "Velocity");
            pitestRunnableLogger.info("Scan speed after set '10.0': " + currentScanSpeed);  

            core_.setProperty(mirrorStage, "Velocity", 1.0);
            currentScanSpeed = core_.getProperty(mirrorStage, "Velocity");
            pitestRunnableLogger.info("Scan speed after set '1.0': " + currentScanSpeed);   
            
            core_.setProperty(mirrorStage, "Velocity", 1);
            currentScanSpeed = core_.getProperty(mirrorStage, "Velocity");
            pitestRunnableLogger.info("Scan speed after set '1' (int): " + currentScanSpeed);  
            
            core_.setProperty(mirrorStage, "Velocity", 0.9);
            currentScanSpeed = core_.getProperty(mirrorStage, "Velocity");
            pitestRunnableLogger.info("Scan speed after set '0.9': " + currentScanSpeed);
            
            core_.setProperty(mirrorStage, "Velocity", 0.1);
            currentScanSpeed = core_.getProperty(mirrorStage, "Velocity");
            pitestRunnableLogger.info("Scan speed after set '0.1': " + currentScanSpeed);
            
            core_.setProperty(mirrorStage, "Velocity", "1e-2");
            currentScanSpeed = core_.getProperty(mirrorStage, "Velocity");
            pitestRunnableLogger.info("Scan speed after set '1e-2': " + currentScanSpeed);
            
            core_.setProperty(mirrorStage, "Velocity", 1e-2);
            currentScanSpeed = core_.getProperty(mirrorStage, "Velocity");
            pitestRunnableLogger.info("Scan speed after set 1e-2: " + currentScanSpeed);
            
            core_.setProperty(mirrorStage, "Velocity", 1e-2);
            currentScanSpeed = core_.getProperty(mirrorStage, "Velocity");
            pitestRunnableLogger.info("Scan speed after set 0.01: " + currentScanSpeed);
            
            // check actual serial
            core_.setSerialPortCommand(port, "VEL? 1", "\n");
            String velans = core_.getSerialPortAnswer(port, "\n");
            pitestRunnableLogger.info("Scan speed from VEL? after set 0.01: " + velans);

            

            
        } catch (Exception e){
            pitestRunnableLogger.severe(e.getMessage());
        }
    }
    
}
