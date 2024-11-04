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
 public class errorTools {
        
    public static void acquisitionErrorWindow(Exception e){
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String exceptionString = sw.toString();
                
        JOptionPane.showMessageDialog(null, 
                              "Acquisition failed: " + exceptionString, 
                              "Acquisition Error", 
                              JOptionPane.ERROR_MESSAGE);
    }
        
    public static void acquisitionErrorWindow(String msg){
        
        JOptionPane.showMessageDialog(null, 
                              "Acquisition failed: " + msg, 
                              "Acquisition Error", 
                              JOptionPane.ERROR_MESSAGE);
    }
}
