//file Serve.java

package blobstorage;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.logging.Logger;

import javax.servlet.http.*;

import com.google.appengine.api.blobstore.BlobInfo;
import com.google.appengine.api.blobstore.BlobInfoFactory;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;

@SuppressWarnings("serial")
public class Serve extends HttpServlet {
 private static final Logger log = Logger.getLogger(Serve.class.getName());
 private BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
 private DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
 private BlobInfoFactory  blobInfoFactory = new BlobInfoFactory(datastoreService);

 @Override
 public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
     String blobkeystring = req.getParameter("blob-key");
     
     if ( blobkeystring != null) {
         BlobKey blobKey = new BlobKey(blobkeystring);
         BlobInfo i = blobInfoFactory.loadBlobInfo(blobKey);
         
         log.info("SERVE: __BlobInfo__: " + i.getFilename() + " " + i.getCreation().toString() );
                  
         blobstoreService.serve(blobKey, res);
         
         res.addDateHeader("Last-Modified", i.getCreation().getTime());         
         
         log.info("SERVE: res: " + res );
     }
     else {
         res.sendRedirect("/");
     }
         
 }
 
}