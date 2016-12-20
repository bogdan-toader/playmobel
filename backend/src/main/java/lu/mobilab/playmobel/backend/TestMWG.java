package lu.mobilab.playmobel.backend;

import org.mwg.*;
import org.mwg.task.*;

import java.util.Random;

import static org.mwg.core.task.Actions.newTask;

/**
 * Created by assaad on 20/12/2016.
 */
public class TestMWG {

    public final static String LEVEL_DB = "/Users/assaad/Desktop/kluster/Geolife Trajectories 1.3/leveldb/";

    public static void main(String[] arg) {
        Graph graph = GraphBuilder.newBuilder()
                .withMemorySize(4000000)
                .withStorage(new LevelDBStorage(LEVEL_DB))
                .build();
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {

                Random random = new Random(123);
                int insert = 1000000;
                double[] lat = new double[insert];
                double[] lng = new double[insert];

                for (int i = 0; i < insert; i++) {
                    lat[i] = random.nextDouble() * 180 - 90;
                    lng[i] = random.nextDouble() * 360 - 180;
                }


                Node node = graph.newNode(0, 0);


                long ts = System.currentTimeMillis();
                for (int i = 0; i < insert; i++) {
                    int finalI = i;
                    node.travelInTime(i, new Callback<Node>() {
                        @Override
                        public void on(Node result) {
                            result.set("lat", Type.DOUBLE, lat[finalI]);
                            result.set("lng", Type.DOUBLE, lng[finalI]);
                            result.free();
                        }
                    });
                    if (i % 100000 == 0) {
                        long te = System.currentTimeMillis() - ts;
                        System.out.println(te + " ms");
                        graph.save(null);
                    }
                }
                long te = System.currentTimeMillis() - ts;
                System.out.println(te + " ms");


                Task loadsave = newTask()
                        .loop("0", insert-1 + "", newTask()
                                .travelInTime("{{i}}")
                                .thenDo(new ActionFunction() {
                                    @Override
                                    public void eval(TaskContext ctx) {
                                        int i = (int) ctx.variable("i").get(0);
                                        Node result = (Node) ctx.result().get(0);
                                        result.set("lat", Type.DOUBLE, lat[i]);
                                        result.set("lng", Type.DOUBLE, lng[i]);
                                        ctx.continueTask();

                                    }
                                })
                                .ifThen(new ConditionalFunction() {
                                    @Override
                                    public boolean eval(TaskContext ctx) {
                                        int i = (int) ctx.variable("i").get(0);
                                        return (i % 100000 == 0);
                                    }
                                }, newTask().println("saving").save())
                        );


                long finalTs = System.currentTimeMillis();
                loadsave.executeWith(graph, node, new Callback<TaskResult>() {
                    @Override
                    public void on(TaskResult result) {
                        result.free();
                        long te = System.currentTimeMillis() - finalTs;
                        System.out.println(te + " ms");
                    }
                });


            }
        });
    }
}
