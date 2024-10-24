/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dopm_mm2.acquisition;


import dopm_mm2.util.MMStudioInstance;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import mmcorej.CMMCore;
import java.util.stream.Collectors;
import org.micromanager.MultiStagePosition;
import org.micromanager.PositionList;
import org.micromanager.Studio;
import org.micromanager.acquisition.ChannelSpec;
import org.micromanager.acquisition.SequenceSettings;

import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableSet;
import dopm_mm2.Devices.DeviceManager;

import java.util.Set;
import java.util.stream.Stream;
import mmcorej.Configuration;
import mmcorej.PropertySetting;
import mmcorej.StrVector;
import org.micromanager.PropertyMap;
import org.micromanager.PropertyMaps;

/** Similar to device manager, but stores current states, current position,
 * channel, z indices etc. Created on MDA start
 *
 * @author OPMuser
 */
public class MDAListener {
    private final CMMCore core_;
    private final Studio mm_;
    SequenceSettings mdaSettings;
    
    private static final Logger acquisitionManagerLogger = Logger.getLogger(MDAListener.class.getName());
        
    private final int acqOrderMode;
    // private final List<String> positionLabels;
    
    private final List<MultiStagePosition> multiStagePositions;
    // private final List<Channel> channels;  // custom objs to describe channels
    private final List<ChannelSpec> channelSpecs;
    private final List<Double> zSlices;
    private final List<Double> timepointsMs;
    private List<String> acqPositionLabels;
 
    // determined by acqOrderMode
    private final int channelOrder;
    private final int positionOrder;
    private final int zOrder;
    private final int timeOrder;
    
    // full list of indices that depends on the acqOrderMode. each have the 
    // same length (npos*ntime*nchan*nz)
    private List<Integer> acqChannelIndices;
    private List<Integer> acqPositionIndices;
    private List<Integer> acqZIndices;
    private List<Integer> acqTimeIndices;
    
    private int nChannelPts;
    private int nPositionPts;
    private int nZPts;
    private int nTimePts;
    
    private int currentMDAAcqIdx;
    private int totalMDAAcqPts;
    
    private int currentAcqChannelIdx;
    private int currentAcqPositionIdx;
    private int currentAcqZIdx;
    private int currentAcqTimeIdx;
    
    private int currentView;
    private Channel channelDetails;
    private ChannelSpec currentAcqChannel;
    private MultiStagePosition currentAcqPos;
    private double currentAcqZ;
    private double currentAcqTime;
    private String currentAcqPositionLabel;
    
    /** Default constructor without device settings 
     * 
     * @throws Exception 
     */
    
    /*
    public MDAListener() 
            throws Exception {
        this(null);
        
    }*/
     /** Constructor with device settings 
     * @param deviceMgr DeviceManager object that contains device info, settings
     * @throws Exception 
     */
    public MDAListener() throws Exception {
        // get MDA settings
        core_ = MMStudioInstance.getCore();
        mm_ = MMStudioInstance.getStudio();
        
        mdaSettings = mm_.acquisitions().
                getAcquisitionSettings();
        
        // 0 - AcqOrderMode.TIME_POS_SLICE_CHANNEL 
        // 1 - AcqOrderMode.TIME_POS_CHANNEL_SLICE 
        // 2 - AcqOrderMode.POS_TIME_SLICE_CHANNEL 
        // 3 - AcqOrderMode.POS_TIME_CHANNEL_SLICE
        
        acqOrderMode = mdaSettings.acqOrderMode();
        
        zSlices = calculateZSlices();
        channelSpecs = mdaSettings.channels();
        timepointsMs = calculateTimepoints();
        multiStagePositions = retrieveMultiStagePositions();
        acqPositionLabels = retrivePositionLabels();
        
        nZPts = zSlices.size();
        nChannelPts = channelSpecs.size();
        nTimePts = timepointsMs.size();
        nPositionPts = multiStagePositions.size();
        
        acquisitionManagerLogger.info(String.format(
                "Got %d timepoints, %d (xy) positions, "
                + "%d z positions, and %d channels",
                nTimePts, nPositionPts, nZPts, nTimePts));
        
        totalMDAAcqPts = nPositionPts*nZPts*nChannelPts*nTimePts;
        
        // start at zero. currentMDAAcqIdx increments every single element of
        // the MDA, i.e. runs up to the final index equal to nt*np*nz*nc-1
        
        currentView = 0;
        currentMDAAcqIdx = 0;
        currentAcqChannelIdx = 0;
        currentAcqPositionIdx = 0;
        currentAcqZIdx = 0;
        currentAcqTimeIdx = 0;
        
        // set Dimension Orders
        
        switch(acqOrderMode){
            case 0:
                timeOrder = 0;
                positionOrder = 1;
                zOrder = 2;
                channelOrder = 3;
                break;

            case 1:
                timeOrder = 0;
                positionOrder = 1;
                channelOrder = 2;
                zOrder = 3;
                break;
                
            case 2:
                positionOrder = 0;
                timeOrder = 1;
                zOrder = 2;
                channelOrder = 3;
                break;
                
            case 3:
                positionOrder = 0;
                timeOrder = 1;
                channelOrder = 2;
                zOrder = 3;
                break;

            default:
                throw new Exception("acqOrderMode must be 0, 1, 2, or 3");
        }
        try {
            generateAcqIndices();
        } catch (Exception e){
            String err = "Failed to generate acquisition "
                    + "indices with exception:" + e.toString();
            acquisitionManagerLogger.severe(err);
            throw new Exception(err);
        }
    }
    
    private ImmutableSet getIndicesOfSet(List<?> list){
        // will be Integer (the class). copyOf so that collector is made into 
        // set, alternatively replace collect with iterator()
        int size = Math.max(1, list.size());  // so that set not empty
        return ImmutableSet.copyOf(
                IntStream.range(0, size)
                        .boxed()
                        .collect(Collectors.toSet()));
    }
    
    private void generateAcqIndices() throws Exception{
        // convert to immutable sets for Guava's cartesianProduct
        ImmutableSet<Integer> timepointsMsIdxSet = getIndicesOfSet(timepointsMs);
        ImmutableSet<Integer> multiStagePositionsIdxSet = getIndicesOfSet(multiStagePositions);
        ImmutableSet<Integer> zSlicesIdxSet = getIndicesOfSet(zSlices);
        ImmutableSet<Integer> channelSpecsIdxSet = getIndicesOfSet(channelSpecs);
       
        // vars contains the list of indices for each dimension, the order of 
        // vars is the same as the acquisition order and is given by
        // positionOrder, channelOrder, zOrder, timeOrder (each are values of
        // 0 to 3)
        //
        // I use google Guava's cartesianProduct function to generate a
        // Cartesian product of the 4 arrays and use it to define linear lists
        // of each dimension, each with length n_total = np*nc*nt*nz 
        //
        // e.g. for 2 pos, 3 chan, 1 z, 1 time (order t,p,z,c)
        // channel index = [0,1,2,0,1,2,0,1,2,0,1,2]
        // z index = [0,0,0,0,0,0,0,0,0,0,0,0]
        // position index = [0,0,0,1,1,1,0,0,0,1,1,1]
        // time index = [0,0,0,0,0,0,0,0,0,0,0,0]
        
        acquisitionManagerLogger.info("Preparing acquisition index var list");
         // Initialize with 4 null elements
        List<ImmutableSet> vars = 
                new ArrayList<>(Arrays.asList(new ImmutableSet[4]));

        // Now set elements at the required positions
        vars.set(positionOrder, multiStagePositionsIdxSet);
        vars.set(channelOrder, channelSpecsIdxSet);
        vars.set(zOrder, zSlicesIdxSet);
        vars.set(timeOrder, timepointsMsIdxSet);
        
        Set<List<Integer>> cartesianProduct;

        acquisitionManagerLogger.info("Calculating Cartesian product");
         // get cartesian product of INDICES
        cartesianProduct = Sets.cartesianProduct(
                    vars.get(0), vars.get(1), vars.get(2), vars.get(3));

        // linearize the cartesianProduct by getting the corresponding value in
        // the Set with n_total elements of Lists with 4 elements (p,c,t,z)
        acquisitionManagerLogger.info("Setting acquisition run indices");
        acqPositionIndices = cartesianProduct.stream()
                .map(idc -> idc.get(positionOrder))
                .collect(Collectors.toList());
        acqChannelIndices = cartesianProduct.stream()
                .map(idc -> idc.get(channelOrder))
                .collect(Collectors.toList());
        acqZIndices = cartesianProduct.stream()
                .map(idc -> idc.get(zOrder))
                .collect(Collectors.toList());
        acqTimeIndices = cartesianProduct.stream()
                .map(idc -> idc.get(timeOrder))
                .collect(Collectors.toList());
    }
    
    private List<Double> calculateZSlices(){
    acquisitionManagerLogger.info("Working out z slices in um");
        List<Double> zSlices;
        if (mdaSettings.useSlices()){
            double zBottomUm = mdaSettings.sliceZBottomUm();
            double zStepUm = mdaSettings.sliceZStepUm();
            double zTopUm = mdaSettings.sliceZTopUm();
            
            zSlices = IntStream.rangeClosed(0, (int)((zTopUm - zBottomUm)/zStepUm))
                           .mapToObj(z -> z*zStepUm + zBottomUm)
                           .collect(Collectors.toList());
          } else {
            zSlices = new ArrayList<>(0);
            zSlices.add(0.0);
        }
        return zSlices;
    }
    
    private List<String> retrivePositionLabels(){
        acquisitionManagerLogger.info("Getting stage positions labels");
        MultiStagePosition[] positions = mm_.positions().getPositionList().getPositions();
        List<String> posLabs;
        posLabs = Arrays.asList(positions).stream().
                map(p->p.getLabel()).collect(Collectors.toList());
        if (posLabs.isEmpty()){
            posLabs = new ArrayList<>();
            posLabs.add("");
        }
        return posLabs;

    }
    
    private List<MultiStagePosition> retrieveMultiStagePositions(){
        acquisitionManagerLogger.info("Getting stage positions");
        PositionList positionList = mm_.positions().getPositionList();
        List<MultiStagePosition> multiStagePositions_ = 
                Arrays.asList(positionList.getPositions());
        MultiStagePosition[] positions = positionList.getPositions();
        
        if (multiStagePositions_.isEmpty()){
            multiStagePositions_ = new ArrayList<>();
            multiStagePositions_.add(new MultiStagePosition());  // dummy (null)
        }
        return multiStagePositions_;
    }
    
    
    private List<Double> calculateTimepoints(){
        acquisitionManagerLogger.info("Working out timepoints in ms");
        List<Double> timepoints; 
        if (mdaSettings.useFrames()){
            double timeIntervalMs = mdaSettings.intervalMs();
            int nTimepoints = mdaSettings.numFrames();
            timepoints = IntStream.rangeClosed(0, nTimepoints)
                                       .mapToObj(t -> t * timeIntervalMs)
                                       .collect(Collectors.toList());
        } else{
            timepoints = (new ArrayList<>(0));
            timepoints.add(0.);
        }
        return timepoints;
        
    }
    
    /** Get all of the property-value pairs in the MicroManager configs and 
     * makes property map with them, useful for SummaryMetadata?
     * @throws Exception
     * @return property map of all device property settings in Groups
     */
    public PropertyMap getAllConfigGroupSettings() throws Exception{
        // empty pmap
        PropertyMap.Builder pmapBuilder = PropertyMaps.builder();
        StrVector allGroups = core_.getAvailableConfigGroups();
        // loop over all groups
        List<String> devicesInConfigs = new ArrayList<>();
        List<String> propertiesInConfigs;
        for (int n = 0; n < allGroups.size(); n++){
            String groupName = allGroups.get(n);
            Configuration config = core_.getConfigGroupState(groupName);
            for (int i = 0; i < config.size(); i++) {
                PropertySetting setting = config.getSetting(i);

                // Get the device name and property name
                String device = setting.getDeviceLabel();
                String property = setting.getPropertyName();
                String deviceAndProperty = 
                        String.format("%s-%s", device, property);
                String value = setting.getPropertyValue();
                pmapBuilder = pmapBuilder.putString(deviceAndProperty, value);
            }
        }
        return pmapBuilder.build();
    }
    
    /** Generate a propertyMap for MDA details such as channel and position 
     * probably unused... need to think about where to generate metadata: here
     * or in the abstract class
     * @return
     * @throws Exception 
     */
    public PropertyMap generatePropertyMdaMap() throws Exception{
        // Get channel group data
        ChannelSpec currentAcqChannelSpec = getCurrentAcqChannel();
        String channelPreset = currentAcqChannelSpec.config();
        double exposure = currentAcqChannelSpec.exposure();
        
        String groupName = currentAcqChannelSpec.channelGroup(); // "Channel"
        Configuration config = core_.getConfigState(groupName, channelPreset);

        // --- Future idea to get more info in metadata
        // Iterate over each PropertySetting in the Configuration object
        //
        PropertyMap pmapGroups = PropertyMaps.builder().build();
        try {
            pmapGroups = getAllConfigGroupSettings();
        } catch (Exception e){
            acquisitionManagerLogger.severe("Failed to get property map "
                    + "of device property settings in groups/presets: " 
                    + e.getMessage());
        }
        
        PropertyMap pmap = PropertyMaps.builder().
                putPropertyMap("MultiStagePosition",
                        getCurrentAcqPos().toPropertyMap()).
                putInteger("positionIdx", getCurrentAcqPositionIdx()).
                putInteger("channelIdx", getCurrentAcqChannelIdx()).
                putInteger("zIdx", getCurrentAcqZIdx()).
                putInteger("timeIdx", getCurrentAcqTimeIdx()).
                putDouble("exposureMs", exposure).
                putString("channel", channelPreset).
                putAll(pmapGroups).
                build();
        
        return pmap;
    }
    
    /** Channel details: laser, filter, etc. TODO
     *  sort of alternative to channelSpec, not implemented might not be used
     */
    class Channel{
        private String filter;
        private String laserLabel;
        private int laserIdx;
        private double laserPower;
        
        /** UNUSED
         * @param groupName Channel group name
         * @param channelPreset channel preset name in Channel group (e.g. 488)
         */
        public Channel(String groupName, String channelPreset) {
        }
        
    }
    
    /** call this at the end of runnable's run() to increment the loop indices, 
     * i.g., the position index (currentAcqPositionIdx), channel index 
     * (currentAcqChannelIdx), z index (currentAcqZIdx), and time index
     * (currentAcqTimeIdx).
     **/
    public void nextAcqPoint() throws IndexOutOfBoundsException{
        if (currentMDAAcqIdx >= totalMDAAcqPts-1){
            if ((currentAcqChannelIdx != nChannelPts-1) |
                    (currentAcqPositionIdx != nPositionPts-1) |
                    (currentAcqZIdx != nZPts-1) |
                    (currentAcqTimeIdx != nTimePts-1)){
                String msg = String.format("Attempted to move to"
                        + " next point in MDA when there are no more: "
                        + "MDA point (%d/%d): nT=%d, nP=%d, nZ=%d, nC=%d.", 
                        (currentMDAAcqIdx+1), totalMDAAcqPts, 
                        nTimePts, nPositionPts, nZPts, nChannelPts);
                throw new IndexOutOfBoundsException(msg);
            }
            acquisitionManagerLogger.info("Reached final point in MDA");
        } else {
            nextMDAAcqIndex();
            updateCurrentAcqChannel();
            updateCurrentAcqPosition();
            updateCurrentAcqZ();
            updateCurrentAcqTime();
        }
    }

    public void nextMDAAcqIndex(){
        currentMDAAcqIdx += 1;
    }
    
    // channel getter setters

    public int getCurrentAcqChannelIdx() {
        return currentAcqChannelIdx;
    }
    
    public ChannelSpec getCurrentAcqChannel() {
        return currentAcqChannel;
    }
    
    public void updateCurrentAcqChannel(){
        this.currentAcqChannelIdx = acqChannelIndices.get(currentMDAAcqIdx);
        this.currentAcqChannel = channelSpecs.get(currentAcqChannelIdx);
    }

    // position getter/setters

    public int getCurrentAcqPositionIdx() {
        return currentAcqPositionIdx;
    }

    public MultiStagePosition getCurrentAcqPos() {
        return currentAcqPos;
    }
    
    public String getCurrentAcqPositionLabel(){
        return currentAcqPositionLabel;
    }
    
    public void updateCurrentAcqPosition() throws IndexOutOfBoundsException {
        this.currentAcqPositionIdx = acqPositionIndices.get(currentMDAAcqIdx);
        this.currentAcqPos = multiStagePositions.get(currentAcqPositionIdx);
        this.currentAcqPositionLabel = acqPositionLabels.get(currentAcqPositionIdx);
    }

    // z slice getter/setters

    public int getCurrentAcqZIdx() {
        return currentAcqZIdx;
    }
    
    public int getCurrentAcqZ() {
        return currentAcqZIdx;
    }

    public void updateCurrentAcqZ() {
        this.currentAcqZIdx = acqZIndices.get(currentMDAAcqIdx);
        this.currentAcqZ = zSlices.get(currentAcqZIdx);
    }

    // timepoint slice getter/setters

    public int getCurrentAcqTimeIdx() {
        return currentAcqTimeIdx;
    }
    
    public double getCurrentAcqTime() {
        return currentAcqTime;
    }
    
    public void updateCurrentAcqTime() {
        this.currentAcqTimeIdx = acqTimeIndices.get(currentMDAAcqIdx);
        this.currentAcqTime = timepointsMs.get(currentAcqTimeIdx);
    }

    public int getCurrentView() {
        return currentView;
    }

    public void setCurrentView(int currentView) throws Exception {
        String viewString = String.format("View %d", currentView);
        // StrVector viewStates = core_.getAvailableConfigs(viewString);
        core_.setConfig("dOPM View", viewString);
        core_.waitForConfig("dOPM View", viewString);
        this.currentView = currentView;
    }
}