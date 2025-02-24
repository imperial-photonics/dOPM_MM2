/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dopm_mm2.acquisition;

import java.util.logging.Logger;
import mmcorej.CMMCore;
import mmcorej.Configuration;
import org.micromanager.MultiStagePosition;
import org.micromanager.PropertyMap;
import org.micromanager.PropertyMaps;
import org.micromanager.Studio;
import org.micromanager.acquisition.ChannelSpec;

/**
 *
 * @author OPMuser
 */
public class MDAIterator {
    private final MDASettings mda;
    private final Studio mm_;
    private final CMMCore core_;
    
    private static final Logger mdaIteratorLogger = 
        Logger.getLogger(MDASettings.class.getName());
        
    private int currentMDAAcqIdx;
    private int totalMDAAcqPts;
    
    private int currentView;
    
    // accessed by mda.get... but nice to have them here
    private int nChannelPts;
    private int nPositionPts;
    private int nZPts;
    private int nTimePts;
    
    public MDAIterator(Studio mm) throws Exception {
        mda = new MDASettings(mm);
        mm_ = mm;
        core_ = mm.getCMMCore();
        
        nChannelPts = mda.getnChannelPts();
        nPositionPts = mda.getnPositionPts();
        nZPts = mda.getnZPts();
        nTimePts = mda.getnTimePts();
    }
    
    /** call this at the end of runnable's run() to increment the loop indices, 
     * i.g., the position index (currentAcqPositionIdx), channel index 
     * (currentAcqChannelIdx), z index (currentAcqZIdx), and time index
     * (currentAcqTimeIdx).
     **/
    public void nextAcqPoint() throws IndexOutOfBoundsException{
        if (currentMDAAcqIdx >= totalMDAAcqPts-1){
            if ((getCurrentAcqChannelIdx() != nChannelPts-1) |
                    (getCurrentAcqPositionIdx() != nPositionPts-1) |
                    (getCurrentAcqZIdx() != nZPts-1) |
                    (getCurrentAcqTimeIdx() != nTimePts-1)){
                String msg = String.format("Attempted to move to"
                        + " next point in MDA when there are no more: "
                        + "MDA point (%d/%d): nT=%d, nP=%d, nZ=%d, nC=%d.", 
                        (currentMDAAcqIdx+1), totalMDAAcqPts, 
                        nTimePts, nPositionPts, nZPts, nChannelPts);
                throw new IndexOutOfBoundsException(msg);
            }
            mdaIteratorLogger.info("Reached final point in MDA");
        } else {
            nextMDAAcqIndex();
        }
    }

    public void nextMDAAcqIndex(){
        currentMDAAcqIdx += 1;
    }
    
    // channel getter setters, dont actually need private variables other than 
    // currentMDAAcqIdx and the respective lists

    public int getCurrentAcqChannelIdx() {
        return mda.getAcqChannelIndices().get(currentMDAAcqIdx);
    }
    
    public ChannelSpec getCurrentAcqChannel() {
        return mda.getChannelSpecs().get(getCurrentAcqChannelIdx());
    }
    
    // position getter/setters

    public int getCurrentAcqPositionIdx() {
        return mda.getAcqPositionIndices().get(currentMDAAcqIdx);
    }

    public MultiStagePosition getCurrentAcqPos() {
        return mda.getMultiStagePositions().get(getCurrentAcqPositionIdx());
    }
    
    public String getCurrentAcqPositionLabel(){
        return getCurrentAcqPos().getLabel();
    }

    // z slice getter/setters

    public int getCurrentAcqZIdx() {
        return mda.getAcqZIndices().get(currentMDAAcqIdx);
    }
    
    public double getCurrentAcqZ() {
        return mda.getzSlices().get(getCurrentAcqZIdx());
    }

    // timepoint slice getter/setters

    public int getCurrentAcqTimeIdx() {
        return mda.getAcqTimeIndices().get(currentMDAAcqIdx);
    }
    
    public double getCurrentAcqTime() {
        return mda.getTimepointsMs().get(getCurrentAcqTimeIdx());
    }

    public int getCurrentView() {
        return currentView;
    }

    public void setCurrentView(int currentView) throws Exception {
        String viewString = String.format("View %d", currentView);
        // StrVector viewStates = core_.getAvailableConfigs(viewString);
        core_.setConfig("dOPM View", viewString);
        mdaIteratorLogger.info("set config to " + currentView);
        core_.waitForConfig("dOPM View", viewString);
        this.currentView = currentView;

    }

}
