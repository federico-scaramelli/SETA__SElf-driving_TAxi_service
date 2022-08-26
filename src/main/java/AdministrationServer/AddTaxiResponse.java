package AdministrationServer;

import TaxiNetwork.GridCell;
import TaxiNetwork.GridHelper;
import TaxiNetwork.TaxiData;

import java.util.ArrayList;
import java.util.Random;

public class AddTaxiResponse
{
    ArrayList<TaxiData> taxiList;
    GridCell position;
    int startingDistrict;

    public AddTaxiResponse()
    {
        taxiList = new ArrayList<TaxiData>(SmartCityManager.getInstance().getTaxiList());
        startingDistrict = new Random().nextInt(4) + 1;
        position = GridHelper.getRechargeStation(startingDistrict);
        System.out.println(position);
    }

    public ArrayList<TaxiData> getTaxiList() { return taxiList; }
    public GridCell getPosition() { return position; }

    public String toString()
    {
        return "List of taxis currently connected to the Smart City: \n" +
                taxiList.toString() +
                //".\n--> Starting assigned district: " +
                //startingDistrict +
                ".\n    |--> Starting recharging station cell: " +
                position;
    }
}
