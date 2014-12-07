package com.example.sample;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ajay on 12/6/14.
 */
public class Constants {

    // Configuration
    public static final int DRIFT_THRESHOLD = 25;

    //Timestamp category
    public static final int TIMESTAMP = 0x01;

    // Driving operation category
    public static final int ACCELERATOR_POSITION = 0x02;
    public static final int BRAKE_PEDAL_STATUS = 0x03;
    public static final int PARKING_BRAKE_STATUS = 0x04;
    public static final int AT_SHIFT_POSITION = 0x05;
    public static final int MANUAL_MODE_STATUS = 0x06;
    public static final int TRANSMISSION_GEAR_POSITION = 0x07;
    public static final int STEERING_WHEEL_ANGLE = 0x08;
    public static final int DOORS_STATUS = 0x09;
    public static final int SEATBELTS_STATUS = 0x0A;
    public static final int HEADLAMP_STATUS = 0x0B;

    // Vehicle status category
    public static final int ENGINE_REVOLUTION_SPEED = 0x0C;
    public static final int VEHICLE_SPEED = 0x0D;
    public static final int ACCELERATION_FRONT_BACK = 0x0E;
    public static final int ACCELERATION_TRANSVERSE = 0x0F;
    public static final int YAW_RATE = 0x10;
    public static final int ODOMETER  = 0x11;
    public static final int FUEL_CONSUMPTION = 0x12;
    public static final int OUTSIDE_TEMPERATURE  = 0x13;
    public static final int ENGINE_COOLANT_TEMPERATURE  = 0x14;
    public static final int ENGINE_OIL_TEMPERATURE  = 0x15;
    public static final int TRANSMISSION_TYPE = 0x16;

    // GPS/GNSS signal category

    public static final int GPS_TIME = 0x17;
    public static final int LATITUDE = 0x18;
    public static final int NORTH_OR_SOUTH = 0x19;
    public static final int LONGITUDE = 0x1A;
    public static final int EAST_OR_WEST = 0x1B;
    public static final int GPS_QUALITY = 0x1C;
    public static final int NUMBER_OF_SATELLITES = 0x1D;
    public static final int ANTENNA_ALTITUDE = 0x1E;
    public static final int ALTITUDE_UNITS = 0x1F;
    public static final int GEOIDAL_SEPARATION = 0x20;
    public static final int GEOIDAL_SEPARATION_UNITS = 0x21;
    public static final int SPEED_OVER_GROUND = 0x22;
    public static final int COURSE_OVER_GROUND = 0x23;


    public static Map<Integer, Integer> dataPrecision;
    static {
        dataPrecision = new HashMap(50);

        dataPrecision.put(1, 32);

        dataPrecision.put(2, 7);
        dataPrecision.put(3, 1);
        dataPrecision.put(4, 1);
        dataPrecision.put(5, 6);
        dataPrecision.put(6, 1);
        dataPrecision.put(7, 4);
        dataPrecision.put(8, 12);
        dataPrecision.put(9, 5);
        dataPrecision.put(10, 2);
        dataPrecision.put(11, 1);

        dataPrecision.put(12, 14);
        dataPrecision.put(13, 9);
        dataPrecision.put(14, 11);
        dataPrecision.put(15, 11);
        dataPrecision.put(16, 14);
        dataPrecision.put(17, 32);
        dataPrecision.put(18, 32);
        dataPrecision.put(19, 9);
        dataPrecision.put(20, 9);
        dataPrecision.put(21, 9);
        dataPrecision.put(22, 8);

        dataPrecision.put(23, 28);
        dataPrecision.put(24, 31);
        dataPrecision.put(25, 1);
        dataPrecision.put(26, 30);
        dataPrecision.put(27, 1);
        dataPrecision.put(28, 2);
        dataPrecision.put(29, 8);
        dataPrecision.put(30, 26);
        dataPrecision.put(31, 1);
        dataPrecision.put(32, 26);
        dataPrecision.put(33, 1);
        dataPrecision.put(34, 20);
        dataPrecision.put(35, 16);
    }

    public static long getPrecision(int key){
        int numPrecisionBits = dataPrecision.get(key);
        int pushRight = 32 - numPrecisionBits;

        long mask = 0xFFFFFFFF;
        return mask >>> pushRight;
    }

    public static boolean isSigned (int key) {
        if ((key > 13) && (key < 17)) {
            return true;
        }

        if ((key > 18) && (key < 22)) {
            return true;
        }

        if (key == 8) {
            return true;
        }

        return false;
    }
}
