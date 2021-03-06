// file Upload.java

package blobstorage;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.*;

//import com.google.appengine.api.LifecycleManager;
//import com.google.appengine.api.LifecycleManager.ShutdownHook;
import com.google.appengine.api.blobstore.FileInfo;
import com.google.appengine.api.blobstore.BlobInfo;
import com.google.appengine.api.blobstore.BlobInfoFactory;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.blobstore.UploadOptions;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;

@SuppressWarnings("serial")
public class Upload extends HttpServlet {
    private static final Logger log = Logger.getLogger(Serve.class.getName());
    private BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
    private DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
    private BlobInfoFactory  blobInfoFactory = new BlobInfoFactory(datastoreService);
  //private EMailRequestManagerSingleton emrm = null;
    
    private static final String PROP_NAME_CLOUD_STORAGE_BUCKET_NAME = "cloudStorageBucketName";
        
    public String GetNewUploadURL() {
        
        String bucket = System.getProperty(PROP_NAME_CLOUD_STORAGE_BUCKET_NAME); //"roverX-GCS-Bucket";
        UploadOptions uploadOptions = UploadOptions.Builder.withGoogleStorageBucketName(bucket);
        String uploadUrl = null;
        
        log.severe("GetNewUploadURL: bucket: " + bucket);
        
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
     *  Content-Disposition: form-data; name="rover1"; date="ddd"; fire="false"; water="false"
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
     *    NOTE that the first <input> of type "text" and value "foo" shows up as the first boundary header section.
     *      
     *      
     * Stream ends with:
     *      \r\n------WebKitFormBoundaryXuA6PslJ1A21INU9--\r\n
     */
    
    @SuppressWarnings("deprecation")
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {

        log.severe("Upload.doPost:Entry");
        
        //@SuppressWarnings("deprecation")
        //Map<String, BlobKey> blobs = blobstoreService.getUploadedBlobs(req);
        
        // The upload url looks like this: http://10.2.100.29:8080/_ah/upload/aglub19hcHBfaWRyIgsSFV9fQmxvYlVwbG9hZFNlc3Npb25fXxiAgICAgICQCQw
        // I believe the blobkey will be parsed from the end of URL.  I don't know how multiple keys are encoded ... hazard a guess that
        //  xxx.createUploadUrl() takes a multi-key argument and keys would then have to be encoded as QSPs. 
        Map<String, List<BlobKey>> blobs = blobstoreService.getUploads(req);
        
        // Note that the above single line does all the work of receiving the blob in multi-part payload and saving it to storage,
        //   everything below here is for debug purposes + sending the response (next upload url to our special repeat-post client)
        
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
        //   INFO: UPLOAD: FileInfo map: {roverX=[<FileInfo: contentType = image/jpeg, creation = Fri Aug 08 10:19:29 CDT 2014, filename = getfile_4.jpg, size = 140828, md5Hash = f6a6d02fc55b21271b17a9eefb3d867b, gsObjectName = /gs/roverX-GCS-Bucket/fake-encoded_gs_key:cm92ZXJYLUdDUy1CdWNrZXQvNXFfUHRGMG1YMmtjMHN4NG1GbjA5QQ-f6a6d02fc55b21271b17a9eefb3d867b>]}
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
        // CURRENTLY: name = "roverX" and filename=rover_<#>_cam<#>.
        //   So, the rover id can be decoded from the filename.
        //   ALSO, We get the rover id value separately with the METADATA ALARMS, below...
        
        BlobKey blobKey = null;
        
        if(blobKeyList == null || blobKeyList.isEmpty()) {        
            res.sendRedirect("/");
            return;
        }
        
        blobKey = blobKeyList.get(0);            
        
        BlobInfo i = blobInfoFactory.loadBlobInfo(blobKey);        
        log.info("UPLOAD: __BlobInfo__: " + i );
        //INFO: UPLOAD: __BlobInfo__: <BlobInfo: <BlobKey: encoded_gs_key:cm92ZXJYLUdDUy1CdWNrZXQvRG55RnZiMnR0RldBbDVPdnowTjNDdw>, contentType = image/jpeg, creation = Thu Aug 07 14:15:50 CDT 2014, filename = rover_1_cam1.jpg, size = 140016, md5Hash = ed21d7f004893bd98f11747d56d7ad5b>
        log.severe("UPLOAD: blobKey: " + blobKey );
        //INFO: UPLOAD: blobKey: <BlobKey: encoded_gs_key:cm92ZXJYLUdDUy1CdWNrZXQvRG55RnZiMnR0RldBbDVPdnowTjNDdw>
        
        //res.sendRedirect("/serve?blob-key=" + blobKey.getKeyString());
        
        res.setContentType("text/plain");
        
        // java client in rover is looking for this to use for next image POST
        res.getWriter().println("NEXT-uploadURL: " + GetNewUploadURL() + "\n" );        
        
        ////////////////////////////////////////////////////////////////////////////////////////////////
        // rest of response is extra debug info + 
        //   METADATA entity storage which includes the alarm data
        //
        
        res.getWriter().println("request: " + req);

        // datastore "kind" for this entity will be "METADATA"
        Entity metadata = new Entity("METADATA");
        
        // enumerate the extra sensor data come in as separate multi-part segments
        @SuppressWarnings("unchecked")
        Enumeration<String> paramNames = req.getParameterNames();
        
        if( paramNames.hasMoreElements() )
        {
            DateFormat df = new SimpleDateFormat("MM dd yyyy HH:mm:ss zzz");
            Date now = new Date();
            
            //boolean fireDetected = false;
            //boolean waterDetected = false;
            //String fireTimestamp = "";
            //String waterTimestamp = "";
            
//            metadata.setProperty( "creation", df.format(now) );
            metadata.setProperty( "creation", now );
            while(paramNames.hasMoreElements())
            {
                String paramName = paramNames.nextElement();
                String paramValue = req.getParameter(paramName);
                
                res.getWriter().println("request:  " + paramName + "=" + paramValue );
                
                log.severe("UPLOAD: ATER URL RESPONSE - request: " + paramName + "=" + paramValue );
                /*  EXAMPLE:
                        INFO: UPLOAD: ATER URL RESPONSE - request: water=true
                        INFO: UPLOAD: ATER URL RESPONSE - request: name=rover1
                        INFO: UPLOAD: ATER URL RESPONSE - request: date=11 20 2014 09:03:30 CST
                        INFO: UPLOAD: ATER URL RESPONSE - request: fire=true
                 */
                
                metadata.setProperty(paramName, paramValue);
                //metadata.getProperty(propertyName)
                                
                /*
                //see ClientMultipartFormPost.java ~311
                if(paramName.equals("fire")) {
                    if( !paramValue.equals("false") ) {
                        fireDetected = true;
                        fireTimestamp = paramValue;
                    }
                } else if(paramName.equals("water")) {
                    if( !paramValue.equals("false") ) {
                        waterDetected = true;
                        waterTimestamp = paramValue;        
                    }
                } //TODO: add "intrusion-VMD" and "intrusion-PIR"
                */
            }     
            
            // SAVE the sensor data to the datastore
            datastoreService.put(metadata);
            
            AlarmRouterSingleton.GetInstance().ProcessAlarm(metadata, blobKey);
            /*
            if( fireDetected || waterDetected ) {
                log.severe("UPLOAD: got alarm data - fire = " + fireTimestamp + ", water = " + waterTimestamp );
                // queue up email job - the job descr should contain the 'blobKey' value
                String htmlBody = "<b> fire=" + fireTimestamp + ", water=" + waterTimestamp + "</b>";
                emrm.AddRequest(new EMailRequest("ss-rover-blobserver@appspot.gserviceaccount.com", 
                                                 "james.wang.atx@gmail.com", 
                                                 "me",
                                                 "rover alarm",
                                                 htmlBody,
                                                 blobKey));
                Queue queue = QueueFactory.getDefaultQueue();
                queue.add( TaskOptions.Builder.withUrl("/task").param("key", "boo"));
            }
            */
        }
        
        // debug info:
        res.getWriter().println("__BlobInfo__: " + i);
        res.getWriter().println("blobKey: " + blobKey);
        
        ////////////////////////////////////////////////////////////////////////////////////////////////////
        // PURGE OLD DATA:
        // query for all datastore blob entities for this bucket, then filter on date/older ones for delete
        // normally you only have 1 bucket per app, so there is no issue with choosing buckets.
        // we will store all rover(s) data in same bucket distinguished with filename.
        
        log.severe("Upload.doPost: Creating new Query __BlobInfo__");
        
        Query query = new Query("__BlobInfo__");
        
        Date date = new Date(); // "now"
        
        // move back to 10 minutes ago (i.e., we will delete all video that is older than that)
        date.setTime( date.getTime() - 1000*60*10 );
        
        log.severe("Upload.doPost: adding 10min filter to blobinfo query results");
        
        query.addFilter("creation", FilterOperator.LESS_THAN, date);
        
        log.severe("Upload.doPost: start interating blobinfo results");
        
        BlobInfoFactory blobInfoFactory = new BlobInfoFactory();
        
        for (Entity ent : datastoreService.prepare(query).asIterable())
        {
            BlobInfo blobInfo = blobInfoFactory.createBlobInfo(ent);
            
            //Date creationdate = (Date)ent.getProperty("creation");            
            log.severe("DELETING Blob Entity: " + ent.toString() );
            try {
                //datastoreService.delete( ent.getKey() );
                blobstoreService.delete(blobInfo.getBlobKey());
            }
            catch( java.lang.IllegalArgumentException e ) {
                log.severe("DELETING Entity failed!" );                
            }
        }
        
        // query for all datastore METADATA entities for this bucket, then filter on date/older ones for deletion
        
        log.severe("Upload.doPost: Creating new Query METADATA");
        Query metadataquery = new Query("METADATA");
        
        log.severe("Upload.doPost: adding 10min filter to metadata query results");
        
        // use same 10 minute old date from prior video query
        metadataquery.addFilter("creation", FilterOperator.LESS_THAN, date);
        
        log.severe("Upload.doPost: start interating metadata results");
        
        for (Entity ent : datastoreService.prepare(metadataquery).asIterable())
        {
            //Date creationdate = (Date)ent.getProperty("creation");            
            log.severe("DELETING METADATA Entity: " + ent.toString() );
            try {
                datastoreService.delete( ent.getKey() );
            }
            catch( java.lang.IllegalArgumentException e ) {
                log.severe("DELETING Entity failed!" );                
            }
        }
    }


    // This class has a Warmup request configured for it in web.xml:
    //   <load-on-startup>1</load-on-startup>
    //
    //   see https://developers.google.com/appengine/docs/java/config/appconfig#using_a_load-on-startup_servlet    
    public void init() {
        log.info("Upload.init() --------------------------" );

        //SendMail.Init();
        
        // works
        //SendMail.SendEMail("james.wang.atx@gmail.com",
        //                   "james.wang@krystallizetechnologies.com",
        //                   "me",
        //                   "testsubject1",
        //                   "test body");

        // worked after adding soteriaut@gmail.com to permissions on page: https://console.developers.google.com/project/ss-rover-blobserver/permissions
        // BUT mail ended up in SPAM?
        //SendMail.SendEMail("soteriaut@gmail.com",
        //                   "james.wang.atx@gmail.com",
        //                   "me",
        //                   "testsubject2",
        //                   "test body2");
        
        // worked but also ended up in SPAM!
        //SendMail.SendEMail("james.wang.atx@gmail.com",
        //        "james.wang.atx@gmail.com",
        //        "me",
        //        "testsubject3",
        //        "test body3");

        // this worked
        //SendMail.SendEMail("ss-rover-blobserver@appspot.gserviceaccount.com",
        //        "james.wang@krystallizetechnologies.com",
        //        "me",
        //        "testsubject4",
        //        "test body4");
        
        SendMail.SendEMail("ss-rover-blobserver@appspot.gserviceaccount.com",
                "james.wang.atx@gmail.com",
                "James Wang",
                "ss-rover-blobserver.appspot.com site",
                "ss-rover-blobserver.appspot.com iniatialized!");
        
      //emrm = EMailRequestManagerSingleton.GetInstance();
        
        //LifecycleManager.getInstance().setShutdownHook(new ShutdownHook() {
        //    public void shutdown() {
        //        EMailRequestManagerSingleton.GetInstance().Shutdown();
        //    }
        //  });
    }

}
