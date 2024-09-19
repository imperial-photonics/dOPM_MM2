/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package dopm_mm2;

import javax.swing.JFrame;
import org.micromanager.MenuPlugin;
import org.micromanager.Studio;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;
import dopm_mm2.GUI.dOPM_hostframe;


/**
 *
 * @author Leo Rowe-Brown and Wenjun Gong
 */
 
@Plugin(type = MenuPlugin.class)
public class dOPM_MM2 implements MenuPlugin, SciJavaPlugin{
    //Name for the plugin
    public static final String MENU_NAME = "dOPM MDA";
    private Studio gui_;    
    public static JFrame frame_;
    
    @Override
    public String getSubMenu() {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        return("Acquisition Tools");
    }

    @Override
    public void onPluginSelected() {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        frame_ = new dOPM_hostframe(gui_);
        frame_.setVisible(true);
    }

    @Override
    public void setContext(Studio studio) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        gui_ = studio;
    }

    @Override
    public String getName() {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        return("dOPM MDA for MicroManager 2");
    }

    @Override
    public String getHelpText() {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        return("Sorry!");
    }

    @Override
    public String getVersion() {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        return("0.0.1");
    }

    @Override
    public String getCopyright() {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        return("Copyright Imperial College London (2020-2024)");
    }
    
}
