package SETA;

import com.google.gson.Gson;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.IOException;

public class Seta
{
    private static final String brokerAddress = "tcp://localhost:1883";
    private static final String topicBasePath = "seta/smartcity/rides/district";
    private static final int qos = 2;

    private static final Gson serializer = new Gson();

    public static void main(String[] argv) throws IOException
    {
        String mqttClientID = MqttClient.generateClientId();
        MqttClient mqttClient;
        try {
            mqttClient = new MqttClient(brokerAddress, mqttClientID);

            // Request a persistent session
            MqttConnectOptions mqttOptions = new MqttConnectOptions();
            mqttOptions.setCleanSession(true);

            // Connect the client
            System.out.println(mqttClientID + " connecting to broker " + brokerAddress);
            mqttClient.connect(mqttOptions);
            System.out.println(mqttClientID + " connected.");

            //Generate two ride requests every 5 seconds
            while(true)
            {
                for (int i = 0; i < 2; i++) {
                    RideRequest request = new RideRequest();
                    int district = GridHelper.getDistrict(request.startingPos);
                    String requestJson = serializer.toJson(request);
                    MqttMessage message = new MqttMessage(requestJson.getBytes());
                    message.setQos(qos);
                    System.out.println("Publishing request to the broker:\n" +
                            "--> Topic: " + (topicBasePath + district) +
                            "\n--> " + request);
                    mqttClient.publish(topicBasePath + district, message);
                    System.out.println("Message published.");
                }

                Thread.sleep(5000);
            }

        } catch (MqttException me ) {
            System.out.println("ERROR! Impossible to create MQTT client instance.");
            System.out.println("-> Reason: " + me.getReasonCode());
            System.out.println("-> Msg: " + me.getMessage());
            System.out.println("-> Loc: " + me.getLocalizedMessage());
            System.out.println("-> Cause: " + me.getCause());
            System.out.println("-> Excep: " + me);
            me.printStackTrace();
            System.exit(0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
