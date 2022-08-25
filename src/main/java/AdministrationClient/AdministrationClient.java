package AdministrationClient;

import AdministrationServer.AvgStatsResponse;
import TaxiNetwork.TaxiData;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.Scanner;

public class AdministrationClient
{
    public static final String adminServerAddress = "http://localhost:9797/";
    public static final String getTaxiListPath = "statistics/get/taxi_list";
    public static final String getLastLocalAvgStatsPath = "statistics/get/avg";

    private static final Gson serializer = new Gson();

    static Client client = Client.create();

    public static void main(String[] argv)
    {
        System.out.println("\n\nSETA - Administration Interface");
        showMenu();
    }

    private static void showMenu() {
        Scanner scan = new Scanner(System.in);
        System.out.println("\nSelect a query: ");
        System.out.println("1 - Taxi list");
        System.out.println("2 - Average of the last N PM10 reads from a specific taxi");
        System.out.println("3 - Average of the PM10 sensors reads occurred on a period");
        System.out.println("4 - Exit");
        try {
            int choice = scan.nextInt();
            switch (choice){
                case 1:
                    getTaxiList();
                    showMenu();
                    return;
                case 2:
                    System.out.println("Select N: ");
                    int n = scan.nextInt();
                    System.out.println("Select ID: ");
                    int id = scan.nextInt();
                    getLastLocalAvgStats(n, id);
                    showMenu();
                    return;
                case 3:
                    System.out.println("AVG 2");
                    showMenu();
                    return;
                case 4:
                    System.out.println("Shutting down...");
                    scan.close();
                    return;
                default:
                    System.out.println("ERROR! Invalid choice.");
                    showMenu();
                    return;
            }
        } catch (InputMismatchException e) {
            System.out.println("ERROR! Invalid choice.");
            showMenu();
        }
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

    public static void getLastLocalAvgStats(int n, int id)
    {
        System.out.println("Requested the average of the last " + n +
                            " local statistics from the taxi " + id + "...");
        AvgStatsResponse avgStats = getLastLocalAvgStatsRequest(client, adminServerAddress +
                                                getLastLocalAvgStatsPath +
                                                "/last_" + n + "_from_" + id);
        if (avgStats != null)
        {
            System.out.println("Last local statistics average received from the server: ");
            System.out.println(avgStats);
        }
    }

    public static AvgStatsResponse getLastLocalAvgStatsRequest(Client client, String url)
    {
        try {
            WebResource webResource = client.resource(url);
            ClientResponse clientResponse = webResource.get(ClientResponse.class);

            if (clientResponse.getStatus() != 200)
            {
                System.out.println("Failed to get the local average statistics packet from the server.\n" +
                        "HTTP Server response:\n" +
                        "--> Error code: " + clientResponse.getStatus() + "\n" +
                        "--> Info: " + clientResponse.getStatusInfo());
                return null;
            }

            AvgStatsResponse avgResponse = serializer.fromJson(
                                            clientResponse.getEntity(String.class),
                                            AvgStatsResponse.class);
            return avgResponse;

        } catch (ClientHandlerException e) {
            System.out.println("Server error: " + e);
            return null;
        }
    }
}
