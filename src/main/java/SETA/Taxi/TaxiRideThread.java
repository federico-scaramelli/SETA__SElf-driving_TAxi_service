package SETA.Taxi;

import AdministrationServer.StatisticsService.Statistics;
import Utils.GridHelper;
import SETA.RideRequest;
import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.eclipse.paho.client.mqttv3.MqttException;

public class TaxiRideThread extends Thread
{
    public static final String updateTaxiDataPath = "taxi/update";
    public static final String sendLocalStatsPath = "statistics/add";

    private final Gson serializer = new Gson();
    Client client = Client.create();

    private final TaxiData myData;
    private final TaxiRidesData myRidesData;
    private final TaxiChargingData myChargingData;
    private final Statistics myLocalStats;
    volatile public RideRequest rideToExecute = null;

    public TaxiRideThread(TaxiData myTaxi, TaxiRidesData myRidesData, TaxiChargingData myChargingData,
                          Statistics myLocalStats, RideRequest ride)
    {
        this.myData = myTaxi;
        this.myRidesData = myRidesData;
        this.myChargingData = myChargingData;
        this.myLocalStats = myLocalStats;
        this.rideToExecute = ride;
    }

    @Override
    public void run()
    {
        System.out.println("\nDriving thread started. Executing ride...");
        try {
            Thread.sleep(8000);
        } catch (Exception e) {
            e.printStackTrace();
        }

        int myDistrict;
        synchronized (myData.currentPosition) {
            myDistrict = GridHelper.getDistrict(myData.getPosition());
            myData.setPosition(rideToExecute.destinationPos);
        }

        if (GridHelper.getDistrict(rideToExecute.destinationPos) != myDistrict) {
            // Change topic
            try {
                myDistrict = GridHelper.getDistrict(rideToExecute.destinationPos);
                TaxiMqttThread.changeTopic(myDistrict);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }

        updateLocalData();
        sendUpdateToRestServer();

        System.out.println("Arrived at destination " + rideToExecute.destinationPos + " with " +
                myData.getBatteryLevel() + " of battery remained.");

        synchronized (myRidesData) {
            myRidesData.isRiding = false;
        }

        // If I received the recharge command, recharge
        synchronized (myChargingData) {
            if (myChargingData.chargeCommandReceived) {
                TaxiProcess.startChargingProcess();
                return;
            }
        }

        // If I'm quitting, send local stats
        synchronized (myData.isQuitting) {
            if (myData.isQuitting) {
                // Send statistics to REST server
                sendLocalStatsToRestServer();
                return;
            }
        }

        // If my battery is too low, recharge
        synchronized (myData.batteryLevel) {
            if (myData.getBatteryLevel() < 30) {
                TaxiProcess.startChargingProcess();
            }
        }
    }

    private void updateLocalData()
    {
        System.out.println("Updating my local data.");
        double totalTraveledKm = rideToExecute.getKm() + GridHelper.getDistance(myData.getPosition(),
                                 rideToExecute.startingPos);

        synchronized (myData.batteryLevel) {
            // Update my TaxiData
            myData.reduceBattery(totalTraveledKm);

            synchronized (myLocalStats) {
                // Update local stats
                myLocalStats.addRideToStat(totalTraveledKm, myData.getBatteryLevel());
            }
        }
    }

    private void sendUpdateToRestServer()
    {
        System.out.println("Sending updated data to the rest server.");

        String serializedStats = serializer.toJson(myData);
        WebResource webResource = client.resource(TaxiProcess.adminServerAddress + updateTaxiDataPath);
        ClientResponse clientResponse = webResource
                .accept("application/json")
                .type("application/json")
                .post(ClientResponse.class, serializedStats);

        if (clientResponse.getStatus() != 200)
        {
            throw new RuntimeException("Failed to update the taxi data on the server.\n" +
                    "HTTP Server response:\n" +
                    "--> Error code: " + clientResponse.getStatus() + "\n" +
                    "--> Info: " + clientResponse.getStatusInfo());
        }
    }

    private void sendLocalStatsToRestServer()
    {
        System.out.println("Sending local stats to the rest server before quitting.");

        Statistics statsCopy;
        synchronized (myLocalStats)
        {
            statsCopy = new Statistics(myLocalStats);
            /*try{
                System.out.println("5 sec di attesa mentre mi copio le stats");
                Thread.sleep(5000);
            }catch(Exception e){
                e.printStackTrace();
            }*/
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
    }
}
