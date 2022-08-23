package AdministrationServer;

import TaxiNetwork.TaxiData;

import java.text.DateFormat;
import java.util.Date;

public class Statistics
{
    public Statistics() {}

    private transient TaxiData taxiData;

    public int ID;

    public float traveledKm;
    public float batteryLevel;
    public float pollutionLevel;
    public int accomplishedRides;

    public long timestamp;

    public Statistics(float traveledKm, float batteryLevel, float pollutionLevel, int accomplishedRides)
    {
        this.traveledKm = traveledKm;
        this.batteryLevel = batteryLevel;
        this.pollutionLevel = pollutionLevel;
        this.accomplishedRides = accomplishedRides;

        timestamp = System.currentTimeMillis();
    }

    public Statistics(Statistics statistics)
    {
        this.ID = statistics.ID;
        this.traveledKm = statistics.traveledKm;
        this.batteryLevel = statistics.batteryLevel;
        this.pollutionLevel = statistics.pollutionLevel;
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
    public void addPollutionLevel(float adding) { pollutionLevel += adding; }
    public void addAccomplishedRide() { accomplishedRides++; }
    public void setTimestamp() { timestamp = System.currentTimeMillis(); }

    public void resetData()
    {
        traveledKm = 0;
        batteryLevel = taxiData.batteryLevel;
        pollutionLevel = 0;
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
                "-> Pollution level: " + pollutionLevel + "\n" +
                "-> Accomplished rides: " + accomplishedRides;
    }
}
