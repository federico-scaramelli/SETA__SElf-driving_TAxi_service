package TaxiNetwork;

public class GridPosition
{
    private int x;
    private int y;

    public int GetDistrict()
    {
        if (x < 0 || y < 0 || x > 9 || y > 9)
            return -1;

        if (x < 5 && y < 5)     return 1;
        if (x >= 5 && y < 5)    return 2;
        if (x < 5)              return 3;
        return 4;
    }

    public int GetX() { return x; }
    public int GetY() { return y; }

    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
