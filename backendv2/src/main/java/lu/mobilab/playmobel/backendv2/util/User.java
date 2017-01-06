package lu.mobilab.playmobel.backendv2.util;

import lu.mobilab.playmobel.backendv2.ndtree.NDTree;
import org.mwg.ml.algorithm.profiling.ProbaDistribution;
import org.mwg.structure.distance.Distance;
import org.mwg.structure.distance.Distances;
import org.mwg.structure.distance.GeoDistance;

import java.util.*;

/**
 * Created by assaad on 16/12/2016.
 */
public class User {
    private TreeMap<Long, double[]> userLatLng;
    private String userId;
    private int uid;
    private TreeMap<Long, GMMJava[]> profiles;

    private static Calendar calendarUTC = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    private static Calendar calendar = Calendar.getInstance();
    private long profileDuration;
    private int minPrecision;
    private GMMConfig config;

    private static Distance distance = GeoDistance.instance();

    public User(String userId, final GMMConfig config, long profileDuration, int minPrecision) {
        this.userId = userId;
        userLatLng = new TreeMap<Long, double[]>();
        profiles = new TreeMap<>();
        this.minPrecision = minPrecision;
        this.config = config;
        this.profileDuration = profileDuration;
    }


    private static int getProfileId(final long timestamp, int minPrecision, boolean utc) {
        Date time = new Date(timestamp);
        if (utc) {

            calendarUTC.setTime(time);
            int day = calendarUTC.get(Calendar.DAY_OF_WEEK); //so this is 1:Sunday -> 7:Saturday
            int hour = calendarUTC.get(Calendar.HOUR_OF_DAY); //this is from 0 ->23
            int min = calendarUTC.get(Calendar.MINUTE);
            int profileId = (((day - 1) * 24 + hour) * 60 + min) / minPrecision;
            return profileId;
        } else {
            calendar.setTime(time);
            int day = calendar.get(Calendar.DAY_OF_WEEK); //so this is 1:Sunday -> 7:Saturday
            int hour = calendar.get(Calendar.HOUR_OF_DAY); //this is from 0 ->23
            int min = calendar.get(Calendar.MINUTE);
            int profileId = (((day - 1) * 24 + hour) * 60 + min) / minPrecision;
            return profileId;
        }
    }

    private GMMJava[] createProfile() {
        GMMJava[] newprofiles = new GMMJava[24 * 7 * 60 / minPrecision + 1];
        for (int i = 0; i < 24 * 7 * 60 / minPrecision; i++) {
            newprofiles[i] = new GMMJava(config);
        }
        return newprofiles;
    }

    private long lasthash = -1;
    private GMMJava[] lastres;

    private GMMJava[] createorGetProfileArray(long time) {
        long t = time / profileDuration;
        if (t == lasthash) {
            return lastres;
        } else {
            GMMJava[] resolvedProfiles;
            if (profiles.size() == 0 || t < profiles.firstKey()) {
                resolvedProfiles = createProfile();
                profiles.put(t, resolvedProfiles);
                lasthash = t;
                lastres = resolvedProfiles;
                return resolvedProfiles;
            } else {
                long lastT = profiles.floorKey(t);
                if (lastT == t) {
                    lasthash = t;
                    resolvedProfiles = profiles.get(lastT);
                    lastres = resolvedProfiles;
                    return resolvedProfiles;
                } else {
                    resolvedProfiles = createProfile();
                    profiles.put(t, resolvedProfiles);
                    lasthash = t;
                    lastres = resolvedProfiles;
                    return resolvedProfiles;
                }
            }
        }
//      return null;
    }

    private GMMJava[] getProfileArray(long time) {
        long t = time / profileDuration;
        if (profiles.size() > 0) {
            if (profiles.floorKey(t) != null) {
                long lastT = profiles.floorKey(t);
                return profiles.get(lastT);
            }
        }
        return null;
    }

    public void learn(final long timestamp, final double[] latlng) {
        GMMJava[] profilesArray = createorGetProfileArray(timestamp);
        profilesArray[getProfileId(timestamp, minPrecision, true)].learnVector(latlng);
    }

    public void insert(final long timestamp, final double[] latlng) {
        userLatLng.put(timestamp, latlng);
    }

    public LatLngObj getLatLng(long timestamp) {
        if (timestamp < userLatLng.firstKey()) {
            return null;
        } else {
            long realtime = userLatLng.floorKey(timestamp);
            return new LatLngObj(userLatLng.get(realtime), timestamp, realtime);
        }
    }

    public ProbaDistribution getDistribution(long timestamp, int level) {
        GMMJava[] profilesArray = getProfileArray(timestamp);
        if (profilesArray != null) {
            return profilesArray[getProfileId(timestamp, minPrecision, true)].generateDistributions(level);
        } else {
            return null;
        }
    }

    public double[] getProbaLocation(double[] latlng, double radius, double[] minlatlng, double[] maxlatlng, int minPrecision, long startTime, long endTime, boolean utc) {
        long st = System.currentTimeMillis();
        NavigableSet<Long> keyset = userLatLng.navigableKeySet().subSet(startTime, true, endTime, true);

        int timeslots = 7 * 24 * 60 / minPrecision;
        int totalinside = 0;
        int[] inside = new int[timeslots];
        int[] total = new int[timeslots];

        //for (Long timekey : keyset) {
        for (long timekey = startTime; timekey < endTime; timekey += minPrecision * 60 * 1000) {
            int profileId = getProfileId(timekey, minPrecision, utc);
            double[] userlatlng = userLatLng.get(userLatLng.floorKey(timekey));
            if (NDTree.checkInside(userlatlng, minlatlng, maxlatlng)) {
                if (distance.measure(latlng, userlatlng) <= radius) {
                    inside[profileId]++;
                    totalinside++;
                }
                total[profileId]++;
            }
        }

        double[] proba = new double[total.length];
        for (int i = 0; i < total.length; i++) {
            if (total[i] != 0) {
                proba[i] = inside[i] * 100.0 / total[i];
            }
        }

        long endtime = System.currentTimeMillis();
        if (keyset.size() != 0) {
            double d = totalinside * 100.0 / keyset.size();
            System.out.println("Found: " + keyset.size() + " points, " + totalinside + " inside this location, overall proba: -> " + d + " %, calculation time: " + (endtime - st) + " ms");
        }

        return proba;
    }


    public String getUserId() {
        return userId;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }
}
