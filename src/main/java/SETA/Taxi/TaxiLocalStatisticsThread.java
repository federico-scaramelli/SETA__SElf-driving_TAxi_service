package SETA.Taxi;

import AdministrationServer.StatisticsService.Statistics;
import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class TaxiLocalStatisticsThread extends Thread
{
    public static final String sendLocalStatsPath = "statistics/add";

    final Statistics originalStats;
    Statistics statsCopy;
    Client client = Client.create();

    private final Gson serializer = new Gson();

    public TaxiLocalStatisticsThread(Statistics originalStats)
    {
        this.originalStats = originalStats;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(5000);

                synchronized (originalStats)
                {
                    statsCopy = new Statistics(originalStats);
                    /*try{
                        System.out.println("5 sec di attesa mentre mi copio le stats");
                        Thread.sleep(5000);
                    }catch(Exception e){
                        e.printStackTrace();
                    }*/
                    originalStats.resetData();
                }

                statsCopy.setTimestamp();

                String serializedStats = serializer.toJson(statsCopy);
                WebResource webResource = client.resource(TaxiProcess.adminServerAddress + sendLocalStatsPath);
                ClientResponse clientResponse = webResource
                        .accept("application/json")
                        .type("application/json")
                        .post(ClientResponse.class, serializedStats);

                if (clientResponse.getStatus() != 200)
                {
                    throw new RuntimeException("Failed to add the local stats to the network.\n" +
                            "HTTP Server response:\n" +
                            "--> Error code: " + clientResponse.getStatus() + "\n" +
                            "--> Info: " + clientResponse.getStatusInfo());
                }
            } catch (Exception e) {
                e.toString();
            }
        }
    }
}
