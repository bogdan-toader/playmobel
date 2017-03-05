package lu.mobilab.playmobel.backendv2.utilgreycat;

import greycat.utility.distance.Distance;
import greycat.utility.distance.GeoDistance;

import java.util.ArrayList;

/**
 * Created by bogdantoader on 05/03/2017.
 */
public class ComparatorFct {
    private int i;
    private int j;
    public double[] pos1;
    public double[] pos2;
    private double geoTemporalDistance;
    public double geoDistance;
    public double timeDiff;
    public double probability;

    private static Distance geoDis = GeoDistance.instance();
    private static Distance dis = new TransportationDistance();

    public ComparatorFct(int i, int j, double[] pos1, double[] pos2) {
        if(pos1==null||pos2==null){
            probability=0;
            return;
        }
        this.i = i;
        this.j = j;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.probability = pos1[5] * pos2[5];
        this.geoTemporalDistance = dis.measure(pos1, pos2);
        this.geoDistance=geoDis.measure(new double[]{pos1[3], pos1[4]}, new double[]{pos2[3], pos2[4]});

        double timeX = pos1[1] * 24 + pos1[2];   // time X would like to leave
        double timeY = pos2[1] * 24 + pos2[2];   // time the candidate is leaving home
        timeDiff=timeX-timeY;

    }

    public double getDistance() {
        return geoTemporalDistance;
    }

    public double getProbability() {
        return probability;
    }

    public static int compare(ComparatorFct p1, ComparatorFct p2) {
        if (p1.getDistance() / p1.probability < p2.getDistance() / p2.getProbability()) return -1;
        if (p1.getDistance() / p1.probability > p2.getDistance() / p2.getProbability()) return 1;
        return 0;
    }

    public static int compareArrays( ArrayList<ComparatorFct> p1, ArrayList<ComparatorFct> p2){

        return compare(p1.get(0),p2.get(0));
    }
}
