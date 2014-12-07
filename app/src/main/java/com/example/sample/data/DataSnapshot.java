package com.example.sample.data;

import com.example.sample.Constants;

import java.util.HashMap;
import java.util.Map;


/**
 * Created by ajay on 12/6/14.
 */
public class DataSnapshot {
    private Map<Integer, Long> data;
    private Long countersteerRating;

    private long previousTimestamp;

    // Efficiency data
    private long cumulativeAdjustedFuelConsumption;
    private long cumulativeAdjustedOdometer;

    private long fuelConsumptionSinceLast;
    private long odometerSinceLast;

    private double kmplCumulative;
    private double kmplSinceLast;

    public DataSnapshot() {
        this.data = new HashMap<Integer, Long>(50);
    }

    public void addData(int key, long value) {
 //       long precision = Constants.getPrecision(key);
 //       long newValue = value & precision;

        this.data.put(key, value);
    }

    public void addData(int key, int value) {
        this.data.put(key, (long)value);
    }

    public Long getData(int key) {
        return this.data.get(key);
    }

    public long calculateCountersteerRating(){
        if (this.countersteerRating != null) {
            return this.countersteerRating;
        } else {
            long transverseAcceleration = this.getData(Constants.ACCELERATION_TRANSVERSE);
            long steeringWheelAngle = this.getData(Constants.STEERING_WHEEL_ANGLE);

            long rating = Math.max(transverseAcceleration * steeringWheelAngle, 0l) * (data.get(Constants.TIMESTAMP) - this.previousTimestamp) / 1000l;

            this.countersteerRating = rating;
            return rating;
        }
    }

    public void calculateFuelConsumptionData(
            Long baselineFuelConsumption,
            Long baselineOdometer,
            Long lastFuelConsumption,
            Long lastOdometer) {

        if (baselineFuelConsumption == null){ return;}
        if (baselineOdometer == null){ return;}
        if (lastFuelConsumption == null){ return;}
        if (lastOdometer == null){ return;}

        cumulativeAdjustedFuelConsumption = getData(Constants.FUEL_CONSUMPTION) - baselineFuelConsumption;
        cumulativeAdjustedOdometer = getData(Constants.ODOMETER) - baselineOdometer;

        fuelConsumptionSinceLast = getData(Constants.FUEL_CONSUMPTION) - lastFuelConsumption;
        odometerSinceLast = getData(Constants.ODOMETER) - lastOdometer;

        calculateEfficiencyRatings();
    }

    private void calculateEfficiencyRatings()  {
        kmplCumulative = (double)cumulativeAdjustedOdometer * 10.0 / (double)cumulativeAdjustedFuelConsumption;
        kmplSinceLast = (double)odometerSinceLast * 10.0 / (double)fuelConsumptionSinceLast;
    }

    public double getKmplCumulative() {
        return this.kmplCumulative;
    }

    public double getKmplSinceLast() {
        return this.kmplSinceLast;
    }

    public void setPreviousTimestamp(long timestamp) {
        this.previousTimestamp = timestamp;
    }
}
