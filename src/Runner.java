import uk.co.omgdrv.simplevgm.VGMPlayer;
import uk.co.omgdrv.simplevgm.model.PsgProvider;
import uk.co.omgdrv.simplevgm.util.Util;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * ${FILE}
 * <p>
 * Federico Berti
 * <p>
 * Copyright 2019
 */
public class Runner {

    private static boolean DISABLE_PSG = false;
    private static String VGM_FOLDER = "/data/emu/vgm/"; //"vgm";
    private static String VGM_FILE = "ghostbuster.vgm";

    private static Predicate<Path> vgmFilesPredicate = p ->
            p.toString().endsWith(".vgm") || p.toString().endsWith(".vgz");


    public static void main(String[] args) throws Exception {
        PsgProvider psgProvider = DISABLE_PSG ? PsgProvider.NO_SOUND : null;
        VGMPlayer v = VGMPlayer.createInstance(psgProvider, 44100);
        Runner r = new Runner();
//        r.playAll(v, VGM_FOLDER);
        r.playRecursive(v, VGM_FOLDER);
    }

    private void playAll(VGMPlayer v, String folderName) throws Exception {
        Path folder = Paths.get(".", folderName);
        List<Path> files = Files.list(folder).filter(vgmFilesPredicate).sorted().collect(Collectors.toList());
        files.stream().forEach(f -> playOne(v, f));
    }

    private void playRecursive(VGMPlayer v, String folderName) throws Exception {
        Path folder = Paths.get(folderName);
        Set<Path> fileSet = new HashSet<>();
        Files.walkFileTree(folder, createFileVisitor(fileSet));
        List<Path> list = new ArrayList<>(fileSet);
        Collections.shuffle(list);
        System.out.println("VGM files found: " + fileSet.size());
        list.stream().forEach(f -> playOne(v, f));
    }

    private void playOne(VGMPlayer v, Path file) {
        try {
            System.out.println("Playing: " + file.toAbsolutePath().toString());
            v.loadFile(file.toAbsolutePath().toString());
            v.startTrack(0);
            waitForCompletion(v);
            v.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void waitForCompletion(VGMPlayer v){
        do {
            Util.sleep(1000);
        } while (v.isPlaying());
    }

    private static FileVisitor<Path> createFileVisitor(Set<Path> fileSet){
        return new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if(vgmFilesPredicate.test(file)) {
                    fileSet.add(file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        };
    }
}
