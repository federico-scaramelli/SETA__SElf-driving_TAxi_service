package AdministrationServer;

import TaxiNetwork.TaxiData;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Statistics
{
    public Statistics() {}

    private transient TaxiData taxiData;

    public int ID;

    public float traveledKm;
    public float batteryLevel;
    public List<Double> pm10Averages = new ArrayList<>();
    public int accomplishedRides;

    public long timestamp;

    public Statistics(Statistics statistics)
    {
        this.ID = statistics.ID;
        this.traveledKm = statistics.traveledKm;
        this.batteryLevel = statistics.batteryLevel;
        this.pm10Averages = statistics.pm10Averages;
        this.accomplishedRides = statistics.accomplishedRides;
        this.timestamp = statistics.timestamp;
    }

    public Statistics(TaxiData taxiData)
    {
        this.taxiData = taxiData;
        this.ID = taxiData.ID;
    }

    public void addTraveledKm(float adding) { traveledKm += adding; }
    public void setBatteryLevel(float level) { batteryLevel += level; }
    public void addPM10AverageValue(double avg)
    {
        pm10Averages.add(avg);
        System.out.println("Received new PM10 avg value: " + pm10Averages);
    }
    public void addAccomplishedRide() { accomplishedRides++; }
    public void setTimestamp() { timestamp = System.currentTimeMillis(); }

    public void resetData()
    {
        traveledKm = 0;
        batteryLevel = taxiData.batteryLevel;
        pm10Averages.clear();
        accomplishedRides = 0;
        timestamp = 0;
    }

    public String toString()
    {
        return "Local statistics of Taxi " + ID +
                " at timestamp " +  DateFormat.getTimeInstance().format(new Date(timestamp)) +
                " containing:\n" +
                "-> Traveled km: " + traveledKm + "\n" +
                "-> Battery level: " + batteryLevel + "\n" +
                "-> Pollution level: " + pm10Averages + "\n" +
                "-> Accomplished rides: " + accomplishedRides;
    }
}
