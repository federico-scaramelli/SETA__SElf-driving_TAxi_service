package AdministrationServer;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.HashMap;

import TaxiNetwork.TaxiData;

@XmlRootElement
public class SmartCityManager
{
    private static SmartCityManager instance;

    @XmlElement (name = "taxi_list")
    private final HashMap<Integer, TaxiData> taxiList;

    private SmartCityManager()
    {
        //taxiList = new ArrayList<Taxi>();
        taxiList = new HashMap<Integer, TaxiData>();
    }

    public synchronized static SmartCityManager getInstance()
    {
        if (instance == null)
        {
            instance = new SmartCityManager();
        }
        return instance;
    }

    public HashMap<Integer, TaxiData> getTaxiMap()  { return taxiList; }
    public ArrayList<TaxiData> getTaxiList()  { return new ArrayList<>(taxiList.values()); }

    public synchronized boolean addTaxi (TaxiData taxi)
    {
        if ( taxiList.containsKey(taxi.getID()) )
        {
            System.out.println("ID already present.\n");
            return false;
        }
        taxiList.put(taxi.getID(), taxi);
        System.out.println("Taxi " + taxi.getID() + " added successfully.");
        return true;
    }

    public boolean removeTaxi(Integer id)
    {
        synchronized (taxiList)
        {
            if (taxiList.containsKey(id)) {
                taxiList.remove(id);
                System.out.println("Taxi " + id + " removed successfully.\n");
                return true;
            }
            System.out.println("Taxi not present on the Smart City.\n");
            return false;
        }
    }
}
