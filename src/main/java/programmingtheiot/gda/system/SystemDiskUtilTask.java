/**
 * This class is part of the Programming the Internet of Things project.
 * 
 * It is provided as a simple shell to guide the student and assist with
 * implementation for the Programming the Internet of Things exercises,
 * and designed to be modified by the student as needed.
 */ 

 package programmingtheiot.gda.system;

 import java.nio.file.FileStore;
 import java.nio.file.FileSystems;
 import java.nio.file.Path;
 import java.nio.file.Files;
 import java.io.IOException;
 
 import java.util.logging.Logger;
 
 import programmingtheiot.common.ConfigConst;
 
 
 /**
  * Shell representation of class for student implementation.
  * 
  */
 public class SystemDiskUtilTask extends BaseSystemUtilTask
 {
     // constructors
     
     /**
      * Default.
      * 
      */
     public SystemDiskUtilTask() {
         super(ConfigConst.NOT_SET, ConfigConst.DEFAULT_TYPE_ID);
     }
     
     
     // public methods
     
     @Override
     public float getTelemetryValue() {
         try {
             // Usamos la raíz del sistema; ajustar la ruta según sea necesario
             Path rootPath = FileSystems.getDefault().getPath("/");
             FileStore store = Files.getFileStore(rootPath);
             
             long totalSpace = store.getTotalSpace();
             long freeSpace = store.getUnallocatedSpace();  // o store.getUsableSpace() según se requiera
             long usedSpace = totalSpace - freeSpace;
             
             double diskUtilPercentage = ((double) usedSpace / totalSpace) * 100.0;
             return (float) diskUtilPercentage;
         } catch (IOException e) {
             _Logger.severe("Error obteniendo la utilización del disco: " + e.getMessage());
             return 0.0f;
         }
     }
     
 }
 