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

/** DeviceManager: class for handling device names in microManager configuration
 *
 * @author lnr19
 */
public class DeviceManager {
    private static final Logger deviceManagerLogger = Logger.getLogger(DeviceManager.class.getName()); 

    private List<String> laserDeviceNames;
    private String filterDeviceName;
    private String XYStageDeviceName;
    private String ZStageDeviceName;
    private String MirrorStageDeviceName;
    private String CameraDeviceName;
    private StrVector deviceList;
    private List<DeviceDetails> detailsOfDevicesInUse;

    // Device settings, states, etc.
    private String[] laserChannelsAcq;
    private String[] laserPowersAcq;
    private String[] filtersAcq;

    // Trigger settings
    private double mirrorScanLength;  // um
    private double triggerDistance;  // um
    private int triggerMode;
    private String[] triggerModeStrings;
    private boolean useMaxScanSpeed = false;
    private double scanSpeedSafetyFactor;
    private double maxTriggeredScanSpeed;  // max possible scan speed for triggering config

    private int[] z_lim;  // unused probably

    private String xyStageName;
    private double xyStageTravelSpeed; // mm/s (um/ms)
    private double xyStageScanSpeed;  //  scan speed used by stages in scanned acq--not travel
    private String xyStageComPort;

    private String mirrorStageName;
    private double mirrorStageSpeed;
    private double mirrorStageScanSpeed;  //  scan speed used by stages in scanned acq--not travel
    private String mirrorStageComPort;
    
    private String zStageName;
    private double zStageTravelSpeed;  // mm/s (um/ms)
    private String zStageComPort;
    
    private double exposureTime;  // ms
    private double actualExposureTime;
    private Rectangle frameSize;

    //Time-lapse settings, TODO
    
    public CMMCore core_ = null;
    
    public DeviceManager(){
        core_ = MMStudioInstance.getCore();
        initVars();
    }
    
    /** Constructor with CMMCore instance injected, prefer to use empty parameter version now
     *
     * @param cmmcore 
     */
    public DeviceManager(CMMCore cmmcore) {
        core_ = cmmcore;
        initVars();
        loadAllDeviceNames();  // uMgr StrVector object
    }
    
    private void initVars(){
        triggerModeStrings = new String[]
                {"External trigger (global exposure)", "External trigger (rolling)", "Untriggered"};
        z_lim = new int[]{-12000000,1000000};
        laserDeviceNames = new ArrayList(Arrays.asList(""));
        
        filterDeviceName = "";
        XYStageDeviceName = "";
        ZStageDeviceName = "";
        MirrorStageDeviceName = "";
        CameraDeviceName = "";
        deviceList = null;
        detailsOfDevicesInUse = null;

        // Device settings, states, etc.
        laserChannelsAcq = null;
        laserPowersAcq = null;
        filtersAcq = null;

        // Trigger settings
        mirrorScanLength = 50.0;  // Initialized to 50 um
        triggerDistance = 1.0;  // Initialized to 1 um
        triggerMode = 1;  // Initialized to 1 (external trigger w/ global reset)
        useMaxScanSpeed = false;  // Initialized to false
        scanSpeedSafetyFactor = 0.95;
        maxTriggeredScanSpeed = 0.01;  // safely slow

        xyStageName = ""; 
        xyStageTravelSpeed = 0.0;
        xyStageScanSpeed = 0.0;
        xyStageComPort = "";

        mirrorStageName = "";
        mirrorStageSpeed = 10.0;
        mirrorStageScanSpeed = 0.01; // safely slow
        mirrorStageComPort = "";

        zStageName = "";
        zStageTravelSpeed = 10.0;
        zStageComPort = "";

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
        *   laser,laserProperty1,laserPropery2,...,\n
        *   camera,cameraProperty,\n
        *   filter,filterProperty,\n
        *   XYStage,XYStageProperty,\n
        *   Zstage,ZstageProperty,\n
        *   mirrorStage,mirrorStageProperty,\n
        *   XYStageCOMPort,XYStageCOMPortProperty,\n
        *   ZstageCOMPort,ZstageCOMPortProperty,\n
        *   mirrorStageCOMPort,mirrorStageCOMPortProperty,\n
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
            deviceManagerLogger.info("getting laser dev");
            List<String> lasersLine = parseList(configData.get(0));
            deviceManagerLogger.info("getting filter dev");
            String filterName = (parseList(configData.get(2))).get(1);
            deviceManagerLogger.info("getting xystage dev");
            String XYStageName = (parseList(configData.get(3))).get(1);
            deviceManagerLogger.info("getting zstage dev");
            String ZStageName = (parseList(configData.get(4))).get(1);
            deviceManagerLogger.info("getting mirorstage dev");
            String mirrStageName = (parseList(configData.get(5))).get(1);
            deviceManagerLogger.info("getting XYStageCOM dev");
            String XYStageCOM = (parseList(configData.get(5))).get(1);
            deviceManagerLogger.info("getting ZStageCOM dev");
            String ZStageCOM = (parseList(configData.get(7))).get(1);
            deviceManagerLogger.info("getting mirrorStageCOM dev");
            String mirrorStageCOM = (parseList(configData.get(8))).get(1);

            setLaserDeviceNames(lasersLine);
            setCameraDeviceName(core_.getCameraDevice());
            setFilterDeviceName(filterName);
            setXYStageDeviceName(XYStageName);
            setZStageDeviceName(ZStageName);
            setMirrorStageDeviceName(mirrStageName);
            setXyStageComPort(XYStageCOM);
            setMirrorStageComPort(mirrorStageCOM);
            setZStageComPort(ZStageCOM);
            
            // now get full list of devices in use
            // start list off with the lasers, then add the rest
            List<String> devicesInUse = getLaserDeviceNames();
            devicesInUse.add(getFilterDeviceName());
            devicesInUse.add(getXYStageDeviceName());
            devicesInUse.add(getZStageDeviceName());
            devicesInUse.add(getMirrorStageDeviceName());
            devicesInUse.add(getCameraDeviceName());
            
            // then their properties, use this to revert after an acquisition for example
            List<DeviceDetails> devicesDetails = new ArrayList<DeviceDetails>();
            for (int i=0; i<devicesInUse.size(); i++){
                devicesDetails.add(new DeviceDetails(devicesInUse.get(i)));
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
        int rowLength = row.size();
        if(rowLength<=1){  // size 1 indicates the column label only
            row.add("");  // add on the value 
        }
        row = row.subList(1,rowLength);
        
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
    
    public double getCameraReadoutTime(){
        /* Get frame time in ms for camera based on the exposure time and the camera mode */
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
        double expMs = getExposureTime();  // ms
        double exp2Ms = expMs - 3.029411*1e-3;
        double minReadoutTimeMs = (Vn+1)*oneHMs;
        double readout_time = 0;
        
        switch (trigger){
            case 0:
                readout_time = ((Vn+1)*oneHMs+Math.max(0, (expMs*1e3-minReadoutTimeMs))); // in ms
                break;
            case 1: // External trigger
                readout_time = (Vn+Math.ceil(exp2Ms/oneHMs)+4)*oneHMs;  // in ms
                // readout_time = (Vn+Math.ceil(exp2/oneH)+4)*oneH;  // in ms
                break;
        }
        deviceManagerLogger.info("Calculated camera readout time as " + readout_time + " ms");
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
        return maxTriggeredScanSpeed;
    }
    
    /** not really used, should be updated by updateMaxTriggeredScanSpeed */
    public void setMaxTriggeredScanSpeed(double maxTriggeredScanSpeed) {
        this.maxTriggeredScanSpeed = maxTriggeredScanSpeed;
        deviceManagerLogger.info("Set maxTriggeredScanSpeed as " + maxTriggeredScanSpeed);
    }

    public void updateMaxTriggeredScanSpeed() {
        setMaxTriggeredScanSpeed(
                (getTriggerDistance()/getCameraReadoutTime())*getScanSpeedSafetyFactor());
    }
    
    
    public String[] getLaserChannelsAcq() {
        return laserChannelsAcq;
    }

    public void setLaserChannelsAcq(String[] laserChannels) {
        this.laserChannelsAcq = laserChannels;
    }

    public String[] getLaserPowersAcq() {
        return laserPowersAcq;
    }

    public void setLaserPowersAcq(String[] laserPowers) {
        this.laserPowersAcq = laserPowers;
    }

    public String[] getFiltersAcq() {
        return filtersAcq;
    }

    public void setFiltersAcq(String[] filters) {
        this.filtersAcq = filters;
    }

    public double getMirrorScanLength() {
        return mirrorScanLength;
    }

    public void setMirrorScanLength(double mirrorScanLength) {
        this.mirrorScanLength = mirrorScanLength;
    }

    public double getTriggerDistance() {
        return triggerDistance;
    }

    /** Trigger distance setter, in um 
     * @param triggerDistance trigger distance in um */
    public void setTriggerDistance(double triggerDistance) {
        this.triggerDistance = triggerDistance;
        deviceManagerLogger.info("Set triggerDistance to " + triggerDistance);
        updateMaxTriggeredScanSpeed();
    }

    public int getTriggerMode() {
        return triggerMode;
    }

    public void setTriggerMode(int triggerMode) {
        this.triggerMode = triggerMode;
        updateMaxTriggeredScanSpeed();
    }

    public boolean getUseMaxScanSpeed() {
        return useMaxScanSpeed;
    }

    public void setUseMaxScanSpeed(boolean useMaxScanSpeed) {
        this.useMaxScanSpeed = useMaxScanSpeed;
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

    public double getXyStageScanSpeed() {
        return xyStageScanSpeed;
    }

    public void setXyStageScanSpeed(double xyStageScanSpeed) {
        this.xyStageScanSpeed = xyStageScanSpeed;
    }
    
    public String getXyStageComPort() {
        return xyStageComPort;
    }

    public void setXyStageComPort(String xyStageComPort) {
        if (!xyStageComPort.equals("")) this.xyStageComPort = xyStageComPort;
    }

    public String getMirrorStageName() {
        return mirrorStageName;
    }

    public void setMirrorStageName(String mirrorStageName) {
        if (!mirrorStageName.equals("")){
            this.mirrorStageName = mirrorStageName;
            setMirrorStageComPort(getPort(mirrorStageName));
        }
    }

    public double getMirrorStageSpeed() {
        return mirrorStageSpeed;
    }

    public void setMirrorStageSpeed(double mirrorStageSpeed) {
        this.mirrorStageSpeed = mirrorStageSpeed;
        // will move this stuff to backend...
        // DeviceManager.setMirrorStageSpeed(mirrorStageSpeed);
    }

    public double getMirrorStageScanSpeed() {
        return mirrorStageScanSpeed;
    }

    public void setMirrorStageScanSpeed(double mirrorStageScanSpeed) {
        this.mirrorStageScanSpeed = mirrorStageScanSpeed;
    }
    

    public String getMirrorStageComPort() {
        return mirrorStageComPort;
    }

    public void setMirrorStageComPort(String mirrorStageComPort) {
        if (!mirrorStageComPort.equals("")) this.mirrorStageComPort = mirrorStageComPort;
    }
    

    public String getzStageName() {
        return zStageName;
    }

    public void setzStageName(String zStageName) {
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
        if (!laserDeviceNames.equals("")) setLaserDeviceNames(new ArrayList<>(Arrays.asList("")));
        return laserDeviceNames;
    }

    
    public void setLaserDeviceNames(List<String> laserDeviceNames) { 
        if (checkInDeviceList(laserDeviceNames)){
            this.laserDeviceNames = laserDeviceNames;
            deviceManagerLogger.info("Laser set to" + laserDeviceNames);
        }
    }

    public String getFilterDeviceName() {
        return filterDeviceName;
    }

    public void setFilterDeviceName(String filterDeviceName) {
        if (checkInDeviceList(filterDeviceName)){
            this.filterDeviceName = filterDeviceName;
            deviceManagerLogger.info("Filter device set to" + filterDeviceName);

        }
        
    }

    public String getXYStageDeviceName() {
        return XYStageDeviceName;
    }

    public void setXYStageDeviceName(String XYStageDeviceName) {
        if (checkInDeviceList(XYStageDeviceName)) {
            this.XYStageDeviceName = XYStageDeviceName;
            deviceManagerLogger.info("XYStage device set to" + XYStageDeviceName);

        }
    }

    public String getZStageDeviceName() {
        return ZStageDeviceName;
    }

    public void setZStageDeviceName(String ZStageDeviceName) {
        if (checkInDeviceList(ZStageDeviceName)){
            this.ZStageDeviceName = ZStageDeviceName;
            deviceManagerLogger.info("ZStage device set to" + ZStageDeviceName);
        }
    }

    public String getMirrorStageDeviceName() {
        return MirrorStageDeviceName;
    }

    public void setMirrorStageDeviceName(String MirrorStageDeviceName) {
        if (checkInDeviceList(MirrorStageDeviceName)) {
            this.MirrorStageDeviceName = MirrorStageDeviceName;
            deviceManagerLogger.info("Mirror stage device set to" + MirrorStageDeviceName);
        }
    }

    public String getCameraDeviceName() {
        return CameraDeviceName;
    }

    public void setCameraDeviceName(String CameraDeviceName) {
        if (checkInDeviceList(CameraDeviceName)){
            this.CameraDeviceName = CameraDeviceName;
            deviceManagerLogger.info("Camera set to" + CameraDeviceName);
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