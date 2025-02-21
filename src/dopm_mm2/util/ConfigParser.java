/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dopm_mm2.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import mmcorej.org.json.JSONArray;  // might as well use these ones?
import mmcorej.org.json.JSONException;
import mmcorej.org.json.JSONObject;

/**
 *
 * @author Leo Rowe-Brown 2025
 */
public class ConfigParser {
    private static final Logger configParserLogger = Logger.getLogger(ConfigParser.class.getName()); 
    private HashMap configMap;
    private List<String> expectedKeys;
    private JSONObject jsonObject;

    public void ConfigParser(){};

    /**
     * 
     * @param configDetailsJson config file path
     */
    public ConfigParser(String configDetailsJson){
        this(configDetailsJson, new ArrayList());
    }
        
    /** Class to parse JSON configs
     *
     * @param configDetailsJson config file path
     * @param expectedKeys keys/entries expected to be in config file
     */
    public ConfigParser(String configDetailsJson, List<String> expectedKeys) {        
        try {
            // Read JSON file
            String content = new String(Files.readAllBytes(Paths.get(configDetailsJson)));
            this.jsonObject = new JSONObject(content);
            this.expectedKeys = expectedKeys;
            configMap = new HashMap<String, List<String>>(); 
        
        } catch (FileNotFoundException ex) {
            configParserLogger.warning(ex.getMessage());
            JOptionPane.showMessageDialog(null,
                    String.format("No config file found at %s",configDetailsJson),
                    "File not found",JOptionPane.ERROR_MESSAGE);
        } catch (IOException ex){
            configParserLogger.warning(ex.getMessage());
            JOptionPane.showMessageDialog(null,
                    String.format("Failed to load file at %s",configDetailsJson),
                    "File not found",JOptionPane.ERROR_MESSAGE);
        } catch (JSONException jex){
            configParserLogger.warning(jex.getMessage());
            JOptionPane.showMessageDialog(null,
                    String.format("Error opening JSON %s", jex.toString()), 
                    "File not found",JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public void parse(){
        try {
            for (String key : expectedKeys) {
                if (jsonObject.has(key)) {
                    JSONArray jsonArray = jsonObject.getJSONArray(key);
                    List<String> values = new ArrayList<>();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        values.add(jsonArray.getString(i));
                    }
                    configMap.put(key, values);
                } else {
                    configParserLogger.warning("No entry for " + key + " found in config!");
                }
            }
            configParserLogger.warning("No expected keys supplied, map "
                    + "won't check missing keys");
            // Log parsed data
            configParserLogger.info("Map: " + configMap.toString());
        } catch (JSONException jex){
            configParserLogger.warning(jex.getMessage());
            JOptionPane.showMessageDialog(null,
                    String.format("Error parsing JSON %s", jex.toString()), 
                    "File not found",JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public HashMap getConfigMap(){
        return configMap;
    }
    
}
