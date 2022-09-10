package SETA.Taxi;

import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Random;

// Class to contain all the data related to charging stuff.
public class TaxiChargingData
{
    public volatile Boolean isCharging = false;
    public volatile Boolean chargeCommandReceived = false;
    public final Integer logicalClockOffset = new Random().nextInt(100) + 1;
    public Integer logicalClock = 0;
    public final HashMap<Integer, TaxiData> chargingCompetitors = new HashMap<>();
    // Can be a simple array since we are not using it as FIFO
    public final PriorityQueue<TaxiChargingRequest> chargingQueue = new PriorityQueue<TaxiChargingRequest>();
    public volatile TaxiChargingRequest currentRechargeRequest = null;
}
