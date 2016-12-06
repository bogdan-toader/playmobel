

var DemoMap = function () {

    var mymap;
    var userPositionMarker;
    var filterCircle;
    var markers;
    var graph;


    var init = function () {

        // how to connect to MWG
        // graph = new org.mwg.GraphBuilder()
        //     .withStorage(new org.mwg.plugin.WSClient("ws://" + window.location.hostname + ":9011"))
        //     .withPlugin(new org.mwg.structure.StructurePlugin())
        //     .withPlugin(new org.mwg.ml.MLPlugin()).
        //     build();
        // graph.connect(function () {
        // });

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
            accessToken: 'pk.eyJ1IjoiZ25haW4iLCJhIjoiY2lzbG96eWZwMDA3NzJucGtwMTd5bXh2MiJ9.tJUI9PFDrl7eENeVW9kaWw'
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

    return {
        init: init
    };
};
