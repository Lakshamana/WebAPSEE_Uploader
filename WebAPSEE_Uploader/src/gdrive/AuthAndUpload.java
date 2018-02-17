package gdrive;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import java.awt.Desktop;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.activation.MimetypesFileTypeMap;
import webapsee_uploader.Util;


public class AuthAndUpload {
    private final HttpTransport httpTransport = new NetHttpTransport();
    private final JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
    private static final String APP_NAME = "WebAPSEE_Uploader";
    private static FileDataStoreFactory dataStoreFactory;
    
    private static final String STORAGE_PATH = Util.getStorageFullPath(APP_NAME);
    private static java.io.File DATA_STORAGE_CREDENTIALS;  
    static {
        try{
            DATA_STORAGE_CREDENTIALS = new java.io.File(STORAGE_PATH);
            dataStoreFactory = new FileDataStoreFactory(DATA_STORAGE_CREDENTIALS);
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }
    
    private static final List<String> SCOPES 
        = Arrays.asList(DriveScopes.DRIVE_FILE);
    private Credential doAuth() throws IOException{
        InputStream in = AuthAndUpload.class.getResourceAsStream("/resources/client_secret.json");
        GoogleClientSecrets sec = null;
        if(in != null)
            sec = GoogleClientSecrets.load(jsonFactory, 
                new InputStreamReader(in));
        in.close();
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        httpTransport, jsonFactory, sec, SCOPES)
                .setDataStoreFactory(dataStoreFactory)
                .setAccessType("offline")
                .build();
            Credential credential = new AuthorizationCodeInstalledApp(flow, 
                    new LocalServerReceiver()).authorize("user");
        System.out.println(
                "Credenciais salvas em " + DATA_STORAGE_CREDENTIALS.getAbsolutePath());
        return credential;
    }
    
    private Drive getService() throws IOException{
        return new Drive.Builder(httpTransport, jsonFactory, doAuth())
                .setApplicationName(APP_NAME)
                .build();
    }
    
    private void doUpload() throws IOException{
        java.io.File fileContent = Util.promptUsuario();
        File testFile = new File();
        testFile.setTitle(fileContent.getName());
        testFile.setDescription("Um documento de teste!");
        String mime = new MimetypesFileTypeMap()
                .getContentType(fileContent);
        testFile.setMimeType(mime);
        testFile.setFileExtension(Util.getFileExtension(fileContent));
        FileContent mediaContent = new FileContent(mime, fileContent);
        Drive.Files.Insert request = getService().files().insert(testFile, mediaContent);
        MediaHttpUploader uploader = request.getMediaHttpUploader();
        uploader.setDirectUploadEnabled(false);
        uploader.setProgressListener(new CustomProgressListener());
        File file = request.execute();
        System.out.println("ID do arquivo: " + file.getId());
    }
    
    public void run() throws IOException{
        doUpload();
        try{
            Desktop desktop = Desktop.getDesktop();
            URI uri = new URI("https://drive.google.com/drive/my-drive");
            desktop.browse(uri);
        }catch(IOException | URISyntaxException ex){
            System.err.println("Erro ao abrir página");
            ex.printStackTrace();
        }
    }
}
