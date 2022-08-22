package AdministrationServer;

import TaxiNetwork.Taxi;

import java.util.ArrayList;
import java.util.HashMap;

public class StatisticsManager
{
    // Singleton
    private static StatisticsManager instance;
    public synchronized static StatisticsManager getInstance() {
        if (instance == null) {
            instance = new StatisticsManager();
        }
        return instance;
    }

    // List of local statistics related to taxi's IDs
    HashMap<Integer, ArrayList<Statistics>> localStatsList;

    // Taxi list String
    public String getTaxiListString(HashMap<Integer, Taxi> taxiList)
    {
        if (taxiList.size() == 0)
            return "Currently there are no taxi connected to the Smart City.";

        String taxiListString = "List of taxis currently located in the Smart City:\n";
        for (Integer key : taxiList.keySet()) {
            taxiListString += "-> " + taxiList.get(key).toString() + "\n";
        }
        return taxiListString;
    }

    // Returns a LocalStatistics object containing the average of the last 'n' local statistics of a taxi with ID 'id'
    public Statistics getAverageLocalStats (int id, int n)
    {
        if (!localStatsList.containsKey(id))
        {
            System.out.println("No local statistics available for taxi " + id);
            return null;
        }
        float avgTraveledKm = 0,
                avgBatteryLevel = 0,
                avgPollutionLevel = 0;
        int accomplishedRides = 0;
        ArrayList<Statistics> localStats = localStatsList.get(id);
        for (int i = localStats.size() - 1; i >= localStats.size() - n; i--) {
            avgTraveledKm += localStats.get(i).traveledKm;
            avgBatteryLevel += localStats.get(i).batteryLevel;
            avgPollutionLevel += localStats.get(i).pollutionLevel;
            accomplishedRides += localStats.get(i).accomplishedRides;
        }
        avgTraveledKm /= n;
        avgBatteryLevel /= n;
        avgPollutionLevel /= n;

        return new Statistics(avgTraveledKm, avgBatteryLevel, avgPollutionLevel, accomplishedRides);
    }

    public Statistics getAverageGlobalStats (long t1, long t2)
    {
        ArrayList<ArrayList<Statistics>> allTaxiesStats = new ArrayList<>();
        for (int key : localStatsList.keySet())
            allTaxiesStats.add(localStatsList.get(key));

        if (allTaxiesStats.isEmpty())
        {
            System.out.println("No local statistics available.");
            return null;
        }

        float avgTraveledKm = 0,
                avgBatteryLevel = 0,
                avgPollutionLevel = 0;
        int accomplishedRides = 0,
                count = 0;

        // For each list of stats for each taxi that has sent at least one
        for (ArrayList<Statistics> localStatsList : allTaxiesStats)
        {
            for (Statistics localStats :  localStatsList)
            {
                if (localStats.timestamp >= t1 && localStats.timestamp <= t2)
                {
                    avgTraveledKm += localStats.traveledKm;
                    avgBatteryLevel += localStats.batteryLevel;
                    avgPollutionLevel += localStats.pollutionLevel;
                    accomplishedRides += localStats.accomplishedRides;
                    count ++;
                }
            }
        }
        avgTraveledKm /= count;
        avgBatteryLevel /= count;
        avgPollutionLevel /= count;

        return new Statistics(avgTraveledKm, avgBatteryLevel, avgPollutionLevel, accomplishedRides);
    }
}
