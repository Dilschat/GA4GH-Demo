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

/*
 * Command-line tool to encrypt/decrypt files at the command line.
 * Allows for multiple files to be processed at the same time.
 * Uses the cipher_stream class; a debug mode is present that allows for
 * random-access to files from the command line (not documented)
 */

package htsjdk.samtools.seekablestream.cipher.ebi;

import com.google.common.io.ByteStreams;
import htsjdk.samtools.*;
import htsjdk.samtools.SamReaderFactory.Option;
import htsjdk.samtools.seekablestream.SeekableFileStream;
import htsjdk.samtools.seekablestream.SeekableStream;

import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author asenf
 */
public class Cipher {
    private int blocksize; // Number of AES Blocks per Access Op
    private int numThreads; // 1 for most, many for some operations
    private int pw_strength;
    FileInputStream[] in;
    FileOutputStream[] out;
    boolean mod, gpg;

    public Cipher(int numThreads, int block, boolean mod, boolean gpg, int pw_strength, String[] in_out_pair) throws FileNotFoundException {
        this.numThreads = (numThreads > 0) ? numThreads : Runtime.getRuntime().availableProcessors();
        this.blocksize = (block > 0) ? block : 2048;
        this.mod = mod;
        this.gpg = gpg;
        this.pw_strength = pw_strength;

        if (in_out_pair.length == 1) { // Use StdIn as input source
            this.in = null;
            this.out = new FileOutputStream[1];
            this.out[0] = new FileOutputStream(in_out_pair[0]);
        } else { // Input file provided
            int numFiles = in_out_pair.length / 2;
            this.in = new FileInputStream[numFiles];
            this.out = new FileOutputStream[numFiles];
            for (int i = 0; i < numFiles; i++) {
                this.in[i] = new FileInputStream(in_out_pair[2 * i]);
                this.out[i] = new FileOutputStream(in_out_pair[2 * i + 1]);
            }
        }
    }

    // This procedure performs the actual work of interacting with the cipher classes
    // It also distributes work to free threads, based on max. thread number (meant for large files)
    public void run(char[] password) {
        if (!this.gpg) {
            CipherStream my_threads[] = new CipherStream[this.numThreads];
            int count = 0, savecount = 0, size = (this.in == null) ? -1 : this.in.length;

            if (this.in != null) { // Input file(s) provided to read from
                boolean alive;
                do {
                    ArrayList indices = new ArrayList(); // idices of "free" threads
                    alive = false;
                    for (int i = 0; i < numThreads; i++) { // find threads that have ended
                        if ((my_threads[i] != null) && (my_threads[i].isAlive())) {
                            alive = true;
                        } else {
                            indices.add(i);
                            if (my_threads[i] != null) {
                                my_threads[i] = null;
                                savecount++; // count completed threads
                            }
                        }
                    }

                    // Previous loop determined free threads; fill them in the next loop
                    if (indices.size() > 0 && count < size) { // If there are open threads, then
                        for (Object indice : indices) { // Fill all open spaces
                            if (count < size) { // Catch errors
                                int index = Integer.parseInt(indice.toString());
                                my_threads[index] = new CipherStream(this.in[count], this.out[count], this.blocksize, password, this.mod, this.pw_strength);
                                my_threads[index].start(); // start the learning algorithm (thread)
                                count++; // count started threads
                            }
                        }
                    }

                    // runs until the number of completed threads equals the number of files, and all threads completed (redundant)
                } while ((savecount < size) || alive);
            } else { // Read from Stdin
                CipherStream cipher_stream = new CipherStream(System.in, this.out[0], this.blocksize, password, this.mod, this.pw_strength);
                cipher_stream.start();

                while (cipher_stream.isAlive()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Cipher.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        } else {
            GPGStream my_threads[] = new GPGStream[this.numThreads];
            int count = 0, savecount = 0, size = (this.in == null) ? -1 : this.in.length;

            boolean alive;
            do {
                ArrayList indices = new ArrayList(); // idices of "free" threads
                alive = false;
                for (int i = 0; i < numThreads; i++) { // find threads that have ended
                    if ((my_threads[i] != null) && (my_threads[i].isAlive())) {
                        alive = true;
                    } else {
                        indices.add(i);
                        if (my_threads[i] != null) {
                            my_threads[i] = null;
                            savecount++; // count completed threads
                        }
                    }
                }

                // Previous loop determined free threads; fill them in the next loop
                if (indices.size() > 0 && count < size) { // If there are open threads, then
                    for (int i = 0; i < indices.size(); i++) { // Fill all open spaces
                        if (count < size) { // Catch errors
                            int index = Integer.parseInt(indices.get(i).toString());
                            my_threads[index] = new GPGStream(this.in[count], this.out[count], this.blocksize, password, this.mod, this.pw_strength);
                            my_threads[index].start(); // start the learning algorithm (thread)
                            count++; // count started threads
                        }
                    }
                }

                // runs until the number of completed threads equals the number of files, and all threads completed (redundant)
            } while ((savecount < size) || alive);
        }
    }

    /**
     * @param args the command line arguments
     *             threads  mode  blocksize  password  in1 out1 in2 out2 in3 out3 ...
     */
    public static void main(String[] args) {
        if (args.length == 0) { // No parameters -- print usage instructions
            System.out.println("Usage:");
            System.out.println("java -jar Cipher.jar --test password file");
            System.out.println("\tThis tests the cipher AES-256-bit Random Access mode");
            System.out.println("java -jar EgaCipherUtils.jar [-e|-d|-eg|-ed] [-f password_file|password] [file_in file_out]+:");
            System.out.println("\t-e AES encrypt, -d AES decrypt, -eg GPG encrypt, -ed GPG decrypt");
            System.out.println("\tpasswordfile is a file that contains the encryption/decryption password");
            System.out.println("\tfiles: input1 output1 input2 output2 input3 ...");
            System.out.println("java -jar EgaCipherUtils.jar --index [--test|--plain] [password] [inputfile]+:");
            System.exit(1);
        }

        int threads = 0; //Integer.parseInt(args[0]);
        int block = 2048; //Integer.parseInt(args[1]);
        String pw = null, passfile;
        int offset = 0;
        boolean mod = true; // true == encrypt, false == decrypt
        boolean gpg = false;
        int pw_strength = 128; // Should be a new parameter!

        if (args[0].equalsIgnoreCase("--test")) { // Test case - to be used for any file of interest!
            test(args[1].toCharArray(), args[2]); // 1 -- password, 2 -- filename
            System.exit(98);
        } else if (args[0].equalsIgnoreCase("--xtest")) { // Test case - to be used for any file of interest!
            xtest(args[1].toCharArray(), args[2]); // 1 -- password, 2 -- filename
            System.exit(98);
        } else if (args[0].equalsIgnoreCase("--index")) {
            String[] args_ = new String[args.length - 1];
            System.arraycopy(args, 1, args_, 0, args_.length);
            index(args_);
            System.exit(0);
        }

        // not a test case. Parse parameters -- mode (actually important)
        if (args[0].equalsIgnoreCase("-e")) {
            mod = true;
            gpg = false;
        } else if (args[0].equalsIgnoreCase("-d")) {
            mod = false;
            gpg = false;
        } else if (args[0].equalsIgnoreCase("-eg")) {
            mod = false;
            gpg = true;
        } else if (args[0].equalsIgnoreCase("-dg")) {
            mod = false;
            gpg = true;
        }

        // -- password directly given, or in a file
        if (args[1].equalsIgnoreCase("-f")) {
            offset = 1;
            passfile = args[2];

            File pwf = new File(passfile);
            if (!pwf.exists()) {
                System.out.println("File " + passfile + " can't be found!");
                System.exit(99);
            }

            try {
                BufferedReader bfr = new BufferedReader(new FileReader(pwf));
                pw = bfr.readLine();
            } catch (IOException ex) {
                Logger.getLogger(Cipher.class.getName()).log(Level.SEVERE, null, ex);
                System.out.println("Error opening/reading file " + passfile);
                System.exit(99);
            }
        } else
            pw = args[1];

        // Files to be encrypted
        int numfiles = (args.length - 2 - offset);
        String[] files = new String[numfiles];
        for (int i = 0; i < numfiles; i++) {
            files[i] = args[i + 2 + offset];
        }

        Cipher t;
        try {
            t = new Cipher(threads, block, mod, gpg, pw_strength, files);
            t.run(pw.toCharArray());
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Cipher.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Error opening/reading file");
        }
    }

    private static void index(String[] args) {
        if (args.length < 2) {
            System.out.println("Required Parameters Errors");
            System.out.println("[File Password] [Input File]+");
            System.exit(1);
        }

        // Test Code - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        if (args[0].equals("--test")) {
            test(Arrays.copyOfRange(args, 1, args.length), 128);
            System.exit(2);
        }
        if (args[0].equals("--plain")) {
            plain(Arrays.copyOfRange(args, 1, args.length));
            System.exit(2);
        }

        // Process parameters --------------------------------------------------
        byte[] pw = args[0].getBytes();

        int numFiles = args.length - 1;
        File[] inputfiles = new File[numFiles];
        for (int i = 0; i < numFiles; i++) {
            inputfiles[i] = new File(args[(i + 1)]);
            if (!inputfiles[i].exists()) {
                System.out.println("File " + args[(i + 1)] + " does not exist!");
                System.exit(1);
            } else if (!inputfiles[i].getName().toLowerCase().endsWith(".bam.cip")) {
                System.out.println("File " + args[(i + 1)] + " is not an AES encrypted BAM file!");
                System.exit(1);
            }

        }

        // Process files - generate BAM Index ----------------------------------
        for (int i = 0; i < inputfiles.length; i++) {
            String md5 = generateIndex(inputfiles[i], pw, 128);
            if (md5 == null)
                System.out.println("Index creation failed for " + inputfiles[i].getName());
            else
                System.out.println("Index creation success for " + inputfiles[i].getName() + "  MD5: " + md5);
        }

        // Done! ---------------------------------------------------------------
    }

    /*
     * Test class - encrypt a file using 256-bit encryption, then random-access
     * decrypt the file byte-by-byte, compare with original file
     */
    private static void test(char[] pass, String file) {
        // 1. Encrypt whole file
        String[] files = {file, file + ".cip"};
        Cipher t;
        try {
            t = new Cipher(1, 2048, true, false, 256, files);
            t.run(pass);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Cipher.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Error opening/reading file");
        }

        // 2. Decrypt in Stages
        SeekableFileStream sft = null; // Original File
        SeekableCipherStream sct = null; // Decrypted File
        try {
            sft = new SeekableFileStream(new File(file));
            sct = new SeekableCipherStream(new SeekableFileStream(new File(file + ".cip")), pass);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Cipher.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Length
        System.out.println(file + " length: " + sft.length());
        System.out.println((file + ".cip") + " length: " + sct.length());

        // Seek
        boolean success = true;
        byte[] test = new byte[1], cipher_test = new byte[1];
        long delta = sft.length() / 1000, pos = 0;
        for (int i = 0; i < 1000; i++) {
            try {
                sft.seek(pos);
                sft.read(test);
                sct.seek(pos);
                sct.read(cipher_test);

                if (test[0] != cipher_test[0]) { // If bytes differ, error!
                    success = false;
                    break;
                }
            } catch (IOException ex) {
                Logger.getLogger(Cipher.class.getName()).log(Level.SEVERE, null, ex);
            }
            pos += delta;
        }

        // Sequential Read
        try {
            sft.seek(0);
            sct.seek(0);

            byte[] b_sft = new byte[10];
            byte[] b_sct = new byte[10];

            int b_ = 0;
            while (b_ >= 0) {
                b_ = sft.read(b_sft);
                b_ = sct.read(b_sct);

                System.out.println(sft.position());
                System.out.println(sct.position());

                if (!Arrays.equals(b_sft, b_sct)) { // If bytes differ, error!
                    success = false;
                    break;
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Cipher.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (success)
            System.out.println("1000 Random Seeks & Sequential Read Successful");
        else
            System.out.println("Seek or Read Errors!");

        // 3. Done
        try {
            sft.close();
            sct.close();
        } catch (IOException ex) {
            Logger.getLogger(Cipher.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void xtest(char[] pass, String file) {

        // Try Limiting Stream *************************************************
        SeekableFileStream sft = null; // Original File
        try {
            sft = new SeekableFileStream(new File(file));
            sft.seek(10);
        } catch (IOException ex) {
            Logger.getLogger(Cipher.class.getName()).log(Level.SEVERE, null, ex);
        }

        InputStream limit = ByteStreams.limit(sft, 15);

        byte[] buffer = new byte[2048];
        try {
            int read = limit.read(buffer);
            System.out.println(read);
            read = limit.read(buffer);
            System.out.println(read);

            // -----------------------------------------------------------------
            FileInputStream fis = new FileInputStream(file);
            FileOutputStream fos = new FileOutputStream(file + ".xtest.cip");

            // *****************************************************************
            // Encrypt file -- testing

            SecretKey secret = Glue.getInstance().getKey(pass, 256);
            byte[] random_iv = new byte[16];
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            random.nextBytes(random_iv);
            AlgorithmParameterSpec paramSpec = new IvParameterSpec(random_iv);
            fos.write(random_iv);
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/CTR/NoPadding"); // load a cipher AES / Segmented Integer Counter
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secret, paramSpec);
            OutputStream out = new CipherOutputStream(fos, cipher);

            // Perform
            long bytes = ByteStreams.copy(fis, out);
            System.out.println("Copied " + bytes);
            out.close();
            fis.close();

            // Test.............................................................
            // 2. Decrypt in Stages
            SeekableCipherStream sct = null; // Decrypted File
            sft = new SeekableFileStream(new File(file));
            sct = new SeekableCipherStream(new SeekableFileStream(new File(file + ".xtest.cip")), pass);

            // Length
            System.out.println(file + " length: " + sft.length());
            System.out.println((file + ".cip") + " length: " + sct.length());

            // Seek
            byte[] test = new byte[1], cipher_test = new byte[1];
            long pos = 0;
            for (int i = 0; i < 1000; i++) {
                sft.seek(pos);
                sft.read(test);
                sct.seek(pos);
                sct.read(cipher_test);

                if (test[0] != cipher_test[0]) { // If bytes differ, error!
                    break;
                }
            }

            // Sequential Read
            sft.seek(0);
            sct.seek(0);

            byte[] b_sft = new byte[10];
            byte[] b_sct = new byte[10];

            int b_ = 0;
            while (b_ >= 0) {
                b_ = sft.read(b_sft);
                b_ = sct.read(b_sct);

                System.out.println(sft.position());
                System.out.println(sct.position());

                if (!Arrays.equals(b_sft, b_sct)) { // If bytes differ, error!
                    break;
                }
            }


            System.out.println();
        } catch (Throwable ex) {
            Logger.getLogger(Cipher.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static String generateIndex(File inputfile, byte[] password, int pw_strength) {
        String md5 = null;

        try {
            // Open cipher stream to encrypted file - - - - - - - - - - - - - - 
            SeekableFileStream sfs_in = new SeekableFileStream(inputfile);
            SeekableCipherStream scs = new SeekableCipherStream(sfs_in, (new String(password)).toCharArray(), 512000, pw_strength);

            // Open regular BAM Indexer on top of encrypted file
            SamReader reader =
                    SamReaderFactory.make()
                            .validationStringency(ValidationStringency.LENIENT)
                            .samRecordFactory(DefaultSAMRecordFactory.getInstance())
                            .open(SamInputResource.of(scs));
            String indexpath = inputfile.getCanonicalPath();
            indexpath = indexpath.substring(0, indexpath.toLowerCase().lastIndexOf(".cip")) + ".bai";
            File output = new File(indexpath);
            BAMIndexer indexer = new BAMIndexer(output, reader.getFileHeader());

            // Do the Index - - - - - - - - - - - - - - - - - - - - - - - - - - 
            long totalRecords = 0;
            for (SAMRecord rec : reader) {
                if (++totalRecords % 1000000 == 0)
                    System.out.println(totalRecords + " reads processed ...");
                indexer.processAlignment(rec);
            }
            indexer.finish();

            reader.close();
            scs.close();

            // Get MD5 - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
            MessageDigest digest;
            digest = MessageDigest.getInstance("MD5");

            InputStream in = new BufferedInputStream(new FileInputStream(new File(indexpath)));
            byte[] buffer = new byte[65535];
            int cnt = 0;
            while ((cnt = in.read(buffer)) > 0) {
                digest.update(buffer, 0, cnt);
            }
            in.close();
            byte[] md5sum = digest.digest();
            md5 = "";
            for (int i = 0; i < md5sum.length; i++)
                md5 += Integer.toString((md5sum[i] & 0xff) + 0x100, 16).substring(1);

            FileWriter fw = new FileWriter(indexpath.concat(".md5"));
            fw.write(md5);
            fw.flush();
            fw.close();

        } catch (NoSuchAlgorithmException | IOException ex) {
            Logger.getLogger(Cipher.class.getName()).log(Level.SEVERE, null, ex);
        }

        return md5;
    }

    private static void test(String[] args, int pw_strength) {
        char[] pw = args[0].toCharArray();
        File encrypted = new File(args[1]);
        File plain = new File(args[2]);

        try {
            SeekableStream ss_plain = new SeekableFileStream(plain);
            SeekableFileStream sfs_in = new SeekableFileStream(encrypted);
            SeekableCipherStream scs = new SeekableCipherStream(sfs_in, pw, 512000, pw_strength);

            byte[] a = new byte[1024], b = new byte[1024];
            int read;
            long pos = 0;

            while ((read = ss_plain.read(a)) > 0) {
                if (scs.read(b) != read) {
                    System.out.println("file end error");
                    System.exit(1);
                }
                if (!Arrays.equals(a, b)) {
                    System.out.println("byte content error -- " + pos);

                    for (int i = 0; i < read; i++) {
                        System.out.println((pos + i) + ": " + (int) a[i] + " - " + (int) b[i]);
                    }
                    System.exit(1);
                }
                pos += read;
            }

            ss_plain.close();
            scs.close();

            System.out.println("Success!");
        } catch (IOException ex) {
            Logger.getLogger(Cipher.class.getName()).log(Level.SEVERE, null, ex);
        }


    }

    private static void plain(String[] args) {
        File plain = new File(args[0]);
        try {
            SeekableStream ss_plain = new SeekableFileStream(plain);
            SamInputResource sir = SamInputResource.of(ss_plain);

            SamReader reader = SamReaderFactory.make().enable(Option.CACHE_FILE_BASED_INDEXES).open(sir);
            //SAMFileReader reader = new SAMFileReader(ss_plain);
            File output = new File(plain.getCanonicalPath() + ".bai");
            BAMIndexer indexer = new BAMIndexer(output, reader.getFileHeader());

            //reader.enableFileSource(true);

            // Do the Index - - - - - - - - - - - - - - - - - - - - - - - - - - 
            for (SAMRecord rec : reader)
                indexer.processAlignment(rec);
            indexer.finish();

            reader.close();
        } catch (IOException ex) {
            Logger.getLogger(Cipher.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
