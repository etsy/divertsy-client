package com.divertsy.hid.usb;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.divertsy.hid.ScaleApplication;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ScaleMeasurement {

    private double scaleWeight;
    private String unit;
    private final double rawScaleWeight;
    private final long now;
    private final String date;
    private final String date_time;
    public static final String[] csv_headers = {"scalename","office","weight", "type",
            "unit","time","date","date_time","bin_info","floor","location"};

    public ScaleMeasurement(double scaleWeight, @NonNull String unit, double rawScaleWeight) {
        this.scaleWeight = scaleWeight;
        this.unit = unit;
        this.rawScaleWeight = rawScaleWeight;

        // Not going to use local format since this could change how data gets encoded
        // for the backend processing.
        this.now = System.currentTimeMillis();
        Date dateObj = new Date(this.now);
        SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd");
        this.date = s.format(dateObj);
        SimpleDateFormat t = new SimpleDateFormat("HH:mm:ss z");
        this.date_time = t.format(dateObj);
    }

    @NonNull
    public String toJson(@NonNull String office, @NonNull String weightType, @Nullable String floor, @Nullable String location) {
        return "[{" +
        "scalename:" + '"' + ScaleApplication.get().getDeviceId() + '"' +
        ", office:" + '"'+ office +'"' +
        ", weight:" + Double.toString(scaleWeight) +
        ", type:" + '"'+ weightType +'"' +
        ", unit:" + '"'+ unit + '"' +
        ", time:" + (int)(now / 1000) +
        ", date:" + '"'+ date + '"' +
        ", date_time:" + '"'+ date_time + '"' +
        ", bin_info:" + Double.toString(rawScaleWeight) +
        (floor == null ? "" : ", floor:" + '"' + floor + '"') +
        (location == null ? "" : ", location:" + '"' + location + '"') +
        "}]";
    }

    @NonNull
    private  String cleanForCSV(@Nullable String input){
        if (input == null)
            return "";
        input = input.replace('"', '\'');
        if(input.contains(",")) {
            input = '"' + input + '"';
        }
        return input;
    }

    @NonNull
    public String toCSV(@NonNull String office, @NonNull String weightType, @Nullable String floor, @Nullable String location) {
        return ScaleApplication.get().getDeviceId() +
                "," + cleanForCSV(office) +
                "," + cleanForCSV(Double.toString(scaleWeight)) +
                "," + cleanForCSV(weightType)  +
                "," + cleanForCSV(unit) +
                "," + (int)(now / 1000) +
                "," + cleanForCSV(date) +
                "," + cleanForCSV(date_time) +
                "," + cleanForCSV(Double.toString(rawScaleWeight)) +
                "," + cleanForCSV(floor) +
                "," + cleanForCSV(location) ;
    }

    public long getTime() {
        return now;
    }

    public double getScaleWeight() {
        return scaleWeight;
    }

    public double getRawScaleWeight() {
        return rawScaleWeight;
    }

    public String getScaleUnit() {
        return unit;
    }

    public static class Builder {
        private String units;
        public double rawScaleWeight;
        private double scaleWeight;

        public Builder units(String units) {
            this.units = units;
            return this;
        }

        public Builder rawScaleWeight(double rawScaleWeight) {
            this.rawScaleWeight = rawScaleWeight;
            return this;
        }

        public Builder scaleWeight(double scaleWeight) {
            this.scaleWeight = scaleWeight;
            return this;
        }

        public ScaleMeasurement build() {
            return new ScaleMeasurement(this.scaleWeight, this.units, this.rawScaleWeight);
        }
    }
}
