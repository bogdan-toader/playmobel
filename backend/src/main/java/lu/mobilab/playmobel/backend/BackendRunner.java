package lu.mobilab.playmobel.backend;

import org.mwg.*;
import org.mwg.task.ActionFunction;
import org.mwg.task.Task;
import org.mwg.task.TaskContext;

import java.io.BufferedReader;
import java.io.FileReader;

import static org.mwg.core.task.Actions.*;

/**
 * Created by bogdan.toader on 24/10/16.
 */
public class BackendRunner {
    public final static String LAT = "lat";
    public final static String LNG = "lng";

    public void start() {

        final Graph g = new GraphBuilder().withMemorySize(1000000).build();
        g.connect(connectionResult -> {
            Node user1 = g.newNode(0, 0);
            g.index(0, 0, "users", new Callback<NodeIndex>() {
                @Override
                public void on(NodeIndex result) {
                    result.addToIndex(user1);
                }
            });
//            String csvfile = "./bogdantoader.csv";
//            loadFromFile(csvfile, user1);


            user1.travelInTime(0, new Callback<Node>() {
                @Override
                public void on(Node result) {
                    user1.set(LAT, Type.DOUBLE, 49.632386);
                    user1.set(LNG, Type.DOUBLE, 6.168544);
                }
            });

            user1.travelInTime(10, new Callback<Node>() {
                @Override
                public void on(Node result) {
                    user1.set(LAT, Type.DOUBLE, 49.612386);
                    user1.set(LNG, Type.DOUBLE, 6.164544);
                }
            });

            user1.travelInTime(20, new Callback<Node>() {
                @Override
                public void on(Node result) {
                    user1.set(LAT, Type.DOUBLE, 49.592386);
                    user1.set(LNG, Type.DOUBLE, 6.163544);
                }
            });

            user1.travelInTime(30, new Callback<Node>() {
                @Override
                public void on(Node result) {
                    user1.set(LAT, Type.DOUBLE, 49.632386);
                    user1.set(LNG, Type.DOUBLE, 6.162544);
                }
            });


            Task testNavigation = org.mwg.core.task.Actions.newTask()
                    .then(println("{{processTime}}"))
                    .then(org.mwg.core.task.Actions.setTime("{{processTime}}"))  //Ah, it's ok, i just misused functions
                    .then(org.mwg.core.task.Actions.readGlobalIndex("users", ""))    //we read the index of all users
                    .forEach(org.mwg.core.task.Actions.newTask()  //for each user
                            .then(org.mwg.core.task.Actions.defineAsVar("user"))          //save the user
                            .then(println("{{result}}")) //I just found a bug in kmf :D :D heheheh
                            //the index is not forwarding the time check: now the time is correct
                            .then(org.mwg.core.task.Actions.attribute(LAT))                      //get the lat
                            .then(org.mwg.core.task.Actions.defineAsVar("lat"))           //save the lat
                            .then(org.mwg.core.task.Actions.readVar("user"))              //reload the user
                            .then(org.mwg.core.task.Actions.attribute(LNG))                     //get the lng
                            .then(org.mwg.core.task.Actions.defineAsVar("lng"))           //save the lng
                            .thenDo(new ActionFunction() {
                                @Override
                                public void eval(TaskContext context) {
                                    System.out.println(context.variable("lat").get(0));
                                    System.out.println(context.variable("lng").get(0));
                                    context.continueTask();
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
