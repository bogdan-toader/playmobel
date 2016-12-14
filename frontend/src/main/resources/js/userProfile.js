var UserProfile = function () {

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


    //a global variable for the selected time :)
    var timestamp=1224730384000;

    var initFlatPickr = function () {
        flatpickr = document.querySelector(".flatpickr").flatpickr({
            utc: true,
            defaultDate: "2008-10-23T02:53:04Z",
            enableTime: true,
            enableSeconds: true,
            onChange: function (dateObject, dateString) {
                var selectedDateTime=dateObject[0];
                timestamp= getTimeStamp(selectedDateTime.getFullYear(),selectedDateTime.getMonth(),selectedDateTime.getDate(),selectedDateTime.getHours(),selectedDateTime.getMinutes(),selectedDateTime.getSeconds());

            }
        });
    };


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
            initUserList();
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



    function userchange() {
        var dropownlist = document.getElementById("userDropDown");

        if (dropownlist.selectedIndex != null) {
            var selected = dropownlist.options[dropownlist.selectedIndex].text;


            var date = new Date();
            date.setTime(timestamp);
            alert(timestamp+" day: "+date.getUTCDay()+" hour: "+date.getUTCHours());


            var context = getProfile.prepare(graph, null, function (result) {
                result.free();
            });
            context.setVariable("processTime", timestamp);
            context.setVariable("selectedUser", selected);
            //getProfile.executeUsing(context);
        }

    }


    var actions = org.mwg.core.task.Actions;

    var getProfile = org.mwg.core.task.Actions.newTask()
        .then(actions.travelInTime("{{processTime}}"))
        .then(actions.readGlobalIndex("users"))
        .forEach(actions.newTask()
            .thenDo(function (context) {
                var user = context.result().get(0);
                if (context.variable("selectedUser").get(0) == user.get("folderId")) {
                    context.continueTask();
                }
                context.continueWith(null);
            })
        )
        .then(actions.println("{{result}}"));


    var loadAllusers = org.mwg.core.task.Actions.newTask()
        .then(actions.travelInTime("{{processTime}}"))
        .then(actions.readGlobalIndex("users"))
        .forEach(actions.newTask()
            .thenDo(function (context) {
                var dropownlist = document.getElementById("userDropDown");
                var user = context.result().get(0);
                var option = document.createElement('option');
                option.text = user.get("folderId");
                option.value = user.id();
                dropownlist.add(option, 0);
                context.continueTask();
            })
        );

    var initUserList = function () {
        var context = loadAllusers.prepare(graph, null, function (result) {
            result.free();
        });
        context.setVariable("processTime", timestamp);
        loadAllusers.executeUsing(context);
    }

    return {
        init: init,
        graph: getGraph,
        userchange: userchange
    };
};
