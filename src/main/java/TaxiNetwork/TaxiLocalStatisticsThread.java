package TaxiNetwork;

import AdministrationServer.Statistics;
import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class TaxiLocalStatisticsThread implements Runnable
{
    public static final String sendLocalStatsPath = "statistics/add";

    Statistics originalStats;
    Statistics statsCopy;
    Client client = Client.create();

    private final Gson serializer = new Gson();

    public TaxiLocalStatisticsThread(Statistics originalStats)
    {
        this.originalStats = originalStats;
    }

    public void run() {
        while (true) {
            try {
                Thread.sleep(2000);

                //SYNC
                statsCopy = new Statistics(originalStats);
                originalStats.resetData();
                //END SYNC

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

                statsCopy.resetData();
            } catch (Exception e) {
                e.toString();
            }
        }
    }
}
