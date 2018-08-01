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
package htsjdk.samtools.seekablestream.cipher.ebi;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.operator.PublicKeyKeyEncryptionMethodGenerator;
import org.bouncycastle.openpgp.operator.bc.BcPGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPublicKeyKeyEncryptionMethodGenerator;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Date;

/**
 * @author asenf
 */
public class GPGOutputStream extends OutputStream {
    private final OutputStream out;
    private final PGPPublicKey pgKey;

    private final String dest = "/stream"; // Dummy Filename for GPG Instatiation

    private OutputStream literalOut = null, encOut = null, compressedOut = null; // PGP
    private int DEFAULT_BUFFER_SIZE = 65 * 1024;                                 // PGP
    private PGPEncryptedDataGenerator encryptedDataGenerator = null;             // PGP
    private PGPCompressedDataGenerator compressedDataGenerator = null;           // PGP
    private PGPLiteralDataGenerator literalDataGenerator = null;                 // PGP

    public GPGOutputStream(OutputStream out, PGPPublicKey gpgKey) throws IOException {
        this.out = out;
        this.pgKey = gpgKey;

        // Set up the GPG Cipher
        Security.addProvider(new BouncyCastleProvider());

        // Encrypted Data Generator -- needs unlimited Security Policy or use OpenJDK
        BcPGPDataEncryptorBuilder pgpdeb = new BcPGPDataEncryptorBuilder(PGPEncryptedData.CAST5);
        pgpdeb.setWithIntegrityPacket(true);
        pgpdeb.setSecureRandom(new SecureRandom());
        encryptedDataGenerator = new PGPEncryptedDataGenerator(pgpdeb);

        try {
            PublicKeyKeyEncryptionMethodGenerator pgppkem = new BcPublicKeyKeyEncryptionMethodGenerator(pgKey);
            encryptedDataGenerator.addMethod(pgppkem);
            encOut = encryptedDataGenerator.open(out, new byte[DEFAULT_BUFFER_SIZE]);
        } catch (PGPException ex) {
            System.out.println("PGP Error: " + ex.getLocalizedMessage());
        }

        // Compression
        compressedDataGenerator = new PGPCompressedDataGenerator(PGPCompressedDataGenerator.ZIP);
        compressedOut = new BufferedOutputStream(compressedDataGenerator.open(encOut));

        // Literal Data Generator and Output Stream
        literalDataGenerator = new PGPLiteralDataGenerator();
        String fileName = this.dest.substring(this.dest.lastIndexOf("/") + 1);
        literalOut = literalDataGenerator.open(compressedOut,
                PGPLiteralData.BINARY, fileName,
                new Date(), new byte[DEFAULT_BUFFER_SIZE]); // 1<<16
    }

    @Override
    public void write(int b) throws IOException {
        literalOut.write(b);
    }

    @Override
    public void write(byte[] arg0) throws IOException {
        literalOut.write(arg0);
    }

    @Override
    public void write(byte[] arg0, int arg1, int arg2) throws IOException {
        literalOut.write(arg0, arg1, arg2);
    }

    @Override
    public void flush() throws IOException {
        literalOut.flush();
    }

    @Override
    public void close() throws IOException {
        literalOut.flush();
        literalOut.flush();
        literalOut.close();
        literalDataGenerator.close();
        compressedOut.close();
        compressedDataGenerator.close();
        encOut.close();
        encryptedDataGenerator.close();
        out.close();
    }

}
