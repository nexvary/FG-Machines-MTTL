package com.fgmachines.rck;

public final class HaierAcProtocol implements IrSignal {
    public enum Mode { AUTO, COOL, DRY, FAN, HEAT }
    public enum Fan { AUTO, LOW, MED, HIGH }

    private boolean power=true, swing;
    private int temp=24;
    private Mode mode=Mode.COOL;
    private Fan fan=Fan.AUTO;

    public HaierAcProtocol power(boolean v){power=v;return this;}
    public HaierAcProtocol temp(int v){temp=Math.max(16,Math.min(30,v));return this;}
    public HaierAcProtocol mode(Mode v){mode=v==null?Mode.COOL:v;return this;}
    public HaierAcProtocol fan(Fan v){fan=v==null?Fan.AUTO:v;return this;}
    public HaierAcProtocol swing(boolean v){swing=v;return this;}

    public byte[] raw(){
        byte[] s=new byte[14]; s[0]=(byte)0xA6;
        s[1]=(byte)((swing?0xC:0)|((temp-16)<<4));
        s[2]=(byte)(7<<5);
        s[4]=(byte)(power?1<<6:0);
        int fc=fan==Fan.HIGH?1:fan==Fan.MED?2:fan==Fan.LOW?3:5;
        s[5]=(byte)(fc<<5);
        int mc=mode==Mode.AUTO?0:mode==Mode.COOL?1:mode==Mode.DRY?2:mode==Mode.HEAT?4:6;
        s[7]=(byte)(mc<<5);
        s[12]=(byte)(power?0x05:0x05);
        int sum=0;for(int i=0;i<13;i++)sum=(sum+(s[i]&0xFF))&0xFF;s[13]=(byte)sum;
        return s;
    }

    @Override public int carrierHz(){return 38000;}
    @Override public String protocolName(){return "HAIER-YR-W02";}
    @Override public int[] pattern(){
        byte[] s=raw();
        return new IrPatternBuilder().item(3000,3000).item(3000,4300)
                .bytesMsb(s,0,s.length,520,1650,650).item(520,100000).toArray();
    }
}
