package blobstorage;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.google.appengine.api.ThreadManager;

public class EMailRequestManagerSingleton
//implements Runnable 
{
    private static final Logger log = Logger.getLogger(Serve.class.getName());

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
          
          //THIS ONLY WORKS FOR BACKEND SERVICE: 
          //Thread thread=ThreadManager.createBackgroundThread(instance);
          //thread.start();
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
        log.severe("EMailRequestManagerSingleton.AddRequest: " + req.toString());
        return _requests.offer(req);
    }

    public void processQueue() {
        EMailRequest req = null;
        
        log.severe("EMailRequestManagerSingleton.processQueue:Entry:q-count=" + _requests.size());
        
        try {
            req = _requests.poll(10, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            //e.printStackTrace();
        }
        
        if( req != null ) {
            log.severe("EMailRequestMgr:processQueue: trying to send email!");
            SendMail.SendEmailWithIMAGEAttachment(req);
        }
    }
    
    /*
    public void run() {
    
        long counter = 0;
        
        while( !ShutdownRequested() ) {
            EMailRequest req = null;
        
            if(counter++ < 10) {
                log.severe("EMailRequestMgr: polling for req... q-size=" + _requests.size());
                //System.out.println("EMailRequestMgr: polling for req... q-size=" + _requests.size());
            }
            
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
    */
}
