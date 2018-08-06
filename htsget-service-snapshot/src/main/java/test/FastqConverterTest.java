package test;

import eu.elixir.ega.ebi.dataedge.service.ena.htsget.service.internal.FastqConverter;
import htsjdk.samtools.CRAMFileReader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.cram.ref.CRAMReferenceSource;
import net.sf.cram.ref.ReferenceSource;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * For running this tests you will need test bam and cram files. This files can be created from test100k.fastq.gz using
 * picard tools and cramtools.
 */
@RunWith(SpringRunner.class)
public class FastqConverterTest {


    @Test
    public void bamConverterTest() throws IOException {
        FastqConverter converter = new FastqConverter();
        ClassLoader classLoader = getClass().getClassLoader();
        File file1 = new File(classLoader.getResource("Test100k.fastq.gz").getFile());
        File expectedFile = new File(classLoader.getResource("converted_files/bam_test_file.bam").getFile());
        FileInputStream fileIn = new FileInputStream(file1);
        FileOutputStream bamStream = new FileOutputStream("test100k.bam");
        converter.convertToBam(fileIn,bamStream);
        File file2 = new File("test100k.bam");
        assertTrue(FileUtils.contentEquals(file2,expectedFile));
        file2.delete();

    }

//    @Test
//    public void cramConverterTest() throws IOException {
//        ClassLoader classLoader = getClass().getClassLoader();
//        FastqConverter converter = new FastqConverter();
//        File actualFile = new File(classLoader.getResource("converted_files/test100k.bam").getFile());
//        File expectedFile = new File("test100k_test_actual.cram");
//        FileInputStream fileIn = new FileInputStream(actualFile);
//        FileOutputStream fos = new FileOutputStream("test100k_test_actual.cram");
//        converter.convertToCram(fileIn,fos);
//        fos.flush();
//        fos.close();
//        File file2 = new File("test100k_test_actual.cram");
//        CRAMReferenceSource source1 = new ReferenceSource();
//        CRAMReferenceSource source2 = new ReferenceSource();
//        CRAMFileReader reader1 = new CRAMFileReader(expectedFile,source1);
//        CRAMFileReader reader2 = new CRAMFileReader(file2,source2);
//        boolean isEquals = true;
//        Iterator<SAMRecord> reader1Iterator = reader1.getIterator();
//        Iterator<SAMRecord> reader2Iterator = reader2.getIterator();
//
//        while (reader1Iterator.hasNext()){
//            SAMRecord record1 = reader1Iterator.next();
//            SAMRecord record2 = null;
//            if(reader2Iterator.hasNext()){
//                record2 = reader2Iterator.next();
//            }
//            else {
//                isEquals = false;
//                break;
//            }
//            assertEquals(record1.toString(),record2.toString());
//        }
//        assertTrue(isEquals);
//        file2.delete();
//        actualFile.delete();
//
//
//    }

}