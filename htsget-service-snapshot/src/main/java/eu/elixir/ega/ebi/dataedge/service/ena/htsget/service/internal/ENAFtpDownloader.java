package eu.elixir.ega.ebi.dataedge.service.ena.htsget.service.internal;

import org.springframework.stereotype.Service;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Class for streaming(downloading) binary data from frp server 
 */
@Service
public class ENAFtpDownloader {
    /**
     * streams data from ftp server
     * @param url link to file on server
     * @return stream from server
     */
    public InputStream getFastqFile(String url) {
        url = String.format("ftp://%s", url);
        URL urlCon = null;
        try {
            urlCon = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        URLConnection conn = null;
        try {
            conn = urlCon.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        InputStream inputStream = null;
        try {
            inputStream = conn.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return inputStream;
    }
}
