package lu.mobilab.playmobel.backendv2;

import greycat.*;
import greycat.internal.tree.NDTree;
import greycat.leveldb.LevelDBStorage;
import greycat.ml.MLPlugin;
import io.undertow.Handlers;
import io.undertow.Undertow;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.util.*;

public class BackendRunnerGC {


//    public final static String DATA_DIR = "/Users/assaad/Desktop/kluster/Geolife Trajectories 1.3/Data/";
//    public final static String DATA_DIR_TEST = "/Users/assaad/Desktop/kluster/Geolife Trajectories 1.3/DataTest/";
//    public final static String DATA_GOOGLE = "/Users/assaad/Desktop/kluster/Geolife Trajectories 1.3/google/";
//    public final static String DATA_DIR_SEL = DATA_GOOGLE;


    public final static String DATA_DIR = "/Users/bogdantoader/Documents/Datasets/Geolife Trajectories 1.3/Data/";
    public final static String DATA_DIR_TEST = "/Users/bogdantoader/Documents/Datasets/Geolife Trajectories 1.3/DataTest/";
    public final static String DATA_GOOGLE = "/Users/bogdantoader/Documents/Datasets/google/";
    public final static String DATA_DIR_SEL = DATA_DIR;

    public final static String LEVEL_DB = "/Users/bogdantoader/Documents/Datasets/leveldb/";


    private static final DecimalFormat df = new DecimalFormat("###,###.#");
    private static final DecimalFormat intf = new DecimalFormat("###,###,###");

    private final double[] gpserr = {0.008, 0.015};
    private final long profileDuration = 4 * 30 * 24 * 3600 * 1000l; //profile duration is 3 months
    private static Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

    private Undertow server;

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
        long endtime = System.currentTimeMillis();
        double time = (endtime - starttime) / 1000.0;
        double speed = totallines;
        speed = speed / time;
        System.out.println("");
        System.out.println("IMPORT COMPLETED with: " + intf.format(totallines) + " timepoints, elapsed time: " + df.format(time) + " s, speed: " + intf.format(speed) + " values/sec");
        System.out.println("");
    }

    private static void reportTime(long starttime, int usersize, int totallines, String userName) {
        long endtime = System.currentTimeMillis();
        double time = (endtime - starttime) / 1000.0;
        double speed = totallines;
        speed = speed / time;
        System.out.println("User: " + String.format("%8s", userName) + "\t size: " + String.format("%10s", intf.format(usersize)) + "\t total: " + String.format("%11s", intf.format(totallines)) + " timepoints\t elapsed time: " + String.format("%5s", df.format(time)) + " s\t speed: " + String.format("%8s", intf.format(speed)) + " values/sec");

    }


    private void loadDataChinese(Graph g, NDTree profile) {
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
                int usertot = 0;
//                User user = new User(username, config, profileDuration, profileprecision);
//                index.put(username, user);

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
                                profile.profile(input);

//                                user.insert(timestamp, latlng);
//                                user.learn(timestamp, latlng);

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


    private void loadDataGoogle(Graph g, NDTree profile) {

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

                    //create user here
//                    User user = new User(username, config, profileDuration, profileprecision);
//                    index.put(username, user);

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

                                profile.profile(input);

                                //learn here
//                                user.insert(timestamp, latlng);
//                                user.learn(timestamp, latlng);
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

    public void start() {

        final Graph g = GraphBuilder.newBuilder().withMemorySize(1000000)
                .withPlugin(new MLPlugin())
                .withStorage(new LevelDBStorage(LEVEL_DB))
                .build();
        g.connect(connectionResult -> {

            double[] min = new double[]{0, 1, 0, -90, -180}; //0:userID, 1:day, 2:hour, 3:gpslat, 4:gpslng
            double[] max = new double[]{200, 7, 24, 90, 180};
            double[] resolution = new double[]{1, 1, 0.1, 0.0005, 0.001}; //Profile resolution: 1 user, 1 day, 0.1 hours = 6 minutes, latlng: 0.0005, 0.001 -> 100m
            //0.008, 0.015 -> 1km, 0.0005, 0.001 -> 100m

            Node profileNode=g.newNode(0,0);

            NDTree profile = (NDTree) profileNode.getOrCreate("profile", Type.NDTREE);
            profile.setMinBound(min);
            profile.setMaxBound(max);
            profile.setResolution(resolution);



            if (DATA_DIR_SEL.toLowerCase().contains("google")) {
                loadDataGoogle(g,profile);
            } else {
                loadDataChinese(g,profile);
            }


            long t=System.currentTimeMillis();
            g.save(new Callback<Boolean>() {
                @Override
                public void on(Boolean result) {
                    System.out.println("saved");
                }
            });
            long s=System.currentTimeMillis();
            System.out.println("time "+(s-t));

            if (server == null) {
                server = Undertow.builder()
                        .addHttpListener(8081, "0.0.0.0")
                        .setHandler(Handlers.path()
                        )
                        .build();
            }
            server.start();
        });
    }


    public static void main(String[] args) {
        BackendRunnerGC runner = new BackendRunnerGC();
        runner.start();
    }

}
