/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package dopm_mm2.Runnables;

import dopm_mm2.Devices.DeviceManager;
import dopm_mm2.Devices.TangoXYStage;
import dopm_mm2.GUI.dOPM_hostframe;
import dopm_mm2.util.FileMM;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
import org.micromanager.StagePosition;
import org.micromanager.PositionList;
import org.micromanager.Studio;
import org.micromanager.acquisition.SequenceSettings;
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
    protected StagePosition stagePosition;
    protected PositionList positionList;
    
    protected final String XYStagePort;
    protected final String mirrorStagePort;
    protected final String DAQDOPort;
    
    // starting stage positions:
    protected double startingXPositionUm;
    protected double startingYPositionUm;
    protected double startingZpositionUm;
    protected double startingMirrorPositionUm;
    
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
        
        runnableLogger.info(String.format("Variables: "
                + "camera %s, "
                + "mirror stage; %s, "
                + "xy stage; %s, "
                + "z stage; %s, "
                + "filter wheel; %s, "
                + "DAQ DO port; %s",
                camName, mirrorStage, XYStage, ZStage, filterWheel, DAQDOPort));
        
        List<String> laserLabels = deviceSettings.getLaserLabels();
        List<String> laserDeviceNames = deviceSettings.getLaserDeviceNames();
        
        if (!laserLabels.isEmpty()){
            lasers = laserDeviceNames;
        } else if (!laserDeviceNames.isEmpty()){
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
        runnableLogger.info("In runnable's run()");
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
        
        // Make camera fast
        try {
            core_.setProperty(camName, "ScanMode", 3);
        } catch (Exception e){
            runnableLogger.severe("Failed to set camera scanmode to fast: " 
                    + e.getMessage());
        }
        
        // Get channel info
        try {
            filter = core_.getProperty(filterWheel, "Label");
            int laserState = Integer.parseInt(
                    core_.getProperty(DAQDOPort, "State"));
            int laserIdx = (int)(Math.log(laserState)/Math.log(2));
            runnableLogger.info("laserIdx: " + laserIdx);
            if (lasers.isEmpty()) {
                laser = String.valueOf(laserIdx);
            } else {
                runnableLogger.info("lasers: " + lasers);
                laser = lasers.get(laserIdx);
            }
        } catch (Exception e){
            logErrorWithWindow(String.format(
                    "Failed to get filter and laser info with error %s",
                    e.getMessage()));
        }
        
        // get position info ? how?  
        // deviceSettings.getCurrentChannelIndex();
        // deviceSettings.getCurrenPositionIndex();
        // deviceSettings.getCurrentZIndex();
        
        // Set scan speed variables accordingly for mirror and xystage
        deviceSettings.upateMaxGlobalTriggeredScanSpeed();
        if (deviceSettings.getUseMaxScanSpeedForMirror()){
            deviceSettings.setMirrorStageCurrentScanSpeed(
                    deviceSettings.getMaxTriggeredScanSpeed());
        } else {
            runnableLogger.info("setting mirror scan speed to " + 
                    deviceSettings.getMirrorStageGlobalScanSpeed());
            deviceSettings.setMirrorStageCurrentScanSpeed(
                    deviceSettings.getMirrorStageGlobalScanSpeed());
        }
        
        if (deviceSettings.getUseMaxScanSpeedForXyStage()){
            deviceSettings.setXyStageCurrentScanSpeed(
                    deviceSettings.getMaxTriggeredScanSpeed());
        } else {
            deviceSettings.setXyStageCurrentScanSpeed(
                    deviceSettings.getXyStageGlobalScanSpeed());
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
        // ------- Do view 1 or view 2 (or both), calls runSingleView ------ //
        // // // // // // // // // // // // // // // // // // // // // // // // 
        
        // View 1
        if (deviceSettings.isView1Imaged()){
            // this config should be set automatically if it doesnt exist from 
            // PIViewPositions.txt (TODO)
            try { 
                core_.setConfig("dOPM View", "View 1");
                core_.waitForConfig("dOPM View", "View 1");
            } catch (Exception e){
                runnableLogger.severe("Failed to change to view 1, "
                        + "check config dOPM View exists with preset View 1");
                return;
            }
            // I think hugh would do 0 and 90 instead of -45 and 45, check
            //runOneView(-deviceSettings.getOpmAngle());  
            currentViewAngle = -deviceSettings.getOpmAngle();
            try {
                storeStageStartingPositions();
                runSingleView(currentViewAngle);
            } catch (Exception e){
                logErrorWithWindow(e);
            } finally {
                cleanupAcq();
                setStagePositionsToStart();
            }

        }
        // View 2
        if (deviceSettings.isView2Imaged()){
            try { 
                core_.setConfig("dOPM View", "View 2");
                core_.waitForConfig("dOPM View", "View 2");
            } catch (Exception e){
                runnableLogger.severe("Failed to change to view 2, "
                        + "check config dOPM View exists with preset View 2");
            }
            currentViewAngle = deviceSettings.getOpmAngle();
            try {
                storeStageStartingPositions();
                runSingleView(currentViewAngle);
            } catch (Exception e){
                logErrorWithWindow(e);
            } finally {
                cleanupAcq();
                setStagePositionsToStart();
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

    protected void logErrorWithWindow(Exception e){
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String exceptionString = sw.toString();
        
        runnableLogger.severe(exceptionString);
        
        JOptionPane.showMessageDialog(null, 
                              "Acquisition failed: " + e.getMessage(), 
                              "Acquisition Error", 
                              JOptionPane.ERROR_MESSAGE);
    }
        
    protected void logErrorWithWindow(String msg){
        runnableLogger.severe(msg);
        
        JOptionPane.showMessageDialog(null, 
                              "Acquisition failed: " + msg, 
                              "Acquisition Error", 
                              JOptionPane.ERROR_MESSAGE);
        
    }
    protected void storeStageStartingPositions() throws Exception{
        // Get start positions
        try {
            startingXPositionUm = core_.getXPosition(XYStage);
            startingYPositionUm = core_.getYPosition(XYStage);
            if (!ZStage.equals("")) startingZpositionUm = core_.getPosition(ZStage);
            startingMirrorPositionUm = core_.getPosition(mirrorStage);  // um
        } catch (Exception e){
            runnableLogger.severe(String.format(
                    "Failed to get starting stage positions with %s",
                    e.getMessage()));
            throw e;
        }
    }
    
    // perhaps a little redundant atm
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
     */
    protected void setStagePositionsToStart(){
        try {
            TangoXYStage.setTangoAxisSpeed(
                    XYStage, deviceSettings.getXyStageTravelSpeed());

            core_.setProperty(mirrorStage, "Velocity", 100);
            core_.setXYPosition(XYStage, startingXPositionUm, startingYPositionUm);
            if (!ZStage.equals("")) core_.setPosition(ZStage, startingZpositionUm);
            core_.setPosition(mirrorStage, startingMirrorPositionUm);
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
    
    /** Create datastore for acquisition using the supplied data save path,
     * filename will be MMStack.
     * @param customPropertyMap property map to be created in runSingleView,
     *   use PropertyMaps.builder to build the property map that has e.g. 
     *   scan length, scan type, trigger distance
     * @return the empty datastore with metadata
     * @throws IOException if datastore creation fails (in FileMM)
     */
    protected Datastore createDatastore(PropertyMap customPropertyMap) 
            throws IOException{
        double storeStartTime = System.currentTimeMillis();
        Datastore store;
        String dataSavePath = (new File(dataOutDir, "MMStack")).getAbsolutePath();
        try {
            store = FileMM.createDatastore(camName, dataSavePath, true);                
            // Add view angle to MM property map for datastore metadata
            PropertyMap myPropertyMap = PropertyMaps.builder().
                putString("scan type", "stage scanning").
                putDouble("angle", currentViewAngle).
                putString("filter", filter).
                putString("laser", laser).
                putDouble("x", startingXPositionUm).
                putDouble("y", startingYPositionUm).
                putDouble("z", startingZpositionUm).
                putAll(customPropertyMap).
                    build();

            SummaryMetadata metaData = mm_.data().summaryMetadataBuilder().
                    userData(myPropertyMap).build();
            store.setSummaryMetadata(metaData);
        } catch (IOException ie){
            throw new IOException("Failed to create datastore in " + dataSavePath +
                    " with " + ie.getMessage());
        }
        double storeCreationTime = System.currentTimeMillis()-storeStartTime;
        runnableLogger.info(String.format("Datastore creation time: %.2f ms", 
                storeCreationTime));
        return store;
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
                                " frames dropped in triggered acquisition, "
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
