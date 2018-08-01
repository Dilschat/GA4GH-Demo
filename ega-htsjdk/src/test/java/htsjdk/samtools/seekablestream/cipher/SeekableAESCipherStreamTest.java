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
package htsjdk.samtools.seekablestream.cipher;

import htsjdk.HtsjdkTest;
import htsjdk.samtools.seekablestream.SeekableFileStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.io.pem.PemReader;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.security.Security;
import java.util.List;
import java.util.stream.Collectors;

public class SeekableAESCipherStreamTest extends HtsjdkTest {

    @BeforeTest
    private void init() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    public void testAESDecryption() throws Exception {
        byte[] privateKeyBytes;
        try (PemReader pemReader = new PemReader(new InputStreamReader(new FileInputStream("src/test/resources/htsjdk/samtools/seekablestream/cipher/ega.sec")))) {
            privateKeyBytes = pemReader.readPemObject().getContent();
        }

        System.out.println("1. Raw file");
        File rawFile = new File("src/test/resources/htsjdk/samtools/seekablestream/cipher/lorem.raw");
        String rawContents = FileUtils.readLines(rawFile, Charset.defaultCharset()).iterator().next();
        System.out.println("\t" + rawContents + "\n");
        System.out.println("2. Encrypted file");
        File encryptedFile = new File("src/test/resources/htsjdk/samtools/seekablestream/cipher/lorem.aes.enc");
        String encryptedContents = FileUtils.readFileToString(encryptedFile, Charset.defaultCharset());
        System.out.println("\t" + encryptedContents + "\n");

        SeekableFileStream encryptedFileStream = new SeekableFileStream(encryptedFile);
        SeekableAESCipherStream seekableAESCipherStream = new SeekableAESCipherStream(encryptedFileStream, privateKeyBytes);

        System.out.println("3. Decrypt whole file");
        seekableAESCipherStream.seek(0);
//        byte[] bytes = IOUtils.readFully(seekableAESCipherStream, 530);
//        System.out.println("new String(bytes, Charset.defaultCharset()) = " + new String(bytes, Charset.defaultCharset()));
//        seekableAESCipherStream.seek(0);
        List<String> strings = IOUtils.readLines(seekableAESCipherStream, Charset.defaultCharset());
        System.out.println("\t" + strings.stream().collect(Collectors.joining("\n")) + "\n");
        Assert.assertEquals(rawContents, strings.iterator().next());

        System.out.println("4. Random-access file decryption");
        int from = 0;
        int length = 100;
        byte[] result = new byte[length];
        seekableAESCipherStream.seek(from);
        seekableAESCipherStream.read(result, 0, length);
        String decryptedString = new String(result, Charset.defaultCharset());
        System.out.println("\tFrom " + from + " to " + (from + length) + ": " + decryptedString);
        String rawString = rawContents.substring(from, from + length);
        Assert.assertEquals(rawString, decryptedString);

        from = 50;
        length = 222;
        result = new byte[length];
        seekableAESCipherStream.seek(from);
        seekableAESCipherStream.read(result, 0, length);
        decryptedString = new String(result, Charset.defaultCharset());
        System.out.println("\tFrom " + from + " to " + (from + length) + ": " + decryptedString);
        rawString = rawContents.substring(from, from + length);
        Assert.assertEquals(rawString, decryptedString);

        from = 222;
        length = 111;
        result = new byte[length];
        seekableAESCipherStream.seek(from);
        seekableAESCipherStream.read(result, 0, length);
        decryptedString = new String(result, Charset.defaultCharset());
        System.out.println("\tFrom " + from + " to " + (from + length) + ": " + decryptedString);
        rawString = rawContents.substring(from, from + length);
        Assert.assertEquals(rawString, decryptedString);

        from = 20;
        length = 100;
        result = new byte[length];
        seekableAESCipherStream.seek(from);
        seekableAESCipherStream.read(result, 0, length);
        decryptedString = new String(result, Charset.defaultCharset());
        System.out.println("\tFrom " + from + " to " + (from + length) + ": " + decryptedString);
        rawString = rawContents.substring(from, from + length);
        Assert.assertEquals(rawString, decryptedString);

        from = 0;
        length = 530;
        result = new byte[length];
        seekableAESCipherStream.seek(from);
        seekableAESCipherStream.read(result, 0, length);
        decryptedString = new String(result, Charset.defaultCharset());
        System.out.println("\tFrom " + from + " to " + (from + length) + ": " + decryptedString);
        Assert.assertTrue(decryptedString.startsWith(rawContents));

        seekableAESCipherStream.close();
    }

}
