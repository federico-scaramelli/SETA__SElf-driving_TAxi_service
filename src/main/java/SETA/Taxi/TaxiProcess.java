package SETA.Taxi;
import AdministrationServer.TaxiService.AddTaxiResponse;
import AdministrationServer.StatisticsService.Statistics;
import SETA.RideRequest;
import SensorPackage.PM10Buffer;
import SensorPackage.PM10ReaderThread;
import SensorPackage.PM10Simulator;
import Utils.GridHelper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.eclipse.paho.client.mqttv3.MqttException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class TaxiProcess
{
    // Connection
    public static final String adminServerAddress = "http://localhost:9797/";
    private static final String addTaxiPath = "taxi/add";
    private static final String removeTaxiPath = "taxi/remove";
    private static final String getTaxiListPath = "statistics/get/taxi_list";
    private static final Gson serializer = new Gson();
    private static Client client = null;
    static TaxiMqttThread mqttThread;
    static TaxiRpcServerThread rpcThread;

    // Taxi Network
    private static ArrayList<TaxiData> taxiList = null;

    // Components
    private static TaxiData myData = new TaxiData();
    private static Statistics localStatistics = null;

    // Rides data container - Used as lock to synchronize and to organize data regarding rides
    private static final TaxiRidesData myRidesData = new TaxiRidesData();

    // Charging data container - Used as lock to synchronize and to organize data regarding charging operations
    private static final TaxiChargingData myChargingData = new TaxiChargingData();


    public static void main(String[] argv) throws InterruptedException
    {
        //region ======= REST CONNECTION + JOIN REQUEST TO THE SERVER =======

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

            // Nobody can access these data at this point of the execution so synchronization is not needed
            myData.setPosition(addTaxiResponse.getStartingPosition());
            taxiList = addTaxiResponse.getTaxiList();
            myRidesData.completedRides.addAll(addTaxiResponse.getCompletedRides());

            System.out.println("Successfully connected to the Smart City.\n");
            System.out.println("Response received from the server:\n" + addTaxiResponse.toString());
            System.out.println("\nStarting data:\n" + myData.toString());
            System.out.println("My Logical Clock offset: " + myChargingData.logicalClockOffset);

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
        pm10SensorThread.start(); // Producer
        pm10ReaderThread.start(); // Consumer
        statisticsThread.start(); // Sender
        //endregion



        //region ======= RPC =======
        // RPC Server
        Server rpcServer = ServerBuilder.forPort(myData.port)
                .addService(new TaxiRpcServerImpl(myData, myRidesData, myChargingData, taxiList)).build();
        rpcThread = new TaxiRpcServerThread(rpcServer);
        rpcThread.start();

        // Start RPC threads to send your data to all the taxis received from the server in the TaxiList
        ArrayList<TaxiRpcNewJoinThread> threads = new ArrayList<>();
        for (TaxiData t : taxiList)
        {
            if (t.getID() != myData.getID()) {
                TaxiRpcNewJoinThread newJoinThread = new TaxiRpcNewJoinThread(myData, myChargingData, t);
                threads.add(newJoinThread);
                newJoinThread.start();
            }
        }
        // Wait until all the taxis receive your joining notification
        for (TaxiRpcNewJoinThread thread : threads) {
            thread.join();
        }
        System.out.println("All the taxis received the notification about your joining!");
        //endregion

        // === Input thread ===
        TaxiInputThread inputThread = new TaxiInputThread(myData, myRidesData, myChargingData, taxiList);
        inputThread.start();
        System.out.println("\nInsert 'quit' to quit the Smart City.\nInsert 'recharge' to request a recharge.");

        // === MQTT BROKER CONNECTION ===
        mqttThread = new TaxiMqttThread(myData, myRidesData, myChargingData, localStatistics);
        mqttThread.start();

        // === Close operations ===
        // Wait until the quit thread has finished
        inputThread.join();
        if (!myData.isQuitting) return;

        // === Request to remove the taxi from the Smart City ===
        try {
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

    // Called to start a competition about a specific ride request.
    public synchronized static void joinCompetition(RideRequest request)
    {
        synchronized (myRidesData)
        {
            // Competitors setup
            myRidesData.rideCompetitors.clear();
            myRidesData.rideCompetitors.addAll(taxiList);
            myRidesData.competitorsCounter = myRidesData.rideCompetitors.size();
            // Competition state update - now I'm in a competition
            myRidesData.competitionState = TaxiRidesData.RideCompetitionState.Pending;

            // Broadcast RPC to compete for the ride
            for (TaxiData t : myRidesData.rideCompetitors)
            {
                TaxiRpcCompetitionThread competitionThread =
                        new TaxiRpcCompetitionThread(myData, myRidesData, myChargingData, t, request);
                competitionThread.start();
            }
        }
    }

    // Called to take a ride when all the positive ACK about a competition are received
    public static void takeRide(RideRequest request) throws MqttException
    {
        // Notify SETA about this ride successfully taken
        mqttThread.notifySetaRequestTaken(request);

        synchronized (myRidesData) {
            // Add the ride to the completed rides list
            myRidesData.completedRides.add(request.ID);

            //Notify all the taxis about the completed ride
            for (TaxiData otherTaxi : taxiList) {
                // Do not notify myself
                if (otherTaxi == myData) continue;

                TaxiRpcConfirmRideThread confirmationThread = new TaxiRpcConfirmRideThread(otherTaxi, request);
                confirmationThread.start();
            }

            // Now I'm riding.
            myRidesData.isRiding = true;
        }

        startRide(request);
    }

    public static void startRide(RideRequest request)
    {
        // === Thread to actually execute the ride ===
        myRidesData.taxiRideThread = new TaxiRideThread(myData, myRidesData, myChargingData, localStatistics, request);
        myRidesData.taxiRideThread.start();
    }

    // Go to the recharge station and try to take it. If there is a queue, wait in the queue.
    public synchronized static void startChargingProcess()
    {
        synchronized (myChargingData) {
            // Now I'm waiting for a recharge.
            myChargingData.currentRechargeRequest =
                    new TaxiChargingRequest(myData.getID(), myData.getPort(), myChargingData.logicalClock);
            System.out.println("\nRecharging process started...");

            // Go to the charging station
            synchronized (myData) {
                GridCell stationCell = GridHelper.getRechargeStation(myData.getPosition());
                double distance = GridHelper.getDistance(myData.getPosition(), stationCell);
                System.out.println("Going to the recharge station " +
                        "[From " + myData.getPosition() + " to " + stationCell);
                myData.setPosition(stationCell);
                myData.reduceBattery(distance);
                System.out.println("Arrived at the recharge station in " + myData.getPosition());
            }


            // Update logical clock since you are sending a messages
            myChargingData.logicalClock += myChargingData.logicalClockOffset;
            System.out.println("Logical clock value: " + myChargingData.logicalClock);

            // Broadcast RPC request to take the recharge station
            myChargingData.chargingCompetitors.clear();
            for (TaxiData t : taxiList) {
                myChargingData.chargingCompetitors.put(t.ID, t);
            }
            myChargingData.chargingCompetitors.put(myData.getID(), myData);

            for (HashMap.Entry<Integer, TaxiData> entry : myChargingData.chargingCompetitors.entrySet()) {
                TaxiData taxi = entry.getValue();
                TaxiRpcRequestChargingThread chargingThread =
                        new TaxiRpcRequestChargingThread(myData, myChargingData, taxi);
                chargingThread.start();
            }
        }
    }

    // Remove a taxi from the list
    public static void removeTaxiFromList(TaxiData taxi)
    {
        synchronized (taxiList) {
            if (!taxiList.contains(taxi)) {
                System.out.println("You tried to remove taxi " + taxi.ID + " but it's not in the list!");
                return;
            }
            taxiList.remove(taxi);
        }
    }

    // Request an updated copy of the taxi list to the REST server.
    // Called in the case a taxi not actually present in the network remains on the local list.
    public static void updateTaxiListAskingRestServer()
    {
        try {
            WebResource webResource = client.resource(adminServerAddress + getTaxiListPath);
            ClientResponse clientResponse = webResource.get(ClientResponse.class);

            if (clientResponse.getStatus() != 200)
            {
                System.out.println("Failed to get the taxi list from the server.\n" +
                        "HTTP Server response:\n" +
                        "--> Error code: " + clientResponse.getStatus() + "\n" +
                        "--> Info: " + clientResponse.getStatusInfo());
            }
            ArrayList<TaxiData> newList = serializer.fromJson(
                    clientResponse.getEntity(String.class),
                    new TypeToken<ArrayList<TaxiData>>(){}.getType());

            synchronized (taxiList) {
                taxiList.clear();
                taxiList.addAll(newList);
            }

            System.out.println("Taxi list updated asking the REST server.");

        } catch (ClientHandlerException e) {
            System.out.println("Server error: " + e);
            e.getCause();
            e.printStackTrace();
        }
    }
}
