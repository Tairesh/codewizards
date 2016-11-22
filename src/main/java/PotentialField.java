import java.awt.Point;

abstract public class PotentialField {
	
    abstract public double getValue(int x, int y);

    public double getValue(Point point) {
        return getValue(point.x, point.y);
    }

}
