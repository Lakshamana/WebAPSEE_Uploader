package dropbox;

import com.dropbox.core.DbxAuthInfo;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.NetworkIOException;
import com.dropbox.core.RetryException;
import com.dropbox.core.json.JsonReader;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.CommitInfo;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.UploadErrorException;
import com.dropbox.core.v2.files.UploadSessionCursor;
import com.dropbox.core.v2.files.UploadSessionFinishErrorException;
import com.dropbox.core.v2.files.UploadSessionLookupErrorException;
import com.dropbox.core.v2.files.WriteMode;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import webapsee_uploader.Util;

public class Uploader {
    
    /* Todo o código original desta classe está disponível
    *+ em https://github.com/dropbox/dropbox-sdk-java/blob/master/
    *+examples/upload-file/src/main/java/com/dropbox/core/examples/
    *+upload_file/Main.java.
    * Algumas alterações foram feitas.
    * Apenas as mensagens de erro estão traduzidas.
    */
    private static final long CHUNKED_UPLOAD_CHUNK_SIZE = 4L << 20; // 1MiB
    private static final int CHUNKED_UPLOAD_MAX_ATTEMPTS = 5;

    /**
     * Uploads a file in a single request. This approach is preferred for small files since it
     * eliminates unnecessary round-trips to the servers.
     *
     * @param dbxClient Dropbox user authenticated client
     * @param localFIle local file to upload
     * @param dropboxPath Where to upload the file to within Dropbox
     */
    private static void uploadFile(DbxClientV2 dbxClient, File localFile, String dropboxPath) {
        try (InputStream in = new FileInputStream(localFile)) {
            FileMetadata metadata = dbxClient.files().uploadBuilder(dropboxPath)
                .withMode(WriteMode.ADD)
                .withClientModified(new Date(localFile.lastModified()))
                .uploadAndFinish(in);

            System.out.println(metadata.toStringMultiline());
        } catch (UploadErrorException ex) {
            System.err.println("Erro 1 em upload para o Dropbox: " + ex.getMessage());
            ex.printStackTrace();
            System.exit(1);
        } catch (DbxException ex) {
            System.err.println("Erro 2 em upload para o Dropbox: " + ex.getMessage());
            System.exit(1);
        } catch (IOException ex) {
            System.err.println("Erro 1 lendo arquivo \"" + localFile + "\": " + ex.getMessage());
            System.exit(1);
        }
    }

    /**
     * Uploads a file in chunks using multiple requests. This approach is preferred for larger files
     * since it allows for more efficient processing of the file contents on the server side and
     * also allows partial uploads to be retried (e.g. network connection problem will not cause you
     * to re-upload all the bytes).
     *
     * @param dbxClient Dropbox user authenticated client
     * @param localFIle local file to upload
     * @param dropboxPath Where to upload the file to within Dropbox
     */
    private static void chunkedUploadFile(DbxClientV2 dbxClient, File localFile, String dropboxPath) {
        long size = localFile.length();

        // assert our file is at least the chunk upload size. We make this assumption in the code
        // below to simplify the logic.
        if (size < CHUNKED_UPLOAD_CHUNK_SIZE) {
            System.err.println("Arquivo pequeno, Use uploadFile(...) no lugar desse.");
            System.exit(1);
            return;
        }

        long uploaded = 0L;
        DbxException thrown = null;

        // Chunked uploads have 3 phases, each of which can accept uploaded bytes:
        //
        //    (1) Start: initiate the upload and get an upload session ID
        //    (2) Append: upload chunks of the file to append to our session
        //    (3) Finish: commit the upload and close the session
        //
        // We track how many bytes we uploaded to determine which phase we should be in.
        String sessionId = null;
        for (int i = 0; i < CHUNKED_UPLOAD_MAX_ATTEMPTS; ++i) {
            if (i > 0) {
                System.out.printf("Tentando novamente upload em partes(%d / %d tentativas)\n", 
                        i + 1, CHUNKED_UPLOAD_MAX_ATTEMPTS);
            }

            try (InputStream in = new FileInputStream(localFile)) {
                // if this is a retry, make sure seek to the correct offset
                in.skip(uploaded);

                // (1) Start
                if (sessionId == null) {
                    System.out.println("O seu download iniciará em instantes!");
                    sessionId = dbxClient.files().uploadSessionStart()
                        .uploadAndFinish(in, CHUNKED_UPLOAD_CHUNK_SIZE)
                        .getSessionId();
                    uploaded += CHUNKED_UPLOAD_CHUNK_SIZE;
                    printProgress(uploaded, size);
                }

                UploadSessionCursor cursor = new UploadSessionCursor(sessionId, uploaded);

                // (2) Append
                while ((size - uploaded) > CHUNKED_UPLOAD_CHUNK_SIZE) {
                    dbxClient.files().uploadSessionAppendV2(cursor)
                        .uploadAndFinish(in, CHUNKED_UPLOAD_CHUNK_SIZE);
                    uploaded += CHUNKED_UPLOAD_CHUNK_SIZE;
                    printProgress(uploaded, size);
                    cursor = new UploadSessionCursor(sessionId, uploaded);
                }

                // (3) Finish
                long remaining = size - uploaded;
                CommitInfo commitInfo = CommitInfo.newBuilder(dropboxPath)
                    .withMode(WriteMode.ADD)
                    .withClientModified(new Date(localFile.lastModified()))
                    .build();
                FileMetadata metadata = dbxClient.files().uploadSessionFinish(cursor, commitInfo)
                    .uploadAndFinish(in, remaining);

                System.out.println("\n\n" + metadata.toStringMultiline());
                return;
            } catch (RetryException ex) {
                thrown = ex;
                // RetryExceptions are never automatically retried by the client for uploads. Must
                // catch this exception even if DbxRequestConfig.getMaxRetries() > 0.
                sleepQuietly(ex.getBackoffMillis());
                continue;
            } catch (NetworkIOException ex) {
                thrown = ex;
                // network issue with Dropbox (maybe a timeout?) try again
                continue;
            } catch (UploadSessionLookupErrorException ex) {
                if (ex.errorValue.isIncorrectOffset()) {
                    thrown = ex;
                    // server offset into the stream doesn't match our offset (uploaded). Seek to
                    // the expected offset according to the server and try again.
                    uploaded = ex.errorValue
                        .getIncorrectOffsetValue()
                        .getCorrectOffset();
                    continue;
                } else {
                    // Some other error occurred, give up.
                    System.err.println("Erro 3 em upload para o Dropbox: " + ex.getMessage());
                    System.exit(1);
                    return;
                }
            } catch (UploadSessionFinishErrorException ex) {
                if (ex.errorValue.isLookupFailed() && ex.errorValue.getLookupFailedValue().isIncorrectOffset()) {
                    thrown = ex;
                    // server offset into the stream doesn't match our offset (uploaded). Seek to
                    // the expected offset according to the server and try again.
                    uploaded = ex.errorValue
                        .getLookupFailedValue()
                        .getIncorrectOffsetValue()
                        .getCorrectOffset();
                    continue;
                } else {
                    // some other error occurred, give up.
                    System.err.println("Erro 4 em upload para o Dropbox: " + ex.getMessage());
                    System.exit(1);
                    return;
                }
            } catch (DbxException ex) {
                System.err.println("Erro 5 em upload para o Dropbox: " + ex.getMessage());
                System.exit(1);
                return;
            } catch (IOException ex) {
                System.err.println("Erro 2 lendo arquivo \"" + localFile + "\": " + ex.getMessage());
                System.exit(1);
                return;
            }
        }

        // if we made it here, then we must have run out of attempts
        System.err.println("Limite de tentativas ultrapassado. Erro mais recente: " + thrown.getMessage());
        System.exit(1);
    }

    private static void printProgress(long uploaded, long size) {
        System.out.printf("\rCarregado(s) %d / %d bytes (%.2f%%)", uploaded, size, 100 * (uploaded / (double) size));
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            // just exit
            System.err.println("Erro em upload para o Dropbox: interrompido durante intervalo.");
            System.exit(1);
        }
    }
    
    private void selectAndDoUpload(DbxAuthInfo authInfo, File localFile, String dropboxPath){
        // Create a DbxClientV2, which is what you use to make API calls.
        DbxRequestConfig requestConfig = new DbxRequestConfig("apseeUploader");
        DbxClientV2 dbxClient = new DbxClientV2(requestConfig, authInfo.getAccessToken(), authInfo.getHost());

        // upload the file with simple upload API if it is small enough, otherwise use chunked
        // upload API for better performance. Arbitrarily chose 2 times our chunk size as the
        // deciding factor. This should really depend on your network.
        if (localFile.length() <= (2 * CHUNKED_UPLOAD_CHUNK_SIZE)) {
            uploadFile(dbxClient, localFile, dropboxPath);
        } else {
            chunkedUploadFile(dbxClient, localFile, dropboxPath);
        }
    }
    
    private DbxAuthInfo authInfo;
    private DbxAuthInfo getAuthInfo() {
        return authInfo;
    }
    
    private void readAuthFromFile(File file){
        try {
            authInfo = DbxAuthInfo.Reader.readFromFile(file.getCanonicalPath());
        } catch (JsonReader.FileLoadException | IOException ex) {
            System.err.println("Error loading <auth-file>: " + ex.getMessage());
            System.exit(1);
        }
    }
    
    public void run(){
        File file = Util.promptUsuario();
        readAuthFromFile(Auth.getAuthFile());
        final String filepath = "/" + file.getName();
        selectAndDoUpload(getAuthInfo(), file.getAbsoluteFile(), filepath);
    }
}
