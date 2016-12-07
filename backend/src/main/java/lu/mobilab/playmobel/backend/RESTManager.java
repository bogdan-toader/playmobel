package lu.mobilab.playmobel.backend;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import io.undertow.util.StatusCodes;
import org.mwg.Graph;
import org.mwg.task.Task;
import org.mwg.task.TaskContext;

import static org.mwg.core.task.Actions.*;


/**
 * Created by gnain on 17/10/16.
 */
public class RESTManager {

    private Undertow server;
    private Graph graph;


    public void start(Graph graph) {
        this.graph = graph;
        if (server == null) {
            server = Undertow.builder()
                    .addHttpListener(8081, "0.0.0.0")
                    .setHandler(Handlers.path()
                            .addPrefixPath("/demo1", demo1Handler)
                    )
                    .build();
        }
        server.start();

    }






    private Task demo1Task = newTask()
            .then(travelInTime("{{processTime}}"))
            .then(readGlobalIndex("users",""))
            .then(setAsVar("users"))
            .thenDo(context -> {
                context.resultAsNodes().get(0).timepoints(org.mwg.Constants.BEGINNING_OF_TIME, org.mwg.Constants.END_OF_TIME,
                        timepoints -> context.continueWith(context.wrap(timepoints)));
            }).forEach(newTask()
                    .then(setAsVar("timepoint"))
                    .then(readVar("users"))
                    .then(travelInTime("{{timepoint}}"))
                    .then(attribute("{{attName}}"))
                    .thenDo(context -> {
                        try {

                            long t = (long) context.variable("timepoint").get(0);
                            double d = (double) context.result().get(0);

                            if ((long) context.variable("timepoint").get(0) != org.mwg.Constants.BEGINNING_OF_TIME) {
                                JsonObject serie = (JsonObject) context.variable("dataSet").get(0);

                                ((JsonArray) serie.get("x")).add(t);
                                ((JsonArray) serie.get("y")).add(d);
                            }
                        } catch (Exception ex) {

                        }
                        context.continueTask();
                    })
            );


    private HttpHandler demo1Handler = new HttpHandler() {
        @Override
        public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {
            JsonArray result = new JsonArray();
            JsonObject serie = new JsonObject();
            serie.add("x", new JsonArray());
            serie.add("y", new JsonArray());
            result.add(serie);

            TaskContext context = demo1Task.prepare(graph, null, taskResult -> {

                httpServerExchange.getResponseHeaders().add(new HttpString("Access-Control-Allow-Origin"), "*");
                //httpServerExchange.getResponseHeaders().add(new HttpString("Access-Control-Allow-Methods"), "GET, POST, PUT, DELETE");

                httpServerExchange.setStatusCode(StatusCodes.OK);
                httpServerExchange.getResponseSender().send(result.toString());
            });
            context.setVariable("processTime", System.currentTimeMillis());
            context.setVariable("dataSet", serie);
            context.setVariable("attName", httpServerExchange.getQueryParameters().get("attributeName").getFirst());
            demo1Task.executeUsing(context);
        }
    };


}
