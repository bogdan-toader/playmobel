package lu.mobilab.playmobel.backendv2.util;

/**
 * Created by assaad on 16/12/2016.
 */
public class LatLngObj {
    private double lat;
    private double lng;
    private long requestedtime;
    private long realtime;

    public LatLngObj(double[] latlng, long requestedtime, long realtime){
        this.lat=latlng[0];
        this.lng=latlng[1];
        this.realtime=realtime;
        this.requestedtime=requestedtime;
    }


    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public long getRequestedtime() {
        return requestedtime;
    }

    public long getRealtime() {
        return realtime;
    }

}
