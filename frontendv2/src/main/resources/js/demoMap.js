var DemoMap = function () {
    var mymap; //the map object
    var flatpickr; //the timedate selector
    var markers; //all the user markers

    var timestamp = 1224730384000;
    timestamp = Date.now(); //Set to now

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
            defaultDate: timestamp,
            enableTime: true,
            enableSeconds: true,
            onChange: function (dateObject, dateString) {
                var selectedDateTime = dateObject[0];
                timestamp = getTimeStamp(selectedDateTime.getFullYear(), selectedDateTime.getMonth(), selectedDateTime.getDate(), selectedDateTime.getHours(), selectedDateTime.getMinutes(), selectedDateTime.getSeconds());
                updateTime();
            }
        });
    }


    var initMap = function () {
        //PARIS: 48.8523947,2.3462913
        //Luxembourg: 49.632386, 6.168544
        //China:39.984702,116.318417
        mymap = L.map('mapid').setView([49.632386, 6.168544], 7);
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

        updateTime();
    };


    function addMarker(nodeID, lat, lng) {
        var marker = L.marker([lat, lng])
            .bindPopup("user: " + nodeID + " (lat,lng): " + lat + "," + lng);
        marker["node"] = nodeID;
        markers.addLayer(marker);
    };


    function updateTime() {
        var starttime = performance.now();
        var xmlhttp = new XMLHttpRequest();
        xmlhttp.onreadystatechange = function () {
            if (xmlhttp.readyState == XMLHttpRequest.DONE) {
                if (xmlhttp.status == 200) {
                    var resultJson = JSON.parse(xmlhttp.responseText);
                    markers.clearLayers();
                    for (var i = 0; i < resultJson.length; i++) {
                        addMarker(resultJson[i].userId,resultJson[i].lat,resultJson[i].lng);
                    }
                    var processtime =  performance.now() - starttime;
                    document.getElementById('messagelbl').innerHTML = "loaded "+resultJson.length+" users in: " + parseFloat(processtime).toFixed(2)+ " ms";
                }
                else if (xmlhttp.status == 400) {
                    alert('There was an error 400');
                }
                else {
                    alert('something else other than 200 was returned');
                }
            }
        };

        var params="timestamp="+timestamp;
        xmlhttp.open("GET", "http://" + window.location.hostname + ":8081/getPositions?"+ encodeURI(params), true);
        xmlhttp.send();
    }


    return {
        init: init,
        updateTime: updateTime
    };
};
