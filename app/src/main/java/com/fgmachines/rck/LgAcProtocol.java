package com.fgmachines.rck;

public final class LgAcProtocol implements IrSignal {
    public enum Mode { COOL, DRY, FAN, AUTO, HEAT }
    public enum Fan { AUTO, LOW, MED, HIGH }
    public enum Model { LG, LG2 }

    private static final long OFF=0x88C0051L, SWING=0x8810001L;
    private boolean power=true, swingToggle;
    private int temp=24;
    private Mode mode=Mode.COOL;
    private Fan fan=Fan.AUTO;
    private Model model=Model.LG;

    public LgAcProtocol power(boolean v){power=v;return this;}
    public LgAcProtocol temp(int v){temp=Math.max(16,Math.min(30,v));return this;}
    public LgAcProtocol mode(Mode v){mode=v==null?Mode.COOL:v;return this;}
    public LgAcProtocol fan(Fan v){fan=v==null?Fan.AUTO:v;return this;}
    public LgAcProtocol model(Model v){model=v==null?Model.LG:v;return this;}
    public LgAcProtocol swingToggle(){swingToggle=true;return this;}

    private static int checksum(long raw){
        long body=raw>>>4; int sum=0;
        for(int i=0;i<4;i++) sum+=(body>>>(i*4))&0xF;
        return sum&0xF;
    }

    public long raw(){
        if(!power) return OFF;
        if(swingToggle) return SWING;
        int mc=mode==Mode.COOL?0:mode==Mode.DRY?1:mode==Mode.FAN?2:mode==Mode.AUTO?3:4;
        int fc=fan==Fan.LOW?1:fan==Fan.MED?2:fan==Fan.HIGH?4:5;
        long r=(0x88L<<20)|((long)mc<<12)|((long)(temp-15)<<8)|((long)fc<<4);
        return r|checksum(r);
    }

    @Override public int carrierHz(){return 38000;}
    @Override public String protocolName(){return model==Model.LG2?"LG2-28":"LG-28";}
    @Override public int[] pattern(){
        long code=raw();
        int hm=model==Model.LG2?3200:8500;
        int hs=model==Model.LG2?9900:4250;
        int bm=model==Model.LG2?480:550;
        return new IrPatternBuilder().item(hm,hs).intMsb(code,28,bm,1600,550).item(bm,100000).toArray();
    }
}
