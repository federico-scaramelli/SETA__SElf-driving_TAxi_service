package SETA;

import Utils.GridHelper;
import com.google.gson.Gson;
import org.eclipse.paho.client.mqttv3.*;

import java.util.LinkedList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Function;
import java.util.stream.Collectors;

import static SETA.Seta.*;

public class SetaDispatchRidesThread extends Thread {

    public SetaDispatchRidesThread(LinkedList<RideRequest> rideQueue) throws MqttException
    {
        String mqttClientID = MqttClient.generateClientId();
        mqttClient = new MqttClient(brokerAddress, mqttClientID, null);

        // Connect the client
        System.out.println(mqttClientID + " connecting to broker " + brokerAddress);
        mqttClient.connect();
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


    //int debugCountTotal = Seta.debugCount;


    @Override
    public void run()
    {
        setupMqtt();

        // Continuously look for new requests to dispatch
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

                // Repeat the requests until it's confirmed
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
        synchronized (completedRides)
        {
            if (completedRides.contains(dispatchedRide))
                return;
        }

        String requestJson = serializer.toJson(dispatchedRide);
        MqttMessage message = new MqttMessage(requestJson.getBytes());
        message.setQos(qos);
        System.out.println("Publishing request to the broker:\n" +
                "--> " + dispatchedRide);// +
                //"\nCompleted: " + completedRides.size());
        int district = GridHelper.getDistrict(dispatchedRide.startingPos);
        mqttClient.publish(topicBasePath + district, message);
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

                // Receive confirmation messages
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

                        //=================debug
                        /*if (completedRides.size() >= debugCountTotal * 2) {
                            System.out.println("=================LISTS===============================================");
                            System.out.println("DISTRICT 1: " + rideQueues.get(0));
                            System.out.println("DISTRICT 2: " + rideQueues.get(1));
                            System.out.println("DISTRICT 3: " + rideQueues.get(2));
                            System.out.println("DISTRICT 4: " + rideQueues.get(3));
                            System.out.println("=================COMPLETED===============================================");
                            System.out.println(completedRides.size());
                            System.out.println(completedRides);

                            System.out.println("=========================DUPLICATES======================");
                            // Get frequencies of each element
                            Map<Integer, Long> frequencies = completedRides.stream()
                                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
                            // then filter only the inputs which have frequency greater than 1
                            frequencies.entrySet().stream()
                                    .filter(entry -> entry.getValue() > 1)
                                    .forEach(entry -> System.out.println(entry.getKey()));
                        }*/
                        //==========================
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    //System.out.println("Message successfully delivered to the MQTT broker.");
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
