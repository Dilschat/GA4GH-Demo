package test;

import eu.elixir.ega.ebi.dataedge.service.ena.htsget.service.internal.ENAFtpDownloader;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ENAftpDownloaderTest {
    /**
     * For running this test you need to put into root folder of project ERR1777637_1.fastq.gz
     from ftp.sra.ebi.ac.uk/vol1/fastq/SRR921/SRR921499/SRR921499.fastq.gz
     **/
    @Test
    public void downloadingTest1() throws IOException {
        String pathToExpectedFile = "/Users/dilsatsalihov/Desktop/gsoc/ega-dataedge/SRR921499.fastq.gz";
        File expectedFile = new File(pathToExpectedFile);
        ENAFtpDownloader downloader = new ENAFtpDownloader();
        InputStream receivedStream = downloader.getFastqFile("ftp.sra.ebi.ac.uk/vol1/fastq/SRR921/SRR921499/SRR921499.fastq.gz");
        File downloadedFile = writeToFile(receivedStream);
        assertTrue(FileUtils.contentEquals(expectedFile,downloadedFile));
    }

    public File writeToFile(InputStream inputStream){
        OutputStream outputStream = null;
        File file = new File("/Users/dilsatsalihov/Desktop/gsoc/ega-dataedge/2.4MB_test.zip");
        try {

            // write the inputStream to a FileOutputStream
            outputStream =
                    new FileOutputStream(file);

            int read = 0;
            byte[] bytes = new byte[1024];

            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }

            System.out.println("Done!");

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (outputStream != null) {
                try {
                    // outputStream.flush();
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
        return file;
    }



}