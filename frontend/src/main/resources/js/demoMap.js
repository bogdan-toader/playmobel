var DemoMap = function () {

    var LAT = "lat";
    var LNG = "lng";

    var mymap;
    var userPositionMarker;
    var filterCircle;
    var markers;
    var graph;


    var init = function () {

        //here the graph is connected at the same port
        graph = new org.mwg.GraphBuilder()
            .withStorage(new org.mwg.plugin.WSClient("ws://" + window.location.hostname + ":9011"))
            .withPlugin(new org.mwg.structure.StructurePlugin())
            .withPlugin(new org.mwg.ml.MLPlugin()).build();
        graph.connect(function () {

        });

        initMap();
    };


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
            }
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

        var extrapolation = document.querySelector("[name=field_extrapolation]").checked;
        if (extrapolation) {
            console.log("extrapolation mode");
            var context = testNavigationPoly.prepare(graph, null, function (result) {
                result.free();
            });
            context.setVariable("processTime", selectedTime);
            testNavigationPoly.executeUsing(context);
        }
        else {
            console.log("normal mode");
            var context = testNavigationNormal.prepare(graph, null, function (result) {
                result.free();
            });
            context.setVariable("processTime", selectedTime);
            testNavigationNormal.executeUsing(context);
        }
    }

    var actions = org.mwg.core.task.Actions;

    var testNavigationNormal = org.mwg.core.task.Actions.newTask()
        .then(actions.travelInTime("{{processTime}}"))  //here we navigate in the requested time
        .then(actions.readGlobalIndex("users"))    //we read the index of all users
        .forEach(actions.newTask()  //for each user
            .then(actions.defineAsVar("user"))          //save the user
            .then(actions.attribute(LAT))                      //get the lat
            .then(actions.defineAsVar("lat"))           //save the lat
            .then(actions.readVar("user"))              //reload the user
            .then(actions.attribute(LNG))                     //get the lng
            .then(actions.defineAsVar("lng"))           //save the lng
            .thenDo(function (context) {
                console.log(context.variable("lat").get(0) + "," + context.variable("lng").get(0));
                filterCircle.setLatLng([context.variable("lat").get(0), context.variable("lng").get(0)]);
                userPositionMarker.setLatLng([context.variable("lat").get(0), context.variable("lng").get(0)]);
                context.continueTask();
            })
        );

    var testNavigationPoly = org.mwg.core.task.Actions.newTask()
        .then(actions.travelInTime("{{processTime}}"))  //here we navigate in the requested time
        .then(actions.readGlobalIndex("users"))    //we read the index of all users
        .forEach(actions.newTask()  //for each user
            .then(actions.defineAsVar("user"))          //save the user
            .action("readContinuous", "latextrap")
            .then(actions.defineAsVar("lat"))           //save the lat
            .then(actions.readVar("user"))              //reload the user
            .action("readContinuous", "lngextrap")
            .then(actions.defineAsVar("lng"))           //save the lng
            .thenDo(function (context) {
                //here the context will have the user, the lat and the lng at the requested process time ok
                //alert(context.variable("lat") + " , " + context.variable("lng"));
                //here are you following? yeess]s
                console.log(context.variable("lat").get(0) + "," + context.variable("lng").get(0));
                filterCircle.setLatLng([context.variable("lat").get(0), context.variable("lng").get(0)]);
                userPositionMarker.setLatLng([context.variable("lat").get(0), context.variable("lng").get(0)]);
                context.continueTask();
            })
        );


    //You see now the front end is reading from the server, the location of the user, instead of doing an alert, i will update a
    // Is it easy? woooow man how cool it is! :D.
    //Now you see with a normal Node, it jumps from one point to another
    //because from 0->10 it resolves one node, from 10->20 another etc
    //is this clear why the point jumps on the map ? yessss
    ///Now will add a Machine leanring extrapolation between points


    return {
        init: init,
        updateTime: updateTime,
        graph: getGraph
    };
};
