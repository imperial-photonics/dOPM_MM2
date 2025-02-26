/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dopm_mm2.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import javax.swing.JOptionPane;

/**
 *
 * @author lnr19
 */
 public class dialogBoxes {
        
    /**
     * @see #confirmWindow(String, String)
     */
    public static boolean confirmWindow(String msg){
        return confirmWindow("Confirm", msg);
    }
     
    /**
     * Dialogue box to confirm an action, e.g. are you sure you want to do this?
     * @param msg message in dialogue box
     * @param title title of dialogue box
     * @return true if the user clicked yes, false if no
     */
    public static boolean confirmWindow(String title, String msg){
        int option = JOptionPane.showConfirmDialog(
                null, msg, msg, JOptionPane.YES_NO_OPTION);
        return (option == JOptionPane.YES_OPTION);
    }
     
    /**
     * Dialogue box to show when an error occurs in the acquisition
     * @param e exception thrown in the acquisition, used as the message 
     * (stacktrace)
     */
    public static void acquisitionErrorWindow(Exception e){
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String exceptionString = sw.toString();
                
        JOptionPane.showMessageDialog(null, 
                              "Acquisition failed: " + exceptionString, 
                              "Acquisition Error", 
                              JOptionPane.ERROR_MESSAGE);
    }
    /**
     * Dialogue box to show when an error occurs in the acquisition
     * @msg message in dialogue box
     * @param msg message in dialogue box
     */  
    public static void acquisitionErrorWindow(String msg){
        
        JOptionPane.showMessageDialog(null, 
                              "Acquisition failed: " + msg, 
                              "Acquisition Error", 
                              JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * @see #acquisitionComplete(String)
     */
    public static void acquisitionComplete(){
        acquisitionComplete("");
    }
    
    /**
     * Dialogue box to show acquisition completed successfully
     * @param msg 
     */
    public static void acquisitionComplete(String msg){
        
        JOptionPane.showMessageDialog(null, 
                              "Successfully acquired dOPM dataset. " + msg, 
                              "Acquisition complete", 
                              JOptionPane.INFORMATION_MESSAGE);
    }
}
