package blobstorage;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.Enumeration;
import java.util.List;
//import java.util.Map;
//import java.util.Map.Entry;
//import java.util.Vector;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.Date;
import java.util.Iterator;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//import com.google.appengine.api.blobstore.BlobInfo;
//import com.google.appengine.api.blobstore.BlobInfoFactory;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
//import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query;
//import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;

@SuppressWarnings("serial")
public class Search extends HttpServlet {
    private static final Logger log = Logger.getLogger(Serve.class.getName());
    private BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
    //private DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
    //private BlobInfoFactory  blobInfoFactory = new BlobInfoFactory(datastoreService);

    @SuppressWarnings("deprecation")
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String datestring = req.getParameter("datestring");
        String rover_id = req.getParameter("roverX");          // expecting: "rover_1_cam1" or "rover_2_cam1"
        String modestring = req.getParameter("mode");          // expecting: "blobinfo", "video", or empty

        @SuppressWarnings("unchecked")
        Enumeration<String> paramNames = req.getParameterNames();
        while(paramNames.hasMoreElements())
        {
            String paramName = paramNames.nextElement();
            String paramValue = req.getParameter(paramName);
            log.info("SEARCH: req:  " + paramName + "=" + paramValue );
        }     

        DateFormat df_search = new SimpleDateFormat("MM dd yyyy HH:mm:ss zzz");

        Date date = null;

        if (datestring != null && !datestring.isEmpty()) {
            try {
                date =  df_search.parse(datestring);         
                log.info("SEARCH: datestring:  " + datestring );
            }
            catch(ParseException e) {
                log.info("SEARCH: ParseException");
                res.setContentType("text/plain");
                res.getWriter().println("Search Failed due to invalid date string");
                return;
            }
        }

        Query query = new Query("__BlobInfo__");

        if(rover_id != null && !rover_id.isEmpty()) {
            query.addFilter("filename", FilterOperator.EQUAL, rover_id + ".jpg");
        }

        if(date != null) {
            query.addFilter("creation", FilterOperator.GREATER_THAN_OR_EQUAL , date);
        }

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService(); 
        PreparedQuery pq = datastore.prepare(query); 
        List<Entity> entList = pq.asList(FetchOptions.Builder.withLimit(10)); 

        log.info("SEARCH: modestring='" + modestring + "'");

        if(entList.isEmpty()) {
            log.info("SEARCH: query failed!");
            res.setContentType("text/plain");
            res.getWriter().println("Search Failed");         
            return;
        }

        if(modestring == null || modestring.equalsIgnoreCase("blobinfo")) {
            handleBlobInfoRequest(req, res, date, datestring, rover_id, entList);
        }
        else {
            if(date == null)
            {
                res.setContentType("text/plain");
                res.getWriter().println("Error! Date is missing (It is required for Video Search)");         
                return;
            }
            
            handleVideoRequest(req, res, date, datestring, rover_id, entList);
        }
    }

    public void handleBlobInfoRequest( HttpServletRequest req,
            HttpServletResponse res,
            Date date,
            String datestring,
            String rover_id,
            List<Entity> entList
            ) throws IOException {
        
        // caller must ensure query results are non-empty
        assert(!entList.isEmpty());

        Iterator<Entity> iterator = entList.iterator();
        int loop = 0;
        while (iterator.hasNext()) {
            Entity ent = iterator.next();

            String[] parts1 = ent.getKey().toString().split("\"");
            String[] parts2 = parts1[1].split("\"");
            String blobkeystring = parts2[0];

            Date creationdate = (Date)ent.getProperty("creation");
            String creationstring = creationdate.toString();
            String filename = (String)ent.getProperty("filename");

            if(date != null) {
                long msDelta = Math.abs(date.getTime() - creationdate.getTime());

                log.info("SEARCH:Blobinfo: requested date = " + datestring + ", found value = " + creationstring);
                log.info("SEARCH:Blobinfo: requested date = " + date.getTime() + ", found value = " + creationdate.getTime() + ", delta = " + msDelta + " ms");

                log.info("SEARCH:Blobinfo: requested rover = " + rover_id + ", found value = " + filename);

                // if the date matches search criteria
                if( msDelta < 1000) {
                    String rover_id_jpg = rover_id + ".jpg";

                    // if filename (indicating rover_id and camera number) matches search criteria
                    if( rover_id == null || rover_id.isEmpty() || rover_id_jpg.equalsIgnoreCase(filename)) {
                        res.setContentType("text/plain");
                        res.getWriter().println(ent);
                        res.getWriter().println("BLOBKEY = " + blobkeystring);
                        res.getWriter().println("CREATION = " + creationstring);
                        return;
                    }
                }

                // search failed
                res.setContentType("text/plain");
                res.getWriter().println("No blobs found matching search criteria");         
                return;
            }
            else {
                // this is request to dump all blobinfo                     
                if(loop == 0) res.setContentType("text/plain");

                res.getWriter().println(ent);
                res.getWriter().println("BLOBKEY = " + blobkeystring);
                res.getWriter().println("CREATION = " + creationstring);
            }

            loop++;
        }

        // done dumping list of blobinfo (not handling empty list with empty response warning since
        //   caller handles that condition.)
    }

    public void handleVideoRequest( HttpServletRequest req,
                                    HttpServletResponse res,
                                    Date date,
                                    String datestring,
                                    String rover_id,
                                    List<Entity> entList
                                    ) throws IOException {
        
        // caller must ensure query results are non-empty
        assert(!entList.isEmpty());
        
        Iterator<Entity> iterator = entList.iterator();
        while (iterator.hasNext()) {
            Entity ent = iterator.next();

            String[] parts1 = ent.getKey().toString().split("\"");
            String[] parts2 = parts1[1].split("\"");
            String blobkeystring = parts2[0];

            BlobKey blobKey = new BlobKey(blobkeystring);

            //blobstoreService.serve(blobKey, res);

            DateFormat df_creation = new SimpleDateFormat("EEE MMM dd kk:mm:ss zzz yyyy");
            
            Date creationdate = (Date)ent.getProperty("creation");
            String creationstring = creationdate.toString();
            String filename = (String)ent.getProperty("filename");

            try {
                Date dateCreation =  df_creation.parse(creationstring);
                res.addDateHeader("Last-Modified", dateCreation.getTime());
            }
            catch(ParseException e) {
                log.info("SEARCH:video: ParseException building 'Last-Modified' response header");
            }

            if (date != null) {
                long msDelta = Math.abs(date.getTime() - creationdate.getTime());

                log.info("SEARCH:video: requested date = " + datestring + ", found value = " + creationstring + ", delta = " + msDelta + " ms");
                log.info("SEARCH:Blobinfo: requested rover = " + rover_id + ", found value = " + filename);

                String rover_id_jpg = rover_id + ".jpg";

                // if the date matches search criteria
                if( msDelta < 1000) {
                    // if filename (indicating rover_id and camera number) matches search criteria
                    if( rover_id == null || rover_id.isEmpty() || rover_id_jpg.equalsIgnoreCase(filename)) {
                        blobstoreService.serve(blobKey, res);
                        return;                             
                    }
                }
            }
        }
        // (not handling empty list with empty response warning since caller handles that condition.)
    }

}

/*
 * <Entity [__BlobInfo__("encoded_gs_key:cm92ZXJYLUdDUy1CdWNrZXQvQmI4aTdKMUdRV281SWhKMkdtQzBWQQ")]:
    creation = Fri Aug 08 11:02:04 CDT 2014
    filename = rover_1_cam1.jpg
    md5_hash = ed21d7f004893bd98f11747d56d7ad5b
    content_type = image/jpeg
    size = 140016
>

BLOBKEY = encoded_gs_key:cm92ZXJYLUdDUy1CdWNrZXQvQmI4aTdKMUdRV281SWhKMkdtQzBWQQ
CREATION = Fri Aug 08 11:02:04 CDT 2014
<Entity [__BlobInfo__("encoded_gs_key:cm92ZXJYLUdDUy1CdWNrZXQvQmlQd2l5cG43Rk1SSEJiLU5PS0dUdw")]:
    creation = Fri Aug 08 10:00:17 CDT 2014
    filename = rover_1_cam1.jpg
    md5_hash = ed21d7f004893bd98f11747d56d7ad5b
    content_type = image/jpeg
    size = 140016
>

BLOBKEY = encoded_gs_key:cm92ZXJYLUdDUy1CdWNrZXQvQmlQd2l5cG43Rk1SSEJiLU5PS0dUdw
CREATION = Fri Aug 08 10:00:17 CDT 2014
<Entity [__BlobInfo__("encoded_gs_key:cm92ZXJYLUdDUy1CdWNrZXQvZ3luWWxXVmI5TUZoN0tocDc5TFh3QQ")]:
    creation = Fri Aug 08 09:59:02 CDT 2014
    filename = rover_1_cam1.jpg
    md5_hash = ed21d7f004893bd98f11747d56d7ad5b
    content_type = image/jpeg
    size = 140016
>

BLOBKEY = encoded_gs_key:cm92ZXJYLUdDUy1CdWNrZXQvZ3luWWxXVmI5TUZoN0tocDc5TFh3QQ
CREATION = Fri Aug 08 09:59:02 CDT 2014
<Entity [__BlobInfo__("encoded_gs_key:cm92ZXJYLUdDUy1CdWNrZXQvaGRpYW9ZOHUySVhxVlNRMklyaXFqdw")]:
    creation = Fri Aug 08 09:53:58 CDT 2014
    filename = rover_1_cam1.jpg
    md5_hash = ed21d7f004893bd98f11747d56d7ad5b
    content_type = image/jpeg
    size = 140016
>

BLOBKEY = encoded_gs_key:cm92ZXJYLUdDUy1CdWNrZXQvaGRpYW9ZOHUySVhxVlNRMklyaXFqdw
CREATION = Fri Aug 08 09:53:58 CDT 2014
<Entity [__BlobInfo__("encoded_gs_key:cm92ZXJYLUdDUy1CdWNrZXQvb0hWbHIxSGk0cWhLMUg1bnlsU1ZDUQ")]:
    creation = Fri Aug 08 11:04:20 CDT 2014
    filename = rover_1_cam1.jpg
    md5_hash = ed21d7f004893bd98f11747d56d7ad5b
    content_type = image/jpeg
    size = 140016
>

BLOBKEY = encoded_gs_key:cm92ZXJYLUdDUy1CdWNrZXQvb0hWbHIxSGk0cWhLMUg1bnlsU1ZDUQ
CREATION = Fri Aug 08 11:04:20 CDT 2014
<Entity [__BlobInfo__("encoded_gs_key:cm92ZXJYLUdDUy1CdWNrZXQvcjdZNW10Nnd2Ykl0TzNDRk5rWmdzQQ")]:
    creation = Fri Aug 08 10:10:25 CDT 2014
    filename = rover_1_cam1.jpg
    md5_hash = ed21d7f004893bd98f11747d56d7ad5b
    content_type = image/jpeg
    size = 140016
>

BLOBKEY = encoded_gs_key:cm92ZXJYLUdDUy1CdWNrZXQvcjdZNW10Nnd2Ykl0TzNDRk5rWmdzQQ
CREATION = Fri Aug 08 10:10:25 CDT 2014
*/
