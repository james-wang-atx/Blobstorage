package blobstorage;

import com.google.appengine.api.blobstore.BlobKey;

public class EMailRequest {
    private String _FromEmailAddr;
    private String _ToEmailAddr;
    private String _recipientName;
    private String _messageSubject;
    private String _htmlBody;
    private BlobKey _blobkey;
    
    public EMailRequest(String from, String to, String recname, String sub, String htmlBody, BlobKey bk) {
        this.SetFromEmailAddr(from);
        this.SetToEmailAddr(to);
        this.SetrecipientName(recname);
        this.SetmessageSubject(sub);
        this.SethtmlBody(htmlBody);
        this.Setblobkey(bk);
    }

    public String GetFromEmailAddr() {
        return _FromEmailAddr;
    }

    private void SetFromEmailAddr(String _FromEmailAddr) {
        this._FromEmailAddr = _FromEmailAddr;
    }

    public String GetToEmailAddr() {
        return _ToEmailAddr;
    }

    private void SetToEmailAddr(String _ToEmailAddr) {
        this._ToEmailAddr = _ToEmailAddr;
    }

    public String GetrecipientName() {
        return _recipientName;
    }

    private void SetrecipientName(String _recipientName) {
        this._recipientName = _recipientName;
    }

    public String GetmessageSubject() {
        return _messageSubject;
    }

    private void SetmessageSubject(String _messageSubject) {
        this._messageSubject = _messageSubject;
    }

    public String GethtmlBody() {
        return _htmlBody;
    }

    private void SethtmlBody(String _htmlBody) {
        this._htmlBody = _htmlBody;
    }

    public BlobKey Getblobkey() {
        return _blobkey;
    }

    private void Setblobkey(BlobKey _blobkey) {
        System.out.println("Setblobkey: " + _blobkey.toString());
        this._blobkey = _blobkey;
    }
}
