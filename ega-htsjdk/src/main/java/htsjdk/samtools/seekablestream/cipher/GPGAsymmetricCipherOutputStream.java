package htsjdk.samtools.seekablestream.cipher;

import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.operator.bc.BcPGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPublicKeyKeyEncryptionMethodGenerator;

import java.io.IOException;
import java.io.OutputStream;
import java.security.SecureRandom;

public class GPGAsymmetricCipherOutputStream extends GPGCipherOutputStream<PGPPublicKey> {

    public GPGAsymmetricCipherOutputStream(OutputStream outputStream, PGPPublicKey key) throws IOException, PGPException {
        super(outputStream, key);
    }

    public GPGAsymmetricCipherOutputStream(OutputStream outputStream, PGPPublicKey key, String filename) throws IOException, PGPException {
        super(outputStream, key, filename);
    }

    @Override
    protected PGPEncryptedDataGenerator getPGPEncryptedDataGenerator(PGPPublicKey publicKey) {
        BcPGPDataEncryptorBuilder dataEncryptorBuilder = new BcPGPDataEncryptorBuilder(publicKey.getAlgorithm());
        dataEncryptorBuilder.setWithIntegrityPacket(true);
        dataEncryptorBuilder.setSecureRandom(new SecureRandom());
        PGPEncryptedDataGenerator encryptedDataGenerator = new PGPEncryptedDataGenerator(dataEncryptorBuilder);
        encryptedDataGenerator.addMethod(new BcPublicKeyKeyEncryptionMethodGenerator(publicKey));
        return encryptedDataGenerator;
    }

}
