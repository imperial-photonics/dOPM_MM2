/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package dopm_mm2.GUI;

import java.util.logging.Logger;
import javax.swing.*;
import org.micromanager.Studio;
import mmcorej.CMMCore;
import org.micromanager.ScriptController;
import org.micromanager.data.Datastore;
import dopm_mm2.Devices.DeviceSettingsManager;
import dopm_mm2.Runnables.MDARunnable;
import dopm_mm2.Runnables.opmSnap;
import dopm_mm2.util.dialogBoxes;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;
import mmcorej.StrVector;
import org.micromanager.LogManager;
import org.micromanager.acquisition.SequenceSettings;
import org.micromanager.propertymap.MutablePropertyMapView;

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
    
    private static final Logger rootLogger = 
        Logger.getLogger("");
    
    private File dopm_mm2Logdir;
    private File dopm_mm2Logfile;
    
    public Thread testPIVolumeThread;
    private boolean interruptFlag;
    private Datastore datastore;
    
    // Base folder directory and other directories
    private String baseFolderDir;
    private String settingsFolderDir;
    private String dataFolderDir;
    
    private String configFilePath;
    
    private boolean saveImgToDisk;
    private boolean runnableIsRunning;
    
    // Device settings object
    private DeviceSettingsManager deviceSettings;    
    private final MutablePropertyMapView dopmUserProfile;
    
    //Other settings ?
    // ...
    
    //Various managers
    private ScriptController sc;

    public dOPM_hostframe(Studio mm) {        
        // singleton which contains studio and core so i dont have to inject every time
        this.mm_ = mm;
        this.core_ = mm.getCMMCore();

        mm_.logs().logMessage("In the hostframe");
        dOPM_hostframeLogger.info("START OF LOG");

        frame_ = this;
        frame_.setTitle("dOPM controller for Micro-Manager 2");
        frame_.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        
        // MicroManager has "User Profiles" which store info for persistence
        // across sessions. E.g., it remembers the last hardware config you used
        // I utilise this to store info for this plugin, hence this.GetClass()
        // which is "dOPM_hostframe"
        dopmUserProfile = mm_.getUserProfile().getSettings(this.getClass());
                
        // set logging format for whole app
        System.setProperty(
            "java.util.logging.SimpleFormatter.format",
            "[%1$tF %1$tT %1$tL] [%4$-7s] %2$s %5$s %n");
                
        saveImgToDisk = true;
        
        String os = System.getProperty("os.name");
        String defaultBasedir;
        String userprofile;
        String localappdata;
        
        if (os.toLowerCase().contains("windows")) { 
            userprofile = System.getenv("USERPROFILE");
            localappdata = System.getenv("LOCALAPPDATA");
        } else {
            userprofile = System.getProperty("user.home");
            localappdata = System.getProperty("user.home");
        }
        
        makeDirsAndLog(localappdata);

        defaultBasedir = new File(
            userprofile, "dopmData").getAbsolutePath();
        
        // synax of .getString: 
        // 1st param: string to look for in config, 2nd param: default value if empty
        baseFolderDir = dopmUserProfile.getString(
                "baseFolderDir", defaultBasedir); 

        dataFolderDir = new File(baseFolderDir, "data").getAbsolutePath();
        settingsFolderDir = new File(baseFolderDir, "settings").getAbsolutePath();
        
        String defaultConfigPath = new File(System.getenv("USERPROFILE"), 
                "dopm_plugin/dopmDeviceConfig.json").getAbsolutePath();
        
        configFilePath = dopmUserProfile.getString(
                "configFilePath", defaultConfigPath);
                
        deviceSettings = new DeviceSettingsManager(mm_);
        deviceSettings.loadSystemSettings(configFilePath);
        
        runnableIsRunning = false;
        
        mm_.logs().logMessage("About to init GUI");

        initComponents();  // init GUI components
        
        try {
            ImageIcon img = new ImageIcon(".\\dopm_icon.png");
            frame_.setIconImage(img.getImage());

        } catch (Exception e){
            dOPM_hostframeLogger.warning("Failed to find dOPM_MM2 icon: " 
                    + e.getMessage());
        }
        
        dOPM_hostframeLogger.info("Initialised dOPM_MM2 hostframe");

    }
    
    private int makeDirsAndLog(String logdir){
        try { 
            LocalDateTime date = LocalDateTime.now(); // Create a date object
            DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern(
                    "yyyyMMddhhmmss");

            String formattedDate = date.format(myFormatObj);
            
            dopm_mm2Logdir = new File(logdir, "Micro-Manager/dopmLogs");
            dopm_mm2Logdir.mkdirs();
            dopm_mm2Logfile = new File(dopm_mm2Logdir, String.format(
                    "dopmRootLog%s.log", formattedDate));
            
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
        
    
    private void openDeviceConfig(){}
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        fileSettingsPanel = new javax.swing.JPanel();
        saveDirectoryLabel = new javax.swing.JLabel();
        saveDirectoryField = new javax.swing.JTextField();
        browseDirectoryField = new javax.swing.JButton();
        saveToDiskCheckBox = new javax.swing.JCheckBox();
        startButton = new javax.swing.JButton();
        stopButton = new javax.swing.JButton();
        scanSettingsPanel = new javax.swing.JPanel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        mirrorScanSettingsPanel = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        scanLengthLabel1 = new javax.swing.JLabel();
        scanIntervalLabel1 = new javax.swing.JLabel();
        scanSpeedLabel1 = new javax.swing.JLabel();
        fracOfMaxMirrorLabel = new javax.swing.JLabel();
        mirrorScanLengthField = new javax.swing.JTextField();
        mirrorScanIntervalField = new javax.swing.JTextField();
        mirrorScanSpeedField = new javax.swing.JTextField();
        fracOfMaxMirrorField = new javax.swing.JTextField();
        mirrorMaxSpeedCheckBox = new javax.swing.JCheckBox();
        xyStageScanSettingsPanel = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        scanLengthLabel = new javax.swing.JLabel();
        xyScanLengthField = new javax.swing.JTextField();
        scanIntervalLabel = new javax.swing.JLabel();
        xyScanIntervalField = new javax.swing.JTextField();
        scanSpeedLabel = new javax.swing.JLabel();
        xyScanSpeedField = new javax.swing.JTextField();
        fracOfMaxXyLabel = new javax.swing.JLabel();
        fracOfMaxXyField = new javax.swing.JTextField();
        xyMaxSpeedCheckBox = new javax.swing.JCheckBox();
        triggerModeLabel = new javax.swing.JLabel();
        triggerModeComboBox = new javax.swing.JComboBox<>();
        mirrorScanRadioButton = new javax.swing.JRadioButton();
        yScanRadioButton = new javax.swing.JRadioButton();
        scanStageTypeLabel = new javax.swing.JLabel();
        viewsLabel = new javax.swing.JLabel();
        view1CheckBox = new javax.swing.JCheckBox();
        view2CheckBox = new javax.swing.JCheckBox();
        previewPanel = new javax.swing.JPanel();
        previewViewComboBox = new javax.swing.JComboBox<>();
        previewChannelComboBox = new javax.swing.JComboBox<>();
        previewChannelLabel = new javax.swing.JLabel();
        previewViewLabel = new javax.swing.JLabel();
        snapTestButton = new javax.swing.JButton();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        openDeviceConfigMenuItem = new javax.swing.JMenuItem();
        clearLogsMenuItem = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();

        fileSettingsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("File settings"));

        saveDirectoryLabel.setText("Save directory");

        saveDirectoryField.setText(getBaseFolderDir());
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

        saveToDiskCheckBox.setSelected(true);
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

        startButton.setText("Run Acquisition");
        startButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startButtonActionPerformed(evt);
            }
        });

        stopButton.setText("Interrupt");
        stopButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopButtonActionPerformed(evt);
            }
        });

        scanSettingsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Scan settings"));

        jTabbedPane1.setBorder(javax.swing.BorderFactory.createTitledBorder(""));
        jTabbedPane1.setToolTipText("");
        jTabbedPane1.setName("Scan settings tabbedPane"); // NOI18N

        mirrorScanSettingsPanel.setPreferredSize(new java.awt.Dimension(411, 90));

        scanLengthLabel1.setText("Z' scan length (µm)");
        scanLengthLabel1.setToolTipText("Scan length in z' (normal to imaged plane direction)");
        scanLengthLabel1.setMaximumSize(new java.awt.Dimension(120, 22));
        scanLengthLabel1.setMinimumSize(new java.awt.Dimension(120, 22));
        scanLengthLabel1.setPreferredSize(new java.awt.Dimension(120, 22));

        scanIntervalLabel1.setText("Z' scan interval (µm)");
        scanIntervalLabel1.setMaximumSize(new java.awt.Dimension(120, 22));
        scanIntervalLabel1.setMinimumSize(new java.awt.Dimension(120, 22));
        scanIntervalLabel1.setPreferredSize(new java.awt.Dimension(120, 22));

        scanSpeedLabel1.setText("Scan speed (µm/ms)");
        scanSpeedLabel1.setToolTipText("Set a \"global\" scan speed for all channels, ticking Max will ignore this. Speed is physical PI stage lateral scan speed.");
        scanSpeedLabel1.setMaximumSize(new java.awt.Dimension(120, 22));
        scanSpeedLabel1.setMinimumSize(new java.awt.Dimension(120, 22));
        scanSpeedLabel1.setPreferredSize(new java.awt.Dimension(120, 22));

        fracOfMaxMirrorLabel.setText("Fraction of max speed");
        fracOfMaxMirrorLabel.setMaximumSize(new java.awt.Dimension(120, 22));
        fracOfMaxMirrorLabel.setMinimumSize(new java.awt.Dimension(120, 22));
        fracOfMaxMirrorLabel.setPreferredSize(new java.awt.Dimension(120, 22));

        mirrorScanLengthField.setText(String.format("%.1f", deviceSettings.getMirrorScanLength()));
        mirrorScanLengthField.setInputVerifier(new typeVerifierDouble());
        mirrorScanLengthField.setMaximumSize(new java.awt.Dimension(100, 22));
        mirrorScanLengthField.setMinimumSize(new java.awt.Dimension(100, 22));
        mirrorScanLengthField.setPreferredSize(new java.awt.Dimension(100, 22));
        mirrorScanLengthField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mirrorScanLengthFieldActionPerformed(evt);
            }
        });

        mirrorScanIntervalField.setText(String.format("%.2f", deviceSettings.getMirrorTriggerDistance()));
        mirrorScanIntervalField.setInputVerifier(new typeVerifierDouble());
        mirrorScanIntervalField.setMaximumSize(new java.awt.Dimension(100, 22));
        mirrorScanIntervalField.setMinimumSize(new java.awt.Dimension(100, 22));
        mirrorScanIntervalField.setPreferredSize(new java.awt.Dimension(100, 22));
        mirrorScanIntervalField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mirrorScanIntervalFieldActionPerformed(evt);
            }
        });

        mirrorScanSpeedField.setText(String.format("%.4f", deviceSettings.getMirrorStageGlobalScanSpeed()));
        mirrorScanSpeedField.setActionCommand("<Not Set>");
        mirrorScanSpeedField.setEnabled(!mirrorMaxSpeedCheckBox.isSelected());
        mirrorScanSpeedField.setInputVerifier(new typeVerifierDouble());
        mirrorScanSpeedField.setMaximumSize(new java.awt.Dimension(100, 22));
        mirrorScanSpeedField.setMinimumSize(new java.awt.Dimension(100, 22));
        mirrorScanSpeedField.setPreferredSize(new java.awt.Dimension(100, 22));
        mirrorScanSpeedField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mirrorScanSpeedFieldActionPerformed(evt);
            }
        });

        fracOfMaxMirrorField.setText(String.format("%.2f",deviceSettings.getScanSpeedSafetyFactorMirror()));
        fracOfMaxMirrorField.setToolTipText("The percentage of the maximum theoretical scan speed, 95% is recommended");
        fracOfMaxMirrorField.setEnabled(mirrorMaxSpeedCheckBox.isSelected());
        fracOfMaxMirrorField.setInputVerifier(new pcVerifier());
        fracOfMaxMirrorField.setMinimumSize(new java.awt.Dimension(50, 22));
        fracOfMaxMirrorField.setName(""); // NOI18N
        fracOfMaxMirrorField.setPreferredSize(new java.awt.Dimension(100, 22));
        fracOfMaxMirrorField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fracOfMaxMirrorFieldActionPerformed(evt);
            }
        });

        mirrorMaxSpeedCheckBox.setText("Auto calculate speed");
        mirrorMaxSpeedCheckBox.setToolTipText("Tick to use fastest possible scanning speed for given settings");
        mirrorMaxSpeedCheckBox.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        mirrorMaxSpeedCheckBox.setMaximumSize(new java.awt.Dimension(133, 22));
        mirrorMaxSpeedCheckBox.setMinimumSize(new java.awt.Dimension(133, 22));
        mirrorMaxSpeedCheckBox.setPreferredSize(new java.awt.Dimension(133, 22));
        mirrorMaxSpeedCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mirrorMaxSpeedCheckBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addGroup(jPanel3Layout.createSequentialGroup()
                            .addComponent(fracOfMaxMirrorLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(fracOfMaxMirrorField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGroup(jPanel3Layout.createSequentialGroup()
                            .addComponent(scanSpeedLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(mirrorScanSpeedField, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(jPanel3Layout.createSequentialGroup()
                            .addComponent(scanLengthLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(mirrorScanLengthField, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(jPanel3Layout.createSequentialGroup()
                            .addComponent(scanIntervalLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(mirrorScanIntervalField, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(mirrorMaxSpeedCheckBox, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(scanLengthLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(mirrorScanLengthField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(scanIntervalLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(mirrorScanIntervalField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(scanSpeedLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(mirrorScanSpeedField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(fracOfMaxMirrorField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(fracOfMaxMirrorLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(mirrorMaxSpeedCheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout mirrorScanSettingsPanelLayout = new javax.swing.GroupLayout(mirrorScanSettingsPanel);
        mirrorScanSettingsPanel.setLayout(mirrorScanSettingsPanelLayout);
        mirrorScanSettingsPanelLayout.setHorizontalGroup(
            mirrorScanSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mirrorScanSettingsPanelLayout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(142, Short.MAX_VALUE))
        );
        mirrorScanSettingsPanelLayout.setVerticalGroup(
            mirrorScanSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mirrorScanSettingsPanelLayout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Mirror scan", mirrorScanSettingsPanel);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 22, Short.MAX_VALUE)
        );

        scanLengthLabel.setText("Scan length (µm)");
        scanLengthLabel.setMaximumSize(new java.awt.Dimension(120, 22));
        scanLengthLabel.setMinimumSize(new java.awt.Dimension(120, 22));
        scanLengthLabel.setName(""); // NOI18N
        scanLengthLabel.setPreferredSize(new java.awt.Dimension(120, 22));

        xyScanLengthField.setText(String.format("%.1f", deviceSettings.getXyStageScanLength()));
        xyScanLengthField.setInputVerifier(new typeVerifierDouble());
        xyScanLengthField.setMaximumSize(new java.awt.Dimension(100, 22));
        xyScanLengthField.setMinimumSize(new java.awt.Dimension(100, 22));
        xyScanLengthField.setPreferredSize(new java.awt.Dimension(100, 22));
        xyScanLengthField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                xyScanLengthFieldActionPerformed(evt);
            }
        });

        scanIntervalLabel.setText("Scan interval (µm)");
        scanIntervalLabel.setMaximumSize(new java.awt.Dimension(120, 22));
        scanIntervalLabel.setMinimumSize(new java.awt.Dimension(120, 22));
        scanIntervalLabel.setPreferredSize(new java.awt.Dimension(120, 22));

        xyScanIntervalField.setText(String.format("%.2f", deviceSettings.getXyStageTriggerDistance()));
        xyScanIntervalField.setInputVerifier(new typeVerifierDouble());
        xyScanIntervalField.setMaximumSize(new java.awt.Dimension(100, 22));
        xyScanIntervalField.setMinimumSize(new java.awt.Dimension(100, 22));
        xyScanIntervalField.setPreferredSize(new java.awt.Dimension(100, 22));
        xyScanIntervalField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                xyScanIntervalFieldActionPerformed(evt);
            }
        });

        scanSpeedLabel.setText("Scan speed (µm/ms)");
        scanSpeedLabel.setToolTipText("Set a \"global\" scan speed for all channels, ticking Max will ignore this");
        scanSpeedLabel.setMaximumSize(new java.awt.Dimension(120, 22));
        scanSpeedLabel.setMinimumSize(new java.awt.Dimension(120, 22));
        scanSpeedLabel.setPreferredSize(new java.awt.Dimension(120, 22));

        xyScanSpeedField.setText(String.format("%.4f", deviceSettings.getXyStageCurrentScanSpeed()));
        xyScanSpeedField.setActionCommand("<Not Set>");
        xyScanSpeedField.setEnabled(!xyMaxSpeedCheckBox.isSelected());
        xyScanSpeedField.setInputVerifier(new typeVerifierDouble());
        xyScanSpeedField.setMinimumSize(new java.awt.Dimension(100, 22));
        xyScanSpeedField.setPreferredSize(new java.awt.Dimension(100, 22));
        xyScanSpeedField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                xyScanSpeedFieldActionPerformed(evt);
            }
        });

        fracOfMaxXyLabel.setText("Fraction of max speed");
        fracOfMaxXyLabel.setMaximumSize(new java.awt.Dimension(120, 22));
        fracOfMaxXyLabel.setMinimumSize(new java.awt.Dimension(120, 22));
        fracOfMaxXyLabel.setPreferredSize(new java.awt.Dimension(120, 22));

        fracOfMaxXyField.setText(String.format("%.2f",deviceSettings.getScanSpeedSafetyFactorXy()));
        fracOfMaxXyField.setToolTipText("The percentage of the maximum theoretical scan speed, 95% is recommended");
        fracOfMaxXyField.setEnabled(xyMaxSpeedCheckBox.isSelected());
        fracOfMaxXyField.setInputVerifier(new pcVerifier());
        fracOfMaxXyField.setMaximumSize(new java.awt.Dimension(100, 22));
        fracOfMaxXyField.setMinimumSize(new java.awt.Dimension(100, 22));
        fracOfMaxXyField.setPreferredSize(new java.awt.Dimension(100, 22));
        fracOfMaxXyField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fracOfMaxXyFieldActionPerformed(evt);
            }
        });

        xyMaxSpeedCheckBox.setText("Auto calculate speed");
        xyMaxSpeedCheckBox.setToolTipText("Tick to use fastest possible scanning speed for given settings");
        xyMaxSpeedCheckBox.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        xyMaxSpeedCheckBox.setMinimumSize(new java.awt.Dimension(133, 22));
        xyMaxSpeedCheckBox.setPreferredSize(new java.awt.Dimension(133, 22));
        xyMaxSpeedCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                xyMaxSpeedCheckBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addComponent(scanLengthLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(xyScanLengthField, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addComponent(scanSpeedLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(xyScanSpeedField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addComponent(scanIntervalLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(xyScanIntervalField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(fracOfMaxXyLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(fracOfMaxXyField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(xyMaxSpeedCheckBox, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(scanLengthLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(xyScanLengthField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(scanIntervalLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(xyScanIntervalField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(scanSpeedLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(xyScanSpeedField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(fracOfMaxXyField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(fracOfMaxXyLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(xyMaxSpeedCheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout xyStageScanSettingsPanelLayout = new javax.swing.GroupLayout(xyStageScanSettingsPanel);
        xyStageScanSettingsPanel.setLayout(xyStageScanSettingsPanelLayout);
        xyStageScanSettingsPanelLayout.setHorizontalGroup(
            xyStageScanSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(xyStageScanSettingsPanelLayout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(146, 146, 146)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        xyStageScanSettingsPanelLayout.setVerticalGroup(
            xyStageScanSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(xyStageScanSettingsPanelLayout.createSequentialGroup()
                .addGap(42, 42, 42)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(xyStageScanSettingsPanelLayout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("XY Stage scan", xyStageScanSettingsPanel);

        triggerModeLabel.setText("Trigger mode");

        triggerModeComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "External trigger (global reset)", "External trigger (global exposure with rolling) <NOT IMPLEMENTED>", "Untriggered <NOT IMPLEMENTED>" }));
        triggerModeComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                triggerModeComboBoxActionPerformed(evt);
            }
        });

        buttonGroup1.add(mirrorScanRadioButton);
        mirrorScanRadioButton.setText("Mirror");
        mirrorScanRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mirrorScanRadioButtonActionPerformed(evt);
            }
        });

        buttonGroup1.add(yScanRadioButton);
        yScanRadioButton.setSelected(true);
        yScanRadioButton.setText("Y Stage");
        yScanRadioButton.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        yScanRadioButton.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        yScanRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                yScanRadioButtonActionPerformed(evt);
            }
        });

        scanStageTypeLabel.setText("Scan type");

        viewsLabel.setText("Views acquired");

        view1CheckBox.setSelected(deviceSettings.isView1Imaged());
        view1CheckBox.setText("View 1");
        view1CheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                view1CheckBoxActionPerformed(evt);
            }
        });

        view2CheckBox.setSelected(deviceSettings.isView2Imaged());
        view2CheckBox.setText("View 2");
        view2CheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                view2CheckBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout scanSettingsPanelLayout = new javax.swing.GroupLayout(scanSettingsPanel);
        scanSettingsPanel.setLayout(scanSettingsPanelLayout);
        scanSettingsPanelLayout.setHorizontalGroup(
            scanSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(scanSettingsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(scanSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(scanSettingsPanelLayout.createSequentialGroup()
                        .addGroup(scanSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(viewsLabel, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(scanStageTypeLabel, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(triggerModeLabel, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(scanSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(scanSettingsPanelLayout.createSequentialGroup()
                                .addComponent(mirrorScanRadioButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(yScanRadioButton))
                            .addGroup(scanSettingsPanelLayout.createSequentialGroup()
                                .addComponent(view1CheckBox)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(view2CheckBox))
                            .addComponent(triggerModeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 208, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addContainerGap())
        );
        scanSettingsPanelLayout.setVerticalGroup(
            scanSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(scanSettingsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(scanSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(triggerModeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(triggerModeLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(scanSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(mirrorScanRadioButton)
                    .addComponent(scanStageTypeLabel)
                    .addComponent(yScanRadioButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(scanSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(scanSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(viewsLabel)
                        .addComponent(view1CheckBox))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, scanSettingsPanelLayout.createSequentialGroup()
                        .addComponent(view2CheckBox)
                        .addContainerGap())))
        );

        jTabbedPane1.getAccessibleContext().setAccessibleName("Mirror scan");

        previewPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Preview"));

        previewViewComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "View 1", "View 2" }));
        previewViewComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                previewViewComboBoxActionPerformed(evt);
            }
        });

        previewChannelComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(getChannelPresets()));
        previewChannelComboBox.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                previewChannelComboBoxFocusGained(evt);
            }
        });

        previewChannelLabel.setText("Channel");

        previewViewLabel.setText("View");

        snapTestButton.setText("Snap Image");
        snapTestButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                snapTestButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout previewPanelLayout = new javax.swing.GroupLayout(previewPanel);
        previewPanel.setLayout(previewPanelLayout);
        previewPanelLayout.setHorizontalGroup(
            previewPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(previewPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(previewChannelLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(previewChannelComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(previewViewLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(previewViewComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(snapTestButton)
                .addContainerGap())
        );
        previewPanelLayout.setVerticalGroup(
            previewPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, previewPanelLayout.createSequentialGroup()
                .addGroup(previewPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(previewChannelComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(previewChannelLabel)
                    .addComponent(previewViewLabel)
                    .addComponent(previewViewComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(snapTestButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jMenu1.setText("File");

        openDeviceConfigMenuItem.setText("Open Device Config");
        openDeviceConfigMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openDeviceConfigMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(openDeviceConfigMenuItem);

        clearLogsMenuItem.setText("Clear dOPM logs");
        clearLogsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearLogsMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(clearLogsMenuItem);

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
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(fileSettingsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(scanSettingsPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(previewPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(startButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(stopButton)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(scanSettingsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(previewPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(fileSettingsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(startButton)
                    .addComponent(stopButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    
    private void triggerModeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_triggerModeComboBoxActionPerformed
        deviceSettings.setTriggerMode(triggerModeComboBox.getSelectedIndex());
        // update the max triggered scan speed since it is affected by triggerMode
    }//GEN-LAST:event_triggerModeComboBoxActionPerformed

    private void browseDirectoryFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseDirectoryFieldActionPerformed
        JFileChooser fc = new JFileChooser(baseFolderDir);
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnVal = fc.showDialog(this, "Select");
        
        if (returnVal == JFileChooser.APPROVE_OPTION){
            setBaseFolderDir(fc.getSelectedFile().getAbsolutePath());
            // and update the GUI too
            saveDirectoryField.setText(baseFolderDir);
        }
    }//GEN-LAST:event_browseDirectoryFieldActionPerformed

    private void stopButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopButtonActionPerformed
        if(runnableIsRunning) this.setInterruptFlag(true);
        else this.setInterruptFlag(false);
    }//GEN-LAST:event_stopButtonActionPerformed

    /**
     * Launches the MDARunnable when the Start Acquisition button is clicked
     * @param evt 
     */
    private void startButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startButtonActionPerformed
        // run the runnable        
        int scanType = deviceSettings.getScanType();
                
        // Method that uses the MDA sequencer: // // // // // // // // // // //
        Runnable volumeAcqRunnable = new MDARunnable(mm_, getDeviceSettings(),
                scanType, getDataFolderDir(), isSaveImgToDisk());
        Thread volumeAcqThread = new Thread(volumeAcqRunnable);
        volumeAcqThread.start();
        setRunnableIsRunning(true);
        
        // Does this do anything now that we pass runnable to acquisitions()?
        // I was looking into getting exceptions from a separate Runnable Thread
        /*
        Thread mirrorScanRunnableThread = new Thread(mirrorScanRunnable);
            mirrorScanRunnableThread.setUncaughtExceptionHandler(
                    new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                // Handle the uncaught exception here (e.g., log it, alert users, etc.)
                dOPM_hostframeLogger.severe("Exception in thread " + t.getName() 
                        + ": " + e.getMessage());
            }
        });*/
    }//GEN-LAST:event_startButtonActionPerformed

    /**
     * Called by 'save to disk' checkbox action, updates {@link setSaveImgToDisk}
     * @param evt 
     */
    private void saveToDiskCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveToDiskCheckBoxActionPerformed
        if (saveToDiskCheckBox.isSelected()){
            setSaveImgToDisk(true);
        } else {
            setSaveImgToDisk(false);
        }
    }//GEN-LAST:event_saveToDiskCheckBoxActionPerformed

    /**
     * Called when action is performed on text field (enter is pressed),
     * updates the save directory with {@link setBaseFolderDir}
     * @param evt 
     */
    private void saveDirectoryFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveDirectoryFieldActionPerformed
        setBaseFolderDir(saveDirectoryField.getText());
    }//GEN-LAST:event_saveDirectoryFieldActionPerformed

    private void snapTestButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_snapTestButtonActionPerformed

        int chanIdx = previewChannelComboBox.getSelectedIndex();
        int viewIdx = previewViewComboBox.getSelectedIndex();
        
        Runnable opmSnapRunnable = new opmSnap(mm_, deviceSettings, chanIdx, viewIdx);
        opmSnapRunnable.run();

    }//GEN-LAST:event_snapTestButtonActionPerformed

    private void openDeviceConfigMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openDeviceConfigMenuItemActionPerformed
        JFileChooser fc = new JFileChooser(baseFolderDir);
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int returnVal = fc.showDialog(this, "Select");

        if (returnVal == JFileChooser.APPROVE_OPTION){
            String configFile = fc.getSelectedFile().getAbsolutePath();
            setConfigFilePath(configFile);
            deviceSettings.loadSystemSettings(configFile);
        }   
    }//GEN-LAST:event_openDeviceConfigMenuItemActionPerformed


    private void mirrorScanRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mirrorScanRadioButtonActionPerformed
        if (mirrorScanRadioButton.isSelected()){
            deviceSettings.setScanType(DeviceSettingsManager.MIRROR_SCAN);
        }
    }//GEN-LAST:event_mirrorScanRadioButtonActionPerformed

    private void yScanRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_yScanRadioButtonActionPerformed
        if (yScanRadioButton.isSelected()){
            deviceSettings.setScanType(DeviceSettingsManager.YSTAGE_SCAN);
        }
    }//GEN-LAST:event_yScanRadioButtonActionPerformed

    private void view2CheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_view2CheckBoxActionPerformed
        if (view2CheckBox.isSelected()){
            deviceSettings.setView2Imaged(true);
        } else deviceSettings.setView2Imaged(false);
        
    }//GEN-LAST:event_view2CheckBoxActionPerformed

    private void view1CheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_view1CheckBoxActionPerformed
        if (view1CheckBox.isSelected()){
            deviceSettings.setView1Imaged(true);
        } else deviceSettings.setView1Imaged(false);
    }//GEN-LAST:event_view1CheckBoxActionPerformed

    private void clearLogsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearLogsMenuItemActionPerformed
        if (!dialogBoxes.confirmWindow(
                "Delete dOPM logs", 
                "Are you sure you want to clear dOPM logs?")){
            return;
        }
        try {
            File[] files = dopm_mm2Logdir.listFiles((dir, name) -> 
                    name.endsWith(".log") & !name.equals(dopm_mm2Logfile.getName()));
            for(File file : files){
                file.delete();
            }
        } catch (SecurityException e){
            dOPM_hostframeLogger.severe("Failed to delete logs "
                    + "(access denied probably) with: " + e.getMessage());
            throw e;
        } catch (Exception e){
            dOPM_hostframeLogger.severe("Failed to delete logs with: " + e.getMessage());
            throw e;
        }
    }//GEN-LAST:event_clearLogsMenuItemActionPerformed

    private void mirrorMaxSpeedCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mirrorMaxSpeedCheckBoxActionPerformed
        if (mirrorMaxSpeedCheckBox.isSelected()){
            deviceSettings.setUseMaxScanSpeedForMirror(true);
            mirrorScanSpeedField.setEnabled(false);
            fracOfMaxMirrorField.setEnabled(true);
        } else {
            deviceSettings.setUseMaxScanSpeedForMirror(false);
            mirrorScanSpeedField.setEnabled(true);
            fracOfMaxMirrorField.setEnabled(false);
        }
    }//GEN-LAST:event_mirrorMaxSpeedCheckBoxActionPerformed

    private void mirrorScanIntervalFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mirrorScanIntervalFieldActionPerformed
        // TODO GET THIS COORD TRANSFORM CORRECT
        // value is supplied in normal coordinates
        double scanIntervalZprime =
                Double.parseDouble(mirrorScanIntervalField.getText());
        // convert to actual lateral scan coordinates
        double scanIntervalLateral = 
                deviceSettings.mirrorNormaltoLateralScan(scanIntervalZprime);
        dOPM_hostframeLogger.info("parsing scan length as " + 
                scanIntervalZprime + ", converted to" + scanIntervalLateral);
        // dOPM_hostframeLogger.info("set lateral mirror scan trigger dist to " + 
        //         scanIntervalLateral);
        deviceSettings.setMirrorTriggerDistance(scanIntervalLateral);
    }//GEN-LAST:event_mirrorScanIntervalFieldActionPerformed

    private void mirrorScanSpeedFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mirrorScanSpeedFieldActionPerformed
        double scanSpeedInput =
                Double.parseDouble(mirrorScanSpeedField.getText());

        // global scan speed setting
        deviceSettings.setMirrorStageGlobalScanSpeed(scanSpeedInput);
        mirrorScanSpeedField.setText(String.format("%.4f",
            deviceSettings.getMirrorStageGlobalScanSpeed()));
    }//GEN-LAST:event_mirrorScanSpeedFieldActionPerformed

    private void mirrorScanLengthFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mirrorScanLengthFieldActionPerformed
        double scanLength = 
                Double.parseDouble(mirrorScanLengthField.getText());
        // convert from z remote scan coordinates (z to mirror lateral)
        // double scanLengthLateral = deviceSettings.lateralScanToLabZ(scanLengthZprime);
        dOPM_hostframeLogger.info("parsing scan length as " + scanLength);
        deviceSettings.setMirrorScanLength(scanLength);
        // dOPM_hostframeLogger.info("set lateral mirror scan length to " + 
        //         scanLengthLateral);
    }//GEN-LAST:event_mirrorScanLengthFieldActionPerformed

    private void fracOfMaxMirrorFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fracOfMaxMirrorFieldActionPerformed
        double mirrorSafetyFactor =
                Double.parseDouble(fracOfMaxMirrorField.getText());
        deviceSettings.setScanSpeedSafetyFactorMirror(mirrorSafetyFactor);    }//GEN-LAST:event_fracOfMaxMirrorFieldActionPerformed

    private void previewViewComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_previewViewComboBoxActionPerformed
        // No code needed, the preview runnable uses the combox state directly
    }//GEN-LAST:event_previewViewComboBoxActionPerformed

    private void fracOfMaxXyFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fracOfMaxXyFieldActionPerformed
        double xySafetyFactor =
        Double.parseDouble(fracOfMaxXyField.getText());
        deviceSettings.setScanSpeedSafetyFactorXy(xySafetyFactor);
    }//GEN-LAST:event_fracOfMaxXyFieldActionPerformed

    private void xyMaxSpeedCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_xyMaxSpeedCheckBoxActionPerformed
        if (xyMaxSpeedCheckBox.isSelected()){
            deviceSettings.setUseMaxScanSpeedForXyStage(true);
            xyScanSpeedField.setEnabled(false);
            fracOfMaxXyField.setEnabled(true);
        } else {
            deviceSettings.setUseMaxScanSpeedForXyStage(false);
            xyScanSpeedField.setEnabled(true);
            fracOfMaxXyField.setEnabled(false);
        }
    }//GEN-LAST:event_xyMaxSpeedCheckBoxActionPerformed

    private void xyScanIntervalFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_xyScanIntervalFieldActionPerformed
        deviceSettings.setXyStageTriggerDistance(
            Double.parseDouble(xyScanIntervalField.getText()));
    }//GEN-LAST:event_xyScanIntervalFieldActionPerformed

    private void xyScanSpeedFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_xyScanSpeedFieldActionPerformed
        double scanSpeedInput = Double.parseDouble(xyScanSpeedField.getText());
        deviceSettings.setXyStageGlobalScanSpeed(scanSpeedInput);

        xyScanSpeedField.setText(String.format("%.4f",
            deviceSettings.getXyStageGlobalScanSpeed()));
    }//GEN-LAST:event_xyScanSpeedFieldActionPerformed

    private void xyScanLengthFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_xyScanLengthFieldActionPerformed
        double scanLength = Double.parseDouble(xyScanLengthField.getText());
        deviceSettings.setXyStageScanLength(scanLength);
    }//GEN-LAST:event_xyScanLengthFieldActionPerformed

    private void previewChannelComboBoxFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_previewChannelComboBoxFocusGained
        if (getChannelPresets().length != previewChannelComboBox.getModel().getSize()){
            previewChannelComboBox.setModel(
                    new javax.swing.DefaultComboBoxModel<>(getChannelPresets()));
        }
    }//GEN-LAST:event_previewChannelComboBoxFocusGained

    /**
     * Get name of presets used in the (current) channel Group, set in the MDA
     * used for the dropdown menu in the preview snap mode
     * @return array channel presets
     */
    private String[] getChannelPresets(){
        try {
            SequenceSettings acquisitionSettings = 
                    mm_.acquisitions().getAcquisitionSettings();
            String channelGroup = acquisitionSettings.channelGroup();
            StrVector availableConfigs = core_.getAvailableConfigs(channelGroup);
            return availableConfigs.toArray();
        } catch (Exception e){
            dOPM_hostframeLogger.warning("Couldn't get MDA settings with " 
                    + e.toString());
            return new String[]{};
        }
    }
    
    
    class pcVerifier extends InputVerifier {
        @Override
        public boolean verify(JComponent input){
            JTextField tf = (JTextField) input;
            try {
                Double pcInput = Double.valueOf(tf.getText());
                return (pcInput >= 0 && pcInput <= 1);
            } catch (Exception e) {
                dOPM_hostframeLogger.warning(e.getMessage());
                return false;
            }
        }
    }
    
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

    public String getConfigFilePath() {
        return configFilePath;
    }

    public void setConfigFilePath(String configFilePath) {
        this.configFilePath = configFilePath;
        dopmUserProfile.putString("configFilePath", configFilePath);
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
    
    public String getBaseFolderDir() {
        return baseFolderDir;
    }

    public void setBaseFolderDir(String baseFolderDir) {
        this.baseFolderDir = baseFolderDir;
        dopmUserProfile.putString(
                "baseFolderDir", baseFolderDir);
        setSettingsFolderDir(
                new File(baseFolderDir, "settings").getAbsolutePath());
        setDataFolderDir(new File(baseFolderDir, "data").getAbsolutePath());

    }

    public String getSettingsFolderDir() {
        return settingsFolderDir;
    }

    /** 
     * Set settings folder. Note: NOT USED. Intended to be where acquisition/run
     * settings were saved, but I just save that stuff in the dataFolderDir
     * @param settingsFolderDir 
     */
    public void setSettingsFolderDir(String settingsFolderDir) {
        this.settingsFolderDir = settingsFolderDir;
    }

    public String getDataFolderDir() {
        return dataFolderDir;
    }

    public void setDataFolderDir(String dataFolderDir) {
        this.dataFolderDir = dataFolderDir;
    }

    public DeviceSettingsManager getDeviceSettings() {
        return deviceSettings;
    }

    public void setDeviceSettings(DeviceSettingsManager deviceSettings) {
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
                mm_.logs().logMessage("Making hostframe");
                new dOPM_hostframe(mm_).setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton browseDirectoryField;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JMenuItem clearLogsMenuItem;
    private javax.swing.JPanel fileSettingsPanel;
    private javax.swing.JTextField fracOfMaxMirrorField;
    private javax.swing.JLabel fracOfMaxMirrorLabel;
    private javax.swing.JTextField fracOfMaxXyField;
    private javax.swing.JLabel fracOfMaxXyLabel;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JCheckBox mirrorMaxSpeedCheckBox;
    private javax.swing.JTextField mirrorScanIntervalField;
    private javax.swing.JTextField mirrorScanLengthField;
    private javax.swing.JRadioButton mirrorScanRadioButton;
    private javax.swing.JPanel mirrorScanSettingsPanel;
    private javax.swing.JTextField mirrorScanSpeedField;
    private javax.swing.JMenuItem openDeviceConfigMenuItem;
    private javax.swing.JComboBox<String> previewChannelComboBox;
    private javax.swing.JLabel previewChannelLabel;
    private javax.swing.JPanel previewPanel;
    private javax.swing.JComboBox<String> previewViewComboBox;
    private javax.swing.JLabel previewViewLabel;
    private javax.swing.JTextField saveDirectoryField;
    private javax.swing.JLabel saveDirectoryLabel;
    private javax.swing.JCheckBox saveToDiskCheckBox;
    private javax.swing.JLabel scanIntervalLabel;
    private javax.swing.JLabel scanIntervalLabel1;
    private javax.swing.JLabel scanLengthLabel;
    private javax.swing.JLabel scanLengthLabel1;
    private javax.swing.JPanel scanSettingsPanel;
    private javax.swing.JLabel scanSpeedLabel;
    private javax.swing.JLabel scanSpeedLabel1;
    private javax.swing.JLabel scanStageTypeLabel;
    private javax.swing.JButton snapTestButton;
    private javax.swing.JButton startButton;
    private javax.swing.JButton stopButton;
    private javax.swing.JComboBox<String> triggerModeComboBox;
    private javax.swing.JLabel triggerModeLabel;
    private javax.swing.JCheckBox view1CheckBox;
    private javax.swing.JCheckBox view2CheckBox;
    private javax.swing.JLabel viewsLabel;
    private javax.swing.JCheckBox xyMaxSpeedCheckBox;
    private javax.swing.JTextField xyScanIntervalField;
    private javax.swing.JTextField xyScanLengthField;
    private javax.swing.JTextField xyScanSpeedField;
    private javax.swing.JPanel xyStageScanSettingsPanel;
    private javax.swing.JRadioButton yScanRadioButton;
    // End of variables declaration//GEN-END:variables
}