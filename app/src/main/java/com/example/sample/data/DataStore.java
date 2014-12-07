package com.example.sample.data;

import com.example.sample.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by ajay on 12/6/14.
 */
public class DataStore {
    public static final String DRIFT_RATING = "DRIFT_RATING";

    private List<DataSnapshot> snapshots;
    private Map<String, Long> aggregates;

    // Derived data

    // Location in radians
    private List<Double> latitudeInRadians;
    private List<Double> longitudeInRadians;

    private long lastKnownTimestamp;

    //Fuel consumption data
    private Long fuelConsumptionBaseline;
    private Long odometerBaseline;

    private Queue<Long> lastFuelConsumption;
    private Queue<Long> lastOdometer;

    public DataStore() {
        this.snapshots = new ArrayList<DataSnapshot>(1000);
        this.aggregates = new HashMap<String, Long>(20);
        this.aggregates.put(DRIFT_RATING, 0l);

        lastFuelConsumption = new LinkedBlockingQueue<Long>();
        lastOdometer = new LinkedBlockingQueue<Long>();
    }

    public void addSnapshot(DataSnapshot snapshot) {
        if (this.snapshots.size() == 0) {
            fuelConsumptionBaseline = snapshot.getData(Constants.FUEL_CONSUMPTION);
            odometerBaseline = snapshot.getData(Constants.ODOMETER);
        }
        lastFuelConsumption.add(snapshot.getData(Constants.FUEL_CONSUMPTION));
        lastOdometer.add(snapshot.getData(Constants.ODOMETER));

        snapshots.add(snapshot);

        snapshot.setPreviousTimestamp(lastKnownTimestamp);

        if (lastFuelConsumption.size() > 10) {
            snapshot.calculateFuelConsumptionData(fuelConsumptionBaseline, odometerBaseline, lastFuelConsumption.remove(), lastOdometer.remove());
        }

        processAggregates(snapshot);
        lastKnownTimestamp = snapshot.getData(Constants.TIMESTAMP);
    }

    private void processAggregates(DataSnapshot snapshot) {
        Long currentDriftRating = this.aggregates.get(DRIFT_RATING);

        if (snapshot.calculateCountersteerRating() > Constants.DRIFT_THRESHOLD) {
            this.aggregates.put(DRIFT_RATING, currentDriftRating + snapshot.calculateCountersteerRating());
        }

//        long unprocessedLatitude = snapshot.getData(Constants.LATITUDE);
//        long unprocessedLongitude = snapshot.getData(Constants.LONGITUDE);

//        processLatitudeAndLongitude(unprocessedLatitude, unprocessedLongitude);

    }

    private void processLatitudeAndLongitude(long unprocessedLatitude, long unprocessedLongitude) {
        double minutesLatitude = ((double)(unprocessedLatitude % 10000000l))/(10000.0);
        double degreesLatitude = ((double) unprocessedLatitude / 10000000l) + minutesLatitude;

        double minutesLongitude = ((double)(unprocessedLongitude % 10000000l))/(10000.0);
        double degreesLongitude = ((double) unprocessedLongitude / 10000000l) + minutesLongitude;

        double latitudeRadians = Math.toRadians(degreesLatitude);
        double longitudeRadians = Math.toRadians(degreesLongitude);

        this.latitudeInRadians.add(latitudeRadians);
        this.longitudeInRadians.add(longitudeRadians);
    }

    public long getDriftScore(){
        return this.aggregates.get(DRIFT_RATING);
    }
}
