package uk.co.omgdrv.simplevgm.util;


import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.function.Predicate;
import java.util.zip.GZIPInputStream;

/**
 * ${FILE}
 * <p>
 * Federico Berti
 * <p>
 * Copyright 2018
 */
public class Util {

    static Predicate<String> compressedVgm = n ->  n.endsWith(".GZ") || n.endsWith(".VGZ");

    // Loads given URL and file within archive, and caches archive for future access
    public static byte[] readFile(String path) throws Exception
    {
        boolean isCompressed = isCompressedByteStream(path);
        byte[] res;
        try (
                InputStream inFile = new FileInputStream(path);
                InputStream inContent = isCompressed ? DataReader.openGZIP(inFile) : inFile
                ) {
            res = DataReader.loadData(inContent);
        }
        return res;
    }

    private static boolean isCompressedByteStream(String path) throws Exception {
        boolean isCompressed = compressedVgm.test(path.toUpperCase());
        if (!isCompressed) {
            try (InputStream inFile = new FileInputStream(path)) {
                byte[] res = new byte[2];
                inFile.read(res, 0, res.length);
                isCompressed = GZIPInputStream.GZIP_MAGIC == getUInt32LE(res);
            }
        }
        return isCompressed;
    }

    // "Resizes" array to new size and preserves elements from in
    public static byte[] resize(byte[] in, int size)
    {
        byte[] out = new byte[size];
        if (size > in.length)
            size = in.length;
        System.arraycopy(in, 0, out, 0, size);
        return out;
    }

    public static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void waitForever() {
        Object lock = new Object();
        synchronized (lock) {
            try {
                lock.wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }


    public static void waitOnObject(Object object, long ms) {
        synchronized (object) {
            try {
                object.wait(ms);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    //bit 1 -> true
    public static boolean bitSetTest(long number, int position) {
        return ((number & (1 << position)) != 0);
    }


    public static void arrayDataCopy(int[][] src, int[][] dest) {
        for (int i = 0; i < src.length; i++) {
            for (int j = 0; j < src[i].length; j++) {
                dest[i] = src[i];
            }
        }
    }

    public static int log2(int n) {
        if (n <= 0) throw new IllegalArgumentException();
        return 31 - Integer.numberOfLeadingZeros(n);
    }

    // unit / pow( 2.0, (double) x / step )
    public static int int_log(int x, int step, int unit){
        int shift = x / step;
        int fraction = (x - shift * step) * unit / step;
        return ((unit - fraction) + (fraction >> 1)) >> shift;
    }

    public static int getUInt32LE(byte[] data, int pos) {
        return getUInt32LE(data[pos], data[pos+1], data[pos+2], data[pos+3]);
    }

    public static int getUInt32LE(int... bytes) {
        int value = (bytes[0] & 0xFF) << 0;
        value = bytes.length > 1 ? value | ((bytes[1] & 0xFF) << 8) : value;
        value = bytes.length > 2 ? value | ((bytes[2] & 0xFF) << 16) : value;
        value = bytes.length > 3 ? value | ((bytes[3] & 0xFF) << 24) : value;
        return value;
    }

    public static int getUInt32LE(byte... bytes) {
        int value = (bytes[0] & 0xFF) << 0;
        value = bytes.length > 1 ? value | ((bytes[1] & 0xFF) << 8) : value;
        value = bytes.length > 2 ? value | ((bytes[2] & 0xFF) << 16) : value;
        value = bytes.length > 3 ? value | ((bytes[3] & 0xFF) << 24) : value;
        return value;
    }

    public static void setUInt32LE(int value, int[] data, int startIndex) {
        data[startIndex + 3] = (value >> 24) & 0xFF;
        data[startIndex + 2] = (value >> 16) & 0xFF;
        data[startIndex + 1] = (value >> 8) & 0xFF;
        data[startIndex] = (value) & 0xFF;
    }

    public static void setUInt32BE(int value, int[] data, int startIndex) {
        data[startIndex] = (value >> 24) & 0xFF;
        data[startIndex + 1] = (value >> 16) & 0xFF;
        data[startIndex + 2] = (value >> 8) & 0xFF;
        data[startIndex + 3] = (value) & 0xFF;
    }

    public static void setSigned16BE(short value, byte[] data, int startIndex) {
        data[startIndex] = (byte) (value >> 8);
        data[startIndex + 1] = (byte) value;
    }

    public static void setSigned16LE(short value, byte[] data, int startIndex) {
        data[startIndex + 1] = (byte) (value >> 8);
        data[startIndex] = (byte) value;
    }

    public static short getSigned16BE(byte lsb, byte msb) {
        return (short) ((msb & 0xFF) | (lsb << 8));
    }

    public static short getSigned16LE(byte lsb, byte msb) {
        return (short) ((lsb & 0xFF) | (msb << 8));
    }

    public static short getSigned16BE(byte... bytes) {
        return getSigned16BE(bytes[0], bytes[1]);
    }

    public static short getSigned16LE(byte... bytes) {
        return getSigned16LE(bytes[0], bytes[1]);
    }

    public static String toStringValue(int... data) {
        String value = "";
        for (int i = 0; i < data.length; i++) {
            value += (char) (data[i] & 0xFF);
        }
        return value;
    }

    public static final String pad4(long reg) {
        String s = Long.toHexString(reg).toUpperCase();
        while (s.length() < 4) {
            s = "0" + s;
        }
        return s;
    }


    public static void writeToFile(Path file, byte[] buffer) {
        try {
            Files.write(file, buffer, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeToFile(Path file, List<String> lines) {
        try {
            Files.write(file, lines, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void convertToWav(String fileName, AudioFormat audioFormat) {
        File input = new File(fileName);
        File output = new File(fileName + ".wav");

        try {
            FileInputStream fileInputStream = new FileInputStream(input);
            AudioInputStream audioInputStream = new AudioInputStream(fileInputStream, audioFormat
                    , input.length());
            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, output);
            audioInputStream.close();
            System.out.println(fileName + ".wav recorded");
        } catch (IOException ioe) {
            ioe.printStackTrace();
            System.out.println("Error writing WAV file");
        }
    }
}
