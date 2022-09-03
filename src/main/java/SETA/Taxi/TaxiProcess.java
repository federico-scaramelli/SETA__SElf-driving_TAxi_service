package SETA.Taxi;
import AdministrationServer.TaxiService.AddTaxiResponse;
import AdministrationServer.StatisticsService.Statistics;
import SETA.RideRequest;
import SensorPackage.PM10Buffer;
import SensorPackage.PM10ReaderThread;
import SensorPackage.PM10Simulator;
import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.IOException;
import java.util.ArrayList;

public class TaxiProcess
{
    public static final String adminServerAddress = "http://localhost:9797/";
    private static final String addTaxiPath = "taxi/add";
    private static final String removeTaxiPath = "taxi/remove";
    private static final Gson serializer = new Gson();

    private static Client client = null;

    // Components
    private static ArrayList<TaxiData> taxiList = null;
    private static TaxiData myData = new TaxiData();
    private static Statistics localStatistics = null;

    public final static ArrayList<TaxiData> currentCompetitors = new ArrayList<>();
    private static TaxiRideThread taxiRideThread = null;
    public static RideRequest currentRideRequest = null;
    static final ArrayList<Integer> completedRides = new ArrayList<>();

    static TaxiMqttThread mqttThread;

    public static void main(String[] argv) throws IOException
    {
        //region ======= REST CONNECTION AND ADDING REQUEST TO THE SERVER =======

        // REST Client to communicate with the administration server
        client = Client.create();

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
            // Nobody can access myData at this point of the execution so synchronization is not needed
            myData.setPosition(addTaxiResponse.getStartingPosition());
            taxiList = addTaxiResponse.getTaxiList();

            System.out.println("Successfully connected to the Smart City.\n");
            System.out.println("Response received from the server:\n" + addTaxiResponse.toString());
            System.out.println("\nStarting data:\n" + myData.toString());

        } catch (Exception e) {
            System.out.println(e.toString());
            System.exit(0);
        }
        //endregion


        //region ======= Statistics Threads =======
        localStatistics = new Statistics(myData);
        PM10Buffer pm10BufferThread = new PM10Buffer();
        PM10Simulator pm10SensorThread = new PM10Simulator(pm10BufferThread);
        PM10ReaderThread pm10ReaderThread = new PM10ReaderThread(localStatistics, pm10BufferThread);
        TaxiLocalStatisticsThread statisticsThread = new TaxiLocalStatisticsThread(localStatistics);
        pm10SensorThread.start();
        pm10ReaderThread.start();
        statisticsThread.start();
        //endregion



        //region ======= RPC =======
        // RPC Server
        Server rpcServer = ServerBuilder.forPort(myData.port)
                .addService(new TaxiRpcServerImpl(myData, taxiList)).build();
        TaxiRpcServerThread rpcThread = new TaxiRpcServerThread(rpcServer);
        rpcThread.start();

        // Start RPC Client threads to send your data to all the taxis received from the server
        for (TaxiData t : taxiList)
        {
            if (t.getID() != myData.getID()) {
                TaxiRpcNewJoinThread rpcClient = new TaxiRpcNewJoinThread(myData, t);
                rpcClient.start();
            }
        }
        //endregion

        // === Input thread ===
        TaxiInputThread inputThread = new TaxiInputThread(myData);
        inputThread.start();
        System.out.println("\nInsert 'quit' to quit the Smart City.");

        //======= MQTT BROKER CONNECTION =======
        mqttThread = new TaxiMqttThread(myData, localStatistics);
        mqttThread.start();

        while (!myData.exited) {}

        // === Request to remove the taxi to the Smart City ===
        try {
            String serializedTaxiData = serializer.toJson(myData.getID());
            WebResource webResource = client.resource(adminServerAddress + removeTaxiPath);
            ClientResponse clientResponse = webResource
                    .accept("application/json")
                    .type("application/json")
                    .post(ClientResponse.class, serializedTaxiData);

            if (clientResponse.getStatus() != 200)
            {
                throw new RuntimeException("Failed to remove the taxi to the network.\n" +
                        "HTTP Server response:\n" +
                        "--> Error code: " + clientResponse.getStatus() + "\n" +
                        "--> Info: " + clientResponse.getStatusInfo());
            }

            System.out.println("Successfully remove from the Smart City.\n");
        } catch (Exception e) {
            System.out.println(e.toString());
            System.exit(0);
        }
    }

    public synchronized static void joinCompetition(RideRequest request)
    {
        currentRideRequest = request;

        synchronized (currentCompetitors) {
            currentCompetitors.clear();
            currentCompetitors.addAll(taxiList);
        }

        for (TaxiData t : taxiList)
        {
            TaxiRpcCompetitionThread competitionThread = new TaxiRpcCompetitionThread(myData, t, request);
            competitionThread.start();
        }
    }

    public static void takeRide(RideRequest request) throws MqttException
    {
        mqttThread.notifySetaRequestTaken(request);

        completedRides.add(request.ID);
        for (TaxiData t : taxiList)
        {
            if (t == myData) continue;
            TaxiRpcConfirmRideThread confirmationThread = new TaxiRpcConfirmRideThread(t, request);
            confirmationThread.start();
        }

        startRide(request);
    }

    public static void startRide(RideRequest request)
    {
        // === Rides thread ===
        taxiRideThread = new TaxiRideThread(myData, localStatistics, request);
        taxiRideThread.start();

        currentRideRequest = null;
    }

    public static void notifyQuit()
    {
        for (TaxiData t : taxiList)
        {
            TaxiRpcNotifyQuit notifyQuitThread = new TaxiRpcNotifyQuit(myData, t);
            notifyQuitThread.start();
        }
    }
}
