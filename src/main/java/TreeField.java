
import model.Tree;

public class TreeField extends PotentialField {

    private final Tree tree;
    private final Point center;

    public TreeField(Tree tree) {
        this.tree = tree;
        this.center = new Point((int) tree.getX() / MyStrategy.POTENTIAL_GRID_COL_SIZE, (int) tree.getY() / MyStrategy.POTENTIAL_GRID_COL_SIZE);
    }

    @Override
    public double getValue(int x, int y) {
        int colSize = MyStrategy.POTENTIAL_GRID_COL_SIZE;
        double distance = center.getDistanceTo(x, y) * colSize;
        if (distance > tree.getRadius() * 5.0 + colSize) {
            return 0.0;
        } else if (distance < tree.getRadius() + colSize) {
            return -500.0;
        } else {
            return -500.0/distance;
        }
    }

}
