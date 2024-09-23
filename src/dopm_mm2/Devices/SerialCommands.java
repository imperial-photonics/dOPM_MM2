/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dopm_mm2.Devices;

import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import mmcorej.CMMCore;

/**
 * To implement shared serial commands e.g., set and check serial command [CURRENTLY UNUSED]
 * @author lnr19
 */
public class SerialCommands {
    CMMCore core_ = null;
    private static final Logger SerialCommandsLogger = Logger.getLogger(SerialCommands.class.getName());
    
    public SerialCommands() {
    }
    
    public SerialCommands(CMMCore core){
        core_ = core;
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
                SerialCommandsLogger.info(String.format(
                        "Sending serial command %s to %s", msg, port));
                core_.setSerialPortCommand(port, msg, "\n");
                // check for errors
                core_.setSerialPortCommand(port, "ERR?", "\n");
                ERR = core_.getSerialPortAnswer(port, "\n");
                if (ERR.equals("0")) {
                    String errMsg = String.format("Error code %s in setting serial command", ERR);
                    SerialCommandsLogger.severe(errMsg);
                    throw new IllegalStateException(errMsg);
                }

                core_.setSerialPortCommand(port, queryMsg, "\n");
                answer = core_.getSerialPortAnswer(port, "\n");
                SerialCommandsLogger.info(String.format(
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
}
