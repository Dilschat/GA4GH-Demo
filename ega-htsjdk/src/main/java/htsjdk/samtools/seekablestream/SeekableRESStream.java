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

import org.springframework.web.util.UriComponentsBuilder;

import java.net.*;

/**
 * @author asenf
 * <p>
 * Attach a SeekableStream directly to a RES_MVC microservice.
 * This is for archive internal usage - there is no access control level in this
 * stream. Other microservices than RES are responsible for AuthN/Z
 * 'auth' is retained in case RES_MVC is deployed with Basic Auth control
 * <p>
 * Assume: File Archive ID specified as part of URL upin instantiation.
 * Destination Format, Key specified upon use; not known upon instantiation.
 */
public class SeekableRESStream extends SeekableBasicAuthHTTPStream {

    private boolean hack = false;
    private String hackExtension = "";

    public SeekableRESStream(final URL url, final String auth, final long fileSize) throws MalformedURLException {
        super(new URL(UriComponentsBuilder.fromHttpUrl(url.toString()).
                replaceQueryParam("destinationFormat", "").
                replaceQueryParam("extension", "").
                build().
                toUriString().replace("destinationFormat=", "").replace("extension=", "")), auth);
        this.contentLength = fileSize;
    }

    public SeekableRESStream(final URL url, final Proxy proxy, final String auth, final long fileSize) throws MalformedURLException {
        super(new URL(UriComponentsBuilder.fromHttpUrl(url.toString()).
                replaceQueryParam("destinationFormat", "").
                replaceQueryParam("extension", "").
                build().
                toUriString().replace("destinationFormat=", "").replace("extension=", "")), proxy, auth);
        this.contentLength = fileSize;
    }

    @Override
    protected void setRange(int len, HttpURLConnection connection) {
        // we don't set range header for RES - we use request parameters (coordinates)
    }

    @Override
    protected URL getActualURLToRead(byte[] buffer, int offset, int len) throws MalformedURLException {
        // RES_MVC Code: Specify URL parameters for RES (replaces 'Range=...')
        String startCoordinate = String.valueOf(position);
        //long endRange = position + len - 1;
        long endRange = position + len;
        // IF we know the total content length, limit the end range to that.
        if (contentLength > 0) {
            endRange = Math.min(endRange, contentLength);
        }
        String endCoordinate = String.valueOf(endRange);
        URI uri = UriComponentsBuilder.fromHttpUrl(url.toString()).
                replaceQueryParam("startCoordinate", startCoordinate).
                replaceQueryParam("endCoordinate", endCoordinate).
                build().
                toUri();
        return uri.toURL();
    }

    @Override
    public String getSource() {
        return hack ? null : url.toString() + hackExtension;
    }

    // A hack to return null as source, which will then default to BAM format in HTSJDK
    public SeekableRESStream setSourceNull(boolean hack) {
        this.hack = hack;
        return this;
    }

    // A hack to fool HTSJDK to recognize the file format based on the extension
    public SeekableRESStream setExtension(String hackExtension) {
        this.hackExtension = hackExtension;
        return this;
    }

}