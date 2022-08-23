package AdministrationClient;

import AdministrationServer.MainServer;
import AdministrationServer.StatisticsService;
import TaxiNetwork.TaxiData;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import java.util.ArrayList;

public class AdministrationClient
{
    public static final String adminServerAddress = "http://localhost:9797/";
    public static final String getTaxiListPath = "statistics/get/taxi_list";

    private static final Gson serializer = new Gson();

    static Client client = Client.create();

    public static void main(String[] argv)
    {
        getTaxiList();
    }

    public static void getTaxiList()
    {
        System.out.println("Taxi list requested...");
        ArrayList<TaxiData> taxiList = getTaxiListRequest(client, adminServerAddress + getTaxiListPath);
        if (taxiList != null)
        {
            System.out.println("Taxi list received from the server:");
            System.out.println(taxiList.toString());
        }
    }

    public static ArrayList<TaxiData> getTaxiListRequest(Client client, String url)
    {
        try {
            WebResource webResource = client.resource(url);
            ClientResponse clientResponse = webResource.get(ClientResponse.class);

            if (clientResponse.getStatus() != 200)
            {
                System.out.println("Failed to get the taxi list from the server.\n" +
                        "HTTP Server response:\n" +
                        "--> Error code: " + clientResponse.getStatus() + "\n" +
                        "--> Info: " + clientResponse.getStatusInfo());
                return null;
            }

            ArrayList<TaxiData> taxiList = serializer.fromJson(
                                              clientResponse.getEntity(String.class),
                                              new TypeToken<ArrayList<TaxiData>>(){}.getType());
            return taxiList;

        } catch (ClientHandlerException e) {
            System.out.println("Server error: " + e);
            return null;
        }
    }
}
