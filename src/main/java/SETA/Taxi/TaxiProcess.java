package SETA.Taxi;
import AdministrationServer.TaxiService.AddTaxiResponse;
import AdministrationServer.StatisticsService.Statistics;
import SETA.RideRequest;
import SensorPackage.PM10Buffer;
import SensorPackage.PM10ReaderThread;
import SensorPackage.PM10Simulator;
import Utils.GridHelper;
import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Random;

public class TaxiProcess
{
    // Connection
    public static final String adminServerAddress = "http://localhost:9797/";
    private static final String addTaxiPath = "taxi/add";
    private static final String removeTaxiPath = "taxi/remove";
    private static final Gson serializer = new Gson();
    private static Client client = null;
    static TaxiMqttThread mqttThread;

    // Network
    private static ArrayList<TaxiData> taxiList = null;

    // Components
    private static TaxiData myData = new TaxiData();
    private static Statistics localStatistics = null;

    // Rides
    private static TaxiRideThread taxiRideThread = null;
    public static RideRequest currentRideRequest = null;
    static final ArrayList<Integer> completedRides = new ArrayList<>();
    public final static ArrayList<TaxiData> currentCompetitors = new ArrayList<>();

    // Charging
    static final Integer logicalClockOffset = new Random().nextInt(100);
    static Integer logicalClock = 0;
    public final static HashMap<Integer, TaxiData> chargingRequestReceivers = new HashMap<>();
    public final static PriorityQueue<TaxiChargingRequest> chargingQueue = new PriorityQueue<TaxiChargingRequest>();


    public static void main(String[] argv) throws IOException, InterruptedException
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
            System.out.println("Logical Clock offset: " + logicalClockOffset);

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
        TaxiQuitThread quitThread = new TaxiQuitThread(myData, taxiList);
        quitThread.start();
        System.out.println("\nInsert 'quit' to quit the Smart City.");

        //======= MQTT BROKER CONNECTION =======
        mqttThread = new TaxiMqttThread(myData, localStatistics);
        mqttThread.start();

        // Wait until the quit thread has done
        quitThread.join();
        //while (!myData.exited) {}

        // === Request to remove the taxi from the Smart City ===
        try {
            String serializedTaxiData = serializer.toJson(myData.getID());
            WebResource webResource = client.resource(adminServerAddress + removeTaxiPath + "/" + myData.ID);
            ClientResponse clientResponse = webResource.delete(ClientResponse.class);

            if (clientResponse.getStatus() != 200)
            {
                throw new RuntimeException("Failed to remove the taxi from the network.\n" +
                        "HTTP Server response:\n" +
                        "--> Error code: " + clientResponse.getStatus() + "\n" +
                        "--> Info: " + clientResponse.getStatusInfo());
            }

            System.out.println("Successfully removed from the Smart City.\n");
            System.exit(0);
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

        for (TaxiData t : currentCompetitors)
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

    public static void startChargingProcess()
    {
        myData.queuedForCharging = true;
        System.out.println("\nRecharging process started...");

        // Go to the charging station
        GridCell stationCell = GridHelper.getRechargeStation(myData.getPosition());
        double distance = GridHelper.getDistance(myData.getPosition(), stationCell);
        myData.setPosition(stationCell);
        myData.reduceBattery(distance);
        System.out.println("\nArrived at recharge station.");

        // Update logical clock
        logicalClock += logicalClockOffset;
        System.out.println("\nLogical clock value: " + logicalClock);

        // Broadcast request
        synchronized (chargingRequestReceivers) {
            chargingRequestReceivers.clear();
            for (TaxiData t : taxiList) {
                chargingRequestReceivers.put(t.ID, t);
            }
            chargingRequestReceivers.put(myData.getID(), myData);
        }

        for(HashMap.Entry<Integer, TaxiData> entry : chargingRequestReceivers.entrySet()) {
            TaxiData taxi = entry.getValue();
            TaxiRpcRequestChargingThread chargingThread = new TaxiRpcRequestChargingThread(myData, taxi);
            chargingThread.start();
        }
    }
}
