package uk.co.omgdrv.simplevgm.psg;

import uk.co.omgdrv.simplevgm.VGMPlayer;
import uk.co.omgdrv.simplevgm.VgmEmu;
import uk.co.omgdrv.simplevgm.model.VgmPsgProvider;
import uk.co.omgdrv.simplevgm.psg.gear.GearPsgProvider;
import uk.co.omgdrv.simplevgm.psg.gear2.Gear2PsgProvider;
import uk.co.omgdrv.simplevgm.psg.green.SmsApu;
import uk.co.omgdrv.simplevgm.psg.nuked.BlipNukedPsgProvider;
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
        GEAR2,
        NUKED,
        NUKED_BLIP,
        GREEN
    }

    private static final boolean SIGNED = true;
    private static AudioFormat audioFormat =
            new AudioFormat(VgmEmu.VGM_SAMPLE_RATE_HZ, 8, 1, SIGNED, false);

    private static Path nukeFile = Paths.get("Nuke_" + System.currentTimeMillis() + ".raw");
    private static Path nukeBlipFile = Paths.get("NukeBlip_" + System.currentTimeMillis() + ".raw");
    private static Path gearFile = Paths.get("Gear_" + System.currentTimeMillis() + ".raw");
    private static Path gearFile2 = Paths.get("Gear2_" + System.currentTimeMillis() + ".raw");
    private static Path greenFile = Paths.get("Green_" + System.currentTimeMillis() + ".raw");

    private GearPsgProvider gearPsg;
    private Gear2PsgProvider gear2Psg;
    private NukedPsgProvider nukePsg;
    private SmsApu greenPsg;
    private BlipNukedPsgProvider blipNukedPsg;
    private VGMPlayer vgmPlayer;
    private StereoBuffer stereoBuffer;

    static int RUN_FOR_SECONDS = 10;

    public static PsgCompare createInstance() {
        PsgCompare p = new PsgCompare();
        p.gearPsg = GearPsgProvider.createInstance(p);
        p.gear2Psg = Gear2PsgProvider.createInstance(p);
        p.nukePsg = NukedPsgProvider.createInstance(p);
        p.blipNukedPsg = BlipNukedPsgProvider.createInstance(p);
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
        gear2Psg.writeData(vgmDelayCycles, data);
        blipNukedPsg.writeData(vgmDelayCycles, data);
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
        gear2Psg.reset();
        blipNukedPsg.reset();
    }

    @Override
    public void writeGG(int time, int data) {
        greenPsg.writeGG(VgmEmu.toPSGTimeGreen(time), data);
    }

    @Override
    public void endFrame(int vgmDelayCycles) {
        nukePsg.endFrame(vgmDelayCycles);
        gearPsg.endFrame(vgmDelayCycles);
        gear2Psg.endFrame(vgmDelayCycles);
        blipNukedPsg.endFrame(vgmDelayCycles);
        greenPsg.endFrame(VgmEmu.toPSGTimeGreen(vgmDelayCycles));
    }

    private void checkIntervalDone() {
//        System.out.println("Seconds: " + nukePsg.secondsElapsed);
        if (nukePsg.secondsElapsed >= RUN_FOR_SECONDS) {
            System.out.println("Stopping after: " + RUN_FOR_SECONDS + " seconds");
            main(null);
            System.exit(0);
        }

    }

    public static void main(String[] args) {
        try {
            List<Path> files = Files.list(Paths.get(".")).
                    filter(f -> f.toString().endsWith(".raw")).collect(Collectors.toList());
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
            case GEAR2:
                Util.writeToFile(gearFile2, buffer);
                break;
            case GREEN:
                //TODO this is 16bit
                Util.writeToFile(greenFile, buffer);
                break;
            case NUKED:
                Util.writeToFile(nukeFile, buffer);
                break;
            case NUKED_BLIP:
                Util.writeToFile(nukeBlipFile, buffer);
                break;
        }
        checkIntervalDone();
    }

    public void setVgmPlayer(VGMPlayer v) {
        this.vgmPlayer = v;
    }
}
