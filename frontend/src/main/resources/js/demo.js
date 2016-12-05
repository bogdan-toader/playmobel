var Demo = function () {

    var graph;
    var attributes = [];

    var init = function () {
        graph = new org.mwg.GraphBuilder().withStorage(new org.mwg.plugin.WSClient("ws://" + window.location.hostname + ":9011")).build();
        graph.connect(function () {
            console.log("Graph connected successfully");
            initDemo1()
        });
    };


    var loadDataTask = org.mwg.task.Actions
        .setTime("{{processTime}}")
        .fromIndexAll("users")
        .properties()
        .foreach(org.mwg.task.Actions.then(function (context) {
            attributes.push(context.result().get(0));
            context.continueTask()
        }));

    function initDemo1() {

        var context = loadDataTask.prepareWith(graph, null, function (result) {
            result.free();
            console.log(attributes);
            createAttributesDropList();
        });
        context.setVariable("processTime", (new Date()).getTime());


        loadDataTask.executeUsing(context);

    }

    function createAttributesDropList() {
        var dropList = document.createElement("select");
        dropList.addEventListener('change', refreshPlot);
        attributes.forEach(function (att) {
            var option = document.createElement("option");
            option.setAttribute("value", att);
            option.textContent = att;
            dropList.appendChild(option);
        });
        document.querySelector("body").insertBefore(dropList, document.querySelector("div[plotscontainer]"));
    }





    function refreshPlot(event) {
        console.log(event);

        var xmlhttp = new XMLHttpRequest();

        xmlhttp.onreadystatechange = function() {
            if (xmlhttp.readyState == XMLHttpRequest.DONE ) {
                if (xmlhttp.status == 200) {

                    console.log(xmlhttp);
                    var resultJson = JSON.parse(xmlhttp.responseText);

                    var xvalues = resultJson[0].x;
                    for(var i = 0; i < xvalues.length; i++) {
                        xvalues[i] = new Date(xvalues[i]);
                    }

                    var TESTER = document.querySelector('div[demoPlot1] div[plot]');
                    var layout = {
                        margin: {
                            width: window.innerWidth-40,
                            l: 30,
                            r: 30,
                            b: 30,
                            t: 30,
                        },
                        showlegend: true,
                        plot_bgcolor: '#c7c7c7'
                    };
                    document.querySelector('div[demoPlot1] div[plotTitle]').textContent = event.srcElement.value;
                    Plotly.purge(TESTER);
                    Plotly.plot(TESTER, resultJson, layout);


                }
                else if (xmlhttp.status == 400) {
                    alert('There was an error 400');
                }
                else {
                    alert('something else other than 200 was returned');
                }
            }
        };

        xmlhttp.open("GET", "http://"+window.location.hostname + ":8081/demo1?attributeName=" + event.srcElement.value, true);
        xmlhttp.send();

    }


    function addPlot() {

    }


    return {
        init: init
    }
};


