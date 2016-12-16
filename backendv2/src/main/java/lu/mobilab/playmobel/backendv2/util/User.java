package lu.mobilab.playmobel.backendv2.util;

import org.mwg.ml.algorithm.profiling.ProbaDistribution;

import java.util.Calendar;
import java.util.Date;
import java.util.TreeMap;

/**
 * Created by assaad on 16/12/2016.
 */
public class User {
    private TreeMap<Long, double[]> userLatLng;
    private String userId;
    private GMMJava[] profiles;
    private static Calendar calendar = Calendar.getInstance();

    public User(String userId, final GMMConfig config){
        this.userId=userId;
        userLatLng=new TreeMap<Long, double[]>();
        profiles=new GMMJava[24*7];
        for(int i=0;i<24*7;i++){
            profiles[i]=new GMMJava(config);
        }
    }

    private static int getProfileId(final long timestamp){
        Date time = new Date(timestamp);
        calendar.setTime(time);
        int day = calendar.get(Calendar.DAY_OF_WEEK); //so this is 1:Sunday -> 7:Saturday
        int hour = calendar.get(Calendar.HOUR_OF_DAY); //this is from 0 ->23
        int profileId = (day - 1) * 24 + hour;
        return profileId;
    }

    public void learn(final long timestamp, final double[] latlng){
        profiles[getProfileId(timestamp)].learnVector(latlng);
    }

    public void insert(final long timestamp, final double[] latlng){
        userLatLng.put(timestamp,latlng);
    }

    public double[] getLatLng(long timestamp){
        if(timestamp<userLatLng.firstKey()){
            return null;
        }
        else {
            return userLatLng.get(userLatLng.floorKey(timestamp));
        }
    }

    public ProbaDistribution getDistribution(long timestamp, int level){
        return profiles[getProfileId(timestamp)].generateDistributions(level);
    }

    public String getUserId() {
        return userId;
    }
}
