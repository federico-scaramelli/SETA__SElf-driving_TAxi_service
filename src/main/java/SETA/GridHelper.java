package SETA;

import java.util.Random;

public class GridHelper
{
    public static double getDistance(GridCell a, GridCell b)
    {
        return Math.sqrt( Math.pow(b.GetX() - a.GetX(), 2) + Math.pow(b.GetY() - a.GetY(), 2) );
    }

    public static int getDistrict(GridCell cell)
    {
        if (cell.GetX() < 0 || cell.GetY() < 0 || cell.GetX() > 9 || cell.GetY() > 9)
            return -1;

        if (cell.GetX() < 5 && cell.GetY() < 5)     return 1;
        if (cell.GetX() >= 5 && cell.GetY() < 5)    return 2;
        if (cell.GetX() < 5)                        return 3;
        return 4;
    }

    public static GridCell getRechargeStation(int district)
    {
        switch (district)
        {
            case 1:
                return new GridCell(0, 0);
            case 2:
                return new GridCell(0, 9);
            case 3:
                return new GridCell(9, 0);
            case 4:
                return new GridCell(9, 9);
            default:
                System.out.println("Cannot retrieve the recharge station. Invalid district number.");
                return null;
        }
    }

    public static GridCell getRechargeStation(GridCell cell)
    {
        return (getRechargeStation(getDistrict(cell)));
    }

    public static GridCell getRandomPosition()
    {
        return new GridCell(new Random().nextInt(10), new Random().nextInt(10));
    }
}
