package SETA;

import java.util.Random;

public class RideRequest
{
    public final int ID;
    public final GridCell startingPos;
    public GridCell destinationPos;

    public RideRequest()
    {
        ID = new Random().nextInt(100000);
        startingPos = GridHelper.getRandomPosition();
        destinationPos = startingPos;
        while (destinationPos.equals(startingPos))
            destinationPos = GridHelper.getRandomPosition();
    }

    public String toString()
    {
        return "Ride request ID " + ID + " from " + startingPos + " to " + destinationPos;
    }
}
