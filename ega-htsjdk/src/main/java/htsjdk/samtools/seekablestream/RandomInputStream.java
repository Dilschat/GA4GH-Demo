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

package htsjdk.samtools.seekablestream;

import java.io.IOException;
import java.util.Random;

/**
 * @author asenf
 * <p>
 * This stream produces random data, it has a length that is given upon instantiation.
 * This is used for testing the system - streaming random data instead of an actual file
 * allows for testing the performance of the system apart from file access and encryption
 */
public class RandomInputStream extends SeekableStream {

    private final Random random;
    private final long size;
    private long sent;

    public RandomInputStream(long size) {
        this.size = size;
        this.sent = 0;
        this.random = new Random();
    }

    @Override
    public int read() throws IOException {
        if (this.sent >= this.size) {
            return -1;
        }
        int random = this.random.nextInt(128);
        this.sent++;
        return random;
    }


    @Override
    public int read(byte b[]) throws IOException {
        if (this.sent >= this.size) {
            return -1;
        }

        int range = b.length;
        long available = this.size - this.sent;
        int using = (int) (range <= available ? range : available);

        byte[] bytes = new byte[using];
        this.random.nextBytes(bytes);
        System.arraycopy(bytes, 0, b, 0, using);
        this.sent += using;

        return using;
    }

    @Override
    public int read(byte buffer[], int offset, int length) throws IOException {
        if (buffer == null) {
            throw new NullPointerException();
        } else if (offset < 0 || length < 0 || length > buffer.length - offset) {
            throw new IndexOutOfBoundsException();
        } else if (length == 0) {
            return 0;
        }

        if (this.sent >= this.size) {
            return -1;
        }

        long available = this.size - this.sent;
        int using = (int) (length <= available ? length : available);

        byte[] bytes = new byte[using];
        this.random.nextBytes(bytes);
        System.arraycopy(bytes, 0, buffer, offset, using);
        this.sent += using;

        return using;
    }

    @Override
    public long skip(long n) {
        long skipped = 0;

        long available = this.size - this.sent;
        skipped = skipped <= available ? skipped : available;
        this.sent += skipped;

        return skipped;
    }

    @Override
    public int available() {
        long available = this.size - this.sent;
        return available > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) available;
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public long length() {
        return this.size;
    }

    @Override
    public long position() {
        return this.sent;
    }

    @Override
    public void seek(long l) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public boolean eof() {
        return (this.sent == this.size);
    }

    @Override
    public String getSource() {
        return "Random Number Generator";
    }

}
