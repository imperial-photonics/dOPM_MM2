/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dopm_mm2.util;

import mmcorej.CMMCore;
import org.micromanager.Studio;

/** Singleton class to access MMStudio and CMMCore globally
 * Initially was just using dependency injection every time
 * 
 * @author lnr19
 */
public class MMStudioInstance {
    private static Studio studioInstance;
    private static CMMCore coreInstance;


    private MMStudioInstance() {} // Private constructor to prevent instantiation

    public static synchronized void initialize(Studio mm){
        if (studioInstance == null) {
            studioInstance = mm;
            coreInstance = mm.getCMMCore();
        }
    }
    
    public static Studio getMMStudioInstance() {
        if (studioInstance == null) {
            throw new IllegalStateException("MMStudioInstance has not been initialized yet.");
        }
        return studioInstance; 
    }

    public static CMMCore getCoreInstance() {
        if (coreInstance == null) {
            throw new IllegalStateException("MMStudioInstance has not been initialized yet.");
        }
        return coreInstance;
    }
}
