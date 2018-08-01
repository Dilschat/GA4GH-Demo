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

import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.operator.KeyFingerPrintCalculator;
import org.bouncycastle.openpgp.operator.PBEDataDecryptorFactory;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.bc.BcPBEDataDecryptorFactory;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author asenf
 */
public class GPGStream extends Thread {
    private static KeyFingerPrintCalculator fingerPrintCalculater = new BcKeyFingerprintCalculator();
    private static BcPGPDigestCalculatorProvider calc = new BcPGPDigestCalculatorProvider();

    public GPGStream(InputStream in, OutputStream out, int blocksize, char[] password, boolean mod, int pw_strength) {
        InputStream in_ = null;
        {
            InputStream clear = null;
            try {
                if (mod) return;
                in_ = org.bouncycastle.openpgp.PGPUtil.getDecoderStream(in);
                PGPObjectFactory pgpF = new PGPObjectFactory(in_, fingerPrintCalculater);
                PGPEncryptedDataList enc;
                Object o = pgpF.nextObject();
                if (o instanceof PGPEncryptedDataList) {
                    enc = (PGPEncryptedDataList) o;
                } else {
                    enc = (PGPEncryptedDataList) pgpF.nextObject();
                }
                PGPPBEEncryptedData pbe = (PGPPBEEncryptedData) enc.get(0);

                // **************
                PBEDataDecryptorFactory pbedff = new BcPBEDataDecryptorFactory(password, calc);
                clear = pbe.getDataStream(pbedff);
                // **************
                PGPObjectFactory pgpFact = new PGPObjectFactory(clear, fingerPrintCalculater);
                PGPCompressedData cData = (PGPCompressedData) pgpFact.nextObject();
                pgpFact = new PGPObjectFactory(cData.getDataStream(), fingerPrintCalculater);
                PGPLiteralData ld = (PGPLiteralData) pgpFact.nextObject();
                InputStream in__ = ld.getInputStream();
            } catch (PGPException ex) {
                Logger.getLogger(GPGStream.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(GPGStream.class.getName()).log(Level.SEVERE, null, ex);
            } catch (Throwable t) {

            } finally {
                try {
                    in_.close();
                } catch (IOException ex) {
                    Logger.getLogger(GPGStream.class.getName()).log(Level.SEVERE, null, ex);
                }
                try {
                    clear.close();
                } catch (IOException ex) {
                    Logger.getLogger(GPGStream.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

    }

    public static InputStream getDecodingGPGInoutStream(FileInputStream fis, char[] pw) throws IOException, PGPException, NoSuchProviderException {
        InputStream in = org.bouncycastle.openpgp.PGPUtil.getDecoderStream(fis);

        PGPObjectFactory pgpF = new PGPObjectFactory(in, fingerPrintCalculater);
        PGPEncryptedDataList enc;

        Object o = pgpF.nextObject();
        if (o instanceof PGPEncryptedDataList) {
            enc = (PGPEncryptedDataList) o;
        } else {
            enc = (PGPEncryptedDataList) pgpF.nextObject();
        }

        PGPPBEEncryptedData pbe = (PGPPBEEncryptedData) enc.get(0);
        // **************
        PBEDataDecryptorFactory pbedff = new BcPBEDataDecryptorFactory(pw, calc);
        InputStream clear = pbe.getDataStream(pbedff);
        // **************
        PGPObjectFactory pgpFact = new PGPObjectFactory(clear, fingerPrintCalculater);
        PGPCompressedData cData = (PGPCompressedData) pgpFact.nextObject();
        pgpFact = new PGPObjectFactory(cData.getDataStream(), fingerPrintCalculater);
        PGPLiteralData ld = (PGPLiteralData) pgpFact.nextObject();

        return ld.getInputStream();
    }

    public static InputStream getDecodingGPGInoutStream(InputStream fis, char[] pw) throws IOException, PGPException, NoSuchProviderException {
        InputStream in = org.bouncycastle.openpgp.PGPUtil.getDecoderStream(fis);

        PGPObjectFactory pgpF = new PGPObjectFactory(in, fingerPrintCalculater);
        PGPEncryptedDataList enc;

        Object o = pgpF.nextObject();
        if (o instanceof PGPEncryptedDataList) {
            enc = (PGPEncryptedDataList) o;
        } else {
            enc = (PGPEncryptedDataList) pgpF.nextObject();
        }

        PGPPBEEncryptedData pbe = (PGPPBEEncryptedData) enc.get(0);
        // **************
        PBEDataDecryptorFactory pbedff = new BcPBEDataDecryptorFactory(pw, calc);
        InputStream clear = pbe.getDataStream(pbedff);
        // **************
        PGPObjectFactory pgpFact = new PGPObjectFactory(clear, fingerPrintCalculater);
        PGPCompressedData cData = (PGPCompressedData) pgpFact.nextObject();
        pgpFact = new PGPObjectFactory(cData.getDataStream(), fingerPrintCalculater);
        PGPLiteralData ld = (PGPLiteralData) pgpFact.nextObject();

        return ld.getInputStream();
    }

    public static void setupGPG() {
        Security.insertProviderAt(new org.bouncycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    public static void getEncodingGPGStream() {
        // TODO - maybe: GPG stream for encoding with public key
    }

}
