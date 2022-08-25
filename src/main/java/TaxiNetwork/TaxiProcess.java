package TaxiNetwork;
import AdministrationServer.AddTaxiResponse;
import SensorPackage.PM10Buffer;
import SensorPackage.PM10ReaderThread;
import SensorPackage.PM10Simulator;
import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import java.io.IOException;

public class TaxiProcess
{
    public static final String adminServerAddress = "http://localhost:9797/";
    public static final String addTaxiPath = "taxi/add";

    private static final Gson serializer = new Gson();

    public static void main(String[] argv) throws IOException
    {
        // Components
        TaxiData myData = new TaxiData();
        TaxiActions myActions = new TaxiActions(myData);


        // REST Client to communicate with the administration server
        Client client = Client.create();

        // === Connect to the network adding the taxi to the Smart City ===
        try {
            String serializedTaxiData = serializer.toJson(myData);
            WebResource webResource = client.resource(adminServerAddress + addTaxiPath);
            ClientResponse clientResponse = webResource
                    .accept("application/json")
                    .type("application/json")
                    .post(ClientResponse.class, serializedTaxiData);

            if (clientResponse.getStatus() != 200)
            {
                throw new RuntimeException("Failed to add the taxi to the network.\n" +
                        "HTTP Server response:\n" +
                        "--> Error code: " + clientResponse.getStatus() + "\n" +
                        "--> Info: " + clientResponse.getStatusInfo());
            }

            AddTaxiResponse addTaxiResponse = serializer.fromJson(
                                                clientResponse.getEntity(String.class),
                                                AddTaxiResponse.class);
            myData.setPosition(addTaxiResponse.getPosition());
            myData.setTaxiList(addTaxiResponse.getTaxiList());

            System.out.println("Successfully connected to the Smart City.\n");
            System.out.println("Response received from the server:\n" + addTaxiResponse.toString());
            System.out.println("\nStarting data:\n" + myData.toString());

        } catch (Exception e) {
            System.out.println(e.toString());
            System.exit(0);
        }

        // === Statistics Threads ===
        PM10Buffer pm10Buffer = new PM10Buffer();
        Thread pm10Sensor = new Thread (new PM10Simulator(pm10Buffer));
        Thread pm10Reader = new Thread (new PM10ReaderThread(myData.getLocalStatistics(), pm10Buffer));
        pm10Sensor.start();
        pm10Reader.start();

        System.in.read();

        /*TaxiLocalStatisticsThread statisticsThread = new TaxiLocalStatisticsThread(myData);
        myActions.SimulateRide();
        statisticsThread.run();
        System.in.read();*/
    }
}
