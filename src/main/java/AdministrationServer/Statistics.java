package AdministrationServer;

public class Statistics
{
    public float traveledKm;
    public float batteryLevel;
    public float pollutionLevel;
    public int accomplishedRides;

    public long timestamp;

    Statistics(float traveledKm, float batteryLevel, float pollutionLevel, int accomplishedRides)
    {
        this.traveledKm = traveledKm;
        this.batteryLevel = batteryLevel;
        this.pollutionLevel = pollutionLevel;
        this.accomplishedRides = accomplishedRides;

        timestamp = System.currentTimeMillis();
    }

}
