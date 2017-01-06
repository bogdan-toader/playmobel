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
import org.mwg.ml.algorithm.profiling.ProbaDistribution;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

public class BackendRunner {


    public final static String DATA_DIR = "/Users/assaad/Desktop/kluster/Geolife Trajectories 1.3/Data/";
    public final static String DATA_DIR_TEST = "/Users/assaad/Desktop/kluster/Geolife Trajectories 1.3/DataTest/";
    public final static String DATA_GOOGLE = "/Users/assaad/Desktop/kluster/Geolife Trajectories 1.3/google/";
    public final static String DATA_DIR_SEL = DATA_DIR;


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


    private void loadDataChinese(NDTree profile) {
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
                                int day = calendar.get(Calendar.DAY_OF_WEEK); //so this is 1:Sunday -> 7:Saturday
                                int hour = calendar.get(Calendar.HOUR_OF_DAY); //this is from 0 ->23
                                int min = calendar.get(Calendar.MINUTE);
                                double x = Double.parseDouble(substr[4]) * 86400;
                                long timestamp = ((long) x - 2209161600l) * 1000;
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
                    user.setUid(userId);
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

    public void start() {

        double[] min = new double[]{0, 1, 0, -90, -180}; //0:userID, 1:day, 2:hour, 3:gpslat, 4:gpslng
        double[] max = new double[]{200, 7, 24, 90, 180};
        double[] resolution = new double[]{1, 1, 0.1, 0.0005, 0.001}; //Profile resolution: 1 user, 1 day, 0.1 hours = 6 minutes, latlng: 0.0005, 0.001 -> 100m
        //0.008, 0.015 -> 1km, 0.0005, 0.001 -> 100m
        int maxPerLevel = 512;
        NDTreeConfig config = new NDTreeConfig(min, max, resolution, maxPerLevel);
        tree = new NDTree(config);


        if (DATA_DIR_SEL.toLowerCase().contains("google")) {
            loadDataGoogle(tree);
        } else {
            loadDataChinese(tree);
        }
        tree.print();


        if (server == null) {
            server = Undertow.builder()
                    .addHttpListener(8081, "0.0.0.0")
                    .setHandler(Handlers.path()
                            .addPrefixPath("/getPositions", getPositions)
                            .addPrefixPath("/getProfile", getProfile)
                            .addPrefixPath("/getUsers", getUsers)
                            .addPrefixPath("/getMostImportantLocs", getMostImportantLocs)
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
            double minlat = Double.parseDouble(httpServerExchange.getQueryParameters().get("minlat").getFirst());
            double minlng = Double.parseDouble(httpServerExchange.getQueryParameters().get("minlng").getFirst());
            double maxlat = Double.parseDouble(httpServerExchange.getQueryParameters().get("maxlat").getFirst());
            double maxlng = Double.parseDouble(httpServerExchange.getQueryParameters().get("maxlng").getFirst());

            double radius = Double.parseDouble(httpServerExchange.getQueryParameters().get("radius").getFirst());
            int ts = Integer.parseInt(httpServerExchange.getQueryParameters().get("ts").getFirst());
            String userid = httpServerExchange.getQueryParameters().get("userid").getFirst();
            User user = index.get(userid);

            double[] latlng = new double[]{lat, lng};
            double[] minlatlng = new double[]{minlat, minlng};
            double[] maxlatlng = new double[]{maxlat, maxlng};
            double[] proba = user.getProbaLocation(latlng, radius, minlatlng, maxlatlng, ts, startts, endts, false);
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


    private HttpHandler getMostImportantLocs = new HttpHandler() {
        @Override
        public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {
            String userid = httpServerExchange.getQueryParameters().get("userid").getFirst();
            int mostVisited = Integer.parseInt(httpServerExchange.getQueryParameters().get("mostvisited").getFirst());
            User user = index.get(userid); //todo to fix here

            // Search request to olap

            double[] reqmin = new double[]{user.getUid(), 0, 00, -90, -180}; //0:userID, 1:day, 2:hour, 3:gpslat, 4:gpslng
            double[] reqmax = new double[]{user.getUid(), 7, 24, 90, 180};

            NDTreeResult filter = tree.filter(reqmin, reqmax);
            System.out.println("Found: " + filter.getGlobal() + " results, in: " + filter.getResult().size() + " atomic results");
            filter.sort();

            //Group by
            System.out.println("");
            String[] groupby = new String[]{"*", "*", "*", "-", "-"};
            NDTreeResult grouped = filter.groupBy(groupby).sort();
            System.out.println("Found: " + grouped.getGlobal() + " results, in: " + grouped.getResult().size() + " atomic results");

            JsonArray result = new JsonArray();

            int min = Math.min(mostVisited, grouped.getResult().size());
            for (int i = 0; i < min; i++) {
                JsonObject serie = new JsonObject();
                double[] latlng = grouped.getResult().get(i).getVal();
                double d = grouped.getResult().get(i).getTot() * 100;
                d = d / grouped.getGlobal();

                serie.add("lat", latlng[0]);
                serie.add("lng", latlng[1]);
                serie.add("weightInt", grouped.getResult().get(i).getTot());
                serie.add("weightTotal", grouped.getGlobal());
                serie.add("weight", d);
                result.add(serie);
            }
            httpServerExchange.getResponseHeaders().add(new HttpString("Access-Control-Allow-Origin"), "*");
            httpServerExchange.setStatusCode(StatusCodes.OK);
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
