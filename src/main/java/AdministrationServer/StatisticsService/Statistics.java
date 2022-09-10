package AdministrationServer.StatisticsService;

import SETA.Taxi.TaxiData;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// Class to contain the local statistics of a taxi
public class Statistics
{
    public Statistics() {}

    private transient TaxiData taxiData;

    public int ID;

    public double traveledKm;
    public double batteryLevel;
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
        this.ID = taxiData.getID();
        this.batteryLevel = taxiData.getBatteryLevel();
    }

    public void addRideToStat(double kmIncrement, double newBattery)
    {
        synchronized (this) {
            addTraveledKm(kmIncrement);
            setBatteryLevel(newBattery);
            addAccomplishedRide();
        }
    }

    private void addTraveledKm(double adding)
    {
        /*System.out.println("Updating stats with calm... (10s)");
        try {
            Thread.sleep(10000);
        } catch (Exception e) {
        }*/
        traveledKm += adding;
    }

    private void setBatteryLevel(double level)
    {
        batteryLevel = level;
    }
    public void addPM10AverageValue(double avg)
    {
        synchronized (this) {
            pm10Averages.add(avg);
        }
        //System.out.println("Received new PM10 avg value: " + pm10Averages);
    }

    private void addAccomplishedRide() { accomplishedRides++; }

    public void setTimestamp() {
        // Not synchronized because I use it only on copies on the local stats and only in a single thread
        timestamp = System.currentTimeMillis();
    }

    public void resetData()
    {
        synchronized (this) {
            traveledKm = 0;
            batteryLevel = taxiData.getBatteryLevel();
            pm10Averages.clear();
            accomplishedRides = 0;
            timestamp = 0;
        }
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
