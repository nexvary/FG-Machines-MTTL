package com.fgmachines.rck;

import java.util.Arrays;
import java.util.List;

public final class FanRemoteProfile {
    public final String id;
    public final String manufacturer;
    public final String model;
    public final Long powerOn;
    public final Long powerOff;
    public final Long speed;
    public final Long swing;
    public final Long timer;
    public final Long sleep;
    public final NecIrSignal.Order order;

    private FanRemoteProfile(String id, String manufacturer, String model,
                             Long powerOn, Long powerOff, Long speed, Long swing,
                             Long timer, Long sleep, NecIrSignal.Order order) {
        this.id=id;this.manufacturer=manufacturer;this.model=model;
        this.powerOn=powerOn;this.powerOff=powerOff;this.speed=speed;this.swing=swing;
        this.timer=timer;this.sleep=sleep;this.order=order;
    }

    public String label(){return manufacturer+" · "+model;}

    public IrSignal signal(Long code){
        if(code==null)throw new IllegalStateException("No verified IR code");
        return new NecIrSignal(code,order,1);
    }

    public static List<FanRemoteProfile> verifiedProfiles(){
        return Arrays.asList(
                new FanRemoteProfile("kanazawa_tower","Kanazawa","Tower Fan",
                        0x00FFA25DL,0x00FFE21DL,0x00FF02FDL,0x00FF38C7L,
                        0x00FF9867L,null,NecIrSignal.Order.LSB_PER_BYTE),
                new FanRemoteProfile("atomberg_bldc","Atomberg","BLDC Fan",
                        0x6E91F300L,0x6E91F300L,0x748BF300L,null,
                        0x6996F300L,0x718EF300L,NecIrSignal.Order.LSB_PER_BYTE)
        );
    }

    public static List<String> targets(){
        return Arrays.asList(
                "Fresh Smart Remote 16 · 500004491",
                "Fresh Shabah Stand Remote 18 · 500004558",
                "Fresh Shabah Wall Remote 18 · 500005315",
                "Fresh Classic Remote 2026 · 500021270",
                "Fresh Top Remote 16 · 500009092",
                "Fresh · Other remote fan",
                "Kanazawa · Tower Fan",
                "Atomberg · BLDC Fan",
                "Other fan · Smart Scan"
        );
    }

    public static FanRemoteProfile byId(String id){
        if(id==null)return null;
        for(FanRemoteProfile p:verifiedProfiles())if(p.id.equals(id))return p;
        return null;
    }
}
