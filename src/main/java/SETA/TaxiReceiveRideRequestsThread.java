package SETA;

import AdministrationServer.Statistics;
import com.google.gson.Gson;
import org.eclipse.paho.client.mqttv3.*;

public class TaxiReceiveRideRequestsThread extends Thread
{
    private static final String brokerAddress = "tcp://localhost:1883";
    private static final Gson serializer = new Gson();
    public static final String topicBasePath = "seta/smartcity/rides/district";
    public static final int qos = 2;

    public TaxiData myData;
    public Statistics localStatistics;
    TaxiRideThread taxiActions = null;
    static MqttClient mqttClient = null;
    static String topic = null;

    public TaxiReceiveRideRequestsThread(TaxiData myData, Statistics localStatistics)
    {
        this.myData = myData;
        this.localStatistics = localStatistics;
    }

    @Override
    public void run()
    {
        String mqttClientID = MqttClient.generateClientId();

        try {
            mqttClient = new MqttClient(brokerAddress, mqttClientID);
            // Request a persistent session
            MqttConnectOptions mqttOptions = new MqttConnectOptions();
            mqttOptions.setCleanSession(true);
            mqttClient.connect(mqttOptions);

            // === Rides thread ===
            taxiActions = new TaxiRideThread(myData, localStatistics, mqttClient);
            taxiActions.start();

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("Connection with the broker lost.");
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
                    System.out.println("Message successfully delivered to the MQTT broker.");
                }
            });

            topic = topicBasePath + GridHelper.getDistrict(myData.getPosition());
            mqttClient.subscribe(topic, qos);
            System.out.println("Successfully subscribed to the MQTT broker topic " + topic);
        } catch (Exception e) {
            System.out.println("ERROR! Impossible to connect to the MQTT broker.");
            System.out.println(e.toString());
            System.exit(0);
        }
    }

    private void handleRideRequestReceiving(RideRequest rideRequest)
    {
        System.out.println("Received from MQTT broker: " + rideRequest);
        TaxiProcess.startCompetition(rideRequest);


        //taxiActions.myRide = rideRequest;
    }

    public static void changeDistrict(int district) throws MqttException {
        mqttClient.unsubscribe(topic);
        System.out.println("Changing district to " + district);
        topic = topicBasePath + district;
        mqttClient.subscribe(topic, TaxiReceiveRideRequestsThread.qos);
    }
}
