/*
 * Copyright 2016 ELIXIR EGA
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
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author asenf
 */
public class BufferedHTTPStream extends SeekableStream {

    private long position = 0;
    private long contentLength = -1;
    private final URL url;
    private final Proxy proxy;
    private final String auth;

    protected OkHttpClient client;

    private LoadingCache<Integer, byte[]> cache;

    private static final int PAGE_SIZE = 1024 * 1024 * 10;
    private static final int NUM_PAGES = 1;

    private String basicAuth = "";

    public BufferedHTTPStream(final URL url) {
        this(url, null, null);
    }

    public BufferedHTTPStream(final URL url, String auth) {
        this(url, null, auth);
    }

    public BufferedHTTPStream(final URL url, Proxy proxy) {
        this(url, proxy, null);
    }

    public BufferedHTTPStream(final URL url, Proxy proxy, String auth) {
        this(url, proxy, auth, -1);
    }

    public BufferedHTTPStream(final URL url, Proxy proxy, String auth, long fileSize) {

        this.proxy = proxy;
        this.url = url;
        this.auth = auth;
        this.contentLength = fileSize;

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        this.client = builder.build();

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

        if (auth != null) {
            // Java bug : http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6459815
            String encoding = new sun.misc.BASE64Encoder().encode(auth.getBytes());
            basicAuth = encoding.replaceAll("\n", "");
        }

        // Init cache
        this.cache = CacheBuilder.newBuilder()
                .maximumSize(NUM_PAGES)
                .concurrencyLevel(NUM_PAGES)
                .build(
                        new CacheLoader<Integer, byte[]>() {
                            public byte[] load(Integer page) throws Exception {
                                return populateCache(page);
                            }
                        });
        System.out.println("Page Size: " + PAGE_SIZE + " Number: " + NUM_PAGES);
    }

    public long position() {
        return position;
    }

    public long length() {
        return contentLength;
    }

    @Override
    public long skip(long n) {
        long bytesToSkip = Math.min(n, contentLength - position);
        position += bytesToSkip;
        return bytesToSkip;
    }

    public boolean eof() {
        return contentLength > 0 && position >= contentLength;
    }

    public void seek(final long position) {
        this.position = position;
    }

    // Read Bytes from API
    public int read(byte[] buffer, int offset, int len) {
        // Get the size of the file
        long fsize = this.contentLength;
        long abs_offset = position + offset; // Read from: offset from current position
        int bytesToRead = (int) Math.min(fsize - abs_offset, len);

        int cachePage = (int) (abs_offset / PAGE_SIZE); // 0,1,2,...
        //System.out.println(" read (): " + abs_offset + " + " + cachePage);

        try {
            long time = System.currentTimeMillis();
            byte[] page = this.get(cachePage);
            time = System.currentTimeMillis() - time;
            System.out.println("Read: start=" + abs_offset + " Get Page: " + time + " -- " + page.length);

            int page_offset = (int) (abs_offset - cachePage * PAGE_SIZE); // delta page start to 'Read from'
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
        position += bytesToRead;

        return bytesToRead;
    }

    public void close() throws IOException {
        // Nothing to do
    }

    public int read() throws IOException {
        byte[] tmp = new byte[1];
        read(tmp, 0, 1);
        return (int) tmp[0] & 0xFF;
    }

    @Override
    public String getSource() {
        return url.toString();
    }

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------

    // separate from stream reading/position
    private byte[] get(int page_number) throws ExecutionException {
        long size = this.contentLength;
        int maxPage = (int) (size / PAGE_SIZE + 1);

        int firstPage = page_number > 0 ? page_number - 1 : 0;
        int lastPage = (page_number + NUM_PAGES - 1) > maxPage ? maxPage : (page_number + NUM_PAGES - 1);
        System.out.println("Getting Page " + page_number + "  (first: " + firstPage + " last: " + lastPage + ")");
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
        System.out.println("populateCache(): page_number: " + page_number);
        int bytesToRead = PAGE_SIZE;
        long offset = page_number * PAGE_SIZE;
        // Prepare buffer to read from file
        byte[] bytesRead = new byte[bytesToRead];

        synchronized (this) {

            try {

                String byteRange = "bytes=" + offset + "-" + (offset + bytesToRead);

                String url = this.url.toString() + "?" + byteRange;
                System.out.println(url);
                Request datasetRequest = new Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Basic " + basicAuth)
                        .build();

                // Execute the request and retrieve the response.
                Response response = client.newCall(datasetRequest).execute();

                InputStream byteStream = response.body().byteStream();
                int bytesRead_;
                byte[] buff = new byte[8000];
                ByteArrayOutputStream bao = new ByteArrayOutputStream();

                while ((bytesRead_ = byteStream.read(buff)) != -1) {
                    bao.write(buff, 0, bytesRead_);
                }

                byte[] result = bao.toByteArray();
                bytesRead = Arrays.copyOf(result, bytesToRead);
            } catch (IOException ex) {
                Logger.getLogger(BufferedHTTPStream.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return bytesRead;
    }

}