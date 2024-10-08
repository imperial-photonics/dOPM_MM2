/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dopm_mm2.Devices;

import dopm_mm2.util.MMStudioInstance;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import mmcorej.CMMCore;

/** Static class to control Marzhauser XY stange with Tango, mostly for 
 * triggering with the ASCII API
 *
 * @author Leo Rowe-Brown
 */
public class TangoXYStage {
    private static final Logger tangoXYLogger
        = Logger.getLogger(TangoXYStage.class.getName());
    
    // Constructor
    public void TangoXYStage() {
    }
    
    // Generic move command in millimeters, tango uses um units (or 10s of um?)
    public static void setTangoXYPositionMillim(String device, double[] position){
        double posX = position[0]*1e3;
        double posY = position[1]*1e3;
        try {
            MMStudioInstance.getCore().setXYPosition(device, posX, posY);
            tangoXYLogger.info(String.format(
                    "Set %s position to (%.4f,%.4f) mm",device, posX, posY));
        } catch (Exception e){
            tangoXYLogger.severe(String.format(
                    "Failed to set %s to position (%.4f,%.4f) mm with: %s",
                    device, posX, posY, e.getMessage()));
            throw new RuntimeException(e);
        }
    }
    
    /** Set Tango trigger distance (in millimeters) for axis <axis>
    * @param port COM port
    * @param axis axis to set trigger distance of
    * @param triggerDistance trigger distance in mm
    * @throws Exception
    */
    public static void setTangoTriggerDistance(
        String port, String axis, double triggerDistance) throws Exception {
            // TODO check min incremental motion i
        String msg = String.format("!trigd %s %.5f", axis, triggerDistance);
        String queryMsg = String.format("?trigd %s", axis);
        double expectedValue = triggerDistance;
        try {
            setAndCheckSerial(port, msg, queryMsg, expectedValue);
        } catch (Exception e) {
            tangoXYLogger.severe("Failed to set PI trigger distance with " + e.getMessage());
            throw e;
        }
    }
    
    public static void setTangoTriggerAxis( String port, String axis) 
                throws Exception {
        // TODO check min incremental motion i
        String msg = String.format("!triga %s", axis);
        String queryMsg = "?triga";
        String expectedValue = axis;
        try {
            setAndCheckSerial(port, msg, queryMsg, expectedValue);
        } catch (Exception e) {
            tangoXYLogger.severe("Failed to set Tango trigger axis with " + e.getMessage());
            throw e;
        }
    }
    
    public static void setTangoTriggerEnable( String port, String axis) 
                throws Exception {
        // TODO check min incremental motion i
        String msg = String.format("!triga %s", axis);
        String queryMsg = "?triga";
        String expectedValue = axis;
        try {
            setAndCheckSerial(port, msg, queryMsg, expectedValue);
        } catch (Exception e) {
            tangoXYLogger.severe("Failed to set Tango trigger axis with " + e.getMessage());
            throw e;
        }
    }
    /** Works by calculating N number of triggers to fit in the desired 
     * trigger range and calculates the actual range resulting from N 
     * triggers separated by triggerDist--Is always less than 
     * desiredTriggerRange.
     **/
    public static double[] setTangoTriggerRange(String port, String axis,
            double[] desiredTriggerRange, double triggerDist) throws Exception {

        double nTriggers = Math.floor(
                (desiredTriggerRange[1]-desiredTriggerRange[0])/triggerDist);
        double startTrigger = desiredTriggerRange[0];
        double endTrigger = startTrigger + nTriggers*triggerDist;
        String msg = String.format("!trigr %.5f %.5f %d", 
                startTrigger, endTrigger, nTriggers );
        return new double[]{startTrigger, endTrigger};
    }

        
    /** sets a string value with tango ASCII e.g., trigd x 0.1. */
    public static void setAndCheckSerial(
            String port, String msg, String queryMsg, String expectedValue)
            throws TimeoutException, IllegalStateException {
        setAndCheckSerial_(port, msg, queryMsg, expectedValue);
        
    }
    /** sets a double value with tango ASCII e.g., triga x. */
    public static void setAndCheckSerial(
        String port, String msg, String queryMsg, Double expectedValue)
        throws TimeoutException, IllegalStateException {
        setAndCheckSerial_(port, msg, queryMsg, expectedValue);
    }
    
    public static void setAndCheckSerial_(
            String port, String msg, String queryMsg, Object expectedValue)
            throws TimeoutException, IllegalStateException {

        String axis = msg.split(" ")[1];
        if (!axis.equals("x") && !axis.equals("y")){
            axis = "x";
        }
        String errCmd = "?err " + axis;

        String answer = "";
        int sleep_intvl_ms = 1000;
        int maxRetry = 5;
        boolean isSet = false;
        String ERR;
        
        // define locally for reuse
        CMMCore core = MMStudioInstance.getCore();

        int i = 0;
        do {
            try {
                tangoXYLogger.info(String.format(
                        "Sending serial command %s to %s", msg, port));
                core.setSerialPortCommand(port, msg, "\r");
                // check for errors
                core.setSerialPortCommand(port, errCmd, "\r");
                ERR = core.getSerialPortAnswer(port, "\r");
                if (!ERR.equals("0")) {
                    String errMsg = String.format(
                            "Error code %1$s in Tango from err after sending command %2$s", ERR, msg);
                    tangoXYLogger.severe(errMsg);
                    throw new Exception(errMsg);
                }

                core.setSerialPortCommand(port, queryMsg, "\r");
                answer = core.getSerialPortAnswer(port, "\r");
                tangoXYLogger.info(String.format(
                        "Received answer %s from %s", answer, port));
                // get value after being set
                if (expectedValue instanceof Double) {
                    double value = Double.parseDouble(answer);
                    double evalueDouble = (Double) expectedValue;
                    isSet = Math.abs(value - evalueDouble) < 1e-7;
                } else if (expectedValue instanceof String) {
                    isSet = answer.equals((String) expectedValue);
                }

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