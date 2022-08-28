package SETA;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;

public class TaxiActions extends Thread
{
    TaxiData myData;
    volatile public RideRequest myRide = null;
    MqttClient mqttClient;

    public TaxiActions(TaxiData myTaxi,  MqttClient mqttClient)
    {
        this.myData = myTaxi;
        this.mqttClient = mqttClient;
    }

    @Override
    public void run() {
        while(true)
        {
            if (myRide == null) continue;

            System.out.println("Driving thread started. Executing ride...");
            myData.setRidingState(true);
            try {
                Thread.sleep(5000);
            } catch (Exception e) {
                e.printStackTrace();
            }

            int myDistrict = GridHelper.getDistrict(myData.getPosition());
            System.out.println("Arrived at destination " + myRide.destinationPos);
            myData.setPosition(myRide.destinationPos);
            if (GridHelper.getDistrict(myRide.destinationPos) != myDistrict) {
                // Change topic
                try {
                    mqttClient.unsubscribe(TaxiProcess.topicBasePath + myDistrict);
                    myDistrict = GridHelper.getDistrict(myRide.destinationPos);
                    System.out.println("Changing district to " + myDistrict);
                    mqttClient.subscribe(TaxiProcess.topicBasePath + myDistrict, TaxiProcess.qos);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }

            double kmToReachStartingPos = GridHelper.getDistance(myData.getPosition(), myRide.startingPos);
            double batteryReduction = myRide.getKm() + kmToReachStartingPos;
            myData.reduceBattery(batteryReduction);
            myData.setRidingState(false);
            myRide = null;
            System.out.println(myData);
        }
    }
}
