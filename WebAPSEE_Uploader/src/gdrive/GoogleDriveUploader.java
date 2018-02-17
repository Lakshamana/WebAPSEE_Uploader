package gdrive;

import java.io.IOException;
import java.net.URISyntaxException;
import webapsee_uploader.IClient;

public class GoogleDriveUploader implements IClient {
    @Override
    public void execute() throws IOException,
            URISyntaxException{
        AuthAndUpload t = new AuthAndUpload();
        t.run();
    }
}
