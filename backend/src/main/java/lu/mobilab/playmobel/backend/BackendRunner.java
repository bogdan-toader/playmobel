package lu.mobilab.playmobel.backend;

import org.mwg.*;
import org.mwg.importer.ImporterActions;
import org.mwg.importer.ImporterPlugin;
import org.mwg.ml.MLPlugin;
import org.mwg.ml.algorithm.regression.PolynomialNode;
import org.mwg.ml.algorithm.regression.actions.SetContinuous;
import org.mwg.task.*;

import java.io.BufferedReader;
import java.io.FileReader;
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
    public final static String LATEXTRAP = "latextrap";
    public final static String LNGEXTRAP = "lngextrap";
    public final static String USERS_INDEX = "users";

    public final static String DATA_DIR = "/Users/bogdan.toader/Documents/Datasets/Geolife Trajectories 1.3/Data/";
    public final static String LEVEL_DB="/Users/bogdan.toader/Documents/Datasets/leveldb/";



    private static Task setValue = newTask()
            .then(travelInTime("{{requestedtime}}"))
            .then(action(SetContinuous.NAME, "latextrap", "{{lat}}"))
            .then(action(SetContinuous.NAME, "lngextrap", "{{lng}}"));


    //this is a good way of coding where you can replace one function by another :) the first function will do extrapolation
    //the second one will just set the variables ok

    private static void setLatLngPoly(final Graph g, final Node user, final long time, final double lat, final double lng) {
        TaskContext context = setValue.prepare(g, user, new Callback<TaskResult>() {
            @Override
            public void on(TaskResult result) {
                result.free();
            }
        });
        context.setGlobalVariable("requestedtime", time);
        context.setGlobalVariable("lat", lat);
        context.setGlobalVariable("lng", lng);
        setValue.executeUsing(context);
    }

    private static void setLanLngNormal(final Graph g, final Node user, final long time, final double lat, final double lng) {
        user.travelInTime(time, new Callback<Node>() {
            @Override
            public void on(Node result) {
                result.set(LAT, Type.DOUBLE, lat);
                result.set(LNG, Type.DOUBLE, lng);
                result.free();
            }
        });
    }

    private static void setLatLng(final Graph g, final Node user, final long time, final double lat, final double lng) {
        setLanLngNormal(g, user, time, lat, lng);
        //setLatLngPoly(g, user, time, lat, lng); I will disable this for a mement just to test
    }


    private static Node createUser(Graph g, String userFolderId) {
        // we start by creating a new user Node in the graph
        Node user1 = g.newNode(0, 0);
        user1.set("folderId",Type.STRING,userFolderId);

        g.index(0, 0, USERS_INDEX, new Callback<NodeIndex>() {
            @Override
            public void on(NodeIndex result) {
                result.addToIndex(user1);
            }
        });

        //here i add the 2 polynomial nodes needed for extrapolation for the lat and lng
        Node userPolyLat = g.newTypedNode(0, 0, PolynomialNode.NAME);
        userPolyLat.set(PolynomialNode.PRECISION, Type.DOUBLE, 0.000001);
        userPolyLat.set(PolynomialNode.MAX_DEGREE, Type.INT, 1);

        Node userPolyLng = g.newTypedNode(0, 0, PolynomialNode.NAME);
        userPolyLng.set(PolynomialNode.PRECISION, Type.DOUBLE, 0.000001);
        userPolyLng.set(PolynomialNode.MAX_DEGREE, Type.INT, 1);


        //I add them to the main user
        user1.addToRelation(LATEXTRAP, userPolyLat);
        user1.addToRelation(LNGEXTRAP, userPolyLng);

        userPolyLat.free();
        userPolyLng.free();

        //and I return the main user
        return user1;
    }


    public void start() {

        final Graph g = new GraphBuilder()
                .withMemorySize(1000000)
                .withPlugin(new MLPlugin())
                .withPlugin(new ImporterPlugin())
                .withStorage(new LevelDBStorage(LEVEL_DB))
                .build();
        g.connect(connectionResult -> {


            //Just one second, I am checking with francois on the name of the files
            //we can create in the framework a task that gets this automatically
            //this will be asesome, otherwise I gave to change the code everytime if I have new dataset
            //no basically this code that i will paste here, should be pasted in kmf


            Task readFileTask = newTask()
                    .then(ImporterActions.readFiles("{{result}}"))
                    .forEach(newTask()
                            .thenDo(new ActionFunction() {
                                @Override
                                public void eval(TaskContext context) {
                                    //we start by getting the path of the folder
                                    String path = (String) context.result().get(0);
                                    //we do a substring to get the user id
                                    String userID = path.substring(path.lastIndexOf("/") + 1);
                                    //we create a user from this id
                                    Node user = createUser(context.graph(), userID);


                                    System.out.println("Loading data for user: "+userID+", memory: "+context.graph().space().available()+", loaded so far: "+context.variable("dataload").get(0)+" timepoints");

                                    //Then I save in the task context, the path, the user ID, and the user node
                                    context.setVariable("path", path);
                                    context.setVariable("userID", userID);
                                    context.setVariable("user", user);

                                    context.continueWith(context.wrap(path + "/Trajectory/")); //Ugly hack to directly reach the foler
                                    //not to waste time

                                }
                            })
                            .then(ImporterActions.readFiles("{{result}}"))
                            .forEach(newTask()
                                    .then(ImporterActions.readLines("{{result}}"))
                                    .forEach(newTask()
                                            .thenDo(new ActionFunction() {
                                                @Override
                                                public void eval(TaskContext ctx) {
                                                    String res= (String) ctx.result().get(0);
                                                    int numOfLine= (int) ctx.variable("i").get(0);

                                                    if(numOfLine>=6){
                                                        String[] substr= res.split(","); //split the string by comma

                                                        Node user= (Node) ctx.variable("user").get(0);

                                                        double lat=Double.parseDouble(substr[0]);
                                                        double lng=Double.parseDouble(substr[1]);

                                                        String dateStr = substr[5]+" "+substr[6];
                                                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                                                        LocalDateTime dateTime = LocalDateTime.parse(dateStr, formatter);

                                                        ZoneOffset zoneOffset = ZoneId.of("GMT").getRules().getOffset(dateTime);

                                                        long timestamp = dateTime.toEpochSecond(zoneOffset);

                                                    

                                                        setLatLng(ctx.graph(),user,timestamp,lat,lng);

                                                        int globalCounter=(int) ctx.variable("dataload").get(0);
                                                        globalCounter++;
                                                        ctx.setGlobalVariable("dataload",globalCounter);




                                                    }
                                                    ctx.continueTask();
                                                }
                                            })
                                    )
                                    .then(save())

                            )
                    )
                    .then(readVar("dataload"));


            TaskContext ctx=readFileTask.prepare(g, DATA_DIR, new Callback<TaskResult>() {
                @Override
                public void on(TaskResult result) {
                    System.out.println("Loaded: "+result.get(0)+" lines of data");
                    result.free();
                }
            });

            ctx.setGlobalVariable("dataload",0);
            readFileTask.executeUsing(ctx);

            //the server will be listening at this port 9011
            WSServer graphServer = new WSServer(g, 9011);
            graphServer.start();

            RESTManager rest = new RESTManager();
            rest.start(g);
        });
    }

    private void loadFromFile(String csvfile, Node user) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(csvfile));
            String line;
            int count = 0;
            String[] attributes = null;
            while ((line = br.readLine()) != null) {
                String[] cells = line.split(";");
                if (count == 0) {
                    attributes = cells;
                } else {
                    long time = Long.parseLong(cells[0]);

                    String[] finalAttributes = attributes;
                    user.travelInTime(time, new Callback<Node>() {
                        @Override
                        public void on(Node result) {
                            for (int i = 1; i < cells.length; i++) {
                                if (!cells[i].equals("")) {
                                    try {
                                        double d = Double.parseDouble(cells[i]);
                                        result.set(finalAttributes[i], Type.DOUBLE, d);
                                    } catch (Exception ex) {
                                        //result.setProperty(finalAttributes[i], Type.STRING, cells[i]);
                                    }
                                }
                            }
                            result.free();
                        }
                    });
                }
                count++;
            }
            System.out.println("read: " + count + " lines");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    public static void main(String[] args) {
        BackendRunner runner = new BackendRunner();
        runner.start();
    }

}
