package blobstorage;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.servlet.http.*;

//import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.UploadOptions;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.tools.cloudstorage.GcsFileOptions;
import com.google.appengine.tools.cloudstorage.GcsFilename;
import com.google.appengine.tools.cloudstorage.GcsOutputChannel;
import com.google.appengine.tools.cloudstorage.GcsService;
import com.google.appengine.tools.cloudstorage.GcsServiceFactory;
import com.google.appengine.tools.cloudstorage.RetryParams;



import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


@SuppressWarnings("serial")
public class UploadURL extends HttpServlet {
    private static final String PROP_NAME_CLOUD_STORAGE_BUCKET_NAME = "cloudStorageBucketName";
    private static final Logger log = Logger.getLogger(Serve.class.getName());

    //private BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
    String bucket = System.getProperty(PROP_NAME_CLOUD_STORAGE_BUCKET_NAME); //"roverX-GCS-Bucket";
    UploadOptions uploadOptions = UploadOptions.Builder.withGoogleStorageBucketName(bucket);
    
    private String uploadUrl = null;
    
    public void SetUploadUrl() {
        try
        {
            uploadUrl = BlobstoreServiceFactory.getBlobstoreService().createUploadUrl("/upload", uploadOptions);
            String myIp = InetAddress.getLocalHost().getHostAddress().toString();
        
            log.severe("UploadURL: bucket: " + bucket + ", uploadUrl=" + uploadUrl);
            
            if( System.getProperty("com.google.appengine.runtime.environment") == "Development" ) {
                uploadUrl = uploadUrl.replace("127.0.0.1", myIp);
                uploadUrl = uploadUrl.replace("localhost", myIp);
            }
        }
        catch ( UnknownHostException e) {
        }
    }
    
    
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
    {
        SetUploadUrl();
            
        resp.setContentType("text/plain");
        resp.getWriter().println(uploadUrl);
        log.info("UPLOAD-URL REQ: " + uploadUrl );
        log.info("UPLOAD-URL REQ: bucket = " + bucket );
        log.info("UPLOAD-URL REQ: uploadOptions = " + uploadOptions );            
    }
}
