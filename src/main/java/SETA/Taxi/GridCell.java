package SETA.Taxi;

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

    public int getX() { return x; }
    public int getY() { return y; }

    public String toString() {
        return "(" + x + ", " + y + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        GridCell pos = (GridCell) o;
        return x == pos.x && y == pos.y;
    }
}
