package com.fgmachines.rck;

public final class ToshibaAcProtocol implements IrSignal {
    public enum Mode { AUTO, COOL, DRY, FAN, HEAT }
    public enum Fan { AUTO, LOW, MED, HIGH }
    public enum Model { GENERIC, WA_TH0X }

    private boolean power=true, swing;
    private int temp=24;
    private Mode mode=Mode.COOL;
    private Fan fan=Fan.AUTO;
    private Model model=Model.GENERIC;

    public ToshibaAcProtocol power(boolean v){power=v;return this;}
    public ToshibaAcProtocol temp(int v){temp=Math.max(17,Math.min(30,v));return this;}
    public ToshibaAcProtocol mode(Mode v){mode=v==null?Mode.COOL:v;return this;}
    public ToshibaAcProtocol fan(Fan v){fan=v==null?Fan.AUTO:v;return this;}
    public ToshibaAcProtocol swing(boolean v){swing=v;return this;}
    public ToshibaAcProtocol model(Model v){model=v==null?Model.GENERIC:v;return this;}

    public byte[] raw(){
        byte[] s=new byte[9];
        s[0]=(byte)0xF2;s[1]=(byte)~0xF2;
        s[2]=(byte)(0x03 | ((model==Model.WA_TH0X?1:0)<<4));
        s[3]=(byte)~s[2];s[4]=0x01;
        s[5]=(byte)((swing?1:2)|((temp-17)<<4));
        int mc=!power?7:mode==Mode.AUTO?0:mode==Mode.COOL?1:mode==Mode.DRY?2:mode==Mode.HEAT?3:4;
        int fc=fan==Fan.LOW?1:fan==Fan.MED?3:fan==Fan.HIGH?5:0;
        s[6]=(byte)(mc|(fc<<5));s[7]=0;
        int x=0;for(int i=0;i<8;i++)x^=s[i]&0xFF;s[8]=(byte)x;
        return s;
    }

    @Override public int carrierHz(){return 38000;}
    @Override public String protocolName(){return "TOSHIBA/"+model.name();}
    @Override public int[] pattern(){
        byte[] s=raw();
        IrPatternBuilder b=new IrPatternBuilder();
        for(int pass=0;pass<2;pass++){
            b.item(4400,4300).bytesMsb(s,0,s.length,580,1600,490);
            if(pass==0)b.item(580,7400); else b.item(580,100000);
        }
        return b.toArray();
    }
}
