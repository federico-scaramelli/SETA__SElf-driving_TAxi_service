package SETA;
import AdministrationServer.AddTaxiResponse;
import AdministrationServer.Statistics;
import SensorPackage.PM10Buffer;
import SensorPackage.PM10ReaderThread;
import SensorPackage.PM10Simulator;
import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.eclipse.paho.client.mqttv3.*;

import java.io.IOException;
import java.util.ArrayList;

public class TaxiProcess
{
    public static final String adminServerAddress = "http://localhost:9797/";
    private static final String addTaxiPath = "taxi/add";
    private static final String brokerAddress = "tcp://localhost:1883";
    public static final String topicBasePath = "seta/smartcity/rides/district";
    public static final int qos = 2;

    private static final Gson serializer = new Gson();

    public static void main(String[] argv) throws IOException
    {
        // Components
        TaxiData myData = new TaxiData();
        ArrayList<TaxiData> taxiList = null;
        Statistics localStatistics = null;

        //region ======= REST CONNECTION AND ADDING REQUEST TO THE SERVER =======

        // REST Client to communicate with the administration server
        Client client = Client.create();

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

        } catch (Exception e) {
            System.out.println(e.toString());
            System.exit(0);
        }
        //endregion

        //region ======= Statistics Threads =======
        localStatistics = new Statistics(myData);
        PM10Buffer pm10Buffer = new PM10Buffer();
        PM10Simulator pm10Sensor = new PM10Simulator(pm10Buffer);
        PM10ReaderThread pm10Reader = new PM10ReaderThread(localStatistics, pm10Buffer);
        TaxiLocalStatisticsThread statisticsThread = new TaxiLocalStatisticsThread(localStatistics);
        pm10Sensor.start();
        pm10Reader.start();
        statisticsThread.start();
        //endregion



        //region ======= RPC =======
        // RPC Server
        Server rpcServer = ServerBuilder.forPort(myData.port).addService(new TaxiImpl(myData, taxiList)).build();
        TaxiRpcServerThread rpcThread = new TaxiRpcServerThread(rpcServer);
        rpcServer.start();

        // RPC Client
        for (TaxiData t : taxiList)
        {
            if (t.getID() != myData.getID()) {
                TaxiRpcClientThread rpcClient = new TaxiRpcClientThread(myData, t);
                rpcClient.start();
            }
        }

        //endregion



        //region ======= MQTT BROKER CONNECTION =======
        String mqttClientID = MqttClient.generateClientId();
        MqttClient mqttClient = null;

        try {
            mqttClient = new MqttClient(brokerAddress, mqttClientID);
            // Request a persistent session
            MqttConnectOptions mqttOptions = new MqttConnectOptions();
            mqttOptions.setCleanSession(true);
            mqttClient.connect(mqttOptions);

            // === Rides thread ===
            TaxiRideThread taxiActions = new TaxiRideThread(myData, localStatistics, mqttClient);
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
                    taxiActions.myRide = rideRequest;
                    System.out.println("Received from MQTT broker: " + rideRequest);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    System.out.println("Message successfully delivered to the MQTT broker.");
                }
            });

            String topic = topicBasePath + GridHelper.getDistrict(myData.getPosition());
            mqttClient.subscribe(topic, qos);
            System.out.println("Successfully subscribed to the MQTT broker topic " + topic);
        } catch (Exception e) {
            System.out.println("ERROR! Impossible to connect to the MQTT broker.");
            System.out.println(e.toString());
            System.exit(0);
        }
        //endregion




        // === Input thread ===

        // === RPC thread ===

        System.in.read();
    }
}
