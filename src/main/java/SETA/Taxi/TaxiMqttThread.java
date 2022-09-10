package SETA.Taxi;

import AdministrationServer.StatisticsService.Statistics;
import Utils.GridHelper;
import SETA.RideRequest;
import com.google.gson.Gson;
import org.eclipse.paho.client.mqttv3.*;


// Thread to handle the communication with the broker
public class TaxiMqttThread extends Thread
{
    private final String brokerAddress = "tcp://localhost:1883";
    private final Gson serializer = new Gson();
    public static final String topicBasePath = "seta/smartcity/rides/district";
    public static final int qos = 2;
    static MqttClient mqttClient = null;
    static String topic = null;


    final public TaxiData myData;
    final TaxiRidesData myRidesData;
    final TaxiChargingData myChargingData;
    final public Statistics localStatistics;

    public TaxiMqttThread(TaxiData myData, TaxiRidesData myRidesData, TaxiChargingData myChargingData, Statistics localStatistics)
    {
        this.myData = myData;
        this.myRidesData = myRidesData;
        this.myChargingData = myChargingData;
        this.localStatistics = localStatistics;
    }

    @Override
    public void run()
    {
        String mqttClientID = MqttClient.generateClientId();

        try {
            mqttClient = new MqttClient(brokerAddress, mqttClientID, null);
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
                public void deliveryComplete(IMqttDeliveryToken token) { }
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
        //System.out.println("\nReceived from MQTT broker: " + rideRequest);

        synchronized (myRidesData)
        {
            // Ignore any requests if you are competing
            if (myRidesData.competitionState == TaxiRidesData.RideCompetitionState.Pending) {
                System.out.println("Request " + rideRequest + " ignored because I'm into a competition.");
                return;
            }

            // Ignore any request if you are running or if the request has been already taken
            if (myRidesData.isRiding || myRidesData.completedRides.contains(rideRequest.ID)) {
                /*System.out.println("Request " + rideRequest + " ignored because " +
                                    "I'm running or it has been already taken.");*/
                return;
            }
        }

        synchronized (myData.isQuitting)
        {
            // Ignore any request if you are quitting the city
            if (myData.isQuitting)
            {
                //System.out.println("Request " + rideRequest + " ignored because I'm quitting the Smart City.");
                return;
            }
        }

        synchronized (myChargingData)
        {
            // Ignore any request if you are charging or you received the command to request a recharge
            if (myChargingData.isCharging || myChargingData.chargeCommandReceived)
            {
                //System.out.println("Request " + rideRequest + " ignored because I'm charging or I have to do it.");
                return;
            }
        }

        // Otherwise join the competition about the received request
        TaxiProcess.joinCompetition(rideRequest);
    }

    // Notify the broker about a request taken in charge by you
    public void notifySetaRequestTaken(RideRequest request) throws MqttException
    {
        String requestJson = serializer.toJson(request);
        MqttMessage message = new MqttMessage(requestJson.getBytes());
        message.setQos(qos);
        System.out.println("\nNotifying SETA about ride request " + request.ID + " taken in charge.");
        mqttClient.publish(topic + "/confirmations", message);
    }

    // Chane the topic since the district is changed
    public static void changeTopic(int district) throws MqttException {
        mqttClient.unsubscribe(topic);
        System.out.println("Changing district to " + district);
        topic = topicBasePath + district;
        mqttClient.subscribe(topic, TaxiMqttThread.qos);
    }
}
