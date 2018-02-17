package dropbox;

import com.dropbox.client2.exception.DropboxException;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import webapsee_uploader.IClient;

public class DropboxUploader implements IClient {

    private static final String DROPBOX_HOME = "https://www.dropbox.com/h";
    @Override
    public void execute() throws DropboxException, InterruptedException {
        Auth a = new Auth();
        Uploader u = new Uploader();
        a.run();
        u.run();
        try{
            Desktop.getDesktop().browse(new URI(DROPBOX_HOME));
        } catch(URISyntaxException | IOException ex){
            ex.printStackTrace();
        }
    }
}
