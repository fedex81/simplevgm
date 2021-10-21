package uk.co.omgdrv.simplevgm.test;

import uk.co.omgdrv.simplevgm.psg.BaseVgmPsgProvider;
import uk.co.omgdrv.simplevgm.psg.PsgCompare;
import uk.co.omgdrv.simplevgm.psg.nuked.NukedPsgProvider;
import uk.co.omgdrv.simplevgm.util.DspUtil;
import uk.co.omgdrv.simplevgm.util.Util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * ${FILE}
 * <p>
 * Federico Berti
 * <p>
 * Copyright 2019
 */
public class FilterTest {

    static double[] filterBuffer = new double[0];
    static double[] filterBuffer2 = new double[0];
    static float[] outFloat = new float[BaseVgmPsgProvider.VGM_SAMPLE_RATE_HZ];
    static double[] outBuffer = new double[BaseVgmPsgProvider.VGM_SAMPLE_RATE_HZ];
    static int[] outInt = new int[BaseVgmPsgProvider.VGM_SAMPLE_RATE_HZ];
    static double RATIO = 0;

    static String rawDataNameDouble = "data.raw";

    public static void main(String[] args) throws IOException {
        double[] rawBuffer = getFileContents();
        RATIO = 1.0 * outBuffer.length / rawBuffer.length;
        filterBuffer = new double[rawBuffer.length];
        filterBuffer2 = new double[rawBuffer.length];
    }

    protected static double[] getFileContents() throws IOException {
        Path file = Paths.get(".", rawDataNameDouble);
        return Files.readAllLines(file).stream().mapToDouble(Double::valueOf).toArray();
    }

    public void testFastResampleNukeOutput(double[] fileContents) {
        String fileName = "data.fast.raw";
        Path file = Paths.get(".", fileName);
        int index = 0;
        double[] rawBuffer;
        do {
            rawBuffer = Arrays.copyOfRange(fileContents, index, index + NukedPsgProvider.NUKED_PSG_SAMPLING_HZ);
            DspUtil.fastHpfResample(rawBuffer, outBuffer);
            byte[] out = new byte[outBuffer.length];
            DspUtil.scale8bit(outBuffer, out);
            Util.writeToFile(file, out);
            index += NukedPsgProvider.NUKED_PSG_SAMPLING_HZ;
        } while (index < fileContents.length);

        Util.convertToWav(fileName, PsgCompare.audioFormat8bit);
    }
}
