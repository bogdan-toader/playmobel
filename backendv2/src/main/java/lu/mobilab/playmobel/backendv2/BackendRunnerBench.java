package lu.mobilab.playmobel.backendv2;


import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import io.undertow.util.StatusCodes;
import lu.mobilab.playmobel.backendv2.ndtree.NDTree;
import lu.mobilab.playmobel.backendv2.ndtree.NDTreeConfig;
import lu.mobilab.playmobel.backendv2.ndtree.NDTreeResult;
import lu.mobilab.playmobel.backendv2.util.GMMConfig;
import lu.mobilab.playmobel.backendv2.util.LatLngObj;
import lu.mobilab.playmobel.backendv2.util.User;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.util.*;

public class BackendRunnerBench {


//    public final static String DATA_DIR = "/Users/assaad/Desktop/kluster/Geolife Trajectories 1.3/Data/";
//    public final static String DATA_DIR_TEST = "/Users/assaad/Desktop/kluster/Geolife Trajectories 1.3/DataTest/";
//    public final static String DATA_GOOGLE = "/Users/assaad/Desktop/kluster/Geolife Trajectories 1.3/google/";
//    public final static String DATA_DIR_SEL = DATA_GOOGLE;



//   public final static String DATA_DIR = "/Volumes/Data/Data/Geolife Trajectories 1.3/Data/";
//   public final static String DATA_DIR_TEST = "/Volumes/Data/Data/Geolife Trajectories 1.3/DataTest/";
//   public final static String DATA_GOOGLE = "/Volumes/Data/Data/Geolife Trajectories 1.3/google/";
//   public final static String DATA_DIR_SEL = DATA_DIR;
//   public final static String DATA_DIR_SEL = DATA_DIR_TEST;

// data BT ----------------------

    public final static String DATA_DIR = "/Users/bogdantoader/Documents/Datasets/Geolife Trajectories 1.3/Data/";
    public final static String DATA_DIR_TEST = "/Users/bogdantoader/Documents/Datasets/Geolife Trajectories 1.3/DataTest/";
    public final static String DATA_GOOGLE = "/Users/bogdantoader/Documents/Datasets/Geolife Trajectories 1.3/google/";
    public final static String DATA_DIR_SEL = DATA_DIR;
//    public final static String DATA_DIR_SEL = DATA_DIR_TEST;


    private static final DecimalFormat df = new DecimalFormat("###,###.#");
    private static final DecimalFormat intf = new DecimalFormat("###,###,###");

    private static int USERS=180;
    private final double[] gpserr = {0.000008, 0.000015};
    private final GMMConfig config = new GMMConfig(2, 80, 2, 10, 2, gpserr);
    private final HashMap<String, User> index = new HashMap<>();
    private final long profileDuration = 4 * 30 * 24 * 3600 * 1000l; //profile duration is 3 months
    private final int profileprecision = 60;
    private String[] usernames;
    private static Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

    private Undertow server;
    private NDTree tree;


    private static String readFile(File filename) {
        String result = "";
        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
            while (line != null) {
                sb.append(line);
                line = br.readLine();
            }
            result = sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private void finalReport(long starttime, int totallines) {
        usernames = new String[index.keySet().size()];
        int c = 0;
        for (String key : index.keySet()) {
            usernames[c] = key;
            c++;
        }
        Arrays.sort(usernames);
        long endtime = System.currentTimeMillis();
        double time = (endtime - starttime) / 1000.0;
        double speed = totallines;
        speed = speed / time;
        System.out.println("");
        System.out.print("IMPORT COMPLETED, Loaded \t" + usernames.length + "\t users with: \t" + intf.format(totallines) + "\t timepoints, elapsed time: \t" + df.format(time) + "\t s, speed: \t" + intf.format(speed) + "\t values/sec\t");
    //    System.out.println("");
    }

    private static void reportTime(long starttime, int usersize, int totallines, String userName) {
        long endtime = System.currentTimeMillis();
        double time = (endtime - starttime) / 1000.0;
        double speed = totallines;
        speed = speed / time;
        System.out.println("User: " + String.format("%8s", userName) + "\t size: " + String.format("%10s", intf.format(usersize)) + "\t total: " + String.format("%11s", intf.format(totallines)) + " timepoints\t elapsed time: " + String.format("%5s", df.format(time)) + " s\t speed: " + String.format("%8s", intf.format(speed)) + " values/sec");

    }


    private void loadDataChinese(NDTree profile) {
        File folder = new File(DATA_DIR_SEL);
        File[] listOfFiles = folder.listFiles();
        String path;
        File subfolder;
        File[] listOfsubFiles;

        int totallines = 0;
        long starttime = System.currentTimeMillis();
        String line;



        for (int i = 0; i < USERS; i++) {
            if (listOfFiles[i].isDirectory()) {
                String username = listOfFiles[i].getName();
                int userid = Integer.parseInt(username);
                int usertot = 0;
                User user = new User(username, config, profileDuration, profileprecision);
                index.put(username, user);
                path = listOfFiles[i].getPath() + "/Trajectory/";
                subfolder = new File(path);
                listOfsubFiles = subfolder.listFiles();

                for (int j = 0; j < listOfsubFiles.length; j++) {
                    if (listOfsubFiles[j].isFile() && listOfsubFiles[j].getName().endsWith(".plt")) {

                        try (BufferedReader br = new BufferedReader(new FileReader(listOfsubFiles[j]))) {
                            for (int k = 0; k < 6; k++) {
                                line = br.readLine();
                            }
                            while ((line = br.readLine()) != null) {
                                double[] input = new double[5]; //0:userID, 1:day, 2:hour, 3:gpslat, 4:gpslng

                                String[] substr = line.split(",");
                                double[] latlng = new double[2];
                                latlng[0] = Double.parseDouble(substr[0]);
                                latlng[1] = Double.parseDouble(substr[1]);

                                double x = Double.parseDouble(substr[4]) * 86400;
                                long timestamp = ((long) x - 2209161600l) * 1000;

                                calendar.setTime(new Date(timestamp));
                                int day = calendar.get(Calendar.DAY_OF_WEEK); //so this is 1:Sunday -> 7:Saturday
                                int hour = calendar.get(Calendar.HOUR_OF_DAY); //this is from 0 ->23
                                int min = calendar.get(Calendar.MINUTE);
                                usertot++;

                                input[0] = userid;
                                input[1] = day;
                                input[2] = hour + min / 60.0;
                                ;
                                input[3] = Double.parseDouble(substr[0]);
                                input[4] = Double.parseDouble(substr[1]);
                                profile.insert(input);

                                user.insert(timestamp, latlng);
                                user.learn(timestamp, latlng);

                                totallines++;
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }


                    }
                }
                reportTime(starttime, usertot, totallines, username);
            }
        }
        finalReport(starttime, totallines);
    }


    private void loadDataGoogle(NDTree profile) {

        File folder = new File(DATA_DIR_SEL);
        File[] listOfFiles = folder.listFiles();
        File[] listOfsubFiles;
        int totallines = 0;
        long starttime = System.currentTimeMillis();
        int userId = 0;
        try {
            for (int i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].isDirectory()) {
                    String username = listOfFiles[i].getName();
                    listOfsubFiles = listOfFiles[i].listFiles();
                    User user = new User(username, config, profileDuration, profileprecision);
                    index.put(username, user);

                    for (int j = 0; j < listOfsubFiles.length; j++) {
                        if (listOfsubFiles[j].isFile() && listOfsubFiles[j].getName().endsWith(".json")) {

                            String jsonData = readFile(listOfsubFiles[j]);
                            JSONObject jobj = new JSONObject(jsonData);
                            int usertot = 0;

                            for (Object objLoc : jobj.getJSONArray("locations")) {
                                JSONObject loc = (JSONObject) objLoc;
                                long timestamp = Long.parseLong(loc.get("timestampMs").toString());
                                long latitudeE7 = Long.parseLong(loc.get("latitudeE7").toString());
                                long longitudeE7 = Long.parseLong(loc.get("longitudeE7").toString());
                                double[] latlng = new double[2];
                                latlng[0] = latitudeE7 / 10000000.0;
                                latlng[1] = longitudeE7 / 10000000.0;

                                double[] input = new double[5]; //0:userID, 1:day, 2:hour, 3:gpslat, 4:gpslng
                                calendar.setTime(new Date(timestamp));
                                int day = calendar.get(Calendar.DAY_OF_WEEK); //so this is 1:Sunday -> 7:Saturday
                                int hour = calendar.get(Calendar.HOUR_OF_DAY); //this is from 0 ->23
                                int min = calendar.get(Calendar.MINUTE);
                                input[0] = userId;
                                input[1] = day;
                                input[2] = hour + min / 60.0;
                                input[3] = latitudeE7 / 10000000.0;
                                input[4] = longitudeE7 / 10000000.0;

                                profile.insert(input);
                                user.insert(timestamp, latlng);
                                user.learn(timestamp, latlng);
                                usertot++;
                                totallines++;
                            }
                            userId++;
                            reportTime(starttime, usertot, totallines, username);
                        }
                    }
                }
            }
            finalReport(starttime, totallines);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void experiment() {
        double[] minlatlngworld = new double[]{-90, -180};              //min bound of the world
        double[] maxlatlngworld = new double[]{90, 180};                //max bound of the world

        double[] minlatlng = new double[]{49.494902, 5.783112};         //min bound of lux
        double[] maxlatlng = new double[]{49.877265, 6.464900};         //max bound of lux

        double[] searchWork = new double[]{49.632510, 6.168830};        //work
        double[] searchHome = new double[]{49.508012, 6.050853};        //home
        double[] searchGarnich = new double[]{49.621166, 5.935100};     //observation

        int minPrecision = 5; //Search every 5 minutes of the full week
        double[] proba = index.get("assaad").getProbaLocation(searchGarnich, 1000, minlatlng, maxlatlng, minPrecision, 1450369538000l, 1481991938000l, false);
        //you get: decimal day, decimal hour, probability in %

        double d, h;

        System.out.println("printing probabilities");
        for (int i = 0; i < proba.length; i++) {
            d = i * 7.0 / proba.length;
            h = d - ((int) d);
            h = h * 24;
            System.out.println(d + "," + h + "," + proba[i]);
        }
    }


    private void testClassical(double[] latlng, double radius) {
        long totaltime = 0;
        int counter = 0;
        long resultfounds = 0;
        for (String s : index.keySet()) {
            User u = index.get(s);
            long[] l = u.testClassicalSpeed(latlng, radius);
            totaltime += l[0];
            resultfounds += l[1];
            counter++;
        }
        System.out.print("\tTotal time taken to iterate on all users, raw data: \t" + totaltime / 1000000 + "\t ms. Avg per user: \t" + totaltime * 1.0 / (counter * 1000000) + "\t ms/user, results: \t"+resultfounds);
    }


    private void testProfile(double[] latlng, double radius) {
        long totaltime = 0;
        int level = 1;
        int counter = 0;
        long resultfounds = 0;
        for (String s : index.keySet()) {
            User u = index.get(s);
            long[] l = u.testProfileSpeed(latlng, radius, level);
            totaltime += l[0];
            resultfounds += l[1];
            counter++;
        }
        System.out.print("\tTotal time taken to iterate on all users, profiles: \t" + totaltime / 1000000 + "\t ms. Avg per user: \t" + totaltime * 1.0 / (counter * 1000000) + "\t ms/user, results: \t"+resultfounds);
    }


    private void start(){
        double[] min = new double[]{0, 1, 0, -90, -180}; //0:userID, 1:day, 2:hour, 3:gpslat, 4:gpslng
        double[] max = new double[]{200, 7, 24, 90, 180};
        double[] resolution = new double[]{1, 1, 0.1, 0.0005, 0.001}; //Profile resolution: 1 user, 1 day, 0.1 hours = 6 minutes, latlng: 0.0005, 0.001 -> 100m
        //0.008, 0.015 -> 1km, 0.0005, 0.001 -> 100m
        int maxPerLevel = 160;
        NDTreeConfig config = new NDTreeConfig(min, max, resolution, maxPerLevel);
        tree = new NDTree(config);


        if (DATA_DIR_SEL.toLowerCase().contains("google")) {
            loadDataGoogle(tree);
        } else {
            loadDataChinese(tree);
        }

        //test profile query speed
        double[] latlng = new double[]{39.988356, 116.316227};
        double radius = 500;
        testClassical(latlng, radius);
        testProfile(latlng, radius);
    }


    public static void main(String[] args) {
        BackendRunnerBench runner = new BackendRunnerBench();
        runner.start();
    }

}
