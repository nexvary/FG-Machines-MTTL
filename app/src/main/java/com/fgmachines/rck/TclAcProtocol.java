package com.fgmachines.rck;

public final class TclAcProtocol implements IrSignal {
    public enum Mode { AUTO, COOL, DRY, FAN, HEAT }
    public enum Fan { AUTO, LOW, MED, HIGH }

    private boolean power=true, swing;
    private int temp=24;
    private Mode mode=Mode.COOL;
    private Fan fan=Fan.AUTO;

    public TclAcProtocol power(boolean v){power=v;return this;}
    public TclAcProtocol temp(int v){temp=Math.max(16,Math.min(31,v));return this;}
    public TclAcProtocol mode(Mode v){mode=v==null?Mode.COOL:v;return this;}
    public TclAcProtocol fan(Fan v){fan=v==null?Fan.AUTO:v;return this;}
    public TclAcProtocol swing(boolean v){swing=v;return this;}

    public byte[] raw(){
        byte[] s=new byte[14];
        s[0]=0x23;s[1]=(byte)0xCB;s[2]=0x26;s[3]=0x01;s[4]=0;
        s[5]=(byte)(power?1<<2:0);
        int mc=mode==Mode.HEAT?1:mode==Mode.DRY?2:mode==Mode.COOL?3:mode==Mode.FAN?7:8;
        s[6]=(byte)mc;
        s[7]=(byte)((31-temp)&0x0F);
        int fc=fan==Fan.LOW?2:fan==Fan.MED?3:fan==Fan.HIGH?5:0;
        s[8]=(byte)(fc | ((swing?7:0)<<3));
        int sum=0;for(int i=0;i<13;i++)sum=(sum+(s[i]&0xFF))&0xFF;
        s[13]=(byte)sum;
        return s;
    }

    @Override public int carrierHz(){return 38000;}
    @Override public String protocolName(){return "TCL112";}
    @Override public int[] pattern(){
        byte[] s=raw();
        return new IrPatternBuilder().item(3000,1650).bytesLsb(s,0,s.length,500,1050,325).item(500,100000).toArray();
    }
}
