package lu.mobilab.playmobel.backend.util;

import org.mwg.Callback;
import org.mwg.Graph;
import org.mwg.Node;
import org.mwg.WSClient;
import org.mwg.ml.algorithm.profiling.GaussianMixtureNode;
import org.mwg.ml.algorithm.profiling.ProbaDistribution;
import org.mwg.task.ActionFunction;
import org.mwg.task.Task;
import org.mwg.task.TaskContext;
import org.mwg.task.TaskResult;

import static org.mwg.core.task.Actions.*;

/**
 * Created by assaad on 14/12/2016.
 */
public class TestProfile {
    public static void main(String[] arg){
        Graph graph = new org.mwg.GraphBuilder()
                .withStorage(new WSClient("ws://localhost:9011"))
                .withPlugin(new org.mwg.structure.StructurePlugin())
                .withPlugin(new org.mwg.ml.MLPlugin()).build();
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                Task getProfile = org.mwg.core.task.Actions.newTask()
                        .then(travelInTime("{{processTime}}"))
                        .then(readGlobalIndex("users", "folderId", "{{selectedUser}}"))
                        .then(setAsVar("user"))
                        .then(traverse("{{profiler}}"))
                        .thenDo(new ActionFunction() {
                            @Override
                            public void eval(TaskContext ctx) {
                                Node user=(Node) ctx.variable("user").get(0);
                                GaussianMixtureNode gmm= (GaussianMixtureNode) ctx.result().get(0);
                                gmm.generateDistributions(0, new Callback<ProbaDistribution>() {
                                    @Override
                                    public void on(ProbaDistribution result) {

                                        int x=0;
                                    }
                                });

                                ctx.continueTask();

                            }
                        });


                TaskContext tc=getProfile.prepare(graph, null, new Callback<TaskResult>() {
                    @Override
                    public void on(TaskResult result) {
                        result.free();
                    }
                });

                tc.setGlobalVariable("processTime",1224730384000l);
                tc.setGlobalVariable("selectedUser","001");
                tc.setGlobalVariable("profiler","profiler55");

                getProfile.executeUsing(tc);

            }
        });
    }
}
