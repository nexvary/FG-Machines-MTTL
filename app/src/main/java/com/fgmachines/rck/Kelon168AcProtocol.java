package com.fgmachines.rck;

public final class Kelon168AcProtocol implements IrSignal {
    public enum Mode { SMART, COOL, DRY, FAN, HEAT }
    public enum Fan { AUTO, LOW, MED, HIGH }

    private boolean power=true, swing;
    private int temp=24;
    private Mode mode=Mode.COOL;
    private Fan fan=Fan.AUTO;

    public Kelon168AcProtocol power(boolean v){power=v;return this;}
    public Kelon168AcProtocol temp(int v){temp=Math.max(16,Math.min(32,v));return this;}
    public Kelon168AcProtocol mode(Mode v){mode=v==null?Mode.COOL:v;return this;}
    public Kelon168AcProtocol fan(Fan v){fan=v==null?Fan.AUTO:v;return this;}
    public Kelon168AcProtocol swing(boolean v){swing=v;return this;}

    private static int xor(byte[] s,int off,int len){int x=0;for(int i=off;i<off+len;i++)x^=s[i]&0xFF;return x;}

    public byte[] raw(){
        byte[] s=new byte[21];
        s[0]=(byte)0x83;s[1]=0x06;s[6]=(byte)0x80;s[18]=0x28;
        int fi=fan==Fan.LOW?1:fan==Fan.MED?2:fan==Fan.HIGH?3:0;
        int[] f1={0,3,2,1};int[] f2={0,1,0,0};
        s[2]=(byte)(f1[fi]&3);
        if(power)s[2]|=1<<2;if(swing)s[2]|=(byte)0x80;
        int mc=mode==Mode.HEAT?0:mode==Mode.SMART?1:mode==Mode.COOL?2:mode==Mode.DRY?3:4;
        int t=mode==Mode.SMART?26:temp;
        s[3]=(byte)(mc|((t-16)<<4));
        s[15]=0x06;if(f2[fi]!=0)s[16]|=1<<1;if(power)s[18]|=1<<4;
        s[13]=(byte)xor(s,2,10);s[20]=(byte)xor(s,14,6);
        return s;
    }

    @Override public int carrierHz(){return 38000;}
    @Override public String protocolName(){return "KELON168/HISENSE";}
    @Override public int[] pattern(){
        byte[] s=raw();
        IrPatternBuilder b=new IrPatternBuilder().item(9000,4600)
                .bytesLsb(s,0,6,560,1680,600).item(560,8000)
                .bytesLsb(s,6,8,560,1680,600).item(560,8000)
                .bytesLsb(s,14,7,560,1680,600).item(560,100000);
        return b.toArray();
    }
}
