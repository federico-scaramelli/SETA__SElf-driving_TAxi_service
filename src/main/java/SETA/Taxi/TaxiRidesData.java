package SETA.Taxi;

import SETA.RideRequest;

import java.util.ArrayList;

public class TaxiRidesData
{
    public enum RideCompetitionState
    {
        Pending,
        Won,
        Lose
    }

    public volatile Boolean isRiding = false;
    public TaxiRideThread taxiRideThread = null;
    public RideRequest currentRideRequest = null;
    RideCompetitionState competitionState = RideCompetitionState.Lose;
    public final ArrayList<Integer> completedRides = new ArrayList<>();
    public final ArrayList<TaxiData> rideCompetitors = new ArrayList<>();
    public Integer competitorsCounter = Integer.MAX_VALUE;
}
