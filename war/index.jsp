<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.google.appengine.api.blobstore.BlobstoreServiceFactory" %>
<%@ page import="com.google.appengine.api.blobstore.BlobstoreService" %>
<%@ page import="com.google.appengine.api.blobstore.UploadOptions" %>
<%@ page import="java.net.InetAddress" %>
<%@ page import="java.net.UnknownHostException" %>

<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<%!      
    static class BlobstoreServiceIPFilter {
        private static final String PROP_NAME_CLOUD_STORAGE_BUCKET_NAME = "cloudStorageBucketName";
            
        static String createUploadUrl(String successPath) {
            String bucket = System.getProperty(PROP_NAME_CLOUD_STORAGE_BUCKET_NAME); //"roverX-GCS-Bucket";
            UploadOptions uploadOptions = UploadOptions.Builder.withGoogleStorageBucketName(bucket);
            
            String uploadUrl = BlobstoreServiceFactory.getBlobstoreService().createUploadUrl(successPath, uploadOptions);
            
            System.out.println("JSP: bucket = " + bucket);
            System.out.println("JSP: uploadUrl = " + uploadUrl);
                        
            try
            {
                String myIp = InetAddress.getLocalHost().getHostAddress().toString();
            
                if( System.getProperty("com.google.appengine.runtime.environment") == "Development" )
                {
                   uploadUrl = uploadUrl.replace("127.0.0.1", myIp);
                   uploadUrl = uploadUrl.replace("localhost", myIp);
                }
            }
            catch ( UnknownHostException e) {
            }
            
            return uploadUrl;
        }
        
        static String getuploadURLString() {
            String uploadURLString = "";
            
            try
            {
                String myIp = InetAddress.getLocalHost().getHostAddress().toString();
                uploadURLString = "http://" + myIp + ":8080/uploadURL";
            }
            catch ( UnknownHostException e) {
            }
            
            return uploadURLString;
        }
        
    }
%>
<%
    BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();    
%>


<html>
    <head>
        <title>Video Storage</title>
        <link type="text/css" rel="stylesheet" href="/stylesheets/main.css" />
        <link rel="import" href="<%= BlobstoreServiceIPFilter.getuploadURLString() %>">
    </head>
    <body>
        <header><h1>Security and Safety Rover - Cloud Video Storage</h1></header>
        <div class="container">
	        <form action="<%= BlobstoreServiceIPFilter.createUploadUrl("/upload") %>" method="post" enctype="multipart/form-data">
		        <fieldset>
		          <legend>Upload Rover Image(jpg) file:</legend>
		            <input type="file" name="roverX"> <br>
		            <input type="submit" value="Submit">
		        </fieldset>
	        </form>
	        
	        <form action="/search" method="get">
		        <fieldset>
		          <legend>Search information:</legend>
		          Rover_id: <input type="text" name="roverX"> <br>
		          Date [MM dd yyyy HH:mm:ss zzz]: <input type="text" name="datestring" size="30"> <br>
		          <input type="radio" name="mode" value="blobinfo" checked>BlobInfo<br>
		          <input type="radio" name="mode" value="video">Video<br>          		          
		          <input type="submit" value="Search" />
		        </fieldset>
	        </form>
	        
	        <form action="<%= BlobstoreServiceIPFilter.getuploadURLString() %>" method="get" target="framename">
	        <input type="submit" value="<%= BlobstoreServiceIPFilter.getuploadURLString() %>">
	        </form>
	        
             
	        
        <form action="/index.jsp" enctype="text/plain" method="get" name="putFile" id="putFile">
          <div>
            Bucket: <input type="text" name="bucket" />
            File Name: <input type="text" name="fileName" />
            <br /> File Contents: <br />
            <textarea name="content" id="content" rows="3" cols="60"></textarea>
            <br />
            <input type="submit" onclick='uploadFile(this)' value="Upload Content" />
          </div>
        </form>
	        
      <td>
        <form name="getFile">
          <div>
            Bucket: <input type="text" name="bucket" id="bucket" />
            File Name: <input type="text" name="fileName" id="fileName" />
          </div>
        </form>
        <form action="/index.html" method="get" name="submitGet">
          <div>
            <input type="submit" onclick='changeGetPath(this)' value="Download Content" />
          </div>
        </form>
      </td>
	        
        </div>
        
  <script>

  
      function setDownloadDefaults() {
        var url = location.search;
        var bucketArg = url.match(/bucket=[^&]*&/);
        if (bucketArg !== null) {
          document.getElementById("bucket").value = bucketArg.shift().slice(7, -1);
        }
        var fileArg = url.match(/fileName=[^&]*&/);
        if (fileArg !== null) {
          document.getElementById("fileName").value = fileArg.shift().slice(9, -1);
        }
      }

      function changeGetPath() {
        var bucket = document.forms["getFile"]["bucket"].value;
        var filename = document.forms["getFile"]["fileName"].value;
        if (bucket == null || bucket == "" || filename == null || filename == "") {
          alert("Both Bucket and FileName are required");
          return false;
        } else {
          document.submitGet.action = "/gcs/" + bucket + "/" + filename;
        }
      }

      function uploadFile() {
        var bucket = document.forms["putFile"]["bucket"].value;
        var filename = document.forms["putFile"]["fileName"].value;
        if (bucket == null || bucket == "" || filename == null || filename == "") {
          alert("Both Bucket and FileName are required");
          return false;
        } else {
          var postData = document.forms["putFile"]["content"].value;
          document.getElementById("content").value = null;

          var request = new XMLHttpRequest();
          request.open("POST", "/gcs/" + bucket + "/" + filename, false);
          request.setRequestHeader("Content-Type", "text/plain;charset=UTF-8");
          request.send(postData);
        }
      }
    </script>
        
    </body>
</html>