var DemoMap = function () {

    var LAT = "lat";
    var LNG = "lng";

    var mymap;
    var userPositionMarker;
    var filterCircle;
    var markers;
    var graph;


    var init = function () {

        // how to connect to MWG
        // the same graph in kmf can be connected from JS side, and from server side
        // this is very important to execute tasks on server and see the results on client

        //let's do a simple example
        // i will load a coordinate of a user on the server side, and here when we connect we will retrieve them
        //on client side
        //let's load a csv
        //man one more time, my goal is not to develop you the whole project,
        // just to show you how to use and you have to develop the rest
        ////o kit takes time in the beginning to learn, but after you can do everything in 1 min

        //here the graph is connected at the same port
        graph = new org.mwg.GraphBuilder()
            .withStorage(new org.mwg.plugin.WSClient("ws://" + window.location.hostname + ":9011"))
            .withPlugin(new org.mwg.structure.StructurePlugin())
            .withPlugin(new org.mwg.ml.MLPlugin()).build();
        graph.connect(function () {

        });

        initMap();
    };


// this is a simple map front end in JS,
// now your goal, is to import csv in KMF
// and here to connect to graph
//you can create a slider that changes in time
// and execute a kmf task to get coordinates of the user
//then you will have a full server + front end running

    var initMap = function () {
        //PARIS: 48.8523947,2.3462913
        //Luxembourg: 49.632386, 6.168544
        mymap = L.map('mapid').setView([49.632386, 6.168544], 12);
        L.tileLayer('https://api.mapbox.com/styles/v1/mapbox/streets-v9/tiles/256/{z}/{x}/{y}?access_token={accessToken}', {
            attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery © <a href="http://mapbox.com">Mapbox</a>',
            maxZoom: 18,
            id: 'test',
            accessToken: 'pk.eyJ1IjoiYm9nZGFudG9hZGVyIiwiYSI6ImNpd2RpejNkMjAwOW0yeWs0NTJ2c2Uxcm4ifQ.PrdepIo60YlnfnqQv4FWug' //replace your token here look here

        }).on('load', function (e) {
            //document.querySelector("#map_init").textContent = document.querySelector("#map_init").textContent + "... Done !";
        }).addTo(mymap);

        mymap.on('click', function (event) {
            //updateNearest(e);
            userPositionMarker.setLatLng([event.latlng.lat, event.latlng.lng]);
            filterCircle.setLatLng([event.latlng.lat, event.latlng.lng]);
            updateFilter();
        });

        markers = new L.layerGroup();
        markers.addTo(mymap);

        initiateUserPosition();

    };

    var initiateUserPosition = function () {
        //Luxembourg: 49.632386, 6.168544
        //PARIS: 48.8344884,2.3716972
        addUserPositionMarker(49.632386, 6.168544);
        addFilterCircle(49.632386, 6.168544)
        //}
    };

    var addUserPositionMarker = function (lat, lng) {
        userPositionMarker = L.marker([lat, lng], {
            icon: L.icon({
                iconUrl: 'img/red_pin.png',
                iconAnchor: [15, 44],
                iconSize: [30, 44],
            }),
            draggable: true,
            zIndexOffset: 1000
        }).bindPopup("Your position").addTo(mymap);

        userPositionMarker["lastDrag"] = (new Date()).getTime();

        userPositionMarker.on("drag", function (event) {
            filterCircle.setLatLng([event.latlng.lat, event.latlng.lng]);
            if (((new Date()).getTime() - userPositionMarker.lastDrag) > 100) { // threshold
                userPositionMarker["lastDrag"] = (new Date()).getTime();
                updateFilter();
            }
        });

        userPositionMarker.on("dragend", function (event) {
            updateFilter();
        });

    };
    var addFilterCircle = function (lat, lng) {
        filterCircle = L.circle([lat, lng], 2000).addTo(mymap);
    };

    var getGraph = function () {
        return graph;
    };

    function updateTime() {
        var form = document.querySelector("#filter_form");
        var selectedTime = form.querySelector("[name=field_time]").value;
        //alert(selectedTime);
        //As you see at this point, here we have the selected time in variable reaching from the client side
        // the only remaining task is to get gps coordinated from the server side
        //for this we create a task ok

        var context = testNavigation.prepare(graph, null, function (result) {
            result.free();
        });

        context.setVariable("processTime", selectedTime);
        console.log("before executing");
        testNavigation.executeUsing(context);


    }

    var testNavigation = org.mwg.core.task.Actions.newTask()
        .then(org.mwg.core.task.Actions.print("{{processTime}}"))
        .then(org.mwg.core.task.Actions.setTime("{{processTime}}"))  //here we navigate in the requested time
        .then(org.mwg.core.task.Actions.readGlobalIndex("users", ""))    //we read the index of all users
        .then(org.mwg.core.task.Actions.print("{{result}}"))
        .forEach(org.mwg.core.task.Actions.newTask()  //for each user
            .then(org.mwg.core.task.Actions.defineAsVar("user"))          //save the user
            .then(org.mwg.core.task.Actions.attribute(LAT))                      //get the lat
            .then(org.mwg.core.task.Actions.defineAsVar("lat"))           //save the lat
            .then(org.mwg.core.task.Actions.readVar("user"))              //reload the user
            .then(org.mwg.core.task.Actions.attribute(LNG))                     //get the lng
            .then(org.mwg.core.task.Actions.defineAsVar("lng"))           //save the lng
            .thenDo(function (context) {
                //here the context will have the user, the lat and the lng at the requested process time ok
                alert(context.variable("lat") + " , " + context.variable("lat"));
                console.log(context.variable("lat"));
                context.continueTask();
            })
        );

    return {
        init: init,
        updateTime: updateTime,
        graph: getGraph
    };
};
