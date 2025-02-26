/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dopm_mm2.Devices;

import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import mmcorej.CMMCore;
import org.micromanager.Studio;

/**
 * Device control functions, not static, needs to be instantiated with CMMCore Use the functions in
 * here for hardware control, these are directly called by Runnables (which are in turn called by
 * the host_frame) 
 * 
 * Uses the static class so methods are directly accessible
 * 
 * Must be initialized before using with {@link #initialize}
 *
 * @author Leo Rowe-Brown
 */

public class PIStage {

    private static final Logger PIStageLogger
            = Logger.getLogger(PIStage.class.getName());
    
    private static Studio mm_;
    private static CMMCore core_;
    
    /**
     * Initializer to get hold of the microManager core object
     * @param mm Studio object associated with running instance of MM
     */
    public static void initialize(Studio mm) {
        if (mm_ == null) {
            mm_ = mm;
            core_ = mm.getCMMCore();
        } else {
            throw new IllegalStateException("Core has already been initialized.");
        }
    }
    
    public PIStage() {
    }

    /* The following commands control stage movement with retry (TODO) ? */
    
    /** Slightly modified setPosition for PIMag to take positions in mm 
     * (and convert to um)
     * @param device, PIMag device name in micro-manager
     * @param position, position to move stage to 
    */
    public static void setPositionMillim(String device, double position){
        try {
            core_.setPosition(device, position*1e3);
            PIStageLogger.info(String.format(
                    "Set %s position to %.4f mm",device, position));
        } catch (Exception e){
            PIStageLogger.severe(String.format(
                    "Failed to set %s to position %.4f mm with: %s",
                    device, position, e.getMessage()));
            throw new RuntimeException(e);
        }
    }
    
    /*
    The following commands wrap the PI ASCII commands for controlling the trigger output from the 
    controller (C-413). 
    These work by changing settings with the necessary command (msg), quering the setting with 
    queryMsg to check if the value that was set (along with first checking ERR?).
    Each throw PISerialExceptions under failure
     */
    
    /** check if PI stage is on target and not moving with SRG command which 
     * returns hex strings. A bit overkill but the only way.
     * 
     * EDIT: actually, if you send 05 as a hex string ((hex) 05) #5 works too
     * not sure how Mark worked this out but I saw it in the debug window
     * 
     * @param port COM port
     * @ return true if stage is ready
     */
    public static boolean checkPIMotion(String port) throws Exception{
        return checkPIMotion(port, 1, 1);
    }
    
    /**
     * 
     * @param port COM port
     * @param device number of device, when there's just one it's 1
     * @param axis axis of device, for single axis device it's 1
     * @return true if stage is ready/not moving
     * @throws Exception 
     */
    public static boolean checkPIMotion(String port, int device, int axis) throws Exception{
        String msg = String.format("SRG? %d %d", device, axis);
        String ansHex;
        String err;
        try {
            core_.setSerialPortCommand(port, msg, "\n");
            ansHex = core_.getSerialPortAnswer(port, "\n");
            core_.setSerialPortCommand(port, "ERR?", "\n");
            err = core_.getSerialPortAnswer(port, "\n");
            if (!err.equals("0")){
                PIStageLogger.severe(String.format("Error %s when checking move status with SRG", err));
                throw new Exception("SRG command: ERR? returned non-zero error " + err);
            } 
            // hex parsing
            String hexStr = (ansHex.split("="))[1];
            hexStr = hexStr.trim();
            hexStr = hexStr.replaceFirst("0x", "");
            String binaryString = 
                    Integer.toBinaryString(Integer.parseInt(hexStr, 16));

            // - from docs: 15-on target, 13-in motion (counting from zero, right to left)
            // check both: why not
            boolean onTarget = binaryString.charAt(0) == '1';
            boolean moving = binaryString.charAt(2) == '1';
            
            return (!onTarget | moving);
            
        } catch (Exception e){
            throw new Exception("Error checking stage motion with SRG with " + e.getMessage());
        }
        
        // this here is old code, on ERR the command is not recognized, so it's 
        // clearly not right. This is a bit of head-scratcher so I'm just going 
        // with SRG and parsing those hex strings...
        /*    
        Returns: movement stats (0 - motion of all axes complete) 
        #5 command is used in GCS ASCII API to indicate that the decimal 5 is being sent directly
        -- not the ASCII character, but for some reason the # char does not seem to be parsed 
        properly when I use micromanager's setSerialPortCommand so I convert decimal 5 into an ASCII

        int msgInt = 5;
        String ans;
        String err;
        String msg = String.valueOf((char)5);
        try {
            core_.setSerialPortCommand(port, msg, "\n");
            ans = core_.getSerialPortAnswer(port, "\n");
            core_.setSerialPortCommand(port, "ERR?", "\n");
            err = core_.getSerialPortAnswer(port, "\n");
            if (!err.equals("0")){
                PIStageLogger.severe(String.format("Error %s when checking move status with #5", err));
            } 
            return ans;
        } catch (Exception e){
            throw new Exception("Error checking stage motion with #5 with " + e.getMessage());
        }
        */
    }
    
    /**
     * Useful for making sure the PI trigger is low in the OR gate
     * @param port
     * @throws Exception 
     */
    public static void setPITriggerLow(String port) throws Exception {
        /* Initialise the PI stage basic trigger output settings */
        setPITriggerEnable(port, 1, 0);  // disable triggering
        setPIDigitalOut(port, 1, 0);  // set digital (trigger) out to low
    }
            
    public static void setupPITriggering(String port, int device) throws Exception {
        /* Initialise the PI stage basic trigger output settings */
        setPITriggerEnable(port, device, 0);  // disable triggering
        setPIDigitalOut(port, device, 0);  // set digital (trigger) out to low
        setPITriggerAxis(port, device, 1);  // set trigger axis to be 1 (our PI is 1-axis anyway)
        setPITriggerMode(port, device, 0);  // set trigger mode to position/distance trigger
    }
    
    public static void setPIDigitalOut(String port, int level) throws Exception {
        setPIDigitalOut(port, 1, level);
    }

    public static void setPIDigitalOut(String port, int device, int level) throws Exception {
        // First make sure triggering is off, the docs say do not use DIO when trigger enabled.
        setPITriggerEnable(port, device, 0);
        String msg = String.format("DIO %1$d %2$d", device, level);  // ASCII command to set DIO
        String queryMsg = String.format("DIO? %1$d", device);  // ASCII command to check DIO setting
        int expectedValue = level;  // kinda stupid that i assign another var but for readability
        try {
            setAndCheckSerial(port, msg, queryMsg, (double) expectedValue);
        } catch (Exception e) {
            PIStageLogger.severe("Failed to set PI trigger axis with " + e.getMessage());
            throw e;
        }
    }

    public static void setPITriggerEnable(String port, int trigOn) throws Exception {
        setPITriggerEnable(port, 1, trigOn);
    }

    /**
     * Enable the PI's trigger (1) or disable (0)
     * @param port COM port
     * @param device if one device connected, 1
     * @param trigOn 0 to disable trigger, 1 to enable
     * @throws Exception 
     */
    public static void setPITriggerEnable(String port, int device, int trigOn) throws Exception {
        // format: TRO? [{<TrigOutID>}] rtn: {<TrigOutID>"="<TrigMode> LF}
        String msg = String.format("TRO %1$d %2$d", device, trigOn);
        String queryMsg = String.format("TRO? %1$d", device);
        // reply format: {<TrigOutID> <CTOPam>"="<Value> LF}
        int expectedValue = trigOn;
        try {
            setAndCheckSerial(port, msg, queryMsg, (double) expectedValue);
        } catch (Exception e) {
            PIStageLogger.severe("Failed to set PI trigger axis with " + e.getMessage());
            throw e;
        }
    }

    public static void setPITriggerAxis(String port, int axis) throws Exception {
        setPITriggerAxis(port, 1, axis);
    }

    public static void setPITriggerAxis(String port, int device, int axis) throws Exception {
        String msg = String.format("CTO %1$d 2 %2$d", device, axis);
        String queryMsg = String.format("CTO? %1$d 2", device);
        // reply format: {<TrigOutID> <CTOPam>"="<Value> LF}
        int expectedValue = axis;
        try {
            setAndCheckSerial(port, msg, queryMsg, (double) expectedValue);
        } catch (Exception e) {
            PIStageLogger.severe("Failed to set PI trigger axis with " + e.getMessage());
            throw e;
        }
    }

    public static void setPITriggerMode(String port, int triggerMode) throws Exception {
        setPITriggerMode(port, 1, triggerMode);
    }
    
    /** 
        Sets trigger method from stage, we only use triggerMode = 0 for position interval
        triggering
        * @param port COM port for PI device e.g. "COM3"
        * @param device device controlled by PI controller, here 1
        * @param triggerMode PI trigger mode, 0-position trigger, 2-On Target 3-MinMax Threshold
        *   6-In Motion
    */
    public static void setPITriggerMode(String port, int device, int triggerMode) throws Exception {

        String msg = String.format("CTO %1$d 3 %2$d", device, triggerMode);
        String queryMsg = String.format("CTO? %1$d 3", device);
        int expectedValue = triggerMode;
        try {
            setAndCheckSerial(port, msg, queryMsg, (double) expectedValue);
        } catch (Exception e) {
            PIStageLogger.severe("Failed to set PI trigger mode with " + e.getMessage());
            throw e;
        }
    }

    /** Set trigger distance (in millimeters) 
    * @param port COM port
    * @param device PI axis device number (1 by default for 1 axis)
    * @param triggerDistance trigger distance in mm
    * @throws Exception
    */
    public static void setPITriggerDistance(String port, int device, double triggerDistance)
            throws Exception {

        // min incremental motion is 0.02um (0.00002mm, 1e-5)
        String triggerDistanceStr = String.format("%.5f", triggerDistance);
        String msg = String.format("CTO %1$d 1 %2$s", device, triggerDistanceStr);
        String queryMsg = String.format("CTO? %1$d 1", device);
        double expectedValue = triggerDistance;
        try {
            setAndCheckSerial(port, msg, queryMsg, expectedValue);
        } catch (Exception e) {
            PIStageLogger.severe("Failed to set PI trigger distance with " + e.getMessage());
            throw e;
        }
    }

    /**
     * Set distance range in which stage triggers. I use this so I can move the 
     * stage up to the range (like a run/ramp up) to deal with acceleration 
     * compressing the triggers (which can result in dropped frames.)
     * @param port
     * @param device
     * @param triggerRange
     * @throws Exception 
     */
    public static void setPITriggerRange(String port, int device, double[] triggerRange) throws Exception {

        String lowerRangeStr = String.format("%.5f", triggerRange[0]);
        String upperRangeStr = String.format("%.5f", triggerRange[1]);
        String msgLower = String.format("CTO %1$d 8 %2$s", device, lowerRangeStr);
        String msgUpper = String.format("CTO %1$d 9 %2$s", device, upperRangeStr);

        String queryMsgLower = String.format("CTO? %1$d 8", device);
        String queryMsgUpper = String.format("CTO? %1$d 9", device);

        double expectedValueLower = triggerRange[0];
        double expectedValueUpper = triggerRange[1];
        
        PIStageLogger.info(String.format("Setting trigger range to [%s, %s]", 
                lowerRangeStr, upperRangeStr));

        try {
            setAndCheckSerial(port, msgLower, queryMsgLower, expectedValueLower);
            setAndCheckSerial(port, msgUpper, queryMsgUpper, expectedValueUpper);

        } catch (Exception e) {
            String errMsg = String.format("Failed to set PI trigger range with %s", e.getMessage());
            PIStageLogger.severe(errMsg);
            throw e;
        }
    }

    public static void clearPISerialOutBuffer(String port) {
        // sometimes the buffer doesnt get read out
        int max_clears = 1000;
        int i = 0;
        while (i < max_clears) {
            try {
                core_.getSerialPortAnswer(port, "\n");
            } catch (Exception e) {
                break;
            }
            i++;
        }
    }
    
    public static void stopPIStage(String port) throws Exception{
        stopOrHaltPIStage(port, "STP");
    }

    public static void haltPIStage(String port) throws Exception{
        stopOrHaltPIStage(port, "HLT");
    }
    
    private static void stopOrHaltPIStage(String port, String msg) throws Exception{
        core_.setSerialPortCommand(port, msg, "\n");
        core_.setSerialPortCommand(port, "ERR?", "\n");
        String answerErr = core_.getSerialPortAnswer(port, "\n");
        if (!answerErr.equals("10")) throw new PIControllerErrorException(String.format(
                "Error code %1$s received from PI controller", answerErr));
    }

    public static void setAndCheckSerial(
            String port, String msg, String queryMsg, Double expectedValue)
            throws TimeoutException, IllegalStateException {

        String answer = "";
        int sleep_intvl_ms = 1000;
        int maxRetry = 5;
        boolean isSet = false;
        String ERR;
        
        int i = 0;
        do {
            try {
                PIStageLogger.info(String.format(
                        "Sending serial command %s to %s", msg, port));
                core_.setSerialPortCommand(port, msg, "\n");
                // check for errors
                core_.setSerialPortCommand(port, "ERR?", "\n");
                ERR = core_.getSerialPortAnswer(port, "\n");
                if (!ERR.equals("0")) {
                    String errMsg = String.format(
                            "Error code %1$s from ERR? after sending command %2$s", ERR, msg);
                    PIStageLogger.severe(errMsg);
                    throw new PIControllerErrorException(errMsg);
                }

                core_.setSerialPortCommand(port, queryMsg, "\n");
                answer = core_.getSerialPortAnswer(port, "\n");
                PIStageLogger.info(String.format(
                        "Received answer %s from %s", answer, port));
                // get value after being set
                double value = Double.parseDouble(answer.split("=")[1]);
                double diff = Math.abs(value - expectedValue);
                isSet = Math.abs(value - expectedValue) < 1e-6;
                PIStageLogger.info(String.format("Value set to %.6f when"
                        + " attemping to set to %.6f (difference: %.7f)", 
                        value, expectedValue, diff));

            } catch (Exception e) {
                if (i > maxRetry) {
                    throw new TimeoutException(String.format("Failed to set serial cmd after %d "
                            + "tries with exception %s", maxRetry, e.getMessage()));
                }
                try {
                    Thread.sleep(sleep_intvl_ms);
                } catch (InterruptedException ie) {
                }
            }
            i++;
        } while (!isSet && i < maxRetry);
        if (i > maxRetry){
            throw new TimeoutException(String.format("Failed to set serial cmd "
                + "after %d tries without exception--check precision?", maxRetry));
        }
    }

    // These send serial command and get serial answer retries are overkill I think... I will end 
    // up doing waits/retries outside of these functions anyway
    public static void sendSerialCommandRetry(String port, String msg) throws PISerialException {
        // Version of sendSerialCommandRetry with just the port and message as args
        sendSerialCommandRetry(port, msg, "\n", 5);
    }

    public static void sendSerialCommandRetry(
            String port, String msg, String terminator, int maxRetry) throws PISerialException {
        boolean sendSerialSuccess = false;
        int intvl_ms = 1000;
        int WAITTIME;
        int attempts = 0;
        while (!sendSerialSuccess || attempts > maxRetry) {
            WAITTIME = attempts * attempts * intvl_ms;
            try {
                core_.setSerialPortCommand(port, msg, terminator);
            } catch (Exception ex) {
                attempts++;
                PIStageLogger.warning("sendSerialCommandRetry failed (waiting "
                        + (WAITTIME / 1000) + "s) Exception:" + ex.getMessage());
                if (attempts > maxRetry) {
                    String errorMsg = "sendSerialCommandRetry failed after " + attempts
                            + " attempts, failed with " + ex.getMessage();
                    PIStageLogger.severe(errorMsg);
                    throw new PISerialException(errorMsg);
                }
                try {
                    Thread.sleep(attempts * attempts * intvl_ms);
                } catch (InterruptedException ie) {
                }

            }
        }
    }

    public static String getSerialAnswerCommandRetry(String port) throws PISerialException {
        // Version of getSerialAnswerCommandRetry with just the port as arg
        return getSerialAnswerCommandRetry(port, "\n", 5);
    }

    public static String getSerialAnswerCommandRetry(
            String port, String terminator, int maxRetry) throws PISerialException {
        boolean getSerialSuccess = false;
        int intvl_ms = 1000;
        int WAITTIME;
        int attempts = 0;
        while (!getSerialSuccess || attempts > maxRetry) {
            WAITTIME = attempts * attempts * intvl_ms;
            try {
                return core_.getSerialPortAnswer(port, terminator);
            } catch (Exception ex) {
                attempts++;
                PIStageLogger.warning("getSerialCommandRetry failed (waiting "
                        + (WAITTIME / 1000) + "s). Exception:" + ex.getMessage());
                if (attempts > maxRetry) {
                    String errorMsg = "getSerialCommandRetry failed after " + attempts
                            + " attempts, failed with " + ex.getMessage();
                    PIStageLogger.severe(errorMsg);
                    throw new PISerialException(errorMsg);
                }
                try {
                    Thread.sleep(attempts * attempts * intvl_ms);
                } catch (InterruptedException ie) {
                }

            }
        }
        return "";
    }
    
    public static String[] viewTriggerSettings(String port){
        String[] settings = new String[5];
        CMMCore core = core_;  // get locally for reuse
        try {
            core.setSerialPortCommand(port, "VEL? 1", "\n");
            settings[0] = "speed," + core.getSerialPortAnswer(port, "\n");
            PIStageLogger.info(settings[0]);
            core.setSerialPortCommand(port, "POS? 1", "\n");
            settings[1] = "position," + core.getSerialPortAnswer(port, "\n");
            PIStageLogger.info(settings[1]);
            core.setSerialPortCommand(port, "CTO? 1 8", "\n");
            settings[2] = "start trigger," + core.getSerialPortAnswer(port, "\n");
            PIStageLogger.info(settings[2]);
            core.setSerialPortCommand(port, "CTO? 1 9", "\n");
            settings[3] = "end trigger," + core.getSerialPortAnswer(port, "\n");
            PIStageLogger.info(settings[3]);
            core.setSerialPortCommand(port, "CTO? 1 1", "\n");
            settings[4] = "trig dist," + core.getSerialPortAnswer(port, "\n");
            PIStageLogger.info(settings[4]);
        } catch (Exception e){
            PIStageLogger.severe("Failed to get trigger settings with " + e.toString());
        }
        return settings;
    }
    
    /** Exception that reports an error message from the stage, not really used
     * 
     */
    static class PIControllerErrorException extends Exception
    {
        public int errorCode;
        public PIControllerErrorException() {}
        
        public PIControllerErrorException(String message){
            super(message + "[Unknown PI controller error code]");
        }
        
        public PIControllerErrorException(String message, int error){
            super(message + String.format("[Error code %d in PI controller]", error));
            errorCode = error;
        }
    }
    static class PISerialException extends Exception
    {
        // Parameterless Constructor
        public PISerialException() {}

        // Constructor that accepts a message
        public PISerialException(String message)
        {
            super(message);
        }
    }
}
