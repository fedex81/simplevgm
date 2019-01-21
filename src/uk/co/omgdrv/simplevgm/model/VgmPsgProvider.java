package uk.co.omgdrv.simplevgm.model;

import uk.co.omgdrv.simplevgm.util.BlipBuffer;

/**
 * ${FILE}
 * <p>
 * Federico Berti
 * <p>
 * Copyright 2019
 */
public interface VgmPsgProvider {

    void writeData(int time, int data);

    void setOutput(BlipBuffer center, BlipBuffer left, BlipBuffer right);

    void reset();

    void writeGG(int time, int data);

    void endFrame(int endTime);

    VgmPsgProvider NO_SOUND = new VgmPsgProvider() {
        @Override
        public void writeData(int time, int data) {

        }

        @Override
        public void setOutput(BlipBuffer center, BlipBuffer left, BlipBuffer right) {

        }

        @Override
        public void reset() {

        }

        @Override
        public void writeGG(int time, int data) {

        }

        @Override
        public void endFrame(int endTime) {

        }
    };
}
