/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dopm_mm2.Runnables;

import dopm_mm2.Devices.DeviceSettingsManager;
import dopm_mm2.acquisition.MdaSettings;
import dopm_mm2.util.dialogBoxes;
import java.util.List;

import java.util.logging.Logger;
import mmcorej.CMMCore;
import mmcorej.StrVector;
import org.micromanager.Album;
import org.micromanager.display.DisplayManager;
import org.micromanager.Studio;
import org.micromanager.acquisition.ChannelSpec;
import org.micromanager.data.Datastore;
import org.micromanager.data.Image;
import org.micromanager.display.DisplayWindow;


/**
 * Runnable to snap a dOPM image for given channel preset and view.
 * Might not be necessary to make this whole runnable just for a snap but I 
 * think MMCore stuff has to happen on a different thread.
 * 
 * You might want to implement a way to change from widefield view to
 * dOPM view i.e. change light path, turret, camera etc. and back again
 * @author Leo Rowe-Brown
 */
public class opmSnap implements Runnable {
    private final CMMCore core_;
    private final Studio mm_;
    private final DeviceSettingsManager deviceSettings;
    
    private final String DAQDOPort;

    private DisplayManager displayManager;
    private static final Logger opmSnapLogger = 
            Logger.getLogger(opmSnap.class.getName());
    private DisplayWindow display;
    private Datastore store;
    private MdaSettings mda;
    
    private String channelGroup;  // get from MdaSettings
    private String viewGroup;  // should be "dOPM View"
    
    private int channelIdx;  // index of preset in channel group to image
    private int viewIdx;  // index of preset in channel group to image

    public opmSnap(Studio mm, DeviceSettingsManager deviceSettings, 
            int channelIdx, int viewIdx) {
        this.core_ = mm.getCMMCore();
        this.mm_ = mm;
        this.deviceSettings = deviceSettings;
        this.DAQDOPort = deviceSettings.getLaserBlankingDOport();

        this.channelIdx = channelIdx;
        this.viewIdx = viewIdx;
        this.viewGroup = "dOPM View";
    }
    
    @Override
    public void run(){
        opmSnapLogger.info("Snapping current OPM view");
        try {
            this.mda = new MdaSettings(mm_);

            // store = mm_.data().createRAMDatastore();
            // display = mm_.displays().createDisplay(store);
            
            String camera = deviceSettings.getdOPMCameraName();
            core_.setProperty("Core", "Camera", camera);
            
            long currentTimeMillis = System.currentTimeMillis();
            // enable blanking 
            try {
                core_.setProperty(DAQDOPort, "Blanking", "On");
            } catch (Exception e){
                dialogBoxes.acquisitionErrorWindow(e);
            }
            long timeElapsed = System.currentTimeMillis() - currentTimeMillis;
            opmSnapLogger.info("Time taken to enable blanking " + timeElapsed + " ms");

            // get channel group info
            List<ChannelSpec> chspec = mda.getChannelSpecs();
            
            if (!chspec.isEmpty()){
                channelGroup = chspec.get(0).channelGroup();
                List<String> channelNames = mda.getChannelNames();
                // set channel config  (if it exists in MDA)
                core_.setConfig(channelGroup, channelNames.get(channelIdx));
            } 
            currentTimeMillis = System.currentTimeMillis();
            // Now set dOPM view
            // get presets from dOPM View config (name is hard coded here)
            StrVector availableConfigs = core_.getAvailableConfigs("dOPM View");
            // if (availableConfigs.get(0).equals("NewPreset")) viewIdx++;
            core_.setConfig(viewGroup, availableConfigs.get(viewIdx));
            timeElapsed = System.currentTimeMillis() - currentTimeMillis;
            opmSnapLogger.info("Time taken to set view " + timeElapsed + " ms");
            currentTimeMillis = System.currentTimeMillis();
            Image img = mm_.live().snap(false).get(0);

            Album album = mm_.getAlbum();
            album.addImage(img);
            opmSnapLogger.info("Time taken to snap " + timeElapsed + " ms");

            // enable blanking 
            try {
                // switch laser off by making all DO low
                core_.setProperty(deviceSettings.getLaserBlankingDOport(), "State", "0");
                core_.setProperty(DAQDOPort, "Blanking", "Off");
            } catch (Exception e){
                dialogBoxes.acquisitionErrorWindow(e);
            }
            
        } catch (Exception e){
            opmSnapLogger.severe(e.getMessage());
        }
    }
    
}

