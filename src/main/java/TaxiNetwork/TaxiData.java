package TaxiNetwork;
import AdministrationServer.Statistics;

import java.util.ArrayList;
import java.util.Random;

public class TaxiData
{
    // === Connection data ===
    public static String adminServerAddress = "http://localhost:9797/";

    // Taxi uses ID numbers from 1 to 1000
    public int ID;
    public final String address = "localhost";
    // Taxi uses ports from 9798 to 65535
    public int port = -1;


    // === Working data ===
    public int batteryLevel = 100;
    public GridPosition currentPosition;


    // === Statistics ===
    Statistics localStatistics;


    // === Other data ===
    ArrayList<TaxiData> taxiList;



    // === Constructors === //
    // Custom ID constructor
    public TaxiData(Integer ID)
    {
        if ( ID < 1 || ID > 1000) {
            throw new RuntimeException("Taxi IDs must have a value between 1 and 1000");
        }
        this.ID = ID;
        port = new Random().nextInt(65535 - 9797) + 9797;
    }

    // Random ID constructor
    public TaxiData()  { this(new Random().nextInt(1000)); }


    // === Getters ===
    public int getID() {
        return ID;
    }
    public int getPort() { return port; }


    // === Utils ===
    // Print
    public String toString()
    {
        String s = "Taxi " + ID;
        if (port != -1)
            s += " available at http://" + address + ":" + port + "/";
        else
            s += " not connected to the network.";
        return s;
    }
}
