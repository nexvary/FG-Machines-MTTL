package com.fgmachines.rck;

public interface IrSignal {
    int carrierHz();
    int[] pattern();
    String protocolName();
}
