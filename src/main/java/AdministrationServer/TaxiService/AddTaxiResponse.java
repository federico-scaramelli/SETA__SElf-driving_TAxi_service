package AdministrationServer.TaxiService;

import AdministrationServer.SmartCityManager;
import SETA.Taxi.GridCell;
import Utils.GridHelper;
import SETA.Taxi.TaxiData;

import java.util.ArrayList;
import java.util.Random;

public class AddTaxiResponse
{
    ArrayList<TaxiData> taxiList;
    ArrayList<Integer> completedRides;
    GridCell startingPosition;

    public AddTaxiResponse()
    {
        taxiList = new ArrayList<TaxiData>(SmartCityManager.getInstance().getTaxiList());

        completedRides = new ArrayList<>(SmartCityManager.getInstance().getCompletedRides());

        int startingDistrict = new Random().nextInt(4) + 1;
        //startingDistrict = 1;
        startingPosition = GridHelper.getRechargeStation(startingDistrict);
        //System.out.println(startingPosition);
    }

    public ArrayList<TaxiData> getTaxiList() { return taxiList; }
    public GridCell getStartingPosition() { return startingPosition; }

    public ArrayList<Integer> getCompletedRides() { return completedRides; }

    public String toString()
    {
        return "List of taxis currently connected to the Smart City: \n" +
                taxiList.toString() +
                //".\n--> Starting assigned district: " +
                //startingDistrict +
                ".\n    |--> Starting recharging station cell: " +
                startingPosition;
    }
}
