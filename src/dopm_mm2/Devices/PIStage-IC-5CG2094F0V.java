/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dopm_mm2.Devices;

import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import mmcorej.CMMCore;

/**
 * Device control functions, not static, needs to be instantiated with CMMCore Use the functions in
 * here for hardware control, these are directly called by Runnables (which are in turn called by
 * the host_frame) The list of functions:
 *
 *
 *
 * @author lnr19
 */

public class PIStage {

    private final CMMCore core_;
    private static final Logger PIStageLogger
            = Logger.getLogger(PIStage.class.getName());

    public PIStage(CMMCore core) {
        core_ = core;
    }

    /* The following commands control stage positions and check they were set correctly */
    public void setPIStagePosition(double position){
        int maxRetries = 10;
        boolean success = false;
        int waitRetryInterval = 10000;
        int nTry = 0;
        while(!success && nTry<maxRetries){
            try {
                core_.setPosition(position);
            } catch (Exception e){
                PIStageLogger.warning("Error setting PI stage position with " + e.getMessage());
                nTry++;
                try {
                    Thread.sleep(waitRetryInterval);
                } catch (InterruptedException interruptE) {
                }
            }
            
        }

    }
    
    /*
    The following commands wrap the PI ASCII commands for controlling the trigger output from the 
    controller (C-413). 
    These work by changing settings with the necessary command (msg), quering the setting with 
    queryMsg to check if the value that was set (along with first checking ERR?).
    Each throw PISerialExceptions under failure
     */
    
    public void setupPITriggering(String port, int device) throws Exception {
        /* Initialise the PI stage basic trigger output settings */
        setPITriggerEnable(port, device, 0);  // disable triggering
        setPIDigitalOut(port, device, 0);  // set digital (trigger) out to low
        setPITriggerAxis(port, device, 1);  // set trigger axis to be 1 (our PI is 1-axis anyway)
        setPITriggerMode(port, device, 0);  // set trigger mode to position/distance trigger
    }

    public void setPIDigitalOut(String port, int device, int level) throws Exception {
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

    public void setPITriggerEnable(String port, int trigOn) throws Exception {
        setPITriggerEnable(port, 1, trigOn);
    }

    public void setPITriggerEnable(String port, int device, int trigOn) throws Exception {
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

    public void setPITriggerAxis(String port, int axis) throws Exception {
        setPITriggerAxis(port, 1, axis);
    }

    public void setPITriggerAxis(String port, int device, int axis) throws Exception {
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

    public void setPITriggerMode(String port, int triggerMode) throws Exception {
        setPITriggerMode(port, 1, triggerMode);
    }

    public void setPITriggerMode(String port, int device, int triggerMode) throws Exception {
        // triggerMode = 0 is what we want 
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

    public void setPITriggerDistance(String port, int device, double triggerDistance)
            throws Exception {

        // min incremental motion is 0.02um (0.00002mm, 1e-5)
        String triggerDistanceStr = String.format("%.5f", triggerDistance);
        String msg = String.format("CTO %1$d 6 %2$s", device, triggerDistanceStr);
        String queryMsg = String.format("CTO? %1$d 6", device);
        double expectedValue = triggerDistance;
        try {
            setAndCheckSerial(port, msg, queryMsg, expectedValue);
        } catch (Exception e) {
            PIStageLogger.severe("Failed to set PI trigger distance with " + e.getMessage());
            throw e;
        }
    }

    public void setPITriggerRange(String port, int device, double[] triggerRange) throws Exception {

        String lowerRangeStr = String.format("%.5f", triggerRange[0]);
        String upperRangeStr = String.format("%.5f", triggerRange[1]);
        String msgLower = String.format("CTO %1$d 8 %2$s", device, lowerRangeStr);
        String msgUpper = String.format("CTO %1$d 9 %2$s", device, upperRangeStr);

        String queryMsgLower = String.format("CTO? %1$d 8", device);
        String queryMsgUpper = String.format("CTO? %1$d 9", device);

        double expectedValueLower = triggerRange[0];
        double expectedValueUpper = triggerRange[1];

        try {
            setAndCheckSerial(port, msgLower, queryMsgLower, expectedValueLower);
            setAndCheckSerial(port, msgUpper, queryMsgUpper, expectedValueUpper);

        } catch (Exception e) {
            String errMsg = String.format("Failed to set PI trigger range with %s", e.getMessage());
            PIStageLogger.severe(errMsg);
            throw e;
        }
    }

    public void clearPISerialOutBuffer(String port) {
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
    
    public void stopPIStage(String port){
        try {
            core_.setSerialPortCommand(port, "STP", "\n");
            core_.setSerialPortCommand(port, "ERR?", "\n");
            // STP (and HLT) set error code to 10
            String answerErr = core_.getSerialPortAnswer(port, "\n");
            if (!answerErr.equals("10")) throw new PIControllerErrorException(String.format(
                    "Error code %1$s received from PI controller"));

        } catch (Exception e){
            
        }
    }

    public void setAndCheckSerial(
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
                isSet = Math.abs(value - expectedValue) < 1e-5;

            } catch (Exception e) {
                i++;
                if (i > maxRetry) {
                    throw new TimeoutException(String.format("Failed to set trigger mode after %d "
                            + "tries with exception %s", maxRetry, e.getMessage()));
                }
                try {
                    Thread.sleep(sleep_intvl_ms);
                } catch (InterruptedException ie) {
                }
            }
        } while (!isSet);
    }

    class PIControllerErrorException extends Exception
    {
        public int errorCode;
        public PIControllerErrorException() {}
        
        public PIControllerErrorException(String message){
            super(message + "[Unknown PI controller error code]");
        }
        
        public PIControllerErrorException(String message, int errorCode){
            super(message + String.format("[Error code %d in PI controller]", errorCode));
            this.errorCode = errorCode;
        }
    }
    class PISerialException extends Exception
    {
        // Parameterless Constructor
        public PISerialException() {}

        // Constructor that accepts a message
        public PISerialException(String message)
        {
            super(message);
        }
    }
    // These send serial command and get serial answer retries are overkill I think... I will end 
    // up doing waits/retries outside of these functions anyway
    public void sendSerialCommandRetry(String port, String msg) throws PISerialException {
        // Version of sendSerialCommandRetry with just the port and message as args
        sendSerialCommandRetry(port, msg, "\n", 5);
    }

    public void sendSerialCommandRetry(
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

    public String getSerialAnswerCommandRetry(String port) throws PISerialException {
        // Version of getSerialAnswerCommandRetry with just the port as arg
        return getSerialAnswerCommandRetry(port, "\n", 5);
    }

    public String getSerialAnswerCommandRetry(
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

}
