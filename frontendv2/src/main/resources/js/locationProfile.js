var UserProfile = function () {

    var DAYS = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"];


    //then the shared variables accross the whole code
    var map; //the map object leaflet map
    var markers; //all the user markers
    var startts;
    var endts;
    var radius=1000;
    var selectedUser = ""; //the selected user


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




    var initMap = function () {
        //PARIS: 48.8523947,2.3462913
        //Luxembourg: 49.632386, 6.168544
        //China: 39.984702,116.318417



        map = L.map('mapid').setView([49.632386, 6.168544], 7);
        L.tileLayer('https://api.mapbox.com/styles/v1/mapbox/streets-v9/tiles/256/{z}/{x}/{y}?access_token={accessToken}', {
            attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery © <a href="http://mapbox.com">Mapbox</a>',
            maxZoom: 18,
            id: 'test',
            accessToken: 'pk.eyJ1IjoiYm9nZGFudG9hZGVyIiwiYSI6ImNpd2RpejNkMjAwOW0yeWs0NTJ2c2Uxcm4ifQ.PrdepIo60YlnfnqQv4FWug'

        }).addTo(map);


        markers = new L.markerClusterGroup();
        markers.addTo(map);

    };


    var init = function () {
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
                    selectedUser=resultJson[0];
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


    function addMarker(nodeID, lat, lng) {
        var marker = L.marker([lat, lng]);
        marker["node"] = nodeID;
        markers.addLayer(marker);
    }


    //When the drop down is changed we call this
    function dropDownChange() {
        var dropownlist = document.getElementById("userDropDown");
        if (dropownlist.options[dropownlist.selectedIndex].text != null) {
            selectedUser = dropownlist.options[dropownlist.selectedIndex].text;
        }
        else {
            selectedUser = "";
        }
    }

    function generateProfile() {
        if (selectedUser != "") {

            var starttime = performance.now();
            var xmlhttp = new XMLHttpRequest();

            xmlhttp.onreadystatechange = function () {
                if (xmlhttp.readyState == XMLHttpRequest.DONE) {
                    if (xmlhttp.status == 200) {
                        var resultJson = JSON.parse(xmlhttp.responseText);


                        var processtime = performance.now() - starttime;
                        document.getElementById('messagelbl').innerHTML = "loaded " + resultJson.length + " profile points in: " + parseFloat(processtime).toFixed(2) + " ms";
                    }
                    else if(xmlhttp.status == 204){
                        markers.clearLayers();
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
        generateProfile: generateProfile
    };
};
