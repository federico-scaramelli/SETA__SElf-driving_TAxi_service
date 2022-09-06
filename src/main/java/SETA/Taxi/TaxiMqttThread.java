package SETA.Taxi;

import AdministrationServer.StatisticsService.Statistics;
import Utils.GridHelper;
import SETA.RideRequest;
import com.google.gson.Gson;
import org.eclipse.paho.client.mqttv3.*;

public class TaxiMqttThread extends Thread
{
    private static final String brokerAddress = "tcp://localhost:1883";
    private static final Gson serializer = new Gson();
    public static final String topicBasePath = "seta/smartcity/rides/district";
    public static final int qos = 2;

    public TaxiData myData;
    public Statistics localStatistics;
    static MqttClient mqttClient = null;
    static String topic = null;

    public TaxiMqttThread(TaxiData myData, Statistics localStatistics)
    {
        this.myData = myData;
        this.localStatistics = localStatistics;
    }

    @Override
    public void run()
    {
        String mqttClientID = MqttClient.generateClientId();

        try {
            mqttClient = new MqttClient(brokerAddress, mqttClientID, null);
            // Request a persistent session
            mqttClient.connect();

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("\nConnection with the broker lost.");
                    System.out.println("--> Cause: " + cause.toString());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String msg = new String(message.getPayload());
                    RideRequest rideRequest = serializer.fromJson(msg, RideRequest.class);
                    handleRideRequestReceiving(rideRequest);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Actually not working because I don't send ACK
                    System.out.println("Message successfully delivered to the MQTT broker.");
                }
            });

            topic = topicBasePath + GridHelper.getDistrict(myData.getPosition());
            mqttClient.subscribe(topic, qos);
            System.out.println("\nSuccessfully subscribed to the MQTT broker topic " + topic);
        } catch (Exception e) {
            System.out.println("\nERROR! Impossible to connect to the MQTT broker.");
            System.out.println(e.toString());
            System.exit(0);
        }
    }

    private void handleRideRequestReceiving(RideRequest rideRequest)
    {
        System.out.println("\nReceived from MQTT broker: " + rideRequest);
        // Ignore the ride request if you are not available
        synchronized (TaxiProcess.completedRides) {
            if (myData.isRiding                                                 // Riding
                    || myData.isExiting                                         // Exiting
                    || myData.getBatteryLevel() <= 30                           // Charging
                    || TaxiProcess.completedRides.contains(rideRequest.ID))     // Already taken
            {
                System.out.println("Request " + rideRequest + " ignored.");
            } else {
                TaxiProcess.joinCompetition(rideRequest);
            }
        }
    }

    public void notifySetaRequestTaken(RideRequest request) throws MqttException
    {
        String requestJson = serializer.toJson(request);
        MqttMessage message = new MqttMessage(requestJson.getBytes());
        message.setQos(qos);
        System.out.println("\nNotifying SETA about ride request " + request.ID + " taken in charge.");
        mqttClient.publish(topic + "/confirmations", message);
    }

    public static void changeTopic(int district) throws MqttException {
        mqttClient.unsubscribe(topic);
        System.out.println("Changing district to " + district);
        topic = topicBasePath + district;
        mqttClient.subscribe(topic, TaxiMqttThread.qos);
    }
}
