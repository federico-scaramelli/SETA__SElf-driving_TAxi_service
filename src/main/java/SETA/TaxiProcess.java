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
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

import java.io.IOException;
import java.util.ArrayList;

public class TaxiProcess
{
    public static final String adminServerAddress = "http://localhost:9797/";
    private static final String addTaxiPath = "taxi/add";
    private static final String brokerAddress = "tcp://localhost:1883";
    private static final String topicBasePath = "seta/smartcity/rides/district";

    private static final Gson serializer = new Gson();

    public static void main(String[] argv) throws IOException
    {
        // Components
        TaxiData myData = new TaxiData();
        ArrayList<TaxiData> taxiList;
        Statistics localStatistics;
        //TaxiActions myActions = new TaxiActions(myData);

        // ======= REST CONNECTION AND ADDING REQUEST TO THE SERVER =======

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



        // ======= MQTT BROKER CONNECTION =======
        String mqttClientID = MqttClient.generateClientId();
        MqttClient mqttClient;
        try {
            mqttClient = new MqttClient(brokerAddress, mqttClientID);
        } catch (Exception e) {
            System.out.println("ERROR! Impossible to create MQTT client instance.");
            System.out.println(e.toString());
            System.exit(0);
        }
        // Request a persistent session
        MqttConnectOptions mqttOptions = new MqttConnectOptions();
        mqttOptions.setCleanSession(true);



        // ======= EXECUTION OF SECONDARY THREADS =======

        // === Statistics Threads ===
        localStatistics = new Statistics(myData);
        PM10Buffer pm10Buffer = new PM10Buffer();
        Thread pm10Sensor = new Thread (new PM10Simulator(pm10Buffer));
        Thread pm10Reader = new Thread (new PM10ReaderThread(localStatistics, pm10Buffer));
        pm10Sensor.start();
        pm10Reader.start();

        TaxiLocalStatisticsThread statisticsThread = new TaxiLocalStatisticsThread(localStatistics);
        //myActions.SimulateRide();
        statisticsThread.run();
        System.in.read();
    }
}
