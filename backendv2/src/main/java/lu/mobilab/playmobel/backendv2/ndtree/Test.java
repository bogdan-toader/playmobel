package lu.mobilab.playmobel.backendv2.ndtree;

import lu.mobilab.playmobel.backendv2.util.User;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.util.*;

/**
 * Created by assaa on 17/12/2016.
 */
public class Test {
    private static final DecimalFormat df = new DecimalFormat("###,###.#");
    private static final DecimalFormat intf = new DecimalFormat("###,###,###");

    public final static String DATA_DIR = "/Users/assaad/Desktop/kluster/Geolife Trajectories 1.3/Data/";
    public final static String DATA_DIR_TEST = "/Users/assaad/Desktop/kluster/Geolife Trajectories 1.3/DataTest/";
    public final static String DATA_GOOGLE = "/Users/assaad/Desktop/kluster/Geolife Trajectories 1.3/google/";
    public final static String DATA_DIR_SEL = DATA_GOOGLE;

    private static Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

    private static void loadDataChinese(NDTree profile, HashMap<String, Integer> users) {
        File folder = new File(DATA_DIR_SEL);
        File[] listOfFiles = folder.listFiles();
        String path;
        File subfolder;
        File[] listOfsubFiles;

        int totallines = 0;
        long starttime = System.currentTimeMillis();
        String line;


        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isDirectory()) {
                String username = listOfFiles[i].getName();
                int userid = Integer.parseInt(username);
                users.put(username, userid);
                path = listOfFiles[i].getPath() + "/Trajectory/";
                subfolder = new File(path);
                listOfsubFiles = subfolder.listFiles();
                int usersize = 0;
                for (int j = 0; j < listOfsubFiles.length; j++) {
                    if (listOfsubFiles[j].isFile() && listOfsubFiles[j].getName().endsWith(".plt")) {

                        try (BufferedReader br = new BufferedReader(new FileReader(listOfsubFiles[j]))) {
                            for (int k = 0; k < 6; k++) {
                                line = br.readLine();
                            }
                            while ((line = br.readLine()) != null) {
                                double[] input = new double[5]; //0:userID, 1:day, 2:hour, 3:gpslat, 4:gpslng
                                input[0] = userid;

                                String[] substr = line.split(",");

                                double x = Double.parseDouble(substr[4]) * 86400;
                                long timestamp = ((long) x - 2209161600l) * 1000;
                                calendar.setTime(new Date(timestamp));
                                int day = calendar.get(Calendar.DAY_OF_WEEK); //so this is 1:Sunday -> 7:Saturday
                                int hour = calendar.get(Calendar.HOUR_OF_DAY); //this is from 0 ->23
                                input[1] = day;
                                input[2] = hour;
                                input[3] = Double.parseDouble(substr[0]);
                                input[4] = Double.parseDouble(substr[1]);

                                //Decompose and insert to profile here
                                profile.insert(input);
                                usersize++;
                                totallines++;
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
                reportTime(starttime, usersize, totallines, username);
            }
        }
        finalReport(starttime, totallines);
    }

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


    private static void loadDataGoogle(NDTree profile, HashMap<String, Integer> users) {

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
                    users.put(username, userId);
                    listOfsubFiles = listOfFiles[i].listFiles();

                    for (int j = 0; j < listOfsubFiles.length; j++) {
                        if (listOfsubFiles[j].isFile() && listOfsubFiles[j].getName().endsWith(".json")) {

                            int usersize = 0;
                            String jsonData = readFile(listOfsubFiles[j]);
                            JSONObject jobj = new JSONObject(jsonData);
                            for (Object objLoc : jobj.getJSONArray("locations")) {
                                JSONObject loc = (JSONObject) objLoc;
                                long timestamp = Long.parseLong(loc.get("timestampMs").toString());
                                long latitudeE7 = Long.parseLong(loc.get("latitudeE7").toString());
                                long longitudeE7 = Long.parseLong(loc.get("longitudeE7").toString());
                                double[] input = new double[5]; //0:userID, 1:day, 2:hour, 3:gpslat, 4:gpslng
                                input[0] = userId;
                                calendar.setTime(new Date(timestamp));
                                int day = calendar.get(Calendar.DAY_OF_WEEK); //so this is 1:Sunday -> 7:Saturday
                                int hour = calendar.get(Calendar.HOUR_OF_DAY); //this is from 0 ->23
                                int min = calendar.get(Calendar.MINUTE);
                                input[1] = day;
                                input[2] = hour + min / 60.0;
                                input[3] = latitudeE7 / 10000000.0;
                                input[4] = longitudeE7 / 10000000.0;

                                profile.insert(input);
                                usersize++;
                                totallines++;
                            }
                            userId++;
                            reportTime(starttime, usersize, totallines, username);
                        }
                    }
                }
            }
            finalReport(starttime, totallines);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    private static void generateTest(NDTree profile, int users, int minPeruser, int maxPerUser) {
        Random rand = new Random(123456);
        int totallines = 0;
        long starttime = System.currentTimeMillis();

        long endTimeStamp = System.currentTimeMillis();
        long duration = 3l * 365l * 24l * 3600l * 1000l;
        long initTimeStamp = endTimeStamp - duration;
        for (int i = 0; i < users; i++) {
            int usertot = rand.nextInt(maxPerUser - minPeruser) + minPeruser;
            for (int j = 0; j < usertot; j++) {
                double[] input = new double[5]; //0:userID, 1:day, 2:hour, 3:gpslat, 4:gpslng
                input[0] = i;
                double ts = rand.nextDouble() * duration + initTimeStamp;
                calendar.setTime(new Date((long) ts));
                int day = calendar.get(Calendar.DAY_OF_WEEK); //so this is 1:Sunday -> 7:Saturday
                int hour = calendar.get(Calendar.HOUR_OF_DAY); //this is from 0 ->23
                input[1] = day;
                input[2] = hour;
                input[3] = rand.nextDouble() * 10 + 40;
                input[4] = rand.nextDouble() * 10 + 10;

                //Decompose and insert to profile here
                profile.insert(input);
                totallines++;
            }
            reportTime(starttime, usertot, totallines, i + "");
        }
        finalReport(starttime, totallines);
    }

    private static void reportTime(long starttime, int usersize, int totallines, String userName) {
        long endtime = System.currentTimeMillis();
        double time = (endtime - starttime) / 1000.0;
        double speed = totallines;
        speed = speed / time;
        System.out.println("User: " + String.format("%8s", userName) + "\t size: " + String.format("%9s", intf.format(usersize)) + "\t total: " + String.format("%11s", intf.format(totallines)) + " timepoints\t elapsed time: " + String.format("%5s", df.format(time)) + " s\t speed: " + String.format("%8s", intf.format(speed)) + " values/sec");

    }

    private static void finalReport(long starttime, int totallines) {
        long endtime = System.currentTimeMillis();
        double time = (endtime - starttime) / 1000.0;
        double speed = totallines;
        speed = speed / time;
        System.out.println("");
        System.out.println("IMPORT COMPLETED, with: " + intf.format(totallines) + " timepoints, elapsed time: " + df.format(time) + " s, speed: " + intf.format(speed) + " values/sec");
        System.out.println("");
    }

    public static void main(String[] arg) {

        double[] min = new double[]{0, 1, 0, -90, -180}; //0:userID, 1:day, 2:hour, 3:gpslat, 4:gpslng
        double[] max = new double[]{200, 7, 24, 90, 180};
        double[] resolution = new double[]{1, 1, 0.1, 0.0005, 0.001}; //0.008, 0.015 -> 1km, 0.0005, 0.001 -> 100m
        int maxPerLevel = 160;
        NDTreeConfig config = new NDTreeConfig(min, max, resolution, maxPerLevel);
        NDTree tree = new NDTree(config);

        HashMap<String, Integer> users = new HashMap<>();

        //generateTest(tree, 180, 100000, 200000);
        if (DATA_DIR_SEL.toLowerCase().contains("google")) {
            loadDataGoogle(tree, users);
        } else {
            loadDataChinese(tree, users);
        }

        tree.print();

        double[] reqmin = new double[]{0, 0, 00, -90, -180}; //0:userID, 1:day, 2:hour, 3:gpslat, 4:gpslng
        double[] reqmax = new double[]{0, 7, 03, 90, 180};

        NDTreeResult filter = tree.filter(reqmin, reqmax);
        System.out.println("Found: " + filter.getGlobal() + " results, in: " + filter.getResult().size() + " atomic results");
        filter.sortAndDisplay(10);

        System.out.println("");
        String[] groupby =new String[]{"*","*","*","0.008", "0.015"};
        NDTreeResult grouped =filter.groupBy(groupby);
        System.out.println("Found: " + grouped.getGlobal() + " results, in: " + grouped.getResult().size() + " atomic results");
        grouped.sortAndDisplay(10);





        reqmin = new double[]{0, 0, 00, 49.494902, 5.783112}; //0:userID, 1:day, 2:hour, 3:gpslat, 4:gpslng
        reqmax = new double[]{0, 7, 24, 49.877265, 6.464900};

        filter = tree.filter(reqmin, reqmax);
        System.out.println("Found: " + filter.getGlobal() + " results, in: " + filter.getResult().size() + " atomic results");
        filter.sortAndDisplay(10);

        System.out.println("");
        groupby =new String[]{"*","1","0.16","0.005", "0.01"};
        grouped =filter.groupBy(groupby);
        System.out.println("Found: " + grouped.getGlobal() + " results, in: " + grouped.getResult().size() + " atomic results");
        grouped.sortAndDisplay(1000);
        int x=0;




    }
}
