/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dopm_mm2.Devices;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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
    private String CameraDeviceName = "";
    private StrVector deviceList;
    private List<DeviceDetails> detailsOfDevicesInUse;

    // Device settings, states, etc.
    private String[] laserChannelsAcq;
    private String[] laserPowersAcq;
    private String[] filtersAcq;

    // Trigger settings
    private double scanLength = 0;  // um
    private double triggerDistance = 1;  // um
    private int triggerMode = 0;
    private String[] triggerModeStrings = 
        {"External trigger (global exposure)", "External trigger (rolling)", "Untriggered"};
    private boolean useMaxScanSpeed = false;
    private double scanSpeedSafetyFactor = 0.95;
    
    private int[] z_lim = {-12000000,1000000};
    
    private String xyStageName;
    private int xyStageTravelSpeed; // mm/s (um/ms)
    private String xyStageComPort;

    private String mirrorStageName;
    private int mirrorStageSpeed;
    private String mirrorStageComPort;
    
    private String zStageName;
    private int zStageTravelSpeed;  // mm/s (um/ms)
    private String zStageComPort;
    
    private double exposureTime;  // ms
    private double actualExposureTime;
    private Rectangle frameSize;

    //Time-lapse settings, TODO
    
    public CMMCore core_ = null;
    
    public DeviceManager(){
        core_ = MMStudioInstance.getCore();
    }
    
    /** Constructor with CMMCore instance injected, prefer to use empty parameter version now
     *
     * @param cmmcore 
     */
    public DeviceManager(CMMCore cmmcore) {
        core_ = cmmcore;
        loadAllDeviceNames();  // uMgr StrVector object
    }
    
    private void loadAllDeviceNames(){
        setDeviceList(core_.getLoadedDevices());
    }
    
    public void loadDeviceNames(String configDetailsCsv){
        /* Loads device names (name in micromanager config) from CSV.
        The idea is to use a GUI to pick the devices from the list, and then
        the GUI interface allows you to save to CSV.
        */
        try (BufferedReader br = new BufferedReader(new FileReader(configDetailsCsv))) {
            List<List<String>> configData = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                configData.add(Arrays.asList(values));
            }
            // first column is the label (so use get(1), and that sublist
            List<String> lasersLine = configData.get(0);
            setLaserDeviceNames(lasersLine.subList(1,lasersLine.size()));  // row 1
            setFilterDeviceName(configData.get(1).get(1));  // row 2
            setXYStageDeviceName(configData.get(2).get(1));  // row 3
            setZStageDeviceName(configData.get(3).get(1));  // row 4
            setMirrorStageDeviceName(configData.get(4).get(1));  // row 5
            setXyStageComPort(configData.get(5).get(1));  // row 6
            setMirrorStageComPort(configData.get(6).get(1));  // row 7
            setCameraDeviceName(core_.getCameraDevice());
            
            // now get full list of devices in use
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
                    String.format("No config file found at %s",configDetailsCsv),
                    "File not found",JOptionPane.ERROR_MESSAGE);
        } catch (IOException ex){
            deviceManagerLogger.warning(ex.getMessage());
            JOptionPane.showMessageDialog(null,
                    String.format("Failed to load file at %s",configDetailsCsv),
                    "File not found",JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex){
            deviceManagerLogger.warning(ex.getMessage());
            JOptionPane.showMessageDialog(null,
                    "Failed to get device names, check if devices are loaded in "
                            + "MicroManager and is correct. Error: " + ex.getMessage(),
                    "File not found", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private boolean checkInDeviceList(String deviceName){
        return checkInDeviceList("", deviceName);
    } 
    
    private boolean checkInDeviceList(String deviceType, String deviceName){
        List<String> deviceNames = new ArrayList<>(Arrays.asList(deviceName));
        return checkInDeviceList(deviceType, deviceNames);
    }
    
    private boolean checkInDeviceList(String deviceType, List<String> deviceNames){
        ArrayList<String> allDevices = 
                new ArrayList<>(Arrays.asList(getDeviceList().toArray()));
        for(int n=0; n<deviceNames.size(); n++){
            // check if in device list

            if (!allDevices.contains(laserDeviceNames.get(n))){
                String errorMsg = String.format("%s does not exist in "
                        + "MicroManager devices for this config, please check the list of"
                        + "devices against the chosen %s device" , 
                        deviceNames.get(n), deviceType);
                deviceManagerLogger.warning(errorMsg);
                JOptionPane.showMessageDialog(null, errorMsg);
                return false;
            }
        }
        return true;
    }
    
    public double getCameraReadoutTime(){
        /* Get frame time for camera based on the exposure time and the camera mode */
        int trigger = getTriggerMode();
        //String triggerSource = core_.getProperty(getCameraDeviceName(), "TRIGGER SOURCE");

        Rectangle roi = getFrameSize();
        // oneH = 9.74439/1000;  // Orca Flash v4 1H in ms (line horizontal readout time)
        double oneH = 4.867647*1e-3; // Orca fusion, note that it's halved(ish) wrt flash
        double Vn = roi.height;
        double Hn = roi.width;
        double exp = getExposureTime();
        double exp2 = exp - 3.029411*1e-3;
        double minReadoutTime = (Vn+1)*oneH;
        double readout_time = 0;
        
        switch (trigger){
            case 0:
                readout_time = ((Vn+1)*oneH+Math.max(0, (exp*1e3-minReadoutTime)))*1e6; // in ms
                break;
            case 1: // External trigger
                readout_time = (Vn+Math.ceil(exp2/oneH)+4)*oneH;  // in ms
                break;
        }
        return readout_time;
    }
    
    // future idea, separate settings by device
    /*public class MirrorDeviceSettings{
        String stageName = getMirrorStageName();
        
        MirrorDeviceSettings(){};
    }*/
            
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

    public double getScanLength() {
        return scanLength;
    }

    public void setScanLength(double scanLength) {
        this.scanLength = scanLength;
    }

    public double getTriggerDistance() {
        return triggerDistance;
    }

    public void setTriggerDistance(double triggerDistance) {
        this.triggerDistance = triggerDistance;
    }

    public int getTriggerMode() {
        return triggerMode;
    }

    public void setTriggerMode(int triggerMode) {
        this.triggerMode = triggerMode;
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
        this.xyStageName = xyStageName;
    }

    public int getXyStageTravelSpeed() {
        return xyStageTravelSpeed;
    }

    public void setXyStageTravelSpeed(int xyStageTravelSpeed) {
        this.xyStageTravelSpeed = xyStageTravelSpeed;
    }

    public String getXyStageComPort() {
        return xyStageComPort;
    }

    public void setXyStageComPort(String xyStageComPort) {
        this.xyStageComPort = xyStageComPort;
    }

    public String getMirrorStageName() {
        return mirrorStageName;
    }

    public void setMirrorStageName(String mirrorStageName) {
        this.mirrorStageName = mirrorStageName;
    }

    public int getMirrorStageSpeed() {
        return mirrorStageSpeed;
    }

    public void setMirrorStageSpeed(int mirrorStageSpeed) {
        this.mirrorStageSpeed = mirrorStageSpeed;
        // will move this stuff to backend...
        // DeviceManager.setMirrorStageSpeed(mirrorStageSpeed);
    }

    public String getMirrorStageComPort() {
        return mirrorStageComPort;
    }

    public void setMirrorStageComPort(String mirrorStageComPort) {
        this.mirrorStageComPort = mirrorStageComPort;
    }
    

    public String getzStageName() {
        return zStageName;
    }

    public void setzStageName(String zStageName) {
        this.zStageName = zStageName;
    }

    public int getzStageTravelSpeed() {
        return zStageTravelSpeed;
    }

    public void setzStageTravelSpeed(int zStageTravelSpeed) {
        this.zStageTravelSpeed = zStageTravelSpeed;
    }

    public String getzStageComPort() {
        return zStageComPort;
    }

    public void setzStageComPort(String zStageComPort) {
        this.zStageComPort = zStageComPort;
    }

    public double getExposureTime() {
        return exposureTime;
    }

    public void setExposureTime(double exposureTime) {
        this.exposureTime = exposureTime;
    }

    public double getActualExposureTime() {
        return actualExposureTime;
    }
    
    public Rectangle getFrameSize() {
        return frameSize;
    }

    public void setFrameSize(Rectangle frameSize) {
        this.frameSize = frameSize;
    }
    
    public void setActualExposureTime(double actualExposureTime) {
        this.actualExposureTime = actualExposureTime;
    }
    
    public List<String> getLaserDeviceNames() {
        return laserDeviceNames;
    }

    
    public void setLaserDeviceNames(List<String> laserDeviceNames) { 
        if (checkInDeviceList("laser", laserDeviceNames)) this.laserDeviceNames = laserDeviceNames;
    }

    public String getFilterDeviceName() {
        return filterDeviceName;
    }

    public void setFilterDeviceName(String filterDeviceName) {
        if (checkInDeviceList("filter", filterDeviceName)) 
            this.filterDeviceName = filterDeviceName;
    }

    public String getXYStageDeviceName() {
        return XYStageDeviceName;
    }

    public void setXYStageDeviceName(String XYStageDeviceName) {
        if (checkInDeviceList("XY stage", XYStageDeviceName)) 
            this.XYStageDeviceName = XYStageDeviceName;
    }

    public String getZStageDeviceName() {
        return ZStageDeviceName;
    }

    public void setZStageDeviceName(String ZStageDeviceName) {
        if (checkInDeviceList("Z stage", ZStageDeviceName)) 
            this.ZStageDeviceName = ZStageDeviceName;
    }

    public String getMirrorStageDeviceName() {
        return MirrorStageDeviceName;
    }

    public void setMirrorStageDeviceName(String MirrorStageDeviceName) {
        if (checkInDeviceList("mirror stage", MirrorStageDeviceName)) 
            this.MirrorStageDeviceName = MirrorStageDeviceName;
    }

    public String getCameraDeviceName() {
        return CameraDeviceName;
    }

    public void setCameraDeviceName(String CameraDeviceName) {
        if (checkInDeviceList("camera", CameraDeviceName))
            this.CameraDeviceName = CameraDeviceName;
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