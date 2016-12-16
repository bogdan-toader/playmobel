var UserProfile = function () {

        //the first zone of the global variables should be the constants
        var LAT = "lat";
        var LNG = "lng";

        var DAYS = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"];

        var DEFAULT_LAT = 49.632386;
        var DEFAULT_LNG = 6.168544;


        //then the shared variables accross the whole code
        var map; //the map object leaflet map
        var graph; //the kmf graph
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
                "radius": 20,

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

            //here the graph is connected at the same port
            graph = new org.mwg.GraphBuilder()
                .withStorage(new org.mwg.plugin.WSClient("ws://" + window.location.hostname + ":9011"))
                .withPlugin(new org.mwg.structure.StructurePlugin())
                .withPlugin(new org.mwg.ml.MLPlugin()).build();
            graph.connect(function () {
                initUserList();

            });


        };


        var getGraph = function () {
            return graph;
        };


        function addMarker(nodeID, day, hour, lat, lng, probability) {
            var marker = L.marker([lat, lng])
                .bindPopup(day + " at " + hour + " o'clock, probability: " + Number(probability).toFixed(2) + "%");
            marker["node"] = nodeID;
            markers.addLayer(marker);
        }


        var actions = org.mwg.core.task.Actions;

        //Context variables: processTime, selectedUser, profilerID, selectedDay, selectedHour
        //processTime: the long timestamp of the current read time
        //selectedUser: the selected user from the drop down
        //profilerID: the unique relation name that resolves the profiler over the day and hour of the day by convention on the backend,
        // it is named "profiler"+id where id is a value from 0 to (24*7)-1=167;
        //selectedDay: the selected Day within the week, should be Sunday -> Saturday
        //selectedHour: the selected hour within the day, shoud be from 0->23
        var getProfile = org.mwg.core.task.Actions.newTask()
            .then(actions.travelInTime("{{processTime}}"))
            .then(actions.readGlobalIndex("users", "folderId", "{{selectedUser}}"))
            .then(actions.traverse("{{profilerID}}"))
            .thenDo(function (context) {
                    var profiler = context.result().get(0);

                    profiler.generateDistributions(0, function (proba) {

                        if (proba != null && proba.distributions != null) {
                            var probasize = proba.distributions.length;

                            var testData = {};
                            testData.data = [];
                            markers.clearLayers();

                            var nodeId = context.variable("selectedUser").get(0);
                            var day = context.variable("selectedDay").get(0);
                            var hour = context.variable("selectedHour").get(0);

                            if (probasize > 0) {
                                var maxPoint = 0;
                                for (var i = 0; i < probasize; i++) {
                                    var avg = proba.distributions[i].means;
                                    testData.data.push({lat: avg[0], lng: avg[1], count: proba.total[i]});

                                    addMarker(nodeId, day, hour, avg[0], avg[1], proba.total[i] * 100 / proba.global);

                                    if (proba.total[i] > maxPoint) {
                                        maxPoint = proba.total[i];
                                    }
                                }
                            }
                            testData.max = maxPoint;
                            heatmap.setData(testData);
                        }
                    })
                    context.continueTask();
                }
            );


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
                var date = new Date();
                date.setTime(timestamp); //this timestamp is long it is set automatically for you here
                var relid = (date.getUTCDay()) * 24 + date.getUTCHours();

                var context = getProfile.prepare(graph, null, function (result) {
                    result.free();
                });

                context.setVariable("processTime", timestamp);
                context.setVariable("selectedUser", selectedUser);
                context.setVariable("profilerID", "profiler" + relid);
                context.setVariable("selectedDay", DAYS[date.getUTCDay()]);
                context.setVariable("selectedHour", date.getUTCHours());
                getProfile.executeUsing(context);
            }
            else {
                console.log("selected user empty");
            }
        }
        

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
                )
                .thenDo(function (context) {
                    userOrTimechange();
                    context.continueTask();
                })
            ;


        var initUserList = function () {
            var context = loadAllusers.prepare(graph, null, function (result) {
                result.free();
            });
            context.setVariable("processTime", timestamp);
            loadAllusers.executeUsing(context); //this is async it directly finish exev

        }

        return {
            init: init,
            graph: getGraph,
            dropDownChange: dropDownChange
        };
    }
    ;
