package htsjdk.samtools.seekablestream.cipher;

import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.operator.bc.BcPBEKeyEncryptionMethodGenerator;
import org.bouncycastle.openpgp.operator.bc.BcPGPDataEncryptorBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.security.SecureRandom;

public class GPGSymmetricCipherOutputStream extends GPGCipherOutputStream<String> {

    public GPGSymmetricCipherOutputStream(OutputStream outputStream, String key) throws IOException, PGPException {
        super(outputStream, key);
    }

    public GPGSymmetricCipherOutputStream(OutputStream outputStream, String key, String filename) throws IOException, PGPException {
        super(outputStream, key, filename);
    }

    @Override
    protected PGPEncryptedDataGenerator getPGPEncryptedDataGenerator(String passphrase) {
        BcPGPDataEncryptorBuilder dataEncryptorBuilder = new BcPGPDataEncryptorBuilder(SymmetricKeyAlgorithmTags.AES_256);
        dataEncryptorBuilder.setWithIntegrityPacket(true);
        dataEncryptorBuilder.setSecureRandom(new SecureRandom());
        PGPEncryptedDataGenerator encryptedDataGenerator = new PGPEncryptedDataGenerator(dataEncryptorBuilder);
        encryptedDataGenerator.addMethod(new BcPBEKeyEncryptionMethodGenerator(passphrase.toCharArray()));
        return encryptedDataGenerator;
    }

}
