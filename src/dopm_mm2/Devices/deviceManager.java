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
import javax.swing.JOptionPane;
import mmcorej.CMMCore;
import mmcorej.StrVector;
import org.apache.xalan.xsltc.compiler.util.ErrorMsg;

/** deviceManager: class for handling device names in microManager configuration
 *
 * @author lnr19
 */
public class deviceManager {
    private static final Logger deviceManagerLogger = Logger.getLogger(deviceManager.class.getName()); 

    private List<String> laserDeviceNames;
    private String filterDeviceName;
    private String XYStageDeviceName;
    private String ZStageDeviceName;
    private String MirrorStageDeviceName;
    private String CameraDeviceName;
    private StrVector deviceList;
    
    public CMMCore core_ = null;
    
    public deviceManager(CMMCore cmmcore) {
        core_ = cmmcore;
        core_.getLoadedDevices();
        getAllDeviceNames();  // uMgr StrVector object
    }
    
    private void getAllDeviceNames(){
        setDeviceList(core_.getLoadedDevices());
    }
    
    public void loadDeviceNames(String configDetailsCsv){
        try (BufferedReader br = new BufferedReader(new FileReader(configDetailsCsv))) {
            List<List<String>> configData = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                configData.add(Arrays.asList(values));
            }
            setLaserDeviceNames(configData.get(0));  // row 0
            setFilterDeviceName(configData.get(1).get(0));  // row 1
            setXYStageDeviceName(configData.get(2).get(0));  // row 2
            setZStageDeviceName(configData.get(3).get(0));  // row 3
            setMirrorStageDeviceName(configData.get(4).get(0));  // row 4
            setCameraDeviceName(core_.getCameraDevice());

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
    
    
}
