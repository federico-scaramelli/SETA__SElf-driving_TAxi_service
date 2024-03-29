package AdministrationServer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import SETA.Taxi.TaxiData;

// Maintain data about the Smart City on the REST server
public class SmartCityManager
{
    // Singleton
    private static SmartCityManager instance;

    // Hashmap for fast access by ID
    private final HashMap<Integer, TaxiData> taxiList;
    // Completed rides list to be sent to the new taxis
    private final ArrayList<Integer> completedRides = new ArrayList<>();

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

    public boolean addTaxi (TaxiData taxi)
    {
        synchronized (taxiList)
        {
            // Check for ID duplicates
            if (taxiList.containsKey(taxi.getID())) {
                System.out.println("ID already present.\n");
                return false;
            }

            // Check for port duplicates
            // Filter hash map with port number
            Optional<Map.Entry<Integer, TaxiData>> matchedEntry =
                    taxiList.entrySet().stream().
                            filter(element -> element.getValue().getPort() == taxi.getPort()).findAny();
            // If another taxi with the same port already exist
            if (matchedEntry.isPresent())
            {
                System.out.println("Port number already used.\n");
                return false;
            }

            taxiList.put(taxi.getID(), taxi);
        }
        System.out.println("Taxi " + taxi.getID() + " added successfully.");
        return true;
    }

    // Update the data of a taxi on REST server side
    public boolean updateTaxi (TaxiData taxi)
    {
        synchronized (taxiList)
        {
            if (!taxiList.containsKey(taxi.getID())) {
                System.out.println("ID not registered.\n");
                return false;
            }
            taxiList.put(taxi.getID(), taxi);
        }
        System.out.println("Taxi updated successfully!");
        return true;
    }

    // Remove a taxi on REST server side if it exists
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

    // Add a completed ride to the list
    public synchronized void addCompletedRide(Integer rideId) {
        if (completedRides.contains(rideId)) {
            System.out.println("ERROR! Received double completed ride! Ride " + rideId);
            throw new RuntimeException();
        }
        completedRides.add(rideId);
    }

    public ArrayList<Integer> getCompletedRides()
    {
        return new ArrayList<Integer>(completedRides);
    }
}
