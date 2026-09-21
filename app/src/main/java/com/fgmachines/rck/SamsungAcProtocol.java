package com.fgmachines.rck;

public final class SamsungAcProtocol implements IrSignal {
    public enum Mode { AUTO, COOL, DRY, FAN, HEAT }
    public enum Fan { AUTO, LOW, MED, HIGH, TURBO }

    private static final int LEAD_M=690, LEAD_S=17844, SEC_M=3086, SEC_S=8864,
            SEC_GAP=2886, BIT_M=586, ONE=1432, ZERO=436;
    private boolean power=true, swing, extended=true;
    private int temp=24;
    private Mode mode=Mode.COOL;
    private Fan fan=Fan.AUTO;

    public SamsungAcProtocol power(boolean v){power=v;extended=true;return this;}
    public SamsungAcProtocol temp(int v){temp=Math.max(16,Math.min(30,v));return this;}
    public SamsungAcProtocol mode(Mode v){mode=v==null?Mode.COOL:v;return this;}
    public SamsungAcProtocol fan(Fan v){fan=v==null?Fan.AUTO:v;return this;}
    public SamsungAcProtocol swing(boolean v){swing=v;return this;}
    public SamsungAcProtocol normalFrame(){extended=false;return this;}

    private static int pop(int v){int n=0;v&=255;while(v!=0){n+=v&1;v>>>=1;}return n;}
    private static int checksum(byte[] s,int off){
        int sum=pop(s[off]&0xFF)+pop(s[off+1]&0x0F)+pop((s[off+2]&0xFF)>>>4);
        for(int i=3;i<7;i++)sum+=pop(s[off+i]&0xFF);
        return (sum^0xFF)&0xFF;
    }
    private static void storeChecksum(byte[] s,int off){
        int sum=checksum(s,off);
        s[off+1]=(byte)((s[off+1]&0x0F)|((sum&0x0F)<<4));
        s[off+2]=(byte)((s[off+2]&0xF0)|((sum>>>4)&0x0F));
    }

    public byte[] raw14(){
        byte[] s={(byte)0x02,(byte)0x92,(byte)0x0F,0,0,0,(byte)0xF0,
                0x01,0x02,(byte)0xAE,0x71,0,0x15,(byte)0xF0};
        int mc=mode==Mode.AUTO?0:mode==Mode.COOL?1:mode==Mode.DRY?2:mode==Mode.FAN?3:4;
        int fc=mode==Mode.AUTO?6:fan==Fan.LOW?2:fan==Fan.MED?4:fan==Fan.HIGH?5:fan==Fan.TURBO?7:0;
        int p=power?3:0;
        s[6]=(byte)((s[6]&~0x30)|(p<<4));
        s[13]=(byte)((s[13]&~0x30)|(p<<4));
        s[9]=(byte)((s[9]&~0x70)|((swing?2:7)<<4));
        s[11]=(byte)((s[11]&0x0F)|((temp-16)<<4));
        s[12]=(byte)((fc<<1)|(mc<<4));
        storeChecksum(s,0);storeChecksum(s,7);
        return s;
    }

    private byte[] extended(byte[] s){
        byte[] mid={0x01,(byte)0xD2,0x0F,0,0,0,0};
        byte[] e=new byte[21];
        System.arraycopy(s,0,e,0,7);System.arraycopy(mid,0,e,7,7);System.arraycopy(s,7,e,14,7);
        storeChecksum(e,0);storeChecksum(e,7);storeChecksum(e,14);
        return e;
    }

    private int[] encode(byte[] s){
        IrPatternBuilder b=new IrPatternBuilder().item(LEAD_M,LEAD_S);
        int sections=s.length/7;
        for(int sec=0;sec<sections;sec++){
            b.item(SEC_M,SEC_S).bytesLsb(s,sec*7,7,BIT_M,ONE,ZERO);
            if(sec+1<sections)b.item(BIT_M,SEC_GAP); else b.item(BIT_M,100000);
        }
        return b.toArray();
    }

    @Override public int carrierHz(){return 38000;}
    @Override public String protocolName(){return extended?"SAMSUNG-AC21":"SAMSUNG-AC14";}
    @Override public int[] pattern(){
        byte[] s=raw14();
        return encode(extended?extended(s):s);
    }
}
