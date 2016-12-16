package lu.mobilab.playmobel.backend.util.javagmm;


import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import io.undertow.util.StatusCodes;
import org.mwg.Graph;
import org.mwg.ml.algorithm.profiling.ProbaDistribution;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;

public class BackendRunner {


    public final static String DATA_DIR = "/Users/assaad/Desktop/kluster/Geolife Trajectories 1.3/Data/";
    public final static String DATA_DIR_TEST = "/Users/assaad/Desktop/kluster/Geolife Trajectories 1.3/DataTest/";
    public final static String DATA_DIR_SEL = DATA_DIR;


//    public final static String DATA_DIR = "/Users/bogdan.toader/Documents/Datasets/Geolife Trajectories 1.3/Data/";
//    public final static String DATA_DIR_TEST = "/Users/bogdan.toader/Documents/Datasets/Geolife Trajectories 1.3/DataTest/";
//    public final static String DATA_DIR_SEL = DATA_DIR_TEST;


    private static final double err = 0.00001;
    private static final double[] gpserr = {err, err};
    private static final GMMConfig config = new GMMConfig(3, 10, 3, 10, 2, gpserr);
    private static final HashMap<String, User> index = new HashMap<>();
    private static final DecimalFormat df = new DecimalFormat("###,###.##");


    private Undertow server;

    private void loadData() {
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
                String userName = listOfFiles[i].getName();
                User user = new User(userName, config);
                index.put(userName, user);
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

                                user.insert(timestamp, latlng);
                                user.learn(timestamp, latlng);

                                totallines++;
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }


                    }
                }

                long endtime = System.currentTimeMillis();
                long time = (endtime - starttime) / 1000;
                double speed = totallines;
                speed = speed / time;
                System.out.println("Loaded user: " + userName + ", total: " + totallines + " timepoints, elapsed time: " + time + "s, speed: " + df.format(speed) + " values/sec");
            }
        }
    }

    public void start() {

        loadData();

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
                double[] latlng = user.getLatLng(timestamp);
                if (latlng != null) {
                    JsonObject serie = new JsonObject();
                    serie.add("userId", user.getUserId());
                    serie.add("lat", latlng[0]);
                    serie.add("lng", latlng[1]);
                    total++;
                    result.add(serie);
                }
            }

            httpServerExchange.getResponseHeaders().add(new HttpString("Access-Control-Allow-Origin"), "*");
            httpServerExchange.setStatusCode(StatusCodes.OK);
            System.out.println("Received request at: " + timestamp + " , returned: " + total + " users");
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

            if(proba!=null &&proba.distributions.length>0) {
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
                System.out.println("Received request at: " + timestamp + " , returned: " + proba.distributions.length + " profile points");
                httpServerExchange.getResponseSender().send(result.toString());
            }
            else{
                httpServerExchange.getResponseHeaders().add(new HttpString("Access-Control-Allow-Origin"), "*");
                httpServerExchange.setStatusCode(StatusCodes.NO_CONTENT);
                System.out.println("Received request at: " + timestamp + " , returned: 0 profile points");
                httpServerExchange.getResponseSender().send("");
            }

        }
    };


    private HttpHandler getUsers = new HttpHandler() {
        @Override
        public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {

            JsonArray result = new JsonArray();

            String[] temp=new String[index.keySet().size()];
            int c=0;
            for (String key : index.keySet()) {
                temp[c]=key;
                c++;
            }
            Arrays.sort(temp);
            for (String key : temp) {
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
