package com.example.cezary.mashtherm;

import java.util.HashMap;

/**
 * Created by Cezary on 06.06.2017.
 */

public class SampleGattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String Thermo_NRF52832 = "00002012-0000-1000-8000-00805f9b34fb";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    static {
        // Sample Services.
        attributes.put(Thermo_NRF52832, "Thermometer NRF52832");
        // Sample Characteristics.
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
