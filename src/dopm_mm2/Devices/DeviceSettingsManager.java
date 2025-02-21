/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dopm_mm2.Devices;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.beans.XMLEncoder;
import java.util.*;
import java.util.logging.Logger;
import java.awt.Rectangle;
import javax.swing.JOptionPane;
import mmcorej.CMMCore;
import mmcorej.StrVector;
import dopm_mm2.util.MMStudioInstance;
import dopm_mm2.util.ConfigParser;
import mmcorej.Configuration;
import mmcorej.PropertySetting;
import org.micromanager.Studio;
import org.micromanager.acquisition.AcquisitionManager;
import org.micromanager.acquisition.ChannelSpec;
import org.micromanager.acquisition.SequenceSettings;

/** DeviceSettingsManager: class for handling device names in microManager configuration
 *
 * @author lnr19
 */
public class DeviceSettingsManager {
    private static final Logger deviceManagerLogger = Logger.getLogger(DeviceSettingsManager.class.getName()); 

    public final static int MIRROR_SCAN = 0;
    public final static int YSTAGE_SCAN = 1;
    public final static int XSTAGE_SCAN = 2;
    
    private List<String> laserDeviceNames;
    private List<String> laserLabels;  // mostly if we decide to do AO control
    private String laserBlankingDOport;  // digital out port name (port0 here)
    private List<String> laserBlankingDOLines;
    private List<String> laserPowerAOports;  // analog out names (a0-3 here)
    
    private String filterDeviceName;
    // private String XYStageDeviceName;
    // private String ZStageDeviceName;
    // private String MirrorStageDeviceName;
    private String dOPMCameraName;
    private StrVector deviceList;

    // Device settings, states, etc.
    private List<String> laserChannelsAcq;
    private List<String> laserPowersAcq;
    private List<String> filtersAcq;

    private List<Object> laserDAQdevices;
    // Trigger settings
    private int scanType = MIRROR_SCAN;
    
    private double xyStageScanLength;  // um
    private double xyStageTriggerDistance;  // um
    private String xyStageScanAxis;  // "x" or "y"
    private boolean useMaxScanSpeedForXyStage = false;
    
    private double mirrorScanLength;  // um
    private double mirrorTriggerDistance;  // um
    private int triggerMode;  // camera setting for triggering (see triggerModeStrings)
    private String[] triggerModeStrings;
    private boolean useMaxScanSpeedForMirror = false;
    
    private double scanSpeedSafetyFactorMirror;
    private double scanSpeedSafetyFactorXy;
    
    //-- the following consider the camera exposure time and thus readout time to 
    // calculate the max possible stage scanning speed for triggered acqs --//
    
    // max possible mirror scan speed for current channel
    private double maxTriggeredMirrorScanSpeed;   
    // max possible xy stage scan speed for current channel
    private double maxTriggeredXyScanSpeed;
    
    // max possible mirror scan speed considering all the exposure times in acq
    // i.e., the lowest of all scan speeds for each channel
    private double maxGlobalTriggeredMirrorScanSpeed; 
    // same for xy stage
    private double maxGlobalTriggeredXyScanSpeed;  

    private int[] z_lim;  // unused probably

    private String xyStageName;
    private double xyStageTravelSpeed; // mm/s (um/ms)
    private double xyStageCurrentScanSpeed;  //  acq scan speed for current channel
    private double xyStageGlobalScanSpeed;  // acq scan speed set same for all channels
    private String xyStageComPort;
    
    private double immersionRI;
    private double opmAngle;
    private double magnification;
    
    private boolean imageView1;
    private boolean imageView2;
    private boolean saveAcquisitionLogs;

    private String mirrorStageName;
    private double mirrorStageTravelSpeed;  // 
    private double mirrorStageCurrentScanSpeed;  //  scan speed used by stages in triggered acq
    private double mirrorStageGlobalScanSpeed;  // acq scan speed set for all channels as set in GUI
    private String mirrorStageComPort;
    
    private String zStageName;
    private double zStageTravelSpeed;  // mm/s (um/ms)
    private String zStageComPort;

    private int currentAcqChannel;
    
    private double exposureTime;  // ms
    private double actualExposureTime;
    private Rectangle frameSize;

    //Time-lapse settings, TODO
    
    public CMMCore core_ = null;
    public Studio mm_ = null;
    
    public DeviceSettingsManager(){
        core_ = MMStudioInstance.getCore();
        mm_ = MMStudioInstance.getStudio();
        initVars();
    }
    
    /** Constructor with CMMCore instance injected, prefer to use empty parameter version now
     *
     * @param cmmcore 
     */
    public DeviceSettingsManager(CMMCore cmmcore) {
        core_ = cmmcore;
        mm_ = MMStudioInstance.getStudio();
        initVars();
        loadAllDeviceNames();  // uMgr StrVector object
    }
    
    private void initVars(){
        triggerModeStrings = new String[]
                {"External trigger (global exposure)", "External trigger (rolling)", "Untriggered"};
        z_lim = new int[]{-12000000,1000000};
        
        // we haven't decided whether we change lasr powers through USB device 
        // interface or modulate with DO/AO lines, so keep these variables
        laserDeviceNames = new ArrayList<>();
        laserBlankingDOLines = new ArrayList<>();
        laserPowerAOports = new ArrayList<>();
        
        filterDeviceName = "";

        dOPMCameraName = "";
        deviceList = null;

        // Device settings, states, etc.
        laserChannelsAcq = null;
        laserPowersAcq = null;
        filtersAcq = null;
        currentAcqChannel = 0;

        // Trigger settings
        scanType = MIRROR_SCAN;
        mirrorScanLength = 50.0;  // Initialized to 50 um
        mirrorTriggerDistance = 1.0;  // Initialized to 1 um
        triggerMode = 0;  // Initialized to 0 (external trigger w/ global reset)
        useMaxScanSpeedForMirror = false;  // Initialized to false
        scanSpeedSafetyFactorMirror = 0.95;
        scanSpeedSafetyFactorXy = 0.95;
        maxTriggeredMirrorScanSpeed = 0.01;  // safely slow
        maxTriggeredXyScanSpeed = 0.01;
        maxGlobalTriggeredMirrorScanSpeed = 0.1;
        maxGlobalTriggeredXyScanSpeed = 0.1;

        xyStageScanLength = 50.0;  // um
        xyStageTriggerDistance = 1.0;  // um
        xyStageScanAxis = "y";  // "x" or "y"
        useMaxScanSpeedForXyStage = false;
        
        xyStageName = core_.getXYStageDevice(); 
        xyStageTravelSpeed = 10.0; // TODO REVERT TO 10
        xyStageCurrentScanSpeed = 0.01;
        xyStageGlobalScanSpeed = 0.01;  // the scan speed used for all channels if useMaxScanSpeed not true
        xyStageComPort = getPortProperty(xyStageName);

        mirrorStageName = "";
        mirrorStageTravelSpeed = 10.0;
        mirrorStageCurrentScanSpeed = 0.01; // safely slow
        mirrorStageGlobalScanSpeed = 0.01;  // the scan speed used for all channels if useMaxScanSpeed not true
        mirrorStageComPort = "COM3";

        zStageName = core_.getFocusDevice();
        zStageTravelSpeed = 10.0;
        zStageComPort = "";
        
        immersionRI = 1.4;
        imageView1 = true;
        imageView2 = false;
        opmAngle = 45;
        magnification = 20*1.4*200/180;
        saveAcquisitionLogs = true;
        
        exposureTime = 5.0;
        actualExposureTime = 5.0;
        frameSize = new Rectangle(0, 0, 2304, 2304);  // Initialized to null
        
    }
    
    // Not really used. To remove?
    public void serializeDeviceSettings(String savepath) throws IOException{
        XMLEncoder e = new XMLEncoder(
                           new BufferedOutputStream(
                               new FileOutputStream(savepath)));
        e.writeObject(this);
        e.close();
    }
    
    private void loadAllDeviceNames(){
        setDeviceList(core_.getLoadedDevices());
    }
    
    /** Loads device names (name in micromanager config) from CSV.
        The idea is to use a GUI to pick the devices from the list, and then
        the GUI interface allows you to save to CSV.
        * @param configDetailsJson Filename of config file, json file
     */
    
    public void loadSystemSettings(String configDetailsJson){

        List<String> expectedKeys = Arrays.asList(new String[]{
            "laser_devices",
            "laser_labels",
            "laser_daq_do_port",
            "laser_daq_blanking_lines",
            "laser_daq_ao_ports",
            "camera_dopm",
            "camera_right",
            "filter",
            "xy_stage",
            "z_stage",
            "mirror_stage",
            "xy_stage_com_port",
            "z_stage_com_port",
            "mirror_stage_com_port",
            "refractive_index",
            "opm_angle",
            "magnification"
        });

        // my config parser class in util
        try {
            ConfigParser configParser = 
                    new ConfigParser(configDetailsJson, expectedKeys);
            configParser.parse();
            HashMap<String, List<String>> configMap = configParser.getConfigMap();

            // device names, ports, etc.
            setdOPMCameraName(
                    configMap.get("camera_dopm"));
            setLaserLabels(configMap.get("laser_labels"));
            setLaserBlankingDOport(
                    configMap.get("laser_daq_do_port"));
            setLaserBlankingDOLines(
                    configMap.get("laser_daq_blanking_lines"));
            setLaserPowerAOports(
                    configMap.get("laser_daq_ao_port"));
            setFilterDeviceName(configMap.get("filter"));
            setXyStageName(configMap.get("xy_stage"));
            setZStageName(configMap.get("z_stage"));
            setMirrorStageName(configMap.get("mirror_stage"));

            List<String> xystagecom = configMap.get("xy_stage_com_port");

            setXyStageComPort(configMap.get("xy_stage_com_port"));
            setMirrorStageComPort(
                    configMap.get("mirror_stage_com_port"));
            
            setZStageComPort(configMap.get("z_stage_com_port"));
            deviceManagerLogger.info("set z stage com port");
            
            // scope values/constants, have to access the Double itself in the
            // list with .get(0) to convert the string to double.
            setImmersionRI(Double.parseDouble(
                    configMap.get("refractive_index").get(0)));
            deviceManagerLogger.info("set RI");
            setOpmAngle(Double.parseDouble(
                    configMap.get("opm_angle").get(0)));
            deviceManagerLogger.info("opm angle");
            setMagnification(Double.parseDouble(
                    configMap.get("magnification").get(0)));

            deviceManagerLogger.info("Set devices with setters");
        } catch (Exception ex){
            deviceManagerLogger.warning(ex.toString());
            JOptionPane.showMessageDialog(null,
                    "Failed to get device names. " + ex.toString(),
                    "dOPM config load failed", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private boolean checkInDeviceList(String deviceName){
        if (deviceName == null){
            deviceManagerLogger.warning("device name is null");
            return false;
        }
        List<String> deviceNames = new ArrayList<>(Arrays.asList(deviceName));
        return checkInDeviceList(deviceNames);
    } 
    
    /** take a list because there are multiple lasers for example */
    private boolean checkInDeviceList(List<String> deviceNames){
        if (deviceNames == null){
            deviceManagerLogger.warning("device names is null");
            return false;
        }
        ArrayList<String> allDevices = 
                new ArrayList<>(Arrays.asList(getDeviceList().toArray()));
        for(int n=0; n<deviceNames.size(); n++){
            // check if in device list

            if (!allDevices.contains(deviceNames.get(n))){
                String errorMsg = String.format("%s does not exist in "
                        + "MicroManager devices for this config, please check the list of "
                        + "devices against the chosen device" , 
                        deviceNames.get(n));
                deviceManagerLogger.warning(errorMsg);
                JOptionPane.showMessageDialog(null, errorMsg);
                return false;
            }
        }
        return true;
    }
    
    /**
     * from the DAQ blanking state work out binary and therefore which laser 
     * is on (only valid for one laser at a time) I don't think this is 
     * necessary because MDA handles this stuff so maybe delete all this stuff
     * @return index of current laser (0-4)
     */
    public int getCurrentLaserIdx(){
        deviceManagerLogger.info("Getting laserIdx" );
        int laserIdx = 0;
        try {
            int laserState = Integer.parseInt(
                    core_.getProperty(laserBlankingDOport, "State"));
            laserIdx = (int)(Math.log(laserState+1)/Math.log(2));
            deviceManagerLogger.info(String.format(
                    "laserState: %d laserIdx %d: ", laserState, laserIdx));
            
        } catch (Exception e){
            deviceManagerLogger.severe("Couldn't get laser index: " 
                    + e.toString());
        }
        return laserIdx;
    }

    /**
     * Get current laser label according to laserLabels (defined in the cfg)
     * @return current laser's label
     */
    public String getCurrentLaser(){
        String laser = "";
        List<String> lasers = new ArrayList<>();
        if (laserLabels.isEmpty()){
            lasers = laserDeviceNames;
        } else if (laserDeviceNames.isEmpty()){
            lasers = laserLabels;
        } else {
            deviceManagerLogger.warning(
                    "Laser labels and laser device names are both empty! "
                    + "Metadata for laser will just be an index");
        }
        
        try {
            int laserIdx = getCurrentLaserIdx();
            if (laserLabels.isEmpty()) {
                laser = String.valueOf(laserIdx);
            } else {
                deviceManagerLogger.info("lasers: " + lasers);
                laser = lasers.get(laserIdx);
            }
        } catch (Exception e){
            deviceManagerLogger.severe(String.format(
                    "Failed to get laser info with error %s",
                    e.getMessage()));
        }
        return laser;
    }

    public String getCurrentFilter(){
        String filter = "";
               
        try {
            filter = core_.getProperty(filterDeviceName, "Label");
        } catch (Exception e){
            deviceManagerLogger.severe(String.format(
                    "Failed to get filter info with error %s",
                    e.getMessage()));
        }
        return filter;
    }
    
    public double getCurrentLaserPower(){
        String laser = getCurrentLaser();
        double power = 0;
        String powerGroupString = String.format("%s power", laser);
        try{
            Configuration cfg = core_.getConfigGroupState(powerGroupString);
            PropertySetting setting = cfg.getSetting(0);
            power = Double.parseDouble(setting.getPropertyValue());
        } catch (Exception e){
            deviceManagerLogger.severe(String.format(
                    "Couldn't get \"%s\"'s power (%s)", laser, e.getMessage()));
        }
        return power;
    }
            
    
    public double getCameraReadoutTime() throws Exception{
       return getCameraReadoutTime(core_.getExposure());
    }
    
    /** Get frame time in ms for camera based on the exposure time and the 
     * camera mode, this function accepts exposure as param
     * @param exposureMs
     * @return camera readout time in ms
     * @throws Exception 
     */
    public double getCameraReadoutTime(double exposureMs) throws Exception{
        
        int trigger = getTriggerMode();
        //String triggerSource = core_.getProperty(getCameraDeviceName(), "TRIGGER SOURCE");
        Rectangle roi;
        try {
            roi = core_.getROI();
        } catch (Exception e){
            deviceManagerLogger.warning("Failed to get ROI, reverting to default");
            roi = getFrameSize();
        }
        // oneH = 9.74439/1000;  // Orca Flash v4 1H in ms (line horizontal readout time)
        double oneHMs = 4.867647*1e-3; // Line readout time for Orca fusion, halved(ish) wrt flash
        double Vn = roi.height;
        double Hn = roi.width;
        // -- this only worked for global exposure time 
        //(not varying between channel e.g. in MDA)
        // double expMs = getExposureTime();  // ms |
        // -----------------------------------------|
        
        // double expMs = core_.getExposure();  // ms |  now in overloaded func
        double exp2Ms = exposureMs - 3.029411*1e-3;
        double minReadoutTimeMs = (Vn+1)*oneHMs;
        double readout_time = 0;
        
        switch (trigger){
            case 0: // External trigger (e.g. global reset)
                readout_time = (Vn+Math.ceil(exp2Ms/oneHMs)+4)*oneHMs;  // in ms
                // readout_time = (Vn+Math.ceil(exp2/oneH)+4)*oneH;  // in ms
                break;
            case 1:
                readout_time = ((Vn+1)*oneHMs+Math.max(0, (exposureMs*1e3-minReadoutTimeMs))); // in ms
                break;
            case 2:
                readout_time = minReadoutTimeMs;
                break;
        }
        deviceManagerLogger.info("Calculated camera readout time as " 
                + readout_time + " ms");
        return readout_time;
    }
    
    // future idea, separate settings by device
    /*public class MirrorDeviceSettings{
        String stageName = getMirrorStageName();
        
        MirrorDeviceSettings(){};
    }*/
    
    private String getPortProperty(String deviceName){
        try {
            return core_.getProperty(deviceName, "Port");
        } catch (Exception e){
            deviceManagerLogger.warning("No port property found for " + deviceName);
        }
        return "";
    }

    public double getScanSpeedSafetyFactorMirror() {
        return scanSpeedSafetyFactorMirror;
    }

    public void setScanSpeedSafetyFactorMirror(double scanSpeedSafetyFactorMirror) {
        this.scanSpeedSafetyFactorMirror = scanSpeedSafetyFactorMirror;
        updateMaxTriggeredMirrorScanSpeed();
    }

    public double getScanSpeedSafetyFactorXy() {
        return scanSpeedSafetyFactorXy;
    }

    public void setScanSpeedSafetyFactorXy(double scanSpeedSafetyFactorXy) {
        if (scanSpeedSafetyFactorXy <= 1 & scanSpeedSafetyFactorXy > 0){
            this.scanSpeedSafetyFactorXy = scanSpeedSafetyFactorXy;
        } else {
            deviceManagerLogger.warning("Safety factor attempted to set out of "
                    + "bounds: " + scanSpeedSafetyFactorXy);
        }
        updateMaxTriggeredXyScanSpeed();
    }
    
    public double getMaxTriggeredXyScanSpeed() {
        return maxTriggeredXyScanSpeed;
    }
    
    public void updateMaxTriggeredXyScanSpeed(){
        double exp = 0;
        try {
            exp = core_.getExposure();
        } catch (Exception e) {
            deviceManagerLogger.severe(e.toString());
        }
        maxTriggeredXyScanSpeed = calculateMaxTriggeredXyScanSpeed(exp);
    }
    
    public double getMaxTriggeredMirrorScanSpeed() {
        return maxTriggeredMirrorScanSpeed;
    }
    
    public void updateMaxTriggeredMirrorScanSpeed(){
        double exp = 0;
        try {
            exp = core_.getExposure();
        } catch (Exception e) {
            deviceManagerLogger.severe(e.toString());
        }
        maxTriggeredMirrorScanSpeed = calculateMaxTriggeredMirrorScanSpeed(exp);
    }
    
    
    /** Update max possible triggered scan speed of mirror for current exp time
     * @param exp exposure time of camera
     * @return calculated maximum mirror scan speed
     */
    public double calculateMaxTriggeredMirrorScanSpeed(double exp){
        try {
            return getMirrorTriggerDistance()/getCameraReadoutTime(exp)*
                            getScanSpeedSafetyFactorMirror();
        } catch (Exception e){
            deviceManagerLogger.severe("Failed to update max scan speed "
                    + "for triggering with mirror with " + e.getMessage());
            return mirrorStageGlobalScanSpeed;
        }
    }
    
    /** Update max possible triggered scan speed of XY stage for current exp time
     * @param exp camera exposure time
     * @return calculated maximum xy stage scan speed
     */
    public double calculateMaxTriggeredXyScanSpeed(double exp){
        try {
            return getXyStageTriggerDistance()/getCameraReadoutTime(exp)*
                            getScanSpeedSafetyFactorXy();
        } catch (Exception e){
            deviceManagerLogger.severe("Failed to update max scan speed "
                    + "for triggering with xy stage with " + e.getMessage());
            return xyStageGlobalScanSpeed;
        }
    }
    
    public void updateCurrentScanSpeedsDuringAcq(){
    
        // Set scan speed variables accordingly for mirror and xystage
        // need to update (calculate) these in case they were updated
        // by a change in the GUI
        
        updateMaxGlobalTriggeredXyScanSpeed();
        updateMaxTriggeredXyScanSpeed();
        updateMaxTriggeredMirrorScanSpeed();
        
        if (getUseMaxScanSpeedForMirror()){
            setMirrorStageCurrentScanSpeed(
                    getMaxTriggeredMirrorScanSpeed());
        } else {
            deviceManagerLogger.info("setting mirror scan speed to " + 
                    getMirrorStageGlobalScanSpeed());
            setMirrorStageCurrentScanSpeed(
                    getMirrorStageGlobalScanSpeed());
        }
        
        if (getUseMaxScanSpeedForXyStage()){
            setXyStageCurrentScanSpeed(
                    getMaxTriggeredXyScanSpeed());
        } else {
            // the one set by the user in the GUI
            setXyStageCurrentScanSpeed(
                    getXyStageGlobalScanSpeed());
        }
    }

    // Loop through channel specs from MDA to get exposures and return the max
    private double getMaxExposureInAcq() throws Exception{
        deviceManagerLogger.info("in getMaxExposure");
        List<ChannelSpec> acqChannels;
        try {
            AcquisitionManager aqcm = mm_.getAcquisitionManager();
            SequenceSettings acqSettings = mm_.getAcquisitionManager().getAcquisitionSettings();
            acqChannels = mm_.getAcquisitionManager().getAcquisitionSettings().
                    channels();
        } catch (Exception e){
            deviceManagerLogger.severe("Failed to get ChannelSpec with: " + 
                    e.getMessage() + " check if MDA is open?");
            throw e;
        }
        double maxExposure = 0;
        double exp_ = 0;
        for (int n = 0; n < acqChannels.size(); n++){
            exp_ = acqChannels.get(n).exposure();
            if (exp_ > maxExposure){
                maxExposure = exp_;
            }
        }
        return maxExposure;
    }

    /** max global XY scan speed considers longest exposure in the MDA
     * 
     * @return max possible global (across all channels/exps) XY scan speed
     */
    public double getMaxGlobalTriggeredXyScanSpeed() {
        return maxGlobalTriggeredXyScanSpeed;
    }
    
    /** max global mirror scan speed considers longest exposure in the MDA
     * 
     * @return max possible global (across all channels/exps) mirror scan speed
     */
    public double getMaxGlobalTriggeredMirrorScanSpeed() {
        return maxGlobalTriggeredMirrorScanSpeed;
    }
    
    /** Update global max triggered xy scan speed
     * (i.e., limited by the highest exp time in the MDA)
     */
    public void updateMaxGlobalTriggeredXyScanSpeed(){
        double maxExp;
        try {
            maxExp = getMaxExposureInAcq();
        } catch (Exception e){
            deviceManagerLogger.severe("Failed to update "
                    + "maxGlobalTriggeredScanSpeed with: " + e.getMessage());
            return;
        }
        deviceManagerLogger.info("Max exposure in acq: " + maxExp);
        double globalMaxScanSpeed = 
                calculateMaxTriggeredXyScanSpeed(maxExp);
        deviceManagerLogger.info("globalMaxScanSpeed: " + globalMaxScanSpeed);
        maxGlobalTriggeredXyScanSpeed = globalMaxScanSpeed;
    }
        
    /** Update global max triggered mirror scan speed
     * (i.e., limited by the highest exp time in the MDA)
     */
    public void upateMaxGlobalTriggeredMirrorScanSpeed(){
        double maxExp;
        try {
            maxExp = getMaxExposureInAcq();
        } catch (Exception e){
            deviceManagerLogger.severe("Failed to update "
                    + "maxGlobalTriggeredScanSpeed with: " + e.getMessage());
            return;
        }
        deviceManagerLogger.info("Max exposure in acq: " + maxExp);
        double globalMaxMirrorScanSpeed = 
                calculateMaxTriggeredMirrorScanSpeed(maxExp);
        deviceManagerLogger.info("globalMaxScanSpeed: " + globalMaxMirrorScanSpeed);
        maxGlobalTriggeredMirrorScanSpeed = globalMaxMirrorScanSpeed;
    }

    // I think the following are not used, this is handled by MDA -----
    // <--------
    public List<String> getLaserLabels() {
        return laserLabels;
    }

    public void setLaserLabels(List<String> laserLabels) {
        this.laserLabels = laserLabels;
        deviceManagerLogger.info("set laser labels to " + laserLabels);
    }
    
    
    public List<String> getLaserChannelsAcq() {
        return laserChannelsAcq;
    }

    public void setLaserChannelsAcq(List<String> laserChannels) {
        this.laserChannelsAcq = laserChannels;
    }

    public List<String> getLaserPowersAcq() {
        return laserPowersAcq;
    }

    public void setLaserPowersAcq(List<String> laserPowers) {
        this.laserPowersAcq = laserPowers;
    }

    public List<String> getFiltersAcq() {
        return filtersAcq;
    }

    public void setFiltersAcq(List<String> filters) {
        this.filtersAcq = filters;
    }
    // ending section of unused code (TODO remove, Hugh)
    // -->
    

    public double lateralScanToMirrorNormal(double lateral){
        return 2*lateral*Math.sin(0.5*getOpmAngle()*Math.PI/180)/getImmersionRI();
    }
    /**
     * Convert from user inputted PI mirror scan length (in z') to actual PI 
     * scan direction. TODO update this formula to Hugh's
     * @param normal scan direction in direction normal to oblique image planes
     * @return 
     */
    public double mirrorNormaltoLateralScan(double normal){
        return normal*getImmersionRI()/(2*Math.sin(0.5*getOpmAngle()*Math.PI/180));
    }
    
    public double lateralScanToLabZ(double lateral){
        return lateralScanToMirrorNormal(lateral)*Math.cos(0.5*getOpmAngle());
        // return getImmersionRI()/(2*normal*Math.sin(getOpmAngle()*Math.PI/180));
    }
    
    public double labZtoLateralScan(double z){
        return mirrorNormaltoLateralScan(z/Math.cos(0.5*getOpmAngle()));
        // return getImmersionRI()/(2*normal*Math.sin(getOpmAngle()*Math.PI/180));
    }
    
    public double getMirrorScanLength() {
        return mirrorScanLength;
    }

    public void setMirrorScanLength(double mirrorScanLength) {
        this.mirrorScanLength = mirrorScanLength;
        deviceManagerLogger.info("Set mirror scan length to " + mirrorScanLength);
    }

    public double getMirrorTriggerDistance() {
        return mirrorTriggerDistance;
    }

    /** Trigger distance setter, in um 
     * @param mirrorTriggerDistance trigger distance in um */
    public void setMirrorTriggerDistance(double mirrorTriggerDistance) {
        this.mirrorTriggerDistance = mirrorTriggerDistance;
        deviceManagerLogger.info("Set mirror trigger distance to " + mirrorTriggerDistance);
        updateMaxTriggeredMirrorScanSpeed();  // the current max speed based on current exposure time
        upateMaxGlobalTriggeredMirrorScanSpeed();  // the lowest max speed across all channels/exposure times
    }

    public double getXyStageScanLength() {
        return xyStageScanLength;
    }

    public void setXyStageScanLength(double xyStageScanLength) {
        this.xyStageScanLength = xyStageScanLength;
        deviceManagerLogger.info("Set xy stage scan length to " 
                + xyStageScanLength);
    }

    public double getXyStageTriggerDistance() {
        return xyStageTriggerDistance;
    }

    public void setXyStageTriggerDistance(double xyStageTriggerDistance) {
        this.xyStageTriggerDistance = xyStageTriggerDistance;
        updateMaxTriggeredXyScanSpeed();
        updateMaxGlobalTriggeredXyScanSpeed();
        deviceManagerLogger.info("Set xy stage trigger distance to " 
                + xyStageTriggerDistance);

    }

    public String getXyStageScanAxis() {
        return xyStageScanAxis;
    }

    public void setXyStageScanAxis(String xyStageScanAxis) {
        this.xyStageScanAxis = xyStageScanAxis;
    }
     
    
    public int getTriggerMode() {
        return triggerMode;
    }

    public void setTriggerMode(int triggerMode) {
        this.triggerMode = triggerMode;
        upateMaxGlobalTriggeredMirrorScanSpeed();
        updateMaxTriggeredMirrorScanSpeed();
        updateMaxGlobalTriggeredXyScanSpeed();
        updateMaxTriggeredXyScanSpeed();
    }

    public int getScanType() {
        return scanType;
    }

    public void setScanType(int scanType) {
        this.scanType = scanType;
    }

    
    public boolean getUseMaxScanSpeedForMirror() {
        return useMaxScanSpeedForMirror;
    }

    public void setUseMaxScanSpeedForMirror(boolean useMaxScanSpeedForMirror) {
        this.useMaxScanSpeedForMirror = useMaxScanSpeedForMirror;
    }

    public int[] getZ_lim() {
        return z_lim;
    }

    public void setZ_lim(int[] z_lim) {
        this.z_lim = z_lim;
    }

    public String getXyStageName() {
        return xyStageName;
    }
    
    public void setXyStageName(List<String> xyStageName) {
        if (!xyStageName.isEmpty()){
            setXyStageName(xyStageName.get(0));
        }
    }
    
    public void setXyStageName(String xyStageName) {
        if (!xyStageName.equals("")) {
            this.xyStageName = xyStageName;
            deviceManagerLogger.info("Set XY stage name to " + xyStageName);
            // get port from property, note that resetting stagename will set COM port to ""
            setXyStageComPort(getPortProperty(xyStageName));
        }
    }

    public double getXyStageTravelSpeed() {
        return xyStageTravelSpeed;
    }

    public void setXyStageTravelSpeed(double xyStageTravelSpeed) {
        this.xyStageTravelSpeed = xyStageTravelSpeed;
    }

    public double getXyStageCurrentScanSpeed() {
        return xyStageCurrentScanSpeed;
    }

    public void setXyStageCurrentScanSpeed(double xyStageCurrentScanSpeed) {
        this.xyStageCurrentScanSpeed = xyStageCurrentScanSpeed;
    }

    public double getXyStageGlobalScanSpeed() {
        return xyStageGlobalScanSpeed;
    }

    public void setXyStageGlobalScanSpeed(double xyStageGlobalScanSpeed) {
        
        double maxGlobalScanSpeed = getMaxGlobalTriggeredXyScanSpeed();
        if (xyStageGlobalScanSpeed > maxGlobalScanSpeed){
            this.xyStageGlobalScanSpeed = maxGlobalScanSpeed;
        } else {
            this.xyStageGlobalScanSpeed = xyStageGlobalScanSpeed;
        }
    }
     
    public String getXyStageComPort() {
        return xyStageComPort;
    }
    
    public void setXyStageComPort(List<String> xyStageComPort) {
        deviceManagerLogger.info("xystagecomport" + xyStageComPort);
        if (!xyStageComPort.isEmpty()){
            deviceManagerLogger.info("Getting XY stage COM port from cfg file");
            setXyStageComPort(xyStageComPort.get(0));
            deviceManagerLogger.info("set xystagecomport");

        }
    }
    
    public void setXyStageComPort(String xyStageComPort) {
        if (!xyStageComPort.equals("")) {
            this.xyStageComPort = xyStageComPort;
            deviceManagerLogger.info("Set XY stage COM port to " + xyStageComPort);
        }
    }
    
    public boolean getUseMaxScanSpeedForXyStage() {
        return useMaxScanSpeedForXyStage;
    }

    public void setUseMaxScanSpeedForXyStage(boolean useMaxScanSpeedForXyStage) {
        deviceManagerLogger.info(String.format(
                "Set setUseMaxScanSpeedForXyStage to %b", useMaxScanSpeedForXyStage));
        this.useMaxScanSpeedForXyStage = useMaxScanSpeedForXyStage;
    }

    public String getMirrorStageName() {
        return mirrorStageName;
    }
    
    public void setMirrorStageName(List<String> mirrorStageName) {
        deviceManagerLogger.info("mirror stage name " + mirrorStageName);
        if (!mirrorStageName.isEmpty()){
            deviceManagerLogger.info("mirror stage name not null");
            setMirrorStageName(mirrorStageName.get(0));
            deviceManagerLogger.info("mirror stage did .get(0)");

        }
    }
    
    public void setMirrorStageName(String mirrorStageName) {
        if (mirrorStageName != null){
            this.mirrorStageName = mirrorStageName;
            deviceManagerLogger.info("Set mirror stage device name to " 
                    + mirrorStageName);
            // try to automatically get COM port
            /* Having issues here, commenting out for now
            deviceManagerLogger.info("getting mirror stage COM port automatically");
            String port = getPortProperty(mirrorStageName);
            deviceManagerLogger.info("port from MM is " + port);
            setMirrorStageComPort(port);
            */
        }
    }

    public double getMirrorStageTravelSpeed() {
        return mirrorStageTravelSpeed;
    }

    public void setMirrorStageTravelSpeed(double mirrorStageTravelSpeed) {
        this.mirrorStageTravelSpeed = mirrorStageTravelSpeed;
        // will move this stuff to backend...
        // DeviceSettingsManager.setMirrorStageSpeed(mirrorStageTravelSpeed);
    }

    public double getMirrorStageCurrentScanSpeed() {
        return mirrorStageCurrentScanSpeed;
    }

    public void setMirrorStageCurrentScanSpeed(double mirrorStageCurrentScanSpeed) {
        this.mirrorStageCurrentScanSpeed = mirrorStageCurrentScanSpeed;        
    }

    public double getMirrorStageGlobalScanSpeed() {
        return mirrorStageGlobalScanSpeed;
    }

    public void setMirrorStageGlobalScanSpeed(double mirrorStageGlobalScanSpeed) {
        double maxGlobalScanSpeed = getMaxGlobalTriggeredXyScanSpeed();
        if (mirrorStageGlobalScanSpeed > maxGlobalScanSpeed){
            this.mirrorStageCurrentScanSpeed = maxGlobalScanSpeed;
        } else {
            this.mirrorStageGlobalScanSpeed = mirrorStageGlobalScanSpeed;
        }
        deviceManagerLogger.info(String.format(
                "set global mirror scan speed to %.4f mm/s", 
                this.mirrorStageGlobalScanSpeed));
    }
    

    public String getMirrorStageComPort() {
        return mirrorStageComPort;
    }
    
    /* e.g... TODO REMOVE
    public void setXyStageComPort(List<String> xyStageComPort) {
        if (xyStageComPort != null){
            setXyStageComPort(xyStageComPort.get(0));
        }
    }
    
    public void setXyStageComPort(String xyStageComPort) {
        if (!xyStageComPort.equals("")) this.xyStageComPort = xyStageComPort;
    }*/
    
    public void setMirrorStageComPort(List<String> mirrorStageComPort) {
        if (!mirrorStageComPort.isEmpty()){
            deviceManagerLogger.info("Getting COM port from cfg file");
            setMirrorStageComPort(mirrorStageComPort.get(0));
        }
    }
    
    public void setMirrorStageComPort(String mirrorStageComPort) {
        if (!mirrorStageComPort.equals("")){
            this.mirrorStageComPort = mirrorStageComPort;
            deviceManagerLogger.info("set mirror stage port to " + mirrorStageComPort);
        }
        deviceManagerLogger.info("done in setMirrorStageComPort");
    }

    public boolean isView1Imaged() {
        return imageView1;
    }

    public void setView1Imaged(boolean imageView1) {
        this.imageView1 = imageView1;
    }

    public boolean isView2Imaged() {
        return imageView2;
    }

    public void setView2Imaged(boolean imageView2) {
        this.imageView2 = imageView2;
    }

    public double getOpmAngle() {
        return opmAngle;
    }

    public void setOpmAngle(List<Double> opmAngle){
        if (!opmAngle.isEmpty()){
            setOpmAngle(opmAngle.get(0));
        }
    }
    
    public void setOpmAngle(double opmAngle) {
        this.opmAngle = opmAngle;
    }

    public double getImmersionRI() {
        return immersionRI;
    }
    
    public void setImmersionRI(List<Double> immersionRI){
        if (!immersionRI.isEmpty()){
            setImmersionRI(immersionRI.get(0));
        }
    }
    
    public void setImmersionRI(double immersionRI) {
        this.immersionRI = immersionRI;
    }

    public double getMagnification() {
        return magnification;
    }

    public void setMagnification(List<Double> magnification){
        if (!magnification.isEmpty()){
            setMagnification(magnification.get(0));
        }
    }
    
    public void setMagnification(double magnification) {
        this.magnification = magnification;
    }

    public boolean isSaveAcquisitionLogs() {
        return saveAcquisitionLogs;
    }

    public void setSaveAcquisitionLogs(boolean saveAcquisitionLogs) {
        this.saveAcquisitionLogs = saveAcquisitionLogs;
    }
    
    
    public String getZStageName() {
        return zStageName;
    }

    public void setZStageName(List<String> zStageName){
        if (!zStageName.isEmpty()){
            setZStageName(zStageName.get(0));
        }
    }
    
    public void setZStageName(String zStageName) {
        if (!zStageName.equals("")){
            this.zStageName = zStageName;
        } else {
            this.zStageName = core_.getFocusDevice();  // might be "" too
        }
        // find z stage port by getting port property (wont work with Ti2)
        setZStageComPort(getPortProperty(zStageName));  

    }

    public double getzStageTravelSpeed() {
        return zStageTravelSpeed;
    }

    public void setzStageTravelSpeed(double zStageTravelSpeed) {
        this.zStageTravelSpeed = zStageTravelSpeed;
    }

    public String getzStageComPort() {
        return zStageComPort;
    }

    public void setZStageComPort(List<String> zStageComPort) {
        if (!zStageComPort.isEmpty()) setZStageComPort(zStageComPort.get(0));
    }
    
    public void setZStageComPort(String zStageComPort) {
        deviceManagerLogger.info("Trying to set z stage com port");
        deviceManagerLogger.info("z stage com port is" + zStageComPort);
        if ( zStageComPort==null){
            deviceManagerLogger.info("z com is null");
        }
        deviceManagerLogger.info("does it equal '' " + zStageComPort.equals(""));
        if (!zStageComPort.equals("")) this.zStageComPort = zStageComPort;
    }

    // NOT IN USE, USE INTERNAL EXPOSURE TIME IN MMCore INSTEAD
    // --------------------------------------------------------
    public double getExposureTime() {
        return exposureTime;
    }

    public void setExposureTime(double exposureTime) {
        this.exposureTime = exposureTime;
        deviceManagerLogger.info("Set exposureTime to " + exposureTime);
        updateMaxTriggeredMirrorScanSpeed();
        updateMaxTriggeredXyScanSpeed();
    }

    public double getActualExposureTime() {
        return actualExposureTime;
    }
    // --------------------------------------------------------
    
    public Rectangle getFrameSize() {
        return frameSize;
    }

    public void setFrameSize(Rectangle frameSize) {
        this.frameSize = frameSize;
        updateMaxTriggeredXyScanSpeed();
        updateMaxTriggeredMirrorScanSpeed();
    }
    
    /* not sure what this is for, i think rolling shutter mode */
    public void setActualExposureTime(double actualExposureTime) {
        this.actualExposureTime = actualExposureTime;
    }
    
    public List<String> getLaserDeviceNames() {
        // if (!laserDeviceNames.equals("")) setLaserDeviceNames(new ArrayList<>(Arrays.asList("")));
        return laserDeviceNames;
    }
    
    public void setLaserDeviceNames(List<String> laserDeviceNames) { 
        if (laserDeviceNames == null){
            deviceManagerLogger.warning("Laser device names is null");
        } else if (checkInDeviceList(laserDeviceNames)){
            this.laserDeviceNames = laserDeviceNames;
            deviceManagerLogger.info("Laser set to" + laserDeviceNames);
        }
    }

    public List<String> getLaserBlankingDOLines() {
        return laserBlankingDOLines;
    }

    public void setLaserBlankingDOLines(List<String> laserBlankingDOLines) throws Exception{
        if (laserBlankingDOLines == null){
            deviceManagerLogger.warning("Laser blanking lines is null");
        } else if (laserBlankingDOLines.isEmpty()){
           deviceManagerLogger.warning("Laser blanking lines is empty");
        } else if (checkInDeviceList(getLaserBlankingDOport())){
            for (int n=0; n<laserBlankingDOLines.size(); n++){
                boolean isProp;
                isProp = core_.hasProperty(getLaserBlankingDOport(), laserBlankingDOLines.get(n));
                if(!isProp) deviceManagerLogger.warning(String.format(
                        "No such DO line %s in DAQ port %s", 
                        laserBlankingDOLines.get(n),
                        getLaserBlankingDOport()));
            }
            this.laserBlankingDOLines = laserBlankingDOLines;
        }
    }

    public String getLaserBlankingDOport() {
        return laserBlankingDOport;
    }

    public void setLaserBlankingDOport(List<String> laserBlankingDOport) {
        if (!laserBlankingDOport.isEmpty()){
            setLaserBlankingDOport(laserBlankingDOport.get(0));
        }
    }
    
    public void setLaserBlankingDOport(String laserBlankingDOport) {
        if (checkInDeviceList(laserBlankingDOport)){
            this.laserBlankingDOport = laserBlankingDOport;
            deviceManagerLogger.info("set daq blanking port to " + laserBlankingDOport);
        }
    }

    public List<String> getLaserPowerAOports() {
        return laserPowerAOports;
    }

    public void setLaserPowerAOports(List<String> laserPowerAOports) {
        if ( laserPowerAOports == null){
            deviceManagerLogger.warning("Laser AO ports is null");
        } else if (checkInDeviceList(laserPowerAOports)){
            this.laserPowerAOports = laserPowerAOports;
            deviceManagerLogger.info("set daq laser ao ports " + laserPowerAOports);
        }
    }

    public String getFilterDeviceName() {
        return filterDeviceName;
    }

    public void setFilterDeviceName(List<String> filterDeviceName) {
        if (!filterDeviceName.isEmpty()){
            setFilterDeviceName(filterDeviceName.get(0));
        }
    }
    
    public void setFilterDeviceName(String filterDeviceName) {
        if (checkInDeviceList(filterDeviceName)){
            this.filterDeviceName = filterDeviceName;
            deviceManagerLogger.info("Filter device set to " + filterDeviceName);

        }
    }

    public String getdOPMCameraName() {
        return dOPMCameraName;
    }

    public void setdOPMCameraName(List<String> dOPMCameraName) {
        if (!dOPMCameraName.isEmpty()){
            setdOPMCameraName(dOPMCameraName.get(0));
        }
    }
    
    public void setdOPMCameraName(String dOPMCameraName) {
        if (checkInDeviceList(dOPMCameraName)){
            this.dOPMCameraName = dOPMCameraName;
            deviceManagerLogger.info("Camera set to" + dOPMCameraName);
        }
    }
    
    
    public StrVector getDeviceList() {
        return deviceList;
    }

    public void setDeviceList(StrVector deviceList) {
        this.deviceList = deviceList;
    }
    
    public String getProperty(String device, String propety){
        String value = "";
        try{
            value = core_.getProperty(device, propety);
        } catch (Exception e){
            deviceManagerLogger.severe(e.getMessage());
        }
        return value;
    }
    
    public void setProperty(String device, String propety, String propertyValue){
        try{
            core_.setProperty(device, propety, propertyValue);
        } catch (Exception e){
            deviceManagerLogger.severe(e.getMessage());
        }
    }
    
    // struct-like pattern for laser devices 
    // controlled by USB. probably never will use this TO REMOVE
    public class USBLaserDevice{
        private String name;
        private String wavelength;
        private String lineDO;
        private String enableGroup;
        private String powerGroup;
        // private String lineAO;
        private String type;
        
        void LaserDevice(String name, String wavelength, String lineDO){
            this.name = name;
            this.lineDO = lineDO;
            this.wavelength = wavelength;
            // this.lineAO = lineAO;
            this.type = "DAQ laser";
            this.powerGroup = String.format("Power %s", wavelength);
        }
        
        void LaserDevice(String name, String wavelength){
            this.name = name;
            this.enableGroup = String.format("%s enable", wavelength);
            this.wavelength = wavelength;
            // this.lineAO = lineAO;
            this.type = "DAQ laser";
            this.powerGroup = String.format("Power %s", wavelength);
        }
    }
}