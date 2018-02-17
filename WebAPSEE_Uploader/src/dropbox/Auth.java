package dropbox;

import com.dropbox.core.DbxAppInfo;
import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.DbxAuthInfo;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.DbxWebAuth;
import com.dropbox.core.json.JsonReader;
import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import webapsee_uploader.Util;

public class Auth {

    private static final String APP_NAME = "apseeUploader";
    private String accessToken;
    private String userId;
    private final String DATA_INFO_PATH = "/resources/test.app";
    private DbxAppInfo appInfo;
    private DbxAuthInfo authInfo;
    
    private void doAuth(){   
        try {
            appInfo = DbxAppInfo.Reader.readFromFile(DATA_INFO_PATH);
        } catch (JsonReader.FileLoadException ex) {
            ex.printStackTrace();
            System.exit(1);
        }

        DbxRequestConfig requestConfig = new DbxRequestConfig("apseeUploader");
        DbxWebAuth webAuth = new DbxWebAuth(requestConfig, appInfo);
        DbxWebAuth.Request webAuthRequest = DbxWebAuth.newRequestBuilder()
                .withNoRedirect()
                .build();

        String authorizeUrl = webAuth.authorize(webAuthRequest);
        try {
            Desktop.getDesktop().browse(new URI(authorizeUrl));
        } catch (URISyntaxException | IOException ex) { 
            ex.printStackTrace();
        }
       
        try {
            System.out.println("Copie aqui o seu código: ");
            String code = new BufferedReader(new InputStreamReader(System.in)).readLine();
            code = code.trim();
            DbxAuthFinish authFinish = webAuth.finishFromCode(code);
            accessToken = authFinish.getAccessToken();
            userId = authFinish.getUserId();
        } catch (IOException | DbxException ex) {
            System.err.println(ex.getMessage());
            System.exit(1); 
        }
    }
    
    private File saveAuthData(String path){
        authInfo = new DbxAuthInfo(accessToken, appInfo.getHost());
        File output = null;
        try {
            output = new File(path);
            DbxAuthInfo.Writer.writeToFile(authInfo, output);
            System.out.println("Credenciais salvas em \"" + output.getAbsolutePath() + "\".");
        } catch (IOException ex) {
            try {
                System.err.println("Não pôde salvar arquivo. Lançando em stderr:");
                DbxAuthInfo.Writer.writeToStream(authInfo, System.err);
                System.err.println();
            } catch (IOException ex1) {
                ex1.printStackTrace();
            }
            ex.printStackTrace();
            System.exit(1);
        }
        return output;
    }
    
    private static final String STORAGE_PATH = 
            Util.createFolderIfNotExists(Util.getStorageFullPath(APP_NAME)) + "credential.auth";
    private static File AUTH_FILE = new File(STORAGE_PATH);
    public void run() {
        if(!AUTH_FILE.exists()){
            doAuth();
            AUTH_FILE = saveAuthData(STORAGE_PATH);
        }
    }
    
    public static File getAuthFile() {
        return AUTH_FILE;
    }
}
