package test;

import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * For running this tests you will need test bam and cram files. This files can be created from test100k.fastq.gz using
 * picard tools and cramtools.
 */
@RunWith(SpringRunner.class)
class FastqConverterTest {

    /*
    @Test
    public void bamConverterTest() throws IOException {
        FastqConverter converter = new FastqConverter();
        File file1 = new File("/Users/dilsatsalihov/Desktop/gsoc/ega-dataedge/Test100k.fastq.gz");
        File expectedFile = new File("/Users/dilsatsalihov/Desktop/gsoc/ega-dataedge/bam_test_file.bam");
        FileInputStream fileIn = new FileInputStream(file1);
        FileOutputStream bamStream = new FileOutputStream("/Users/dilsatsalihov/Desktop/gsoc/ega-dataedge/test100k.bam");
        converter.convertToBam(fileIn,bamStream);
        File file2 = new File("/Users/dilsatsalihov/Desktop/gsoc/ega-dataedge/test100k.bam");
        assertTrue(FileUtils.contentEquals(file2,expectedFile));
    }

    @Test
    public void cramConverterTest() throws IOException {
        FastqConverter converter = new FastqConverter();
        File actualFile = new File("/Users/dilsatsalihov/Desktop/gsoc/ega-dataedge/test100k.bam");
        File expectedFile = new File("/Users/dilsatsalihov/Desktop/gsoc/ega-dataedge/test100k_test.cram");
        FileInputStream fileIn = new FileInputStream(actualFile);
        FileOutputStream fos = new FileOutputStream("/Users/dilsatsalihov/Desktop/gsoc/ega-dataedge/test100k_test_actual.cram");
        converter.convertToCram(fileIn,fos);
        fos.flush();
        fos.close();
        File file2 = new File("/Users/dilsatsalihov/Desktop/gsoc/ega-dataedge/test100k_test_actual.cram");
        CRAMReferenceSource source1 = new ReferenceSource();
        CRAMReferenceSource source2 = new ReferenceSource();
        CRAMFileReader reader1 = new CRAMFileReader(expectedFile,source1);
        CRAMFileReader reader2 = new CRAMFileReader(file2,source2);
        boolean isEquals = true;
        Iterator<SAMRecord> reader1Iterator = reader1.getIterator();
        Iterator<SAMRecord> reader2Iterator = reader2.getIterator();

        while (reader1Iterator.hasNext()){
            SAMRecord record1 = reader1Iterator.next();
            SAMRecord record2 = null;
            if(reader2Iterator.hasNext()){
                record2 = reader2Iterator.next();
            }
            else {
                isEquals = false;
                break;
            }
            assertEquals(record1.toString(),record2.toString());
        }
        assertTrue(isEquals);


    }
    */
}