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
    private TreeMap<Long, GMMJava[]> profiles;
    private static Calendar calendar = Calendar.getInstance();
    private long profileDuration;
    private GMMConfig config;

    public User(String userId, final GMMConfig config, long profileDuration) {
        this.userId = userId;
        userLatLng = new TreeMap<Long, double[]>();
        profiles = new TreeMap<>();
        this.config = config;
        this.profileDuration = profileDuration;
    }



    private static int getProfileId(final long timestamp) {
        Date time = new Date(timestamp);
        calendar.setTime(time);
        int day = calendar.get(Calendar.DAY_OF_WEEK); //so this is 1:Sunday -> 7:Saturday
        int hour = calendar.get(Calendar.HOUR_OF_DAY); //this is from 0 ->23
        int profileId = (day - 1) * 24 + hour;
        return profileId;
    }

    private GMMJava[] createProfile(){
        GMMJava[] newprofiles = new GMMJava[24 * 7];
        for (int i = 0; i < 24 * 7; i++) {
            newprofiles[i] = new GMMJava(config);
        }
        return newprofiles;
    }

    private long lasthash=-1;
    private GMMJava[] lastres;

    private GMMJava[] createorGetProfileArray(long time){
        long t = time / profileDuration;
        if(t==lasthash){
            return lastres;
        }
        else {
            GMMJava[] resolvedProfiles;
            if (profiles.size() == 0 || t < profiles.firstKey()) {
                resolvedProfiles = createProfile();
                profiles.put(t, resolvedProfiles);
                lasthash=t;
                lastres=resolvedProfiles;
                return resolvedProfiles;
            } else {
                long lastT = profiles.floorKey(t);
                if (lastT == t) {
                    lasthash=t;
                    resolvedProfiles =profiles.get(lastT);
                    lastres=resolvedProfiles;
                    return resolvedProfiles;
                } else {
                    resolvedProfiles = createProfile();
                    profiles.put(t, resolvedProfiles);
                    lasthash=t;
                    lastres=resolvedProfiles;
                    return resolvedProfiles;
                }
            }
        }
//      return null;
    }

    private GMMJava[] getProfileArray(long time){
        long t = time / profileDuration;
        if(profiles.size()>0) {
            if(profiles.floorKey(t)!=null) {
                long lastT = profiles.floorKey(t);
                return profiles.get(lastT);
            }
        }
        return null;
    }

    public void learn(final long timestamp, final double[] latlng) {
        GMMJava[] profilesArray= createorGetProfileArray(timestamp);
        profilesArray[getProfileId(timestamp)].learnVector(latlng);
    }

    public void insert(final long timestamp, final double[] latlng) {
        userLatLng.put(timestamp, latlng);
    }

    public LatLngObj getLatLng(long timestamp) {
        if (timestamp < userLatLng.firstKey()) {
            return null;
        } else {
            long realtime=userLatLng.floorKey(timestamp);
            return new LatLngObj(userLatLng.get(realtime),timestamp,realtime);
        }
    }

    public ProbaDistribution getDistribution(long timestamp, int level) {
        GMMJava[] profilesArray = getProfileArray(timestamp);
        if(profilesArray!=null) {
            return profilesArray[getProfileId(timestamp)].generateDistributions(level);
        }
        else {
            return null;
        }
    }

    public String getUserId() {
        return userId;
    }
}
