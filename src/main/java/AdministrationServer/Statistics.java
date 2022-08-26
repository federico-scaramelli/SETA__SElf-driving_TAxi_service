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
        this.pm10Averages = new ArrayList<>(statistics.pm10Averages);
        this.accomplishedRides = statistics.accomplishedRides;
        this.timestamp = statistics.timestamp;
    }

    public Statistics(TaxiData taxiData)
    {
        this.taxiData = taxiData;
        this.ID = taxiData.ID;
        this.batteryLevel = taxiData.batteryLevel;
    }

    public synchronized void addTraveledKm(float adding) { traveledKm += adding; }
    public synchronized void setBatteryLevel(float level) { batteryLevel += level; }
    public synchronized void addPM10AverageValue(double avg)
    {
        pm10Averages.add(avg);
        System.out.println("Received new PM10 avg value: " + pm10Averages);
    }
    public synchronized void addAccomplishedRide() { accomplishedRides++; }
    public synchronized void setTimestamp() { timestamp = System.currentTimeMillis(); }

    public synchronized void resetData()
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
