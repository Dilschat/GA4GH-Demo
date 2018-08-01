/*
 * Copyright 2017 ELIXIR EGA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package htsjdk.samtools.seekablestream;

import htsjdk.samtools.seekablestream.SeekableStream;
import htsjdk.samtools.util.HttpUtils;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;

/**
 * @author asenf
 * 
 * Attach a SeekableStream directly to a RES_MVC microservice.
 * This is for archive internal usage - there is no access control level in this
 * stream. Other microservices than RES are responsible for AuthN/Z
 * 'auth' is retained in case RES_MVC is deployed with Basic Auth control
 * 
 * Assume: File Archive ID specified as part of URL upin instantiation.
 * Destination Format, Key specified upon use; not known upon instantiation.
 */
public class EgaSeekableResStream extends SeekableStream {

    private long position = 0;
    private long marked = 0;
    private int readLimit = 0;
    private long contentLength = -1;
    private final URL url;  // RES_MVC Microservice
    private final Proxy proxy;
    private final String auth; // Basic Auth (optional)
    
    private boolean hack = false;
    private String hack_extension = "";

    public EgaSeekableResStream(final URL url) {
        this(url, null, null);
    }

    public EgaSeekableResStream(final URL url, String auth) {
        this(url, null, auth);
    }

    public EgaSeekableResStream(final URL url, Proxy proxy) {
        this(url, proxy, null);
    }
    
    public EgaSeekableResStream(final URL url, Proxy proxy, String auth) {
        this(url, proxy, auth, -1);
    }
    
    public EgaSeekableResStream(final URL url, Proxy proxy, String auth, long fileSize) {

        this.proxy = proxy;
        this.url = url;
        this.auth = auth;
        this.contentLength = fileSize - 16; // This is true for AES Encrypted Streams (first 16 bytes = IV)

        // Try to get the file length
        // Note: This also sets setDefaultUseCaches(false), which is important
        final String contentLengthString = HttpUtils.getHeaderField(url, "Content-Length");
        if (contentLengthString != null && contentLength == -1) {
            try {
                contentLength = Long.parseLong(contentLengthString);
            }
            catch (NumberFormatException ignored) {
                System.err.println("WARNING: Invalid content length (" + contentLengthString + "  for: " + url);
                contentLength = -1;
            }
        }
        
    }

    public long position() {
        return position;
    }

    public long length() {
        return contentLength;
    }

    @Override
    public long skip(long n) throws IOException {
        long bytesToSkip = Math.min(n, contentLength - position);
        position += bytesToSkip;
        return bytesToSkip;
    }

    public boolean eof() throws IOException {
        return contentLength > 0 && position >= contentLength;
    }

    public void seek(final long position) throws IOException {
        if (position < this.contentLength)
            this.position = position;
        else
            throw new IOException("requesting seek past end of stream: " + position + " (max: " + this.contentLength + ")  " + url.toString());
    }
    public int read(byte[] buffer, int offset, int len) throws IOException {
        return read(buffer, offset, len, "plain", ""); // Default unencrypted Stream
    }

    public int read(byte[] buffer, int offset, int len,
            String destinationFormat, String destinationKey) throws IOException {

        if (offset < 0 || len < 0 || (offset + len) > buffer.length) {
            throw new IndexOutOfBoundsException("Offset="+offset+",len="+len+",buflen="+buffer.length);
        }
        if (len == 0 || position == contentLength) {
            if (position >= contentLength) {
                return -1;
            }
            return 0;
        }
        if (position + len > contentLength) {
            len = (int) (contentLength-position);
        }

        HttpURLConnection connection = null;
        InputStream is = null;
        String byteRange = "";
        int n = 0;
        try {
            // RES_MVC Code: Specify URL parameters for RES (replaces 'Range=...')
            String res_url = url.toString();
            String startCoordinate = String.valueOf(position);
            //long endRange = position + len - 1;
            long endRange = position + len;
            // IF we know the total content length, limit the end range to that.
            if (contentLength > 0) {
                endRange = Math.min(endRange, contentLength);
            }
            String endCoordinate = String.valueOf(endRange);
            res_url += "?startCoordinate=" + startCoordinate +
                    "&endCoordinate=" + endCoordinate + 
                    "&destinationFormat=" + destinationFormat;
            if (destinationKey!=null && destinationKey.length()>0) {
                res_url += "&destinationKey=" + destinationKey;
            }
            URL urlResMvc = new URL(res_url);
            
            connection = proxy == null ?
                    (HttpURLConnection) urlResMvc.openConnection() :
                    (HttpURLConnection) urlResMvc.openConnection(proxy);
            if (auth!=null) {
                // Java bug : http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6459815
                String encoding = new sun.misc.BASE64Encoder().encode (auth.getBytes());
                encoding = encoding.replaceAll("\n", "");  
                String basicAuth = "Basic " + encoding;
                connection.setRequestProperty ("Authorization", basicAuth);
            }

            is = connection.getInputStream();

            while (n < len && is!=null) {
                int count = is.read(buffer, offset + n, len - n);
                if (count < 0) {
                    if (n == 0) {
                        return -1;
                    } else {
                       break;
                    }
                }
                n += count;
            }

            position += n;

            return n;

        }

        catch (IOException e) {
            // THis is a bit of a hack, but its not clear how else to handle this.  If a byte range is specified
            // that goes past the end of the file the response code will be 416.  The MAC os translates this to
            // an IOException with the 416 code in the message.  Windows translates the error to an EOFException.
            //
            //  The BAM file iterator  uses the return value to detect end of file (specifically looks for n == 0).
            if (e.getMessage().contains("416") || (e instanceof EOFException)) {
                if (n == 0) {
                    return -1;
                } else {
                    position += n;
                    // As we are at EOF, the contentLength and position are by definition =
                    contentLength = position;
                    return n;
                }
            } else {
                throw e;
            }

        }

        finally {
            if (is != null) {
                is.close();
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }


    public void close() throws IOException {
        // Nothing to do
    }


    public int read() throws IOException {
    	byte []tmp=new byte[1];
    	read(tmp,0,1);
    	return (int) tmp[0] & 0xFF; 
    }

    @Override
    public String getSource() {
        return hack?null:url.toString()+hack_extension;
    }

    // A hack to return null as source, which will then default to BAM format in HTSJDK
    public EgaSeekableResStream setSourceNull(boolean hack) {
        this.hack = hack;
        return this;
    }
    
    // A hack to fool HTSJDK to recognize the file format based on the extension
    public EgaSeekableResStream setExtension(String hack_extension) {
        this.hack_extension = hack_extension;
        return this;
    }
    
    @Override
    public void reset() {
        if (position-marked > readLimit) {
            this.position = 0;
        } else {
            this.position = this.marked;
        }
    }
    
    public void mark(int readLimit) {
        this.marked = this.position;
        this.readLimit = readLimit;
    }
    
    public boolean markSupported() {
        return true;
    }
}