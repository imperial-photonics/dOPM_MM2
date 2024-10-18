/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package dopm_mm2.Runnables;

import dopm_mm2.Devices.DeviceManager;
import dopm_mm2.GUI.dOPM_hostframe;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javax.swing.JOptionPane;
import mmcorej.CMMCore;
import mmcorej.TaggedImage;
import org.micromanager.PropertyMap;
import org.micromanager.PropertyMaps;
import org.micromanager.Studio;
import org.micromanager.data.Coords;
import org.micromanager.data.Datastore;
import org.micromanager.data.Image;
import org.micromanager.data.SummaryMetadata;

/** Abstract class for dOPM runnables, switching between views and calling 
 * runSingleView for each view.
 * When writing a new dOPM runnable that extends this abstract class, 
 * runSingleView is overloaded and you shouldn't need to do anything 
 * with run itself
 * 
 * @author Leo Rowe-Brown
 */
public abstract class AbstractAcquisitionRunnable implements Runnable {
    
    protected final dOPM_hostframe frame_;
    protected final CMMCore core_;
    protected final Studio mm_;
    protected final DeviceManager deviceSettings;
    protected double currentViewAngle;
    
    protected String settingsOutDir;
    protected String dataOutDir;

    protected final String camName;
    protected final String mirrorStage;
    protected final String XYStage;
    protected final String ZStage;
    protected final String filterWheel;
    protected String filter;
    protected String laser;
    protected List<String> lasers;
    
    protected final String XYStagePort;
    protected final String mirrorStagePort;
    protected final String DAQDOPort;
    
    // starting stage positions:
    protected double startingXPosition;
    protected double startingYPosition;
    protected double startingMirrorPosition;
    
    protected double volumeScanLength;
    
    protected static final Logger runnableLogger = 
        Logger.getLogger(PIScanRunnable.class.getName());
       
    public AbstractAcquisitionRunnable(dOPM_hostframe frame_ref) {
        frame_ = frame_ref;
        mm_ = dOPM_hostframe.mm_;
        core_ = mm_.getCMMCore();
        deviceSettings = frame_.getDeviceSettings();
        dataOutDir = frame_.getDataFolderDir().getAbsolutePath();
        settingsOutDir = 
                    frame_.getSettingsFolderDir().getAbsolutePath();
        
        // device variables
        camName = deviceSettings.getLeftCameraName();
        mirrorStage = deviceSettings.getMirrorStageName();
        XYStage = deviceSettings.getXyStageName();
        ZStage = deviceSettings.getZStageName();
        filterWheel = deviceSettings.getFilterDeviceName();
        DAQDOPort = deviceSettings.getLaserBlankingDOport();
        
        List<String> laserLabels = deviceSettings.getLaserLabels();
        List<String> laserDeviceNames = deviceSettings.getLaserDeviceNames();
        
        if (laserLabels.isEmpty()){
            lasers = laserDeviceNames;
        } else if (laserDeviceNames.isEmpty()){
            lasers = laserLabels;
        } else {
            runnableLogger.warning(
                    "Laser labels and laser device names are both empty! "
                    + "Metadata for laser will just be an index");
        }
        
        XYStagePort = deviceSettings.getXyStageComPort();
        mirrorStagePort = deviceSettings.getMirrorStageComPort();
    }
    
    @Override
    public void run(){
        // Create log file
        try { 
            LocalDateTime date = LocalDateTime.now(); // Create a date object
            DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern(
                    "yyyyMMddhhmmss");

            String formattedDate = date.format(myFormatObj);

            // Just print log for this runnable for debugging directly
            FileHandler fh = new FileHandler(new File(
                    settingsOutDir, 
                    String.format("acqLog%s.txt", formattedDate)).toString());
 
            runnableLogger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();  
            fh.setFormatter(formatter);  
            runnableLogger.info(String.format("Started log for %s at %s", 
                    this.getClass().getName(), formattedDate));
        } catch (IOException ioe){
            runnableLogger.severe("Failed to create log with " + 
                    ioe.getMessage());
        } catch (SecurityException se){
            runnableLogger.severe("Failed to create dirs " + se.getMessage());
        }     
        
        // Get start positions
        try {
            startingXPosition = core_.getXPosition();
            startingYPosition = core_.getYPosition();
            startingMirrorPosition = core_.getPosition(mirrorStage)*1e-3;
        } catch (Exception e){
            runnableLogger.severe(String.format(
                    "Failed to get starting stage positions with %s",
                    e.getMessage()));
        }
        
        // Get channel info
        try {
            filter = core_.getProperty(filterWheel, "Label");
            int laserState = Integer.parseInt(
                    core_.getProperty(DAQDOPort, "State"));
            int laserIdx = (int)(Math.log(laserState)/Math.log(2));
            if (lasers.isEmpty()) {
                laser = String.valueOf(laserIdx);
            } else {
                laser = lasers.get(laserIdx);
            }
        } catch (Exception e){
            logErrorWithWindow(String.format(
                    "Failed to get filter and laser info with error %s",
                    e.getMessage()));
            
        }
        // Enable external triggering if applicable, 
        // maybe move this for readability?
        if (deviceSettings.getTriggerMode() != 2){
            try {
                setupCameraTriggering();
            } catch (Exception e){
                logErrorWithWindow(String.format(
                    "Failed to set camera trigger settings and enable external"
                    + "triggering with error %s",
                    e.getMessage()));
            }
        }
        
        // // // // // // // // // // // // // // // // // // // // // // // // 
        // ACQUISITION SECTION --------------------------------------------- //
        // --------Do view 1 or view 2 (or both), calls runSingleView------- //
        // // // // // // // // // // // // // // // // // // // // // // // // 
        
        // View 1
        if (deviceSettings.isView1Imaged()){
            // this config should be set automatically if it doesnt exist from 
            // PIViewPositions.txt (TODO)
            try { 
                core_.setConfig("dOPM View", "View 1");
            } catch (Exception e){
                runnableLogger.severe("Failed to change to view 1, "
                        + "check config dOPM View exists with preset View 1");
                return;
            }
            // I think hugh would do 0 and 90 instead of -45 and 45, check
            //runOneView(-deviceSettings.getOpmAngle());  
            currentViewAngle = -deviceSettings.getOpmAngle();
            try {
                runSingleView(currentViewAngle);
            } catch (Exception e){
                logErrorWithWindow(e.getMessage());
            } finally {
                cleanupAcq();
                resetStagePositionsToStart("View 1");
            }

        }
        // View 2
        if (deviceSettings.isView2Imaged()){
            try { 
                core_.setConfig("dOPM View", "View 2");
            } catch (Exception e){
                runnableLogger.severe("Failed to change to view 2, "
                        + "check config dOPM View exists with preset View 2");
            }
            currentViewAngle = deviceSettings.getOpmAngle();
            try {
                runSingleView(currentViewAngle);
            } catch (Exception e){
                logErrorWithWindow(e.getMessage());
            } finally {
                cleanupAcq();
                resetStagePositionsToStart("View 2");
            }
        }
        
        // // // // // // // // // // // // // // // // // // // // // // // // 
        
        try {
            stopCameraTriggering();
        } catch (Exception e){
            logErrorWithWindow("Failed to switch camera to internal triggering "
                    + "with " + e.getMessage());
        }
    }
    
    public void runSingleView(double currentViewAngle) throws Exception{
        // MAIN BODY OF CODE GOES HERE, use @Override
    }
    
    protected void logErrorWithWindow(String msg){
        runnableLogger.severe(msg);
        
        JOptionPane.showMessageDialog(null, 
                              "Acquisition failed: " + msg, 
                              "Acquisition Error", 
                              JOptionPane.ERROR_MESSAGE);
        
    }
    
    protected void cleanupAcq(){
        runnableLogger.info("Cleaning up acquisition, lasers -> off");
        try {
            core_.setProperty(DAQDOPort, "State", 0);
            core_.setProperty(DAQDOPort, "Blanking", "Off");
        } catch (Exception e){
            logErrorWithWindow("Failed to switch off lasers and blanking via DAQ");
        }

        try {
            double stopAcqStart = System.currentTimeMillis();
            if (core_.isSequenceRunning()){
                core_.stopSequenceAcquisition();
                logErrorWithWindow(String.format(
                        "Stopped previous acquisition in %.1f ms",
                        (System.currentTimeMillis() - stopAcqStart) ));
            }
        } catch (Exception e){
                
        }
    }
    
    /** Reset stage positions and change them to the travel speed (fast)
     * 
     * @param viewPreset 
     */
    protected void resetStagePositionsToStart(String viewPreset){
        try {
            core_.setProperty(XYStage, "Velocity", 
                    deviceSettings.getXyStageTravelSpeed());
            core_.setProperty(mirrorStage, "Velocity", 100);
            core_.setXYPosition(
                XYStage, startingXPosition, startingYPosition);
            core_.setConfig("dOPM View", viewPreset);
        } catch (Exception e){
            logErrorWithWindow("Failed to reset stage "
                    + "positions after acquisition with " + 
                    e.getMessage());
        }
    }
    
    protected void setupCameraTriggering() throws Exception{
        core_.setProperty(camName, "TriggerPolarity","POSITIVE");
        core_.setProperty(camName, "TRIGGER SOURCE","EXTERNAL");
        core_.setProperty(camName, "OUTPUT TRIGGER KIND[0]","EXPOSURE");
        core_.setProperty(camName, "OUTPUT TRIGGER POLARITY[0]","POSITIVE");
        core_.setProperty(camName, "OUTPUT TRIGGER SOURCE[0]","TRIGGER");
        core_.setProperty(camName, "TRIGGER GLOBAL EXPOSURE","GLOBAL RESET");
    }
    
    /** Switches back to internal triggering: A bit pointless but just to keep 
     * everything consistent */
    protected void stopCameraTriggering() throws Exception{
        core_.setProperty(camName, "TRIGGER SOURCE","INTERNAL");
    }
    
    protected Datastore acquireTriggeredDataset(Datastore store, double scanEnd, int nFramesTotal)
            throws Exception {
        // Coords.Builder cb = Coordinates.builder();
        boolean timeout = false;
        double acqTimeStart = System.currentTimeMillis();
    
        // Coords.Builder cb = mm_.data().coordsBuilder().z(0).channel(0).stagePosition(0);
        Coords.Builder cb = mm_.data().coordsBuilder().z(0);

        boolean grabbed = false;
        int nFrames = 0;

        double frameTimeTotal = 0;
        int frameTimeout = 2000; // if no frame received for 2s, time out


        while (nFrames < nFramesTotal && !timeout){
 
                double tic=System.currentTimeMillis();
                double toc=tic;

                grabbed = false;
                while(toc-tic < frameTimeout && !grabbed){
                        // wait for an image in the circular buffer
                        if (core_.getRemainingImageCount() > 0){
                            TaggedImage img = core_.popNextTaggedImage();	// TaggedImage
                            // runnableLogger.info("Got tagged image:" + nFrames);
                            Image tmp = mm_.data().convertTaggedImage(img);  // Image 
                            // does this copy in memory? inefficient?
                            Image cbImg = tmp.copyAtCoords(cb.z(nFrames).build());
                            store.putImage(cbImg);
                            grabbed = true;
                            nFrames++;
                        }
                        toc = System.currentTimeMillis(); 
                }
                if (toc-tic >= frameTimeout){
                        runnableLogger.severe(String.format(
                                "%d FRAMES DROPPED", (nFramesTotal-nFrames)));
                        timeout = true;  // actually redundant
                        if (nFrames==0){
                            throw new TimeoutException("No frames acquired in triggered "
                                + "acquisition. Check hardware and wiring");
                        } else {
                            throw new TimeoutException((nFramesTotal-nFrames) + 
                                    "frames dropped in triggered acquisition,"
                                    + "check camera speed settings, trigger "
                                    + "distance, exposure, scan speed");
                        }
                }
                frameTimeTotal += (toc-tic);
        }
        
        double acqTimeStop = System.currentTimeMillis() - acqTimeStart; 
        
        runnableLogger.info(String.format("Frames acquired: %s (%d dropped)", 
                nFrames, (nFramesTotal-nFrames)));
        runnableLogger.info(String.format("Actual effective FPS: %.2f", 
                1e3*nFrames/frameTimeTotal));

        runnableLogger.info(String.format("Time in frame grabbing loop %.1f ms", 
                frameTimeTotal));
        runnableLogger.info(String.format("Total time in "
                + "acquireTriggeredDataset %.1f ms", acqTimeStop));

        return store;
    }
}
