package gdrive;

import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import java.io.IOException;

public class CustomProgressListener implements MediaHttpUploaderProgressListener {
  @Override
  public void progressChanged(MediaHttpUploader uploader) throws IOException {
        switch (uploader.getUploadState()) {
            case INITIATION_STARTED:
                System.out.println("Upload iniciado!");
                break;
            case MEDIA_IN_PROGRESS:
                System.out.printf("\rCarregado(s) %d / %d bytes (%.2f%%)", 
                    uploader.getNumBytesUploaded(),
                    uploader.getMediaContent().getLength(),
                    uploader.getProgress() * 100);
                    break;
            case MEDIA_COMPLETE:
                System.out.println("\nUpload completo!");
                break;
        }
    }
}
