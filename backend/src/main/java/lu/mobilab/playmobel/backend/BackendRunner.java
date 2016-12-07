package lu.mobilab.playmobel.backend;

import org.mwg.*;
import org.mwg.ml.MLPlugin;
import org.mwg.ml.algorithm.regression.PolynomialNode;
import org.mwg.ml.algorithm.regression.actions.SetContinuous;
import org.mwg.task.ActionFunction;
import org.mwg.task.Task;
import org.mwg.task.TaskContext;
import org.mwg.task.TaskResult;

import java.io.BufferedReader;
import java.io.FileReader;

import static org.mwg.core.task.Actions.*;


/**
 * Created by bogdan.toader on 24/10/16.
 */
public class BackendRunner {
    public final static String LAT = "lat";
    public final static String LNG = "lng";

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
            }
        });
    }

    public void start() {

        final Graph g = new GraphBuilder()
                .withMemorySize(1000000)
                .withPlugin(new MLPlugin())
                .build();
        g.connect(connectionResult -> {


            Node userPolyLat = g.newTypedNode(0, 0, PolynomialNode.NAME);
            userPolyLat.set(PolynomialNode.PRECISION, Type.DOUBLE, 0.0001);
            Node userPolyLng = g.newTypedNode(0, 0, PolynomialNode.NAME);
            userPolyLng.set(PolynomialNode.PRECISION, Type.DOUBLE, 0.0001);


            Node user1 = g.newNode(0, 0);
            g.index(0, 0, "users", new Callback<NodeIndex>() {
                @Override
                public void on(NodeIndex result) {
                    result.addToIndex(user1);
                }
            });


            user1.addToRelation("latextrap", userPolyLat);
            user1.addToRelation("lngextrap", userPolyLng);

//            String csvfile = "./bogdantoader.csv";
//            loadFromFile(csvfile, user1);

            setLanLngNormal(g, user1, 0, 49.632386, 6.168544);
            setLanLngNormal(g, user1, 10, 49.732386, 6.268544);
            setLanLngNormal(g, user1, 20, 49.832386, 6.368544);
            setLanLngNormal(g, user1, 30, 49.932386, 6.468544);
            setLanLngNormal(g, user1, 100, 51.932386, 7.468544);


            setLatLngPoly(g, user1, 0, 49.632386, 6.168544);
            setLatLngPoly(g, user1, 10, 49.732386, 6.268544);
            setLatLngPoly(g, user1, 20, 49.832386, 6.368544);
            setLatLngPoly(g, user1, 30, 49.932386, 6.468544);
            setLatLngPoly(g, user1, 100, 51.932386, 7.468544);


            Task testNavigation = newTask()
                    .println("{{processTime}}")
                    .travelInTime("{{processTime}}")  //Ah, it's ok, i just misused functions
                    .readGlobalIndex("users")   //we read the index of all users
                    .forEach(newTask()  //for each user
                            .defineAsVar("user")         //save the user
                            .println("{{result}}") //I just found a bug in kmf :D :D heheheh
                            //the index is not forwarding the time check: now the time is correct
                            .attribute(LAT)                      //get the lat
                            .defineAsVar("lat")           //save the lat
                            .readVar("user")              //reload the user
                            .attribute(LNG)                     //get the lng
                            .defineAsVar("lng")          //save the lng
                            .thenDo(new ActionFunction() {
                                @Override
                                public void eval(TaskContext taskContext) {
                                    System.out.println(taskContext.variable("lat").get(0));
                                    System.out.println(taskContext.variable("lng").get(0));
                                    taskContext.continueTask();
                                }
                            })
                    );

            TaskContext context = testNavigation.prepare(g, null, taskResult -> {
                taskResult.free();

            });
            context.setVariable("processTime", System.currentTimeMillis());
            testNavigation.executeUsing(context);


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
