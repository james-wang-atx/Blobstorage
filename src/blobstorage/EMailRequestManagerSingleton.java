package blobstorage;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.google.appengine.api.ThreadManager;

public class EMailRequestManagerSingleton implements Runnable {

    private LinkedBlockingQueue<EMailRequest> _requests;
    private boolean shutdown = false;
    
    private static EMailRequestManagerSingleton instance = null;
    
    // defeat public instantiation
    protected EMailRequestManagerSingleton() {
        _requests = new LinkedBlockingQueue<EMailRequest>();
    }
    
    public static EMailRequestManagerSingleton GetInstance() {
       if(instance == null) {
          instance = new EMailRequestManagerSingleton();
          
          //MUST USE Google's ThreadMangaer vs. "Thread thread = new Thread(instance);"
          Thread thread=ThreadManager.createBackgroundThread(instance);
          thread.start();
       }
       return instance;
    }
    
    public synchronized void Shutdown() {
        shutdown = true;
        System.out.println("EMailRequestManagerSingleton.Shutdown!!!!!!!!!");
    }
    
    private synchronized boolean ShutdownRequested() {
        return shutdown;
    }
    
    public boolean AddRequest(EMailRequest req) {
        System.out.println("EMailRequestManagerSingleton.AddRequest: " + req.toString());
        return _requests.offer(req);
    }
    
    public void run() {
    
        while( !ShutdownRequested() ) {
            EMailRequest req = null;
            
            System.out.println("EMailRequestMgr: polling for req... q-size=" + _requests.size());
            
            try {
                req = _requests.poll(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                //e.printStackTrace();
            }
            
            if( req != null ) {
                System.out.println("EMailRequestMgr: trying to send email!");
                SendMail.SendEmailWithIMAGEAttachment(req);
            }
        }
    }

}
