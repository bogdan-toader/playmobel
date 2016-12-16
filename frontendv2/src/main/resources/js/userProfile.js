var UserProfile = function () {

    var DAYS = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"];


    //then the shared variables accross the whole code
    var map; //the map object leaflet map
    var flatpickr; //the timedate selector
    var markers; //all the user markers
    var heatmap;//for heatmap for user
    var timestamp = 1224833400000; //the time stamp selected
    var selectedUser = "000"; //the selected user

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
            utc: true,
            defaultDate: timestamp,
            enableTime: true,
            enableSeconds: true,
            onChange: function (dateObject, dateString) {
                var selectedDateTime = dateObject[0];
                timestamp = getTimeStamp(selectedDateTime.getFullYear(), selectedDateTime.getMonth(), selectedDateTime.getDate(), selectedDateTime.getHours(), selectedDateTime.getMinutes(), selectedDateTime.getSeconds());
                userOrTimechange();

            }
        });
    };


    var initMap = function () {
        //PARIS: 48.8523947,2.3462913
        //Luxembourg: 49.632386, 6.168544
        //China:39.984702,116.318417

        var cfg = {
            // radius should be small ONLY if scaleRadius is true (or small radius is intended)
            // if scaleRadius is false it will be the constant radius used in pixels
            "radius": 40,

            "maxOpacity": .8,
            // scales the radius based on map zoom
            "scaleRadius": false,
            // if set to false the heatmap uses the global maximum for colorization
            // if activated: uses the data maximum within the current map boundaries
            //   (there will always be a red spot with useLocalExtremas true)
            "useLocalExtrema": false,
            // which field name in your data represents the latitude - default "lat"
            latField: 'lat',
            // which field name in your data represents the longitude - default "lng"
            lngField: 'lng',
            // which field name in your data represents the data value - default "value"
            valueField: 'count'
        };


        heatmap = new HeatmapOverlay(cfg);

        map = L.map('mapid').setView([39.984702, 116.318417], 7);
        L.tileLayer('https://api.mapbox.com/styles/v1/mapbox/streets-v9/tiles/256/{z}/{x}/{y}?access_token={accessToken}', {
            attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery © <a href="http://mapbox.com">Mapbox</a>',
            maxZoom: 18,
            id: 'test',
            accessToken: 'pk.eyJ1IjoiYm9nZGFudG9hZGVyIiwiYSI6ImNpd2RpejNkMjAwOW0yeWs0NTJ2c2Uxcm4ifQ.PrdepIo60YlnfnqQv4FWug'

        }).addTo(map);


        map.addLayer(heatmap);

        markers = new L.markerClusterGroup();
        markers.addTo(map);

    };


    var init = function () {
        initFlatPickr();
        initMap();
        initUserList();
    };

    var initUserList = function () {
        var starttime = performance.now();
        var xmlhttp = new XMLHttpRequest();
        xmlhttp.onreadystatechange = function () {
            if (xmlhttp.readyState == XMLHttpRequest.DONE) {
                if (xmlhttp.status == 200) {
                    var resultJson = JSON.parse(xmlhttp.responseText);
                    var dropownlist = document.getElementById("userDropDown");
                    for (var i = 0; i < resultJson.length; i++) {
                        var option = document.createElement('option');
                        option.value = option.text = resultJson[i];
                        dropownlist.add(option, 0);
                    }
                    var processtime = performance.now() - starttime;
                    document.getElementById('messagelbl').innerHTML = "loaded " + resultJson.length + " users in: " + parseFloat(processtime).toFixed(2) + " ms";
                }
                else if (xmlhttp.status == 400) {
                    alert('There was an error 400');
                }
                else {
                    alert('something else other than 200 was returned');
                }
            }
        };
        xmlhttp.open("GET", "http://" + window.location.hostname + ":8081/getUsers", true);
        xmlhttp.send();

    }


    function addMarker(nodeID, day, hour, lat, lng, probability) {
        var marker = L.marker([lat, lng])
            .bindPopup("user: "+nodeID+ ", "+day + " at " + hour + " o'clock, probability: " + Number(probability).toFixed(2) + "%");
        marker["node"] = nodeID;
        markers.addLayer(marker);
    }


    //When the drop down is changed we call this
    function dropDownChange() {
        var dropownlist = document.getElementById("userDropDown");
        if (dropownlist.options[dropownlist.selectedIndex].text != null) {
            selectedUser = dropownlist.options[dropownlist.selectedIndex].text;
            userOrTimechange();
        }
        else {
            selectedUser = "";
        }
    }

    function userOrTimechange() {
        if (selectedUser != "") {

            var starttime = performance.now();
            var xmlhttp = new XMLHttpRequest();

            var date = new Date();
            date.setTime(timestamp);
            var day=DAYS[date.getUTCDay()];
            var hour=date.getUTCHours();

            xmlhttp.onreadystatechange = function () {
                if (xmlhttp.readyState == XMLHttpRequest.DONE) {
                    if (xmlhttp.status == 200) {
                        var resultJson = JSON.parse(xmlhttp.responseText);

                        var maxPoint=0;
                        var testData = {};
                        testData.data = [];
                        markers.clearLayers();
                        for (var i = 0; i < resultJson.length; i++) {

                            testData.data.push({lat: resultJson[i].lat, lng: resultJson[i].lng, count: resultJson[i].weightInt});
                            addMarker(selectedUser, day, hour, resultJson[i].lat, resultJson[i].lng, resultJson[i].weight);

                            if (resultJson[i].weightInt > maxPoint) {
                                maxPoint = resultJson[i].weightInt ;
                            }
                        }

                        testData.max = maxPoint;
                        heatmap.setData(testData);

                        var processtime = performance.now() - starttime;
                        document.getElementById('messagelbl').innerHTML = "loaded " + resultJson.length + " profile points in: " + parseFloat(processtime).toFixed(2) + " ms";
                    }
                    else if(xmlhttp.status == 204){
                        var processtime = performance.now() - starttime;
                        document.getElementById('messagelbl').innerHTML = "No profile points found for this user, loaded in: " + parseFloat(processtime).toFixed(2) + " ms";
                    }
                    else if (xmlhttp.status == 400) {
                        alert('There was an error 400');
                    }
                    else {
                        alert('something else other than 200 was returned');
                    }
                }
            };
            var params = "timestamp=" + timestamp + "&userid=" + selectedUser;
            xmlhttp.open("GET", "http://" + window.location.hostname + ":8081/getProfile?"+ encodeURI(params), true);
            xmlhttp.send();
        }
        else {
            console.log("selected user empty");
        }
    }

    return {
        init: init,
        dropDownChange: dropDownChange
    };
};
