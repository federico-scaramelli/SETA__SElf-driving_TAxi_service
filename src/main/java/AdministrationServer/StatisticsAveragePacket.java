package AdministrationServer;

import java.util.List;

public class StatisticsAveragePacket
{
    public StatisticsAveragePacket() {}

    public double traveledKm;
    public double batteryLevel;
    public double pm10Averages;
    public int accomplishedRides;

    public StatisticsAveragePacket(double traveledKm, double batteryLevel, double pollutionLevel, int accomplishedRides)
    {
        this.traveledKm = traveledKm;
        this.batteryLevel = batteryLevel;
        this.pm10Averages = pollutionLevel;
        this.accomplishedRides = accomplishedRides;
    }
}
