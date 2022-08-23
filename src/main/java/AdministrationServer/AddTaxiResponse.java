package AdministrationServer;

import TaxiNetwork.GridCell;
import TaxiNetwork.TaxiData;

import java.util.ArrayList;
import java.util.Random;

public class AddTaxiResponse
{
    ArrayList<TaxiData> taxiList;
    int district;

    public AddTaxiResponse()
    {
        taxiList = new ArrayList<TaxiData>(SmartCityManager.getInstance().getTaxiList());
        district = new Random().nextInt(4) + 1;
    }

    public ArrayList<TaxiData> getTaxiList() { return taxiList; }
    public int getDistrict() { return district; }

    public String toString()
    {
        return "List of taxis currently connected to the Smart City: \n" +
                taxiList.toString() +
                ".\n--> Starting assigned district: " +
                district;
    }
}
