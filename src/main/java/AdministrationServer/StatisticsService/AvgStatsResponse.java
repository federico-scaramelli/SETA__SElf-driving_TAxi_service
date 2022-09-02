package AdministrationServer.StatisticsService;

import java.util.Objects;

public class AvgStatsResponse
{
    public double traveledKm;
    public double batteryLevel;
    public double pm10Averages;
    public int accomplishedRides;
    public String additionalMessage = "";

    public AvgStatsResponse(double traveledKm, double batteryLevel, double pollutionLevel, int accomplishedRides)
    {
        this.traveledKm = traveledKm;
        this.batteryLevel = batteryLevel;
        this.pm10Averages = pollutionLevel;
        this.accomplishedRides = accomplishedRides;
    }

    public void setAdditionalMessage(String msg)
    {
        additionalMessage += msg;
    }

    public String toString()
    {
        String s = "";

        if (!Objects.equals(additionalMessage, ""))
            s = "WARNING: " + additionalMessage + "\n";

        s += "-> Traveled km avg: " + traveledKm +
           "\n-> Battery level avg: " + batteryLevel +
           "\n-> PM10 reads avg: " + pm10Averages +
           "\n-> Accomplished rides: " + accomplishedRides;

        return s;
    }
}
