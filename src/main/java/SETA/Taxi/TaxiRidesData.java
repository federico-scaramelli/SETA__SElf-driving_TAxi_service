package SETA.Taxi;
import java.util.ArrayList;

// Class to contain all the data related to rides stuff.
public class TaxiRidesData
{
    public enum RideCompetitionState
    {
        Pending,
        Idle
    }

    public Boolean isRiding = false;
    public TaxiRideThread taxiRideThread = null;
    RideCompetitionState competitionState = RideCompetitionState.Idle;
    public final ArrayList<Integer> completedRides = new ArrayList<>();
    public final ArrayList<TaxiData> rideCompetitors = new ArrayList<>();
    public Integer competitorsCounter = Integer.MAX_VALUE;
}
