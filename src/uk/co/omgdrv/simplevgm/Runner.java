package uk.co.omgdrv.simplevgm;

import uk.co.omgdrv.simplevgm.model.VgmPsgProvider;
import uk.co.omgdrv.simplevgm.psg.PsgCompare;
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
    private static String VGM_FOLDER = "vgm/psg";
    private static String VGM_FILE = null; //"vgm/test03.vgm"; //"vgm/flash/Flash, The - 01  - Title Screen.vgz";
    private static boolean runPsgCompare = false;

    private static Predicate<Path> vgmFilesPredicate = p ->
            p.toString().endsWith(".vgm") || p.toString().endsWith(".vgz");

    public static void main(String[] args) throws Exception {
        Path path = getPathToPlay(args);
        boolean isFolder = path.toFile().isDirectory();
        System.out.println(String.format("Playing %s: %s",
                (isFolder ? "folder" : "file"), path.toAbsolutePath().toString()));
        VgmPsgProvider psgProvider = runPsgCompare ? PsgCompare.createInstance() : null;
        VGMPlayer v = VGMPlayer.createInstance(psgProvider);
        if(isFolder){
            playRecursive(v, path);
        } else {
            playOne(v, path);
        }
    }

    private static Path getPathToPlay(String[] args){
        String s = VGM_FILE != null ? VGM_FILE : (VGM_FOLDER != null ? VGM_FOLDER : ".");
        Path p = Paths.get(s);
        if(args.length > 0){
             p = Paths.get(args[0]);
        }
        return p;
    }

    public static List<Path> getRecursiveVgmFiles(Path folder) throws IOException {
        Set<Path> fileSet = new HashSet<>();
        Files.walkFileTree(folder, createFileVisitor(fileSet));
        List<Path> list = new ArrayList<>(fileSet);
        Collections.shuffle(list);
        System.out.println("VGM files found: " + fileSet.size());
        return list;
    }

    private void playAll(VGMPlayer v, String folderName) throws Exception {
        Path folder = Paths.get(".", folderName);
        List<Path> files = Files.list(folder).filter(vgmFilesPredicate).sorted().collect(Collectors.toList());
        files.stream().forEach(f -> playOne(v, f));
    }

    public static void playRecursive(VGMPlayer v, Path folder) throws Exception {
        getRecursiveVgmFiles(folder).stream().forEach(f -> playOne(v, f));
    }

    private static void playOne(VGMPlayer v, Path file) {
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

    private static void waitForCompletion(VGMPlayer v) {
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
