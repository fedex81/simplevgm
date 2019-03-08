package uk.co.omgdrv.simplevgm.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * ${FILE}
 * <p>
 * Federico Berti
 * <p>
 * Copyright 2019
 */
public class BlipHelper {

    static Path file = Paths.get(".", System.currentTimeMillis() + ".raw");
    static boolean writeToFile = true;

    /**
     * out[0] = MSB LEFT
     * out[1] = LSB RIGHT
     * out[2] = MSB RIGHT
     * out[3] = LSB RIGHT
     */
    public static void printStereoData(String name, byte[] out, int start, int end, boolean bit8) {
        List<String> lines = new ArrayList<>();
        for (int i = start; i < end; i += 4) {
            int left = Util.getSigned16BE(out[i], out[i + 1]);
            int right = Util.getSigned16BE(out[i + 2], out[i + 3]);
            int val = (left + right) >> 1;
            int printVal = bit8 ? val >> 8 : val;
            System.out.println(name + "," + printVal);
            lines.add("" + val);
        }
        writeToFile(lines);
    }

    private static void writeToFile(List<String> lines) {
        if (writeToFile) {
            try {
                Files.write(file, lines, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
