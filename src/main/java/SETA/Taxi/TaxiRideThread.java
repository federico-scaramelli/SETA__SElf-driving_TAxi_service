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
    private final Statistics myLocalStats;
    volatile public RideRequest myRide = null;

    public TaxiRideThread(TaxiData myTaxi, Statistics myLocalStats, RideRequest ride)
    {
        this.myData = myTaxi;
        this.myLocalStats = myLocalStats;
        this.myRide = ride;
    }

    @Override
    public void run()
    {
        System.out.println("\nDriving thread started. Executing ride...");
        try {
            //Thread.sleep(15000);
        } catch (Exception e) {
            e.printStackTrace();
        }

        int myDistrict = GridHelper.getDistrict(myData.getPosition());
        myData.setPosition(myRide.destinationPos);

        if (GridHelper.getDistrict(myRide.destinationPos) != myDistrict) {
            // Change topic
            try {
                myDistrict = GridHelper.getDistrict(myRide.destinationPos);
                TaxiMqttThread.changeTopic(myDistrict);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }

        updateData();
        sendUpdateToRestServer();

        System.out.println("Arrived at destination " + myRide.destinationPos + " with " +
                myData.getBatteryLevel() + " of battery remained.");

        if (myData.isExiting) {
            // Send statistics to REST server
            sendLocalStatsToRestServer();
        }

        myData.setRidingState(false);

        if (myData.getBatteryLevel() < 30)
        {
            TaxiProcess.startChargingProcess();
        }
    }

    private void updateData()
    {
        double totalTraveledKm = myRide.getKm() + GridHelper.getDistance(myData.getPosition(), myRide.startingPos);

        // Update my TaxiData
        myData.reduceBattery(totalTraveledKm);

        // Update local stats
        myLocalStats.addRideToStat(totalTraveledKm, myData.getBatteryLevel());
    }

    private void sendUpdateToRestServer()
    {
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
