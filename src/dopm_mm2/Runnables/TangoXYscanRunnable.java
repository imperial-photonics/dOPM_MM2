/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dopm_mm2.Runnables;

import dopm_mm2.Devices.DeviceManager;
import dopm_mm2.Devices.PIStage;
import dopm_mm2.GUI.dOPM_hostframe;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import mmcorej.CMMCore;
import org.micromanager.ScriptController;
import org.micromanager.Studio;
import org.micromanager.display.DisplayManager;
import org.micromanager.display.DisplayWindow;

/**
 *
 * @author OPMuser
 */
public class TangoXYscanRunnable implements runnable {
    private final dOPM_hostframe frame_;
    private final CMMCore core_;
    private final Studio mm_;
    private DisplayManager displayManager;
    private int PIDeviceID;
    private static final Logger tangoRunnableLogger = 
    Logger.getLogger(PIScanRunnable.class.getName());
    
    private ScriptController sc;

    private DeviceManager deviceSettings;
    private PIStage control;
    private DisplayWindow display;
    
    private String camName;
    private String mirrorStage;
    private String XYStage;
    private String ZStage;
    
    private double scanLengthMillim;
    private double scanSpeed;
    private double trigDistMillim;
    private double exposure;
    
    // Set serial command
    private String port;
    private String commandTerminator = "\n"; 
    
    private String settingsOutDir;
    private String dataOutDir;
    private String baseDir;
    private String dataSavePath;
    private FileHandler fh;
    
    boolean saveImgToDisk;

    public TangoXYscanRunnable(dOPM_hostframe frame_ref) {
        tangoRunnableLogger.info("Entered TangoXYscanRunnable runnable");
        frame_ = frame_ref;
        mm_ = dOPM_hostframe.mm_;
        core_ = mm_.getCMMCore();
        deviceSettings = frame_.getDeviceSettings();
        PIDeviceID = 1;
        
        saveImgToDisk = frame_.isSaveImgToDisk();
        
        settingsOutDir = frame_.getSettingsFolderDir().getAbsolutePath();
        dataOutDir = frame_.getDataFolderDir().getAbsolutePath();
        baseDir = frame_.getBaseFolderDir().getAbsolutePath();
        
        tangoRunnableLogger.info("dataOutDir: " + dataOutDir);
        tangoRunnableLogger.info("baseDir: " + baseDir);
        tangoRunnableLogger.info("settingsOutDir: " + settingsOutDir);
        
        // FileWriter logFile = new FileWriter(settingsOutDir + "log.txt");
        // BufferedWriter runLog = new BufferedWriter(logFile);
                
        sc = mm_.getScriptController();
        
        camName = deviceSettings.getLeftCameraName();
        mirrorStage = deviceSettings.getMirrorStageName();
        XYStage = deviceSettings.getXyStageName();
        ZStage = deviceSettings.getZStageName();
        
        // Set serial command
        port = deviceSettings.getXyStageComPort();
        tangoRunnableLogger.info("For Tango, using port: " + port);
        commandTerminator = "\n"; 
        
        scanLengthMillim = deviceSettings.getXyStageScanLength()*1e-3;  // target scan end in mm
        scanSpeed = deviceSettings.getXyStageScanSpeed();  // scan speed in mm/s or um/ms
        trigDistMillim = deviceSettings.getXyTriggerDistance()*1e-3;  // trigger distance in um
        exposure = deviceSettings.getExposureTime();
        
        tangoRunnableLogger.info(String.format("Got device settings from hostframe: "
                + "scan length[mm] %.5f; "
                + "scanSspeed[mm/s] %.5f; "
                + "trigger distance[mm] %.5f;"
                + "exposure time[ms] %.5f", 
                scanLengthMillim, scanSpeed, trigDistMillim, exposure));
    }
}