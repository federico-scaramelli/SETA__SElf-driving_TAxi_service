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

    static long timeout = 2000;
    // Array of queues of ride requests, one for each district
    static ArrayList<LinkedList<RideRequest>> rideQueues = new ArrayList<LinkedList<RideRequest>>();
    static final ArrayList<Integer> completedRides = new ArrayList<>();


    static int debugCount = 50;

    public static void main(String[] argv) throws IOException, MqttException
    {
        for (int i = 0; i < 4; i++)
        {
            rideQueues.add(new LinkedList<RideRequest>());
            // Thread to dispatch rides and receive confirmations. One for each district.
            SetaDispatchRidesThread dispatchRidesThread = new SetaDispatchRidesThread(rideQueues.get(i));
            dispatchRidesThread.start();
        }

        // Thread to generate two ride requests every 5 seconds
        SetaGenerateRidesThread generateRidesThread = new SetaGenerateRidesThread();
        generateRidesThread.start();

        System.in.read();
    }
}