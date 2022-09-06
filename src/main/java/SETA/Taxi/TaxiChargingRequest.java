package SETA.Taxi;

public class TaxiChargingRequest implements Comparable<TaxiChargingRequest>
{
    public TaxiData taxi;
    public Integer timestamp;

    public TaxiChargingRequest(TaxiData taxi, Integer timestamp)
    {
        this.taxi = taxi;
        this.timestamp = timestamp;
    }

    @Override
    public int compareTo(TaxiChargingRequest o) {
        if (timestamp > o.timestamp)
            return -1;
        if (timestamp < o.timestamp)
            return 1;
        if (taxi.getID() < o.taxi.getID())
            return -1;
        return 1;
    }
}
