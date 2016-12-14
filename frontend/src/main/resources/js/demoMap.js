var DemoMap = function () {

    var LAT = "lat";
    var LNG = "lng";

    var DEFAULT_LAT = 49.632386;
    var DEFAULT_LNG = 6.168544;

    var mymap; //the map object
    var graph; //the kmf graph
    var flatpickr; //the timedate selector
    var markers; //all the user markers


    function getTimeStamp(year, month, day, hour, min, sec) {
        var parsedUnixTime = new Date();
        parsedUnixTime.setUTCFullYear(year);
        parsedUnixTime.setUTCMonth(month);
        parsedUnixTime.setUTCDate(day);
        parsedUnixTime.setUTCHours(hour);
        parsedUnixTime.setUTCMinutes(min);
        parsedUnixTime.setUTCSeconds(sec);
        parsedUnixTime.setUTCMilliseconds(0);
        return parsedUnixTime.getTime();
    }


    var initFlatPickr = function () {
        flatpickr = document.querySelector(".flatpickr").flatpickr({
            defaultDate: "2008-10-23T02:53:04",
            enableTime: true,
            enableSeconds: true,
            onChange: function (dateObject, dateString) {
                updateTime();
            }
        });
    }


    var initMap = function () {
        //PARIS: 48.8523947,2.3462913
        //Luxembourg: 49.632386, 6.168544
        //China:39.984702,116.318417
        mymap = L.map('mapid').setView([39.984702, 116.318417], 7);
        L.tileLayer('https://api.mapbox.com/styles/v1/mapbox/streets-v9/tiles/256/{z}/{x}/{y}?access_token={accessToken}', {
            attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery © <a href="http://mapbox.com">Mapbox</a>',
            maxZoom: 18,
            id: 'test',
            accessToken: 'pk.eyJ1IjoiYm9nZGFudG9hZGVyIiwiYSI6ImNpd2RpejNkMjAwOW0yeWs0NTJ2c2Uxcm4ifQ.PrdepIo60YlnfnqQv4FWug'

        }).on('load', function (e) {
            //document.querySelector("#map_init").textContent = document.querySelector("#map_init").textContent + "... Done !";
        }).addTo(mymap);

        markers = new L.markerClusterGroup();
        markers.addTo(mymap);
    };


    var init = function () {

        initFlatPickr();
        initMap();

        //here the graph is connected at the same port
        graph = new org.mwg.GraphBuilder()
            .withStorage(new org.mwg.plugin.WSClient("ws://" + window.location.hostname + ":9011"))
            .withPlugin(new org.mwg.structure.StructurePlugin())
            .withPlugin(new org.mwg.ml.MLPlugin()).build();
        graph.connect(function () {
            updateTime();

        });


    };


    function addMarker(nodeID, lat, lng, context) {
        var marker = L.marker([lat, lng])
            .bindPopup("user Id: " + nodeID);
        marker["node"] = nodeID;
        marker.on('click', function (e) {
            alert("user Id: " + e.target.node + " (lat,lng): " + e.latlng.lat + "," + e.latlng.lng);
        });
        markers.addLayer(marker);
        context.continueTask();
    };


    var getGraph = function () {
        return graph;
    };

    function getSelectedTime() {
        var dateObject = flatpickr.selectedDateObj;
        var timestamp = getTimeStamp(dateObject.getFullYear(), dateObject.getMonth(), dateObject.getDate(), dateObject.getHours(), dateObject.getMinutes(), dateObject.getSeconds());
        return timestamp;
    }

    function updateTime() {
        var timestamp = getSelectedTime();
        var context = createAllUsersTask.prepare(graph, null, function (result) {
            result.free();
        });
        context.setVariable("processTime", timestamp);
        createAllUsersTask.executeUsing(context);

    }

    var actions = org.mwg.core.task.Actions;

    var createAllUsersTask = org.mwg.core.task.Actions.newTask()
            .thenDo(function (context) {
                context.addToGlobalVariable("startTime", performance.now());
                context.addToGlobalVariable("counter", 0);
                markers.clearLayers();
                context.continueTask();
            })
            .then(actions.travelInTime("{{processTime}}"))
            .then(actions.readGlobalIndex("users"))
            .thenDo(function (context) {
                context.continueTask();
            })
            .forEach(actions.newTask()
                .thenDo(function (context) {
                    var user = context.result().get(0);
                    var lat = user.get("lat");
                    var lng = user.get("lng");
                    var userID = user.get("folderId");
                    if (lat != null && lng != null) {
                        var counter = context.variable("counter").get(0) + 1;
                        context.setGlobalVariable("counter", counter);
                        addMarker(userID, lat, lng, context);
                    }
                    else {
                        context.continueTask();
                    }
                })
            )
            .thenDo(function (context) {
                var endtime = performance.now();
                var starttime = context.variable("startTime").get(0);
                var counter= context.variable("counter").get(0);

                var processtime = endtime - starttime;
                document.getElementById('messagelbl').innerHTML = "loaded "+counter+" users in: " + parseFloat(processtime).toFixed(2)+ " ms";
                context.continueTask();
            })
        ;


    return {
        init: init,
        updateTime: updateTime,
        graph: getGraph
    };
};
