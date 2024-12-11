/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dopm_mm2.util;

import dopm_mm2.Devices.SerialCommands;
import org.micromanager.data.Datastore;
import org.micromanager.Studio;
import mmcorej.CMMCore;
import java.io.IOException;
import java.io.File;
import java.util.List;
import java.util.logging.Logger;
import org.micromanager.ScriptController;

/**
 * For managing datastore creation, deletion etc.
 * @author lnr19
 */
public class FileMM {
    private static final Logger fileLogger = Logger.getLogger(SerialCommands.class.getName());

    /** Preferred constructor, for static use
     * 
     */
    public FileMM() {
    }

    public static Datastore createDatastore(String camName, String saveDir, 
            boolean overwrite) throws IOException{
        return createDatastore(camName, saveDir, overwrite, 
                false, true);
    }
    
    public static Datastore createDatastore(String camName, String saveDir, 
            boolean overwrite, boolean separateMetadata) throws IOException{
        return createDatastore(camName, saveDir, overwrite, separateMetadata, true);
    }
            
    public static Datastore createDatastore(
            String camName, String saveDir, 
            boolean overwrite, boolean separateMetadata,
            boolean useNDtiff) throws IOException{
	// just CAN NOT delete the directory in java for some reason
	// so have to do a terrible alternative
	Datastore img_ds;
	String fileSaveFolderDir = saveDir + "/";
	// overwrite if retrying for example
        fileLogger.info("Making dir " + fileSaveFolderDir);
	new File(fileSaveFolderDir).mkdirs();
	
	System.gc();
        
        Studio mm_ = MMStudioInstance.getStudio();
        CMMCore core_ = MMStudioInstance.getCore();
        ScriptController sc = mm_.getScriptController();
	
	try {
            // data() is datamanager
            // new FileMM(fileSaveFolderDir).mkdirs();
            String fullDir = fileSaveFolderDir + camName + "/";
            // checks if there is already the dir for the datastore
            // that is being created, assumes file is there too
            File dir = new File(fullDir);

            if (dir.isDirectory()){
                if (!overwrite){
                    throw new IOException("File already exists, use overwrite=true "+
                    "when calling createDatastore if you want to overwrite");
                }
                fileLogger.info("Overwriting previous attempt in " + fullDir);
                try {
                    // delete if already exists (for retries)
                    deleteDatastore(camName, fileSaveFolderDir, true); 
                } catch (IOException e){
                    throw new IOException("Failed to overwrite datastore in " + 
                        fileSaveFolderDir + " with " + e.getMessage() );
                }
            }
            // sc.message("Creating TIFF stack datastore in " + fullDir);
            fileLogger.info("Creating TIFF stack datastore in " + fullDir);
            
            if (useNDtiff){
                img_ds = mm_.data().createNDTIFFDatastore(fullDir);
            } else {
                img_ds = mm_.data().createMultipageTIFFDatastore(
                        fullDir, separateMetadata, false);
            }
            
		
	} catch (IOException ex) {
            // mmlog = mm_.getLogManager();
            String errorMsg = "Error creating datastore in " + 
                    fileSaveFolderDir + camName + "/ with " + ex.getMessage();
            fileLogger.severe("Error creating datastore in " + fileSaveFolderDir + camName 
                    +"/ with " + ex.getMessage());
            // sc.message("Error in creating datastore for " + camName + "!");
            throw new IOException(errorMsg);
	}
	sc.message("Successfully created datastores!");
	return img_ds;
}

    public static Datastore createDatastore(String camName, String saveDir) throws IOException{
	return createDatastore(camName, saveDir, false);
    }

    public static int deleteDatastore(String camName, String saveDir, boolean delDirBool) throws IOException{
        
        Studio mm_ = MMStudioInstance.getStudio();
        CMMCore core_ = MMStudioInstance.getCore();
        ScriptController sc = mm_.getScriptController();
        
        String fileSaveFolderDir = saveDir + "/";
        // overwrite if retrying for example
        System.gc();
        String fullDir = fileSaveFolderDir + camName + "/";
        File dir = new File(fullDir);
        // sc.message("Attempting to delete");
        if (dir.isDirectory()){
            try {
                String[] fileNames = new File(fullDir).list();
                for(int i=0; i<fileNames.length; i++){ 
                    fileLogger.info("Deleting " + fileNames[i]
                            + " (" + (i+1) + "/" + fileNames.length + ")");
                    File file = new File(fullDir + fileNames[i]);
                    if(file.isFile()){
                        file.delete();
                        if (!file.exists()){
                            fileLogger.info("Successfully deleted " + fileNames[i]);
                            // sc.message("Successfully deleted " + fileNames[i]);
                        } else {
                            fileLogger.severe("Failed to delete file " + fileNames[i]);
                            // sc.message("Failed to delete file " + fileNames[i]);
                        }
                    }
                }
                // now delete dir
                if (delDirBool){
                    if (! dir.delete()){
                        sc.message("Failed to delete directory " + fullDir 
                                + ", check if all files were deleted first");
                        throw new IOException("Can't delete dir, files still there");
                    }
                }
            } catch (IOException ex) {
                fileLogger.severe("Error deleting datastore in" + fullDir +
                        " with " + ex.getMessage());
                fileLogger.info("Error deleting datastore for " + fullDir + "!");
                // sc.message("Error deleting datastore for " + fullDir + "!");
                throw ex;
            }
            sc.message("Successfully deleted datastores! (" + fullDir + ")");
        }
        else {
            throw new IOException("No directory named " + fullDir); 
        }
        return 0;
    }

    public static int deleteDatastore(String camName, String saveDir) throws IOException{
            // delete dir too by default
            return deleteDatastore(camName, saveDir, true);
    }
}