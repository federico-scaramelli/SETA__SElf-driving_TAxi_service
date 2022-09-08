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

public class TaxiProcess
{
    // Connection
    public static final String adminServerAddress = "http://localhost:9797/";
    private static final String addTaxiPath = "taxi/add";
    private static final String removeTaxiPath = "taxi/remove";
    private static final Gson serializer = new Gson();
    private static Client client = null;
    static TaxiMqttThread mqttThread;
    static TaxiRpcServerThread rpcThread;

    // Network
    private static ArrayList<TaxiData> taxiList = null;

    // Components
    private static TaxiData myData = new TaxiData();
    private static Statistics localStatistics = null;

    // Rides data container - Used as lock to synchronize and to organize the data into subclasses
    private static final TaxiRidesData myRidesData = new TaxiRidesData();

    // Charging data container - Used as lock to synchronize and to organize the data into subclasses
    private static final TaxiChargingData myChargingData = new TaxiChargingData();


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
            System.out.println("Logical Clock offset: " + myChargingData.logicalClockOffset);

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
                .addService(new TaxiRpcServerImpl(myData, myRidesData, myChargingData, taxiList)).build();
        rpcThread = new TaxiRpcServerThread(rpcServer);
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
        TaxiInputThread quitThread = new TaxiInputThread(myData, myRidesData, myChargingData, taxiList);
        quitThread.start();
        System.out.println("\nInsert 'quit' to quit the Smart City.");

        //======= MQTT BROKER CONNECTION =======
        mqttThread = new TaxiMqttThread(myData, myRidesData, myChargingData, localStatistics);
        mqttThread.start();

        // Wait until the quit thread has done
        quitThread.join();
        if (!myData.isQuitting) return;
        //while (!myData.exited) {}

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

    public synchronized static void joinCompetition(RideRequest request)
    {
        synchronized (myRidesData)
        {
            myRidesData.currentRideRequest = request;
            myRidesData.rideCompetitors.clear();
            myRidesData.rideCompetitors.addAll(taxiList);
            myRidesData.competitorsCounter = myRidesData.rideCompetitors.size();
            myRidesData.competitionState = TaxiRidesData.RideCompetitionState.Pending;

            for (TaxiData t : myRidesData.rideCompetitors)
            {
                TaxiRpcCompetitionThread competitionThread =
                        new TaxiRpcCompetitionThread(myData, myRidesData, myChargingData, t, request);
                competitionThread.start();
            }
        }
    }

    public static void takeRide(RideRequest request) throws MqttException
    {
        mqttThread.notifySetaRequestTaken(request);

        synchronized (myRidesData) {
            // Add the ride to the completed rides list
            myRidesData.completedRides.add(request.ID);
            myRidesData.competitionState = TaxiRidesData.RideCompetitionState.Idle;

            //Notify all the taxis about the completed ride
            for (TaxiData otherTaxi : taxiList) {
                // Do not notify myself
                if (otherTaxi == myData) continue;

                TaxiRpcConfirmRideThread confirmationThread = new TaxiRpcConfirmRideThread(otherTaxi, request);
                confirmationThread.start();
            }
        }

        startRide(request);
    }

    public static void startRide(RideRequest request)
    {
        // === Thread to actually execute the ride ===
        myRidesData.taxiRideThread = new TaxiRideThread(myData, myRidesData, myChargingData, localStatistics, request);
        myRidesData.taxiRideThread.start();
        synchronized (myRidesData.currentRideRequest) {
            myRidesData.currentRideRequest = null;
        }
    }

    public static void startChargingProcess()
    {
        myChargingData.currentRechargeRequest =
                new TaxiChargingRequest(myData.getID(), myData.getPort(), myChargingData.logicalClock);
        System.out.println("\nRecharging process started...");

        // Go to the charging station
        GridCell stationCell = GridHelper.getRechargeStation(myData.getPosition());
        double distance = GridHelper.getDistance(myData.getPosition(), stationCell);
        myData.setPosition(stationCell);
        myData.reduceBattery(distance);
        System.out.println("Arrived at recharge station.");

        // Update logical clock since you are sending a messages
        synchronized (myChargingData.logicalClock) {
            myChargingData.logicalClock += myChargingData.logicalClockOffset;
        }
        System.out.println("Logical clock value: " + myChargingData.logicalClock);

        // Broadcast request
        synchronized (myChargingData.chargingCompetitors) {
            myChargingData.chargingCompetitors.clear();
            for (TaxiData t : taxiList) {
                myChargingData.chargingCompetitors.put(t.ID, t);
            }
            myChargingData.chargingCompetitors.put(myData.getID(), myData);
        }

        for(HashMap.Entry<Integer, TaxiData> entry : myChargingData.chargingCompetitors.entrySet()) {
            TaxiData taxi = entry.getValue();
            TaxiRpcRequestChargingThread chargingThread =
                    new TaxiRpcRequestChargingThread(myData, myChargingData, taxi);
            chargingThread.start();
        }
    }
}
