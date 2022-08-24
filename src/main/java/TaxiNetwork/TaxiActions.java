package TaxiNetwork;

public class TaxiActions
{
    TaxiData myTaxi;

    public TaxiActions(TaxiData myTaxi) { this.myTaxi = myTaxi; }

    public void SimulateRide()
    {
        myTaxi.getLocalStatistics().addTraveledKm(10.4f);
        myTaxi.getLocalStatistics().addPM10AverageValue(2f);
        myTaxi.getLocalStatistics().addPM10AverageValue(4f);
        myTaxi.getLocalStatistics().setBatteryLevel(85);
        myTaxi.getLocalStatistics().addAccomplishedRide();
    }

}
