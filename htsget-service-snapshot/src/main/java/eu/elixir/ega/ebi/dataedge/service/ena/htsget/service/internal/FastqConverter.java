package eu.elixir.ega.ebi.dataedge.service.ena.htsget.service.internal;


import htsjdk.samtools.*;
import net.sf.cram.ref.ReferenceSource;
import htsjdk.samtools.fastq.FastqReader;
import htsjdk.samtools.util.FastqQualityFormat;
import htsjdk.samtools.util.Log;
import org.springframework.stereotype.Service;
import picard.sam.FastqToSam;

import javax.validation.constraints.NotNull;
import java.io.*;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

import static htsjdk.samtools.ValidationStringency.STRICT;

/**
 * Class for converting FASTQ file to required format (BAM or CRAM)
 */

@Service
public class FastqConverter {
    /**
     * Converts fastq to bam. This function do convertation exactly as picard command line app.
     *
     * @param readedFastqFile inputstream from fastq file
     * @param resultingStream outputstream of converted to bam file
     * @return resultingStream
     */
    public OutputStream convertToBam(@NotNull InputStream readedFastqFile, @NotNull OutputStream resultingStream) throws IOException {
        BufferedReader buf = null;
        try {
            buf = new BufferedReader(new InputStreamReader(new GZIPInputStream(readedFastqFile)));
        }catch (ZipException e){
            buf = new BufferedReader(new InputStreamReader(readedFastqFile));
        }
        FastqReader fastqReader = new FastqReader(buf);
        SAMFileWriterFactory writerFactory = new SAMFileWriterFactory();
        FastqToSam converter = new FastqToSam();
        SAMReadGroupRecord rgroup = new SAMReadGroupRecord("A");
        rgroup.setSample("fastq_file_converted_to_bam");
        SAMFileHeader header = new SAMFileHeader();
        header.addReadGroup(rgroup);
        header.setComments(new ArrayList<>());
        header.setSortOrder(SAMFileHeader.SortOrder.unsorted);
        converter.QUALITY_FORMAT = FastqQualityFormat.Standard;
        converter.USE_SEQUENTIAL_FASTQS=false;
        converter.READ_GROUP_NAME="A";
        converter.MIN_Q=0;
        converter.MAX_Q=93;
        converter.ALLOW_AND_IGNORE_EMPTY_LINES=false;
        converter.VERBOSITY=Log.LogLevel.INFO;
        converter.QUIET=false;
        converter.VALIDATION_STRINGENCY=STRICT;
        converter.COMPRESSION_LEVEL=5;
        converter.MAX_RECORDS_IN_RAM=500000;
        converter.CREATE_INDEX=false;
        converter.CREATE_MD5_FILE=false;
        converter.USE_JDK_DEFLATER=false;
        converter.USE_JDK_INFLATER=false;
        converter.SORT_ORDER = SAMFileHeader.SortOrder.unsorted;
        converter.SAMPLE_NAME="fastq_file_converted_to_bam";
        converter.GA4GH_CLIENT_SECRETS="client_secrets.json";
        header=converter.createSamFileHeader().clone();
        SAMFileWriter samFileWriter = writerFactory.makeBAMWriter(header.clone(), false, resultingStream);
        converter.makeItSo(fastqReader, null, samFileWriter);
        System.out.println(fastqReader.hasNext());
        fastqReader.close();
        samFileWriter.close();
        return resultingStream;
    }

    /**
     * converts fastq to cram
     *
     * @param inputBam        inputstream from fastq file
     * @param resultingStream outputstream of converted to cram file
     * @return resultingStream
     */
    public OutputStream convertToCram(@NotNull InputStream inputBam, @NotNull OutputStream resultingStream) {
        ReferenceSource source = new ReferenceSource();
        SamReaderFactory samReaderFactory= SamReaderFactory.makeDefault().validationStringency(ValidationStringency.DEFAULT_STRINGENCY);
        SamReader samReader = samReaderFactory.open(SamInputResource.of(inputBam));
        CRAMFileWriter writer = new CRAMFileWriter(resultingStream, source, samReader.getFileHeader(),
                samReader.getResourceDescription());
        SAMFileHeader header = samReader.getFileHeader();
        SAMRecordIterator recordsIterator = samReader.iterator();
        writer.setHeader(header.clone());
        writer.setPreserveReadNames(false);
        while (recordsIterator.hasNext()) {
            writer.addAlignment(recordsIterator.next());
        }

        recordsIterator.close();
        writer.close();

        return resultingStream;
    }
}
