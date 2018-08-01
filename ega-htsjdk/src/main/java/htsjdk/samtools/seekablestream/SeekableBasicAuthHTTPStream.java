/*
 * The MIT License
 *
 * Copyright (c) 2013 The Broad Institute
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package htsjdk.samtools.seekablestream;

import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;

/**
 * @author jrobinso
 */
public class SeekableBasicAuthHTTPStream extends SeekableHTTPStream {

    protected String auth;

    public SeekableBasicAuthHTTPStream(final URL url, final String auth) {
        super(url);
        this.auth = auth;
    }

    public SeekableBasicAuthHTTPStream(final URL url, Proxy proxy, final String auth) {
        super(url, proxy);
        this.auth = auth;
        // Get Auth from URL, if applicable
        if (auth == null || auth.length() == 0) {
            this.auth = url.getUserInfo();
        }
    }

    @Override
    protected void setAuthentication(HttpURLConnection connection) {
        if (auth != null && auth.length() > 0) {
            // Java bug : http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6459815
            String encoding = new sun.misc.BASE64Encoder().encode(auth.getBytes());
            encoding = encoding.replaceAll("\n", "");
            String basicAuth = "Basic " + encoding;
            connection.setRequestProperty("Authorization", basicAuth);
        }
    }

}
