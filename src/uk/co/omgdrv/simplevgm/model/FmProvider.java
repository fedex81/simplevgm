package uk.co.omgdrv.simplevgm.model;

/**
 * ${FILE}
 * <p>
 * Federico Berti
 * <p>
 * Copyright 2019
 */
public interface FmProvider {

    int reset();

    int init(int Clock, int Rate);

    void update(int[] buf_lr, int offset, int end);

    void write0(int addr, int data);

    void write1(int addr, int data);
}
