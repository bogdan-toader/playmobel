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


    private HttpHandler demo1Handler = new HttpHandler() {
        @Override
        public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {


        }
    };


}
