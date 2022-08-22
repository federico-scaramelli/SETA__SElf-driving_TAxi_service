package TaxiNetwork;

import AdministrationServer.Statistics;

public class Taxi
{
    private int ID;
    private final String address = "localhost";
    private String port;

    Statistics localStatistics;

    // Constructor
    public Taxi(Integer ID)
    {
        this.ID = ID;
    }

    // Getters
    public int getID() {
        return ID;
    }
    public String getPort() { return port; }

    // Print
    public String toString()
    {
        String s = "Taxi " + ID;
        if (port != null)
            s += " available at " + address + ":" + port;
        else
            s += " not connected to the network.";
        return s;
    }
}
