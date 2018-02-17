package webapsee_uploader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class Util {
    public static boolean isWindows() {
        return getOsName().toLowerCase().startsWith("windows");
    }
    
    private static String OS = null;
    public static String getOsName() {
        if (OS == null) 
            OS = System.getProperty("os.name");
        return OS;
    }
    
    public static String createFolderIfNotExists(String path) {
        java.io.File file = new java.io.File(path);
        if (!file.exists()) 
            file.mkdirs();
        return path;
    }

    public static String getStorageFullPath(String appName) {
        if (isWindows()) 
            return System.getProperty("user.home") + "\\.credentials\\" + appName + "\\";
        else 
            return System.getProperty("user.home") + "/.credentials/" + appName + "/";
    }
    
    public static File promptUsuario(){
        System.out.println("Digite o caminho do arquivo a ser feito o upload: ");
        String path = null;
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        try {
            path = br.readLine();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return new File(path);
    }
    
    public static String getFileExtension(File file) {
        String fileName = file.getName();
        if(fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0)
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        else return "";
    }
}
