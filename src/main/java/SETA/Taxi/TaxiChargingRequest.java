package SETA.Taxi;

public class TaxiChargingRequest implements Comparable<TaxiChargingRequest>
{
    public int taxiId;
    public int taxiPort;
    public Integer timestamp;

    public TaxiChargingRequest(int taxiId, int taxiPort, Integer timestamp)
    {
        this.taxiId = taxiId;
        this.taxiPort = taxiPort;
        this.timestamp = timestamp;
    }

    @Override
    public int compareTo(TaxiChargingRequest o) {
        if (timestamp > o.timestamp)
            return -1;
        if (timestamp < o.timestamp)
            return 1;
        if (taxiId < o.taxiId)
            return -1;
        return 1;
    }

    @Override
    public String toString()
    {
        return "Charging request from " + taxiId + " with timestamp " + timestamp;
    }
}
