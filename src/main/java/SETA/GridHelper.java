package SETA;

import java.util.Random;

public class GridHelper
{
    public static double getDistance(GridCell a, GridCell b)
    {
        return Math.sqrt( Math.pow(b.getX() - a.getX(), 2) + Math.pow(b.getY() - a.getY(), 2) );
    }

    public static int getDistrict(GridCell cell)
    {
        if (cell.getX() < 0 || cell.getY() < 0 || cell.getX() > 9 || cell.getY() > 9)
            return -1;

        if (cell.getX() < 5 && cell.getY() < 5)     return 1;
        if (cell.getX() >= 5 && cell.getY() < 5)    return 2;
        if (cell.getX() < 5)                        return 3;
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
