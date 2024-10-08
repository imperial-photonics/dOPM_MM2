/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package dopm_mm2.GUI;

import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import org.micromanager.Studio;
import mmcorej.CMMCore;
import org.micromanager.ScriptController;
import org.micromanager.data.Datastore;
import dopm_mm2.Devices.DeviceManager;
import dopm_mm2.Runnables.PIScanRunnable;
import dopm_mm2.Runnables.PITriggerTest;
import dopm_mm2.util.MMStudioInstance;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;

/**
 *
 * @author lnr19
 */
public class dOPM_hostframe extends javax.swing.JFrame {

    static dOPM_hostframe frame_;
    public static Studio mm_ = null;
    public CMMCore core_ = null;
    private static final Logger dOPM_hostframeLogger = 
            Logger.getLogger(dOPM_hostframe.class.getName());
    
    // TODO: save this to a file
    private static final Logger rootLogger = 
        Logger.getLogger("");
    
    public Thread testPIVolumeThread;
    private boolean interruptFlag;
    private Datastore datastore;
    
    // Base folder directory and other directories
    private File baseFolderDir;
    private File settingsFolderDir;
    private File dataFolderDir;
    
    private File defaultConfigFile;
    
    private boolean saveImgToDisk;
    private boolean runnableIsRunning;
    
    // Device settings object
    DeviceManager deviceSettings;    

    //Other settings ?
    // ...
    
    //Various managers
    private ScriptController sc;
    
    public dOPM_hostframe() {
        makeDirsAndLog();
        initComponents();
    }

    public dOPM_hostframe(Studio mm) {        
        // singleton which contains studio and core so i dont have to inject every time
        MMStudioInstance.initialize(mm);
        mm_ = mm;
        core_ = MMStudioInstance.getCore();

        frame_ = this;
        frame_.setTitle("dOPM controller for Micro-manager 2");
        frame_.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
                
        saveImgToDisk = false;
        baseFolderDir = new File("C:\\Users\\CRICKOPMuser\\Documents\\Leo\\micromanager");
        dataFolderDir = new File(baseFolderDir, "data");
        settingsFolderDir = new File(baseFolderDir, "settings");
        
        defaultConfigFile = new File("C:\\Users\\CRICKOPMuser\\Documents\\" + 
                "Leo\\micromanager\\dopm_plugin\\deviceConfigDaqTest.csv").getAbsoluteFile();
                
        deviceSettings = new DeviceManager(core_);
        deviceSettings.loadDeviceNames(defaultConfigFile);
        
        runnableIsRunning = false;
        
        makeDirsAndLog();
        initComponents();
        
        dOPM_hostframeLogger.info("Initialised dOPM_MM2 hostframe");

    }
    
    private int makeDirsAndLog(){
        try { 
            LocalDateTime date = LocalDateTime.now(); // Create a date object
            DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern(
                    "yyyyMMddhhmmss");

            String formattedDate = date.format(myFormatObj);
            
            String appdata = System.getenv("APPDATA");
            File dopm_mm2Logdir = new File(appdata, "../Local/Micro-Manager/dopmLogs");
            dopm_mm2Logdir.mkdir();
            File dopm_mm2Logfile = new File(dopm_mm2Logdir, String.format(
                    "dopmRootLog%s.txt", formattedDate));
            
            FileHandler fh = new FileHandler(dopm_mm2Logfile.getAbsolutePath());
 
            rootLogger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();  
            fh.setFormatter(formatter);  
        } catch (IOException ioe){
            rootLogger.severe("Failed to create log with " + ioe.getMessage());
            return 1;
        } catch (SecurityException se){
            rootLogger.severe("Failed to create log file " + se.getMessage());
            return 1;
        }
        return 0;
    }
        
    private Object[] getLaserChannelOptions() throws Exception{
        throw new Exception("Not implemented");
    }
    
    private void getPossibleDeviceStates(){
        // TODO delete
    }
    
    private void openDeviceConfig(){
        
    }
    
    private void tableHandler() {
        int rowCount = channelTable.getRowCount();
        int populatedRowCount = rowCount;
        
        // String[] laserChannelsInTable
        
        for (int r_i = 0; r_i < rowCount; r_i++){
            Object laser = channelTable.getValueAt(r_i, 0);
            Object power = channelTable.getValueAt(r_i, 0);
            Object filter = channelTable.getValueAt(r_i, 0);
            if (laser == null | power == null | filter == null){
                populatedRowCount = r_i + 1;
                break;
            }
        }
        // this.setLaserChannels(laserChannelsInTable);
        // this.setLaserPowers(laserPowersInTable);
        // this.setFilters(FiltersInTable);

        // getValueAt(int row, int column)
    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        cameraSettingsPanel = new javax.swing.JPanel();
        exposureTimeLabel = new javax.swing.JLabel();
        triggerModeLabel = new javax.swing.JLabel();
        exposureTimeField = new javax.swing.JTextField();
        triggerModeComboBox = new javax.swing.JComboBox<>();
        fileSettingsPanel = new javax.swing.JPanel();
        saveDirectoryLabel = new javax.swing.JLabel();
        saveDirectoryField = new javax.swing.JTextField();
        browseDirectoryField = new javax.swing.JButton();
        saveToDiskCheckBox = new javax.swing.JCheckBox();
        channelSettingsPanel = new javax.swing.JPanel();
        channelScrollPane = new javax.swing.JScrollPane();
        channelTable = new javax.swing.JTable();
        addRowButton = new javax.swing.JButton();
        removeRowButton = new javax.swing.JButton();
        addChannelComboBox = new javax.swing.JComboBox<>();
        addFilterComboBox = new javax.swing.JComboBox<>();
        addPowerField = new javax.swing.JTextField();
        startButton = new javax.swing.JButton();
        stopButton = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        debugTextArea = new javax.swing.JTextArea();
        jProgressBar = new javax.swing.JProgressBar();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        mirrorScanSettingsPanel = new javax.swing.JPanel();
        scanLengthLabel = new javax.swing.JLabel();
        scanSpeedLabel = new javax.swing.JLabel();
        scanIntervalLabel = new javax.swing.JLabel();
        scanLengthField = new javax.swing.JTextField();
        scanSpeedField = new javax.swing.JTextField();
        scanIntervalField = new javax.swing.JTextField();
        maxSpeedCheckBox = new javax.swing.JCheckBox();
        xyStageScanSettingsPanel = new javax.swing.JPanel();
        snapTestButton = new javax.swing.JButton();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        openDeviceConfigMenuItem = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        cameraSettingsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Camera settings", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

        exposureTimeLabel.setText("Exposure time (ms)");

        triggerModeLabel.setText("Trigger mode");

        exposureTimeField.setText(String.format("%.1f", deviceSettings.getExposureTime()));
        exposureTimeField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exposureTimeFieldActionPerformed(evt);
            }
        });

        triggerModeComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "External trigger (global exposure)", "External trigger (rolling)", "Untriggered" }));
        triggerModeComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                triggerModeComboBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout cameraSettingsPanelLayout = new javax.swing.GroupLayout(cameraSettingsPanel);
        cameraSettingsPanel.setLayout(cameraSettingsPanelLayout);
        cameraSettingsPanelLayout.setHorizontalGroup(
            cameraSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(cameraSettingsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(cameraSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(cameraSettingsPanelLayout.createSequentialGroup()
                        .addComponent(exposureTimeLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(exposureTimeField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(cameraSettingsPanelLayout.createSequentialGroup()
                        .addComponent(triggerModeLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(triggerModeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        cameraSettingsPanelLayout.setVerticalGroup(
            cameraSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(cameraSettingsPanelLayout.createSequentialGroup()
                .addGroup(cameraSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(exposureTimeLabel)
                    .addComponent(exposureTimeField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(cameraSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(triggerModeLabel)
                    .addComponent(triggerModeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(14, Short.MAX_VALUE))
        );

        fileSettingsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("File settings"));

        saveDirectoryLabel.setText("Save directory");

        saveDirectoryField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveDirectoryFieldActionPerformed(evt);
            }
        });

        browseDirectoryField.setText("Browse");
        browseDirectoryField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseDirectoryFieldActionPerformed(evt);
            }
        });

        saveToDiskCheckBox.setText("Save to disk");
        saveToDiskCheckBox.setAlignmentX(1.0F);
        saveToDiskCheckBox.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        saveToDiskCheckBox.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        saveToDiskCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveToDiskCheckBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout fileSettingsPanelLayout = new javax.swing.GroupLayout(fileSettingsPanel);
        fileSettingsPanel.setLayout(fileSettingsPanelLayout);
        fileSettingsPanelLayout.setHorizontalGroup(
            fileSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(fileSettingsPanelLayout.createSequentialGroup()
                .addGroup(fileSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(fileSettingsPanelLayout.createSequentialGroup()
                        .addGap(7, 7, 7)
                        .addComponent(saveDirectoryLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(saveDirectoryField, javax.swing.GroupLayout.PREFERRED_SIZE, 222, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(browseDirectoryField))
                    .addGroup(fileSettingsPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(saveToDiskCheckBox)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        fileSettingsPanelLayout.setVerticalGroup(
            fileSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(fileSettingsPanelLayout.createSequentialGroup()
                .addGroup(fileSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(saveDirectoryLabel)
                    .addComponent(saveDirectoryField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(browseDirectoryField))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(saveToDiskCheckBox)
                .addContainerGap())
        );

        channelSettingsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Acquisition sequencer", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

        channelTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null}
            },
            new String [] {
                "Laser ", "Power", "Filter"
            }
        ));
        channelTable.setRowSelectionAllowed(false);
        channelTable.setShowGrid(true);
        channelScrollPane.setViewportView(channelTable);

        addRowButton.setText("Add");
        addRowButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addRowButtonActionPerformed(evt);
            }
        });

        removeRowButton.setText("Remove");
        removeRowButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeRowButtonActionPerformed(evt);
            }
        });

        addChannelComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addChannelComboBoxActionPerformed(evt);
            }
        });

        addFilterComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "525" }));

        addPowerField.setText("5");

        javax.swing.GroupLayout channelSettingsPanelLayout = new javax.swing.GroupLayout(channelSettingsPanel);
        channelSettingsPanel.setLayout(channelSettingsPanelLayout);
        channelSettingsPanelLayout.setHorizontalGroup(
            channelSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(channelSettingsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(channelSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(channelSettingsPanelLayout.createSequentialGroup()
                        .addComponent(addChannelComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(addPowerField, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(addFilterComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(channelScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(channelSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(removeRowButton)
                    .addComponent(addRowButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        channelSettingsPanelLayout.setVerticalGroup(
            channelSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(channelSettingsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(channelScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(channelSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(addChannelComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(addFilterComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(addRowButton)
                    .addComponent(addPowerField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(removeRowButton)
                .addContainerGap(8, Short.MAX_VALUE))
        );

        startButton.setText("Start");
        startButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startButtonActionPerformed(evt);
            }
        });

        stopButton.setText("Stop");
        stopButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopButtonActionPerformed(evt);
            }
        });

        debugTextArea.setEditable(false);
        debugTextArea.setColumns(20);
        debugTextArea.setRows(5);
        jScrollPane1.setViewportView(debugTextArea);

        jTabbedPane1.setName("Mirror scan"); // NOI18N

        mirrorScanSettingsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Scan settings", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

        scanLengthLabel.setText("Scan length (µm)");

        scanSpeedLabel.setText("Scan speed (µm/ms)");

        scanIntervalLabel.setText("Scan interval (µm)");

        scanLengthField.setText(String.format("%.1f", deviceSettings.getMirrorScanLength()));
        scanLengthField.setInputVerifier(new typeVerifierDouble());
        scanLengthField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                scanLengthFieldActionPerformed(evt);
            }
        });

        scanSpeedField.setText(String.format("%.4f", deviceSettings.getMirrorStageScanSpeed()));
        scanSpeedField.setActionCommand("<Not Set>");
        scanSpeedField.setInputVerifier(new typeVerifierDouble());
        scanSpeedField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                scanSpeedFieldActionPerformed(evt);
            }
        });

        scanIntervalField.setText(String.format("%.2f", deviceSettings.getTriggerDistance()));
        scanIntervalField.setInputVerifier(new typeVerifierDouble());
        scanIntervalField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                scanIntervalFieldActionPerformed(evt);
            }
        });

        maxSpeedCheckBox.setText("Max");
        maxSpeedCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                maxSpeedCheckBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout mirrorScanSettingsPanelLayout = new javax.swing.GroupLayout(mirrorScanSettingsPanel);
        mirrorScanSettingsPanel.setLayout(mirrorScanSettingsPanelLayout);
        mirrorScanSettingsPanelLayout.setHorizontalGroup(
            mirrorScanSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mirrorScanSettingsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mirrorScanSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(scanSpeedLabel)
                    .addComponent(scanIntervalLabel)
                    .addComponent(scanLengthLabel))
                .addGap(18, 18, 18)
                .addGroup(mirrorScanSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(scanSpeedField)
                    .addComponent(scanIntervalField)
                    .addComponent(scanLengthField))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(maxSpeedCheckBox)
                .addContainerGap(150, Short.MAX_VALUE))
        );
        mirrorScanSettingsPanelLayout.setVerticalGroup(
            mirrorScanSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mirrorScanSettingsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mirrorScanSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(scanLengthLabel)
                    .addComponent(scanLengthField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mirrorScanSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(scanSpeedLabel)
                    .addComponent(scanSpeedField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(maxSpeedCheckBox))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mirrorScanSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(scanIntervalLabel)
                    .addComponent(scanIntervalField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Mirror scan", mirrorScanSettingsPanel);

        javax.swing.GroupLayout xyStageScanSettingsPanelLayout = new javax.swing.GroupLayout(xyStageScanSettingsPanel);
        xyStageScanSettingsPanel.setLayout(xyStageScanSettingsPanelLayout);
        xyStageScanSettingsPanelLayout.setHorizontalGroup(
            xyStageScanSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 430, Short.MAX_VALUE)
        );
        xyStageScanSettingsPanelLayout.setVerticalGroup(
            xyStageScanSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 113, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("XY Stage scan (Y)", xyStageScanSettingsPanel);

        snapTestButton.setText("snap test");
        snapTestButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                snapTestButtonActionPerformed(evt);
            }
        });

        jMenu1.setText("File");

        openDeviceConfigMenuItem.setText("Open Device Config");
        openDeviceConfigMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openDeviceConfigMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(openDeviceConfigMenuItem);

        jMenuBar1.add(jMenu1);

        jMenu2.setText("Edit");
        jMenuBar1.add(jMenu2);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(startButton)
                            .addComponent(stopButton)
                            .addComponent(snapTestButton))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jProgressBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jScrollPane1)))
                    .addComponent(fileSettingsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(channelSettingsPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(cameraSettingsPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jTabbedPane1, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cameraSettingsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(channelSettingsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(fileSettingsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 69, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, 13, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(startButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(stopButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(snapTestButton)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        channelSettingsPanel.getAccessibleContext().setAccessibleName("Acquisition sequencer");
        jTabbedPane1.getAccessibleContext().setAccessibleName("Mirror scan");
        jTabbedPane1.getAccessibleContext().setAccessibleDescription("");

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /** for max speed being displayed */
    private void updateScanSpeedField(){
        if (deviceSettings.getUseMaxScanSpeed()){
            scanSpeedField.setText(
                    String.format("%.4f", deviceSettings.getMaxTriggeredScanSpeed()));
        } else {
            scanSpeedField.setText(
                    String.format("%.4f", deviceSettings.getMirrorStageScanSpeed())); 
        }
    }
    
    
    private void maxSpeedCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_maxSpeedCheckBoxActionPerformed
        if (maxSpeedCheckBox.isSelected()){
            deviceSettings.setUseMaxScanSpeed(true);
            deviceSettings.updateMaxTriggeredScanSpeed();  // probably unneeded (remove?)
            double maxScanSpeed = deviceSettings.getMaxTriggeredScanSpeed();
            updateScanSpeedField();
        } else {
            deviceSettings.setUseMaxScanSpeed(false);
            updateScanSpeedField();
            // TODO
            // double currentScanSpeed = deviceSettings.getScanSpeed();
            // scanSpeedField.setText(String.format("%.3f", currentScanSpeed));
        }
        
    }//GEN-LAST:event_maxSpeedCheckBoxActionPerformed

    
    private void triggerModeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_triggerModeComboBoxActionPerformed
        deviceSettings.setTriggerMode(triggerModeComboBox.getSelectedIndex());
        // update the max triggered scan speed since it is affected by triggerMode
        updateScanSpeedField();

    }//GEN-LAST:event_triggerModeComboBoxActionPerformed

    private void browseDirectoryFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseDirectoryFieldActionPerformed
        JFileChooser fc = new JFileChooser(baseFolderDir);
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnVal = fc.showDialog(this, "Select");
        
        if (returnVal == JFileChooser.APPROVE_OPTION){
            baseFolderDir = fc.getSelectedFile();
            saveDirectoryField.setText(baseFolderDir.getAbsolutePath());
            setDataFolderDir(new File(baseFolderDir, "data"));
        }
    }//GEN-LAST:event_browseDirectoryFieldActionPerformed

    private void addRowButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addRowButtonActionPerformed
        DefaultTableModel model = (DefaultTableModel) channelTable.getModel();
        
        try {

        // Object laser = addChannelComboBox.getSelectedItem();
        // will throw nullptr if these are empty
            if (addChannelComboBox.getItemCount()==0 || 
                    addFilterComboBox.getItemCount() ==0 ){
                throw new ArrayIndexOutOfBoundsException(
                        "Laser channel and or filter list is empty, add these to "
                                + "the csv config (File>Open Device Config)");
            }
            
            String laserToAdd = addChannelComboBox.getSelectedItem().toString();
            String powerToAdd = addPowerField.getText();
            String filterToAdd = addFilterComboBox.getSelectedItem().toString();            
            model.addRow(new Object[]{"test1","test2","test3"});
            // model.addRow(new Object[]{laserToAdd,powerToAdd,filterToAdd});
        } catch (Exception e){
            dOPM_hostframeLogger.severe("Couldn't add row with: " + e.getMessage());
        }
    }//GEN-LAST:event_addRowButtonActionPerformed

    private void removeRowButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeRowButtonActionPerformed
        DefaultTableModel model = (DefaultTableModel) channelTable.getModel();
        int lastRow = channelTable.getRowCount()-1;
        model.removeRow(lastRow);
    }//GEN-LAST:event_removeRowButtonActionPerformed

    private void stopButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopButtonActionPerformed
        if(runnableIsRunning) this.setInterruptFlag(true);
        else this.setInterruptFlag(false);
    }//GEN-LAST:event_stopButtonActionPerformed

    private void scanLengthFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_scanLengthFieldActionPerformed
        String scanLengthStr = scanLengthField.getText();
        double scanLength = Double.parseDouble(scanLengthStr);
        dOPM_hostframeLogger.info("parsing scan length as " + scanLengthStr);
        deviceSettings.setMirrorScanLength(scanLength);
    }//GEN-LAST:event_scanLengthFieldActionPerformed

    private void startButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startButtonActionPerformed
        // run the runnable
        // PIScanRunnable mirrorScanRunnable = new PIScanRunnable(this);
        PIScanRunnable mirrorScanRunnable = new PIScanRunnable(this); 
        Thread mirrorScanRunnableThread = new Thread(mirrorScanRunnable);
            mirrorScanRunnableThread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                // Handle the uncaught exception here (e.g., log it, alert users, etc.)
                dOPM_hostframeLogger.severe("Exception in thread " + t.getName() + ": " + e.getMessage());
            }
        });        
        mirrorScanRunnableThread.start();
        setRunnableIsRunning(true);
    }//GEN-LAST:event_startButtonActionPerformed

    private void saveToDiskCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveToDiskCheckBoxActionPerformed
        if (saveToDiskCheckBox.isSelected()){
            setSaveImgToDisk(true);
        } else {
            setSaveImgToDisk(false);
        }
    }//GEN-LAST:event_saveToDiskCheckBoxActionPerformed

    
    /** At some point I need to worry about distinguishing mirror stage from XY stage */
    private void scanSpeedFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_scanSpeedFieldActionPerformed
        double scanSpeed = Double.parseDouble(scanSpeedField.getText());
        // TODO replace with input verifier
        double maxTrigScanSpeed = deviceSettings.getMaxTriggeredScanSpeed();
        if (scanSpeed > maxTrigScanSpeed){
            deviceSettings.setMirrorStageScanSpeed(maxTrigScanSpeed);
            scanSpeedField.setText(String.format("%.4f", maxTrigScanSpeed));
        }             
        else
            updateScanSpeedField();
    }//GEN-LAST:event_scanSpeedFieldActionPerformed

    private void saveDirectoryFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveDirectoryFieldActionPerformed
        setBaseFolderDir(new File(saveDirectoryField.getText()));
    }//GEN-LAST:event_saveDirectoryFieldActionPerformed

    private void addChannelComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addChannelComboBoxActionPerformed
        // TODO add your handling code here: not sure if I need any?
    }//GEN-LAST:event_addChannelComboBoxActionPerformed

    private void exposureTimeFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exposureTimeFieldActionPerformed
        deviceSettings.setExposureTime(Double.parseDouble(exposureTimeField.getText()));
        // update the max triggered scan speed since it is affected by exposureTime
        updateScanSpeedField();
        
    }//GEN-LAST:event_exposureTimeFieldActionPerformed

    private void scanIntervalFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_scanIntervalFieldActionPerformed
        deviceSettings.setTriggerDistance(Double.parseDouble(scanIntervalField.getText()));
        // update the max triggered scan speed since it is affected by triggerDistance
        updateScanSpeedField();
    }//GEN-LAST:event_scanIntervalFieldActionPerformed

    private void snapTestButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_snapTestButtonActionPerformed
        Runnable testRunnable = new PITriggerTest(core_, mm_);
        Thread testThread = new Thread(testRunnable);
        testThread.start();
    }//GEN-LAST:event_snapTestButtonActionPerformed

    private void openDeviceConfigMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openDeviceConfigMenuItemActionPerformed
        JFileChooser fc = new JFileChooser(baseFolderDir);
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int returnVal = fc.showDialog(this, "Select");

        if (returnVal == JFileChooser.APPROVE_OPTION){
            File configFile = fc.getSelectedFile();
            setDefaultConfigFile(configFile);
            deviceSettings.loadDeviceNames(configFile);
        }   
    }//GEN-LAST:event_openDeviceConfigMenuItemActionPerformed

    
    
    class typeVerifierDouble extends InputVerifier {
        @Override
        public boolean verify(JComponent input) {
            JTextField tf = (JTextField) input;
            try {
                Double.parseDouble(tf.getText());
                return true;
            } catch (Exception e) {
                dOPM_hostframeLogger.warning(e.getMessage());
                return false;
            }
        }
    } 

    class typeVerifier extends InputVerifier {
        String type;
        private typeVerifier(String type){
            this.type = type;
        }
        @Override
        public boolean verify(JComponent input) {
            JTextField tf = (JTextField) input;
            try {
                switch (type){
                case "double":
                    Double.parseDouble(tf.getText());
                    return true;
                case "int":
                    Integer.parseInt(tf.getText());
                    return true;
                case "string":
                    tf.getText(); // I mean, this is always a string.
                    return true;
                }
            } catch (Exception e) {
                dOPM_hostframeLogger.warning(String.format("invalid input for type %s. %s",
                        type, e.getMessage()));
                return false;
            }
            return false;
        }
    }

    public boolean isSaveImgToDisk() {
        return saveImgToDisk;
    }

    public void setSaveImgToDisk(boolean saveImgToDisk) {
        this.saveImgToDisk = saveImgToDisk;
    }

    public File getDefaultConfigFile() {
        return defaultConfigFile;
    }

    public void setDefaultConfigFile(File defaultConfigFile) {
        this.defaultConfigFile = defaultConfigFile;
    }

    public boolean isRunnableIsRunning() {
        return runnableIsRunning;
    }

    public void setRunnableIsRunning(boolean runnableIsRunning) {
        this.runnableIsRunning = runnableIsRunning;
    }
    
    
    public boolean getInterruptFlag() {
        return interruptFlag;
    }

    public void setInterruptFlag(boolean interruptFlag) {
        this.interruptFlag = interruptFlag;
    } 
    
    public File getBaseFolderDir() {
        return baseFolderDir;
    }

    public void setBaseFolderDir(File baseFolderDir) {
        this.baseFolderDir = baseFolderDir;
        setSettingsFolderDir(new File(baseFolderDir, "settings"));
        setDataFolderDir(new File(baseFolderDir, "data"));
    }

    public File getSettingsFolderDir() {
        return settingsFolderDir;
    }

    public void setSettingsFolderDir(File settingsFolderDir) {
        this.settingsFolderDir = settingsFolderDir;
    }

    public File getDataFolderDir() {
        return dataFolderDir;
    }

    public void setDataFolderDir(File dataFolderDir) {
        this.dataFolderDir = dataFolderDir;
    }

    public DeviceManager getDeviceSettings() {
        return deviceSettings;
    }

    public void setDeviceSettings(DeviceManager deviceSettings) {
        this.deviceSettings = deviceSettings;
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            // TODO REPLACE util.logging.level with shorthand
            dOPM_hostframeLogger.log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            dOPM_hostframeLogger.log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            dOPM_hostframeLogger.log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            dOPM_hostframeLogger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new dOPM_hostframe().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> addChannelComboBox;
    private javax.swing.JComboBox<String> addFilterComboBox;
    private javax.swing.JTextField addPowerField;
    private javax.swing.JButton addRowButton;
    private javax.swing.JButton browseDirectoryField;
    private javax.swing.JPanel cameraSettingsPanel;
    private javax.swing.JScrollPane channelScrollPane;
    private javax.swing.JPanel channelSettingsPanel;
    private javax.swing.JTable channelTable;
    private javax.swing.JTextArea debugTextArea;
    private javax.swing.JTextField exposureTimeField;
    private javax.swing.JLabel exposureTimeLabel;
    private javax.swing.JPanel fileSettingsPanel;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JProgressBar jProgressBar;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JCheckBox maxSpeedCheckBox;
    private javax.swing.JPanel mirrorScanSettingsPanel;
    private javax.swing.JMenuItem openDeviceConfigMenuItem;
    private javax.swing.JButton removeRowButton;
    private javax.swing.JTextField saveDirectoryField;
    private javax.swing.JLabel saveDirectoryLabel;
    private javax.swing.JCheckBox saveToDiskCheckBox;
    private javax.swing.JTextField scanIntervalField;
    private javax.swing.JLabel scanIntervalLabel;
    private javax.swing.JTextField scanLengthField;
    private javax.swing.JLabel scanLengthLabel;
    private javax.swing.JTextField scanSpeedField;
    private javax.swing.JLabel scanSpeedLabel;
    private javax.swing.JButton snapTestButton;
    private javax.swing.JButton startButton;
    private javax.swing.JButton stopButton;
    private javax.swing.JComboBox<String> triggerModeComboBox;
    private javax.swing.JLabel triggerModeLabel;
    private javax.swing.JPanel xyStageScanSettingsPanel;
    // End of variables declaration//GEN-END:variables
}
