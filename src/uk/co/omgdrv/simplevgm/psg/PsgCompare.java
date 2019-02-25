package uk.co.omgdrv.simplevgm.psg;

import uk.co.omgdrv.simplevgm.VGMPlayer;
import uk.co.omgdrv.simplevgm.VgmEmu;
import uk.co.omgdrv.simplevgm.model.VgmPsgProvider;
import uk.co.omgdrv.simplevgm.psg.gear.GearPsgProvider;
import uk.co.omgdrv.simplevgm.psg.green.SmsApu;
import uk.co.omgdrv.simplevgm.psg.nuked.NukedPsgProvider;
import uk.co.omgdrv.simplevgm.util.BlipBuffer;
import uk.co.omgdrv.simplevgm.util.StereoBuffer;
import uk.co.omgdrv.simplevgm.util.Util;

import javax.sound.sampled.AudioFormat;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ${FILE}
 * <p>
 * Federico Berti
 * <p>
 * Copyright 2019
 */
public class PsgCompare implements VgmPsgProvider {


    public enum PsgType {
        GEAR,
        NUKED,
        GREEN
    }

    private static final boolean SIGNED = true;
    private static AudioFormat audioFormat =
            new AudioFormat(VgmEmu.VGM_SAMPLE_RATE_HZ, 8, 1, SIGNED, false);

    private static Path nukeFile = Paths.get("Nuke_" + System.currentTimeMillis() + ".raw");
    private static Path gearFile = Paths.get("Gear_" + System.currentTimeMillis() + ".raw");
    private static Path greenFile = Paths.get("Green_" + System.currentTimeMillis() + ".raw");

    private GearPsgProvider gearPsg;
    private NukedPsgProvider nukePsg;
    private SmsApu greenPsg;
    private VGMPlayer vgmPlayer;
    private StereoBuffer stereoBuffer;

    static int RUN_FOR_SECONDS = 120;
    private byte[] greenBuffer = new byte[VgmEmu.VGM_SAMPLE_RATE_HZ];


    public static PsgCompare createInstance() {
        PsgCompare p = new PsgCompare();
        p.gearPsg = GearPsgProvider.createInstance(p);
        p.nukePsg = NukedPsgProvider.createInstance(p);
        p.greenPsg = new SmsApu();
        return p;
    }

    @Override
    public void writeData(int vgmDelayCycles, int data) {
        if (stereoBuffer == null) {
            stereoBuffer = vgmPlayer.getEmu().getStereoBuffer();
        }
        nukePsg.writeData(vgmDelayCycles, data);
        gearPsg.writeData(vgmDelayCycles, data);
        greenPsg.writeData(VgmEmu.toPSGTimeGreen(vgmDelayCycles), data);
    }

    @Override
    public void setOutput(BlipBuffer center, BlipBuffer left, BlipBuffer right) {
        greenPsg.setOutput(center, left, right);
    }

    @Override
    public void reset() {
        nukePsg.reset();
        gearPsg.reset();
        greenPsg.reset();
    }

    @Override
    public void writeGG(int time, int data) {
        greenPsg.writeGG(VgmEmu.toPSGTimeGreen(time), data);
    }

    @Override
    public void endFrame(int vgmDelayCycles) {
        nukePsg.endFrame(vgmDelayCycles);
        gearPsg.endFrame(vgmDelayCycles);
        greenPsg.endFrame(VgmEmu.toPSGTimeGreen(vgmDelayCycles));
//        readStereoBufferSamples();
    }

    int startBuffer = 0;

    //TODO
    private void readStereoBufferSamples() {
        int num = 0;
        do {
            num = stereoBuffer.samplesAvail() / 2;
            if (num > 0) {
                if (startBuffer + num < VgmEmu.VGM_SAMPLE_RATE_HZ) {
                    stereoBuffer.readSamples(greenBuffer, startBuffer, num);
                } else {
                    stereoBuffer.readSamples(greenBuffer, startBuffer, VgmEmu.VGM_SAMPLE_RATE_HZ);
                }
                startBuffer += num;
                if (startBuffer >= VgmEmu.VGM_SAMPLE_RATE_HZ) {
                    pushData(PsgType.GREEN, greenBuffer);
                    startBuffer = 0;
                }
            }
        } while (num > 0);
    }

    private void checkIntervalDone() {
        System.out.println("Seconds: " + nukePsg.secondsElapsed);
        if (nukePsg.secondsElapsed >= RUN_FOR_SECONDS) {
            System.out.println("Stopping after: " + RUN_FOR_SECONDS + " seconds");
            main(null);
            System.exit(0);
        }

    }

    public static void main(String[] args) {
        try {
            List<Path> files = Files.list(Paths.get(".")).filter(f -> f.toString().endsWith(".raw")).collect(Collectors.toList());
            files.stream().forEach(f -> Util.convertToWav(f.getFileName().toString(), audioFormat));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void pushData(PsgType type, byte[] buffer) {
        switch (type) {
            case GEAR:
                Util.writeToFile(gearFile, buffer);
                break;
            case GREEN:
                Util.writeToFile(greenFile, buffer);
                break;
            case NUKED:
                Util.writeToFile(nukeFile, buffer);
                break;
        }
        checkIntervalDone();
    }

    public void setVgmPlayer(VGMPlayer v) {
        this.vgmPlayer = v;
    }
}
