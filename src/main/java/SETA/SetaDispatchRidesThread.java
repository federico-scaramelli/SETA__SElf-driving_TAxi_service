package SETA;

import Utils.GridHelper;
import com.google.gson.Gson;
import org.eclipse.paho.client.mqttv3.*;

import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import static SETA.Seta.*;

public class SetaDispatchRidesThread extends Thread {

    public SetaDispatchRidesThread(LinkedList<RideRequest> rideQueue) throws MqttException
    {
        String mqttClientID = MqttClient.generateClientId();
        mqttClient = new MqttClient(brokerAddress, mqttClientID);

        // Request a persistent session
        MqttConnectOptions mqttOptions = new MqttConnectOptions();
        mqttOptions.setCleanSession(true);

        // Connect the client
        System.out.println(mqttClientID + " connecting to broker " + brokerAddress);
        mqttClient.connect(mqttOptions);
        System.out.println(mqttClientID + " connected.");

        this.rideQueue = rideQueue;
    }

    final int qos = 2;
    final Gson serializer = new Gson();
    final MqttClient mqttClient;
    Timer timerRepeatRequestDispatch = new Timer();
    TimerTask timerTask;
    final LinkedList<RideRequest> rideQueue;
    RideRequest dispatchedRide = null;


    @Override
    public void run()
    {
        setupMqtt();

        while (true)
        {
            synchronized (rideQueue)
            {
                // If I already have dispatched a ride, or I don't have any ride to dispatch
                while (dispatchedRide != null || rideQueue.size() == 0)
                {
                    try {
                        //System.out.println("--> Waiting for a ride in the queue...");
                        rideQueue.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                System.out.println("--> Ride poll!");
                dispatchedRide = rideQueue.poll();

                timerTask = new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            publishRideRequest();
                        } catch (MqttException e) {
                            throw new RuntimeException(e);
                        }
                    }
                };
                timerRepeatRequestDispatch.schedule(timerTask, 0, Seta.timeout);

                //System.out.println("--> Notify!");
                rideQueue.notify();
            }
        }
    }

    private void publishRideRequest() throws MqttException
    {
        String requestJson = serializer.toJson(dispatchedRide);
        MqttMessage message = new MqttMessage(requestJson.getBytes());
        message.setQos(qos);
        System.out.println("Publishing request to the broker:\n" +
                "--> " + dispatchedRide);
        int district = GridHelper.getDistrict(dispatchedRide.startingPos);
        mqttClient.publish(topicBasePath + district, message);
        System.out.println("Message published.");
    }

    private void setupMqtt()
    {
        try {
            // Subscribe to all the district topics to receive messages from taxis
            mqttClient.subscribe(topicBasePath + 1 + "/confirmations");
            mqttClient.subscribe(topicBasePath + 2 + "/confirmations");
            mqttClient.subscribe(topicBasePath + 3 + "/confirmations");
            mqttClient.subscribe(topicBasePath + 4 + "/confirmations");

            // Set callback for receiving messages
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("\nConnection with the broker lost.");
                    System.out.println("--> Cause: " + cause.toString());
                    cause.printStackTrace();
                }

                @Override
                public void messageArrived(String topic, MqttMessage message)
                {
                    String msg = new String(message.getPayload());
                    RideRequest request = serializer.fromJson(msg, RideRequest.class);

                    if (dispatchedRide == null || request.ID != dispatchedRide.ID) return;

                    int district = Integer.parseInt(topic.substring(
                                                    topic.lastIndexOf('/') - 1,
                                                    topic.lastIndexOf('/')));

                    dispatchedRide = null;
                    timerTask.cancel();
                    timerTask = null;
                    timerRepeatRequestDispatch.cancel();
                    timerRepeatRequestDispatch = new Timer();

                    System.out.println("Ride " + request.ID + " confirmed from district " + district);
                    synchronized (rideQueue) {
                        rideQueue.notify();
                    }
                    synchronized (Seta.completedRides) {
                        Seta.completedRides.add(request.ID);
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    System.out.println("Message successfully delivered to the MQTT broker.");
                }
            });
        }
        catch (MqttSecurityException e) {
            throw new RuntimeException(e);
        } catch (MqttException me ) {
            System.out.println("ERROR! Impossible to create MQTT client instance.");
            System.out.println("-> Reason: " + me.getReasonCode());
            System.out.println("-> Msg: " + me.getMessage());
            System.out.println("-> Loc: " + me.getLocalizedMessage());
            System.out.println("-> Cause: " + me.getCause());
            System.out.println("-> Exception: " + me);
            me.printStackTrace();
            System.exit(0);
        }
    }
}
