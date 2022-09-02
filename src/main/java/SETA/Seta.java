package SETA;
import com.google.gson.Gson;
import org.eclipse.paho.client.mqttv3.*;
import java.io.IOException;
import java.util.*;

/*
{"ID":9778,"startingPos":{"x":9,"y":1},"destinationPos":{"x":5,"y":6}}
*/

public class Seta
{
    static final String brokerAddress = "tcp://localhost:1883";
    static final String topicBasePath = "seta/smartcity/rides/district";
    static final int qos = 2;
    static final Gson serializer = new Gson();

    static long timeout = 5000;
    static ArrayList<LinkedList<RideRequest>> rideQueues = new ArrayList<LinkedList<RideRequest>>();
    static final ArrayList<Integer> completedRides = new ArrayList<>();

    public static void main(String[] argv) throws IOException, MqttException
    {
        for (int i = 0; i < 1; i++)
        {
            rideQueues.add(new LinkedList<RideRequest>());
            // Dispatch rides and receive confirmations
            SetaDispatchRidesThread dispatchRidesThread = new SetaDispatchRidesThread(rideQueues.get(i));
            dispatchRidesThread.start();
        }

        // Generate two ride requests every 5 seconds
        SetaGenerateRidesThread generateRidesThread = new SetaGenerateRidesThread();
        generateRidesThread.start();

        System.in.read();
    }
}