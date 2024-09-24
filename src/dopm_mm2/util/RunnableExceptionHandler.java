/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dopm_mm2.util;

import java.util.logging.Logger;

/** To run MM commands in runnables without having to try catch every time:
 * reports the error to the logger
 * 
 * Usage:
 * RunnableExceptionHandler.runAndLogging((param1, param2...) -> <methodToRun(param1, param2...)>);
 *
 * @author lnr19
 */


public class RunnableExceptionHandler {

    // Method to wrap the execution and handle exceptions
    public static String runAndLogging(MethodWrapper methodWrapper, Logger logger, String... params) {
        try {
            return methodWrapper.execute(params);
        } catch (Exception e) {
            logger.severe("MMException occurred: " + e.getMessage());
            return "";
        }
    }
    
    @FunctionalInterface
    public interface MethodWrapper {
        String execute(String... params) throws Exception;
    }
}