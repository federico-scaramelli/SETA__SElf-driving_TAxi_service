package SETA.Taxi;

import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Random;

public class TaxiChargingData
{
    public volatile Boolean isCharging = false;
    public volatile Boolean chargeCommandReceived = false;
    public final Integer logicalClockOffset = new Random().nextInt(100);
    public Integer logicalClock = 0;

    /*
    Can be substituted with array using Optional to search for elements
    Optional<TaxiData> result = myList.stream().parallel()
                    .filter(taxi -> taxi.getID() == quitNotification.getTaxiId()).findAny();
            if (result.isPresent())
                myList.remove(result.get());
     */
    public final HashMap<Integer, TaxiData> chargingCompetitors = new HashMap<>();
    // Can be a simple array since we are not using it as FIFO
    public final PriorityQueue<TaxiChargingRequest> chargingQueue = new PriorityQueue<TaxiChargingRequest>();
    public TaxiChargingRequest currentRechargeRequest = null;
}
