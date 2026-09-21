package com.fgmachines.rck;

import java.util.ArrayList;
import java.util.List;

final class IrPatternBuilder {
    private final List<Integer> values = new ArrayList<>();

    IrPatternBuilder item(int mark, int space) {
        values.add(mark);
        values.add(space);
        return this;
    }

    IrPatternBuilder mark(int mark) {
        values.add(mark);
        return this;
    }

    IrPatternBuilder bit(boolean one, int mark, int oneSpace, int zeroSpace) {
        values.add(mark);
        values.add(one ? oneSpace : zeroSpace);
        return this;
    }

    IrPatternBuilder bytesLsb(byte[] data, int offset, int length,
                              int mark, int oneSpace, int zeroSpace) {
        for (int i = offset; i < offset + length; i++) {
            int value = data[i] & 0xFF;
            for (int bit = 0; bit < 8; bit++) {
                bit((value & (1 << bit)) != 0, mark, oneSpace, zeroSpace);
            }
        }
        return this;
    }

    IrPatternBuilder bytesMsb(byte[] data, int offset, int length,
                              int mark, int oneSpace, int zeroSpace) {
        for (int i = offset; i < offset + length; i++) {
            int value = data[i] & 0xFF;
            for (int bit = 7; bit >= 0; bit--) {
                bit((value & (1 << bit)) != 0, mark, oneSpace, zeroSpace);
            }
        }
        return this;
    }

    IrPatternBuilder intMsb(long value, int bits, int mark, int oneSpace, int zeroSpace) {
        for (int bit = bits - 1; bit >= 0; bit--) {
            bit((value & (1L << bit)) != 0, mark, oneSpace, zeroSpace);
        }
        return this;
    }

    IrPatternBuilder intLsb(long value, int bits, int mark, int oneSpace, int zeroSpace) {
        for (int bit = 0; bit < bits; bit++) {
            bit((value & (1L << bit)) != 0, mark, oneSpace, zeroSpace);
        }
        return this;
    }

    int[] toArray() {
        int[] out = new int[values.size()];
        for (int i = 0; i < values.size(); i++) out[i] = values.get(i);
        return out;
    }
}
