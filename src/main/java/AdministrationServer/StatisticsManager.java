package AdministrationServer;

import TaxiNetwork.TaxiData;

import java.util.ArrayList;
import java.util.HashMap;

public class StatisticsManager
{
    // Singleton
    private static StatisticsManager instance;

    // List of local statistics related to taxi's IDs
    private final HashMap<Integer, ArrayList<Statistics>> localStatsList;

    private StatisticsManager()
    {
        localStatsList = new HashMap<Integer, ArrayList<Statistics>>();
    }

    public synchronized static StatisticsManager getInstance() {
        if (instance == null) {
            instance = new StatisticsManager();
        }
        return instance;
    }

    public synchronized void AddLocalStatistics(Statistics localStats)
    {
        if (localStatsList.containsKey(localStats.ID)) {
            localStatsList.get(localStats.ID).add(localStats);
        } else {
            localStatsList.computeIfAbsent(localStats.ID, s -> new ArrayList<>()).add(localStats);
        }
    }

    // Taxi list String
    public String getTaxiListString(ArrayList<TaxiData> taxiList)
    {
        if (taxiList.size() == 0)
            return "Currently there are no taxi connected to the Smart City.";

        StringBuilder taxiListString = new StringBuilder("List of taxis currently located in the Smart City:\n");
        for (TaxiData taxiData : taxiList)
        {
            taxiListString.append("-> ").append(taxiData.toString()).append("\n");
        }
        return taxiListString.toString();
    }

    // Returns a StatisticsAveragePacket object containing the average
    // of the last 'n' local statistics of a taxi with ID 'id'
    public synchronized AvgStatsResponse getAverageLocalStats (int id, int n)
    {
        if (!localStatsList.containsKey(id))
        {
            System.out.println("No local statistics available for taxi " + id);
            return null;
        }
        double avgTraveledKm = 0;
        double avgBatteryLevel = 0;
        double avgPollutionLevel = 0;
        int accomplishedRides = 0;
        ArrayList<Statistics> localStats = localStatsList.get(id);

        for (int i = localStats.size() - 1; i >= localStats.size() - n; i--) {
            double tempPM10Averages = 0.0;
            avgTraveledKm += localStats.get(i).traveledKm;
            avgBatteryLevel += localStats.get(i).batteryLevel;
            for (double pm10Avg : localStats.get(i).pm10Averages) {
                tempPM10Averages += pm10Avg;
            }
            tempPM10Averages /= localStats.get(i).pm10Averages.size();
            avgPollutionLevel += tempPM10Averages;
            accomplishedRides += localStats.get(i).accomplishedRides;
        }
        avgTraveledKm /= n;
        avgBatteryLevel /= n;
        avgPollutionLevel /= n;

        return new AvgStatsResponse(avgTraveledKm, avgBatteryLevel, avgPollutionLevel, accomplishedRides);
    }

    public synchronized int getLocalStatsCount(int id)
    {
        if (!localStatsList.containsKey(id))
            return 0;
        return localStatsList.get(id).size();
    }

    // Returns a StatisticsAveragePacket object containing the average
    // of all the local statistics received with timestamps between t1 and t2
    public AvgStatsResponse getAverageGlobalStats (long t1, long t2)
    {
        ArrayList<ArrayList<Statistics>> allTaxiesStats = new ArrayList<>();
        for (int key : localStatsList.keySet())
            allTaxiesStats.add(localStatsList.get(key));

        if (allTaxiesStats.isEmpty())
        {
            System.out.println("No local statistics available.");
            return null;
        }

        double avgTraveledKm = 0;
        double avgBatteryLevel = 0;
        double avgPollutionLevel = 0;
        int accomplishedRides = 0;
        int count = 0;

        // For each list of stats for each taxi that has sent at least one
        for (ArrayList<Statistics> localStatsList : allTaxiesStats)
        {
            for (Statistics localStats :  localStatsList)
            {
                if (localStats.timestamp >= t1 && localStats.timestamp <= t2)
                {
                    avgTraveledKm += localStats.traveledKm;
                    avgBatteryLevel += localStats.batteryLevel;
                    for (double pm10Avg : localStats.pm10Averages) {
                        avgPollutionLevel += pm10Avg;
                    }
                    accomplishedRides += localStats.accomplishedRides;
                    count ++;
                }
            }
        }
        avgTraveledKm /= count;
        avgBatteryLevel /= count;
        avgPollutionLevel /= count;

        return new AvgStatsResponse(avgTraveledKm, avgBatteryLevel, avgPollutionLevel, accomplishedRides);
    }
}
