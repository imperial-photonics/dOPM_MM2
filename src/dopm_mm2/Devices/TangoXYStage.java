 /*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dopm_mm2.Devices;

import dopm_mm2.util.MMStudioInstance;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    
    /** Generic tango move command that takes scanAxis, uses MMCore.
     * 
     * @param device XY stage device name
     * @param position target position for axis in um
     * @param axis axis to move, x or y
     */
    // Generic move command that takes scanAxis, uses MMCore functionality.
    // This feels clunky..
    public static void setAxisPosition(
            String device, double position, String axis) throws Exception {
        try {
            double posXStart = MMStudioInstance.getCore().getXPosition(device);
            double posYStart = MMStudioInstance.getCore().getYPosition(device);
            
            switch (axis){
                case "x":
                    MMStudioInstance.getCore().setXYPosition(
                            device, position, posYStart);
                    tangoXYLogger.info(String.format("Set %s position to "
                            + "%.2f um", axis, position));
                    break;
                case "y":
                    MMStudioInstance.getCore().setXYPosition(
                            device, posXStart, position);
                    tangoXYLogger.info(String.format("Set %s position to "
                            + "%.2f um", axis, position));
                    break;
                default:
                    throw new Exception(String.format("%s is an invalid axis, "
                            + "use x or y", axis));
            }
        } catch (Exception e){
            tangoXYLogger.severe(String.format(
                    "Failed to set %s %s to position %.2f um with: %s",
                    device, axis, position, e.getMessage()));
            throw new Exception(e);
        }
    }
    
    /** set speed of both Tango axes
     * 
     * @param device
     * @param speed
     * @throws Exception 
     */
    public static void setTangoAxisSpeed(String device, double speed) 
            throws Exception{
        setTangoAxisSpeed(device, "x", speed);
        setTangoAxisSpeed(device, "y", speed);
    }

    /** set speed of specific Tango axis "x" or "y"
     * 
     * @param device
     * @param axis
     * @param speed
     * @throws Exception 
     */
    public static void setTangoAxisSpeed(String device, String axis, 
            double speed) throws Exception{
        switch(axis){
            case "x":
                MMStudioInstance.getCore().setProperty(device, "SpeedX [mm/s]", speed);
                break;
            case "y":
                MMStudioInstance.getCore().setProperty(device, "SpeedY [mm/s]", speed);
                break;
            default:
                throw new IllegalArgumentException("Invalid axis, use x or y"); 
        }      
    }
    
    // Generic move command in millimeters, tango uses um units (or 10s of um?)
    public static void setTangoXYPositionMillim(
            String device, double[] position) throws Exception{
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
            throw new Exception(e);
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
        // TODO check min incremental motion i, implement a version that gets 
        // axis implicitly
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
    
    public static void setTangoTriggerAxis(String port, String axis) 
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
    
    public static void setTangoTriggerEnable(
            String port, String axis, int trigOn) throws Exception {
        // TODO check min incremental motion i
        String msg = String.format("!trig %s %d", axis, trigOn);
        String queryMsg = "?trig";
        double expectedValue = trigOn;
        try {
            setAndCheckSerial(port, msg, queryMsg, expectedValue);
        } catch (Exception e) {
            tangoXYLogger.severe("Failed to set Tango trigger axis with " + e.getMessage());
            throw e;
        }
    }
    
    /** Set desired trigger range and calculate and return actual ranged based
     * on integer number of triggers considering the trigger distance
     * Is always less than desiredTriggerRange; this method gets trigd
     * @param port COM port
     * @param axis axis to trigger over, x or y
     * @param desiredTriggerRange desired volume scan range
     * @return actual trigger range
     * @throws Exception if setting trigger range fails
     **/
    public static double[] setTangoTriggerRange(String port, String axis,
            double[] desiredTriggerRange) throws Exception {
        MMStudioInstance.getCore().setSerialPortCommand(
                port, "?trigd " + axis, "\r");
        // in mm
        double triggerDist = Double.parseDouble(MMStudioInstance.getCore().
                getSerialPortAnswer(port, "\r"));
        return setTangoTriggerRange(port, axis,
            desiredTriggerRange, triggerDist);
    }
    
    /** Works by calculating N number of triggers to fit in the desired 
     * trigger range and calculates the actual range resulting from N 
     * triggers separated by triggerDist--Is always less than 
     * desiredTriggerRange.
     * @param port COM port
     * @param axis axis to trigger over, x or y
     * @param desiredTriggerRange desired volume scan range
     * @param triggerDist trigger distance used to calculation trigger range
     * @return actual trigger range
     * @throws Exception if setting trigger range fails
     **/
    public static double[] setTangoTriggerRange(String port, String axis,
            double[] desiredTriggerRange, double triggerDist) throws Exception {

        int nTriggers = (int)Math.floor(
                (desiredTriggerRange[1]-desiredTriggerRange[0])/triggerDist);
        double startTrigger = desiredTriggerRange[0];
        double endTrigger = startTrigger + nTriggers*triggerDist;
        // note that trigr sets trigm 20 implicitly
        String msg = String.format("!trigr %.5f %.5f %d", 
                startTrigger, endTrigger, nTriggers );
        String queryMsg = String.format("?trigr %.5f %.5f %d", 
            startTrigger, endTrigger, nTriggers );
        setAndCheckSerial(port, msg, msg, endTrigger);
        return new double[]{startTrigger, endTrigger};
    }

        
    /** sets a string value with tango ASCII e.g., trigd x 0.1. 
     * @param port COM port
     * @param msg serial command used to set value
     * @param queryMsg serial command used to check value
     * @param expectedValue value (string) that is trying to be set
     * @throws TimeoutException if fails to set the value after 5 retries
     **/
    public static void setAndCheckSerial(
            String port, String msg, String queryMsg, String expectedValue)
            throws TimeoutException, IllegalStateException {
        setAndCheckSerial_(port, msg, queryMsg, expectedValue);
        
    }
    /** sets a double value with tango ASCII e.g., triga x. 
     * @param port COM port
     * @param msg serial command used to set value
     * @param queryMsg serial command used to check value
     * @param expectedValue value (double) that is trying to be set
     * @throws TimeoutException if fails to set the value after 5 retries
     **/
    public static void setAndCheckSerial(
        String port, String msg, String queryMsg, Double expectedValue)
        throws TimeoutException, IllegalStateException {
        setAndCheckSerial_(port, msg, queryMsg, expectedValue);
    }
    
    /** sets a double value with tango ASCII e.g., triga x. 
     * @param port COM port
     * @param msg serial command used to set value
     * @param queryMsg serial command used to check value
     * @param expectedValue value that is trying to be set
     * @throws TimeoutException if fails to set the value after 5 retries
     **/
    public static void setAndCheckSerial_(
            String port, String msg, String queryMsg, Object expectedValue)
            throws TimeoutException, IllegalStateException {

        Pattern p = Pattern.compile("[xy]");
        Matcher m = p.matcher(msg);
        String axis = "";
        while(m.find()){
            axis = m.group(0);
        }

        String errCmd = "err";  

        String answer;
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
                            "Error code %1$s in Tango from err after sending "
                                    + "command %2$s", ERR, msg);
                    tangoXYLogger.severe(errMsg);
                    throw new Exception(errMsg);
                }
                // check if set correctly
                core.setSerialPortCommand(port, queryMsg, "\r");
                answer = core.getSerialPortAnswer(port, "\r");
                tangoXYLogger.info(String.format(
                        "Received answer %s from %s", answer, port));
                
                // get value after being set, could be string or double
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