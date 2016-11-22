
import model.Faction;

public class MyMath {
    
    public static int getMaxIndex(int a, int b, int c)
    {
        if (a > b && a > c) {
            return 0;
        } else if (c > a && c > b) {
            return 2;
        } else {
            return 1;
        }
    }
    
    public static int getMinIndex(int a, int b, int c)
    {
        if (a < b && a < c) {
            return 0;
        } else if (c < a && c < b) {
            return 2;
        } else {
            return 1;
        }
    }

    public static boolean nearlyEquals(Float numberA, Float numberB, float epsilon) {
        if (numberA == null) {
            return numberB == null;
        }

        if (numberB == null) {
            return false;
        }

        if (numberA.equals(numberB)) {
            return true;
        }

        if (Float.isInfinite(numberA) || Float.isNaN(numberA)
                || Float.isInfinite(numberB) || Float.isNaN(numberB)) {
            return false;
        }

        return Math.abs(numberA - numberB) < epsilon;
    }

    public static boolean nearlyEquals(Double numberA, Double numberB, double epsilon) {
        if (numberA == null) {
            return numberB == null;
        }

        if (numberB == null) {
            return false;
        }

        if (numberA.equals(numberB)) {
            return true;
        }

        if (Double.isInfinite(numberA) || Double.isNaN(numberA)
                || Double.isInfinite(numberB) || Double.isNaN(numberB)) {
            return false;
        }

        return Math.abs(numberA - numberB) < epsilon;
    }
    
    public static double normalizeAngle(double angle)
    {
        double pi = StrictMath.PI;
        while (angle < -pi || angle > pi) {
            if (angle > pi) {
                angle -= pi*2.0;
            } else if (angle < -pi) {
                angle += pi*2.0;
            }
        }
        return angle;
    }

//    public static double normalizeAngle(double angle)
//    {
//        double pi = StrictMath.PI;
//        while (angle < 0 || angle > pi*2.0) {
//            if (angle > pi*2.0) {
//                angle -= pi*2.0;
//            } else if (angle < 0) {
//                angle += pi*2.0;
//            }
//        }
//        return angle-pi;
//    }
        
}
