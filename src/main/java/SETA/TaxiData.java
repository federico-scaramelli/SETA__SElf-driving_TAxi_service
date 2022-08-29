package SETA;

import java.text.DecimalFormat;
import java.util.Random;

public class TaxiData
{
    // === Connection data ===

    // Taxi uses ID numbers from 1 to 1000
    public final int ID;
    public final String address = "localhost";
    // Taxi uses ports from 9798 to 65535
    public final int port;

    // === Working data ===
    public double batteryLevel = 100;
    public GridCell currentPosition;
    public boolean isRiding = false;

    // === Constructors === //
    // Custom ID and port constructor
    public TaxiData(int ID, int port)
    {
        if ( ID < 1 || ID > 1000) {
            throw new RuntimeException("Taxi IDs must have a value between 1 and 1000");
        }
        this.ID = ID;
        this.port = port;
    }

    // Random ID and port constructor
    public TaxiData()
    {
        this (new Random().nextInt(1001) - 1,
                new Random().nextInt(65536 - 9797) + 9797);
    }

    // === Getters ===
    public int getID() {
        return ID;
    }
    public int getPort() { return port; }
    public GridCell getPosition() { return currentPosition; }
    public double getBatteryLevel() { return batteryLevel; }
    public void reduceBattery(double reduction) { batteryLevel -= reduction; }

    // === Setters ===
    public synchronized void setPosition(GridCell newPosition) { this.currentPosition = newPosition; }
    public synchronized void setBattery(double battery) { this.batteryLevel = battery; }
    public void setRidingState(boolean b) { isRiding = b; }


    // === Utils ===
    // Print
    public String toString()
    {
        DecimalFormat formatter = new DecimalFormat("#00.0");
        String s = "Taxi " + ID;
        if (currentPosition != null) {
            s +=  " in position " + currentPosition.toString() +
            " with a battery level of " + formatter.format(batteryLevel) + "%";
        }
        s += " available at http://" + address + ":" + port + "/";
        return s;
    }


}
