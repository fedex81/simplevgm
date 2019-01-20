package uk.co.omgdrv.simplevgm;

import uk.co.omgdrv.simplevgm.model.VgmHeader;
import uk.co.omgdrv.simplevgm.util.Util;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * ${FILE}
 * <p>
 * Federico Berti
 * <p>
 * Copyright 2019
 */
public class Scanner {

    private static String VGM_FOLDER = "/data/emu/vgm"; //"vgm";

    public static void main(String[] args) throws Exception {
        Path p = Paths.get(VGM_FOLDER);
        Runner.getRecursiveVgmFiles(p).stream().forEach(Scanner::printVgmHeader);
    }

    private static void printVgmHeader(Path p){
        String filepath = p.toAbsolutePath().toString();
        try {
            byte[] data = Util.readFile(p.toAbsolutePath().toString());
            VgmHeader vgmHeader = VgmHeader.loadHeader(data);
            if(vgmHeader.getYm2612Clk() == 0 && vgmHeader.getVersion() <= 0x101) {
                System.out.println("File: " +filepath);
                System.out.println(vgmHeader + "\n\n");
            }
        } catch (Exception e) {
            System.err.println("File: " +filepath  + ", " + e.getMessage());
        }
    }
}
