package uk.co.omgdrv.simplevgm.test;

import uk.co.omgdrv.simplevgm.psg.BaseVgmPsgProvider;
import uk.co.omgdrv.simplevgm.psg.PsgCompare;
import uk.co.omgdrv.simplevgm.psg.nuked.NukedPsgProvider;
import uk.co.omgdrv.simplevgm.util.DspUtil;
import uk.co.omgdrv.simplevgm.util.FilterHelper;
import uk.co.omgdrv.simplevgm.util.Util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.IntStream;

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

        FilterTest ft = new FilterTest();
        ft.testSlowResampleNukeOutput(rawBuffer);
    }

    protected static double[] getFileContents() throws IOException {
        Path file = Paths.get(".", rawDataNameDouble);
        return Files.readAllLines(file).stream().mapToDouble(Double::valueOf).toArray();
    }

    public void testResampler4jOutput(double[] rawBuffer) {
        float[] in = new float[rawBuffer.length];
        int scaleFactor = 4;

        DspUtil.highPass(FilterHelper.FilterType.BUTTERWORTH, rawBuffer, filterBuffer);
        IntStream.range(0, filterBuffer.length).forEach(i -> in[i] = (float) (filterBuffer[i] / scaleFactor)); //[-1;1]
        DspUtil.printDouble("hpf", filterBuffer);

        DspUtil.resample4j(in, outFloat);

        IntStream.range(0, outFloat.length).forEach(i -> outFloat[i] = scaleFactor * outFloat[i]);
        DspUtil.printFloat("res4j", outFloat);
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

    public void testSlowResampleNukeOutput(double[] fileContents) {
        String fileName = "data.slow.raw";
        Path file = Paths.get(".", fileName);
        int index = 0;
        double[] rawBuffer;
        double[] filterBuffer = new double[NukedPsgProvider.NUKED_PSG_SAMPLING_HZ];
        do {
            rawBuffer = Arrays.copyOfRange(fileContents, index, index + NukedPsgProvider.NUKED_PSG_SAMPLING_HZ);
            DspUtil.highPass(FilterHelper.FilterType.BUTTERWORTH, rawBuffer, filterBuffer);
            DspUtil.resample(filterBuffer, outBuffer);
            byte[] out = new byte[outBuffer.length];
            DspUtil.scale8bit(outBuffer, out);
            Util.writeToFile(file, out);
            index += NukedPsgProvider.NUKED_PSG_SAMPLING_HZ;
        } while (index < fileContents.length);

        Util.convertToWav(fileName, PsgCompare.audioFormat8bit);
    }

    public void testFilterOutput(double[] rawBuffer) {
        for (FilterHelper.FilterType ft : FilterHelper.FilterType.values()) {
            DspUtil.highPass(ft, rawBuffer, filterBuffer);
            DspUtil.printDouble(ft.toString() + "_HP", filterBuffer);

            DspUtil.lowPass(ft, rawBuffer, filterBuffer);
            DspUtil.printDouble(ft.toString() + "_LP", filterBuffer);

            DspUtil.bandPass(ft, rawBuffer, filterBuffer);
            DspUtil.printDouble(ft.toString() + "_BP", filterBuffer);

            DspUtil.highPass(ft, rawBuffer, filterBuffer);
            DspUtil.printDouble(ft.toString() + "_HP", filterBuffer);

            DspUtil.lowPass(ft, rawBuffer, filterBuffer2);
            DspUtil.highPass(ft, filterBuffer2, filterBuffer);
            DspUtil.printDouble(ft.toString() + "_LPHP", filterBuffer);

            DspUtil.highPass(ft, rawBuffer, filterBuffer2);
            DspUtil.lowPass(ft, filterBuffer2, filterBuffer);
            DspUtil.printDouble(ft.toString() + "_HPLP", filterBuffer);
        }

    }
}
