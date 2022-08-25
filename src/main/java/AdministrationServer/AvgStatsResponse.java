package AdministrationServer;

public class AvgStatsResponse
{
    public AvgStatsResponse() {}

    public double traveledKm;
    public double batteryLevel;
    public double pm10Averages;
    public int accomplishedRides;

    public AvgStatsResponse(double traveledKm, double batteryLevel, double pollutionLevel, int accomplishedRides)
    {
        this.traveledKm = traveledKm;
        this.batteryLevel = batteryLevel;
        this.pm10Averages = pollutionLevel;
        this.accomplishedRides = accomplishedRides;
    }

    public String toString()
    {
        return  "-> Traveled km avg: " + traveledKm +
                "\n-> Battery level avg: " + batteryLevel +
                "\n-> PM10 reads avg: " + pm10Averages +
                "\n-> Accomplished rides: " + accomplishedRides;
    }
}
