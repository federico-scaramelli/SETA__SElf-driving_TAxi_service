package AdministrationServer;

import javax.xml.bind.annotation.XmlElement;
import java.util.HashMap;

import TaxiNetwork.TaxiData;

public class SmartCityManager
{
    private static SmartCityManager instance;

    @XmlElement (name = "taxi_list")
    private HashMap<Integer, TaxiData> taxiList;
    //private ArrayList<Taxi> taxiList;

    private SmartCityManager()
    {
        //taxiList = new ArrayList<Taxi>();
        taxiList = new HashMap<Integer, TaxiData>();
        taxiList.put(13, new TaxiData(13));
        taxiList.put(14, new TaxiData(14));
    }

    public synchronized static SmartCityManager getInstance()
    {
        if (instance == null)
        {
            instance = new SmartCityManager();
        }
        return instance;
    }

    //public ArrayList<Taxi> getTaxiList() { return taxiList; }
    public HashMap<Integer, TaxiData> getTaxiList()  { return taxiList; }

    public synchronized boolean addTaxi (TaxiData taxi)
    {
        if ( taxiList.containsKey(taxi.getID()) )
        {
            System.out.println("ID already present.\n");
            return false;
        }

        taxiList.put(taxi.getID(), taxi);
        System.out.println("Taxi added successfully.");
        System.out.println(taxi.toString());
        return true;
    }

    public synchronized boolean removeTaxi(Integer id)
    {
        if ( taxiList.containsKey(id) )
        {
            taxiList.remove(id);
            System.out.println("Taxi " + id + " removed successfully.\n");
            return true;
        }
        System.out.println("Taxi not present on the Smart City.\n");
        return false;
    }
}
