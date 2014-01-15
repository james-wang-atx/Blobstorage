// file Upload.java

package blobstorage;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.*;

import com.google.appengine.api.blobstore.FileInfo;
import com.google.appengine.api.blobstore.BlobInfo;
import com.google.appengine.api.blobstore.BlobInfoFactory;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.blobstore.UploadOptions;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;

@SuppressWarnings("serial")
public class Upload extends HttpServlet {
    private static final Logger log = Logger.getLogger(Serve.class.getName());
    private BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
    private DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
    private BlobInfoFactory  blobInfoFactory = new BlobInfoFactory(datastoreService);
    
    private static final String PROP_NAME_CLOUD_STORAGE_BUCKET_NAME = "cloudStorageBucketName";
        
    public String GetNewUploadURL() {
        
        String bucket = System.getProperty(PROP_NAME_CLOUD_STORAGE_BUCKET_NAME); //"roverX-GCS-Bucket";
        UploadOptions uploadOptions = UploadOptions.Builder.withGoogleStorageBucketName(bucket);
        String uploadUrl = null;
        
        log.info("GetNewUploadURL: bucket: " + bucket);
        
        try
        {
            uploadUrl = BlobstoreServiceFactory.getBlobstoreService().createUploadUrl("/upload", uploadOptions);
            String myIp = InetAddress.getLocalHost().getHostAddress().toString();
        
            if( System.getProperty("com.google.appengine.runtime.environment") == "Development" ) {
                uploadUrl = uploadUrl.replace("127.0.0.1", myIp);
                uploadUrl = uploadUrl.replace("localhost", myIp);
            }
        }
        catch ( UnknownHostException e) {
        }
        
        return uploadUrl;
    }

          
    /*
     * (non-Javadoc)
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     * 
     * The metadata from the upload form comes in as headers following the boundary markers:
     *  ------WebKitFormBoundaryXuA6PslJ1A21INU9
     *  Content-Disposition: form-data; name="foo"
     *  
     *  ------WebKitFormBoundaryXuA6PslJ1A21INU9
     *  Content-Disposition: form-data; name="roverX"; filename="rover_1_cam1.jpg"
     *  Content-Type: image/jpeg
     *  
     *  ......JFIF.....,.,.....
     *  ...
     *  ...
     *  
     *  
     *  
     *  These two boundary header sections are due to the <form> definition in JSP, example:
     *  
     *    <form action="<%= blobstoreService.createUploadUrl("/upload") %>" method="post" enctype="multipart/form-data">
     *        <input type="text" name="foo">
     *        <input type="file" name="roverX">
     *        <input type="submit" value="Submit">
     *    </form>
     *    
     *    NOTE that the first <input> of type "text" and value "foo" has been deleted, so that first boundary header section should
     *      no longer be present on wire.
     *      
     *      
     * Stream ends with:
     *      \r\n------WebKitFormBoundaryXuA6PslJ1A21INU9--\r\n
     */
    
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {

        //@SuppressWarnings("deprecation")
        //Map<String, BlobKey> blobs = blobstoreService.getUploadedBlobs(req);
        
        Map<String, List<BlobKey>> blobs = blobstoreService.getUploads(req);
        
        // EXAMPLE LOG OUTPUT --> INFO: UPLOAD: blobs map: {roverX=<BlobKey: rpg8D5DCjsubtsNaTfxrpg>}
        //   The String comes from the html form: <input type="file" name="roverX">
        //   This form data is sent in HTTP as MIME header values after boundary marker (see above)
        log.info("UPLOAD: BlobKey map: " + blobs);
        
        // The Map is keyed on the name="roverX" sent in header and also defined in index.jps (i.e., <input type="file" name="roverX">),
        //   so only one list keyed on "roverX" is expected.  Of course, the List should only contain one entry for the one image uploaded.
        List<BlobKey> blobKeyList = blobs.get("roverX");
        
        // This Map is keyed same way as the String-to-List<BlobKey> above, so only one list is expected keyed on "roverX",
        //   which should be accessible via: List<FileInfo> fileInfoList = fileInfos.get("roverX");
        //   Of course, as with other Map, the List should only contain one entry for the one image uploaded.
        Map<String, List<FileInfo>> fileInfos = blobstoreService.getFileInfos(req);
        
        // EXAMPLE LOG OUTPUT -->
        //   INFO: UPLOAD: FileInfo map: {roverX=[<FileInfo: contentType = image/jpeg, creation = Wed Jan 01 23:10:33 CST 2014, filename = rover_2_cam1.jpg, size = 129083, md5Hash = b350fec552ae4e468247942665457167>]}
        //   INFO: UPLOAD: FileInfo map: {roverX=[<FileInfo: contentType = image/jpeg, creation = Sun Jan 05 20:37:55 CST 2014, filename = rover_1_cam1.jpg, size = 129896, md5Hash = f7c80b3a1f32df68384ed59487b5cbba, gsObjectName = /gs/roverX-GCS-Bucket/fake-encoded_gs_key:cm92ZXJYLUdDUy1CdWNrZXQvSkZDQWhKMTF3TFhhdzU3Q3gtbi1Sdw-f7c80b3a1f32df68384ed59487b5cbba>]}
        log.info("UPLOAD: FileInfo map: " + fileInfos);
        
        List<FileInfo> fileInfoList = fileInfos.get("roverX");
        
        if( fileInfoList != null)
        {
            FileInfo fileinfo = fileInfoList.get(0);
            String gcsName = fileinfo.getGsObjectName();
            log.info("UPLOAD: GCS Name: " + gcsName);
        }
        
        // It is possible to have each rover set the MIME header, like this:
        //   name="rover_<id>"; filename="rover_<id>.jpg"
        // But, we would have to build a map here (e.g., one map for each rover_<id> containing timestamps and BlobKey's for every
        //   video frame belonging to that rover.)
        
        BlobKey blobKey = null;
        
        if(blobKeyList == null || blobKeyList.isEmpty()) {        
            res.sendRedirect("/");
            return;
        }
        
        blobKey = blobKeyList.get(0);            
        
        BlobInfo i = blobInfoFactory.loadBlobInfo(blobKey);        
        log.info("UPLOAD: __BlobInfo__: " + i );
        log.info("UPLOAD: blobKey: " + blobKey );

        //res.sendRedirect("/serve?blob-key=" + blobKey.getKeyString());
        
        res.setContentType("text/plain");
        
        // java client in rover is looking for this to use for next image POST
        res.getWriter().println("NEXT-uploadURL: " + GetNewUploadURL() + "\n" );        
        
        // rest of response is extra debug info
        
        res.getWriter().println("request: " + req);        
        @SuppressWarnings("unchecked")
        Enumeration<String> paramNames = req.getParameterNames();
        while(paramNames.hasMoreElements())
        {
            String paramName = paramNames.nextElement();
            String paramValue = req.getParameter(paramName);
            res.getWriter().println("request:  " + paramName + "=" + paramValue );
        }     
        
        res.getWriter().println("__BlobInfo__: " + i);
        res.getWriter().println("blobKey: " + blobKey);
    }
}
