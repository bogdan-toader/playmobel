package lu.mobilab.playmobel.backend;

import org.mwg.*;
import org.mwg.importer.ImporterActions;
import org.mwg.importer.ImporterPlugin;
import org.mwg.ml.MLPlugin;
import org.mwg.task.*;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static org.mwg.core.task.Actions.*;


/**
 * Created by bogdan.toader on 24/10/16.
 */
public class BackendRunner {
    public final static String LAT = "lat";
    public final static String LNG = "lng";
    public final static String USERS_INDEX = "users";


    public final static String DATA_DIR = "/Users/assaad/Desktop/kluster/Geolife Trajectories 1.3/Data/";
    public final static String DATA_DIR_TEST = "/Users/assaad/Desktop/kluster/Geolife Trajectories 1.3/DataTest/";
    public final static String DATA_DIR_SEL = DATA_DIR;
    public final static String LEVEL_DB = "/Users/assaad/Desktop/kluster/Geolife Trajectories 1.3/leveldb/";


//    public final static String DATA_DIR = "/Users/bogdan.toader/Documents/Datasets/Geolife Trajectories 1.3/Data/";
//    public final static String DATA_DIR_TEST = "/Users/bogdan.toader/Documents/Datasets/Geolife Trajectories 1.3/DataTest/";
//    public final static String DATA_DIR_SEL=DATA_DIR;
//    public final static String LEVEL_DB = "/Users/bogdan.toader/Documents/Datasets/leveldb/";




    private static Node createUser(Graph g, String userFolderId) {
        Node user1 = g.newNode(0, 0);
        user1.set("folderId", Type.STRING, userFolderId);

        g.index(0, 0, USERS_INDEX, new Callback<NodeIndex>() {
            @Override
            public void on(NodeIndex result) {
                result.addToIndex(user1);
            }
        });


        return user1;
    }


    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    public void start() {

        final Graph g = new GraphBuilder()
                .withMemorySize(1000000)
                .withPlugin(new MLPlugin())
                .withPlugin(new ImporterPlugin())
                .withStorage(new LevelDBStorage(LEVEL_DB))
//                .withPlugin(new OffHeapMemoryPlugin())
//                .withScheduler(new HybridScheduler())
                .build();
        g.connect(connectionResult -> {
            Task readFileTask = newTask()
                    .then(ImporterActions.readFiles("{{result}}"))
                    .forEach(newTask()
                            .thenDo(new ActionFunction() {
                                @Override
                                public void eval(TaskContext context) {
                                    String path = (String) context.result().get(0);
                                    String userID = path.substring(path.lastIndexOf("/") + 1);
                                    context.defineVariable("path", path);
                                    context.defineVariable("userID", userID);
                                    Node user = createUser(context.graph(), userID);
                                    context.defineVariable("user", user);
                                    context.continueWith(context.wrap(path + "/Trajectory/"));
                                }
                            })
                            .then(ImporterActions.readFiles("{{result}}"))
                            .forEach(newTask()
                                    .then(ImporterActions.readLines("{{result}}"))
                                    .forEach(newTask()
                                            .thenDo(new ActionFunction() {
                                                @Override
                                                public void eval(TaskContext ctx) {
                                                    String res = (String) ctx.result().get(0);
                                                    int numOfLine = (int) ctx.variable("i").get(0);

                                                    if (numOfLine >= 6) {
                                                        String[] substr = res.split(","); //split the string by comma
                                                        Node user = (Node) ctx.variable("user").get(0);

                                                        final double lat = Double.parseDouble(substr[0]);
                                                        final double lng = Double.parseDouble(substr[1]);

                                                        String dateStr = substr[5] + " " + substr[6];

                                                        LocalDateTime dateTime = LocalDateTime.parse(dateStr, formatter);
                                                        ZoneOffset zoneOffset = ZoneId.of("GMT").getRules().getOffset(dateTime);
                                                        long timestamp = dateTime.toEpochSecond(zoneOffset) * 1000; //to get in ms

                                                        int globalCounter = (int) ctx.variable("dataload").get(0);
                                                        globalCounter++;
                                                        ctx.setGlobalVariable("dataload", globalCounter);

                                                        user.travelInTime(timestamp, new Callback<Node>() {
                                                            @Override
                                                            public void on(Node result) {
                                                                result.set(LAT, Type.DOUBLE, lat);
                                                                result.set(LNG, Type.DOUBLE, lng);
                                                                result.free();
                                                                ctx.continueTask();
                                                            }
                                                        });

                                                    } else {
                                                        ctx.continueTask();
                                                    }
                                                }
                                            })
                                    )
                                    .then(save())
                            )
                            .thenDo(new ActionFunction() {
                                @Override
                                public void eval(TaskContext ctx) {
                                    long endtime = System.currentTimeMillis();
                                    long starttime = (long) ctx.variable("start").get(0);
                                    int counter = (int) ctx.variable("dataload").get(0);
                                    long time = (endtime - starttime) / 1000;
                                    double speed = counter;
                                    speed = speed / time;

                                    DecimalFormat df = new DecimalFormat("###,###.##");
                                    String userID = (String) ctx.variable("userID").get(0);
                                    System.out.println("Loaded user: " + userID + ", total: " + ctx.variable("dataload").get(0) + " timepoints, elapsed time: " + time + "s, speed: " + df.format(speed) + " values/sec");
                                    ctx.continueTask();
                                }
                            })
                    )
                    .thenDo(new ActionFunction() {
                        @Override
                        public void eval(TaskContext ctx) {
                            long endtime = System.currentTimeMillis();
                            long starttime = (long) ctx.variable("start").get(0);
                            int counter = (int) ctx.variable("dataload").get(0);
                            long time = (endtime - starttime) / 1000;
                            double speed = counter;
                            speed = speed / time;

                            DecimalFormat df = new DecimalFormat("###,###.##");

                            System.out.println("Loaded " + counter + " timepoints in " + time + " seconds, speed: " + df.format(speed) + " values/sec");

                            //the server will be listening at this port 9011
                            WSServer graphServer = new WSServer(g, 9011);
                            graphServer.start();

                            RESTManager rest = new RESTManager();
                            rest.start(g);

                            ctx.continueTask();
                        }
                    });



            TaskContext ctx = readFileTask.prepare(g, DATA_DIR_SEL, new Callback<TaskResult>() {
                @Override
                public void on(TaskResult result) {
                    result.free();
                }
            });

            ctx.setGlobalVariable("start", System.currentTimeMillis());
            ctx.setGlobalVariable("dataload", 0);
            readFileTask.executeUsing(ctx);


        });
    }

    public static void main(String[] args) {
        BackendRunner runner = new BackendRunner();
        runner.start();
    }

}
