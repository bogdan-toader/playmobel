package lu.mobilab.playmobel.backendv2.utilgreycat;

import greycat.utility.distance.Distance;
import greycat.utility.distance.GeoDistance;

/**
 * Created by bogdantoader on 04/03/2017.
 */
public class TransportationDistance implements Distance {


    private static GeoDistance geo = GeoDistance.instance();
    private static double drivingSpeed = 50000;//driving speed should be in m/hour because geo is giving distance in meter, and our time is in hours

    private static double penalty=7*24;
    private static double pickupRange= 10000; //the maximum distance in meters where a person can be considered your neighbour.

    // 0:userID, 1:day, 2:hour, 3:gpslat, 4:gpslng
    // 0:userID, 1:day, 2:hour, 3:gpslat, 4:gpslng
    @Override
    public double measure(double[] x, double[] y) {
        //If it is the same user ID:
        if (x[0] == y[0]) {
            return Double.MAX_VALUE;
        }

        //convert the day and hour into one timeslot
        double timeX = x[1] * 24 + x[2];   // time X would like to leave


        double timeY = y[1] * 24 + y[2];   // time the candidate is leaving home
        double geodist = geo.measure(new double[]{x[3], x[4]}, new double[]{y[3], y[4]}); //the distance separating them

        if(geodist>pickupRange){
            return geodist / drivingSpeed+penalty;  //or return double max
        }


        double arrivaltime = timeY + geodist / drivingSpeed; //this is the estimated arrival time

        double timediff = Math.abs(arrivaltime - timeX);
        if (timediff > 3.5 * 24) {
            timediff = Math.abs(7 * 24 - timediff);
        }

        return timediff;
    }

    @Override
    public boolean compare(double x, double y) {
        return x < y;
    }

    @Override
    public double getMinValue() {
        return 0;
    }

    @Override
    public double getMaxValue() {
        return Double.MAX_VALUE;
    }


    public static void main(String[] arg) {

        TransportationDistance dist = new TransportationDistance();

        // 0:userID, 1:day, 2:hour, 3:gpslat, 4:gpslng

        double[] user = new double[]{0, 3, 8, 32.1, 33.1};
        double[] user2 = new double[]{1, 3, 7.4, 32.3, 33.3};
        double[] user3 = new double[]{1, 3, 9, 32.3, 33.3};
        System.out.println("dist to user 2: "+dist.measure(user, user2));
        System.out.println("dist to user 3: "+dist.measure(user, user3));

    }
}
