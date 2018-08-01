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
package htsjdk.samtools.seekablestream.ebi;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import htsjdk.samtools.seekablestream.SeekableStream;
import htsjdk.samtools.util.HttpUtils;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Response;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.util.concurrent.ExecutionException;

/**
 * @author asenf
 */
public class AsyncBufferedSeekableHTTPStream extends SeekableStream {

    // HTTP Client to access CleverSafe
    AsyncHttpClient asyncHttpClient = new DefaultAsyncHttpClient();

    // Stream-related variables
    private long position = 0;
    private long contentLength = -1;
    private final URL url;
    private final Proxy proxy;
    private final String auth;

    private LoadingCache<Integer, byte[]> cache;

    private static final int PAGE_SIZE = 1024 * 512;
    private static final int NUM_PAGES = 20;

    private String basicAuth = "";

    public AsyncBufferedSeekableHTTPStream(final URL url) {
        this(url, null, null);
    }

    public AsyncBufferedSeekableHTTPStream(final URL url, String auth) {
        this(url, null, auth);
    }

    public AsyncBufferedSeekableHTTPStream(final URL url, Proxy proxy) {
        this(url, proxy, null);
    }

    public AsyncBufferedSeekableHTTPStream(final URL url, Proxy proxy, String auth) {
        this(url, proxy, auth, -1);
    }

    public AsyncBufferedSeekableHTTPStream(final URL url, Proxy proxy, String auth, long fileSize) {
        this.asyncHttpClient = new DefaultAsyncHttpClient();

        this.proxy = proxy;
        this.url = url;
        this.auth = auth;
        this.contentLength = fileSize;

        if (auth != null) {
            // Java bug : http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6459815
            String encoding = new sun.misc.BASE64Encoder().encode(auth.getBytes());
            encoding = encoding.replaceAll("\n", "");
            basicAuth = "Basic " + encoding;
        } else if (url.getUserInfo() != null) {
            String encoding = new sun.misc.BASE64Encoder().encode(url.getUserInfo().getBytes());
            encoding = encoding.replaceAll("\n", "");
            basicAuth = "Basic " + encoding;
        }

        // Init cache
        this.cache = CacheBuilder.newBuilder()
                .maximumSize(NUM_PAGES)
                .concurrencyLevel(NUM_PAGES)
                .build(
                        new CacheLoader<Integer, byte[]>() {
                            public byte[] load(Integer page) {
                                return populateCache(page);
                            }
                        });

        // Try to get the file length
        // Note: This also sets setDefaultUseCaches(false), which is important
        final String contentLengthString = HttpUtils.getHeaderField(url, "Content-Length");
        if (contentLengthString != null && contentLength == -1) {
            try {
                contentLength = Long.parseLong(contentLengthString);
            } catch (NumberFormatException ignored) {
                System.err.println("WARNING: Invalid content length (" + contentLengthString + "  for: " + url);
                contentLength = -1;
            }
        }
    }

    @Override
    public long length() {
        return contentLength;
    }

    @Override
    public long position() {
        return position;
    }

    @Override
    public void seek(long l) {
        this.position = position;
    }

    @Override
    public int read(byte[] buffer, int offset, int len) throws IOException {
        // Get the size of the file; offset is for buffer, not for stream
        long fsize = this.contentLength;
        int bytesToRead = (int) Math.min(fsize - position, len);

        int cachePage = (int) (position / PAGE_SIZE); // 0,1,2,...

        try {
            byte[] page = this.get(cachePage);

            int page_offset = (int) (position - cachePage * PAGE_SIZE); // delta page start to 'Read from'
            int bytesToCopy = Math.min(bytesToRead, page.length - page_offset); // don't read past end of page
            int offset_ = offset;
            System.arraycopy(page, page_offset, buffer, offset_, bytesToCopy);
            offset_ += bytesToCopy;
            this.position += bytesToCopy;

            int bytesRemaining = bytesToRead - bytesToCopy;
            while (bytesRemaining > 0) {
                page = this.get(++cachePage); // this.cache.get(cachePage+1);
                bytesToCopy = Math.min(bytesRemaining, page.length); // don't read past end of page
                System.arraycopy(page, 0, buffer, offset_, bytesToCopy);
                bytesRemaining -= bytesToCopy;
                offset_ += bytesToCopy;
                this.position += bytesToCopy;
            }
        } catch (ExecutionException e) {
            System.out.println(e);
            return 0;
        }

        return bytesToRead;
    }

    @Override
    public void close() {
        // Clear cache; nothing else to do
        this.cache = null;
    }

    @Override
    public boolean eof() {
        return contentLength > 0 && position >= contentLength;
    }

    @Override
    public String getSource() {
        return url.toString();
    }

    @Override
    public int read() throws IOException {
        byte[] tmp = new byte[1];
        read(tmp, 0, 1);
        return (int) tmp[0] & 0xFF;
    }

    // ------------------------------------------------------------------------- Cache Population
    // ------------------------------------------------------------------------- Guava Cache

    // separate from stream reading/position
    private byte[] get(int page_number) throws ExecutionException {
        int maxPage = (int) (this.contentLength / PAGE_SIZE + 1); // Don'd read past end of stream

        int firstPage = page_number > 0 ? page_number - 1 : 0; // Get prior cache page, just in case
        int lastPage = (page_number + NUM_PAGES - 1) > maxPage ? maxPage : (page_number + NUM_PAGES - 1);
        for (int i = firstPage; i < lastPage; i++) {
            final int page_i = i;
            new Thread(() -> {
                try {
                    this.cache.get(page_i);
                } catch (ExecutionException ignored) {
                }
            }).start();
        }

        return this.cache.get(page_number);
    }

    private byte[] populateCache(int page_number) {
        // Last Page Handling
        int maxPage = (int) (this.contentLength / PAGE_SIZE + 1); // Don'd read past end of stream
        if (page_number > maxPage)
            return new byte[]{};

        long offset = page_number * PAGE_SIZE;
        final int bytesToRead = (int) ((page_number == maxPage) ? (this.contentLength - offset) : PAGE_SIZE);

        // Prepare buffer to read from file
        final byte[] bytesRead = new byte[bytesToRead];

        synchronized (this) {
            try {
                String byteRange = "bytes=" + offset + "-" + (offset + bytesToRead);
                asyncHttpClient.prepareGet(this.url.toString() + "?" + byteRange)
                        .addHeader("Authorization", basicAuth)
                        .addQueryParam("bytes", offset + "-" + (offset + bytesToRead))
                        .execute(new AsyncCompletionHandler<Response>() {
                            @Override
                            public Response onCompleted(Response response) throws Exception {
                                // Do something with the Response
                                // Copy from response into buffer
                                byte[] responseBodyAsBytes = response.getResponseBodyAsBytes();
                                System.arraycopy(responseBodyAsBytes, 0, bytesRead, 0, bytesToRead);
                                asyncHttpClient.close();
                                return response;
                            }

                            @Override
                            public void onThrowable(Throwable t) {
                                // Something wrong happened.
                            }
                        });

            } catch (Throwable ignored) {
            }
        }
        return bytesRead;
    }

}
