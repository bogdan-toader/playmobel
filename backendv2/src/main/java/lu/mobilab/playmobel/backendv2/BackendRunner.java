package lu.mobilab.playmobel.backendv2;


import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import io.undertow.util.StatusCodes;
import lu.mobilab.playmobel.backendv2.util.GMMConfig;
import lu.mobilab.playmobel.backendv2.util.LatLngObj;
import lu.mobilab.playmobel.backendv2.util.User;
import org.json.JSONObject;
import org.mwg.ml.algorithm.profiling.ProbaDistribution;

import java.io.*;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;

public class BackendRunner {


    public final static String DATA_DIR = "/Users/assaad/Desktop/kluster/Geolife Trajectories 1.3/Data/";
    public final static String DATA_DIR_TEST = "/Users/assaad/Desktop/kluster/Geolife Trajectories 1.3/DataTest/";
    public final static String DATA_GOOGLE = "/Users/assaad/Desktop/kluster/Geolife Trajectories 1.3/google/";
    public final static String DATA_DIR_SEL = DATA_GOOGLE;


//    public final static String DATA_DIR = "/Users/bogdan.toader/Documents/Datasets/Geolife Trajectories 1.3/Data/";
//    public final static String DATA_DIR_TEST = "/Users/bogdan.toader/Documents/Datasets/Geolife Trajectories 1.3/DataTest/";
//    public final static String DATA_GOOGLE = "/Users/bogdan.toader/Documents/Datasets/google/";
//    public final static String DATA_DIR_SEL = DATA_GOOGLE;


    private static final DecimalFormat df = new DecimalFormat("###,###.#");
    private static final DecimalFormat intf = new DecimalFormat("###,###,###");

    private final double[] gpserr = {0.008, 0.015};
    private final GMMConfig config = new GMMConfig(3, 10, 3, 10, 2, gpserr);
    private final HashMap<String, User> index = new HashMap<>();
    private final long profileDuration = 4 * 30 * 24 * 3600 * 1000l; //profile duration is 3 months
    private final int profileprecision = 60;
    private String[] usernames;

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


    private void loadDataChinese() {
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
                                String[] substr = line.split(",");
                                double[] latlng = new double[2];
                                latlng[0] = Double.parseDouble(substr[0]);
                                latlng[1] = Double.parseDouble(substr[1]);

                                double x = Double.parseDouble(substr[4]) * 86400;
                                long timestamp = ((long) x - 2209161600l) * 1000;
                                usertot++;

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
        System.out.println("IMPORT COMPLETED, Loaded " + usernames.length + " users with: " + intf.format(totallines) + " timepoints, elapsed time: " + df.format(time) + " s, speed: " + intf.format(speed) + " values/sec");
        System.out.println("");
    }

    private static void reportTime(long starttime, int usersize, int totallines, String userName) {
        long endtime = System.currentTimeMillis();
        double time = (endtime - starttime) / 1000.0;
        double speed = totallines;
        speed = speed / time;
        System.out.println("User: " + String.format("%8s", userName) + "\t size: " + String.format("%10s", intf.format(usersize)) + "\t total: " + String.format("%11s", intf.format(totallines)) + " timepoints\t elapsed time: " + String.format("%5s", df.format(time)) + " s\t speed: " + String.format("%8s", intf.format(speed)) + " values/sec");

    }


    private void loadDataGoogle() {

        File folder = new File(DATA_DIR_SEL);
        File[] listOfFiles = folder.listFiles();
        File[] listOfsubFiles;
        int totallines = 0;
        long starttime = System.currentTimeMillis();

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

                                user.insert(timestamp, latlng);
                                user.learn(timestamp, latlng);
                                usertot++;
                                totallines++;
                            }
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

        if (DATA_DIR_SEL.toLowerCase().contains("google")) {
            loadDataGoogle();
        } else {
            loadDataChinese();
        }


        if (server == null) {
            server = Undertow.builder()
                    .addHttpListener(8081, "0.0.0.0")
                    .setHandler(Handlers.path()
                            .addPrefixPath("/getPositions", getPositions)
                            .addPrefixPath("/getProfile", getProfile)
                            .addPrefixPath("/getUsers", getUsers)
                    )
                    .build();
        }
        server.start();
    }


    private HttpHandler getPositions = new HttpHandler() {
        @Override
        public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {
            long timestamp = Long.parseLong(httpServerExchange.getQueryParameters().get("timestamp").getFirst());

            JsonArray result = new JsonArray();
            int total = 0;
            for (String key : index.keySet()) {
                User user = index.get(key);
                LatLngObj latlng = user.getLatLng(timestamp);
                if (latlng != null) {
                    JsonObject serie = new JsonObject();
                    serie.add("userId", user.getUserId());
                    serie.add("lat", latlng.getLat());
                    serie.add("lng", latlng.getLng());
                    serie.add("realtime", latlng.getRealtime());
                    total++;
                    result.add(serie);
                }
            }

            httpServerExchange.getResponseHeaders().add(new HttpString("Access-Control-Allow-Origin"), "*");
            httpServerExchange.setStatusCode(StatusCodes.OK);
            System.out.println("Get all positions request at time: " + timestamp + ", returned: " + total + " users");
            httpServerExchange.getResponseSender().send(result.toString());
        }
    };

    private HttpHandler getProfile = new HttpHandler() {
        @Override
        public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {
            long timestamp = Long.parseLong(httpServerExchange.getQueryParameters().get("timestamp").getFirst());
            String userid = httpServerExchange.getQueryParameters().get("userid").getFirst();
            User user = index.get(userid);
            ProbaDistribution proba = user.getDistribution(timestamp, 0);

            if (proba != null && proba.distributions.length > 0) {
                JsonArray result = new JsonArray();
                for (int i = 0; i < proba.distributions.length; i++) {
                    JsonObject serie = new JsonObject();
                    double[] latlng = proba.distributions[i].getAvg();
                    double d = proba.total[i] * 100;
                    d = d / proba.global;

                    serie.add("lat", latlng[0]);
                    serie.add("lng", latlng[1]);
                    serie.add("weightInt", proba.total[i]);
                    serie.add("weightTotal", proba.global);
                    serie.add("weight", d);
                    result.add(serie);
                }

                httpServerExchange.getResponseHeaders().add(new HttpString("Access-Control-Allow-Origin"), "*");
                httpServerExchange.setStatusCode(StatusCodes.OK);
                System.out.println("Get Profile of user: " + userid + " at time: " + timestamp + ", returned: " + proba.distributions.length + " profile points");
                httpServerExchange.getResponseSender().send(result.toString());
            } else {
                httpServerExchange.getResponseHeaders().add(new HttpString("Access-Control-Allow-Origin"), "*");
                httpServerExchange.setStatusCode(StatusCodes.NO_CONTENT);
                System.out.println("Get Profile of user: " + userid + " at time: " + timestamp + ", returned: 0 profile points");
                httpServerExchange.getResponseSender().send("");
            }

        }
    };


    private HttpHandler getProfileLocation = new HttpHandler() {
        @Override
        public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {
            long startts = Long.parseLong(httpServerExchange.getQueryParameters().get("startts").getFirst());
            long endts = Long.parseLong(httpServerExchange.getQueryParameters().get("endts").getFirst());
            double lat = Double.parseDouble(httpServerExchange.getQueryParameters().get("lat").getFirst());
            double lng = Double.parseDouble(httpServerExchange.getQueryParameters().get("lng").getFirst());
            double radius = Double.parseDouble(httpServerExchange.getQueryParameters().get("radius").getFirst());
            int ts = Integer.parseInt(httpServerExchange.getQueryParameters().get("ts").getFirst());
            String userid = httpServerExchange.getQueryParameters().get("userid").getFirst();
            User user = index.get(userid);

            double[] latlng = new double[]{lat, lng};
            double[] proba = user.getProbaLocation(latlng, radius, ts, startts, endts);
            double d = 0;
            JsonArray result = new JsonArray();

            for (int i = 0; i < proba.length; i++) {
                d = i * 7.0 / proba.length;
                JsonObject serie = new JsonObject();
                serie.add("time", d);
                serie.add("proba", proba[i]);
                result.add(serie);
            }
            httpServerExchange.getResponseHeaders().add(new HttpString("Access-Control-Allow-Origin"), "*");
            httpServerExchange.setStatusCode(StatusCodes.OK);
            System.out.println("Profiling location: " + lat + "," + lng + " completed");
            httpServerExchange.getResponseSender().send(result.toString());

        }
    };

    private HttpHandler getUsers = new HttpHandler() {
        @Override
        public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {

            JsonArray result = new JsonArray();

            for (String key : usernames) {
                result.add(key);
            }

            httpServerExchange.getResponseHeaders().add(new HttpString("Access-Control-Allow-Origin"), "*");
            httpServerExchange.setStatusCode(StatusCodes.OK);
            System.out.println("Listing all users: " + index.keySet().size() + " users returned!");
            httpServerExchange.getResponseSender().send(result.toString());

        }
    };

    public static void main(String[] args) {
        BackendRunner runner = new BackendRunner();
        runner.start();
    }

}
