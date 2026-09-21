package com.fgmachines.rck;

public final class NecIrSignal implements IrSignal {
    public enum Order { LSB_PER_BYTE, MSB_32 }
    private final long code;
    private final Order order;
    private final int repeats;

    public NecIrSignal(long code, Order order, int repeats){
        this.code=code&0xFFFFFFFFL;
        this.order=order==null?Order.LSB_PER_BYTE:order;
        this.repeats=Math.max(0,repeats);
    }

    private void frame(IrPatternBuilder b){
        b.item(9000,4500);
        if(order==Order.MSB_32){
            b.intMsb(code,32,560,1690,560);
        }else{
            for(int shift=24;shift>=0;shift-=8){
                int v=(int)((code>>>shift)&0xFF);
                for(int bit=0;bit<8;bit++)b.bit((v&(1<<bit))!=0,560,1690,560);
            }
        }
        b.item(560,40000);
    }

    @Override public int carrierHz(){return 38000;}
    @Override public String protocolName(){return "NEC32";}
    @Override public int[] pattern(){
        IrPatternBuilder b=new IrPatternBuilder();
        for(int i=0;i<=repeats;i++)frame(b);
        return b.toArray();
    }
}
