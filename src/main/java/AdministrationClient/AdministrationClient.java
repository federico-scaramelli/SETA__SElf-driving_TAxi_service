package AdministrationClient;

import AdministrationServer.AvgStatsResponse;
import TaxiNetwork.TaxiData;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import java.text.DateFormat;
import java.time.*;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoField;
import java.util.*;

public class AdministrationClient
{
    public static final String adminServerAddress = "http://localhost:9797/";
    public static final String getTaxiListPath = "statistics/get/taxi_list";
    public static final String queryStatisticsPath = "statistics/get/avg";

    private static final Gson serializer = new Gson();
    private static final Locale defaultFormattingLocale = Locale.ITALY;
    private static final DateTimeFormatter timeFormatter
                        = DateTimeFormatter.ISO_LOCAL_TIME;

    static Client client = Client.create();
    static Scanner scan = new Scanner(System.in);

    public static void main(String[] argv)
    {
        System.out.println("\n\nSETA - Administration Interface");
        showMainMenu();
    }

    private static void showMainMenu() {
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
                    showMainMenu();
                    return;
                case 2:
                    showGetLastLocalAvgStatsMenu();
                    showMainMenu();
                    return;
                case 3:
                    showGetGlobalAvgStatsMenu();
                    showMainMenu();
                    return;
                case 4:
                    System.out.println("Shutting down...");
                    scan.close();
                    return;
                default:
                    System.out.println("ERROR! Invalid choice.");
                    showMainMenu();
                    return;
            }
        } catch (InputMismatchException e) {
            System.out.println("ERROR! Invalid choice.");
            showMainMenu();
        }
    }

    private static void showGetLastLocalAvgStatsMenu()
    {
        System.out.println("Select N: ");
        int n = scan.nextInt();
        System.out.println("Select ID: ");
        int id = scan.nextInt();
        getLastLocalAvgStats(n, id);
    }

    private static void showGetGlobalAvgStatsMenu()
    {
        long t1 = 0;
        long t2 = 0;

        System.out.println("Enter t1 in the following format: HH:mm[:ss.ns]\n" +
                "Note: [optional values]");
        Scanner inputScanner = new Scanner(System.in);
        String timeString = inputScanner.nextLine();
        try {
            LocalTime inputTime = LocalTime.parse(timeString, timeFormatter);
            System.out.println("Time entered was " + inputTime);
            LocalDateTime time = LocalDateTime.of (LocalDate.now(), inputTime);
            t1 = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            System.out.println("Invalid time: " + timeString);
            return;
        }

        System.out.println("Enter t2 in the following format: HH:mm[:ss.ns]\n" +
                "Note: [optional values]");
        timeString = inputScanner.nextLine();
        try {
            LocalTime inputTime = LocalTime.parse(timeString, timeFormatter);
            System.out.println("Time entered was " + inputTime);
            LocalDateTime time = LocalDateTime.of (LocalDate.now(), inputTime);
            t2 = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            System.out.println("Invalid time: " + timeString);
            return;
        }
        /*System.out.println(t1);
        System.out.println(t2);*/
        getGlobalAvgStats(t1, t2);
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
                queryStatisticsPath + "/last_" + n + "_from_" + id);
        if (avgStats != null)
        {
            System.out.println("Response received from the server: ");
            System.out.println(avgStats);
        }
    }

    public static AvgStatsResponse getLastLocalAvgStatsRequest(Client client, String url)
    {
        try {
            WebResource webResource = client.resource(url);
            ClientResponse clientResponse = webResource.get(ClientResponse.class);

            if (clientResponse.getStatus() == 204)
            {
                System.out.println("ERROR! No local statistics available for the requested taxi.");
                return null;
            }

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

    public static void getGlobalAvgStats(long t1, long t2)
    {
        System.out.println("Requested the average of global statistics between " +
                            DateFormat.getTimeInstance().format(new Date(t1)) +
                            " and " + DateFormat.getTimeInstance().format(new Date(t2)));
        AvgStatsResponse avgStats = getLastLocalAvgStatsRequest(client, adminServerAddress +
                queryStatisticsPath + "/global_from_" + t1 + "_to_" + t2);
        if (avgStats != null)
        {
            System.out.println("Response received from the server: ");
            System.out.println(avgStats);
        }
    }

    public static AvgStatsResponse getGlobalAvgStatsRequest(Client client, String url)
    {
        try {
            WebResource webResource = client.resource(url);
            ClientResponse clientResponse = webResource.get(ClientResponse.class);

            if (clientResponse.getStatus() == 204)
            {
                System.out.println("ERROR! No statistics registered on the requested period.");
                return null;
            }

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
