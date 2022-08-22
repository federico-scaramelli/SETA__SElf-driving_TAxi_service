package TaxiNetwork;

public class GridHelper
{
    public static double GetDistance (GridPosition a, GridPosition b)
    {
        return Math.sqrt( Math.pow(b.GetX() - a.GetX(), 2) + Math.pow(b.GetY() - a.GetY(), 2) );
    }
}
