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
import mmcorej.Configuration;
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
import org.micromanager.data.Metadata;
import org.micromanager.data.SummaryMetadata;

import org.micromanager.acqj.internal.Engine;
import com.google.common.collect.Lists;
import dopm_mm2.acquisition.MDAListener;

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
    protected final MDAListener currentAcq;
    protected double currentViewAngle;
    
    protected String settingsOutDir;
    protected String dataOutDir;
    protected String acqTimestamp;

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
       
    public AbstractAcquisitionRunnable(dOPM_hostframe frame_ref, 
            MDAListener acqListener) {
        frame_ = frame_ref;
        mm_ = dOPM_hostframe.mm_;
        core_ = mm_.getCMMCore();
        deviceSettings = frame_.getDeviceSettings();
        currentAcq = acqListener;
        
        LocalDateTime date = LocalDateTime.now(); // Create a date object
        DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern(
                    "yyyyMMddhhmmss");

        String formattedDate = date.format(myFormatObj);
        acqTimestamp = formattedDate;
        
        File dataOutRootDir = frame_.getDataFolderDir();//.getAbsolutePath();
        File settingsRootOutDir = 
                    frame_.getSettingsFolderDir();//.getAbsolutePath();
        
        // make the dirs in a timestamped subdir so it doesn't overwrite
        dataOutDir = new File(
                dataOutRootDir, acqTimestamp).getAbsolutePath();
        settingsOutDir = new File(
                settingsRootOutDir, acqTimestamp).getAbsolutePath();
        
        new File(dataOutDir).mkdirs();
        new File(settingsOutDir).mkdirs();
        
        runnableLogger.info("dataOutDir: " + dataOutDir);
        runnableLogger.info("settingsOutDir: " + settingsOutDir);
        
        // create log file
        createLog(settingsRootOutDir.getAbsolutePath());
        
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
        
        XYStagePort = deviceSettings.getXyStageComPort();
        mirrorStagePort = deviceSettings.getMirrorStageComPort();
    }
    
    private void createLog(String logOutDir){
        try { 
            // Just print log for this runnable for debugging directly
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
        runnableLogger.info("In runnable's run()");
        // Create log file                 
        
        // Make camera fast
        try {
            core_.setProperty(camName, "ScanMode", 3);
        } catch (Exception e){
            runnableLogger.severe("Failed to set camera scanmode to fast: " 
                    + e.getMessage());
        }
        
        // Set scan speed variables accordingly for mirror and xystage
        deviceSettings.updateCurrentScanSpeedsDuringAcq();
        
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
                currentAcq.setCurrentView(1);
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
                currentAcq.setCurrentView(2);
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
        
        // Update the MDA indices, IMPORTANT FOR FILE SAVING/METADATA!
        if (currentAcq!=null){  // should never be null, i removed the ability for that
            currentAcq.nextAcqPoint();
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
            throws IOException, Exception{
        double storeStartTime = System.currentTimeMillis();
        Datastore store;
        String fileName;
        
        // get file name based on position in MDA. consider using device 
        // settings to save into filename laser, power, exposure, filter and 
        // then the currentAcq just for time, position, z scan plane (if used)
        if (currentAcq!=null){
            fileName = String.format("dOPM_t%04d_p%04d_z%04d_c%04d_view%d", 
                    currentAcq.getCurrentAcqTimeIdx(),
                    currentAcq.getCurrentAcqPositionIdx(),
                    currentAcq.getCurrentAcqZIdx(),
                    currentAcq.getCurrentAcqChannelIdx(),
                    currentAcq.getCurrentView()
                );    
        } else{
            int i=0;
            while(new File(dataOutDir, 
                    String.format("MMStack_n%04d", i)).exists()){
                i++;
            }
            fileName = (String.format("MMStack_n%04d", i));
        }
        
        String dataSavePath = (new File(dataOutDir, fileName)).getAbsolutePath();
        
        try {
            runnableLogger.info("creating datastore in " + dataSavePath);
            store = FileMM.createDatastore(camName, dataSavePath, true);    
        } catch (IOException ie){
            throw new IOException("Failed to create datastore with "
                    + ie.getMessage());
        } catch (Exception e){
            throw new Exception("Uknown error when creating datastore with "
                    + e.getMessage());
        }
        
        PropertyMap myPropertyMap; 
        try {
            // Get my MDAListener metadata
            // possibly redudant, this was just used to save file

            // retrivePositionLabels() <- USE THIS SOON TODO?
            
            runnableLogger.info("Getting more metadata");
            myPropertyMap = PropertyMaps.builder().
                putDouble("angle", currentViewAngle).
                putString("filter", deviceSettings.getCurrentFilter()).
                putString("laser", deviceSettings.getCurrentLaser()).
                putDouble("power", deviceSettings.getCurrentLaserPower()).
                putDouble("x", startingXPositionUm).
                putDouble("y", startingYPositionUm).
                putDouble("z", startingZpositionUm).
                putInteger("positionIdx", currentAcq.getCurrentAcqPositionIdx()).
                putInteger("channelIdx", currentAcq.getCurrentAcqChannelIdx()).
                putInteger("zIdx", currentAcq.getCurrentAcqZIdx()).
                putInteger("timeIdx", currentAcq.getCurrentAcqTimeIdx()).
                putAll(customPropertyMap).
                    build();
        } catch (Exception e){
            runnableLogger.severe("Failed to create datastore metadata, falling"
                            + " back to summary metadata" + e.getMessage());
            myPropertyMap = PropertyMaps.builder().build();
            
        } 
        SummaryMetadata metaData = mm_.data().summaryMetadataBuilder().
                userData(myPropertyMap).build();
        store.setSummaryMetadata(metaData);
        
        double storeCreationTime = System.currentTimeMillis() - storeStartTime;
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
                
        
        // TODO TO REMOVE
        Metadata generateMetadata = null;

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
                        // System.out.println("TAGS: " + img.tags.toString());
                        
                        // runnableLogger.info("Got tagged image:" + nFrames);
                        Image tmp = mm_.data().convertTaggedImage(img);  // Image 
                        generateMetadata = mm_.acquisitions().generateMetadata(tmp, true);  //TODO REMOVE

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
        if (generateMetadata != null) System.out.println("metadata from generate: " + generateMetadata.toString());
        
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
