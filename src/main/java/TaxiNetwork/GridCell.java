package TaxiNetwork;

public class GridCell
{
    public GridCell() {}

    public int x;
    public int y;

    public GridCell(int x, int y)
    {
        this.x = x;
        this.y = y;
    }

    public int GetX() { return x; }
    public int GetY() { return y; }

    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
