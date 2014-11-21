package blobstorage;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import com.google.appengine.api.blobstore.BlobInfo;
import com.google.appengine.api.blobstore.BlobInfoFactory;
//import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;

public class SendMail
{
    //private static boolean hasInit = false;
    private static BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
    private static DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
    private static BlobInfoFactory  blobInfoFactory = new BlobInfoFactory(datastoreService);
    
/*
    public static void Init() {
        blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
        datastoreService = DatastoreServiceFactory.getDatastoreService();
        blobInfoFactory = new BlobInfoFactory(datastoreService);
        hasInit = true;
    }
*/    
    public static void SendEMail(String FromEmailAddr,
                                 String ToEmailAddr,
                                 String recipientName,
                                 String messageSubject,
                                 String messageText)
    {
        Properties prop = new Properties();
        Session session = Session.getDefaultInstance(prop,null);
        try
        {    
            Message msg = new MimeMessage(session);
            
            msg.setFrom(new InternetAddress(FromEmailAddr));
            msg.addRecipient(Message.RecipientType.TO, new InternetAddress(ToEmailAddr, recipientName));
            msg.setSubject(messageSubject);
            msg.setText(messageText);            
            
            Transport.send(msg);
            System.out.println("SendEMail to " + ToEmailAddr + ", from " + FromEmailAddr);
        }
        catch (AddressException e)
        {
            e.printStackTrace();
        }
        catch (MessagingException e)
        {
            e.printStackTrace();
        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }
    }
    /*
    public static void SendEmailWithIMAGEAttachment( String FromEmailAddr,
                                                     String ToEmailAddr,
                                                     String recipientName,
                                                     String messageSubject,
                                                     String htmlBody,
                                                     BlobKey blobkey ) {
        
        Properties prop = new Properties();
        Session session = Session.getDefaultInstance(prop,null);
        try
        {    
            Message msg = new MimeMessage(session);
            
            msg.setFrom(new InternetAddress(FromEmailAddr));
            msg.addRecipient(Message.RecipientType.TO, new InternetAddress(ToEmailAddr, recipientName));
            msg.setSubject(messageSubject);
            
            
            
            //msg.setText(messageText);            
         
            
            //String htmlBody = "<body> test </body>";

            Multipart mp = new MimeMultipart();

            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlBody, "text/html");
            mp.addBodyPart(htmlPart);

            ////////////////////////////////
            // ATTACH IMAGE FROM BLOBSTORE
            
            BlobInfo blobInfo = blobInfoFactory.loadBlobInfo(blobkey);
            long filesize = blobInfo.getSize();
            byte[] blobData = blobstoreService.fetchData(blobkey, 0, filesize);
            
            MimeBodyPart attachment = new MimeBodyPart();
            attachment.setFileName("image.jpeg");
            attachment.setContent(blobData, "image/jpeg");
            mp.addBodyPart(attachment);
            ////////////////////////////////
            
            msg.setContent(mp);

            
            
            Transport.send(msg);
            System.out.println("SendEMail to " + ToEmailAddr + ", from " + FromEmailAddr);
        }
        catch (AddressException e)
        {
            e.printStackTrace();
        }
        catch (MessagingException e)
        {
            e.printStackTrace();
        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }        
    }
    */
    
    public static void SendEmailWithIMAGEAttachment(EMailRequest req) {

        Properties prop = new Properties();
        Session session = Session.getDefaultInstance(prop, null);
        try {
            Message msg = new MimeMessage(session);

            msg.setFrom(new InternetAddress(req.GetFromEmailAddr()));
            msg.addRecipient( Message.RecipientType.TO, new InternetAddress( req.GetToEmailAddr(), req.GetrecipientName() ) );
            msg.setSubject(req.GetmessageSubject());

            Multipart mp = new MimeMultipart();
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(req.GethtmlBody(), "text/html");
            mp.addBodyPart(htmlPart);

            // //////////////////////////////
            // ATTACH IMAGE FROM BLOBSTORE

            //System.out.println("SendEmailWithIMAGEAttachment: req.Getblobkey=" + req.Getblobkey().toString());
            //System.out.println("SendEmailWithIMAGEAttachment: blobstoreService = " + blobstoreService);
            //System.out.println("SendEmailWithIMAGEAttachment: datastoreService = " + datastoreService);
            //System.out.println("SendEmailWithIMAGEAttachment: blobInfoFactory = " + blobInfoFactory);
            
            BlobInfo blobInfo = blobInfoFactory.loadBlobInfo(req.Getblobkey());
            long filesize = blobInfo.getSize();
            //long filesize = BlobstoreService.MAX_BLOB_FETCH_SIZE - 1;
            
            System.out.println("SendEmailWithIMAGEAttachment: filesize  = " + filesize);
            
            byte[] blobData = blobstoreService.fetchData(req.Getblobkey(), 0, filesize);

            MimeBodyPart attachment = new MimeBodyPart();
            
            attachment.setFileName("image.jpg");

            // THIS CODE, BASED ON GOOGLE EXAMPLE, DOESN'T WORK... it causes exception:
            //   javax.mail.SendFailedException: Send failure (javax.mail.MessagingException: Converting attachment data failed)
            //      at javax.mail.Transport.send(Transport.java:163)
            //          at javax.mail.Transport.send(Transport.java:48)
            //          at blobstorage.SendMail.SendEmailWithIMAGEAttachment(SendMail.java:179)
            //          at blobstorage.EMailRequestManagerSingleton.run(EMailRequestManagerSingleton.java:59)
            //          at com.google.appengine.tools.development.BackgroundThreadFactory$1$1.run(BackgroundThreadFactory.java:60)
            //      Caused by: javax.mail.MessagingException: Converting attachment data failed
            //          at com.google.appengine.api.mail.stdimpl.GMTransport.sendMessage(GMTransport.java:215)
            //          at javax.mail.Transport.send(Transport.java:95)            
            //attachment.setContent(blobData, "image/jpeg");
            
            // This is based on example here: https://groups.google.com/forum/#!msg/google-appengine-java/5LwT4JG6LiY/bXwf2d_lUKYJ
            //   (at least it doesn't except)
            DataSource src = new ByteArrayDataSource(blobData, "image/jpeg"); 
            attachment.setDataHandler(new DataHandler(src)); 
            
            mp.addBodyPart(attachment);
            // //////////////////////////////

            msg.setContent(mp);

            Transport.send(msg);
            System.out.println("SendEMail to " + req.GetToEmailAddr() + ", from " + req.GetFromEmailAddr());
        } catch (AddressException e) {
            e.printStackTrace();
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
    
}