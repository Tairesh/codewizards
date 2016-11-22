import java.awt.Color;

public interface IVisualClient {

    /**
     * start queueing commands to be displayed either before main drawing
     */
    public void beginPre();

    /**
     * start queueing commands to be displayed either after main drawing
     */
    public void beginPost();
    
    public void beginAbs();
    public void endAbs();

    /**
     * mark either "pre" queue of commands as ready to be displayed
     */
    public void endPre();

    /**
     * mark either "post" queue of commands as ready to be displayed
     */
    public void endPost();

    /**
     * draw a circle at (x, y) with radius r and color color
     */
    public void circle(double x, double y, double r, Color color);

    /**
     * draw a filled circle at (x, y) with radius r and color color
     */
    public void fillCircle(double x, double y, double r, Color color);

    /**
     * draw a rect with corners at (x, y) to (x, y) with color color
     */
    public void rect(double x1, double y1, double x2, double y2, Color color);

    /**
     * draw a filled rect with corners at (x1, y1) to (x2, y2) with color color
     */
    public void fillRect(double x1, double y1, double x2, double y2, Color color);

    /**
     * draw a line from (x1, y1) to (x2, y2) with color color
     */
    public void line(double x1, double y1, double x2, double y2, Color color);

    public void arc(double centerX, double centerY, double radius, double startAngle, double arcAngle, Color color);

    public void fillArc(double centerX, double centerY, double radius, double startAngle, double arcAngle, Color color);
    /**
     * show msg at coordinates (x, y) with color color
     */
    public void text(double x, double y, String msg, Color color);

    public void stop();
    
    public void sync(int tickIndex);
}
