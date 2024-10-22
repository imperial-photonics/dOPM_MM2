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
import org.micromanager.Studio;
import org.micromanager.acquisition.AcquisitionManager;
import org.micromanager.acquisition.ChannelSpec;
import org.micromanager.acquisition.SequenceSettings;

/** DeviceManager: class for handling device names in microManager configuration
 *
 * @author lnr19
 */
public class DeviceManager {
    private static final Logger deviceManagerLogger = Logger.getLogger(DeviceManager.class.getName()); 

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
    private String leftCameraName;
    private StrVector deviceList;
    private List<DeviceDetails> detailsOfDevicesInUse;

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
    
    private double scanSpeedSafetyFactor;
    // max possible scan speed for current channel
    private double maxTriggeredScanSpeed;  
    // max possible scan speed considering all the exposure times in acq
    // i.e., the lowest of all scan speeds for each channel
    private double maxGlobalTriggeredScanSpeed;  
    
    private int[] z_lim;  // unused probably

    private String xyStageName;
    private double xyStageTravelSpeed; // mm/s (um/ms)
    private double xyStageCurrentScanSpeed;  //  acq scan speed for current channel
    private double xyStageGlobalScanSpeed;  // acq scan speed set same for all channels
    private String xyStageComPort;
    
    private double opmAngle;
    private boolean imageView1;
    private boolean imageView2;
    private boolean saveAcquisitionLogs;

    private String mirrorStageName;
    private double mirrorStageTravelSpeed;
    private double mirrorStageCurrentScanSpeed;  //  scan speed used by stages in scanned acq--not travel
    private double mirrorStageGlobalScanSpeed;  // acq scan speed set same for all channels
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
    
    public DeviceManager(){
        core_ = MMStudioInstance.getCore();
        mm_ = MMStudioInstance.getStudio();
        initVars();
    }
    
    /** Constructor with CMMCore instance injected, prefer to use empty parameter version now
     *
     * @param cmmcore 
     */
    public DeviceManager(CMMCore cmmcore) {
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

        leftCameraName = "";
        deviceList = null;
        detailsOfDevicesInUse = null;

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
        scanSpeedSafetyFactor = 0.95;
        maxTriggeredScanSpeed = 0.01;  // safely slow
        maxGlobalTriggeredScanSpeed = 0.1;

        xyStageScanLength = 50.0;  // um
        xyStageTriggerDistance = 1.0;  // um
        xyStageScanAxis = "y";  // "x" or "y"
        useMaxScanSpeedForXyStage = false;
        
        xyStageName = ""; 
        xyStageTravelSpeed = 10.0;
        xyStageCurrentScanSpeed = 0.01;
        xyStageGlobalScanSpeed = 0.01;  // the scan speed used for all channels if useMaxScanSpeed not true
        xyStageComPort = "COM7";

        mirrorStageName = "";
        mirrorStageTravelSpeed = 10.0;
        mirrorStageCurrentScanSpeed = 0.01; // safely slow
        mirrorStageGlobalScanSpeed = 0.01;  // the scan speed used for all channels if useMaxScanSpeed not true
        mirrorStageComPort = "COM3";

        zStageName = "";
        zStageTravelSpeed = 10.0;
        zStageComPort = "";
        
        imageView1 = true;
        imageView2 = false;
        opmAngle = 45;
        saveAcquisitionLogs = true;
        
        exposureTime = 5.0;
        actualExposureTime = 5.0;
        frameSize = new Rectangle(0, 0, 2304, 2304);  // Initialized to null
        
    }
    
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
        * @param configDetailsCsv Filename of config file, format:
        * ----- CSV file -----
        *   [0] laser,laserDeviceName1,laserDeviceName2,...,\n
        *   [1] laser,laserBlankingLine1,laserBlankingLine2,...,\n
        *   [2] camera,cameraDeviceName,\n
        *   [3] filter,filterDeviceName,\n
        *   [4] XYStage,XYStageDeviceName,\n
        *   [5] Zstage,ZstageDeviceName,\n
        *   [6] mirrorStage,mirrorStageDeviceName,\n
        *   [7] XYStageCOMPort,XYStageCOMPort,\n
        *   [8] ZstageCOMPort,ZstageCOMPort,\n
        *   [9] mirrorStageCOMPort,mirrorStageCOMPort,\n
        *   [10] DAQDOport,daqDOPortName,\n  
        * ---------------------
     */
    public void loadDeviceNames(File configDetailsCsv){
        try (BufferedReader br = new BufferedReader(new FileReader(configDetailsCsv))) {

            List<List<String>> configData = new ArrayList<>();
            String line;
            
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                deviceManagerLogger.info("Reading out from CSV " + values[0]);
                configData.add(new ArrayList(Arrays.asList(values)));
            }
            
            // see parseList method below this method   
            // first column is the label (so use get(1), and that sublist
            
            /* deviceManagerLogger.info("getting laser dev");
            List<String> lasersLine = parseList(configData.get(0)); */
            
            List<String> laserLabs = parseList(configData.get(1));
            String laserDOPort = (parseList(configData.get(2))).get(0);
            List<String> laserDOLines = parseList(configData.get(3));
            List<String> laserAOports = parseList(configData.get(4));
            String filter = (parseList(configData.get(7))).get(0);
            String XYStage = (parseList(configData.get(8))).get(0);
            String ZStage = (parseList(configData.get(9))).get(0);
            String mirrorStage = (parseList(configData.get(10))).get(0);
            String XYStageCOM = (parseList(configData.get(11))).get(0);
            String ZStageCOM = (parseList(configData.get(12))).get(0);
            String mirrorStageCOM = (parseList(configData.get(13))).get(0);

            setLeftCameraName(core_.getCameraDevice());
            setLaserLabels(laserLabs);
            setLaserBlankingDOport(laserDOPort);
            setLaserBlankingDOLines(laserDOLines);
            setLaserPowerAOports(laserAOports);
            setFilterDeviceName(filter);
            setXyStageName(XYStage);
            setZStageName(ZStage);
            setMirrorStageName(mirrorStage);
            setXyStageComPort(XYStageCOM);
            setMirrorStageComPort(mirrorStageCOM);
            setZStageComPort(ZStageCOM);
            
            
            deviceManagerLogger.info("Set devices with setters");
            // now get full list of devices in use
            // start list off with the lasers, then add the rest
            List<String> devicesInUse = getLaserDeviceNames();
            devicesInUse.add(getFilterDeviceName());
            devicesInUse.add(getXyStageName());
            devicesInUse.add(getZStageName());
            devicesInUse.add(getMirrorStageName());
            devicesInUse.add(getLeftCameraName());
            
            // then their properties, use this to revert after an acquisition for example
            List<DeviceDetails> devicesDetails = new ArrayList<DeviceDetails>();
            for (int i=0; i<devicesInUse.size(); i++){
                devicesDetails.add(new DeviceDetails(devicesInUse.get(i)));
                deviceManagerLogger.info("adding device " + devicesInUse.get(i));
            }
            setDetailsOfDevicesInUse(devicesDetails);

        } catch (FileNotFoundException ex) {
            deviceManagerLogger.warning(ex.getMessage());
            JOptionPane.showMessageDialog(null,
                    String.format("No config file found at %s",configDetailsCsv.getAbsoluteFile()),
                    "File not found",JOptionPane.ERROR_MESSAGE);
        } catch (IOException ex){
            deviceManagerLogger.warning(ex.getMessage());
            JOptionPane.showMessageDialog(null,
                    String.format("Failed to load file at %s",configDetailsCsv.getAbsoluteFile()),
                    "File not found",JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex){
            deviceManagerLogger.warning(ex.getMessage());
            JOptionPane.showMessageDialog(null,
                    "Failed to get device names, check if devices are loaded in "
                            + "MicroManager and is correct. Error: " + ex.getMessage(),
                    "File not found", JOptionPane.ERROR_MESSAGE);
        }
    }
    private List<String> parseList(List<String> row){
        deviceManagerLogger.info("Line read from device config: " + row);
        if(row.size()==1){  // size 1 indicates the column label only
            row.add("");  // add on the value 
        }
        row = row.subList(1,row.size());
        for (int r_i=0; r_i<row.size(); r_i++){
            row.set(r_i, row.get(r_i).replaceAll("\\s+",""));
        }
        
        deviceManagerLogger.info("Devices in config: " + row);
        return row;
    }
    
    private boolean checkInDeviceList(String deviceName){
        List<String> deviceNames = new ArrayList<>(Arrays.asList(deviceName));
        return checkInDeviceList(deviceNames);
    } 
    
    /** take a list because there are multiple lasers for example */
    private boolean checkInDeviceList(List<String> deviceNames){
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
    
    private String getPort(String deviceName){
        try {
            return core_.getProperty(deviceName, "Port");
        } catch (Exception e){
            deviceManagerLogger.warning("No port property found for " + deviceName);
        }
        return "";
    }

    public double getScanSpeedSafetyFactor() {
        return scanSpeedSafetyFactor;
    }

    public void setScanSpeedSafetyFactor(double scanSpeedSafetyFactor) {
        this.scanSpeedSafetyFactor = scanSpeedSafetyFactor;
        updateMaxTriggeredScanSpeed();
    }

    public double getMaxTriggeredScanSpeed() {
        updateMaxTriggeredScanSpeed();
        return maxTriggeredScanSpeed;
    }
    
    /** not really used, should be updated by updateMaxTriggeredScanSpeed */
    public void setMaxTriggeredScanSpeed(double maxTriggeredScanSpeed) {
        this.maxTriggeredScanSpeed = maxTriggeredScanSpeed;
        deviceManagerLogger.info("Set maxTriggeredScanSpeed as " + maxTriggeredScanSpeed);
    }

    public void updateMaxTriggeredScanSpeed() {
        double exp;
        try {
            exp = core_.getExposure();
            setMaxTriggeredScanSpeed(calculateMaxTriggeredScanSpeed(exp));
        } catch (Exception e){
            deviceManagerLogger.severe(
                    "Failed to get current exposure with: " + e.getMessage());
        }
    }
    
    /** calculateMaxTriggeredScanSpeed takes custom exposure time 
     * as input
     * @param expMs 
     * @return max triggered scan speed for given exposure
     */
    public double calculateMaxTriggeredScanSpeed(double expMs) {
        try {
            return ((getMirrorTriggerDistance()/getCameraReadoutTime(expMs))*
                            getScanSpeedSafetyFactor());
        } catch (Exception e){
            deviceManagerLogger.severe("Failed to update max scan speed "
                    + "for triggering with " + e.getMessage());
            return 0;
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

    // just returns result of update 
    public double getMaxGlobalTriggeredScanSpeed() {
        upateMaxGlobalTriggeredScanSpeed();
        return maxGlobalTriggeredScanSpeed;
    }

    public void upateMaxGlobalTriggeredScanSpeed(){
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
                calculateMaxTriggeredScanSpeed(maxExp);
        deviceManagerLogger.info("globalMaxScanSpeed: " + globalMaxScanSpeed);
        setMaxGlobalTriggeredScanSpeed(globalMaxScanSpeed);
    }
    
    private void setMaxGlobalTriggeredScanSpeed(double globalMaxScanSpeed){
        maxGlobalTriggeredScanSpeed = globalMaxScanSpeed;
    }
    

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
        deviceManagerLogger.info("Set triggerDistance to " + mirrorTriggerDistance);
        updateMaxTriggeredScanSpeed();  // the current max speed based on current exposure time
        upateMaxGlobalTriggeredScanSpeed();  // the lowest max speed across all channels/exposure times
    }

    public double getXyStageScanLength() {
        return xyStageScanLength;
    }

    public void setXyStageScanLength(double xyStageScanLength) {
        this.xyStageScanLength = xyStageScanLength;
    }

    public double getXyStageTriggerDistance() {
        return xyStageTriggerDistance;
    }

    public void setXyStageTriggerDistance(double xyStageTriggerDistance) {
        this.xyStageTriggerDistance = xyStageTriggerDistance;
        updateMaxTriggeredScanSpeed();
        upateMaxGlobalTriggeredScanSpeed();
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
        upateMaxGlobalTriggeredScanSpeed();
        updateMaxTriggeredScanSpeed();
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

    public void setXyStageName(String xyStageName) {
        if (!xyStageName.equals("")) {
            this.xyStageName = xyStageName;
            deviceManagerLogger.info("Set XY stage name to " + xyStageName);
            // get port from property, note that resetting stagename will set COM port to ""
            setXyStageComPort(getPort(xyStageName));
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
        
        double maxGlobalScanSpeed = getMaxGlobalTriggeredScanSpeed();
        if (xyStageGlobalScanSpeed > maxGlobalScanSpeed){
            this.xyStageGlobalScanSpeed = maxGlobalScanSpeed;
        } else {
            this.xyStageGlobalScanSpeed = xyStageGlobalScanSpeed;
        }
        
    }
     
    public String getXyStageComPort() {
        return xyStageComPort;
    }

    public void setXyStageComPort(String xyStageComPort) {
        if (!xyStageComPort.equals("")) this.xyStageComPort = xyStageComPort;
    }
    
    public boolean getUseMaxScanSpeedForXyStage() {
        return useMaxScanSpeedForXyStage;
    }

    public void setUseMaxScanSpeedForXyStage(boolean useMaxScanSpeedForXyStage) {
        this.useMaxScanSpeedForXyStage = useMaxScanSpeedForXyStage;

    }
    

    public String getMirrorStageName() {
        return mirrorStageName;
    }

    public void setMirrorStageName(String mirrorStageName) {
        if (!mirrorStageName.equals("")){
            this.mirrorStageName = mirrorStageName;
            deviceManagerLogger.info("Set mirror stage device name to " 
                    + mirrorStageName);
            setMirrorStageComPort(getPort(mirrorStageName));
        }
    }

    public double getMirrorStageTravelSpeed() {
        return mirrorStageTravelSpeed;
    }

    public void setMirrorStageTravelSpeed(double mirrorStageTravelSpeed) {
        this.mirrorStageTravelSpeed = mirrorStageTravelSpeed;
        // will move this stuff to backend...
        // DeviceManager.setMirrorStageSpeed(mirrorStageTravelSpeed);
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
        double maxGlobalScanSpeed = getMaxGlobalTriggeredScanSpeed();
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

    public void setMirrorStageComPort(String mirrorStageComPort) {
        if (!mirrorStageComPort.equals("")) this.mirrorStageComPort = mirrorStageComPort;
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

    public void setOpmAngle(double opmAngle) {
        this.opmAngle = opmAngle;
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

    public void setZStageName(String zStageName) {
        if (!zStageName.equals("")){
            this.zStageName = zStageName;
            setZStageComPort(getPort(zStageName));
        }
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

    public void setZStageComPort(String zStageComPort) {
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
        updateMaxTriggeredScanSpeed();
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
        updateMaxTriggeredScanSpeed();
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
        if (checkInDeviceList(laserDeviceNames)){
            this.laserDeviceNames = laserDeviceNames;
            deviceManagerLogger.info("Laser set to" + laserDeviceNames);
        }
    }

    public List<String> getLaserBlankingDOLines() {
        return laserBlankingDOLines;
    }

    public void setLaserBlankingDOLines(List<String> laserBlankingDOLines) throws Exception{
        if (checkInDeviceList(getLaserBlankingDOport())){
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

    public void setLaserBlankingDOport(String laserBlankingDOport) {
        this.laserBlankingDOport = laserBlankingDOport;
    }

    public List<String> getLaserPowerAOports() {
        return laserPowerAOports;
    }

    public void setLaserPowerAOports(List<String> laserPowerAOports) {
        this.laserPowerAOports = laserPowerAOports;
    }

    public String getFilterDeviceName() {
        return filterDeviceName;
    }

    public void setFilterDeviceName(String filterDeviceName) {
        if (checkInDeviceList(filterDeviceName)){
            this.filterDeviceName = filterDeviceName;
            deviceManagerLogger.info("Filter device set to " + filterDeviceName);

        }
    }

    public String getLeftCameraName() {
        return leftCameraName;
    }

    public void setLeftCameraName(String leftCameraName) {
        if (checkInDeviceList(leftCameraName)){
            this.leftCameraName = leftCameraName;
            deviceManagerLogger.info("Camera set to" + leftCameraName);
        }
    }

    
    
    public StrVector getDeviceList() {
        return deviceList;
    }

    public void setDeviceList(StrVector deviceList) {
        this.deviceList = deviceList;
    }

    public List<DeviceDetails> getDetailsOfDevicesInUse() {
        return detailsOfDevicesInUse;
    }

    public void setDetailsOfDevicesInUse(List<DeviceDetails> detailsOfDevicesInUse) {
        this.detailsOfDevicesInUse = detailsOfDevicesInUse;
    }

    public void resetDeviceSettings(List<DeviceDetails> detailsOfDevicesInUse){
        /* given the details of devices, their properties and values saved in 
        detailsOfDevicesInUse, settings are restored with this method*/
        int nDevices = detailsOfDevicesInUse.size();
        for (int n=0; n<nDevices; n++){
            String deviceName = detailsOfDevicesInUse.get(n).name;
            String[] properties = detailsOfDevicesInUse.get(n).propertyNames;
            String[] propertyValues = detailsOfDevicesInUse.get(n).propertyValues;
            int nProperties = properties.length;
            for (int p_i=0; p_i<nProperties; p_i++){
                setProperty(deviceName, properties[p_i], propertyValues[p_i]);
            }
            
        }
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
    public class LaserDevice{
        private String name;
        private String lineDO;
        private String lineAO;
        
        void LaserDevice(String name, String lineDO, String lineAO){
            this.name = name;
            this.lineDO = lineDO;
            this.lineAO = lineAO;
        }
    }
    
    public class DeviceDetails{
        private String name;
        private String[] propertyNames;
        private String[] propertyValues;
        
        DeviceDetails(){
        }
        
        DeviceDetails(String deviceName){
            name = deviceName;
            try {
                propertyNames = core_.getDevicePropertyNames(name).toArray();
                for (int n=0; n<propertyNames.length; n++){
                    propertyValues[n] = getProperty(name, propertyNames[n]);
                }
            } catch (Exception e){
                deviceManagerLogger.severe(e.getMessage());
            }
        }
    }
}