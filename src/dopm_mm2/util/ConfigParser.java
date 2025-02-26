/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dopm_mm2.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
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
     * Class to parse JSON configs
     * @param configDetailsJson config file path
     */
    public ConfigParser(String configDetailsJson) {        
        try {
            // Read JSON file
            configParserLogger.info("Reading JSON config");
            String content = new String(Files.readAllBytes(Paths.get(configDetailsJson)));
            this.jsonObject = new JSONObject(content);
        } catch (IOException ex){
            configParserLogger.warning(ex.getMessage());
            JOptionPane.showMessageDialog(null,
                    String.format("Failed to load file at %s",configDetailsJson),
                    "File not found",JOptionPane.ERROR_MESSAGE);
        } catch (InvalidPathException exp){
            configParserLogger.warning(exp.getMessage());
            JOptionPane.showMessageDialog(null,
                    String.format("Invalid path to file %s", exp.toString()), 
                    "File not found",JOptionPane.ERROR_MESSAGE);
        } catch (JSONException jex){
            configParserLogger.warning(jex.getMessage());
            JOptionPane.showMessageDialog(null,
                    String.format("Error parsing JSON %s", jex.toString()), 
                    "JSON error",JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public HashMap getMap(List<String> expectedKeys){
        configMap = new HashMap<String, List<String>>(); 
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
            configParserLogger.info("Map: " + configMap.toString());
            return configMap;
        } catch (JSONException jex){
            configParserLogger.warning(jex.getMessage());
            JOptionPane.showMessageDialog(null,
                    String.format("Error getting data from JSON %s", jex.toString()), 
                    "JSON error",JOptionPane.ERROR_MESSAGE);
            return configMap;
        }
    }
    
    public HashMap getMap(){
        // get iterator for keys in json
        configParserLogger.warning(
                "Getting config map blindly (without checking expected keys)");
        Iterator<String> keys = jsonObject.keys();
        List<String> keysStr = new ArrayList<>();
        while(keys.hasNext()){
            keysStr.add(keys.next());
        }
        return getMap(keysStr);
    }
    
}
