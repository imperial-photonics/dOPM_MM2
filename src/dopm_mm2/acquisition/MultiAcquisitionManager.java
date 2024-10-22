/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dopm_mm2.acquisition;

import dopm_mm2.util.MMStudioInstance;
import java.util.logging.Logger;
import mmcorej.CMMCore;
import org.micromanager.MultiStagePosition;
import org.micromanager.PositionList;
import org.micromanager.Studio;
import org.micromanager.acquisition.ChannelSpec;
import org.micromanager.acquisition.SequenceSettings;

/** Similar to device manager, but stores current states, current position,
 * channel, z indices etc. Created on MDA start
 *
 * @author OPMuser
 */
public class MultiAcquisitionManager {
    private final CMMCore core_;
    private final Studio mm_;
    
    private static final Logger acquisitionManagerLogger = Logger.getLogger(
        MultiAcquisitionManager.class.getName());
    
    private PositionList positionList;
    private ChannelSpec[] channels;
    private double zSlices;
    
    private int currentAcqChannelIdx;
    private int currentAcqPosIdx;
    private int currentAcqZIdx;
    
    private ChannelSpec currentAcqChannel;
    private MultiStagePosition currentAcqPos;
    private double currentAcqZ;
    
    private String currentPosLabel;
    
    
    public MultiAcquisitionManager() {
        // get MDA settings
        core_ = MMStudioInstance.getCore();
        mm_ = MMStudioInstance.getStudio();
        SequenceSettings mdaSettings = mm_.acquisitions().
                getAcquisitionSettings();
        
    }

    public int getCurrentAcqPos() {
        return currentAcqPosIdx;
    }

    public void setCurrentAcqPos(int currentAcqPosIdx) {
        this.currentAcqPosIdx = currentAcqPosIdx;
    }

    public int getCurrentAcqZ() {
        return currentAcqZIdx;
    }

    public void setCurrentAcqZ(int currentAcqZIdx) {
        this.currentAcqZIdx = currentAcqZIdx;
    }
    
    public int getCurrentAcqChannel() {
        return currentAcqChannelIdx;
    }
    
    public void nextChannel(){
        
        int nextChan = getCurrentAcqChannelIdx() + 1;
        if (nextChan >= channels.length){
            nextChan = 0;
            acquisitionManagerLogger.info("Returning to first channel in acq list");
        }
        setCurrentAcqChannelIdx(nextChan);
    }

    public void setCurrentAcqChannel(int currentAcqChannelIdx) throws 
            IndexOutOfBoundsException {
        if (currentAcqChannelIdx >= channels.length){
            String errMsg = "Attempted to set current acquisition channel "
                    + "outside bounds of the acquisiton channel list";
            acquisitionManagerLogger.severe(errMsg);
            throw new IndexOutOfBoundsException(errMsg);
        } else {
            this.currentAcqChannelIdx = currentAcqChannelIdx;
        }
    }

    private int getCurrentAcqChannelIdx() {
        return currentAcqChannelIdx;
    }

    private void setCurrentAcqChannelIdx(int currentAcqChannelIdx) {
        this.currentAcqChannelIdx = currentAcqChannelIdx;
    }

    private int getCurrentAcqPosIdx() {
        return currentAcqPosIdx;
    }

    private void setCurrentAcqPosIdx(int currentAcqPosIdx) {
        this.currentAcqPosIdx = currentAcqPosIdx;
    }

    private int getCurrentAcqZIdx() {
        return currentAcqZIdx;
    }

    private void setCurrentAcqZIdx(int currentAcqZIdx) {
        this.currentAcqZIdx = currentAcqZIdx;
    }
    
    
    
}
