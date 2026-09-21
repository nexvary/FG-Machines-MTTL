package com.fgmachines.rck;

public final class GreeAcProtocol implements IrSignal {
    public enum Mode { AUTO, COOL, DRY, FAN, HEAT }
    public enum Fan { AUTO, LOW, MED, HIGH }
    public enum Model { YAW1F, YBOFB, YX1FSF }

    private static final int H=9000, HS=4000, M=620, O=1600, Z=540, GAP=19000;
    private boolean power = true, swing;
    private int temp = 24;
    private Mode mode = Mode.COOL;
    private Fan fan = Fan.AUTO;
    private Model model = Model.YAW1F;

    public GreeAcProtocol power(boolean v){power=v;return this;}
    public GreeAcProtocol temp(int v){temp=Math.max(16,Math.min(30,v));return this;}
    public GreeAcProtocol mode(Mode v){mode=v==null?Mode.COOL:v;return this;}
    public GreeAcProtocol fan(Fan v){fan=v==null?Fan.AUTO:v;return this;}
    public GreeAcProtocol swing(boolean v){swing=v;return this;}
    public GreeAcProtocol model(Model v){model=v==null?Model.YAW1F:v;return this;}

    private static int checksum(byte[] st){
        int sum=10;
        for(int i=0;i<4;i++) sum+=st[i]&0x0F;
        for(int i=4;i<7;i++) sum+=(st[i]&0xFF)>>>4;
        return sum&0x0F;
    }

    public byte[] raw(){
        byte[] s=new byte[8];
        int mc=mode==Mode.AUTO?0:mode==Mode.COOL?1:mode==Mode.DRY?2:mode==Mode.FAN?3:4;
        int fc=fan==Fan.LOW?1:fan==Fan.MED?2:fan==Fan.HIGH?3:0;
        int t=mode==Mode.AUTO?25:temp;
        s[0]=(byte)(mc | (power?8:0) | (fc<<4) | (swing?0x40:0));
        s[1]=(byte)(t-16);
        s[2]=(byte)((power&&model==Model.YAW1F)?0x40:0);
        s[3]=0x50;
        s[4]=(byte)(swing?1:0);
        s[5]=0x20;
        s[6]=0;
        s[7]=(byte)(checksum(s)<<4);
        return s;
    }

    @Override public int carrierHz(){return 38000;}
    @Override public String protocolName(){return "GREE/"+model.name();}
    @Override public int[] pattern(){
        byte[] s=raw();
        IrPatternBuilder b=new IrPatternBuilder().item(H,HS)
                .bytesLsb(s,0,4,M,O,Z)
                .bit(false,M,O,Z).bit(true,M,O,Z).bit(false,M,O,Z)
                .item(M,GAP)
                .bytesLsb(s,4,4,M,O,Z)
                .item(M,GAP);
        return b.toArray();
    }
}
