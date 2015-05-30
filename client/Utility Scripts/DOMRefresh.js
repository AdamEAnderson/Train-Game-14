/// <reference path="../libraries/raphael.js" />
/// <reference path="http://code.jquery.com/jquery-2.0.0.js" /> 
/// <reference path="http://underscorejs.org/underscore.js" /> 
/// <reference path="http://code.jquery.com/ui/jquery-ui-1-9-git.js" /> 
/// <reference path="../libraries/raphael-pan-zoom.js" /> 
/// <reference path="../libraries/d3.js" /> 
/// <reference path="../Globals.js" /> 
/// <reference path="../Utility Scripts/Utilities.js" /> 
/// <reference path="../Utility Scripts/DOMChecker.js" /> 

var refreshPlayers = function (players) {
    $('#players').empty();
    for (var i = 0; i < players.length; i++) {
        $('#players').append($('<input type="radio" id="' + i + '"><label for="' + i + '">' + players[i].pid + '</label></input>').attr({ 'readonly': '', 'disabled': '' }));
    }
    $('#players').buttonset();
    for (var i = 0; i < players.length; i++) {
        $('#' + i).next().css('border-color', players[i].color);
    }
};

var refreshCards = function (cards) {
    var iconPath = location.origin + join(location.pathname, '../../data/icons');
    for (var c = 0; c < cards.length; ++c) {
        if ($('#hand').children().eq(c).length != 0)
            $('#hand').children().eq(c).empty();
        else
            $('#hand').append($('<div class="card"/>').draggable({
                start: function (event, ui) {
                    $(this).data('dragging', true);
                },
                stop: function (event, ui) {
                    setTimeout(function () {
                        $(event.target).data('dragging', false);
                    }, 1);
                }
            }));
        card = cards[c];
        for (var t = 0; t < card.trips.length; ++t) {
            var iconPNG = iconPath + "/" + card.trips[t].load + '.png';
            $('#hand').children().eq(c).append('<div class="trip"><table><tr>' + '<td style="width:6%"><img width=30px height=30px src="' + iconPNG + '" /></td>' + '<td class="cardloadtd" style="width:39%">' + card.trips[t].load + '</td>' + '</td><td class="carddesttd" style="width:39%">' + card.trips[t].dest + '</td><td style="width:6%">' + card.trips[t].cost + '</td></tr></table></div>');
            var trip = $('#hand').children().eq(c).children().eq(t);
            if (typeof selectedTrip[c] != 'undefined' && selectedTrip[c].index == t)
                trip.addClass('selected');
            var tripObj = card.trips[t];
            var selectedTripObj = typeof selectedTrip[c] == 'undefined' ? undefined : selectedTrip[c].trip;
            if (typeof selectedTrip[c] != 'undefined' && selectedTrip[c].index == t && (selectedTripObj.dest != tripObj.dest || selectedTripObj.cost != tripObj.cost || selectedTripObj.load != tripObj.load)) {
                $('#hand').children().eq(c).children().eq(t).removeClass('selected');
                selectedTrip[c] = undefined;
            }
            trip.click([JSON.parse(JSON.stringify(c)), JSON.parse(JSON.stringify(t)), JSON.parse(JSON.stringify(cards))], function (e) {
                var args = e.data;
                if ($('#hand').children().eq(args[0]).data('dragging')) return;
                if (typeof selectedTrip[args[0]] != 'undefined')
                    $('#hand').children().eq(args[0]).children().eq(selectedTrip[args[0]].index).removeClass('selected');
                if ((typeof selectedTrip[args[0]] != 'undefined' && selectedTrip[args[0]].index != args[1]) || typeof selectedTrip[args[0]] == 'undefined') {
                    $('#hand').children().eq(args[0]).children().eq(args[1]).addClass('selected');
                    selectedTrip[args[0]] = { index: args[1], trip: args[2][args[0]].trips[args[1]] };
                }
                else
                    selectedTrip[args[0]] = undefined;
            });
            $('#hand').children().eq(c).children().last().find('.cardloadtd').dblclick([JSON.parse(JSON.stringify(t)), JSON.parse(JSON.stringify(card))], function (e) {
                var cities = gameData.loadset[e.data[1].trips[e.data[0]].load];
                for (var i = 0; i < cities.length; i++) {
                    var cityMs = citiesTable[cities[i]];
                    for (var j = 0; j < cityMs.length; j++) {
                        var city = cityMs[j];
                        var jQ = $(document.getElementById('milepost' + city.x + ',' + city.y));
                        //jQ.hide();
                        var ex = true;
                        var stop = false;
                        var interval = setInterval(function (k, l, cit) {
                            if (stop)
                                return;
                            var r;
                            if ($(document.getElementById('milepost' + citiesTable[cities[k]][l].x + ',' + citiesTable[cities[k]][l].y)).css('r'))
                                r = parseInt($(document.getElementById('milepost' + citiesTable[cities[k]][l].x + ',' + citiesTable[cities[k]][l].y)).css('r').replace('px', ''));
                            else
                                r = parseInt($(document.getElementById('milepost' + citiesTable[cities[k]][l].x + ',' + citiesTable[cities[k]][l].y)).attr('r').replace('px', ''));
                            var smallVal = cit.type == "MAJORCITY" ? 3 : 9;
                            var largeVal = cit.type == "MAJORCITY" ? 13 : 19;
                            //console.log((r < 10 ? 'expanding' : 'contracting') + ' for the ' + (cit.type == "MAJORCITY"?'major ':'') + 'city of ' + cit.city.name);
                            if (r < 10)
                                $(document.getElementById('milepost' + citiesTable[cities[k]][l].x + ',' + citiesTable[cities[k]][l].y)).animate({ 'svgR': largeVal }, 500);
                            else if (r > 10)
                                $(document.getElementById('milepost' + citiesTable[cities[k]][l].x + ',' + citiesTable[cities[k]][l].y)).animate({ 'svgR': smallVal }, 500);
                            else
                                $(document.getElementById('milepost' + citiesTable[cities[k]][l].x + ',' + citiesTable[cities[k]][l].y)).animate({ 'svgR': largeVal }, 500);
                        }, 500, JSON.parse(JSON.stringify(i)), JSON.parse(JSON.stringify(j)), JSON.parse(JSON.stringify(city)));
                        setTimeout(function (k, l, inter, cit) {
                            $(document.getElementById('milepost' + citiesTable[cities[k]][l].x + ',' + citiesTable[cities[k]][l].y)).stop();
                            clearInterval(inter);
                            //console.log('clearing interval for ' + (cit.type == "MAJORCITY"?'major city ':'') + cit.city.name);
                            var smallVal = cit.type == "MAJORCITY" ? 3 : 9;
                            $(document.getElementById('milepost' + citiesTable[cities[k]][l].x + ',' + citiesTable[cities[k]][l].y)).attr('svgR', smallVal);
                            $(document.getElementById('milepost' + citiesTable[cities[k]][l].x + ',' + citiesTable[cities[k]][l].y)).css({ 'svgR': smallVal });
                            stop = true;
                        }, 3500, JSON.parse(JSON.stringify(i)), JSON.parse(JSON.stringify(j)), JSON.parse(JSON.stringify(interval)), JSON.parse(JSON.stringify(city)));
                    }
                }
            });
            $('#hand').children().eq(c).children().last().find('.carddesttd').dblclick([JSON.parse(JSON.stringify(t)), JSON.parse(JSON.stringify(card))], function (e) {
                var cityMs = citiesTable[e.data[1].trips[e.data[0]].dest];
                for (var i = 0; i < cityMs.length; i++) {
                    var city = JSON.parse(JSON.stringify({ x: cityMs[i].x, y: cityMs[i].y }));
                    var jQ = $(document.getElementById('milepost' + city.x + ',' + city.y));
                    var jQstring = 'milepost' + city.x + ',' + city.y;
                    var stop = false;
                    var interval = setInterval(function (str, cit) {
                        var jQN = $(document.getElementById(str));
                        if (stop)
                            return;
                        var r;
                        if (jQN.css('r'))
                            r = parseInt(jQN.css('r').replace('px', ''));
                        else
                            r = parseInt(jQN.attr('r').replace('px', ''));
                        var smallVal = cit.type == "MAJORCITY" ? 3 : 9;
                        var largeVal = cit.type == "MAJORCITY" ? 13 : 19;
                        //console.log((r < 10 ? 'expanding' : 'contracting') + ' for the ' + (cit.type == "MAJORCITY" ? 'major ' : '') + 'city of ' + cit.city.name);
                        if (r < 10)
                            jQN.animate({ 'svgR': largeVal }, 500);
                        else if (r > 10)
                            jQN.animate({ 'svgR': smallVal }, 500);
                        else
                            jQN.animate({ 'svgR': largeVal }, 500);
                    }, 500, jQstring.toString(), JSON.parse(JSON.stringify(cityMs[i])));
                    setTimeout(function (str, int, cit) {
                        var jQN = $(document.getElementById(str));
                        jQN.stop();
                        clearInterval(int);
                        //console.log('clearing interval for ' + (cit.type == "MAJORCITY"?'major city ':'') + cit.city.name);
                        var smallVal = cit.type == "MAJORCITY" ? 3 : 9;
                        jQN.attr('svgR', smallVal);
                        jQN.css({ 'svgR': smallVal });
                        stop = true;
                    }, 3500, jQstring.toString(), JSON.parse(JSON.stringify(interval)), JSON.parse(JSON.stringify(cityMs[i])));
                }
            });
        }
    }
};

var refreshMovesRemaining = function (trains) {
    var countMovesMade = 0;
    for (var t = 0; t < movesMadeThisTurn.length; ++t) {
        countMovesMade = movesMadeThisTurn[t];
        if (movesMade != undefined)
            countMovesMade += movesMade[t].length;
        if ($('#train' + t + ' > .trainCard > .moveCounter').length == 0) // move counter not yet created
            $('#train' + t + ' > .trainCard').append('<div class="moveCounter"/>');
        var moveCounterJS = $('#train' + t + ' > .trainCard > .moveCounter');
        moveCounterJS.empty();
        var movesRemaining = trains[t].speed - countMovesMade;
        moveCounterJS.append('<p><span>' + movesRemaining + '</span></p>');
    }
}

var refreshTrains = function (trains, myturn) {
    var imageSizes = { '122': 20, '123': 15, '162': 20, '163': 15, '202': 20, '203': 15 };
    var iconPath = location.origin + join(location.pathname, '../../data/icons');
    for (var t = 0; t < trains.length; ++t) {
        var train;
        if ($('#train' + t).length != 0) {
            $('#train' + t).empty();
            train = $('#train' + t);
        }
        else {
            $('#trains').append($('<div class="train" id="train' + t + '"/>').draggable());
            var train = $('#trains').children().eq(t);
        }
        train.append('<div class="trainCard"/>');
        $('#train' + t + '>.trainCard').addClass('trainCard' + trains[t].speed + trains[t].loads.length).append($('<img src="../../data/artwork/' + trains[t].speed + ' Train ' + trains[t].loads.length + '.png"/>'));
        var trainCard = train.children().eq(0);
        //if(trains.length - 1 > 0)
        //    trainCard.append('<p><span>#' + (t + 1) + ' - ' + trains[t].speed + '</span></p>');
        //else
        //    trainCard.append('<p><span>' + trains[t].speed + '</span></p>');
        for (var l = 0; l < trains[t].loads.length; ++l) {
            var load = trains[t].loads[l];
            if (!load) {
                load = " ";
                trainCard.append('<p><span>' + load + '</span></p>');
            }
            else {
                var iconPNG = iconPath + "/" + load + '.png';
                trainCard.append('<p><span>' + trains[t].loads[l] + '</span><img width="' + imageSizes[trains[t].speed + '' + trains[t].loads.length] + '" height="' + imageSizes[trains[t].speed + '' + trains[t].loads.length] + '" src="' + iconPNG + '" /></p>');
            }
            $('#train' + t + ' > div.trainCard > p:last-child').addClass('compartment');
            $('#train' + t + ' > div.trainCard > p:last-child').attr('id', 'compartment' + (l + 1));
        }
    }
    if (myturn)
        refreshMovesRemaining(trains);
    $('div.trainCard122 > p.compartment').height(0.4 * $('div.trainCard122').height());
    $('div.trainCard122 > p.compartment').css({ 'top': (73 / 200) * $('div.trainCard122').height() });
};

var refreshTrainLocations = function (players) {
    for (var i = 0; i < players.length; i++) {
        if (players[i].pid == pid)
            continue;
        for (var j = 0; j < players[i].trains.length; j++) {
            if (!players[i].trains[j].loc)
                continue;
            $(document.getElementById('train' + players[i].pid + j)).remove();
            var milepost = players[i].trains[j].loc;
            var mpsvg = findMilepost(milepost.x, milepost.y);
            $('#trains' + players[i].pid).append($(document.createElementNS('http://www.w3.org/2000/svg', 'circle')).attr({ 'id': 'train' + players[i].pid + j, 'cx': mpsvg.x, 'cy': mpsvg.y, 'r': 10, 'fill': players[i].color }).click(function () {
                $(document.getElementById('milepost' + lastStatusMessage.players[i].trains[j].loc.x + ',' + lastStatusMessage.players[i].trains[j].loc.y)).click();
            }));
        }
    }
};

var refreshRails = function (players) {
   /* otherPlayersEdgesBuilt = [];
    for (var i = 0; i < players.length; i++) {
        if (players[i].pid == pid)
            continue;
        $('#pid' + players[i].pid).empty();
        var rail = players[i].rail;
        for (var key in rail) {
            var builtEdges = rail[key];
            for (var k = 0; k < builtEdges.length; k++) {
                var m1 = builtEdges[k];
                var m2 = JSON.parse(key);
                var m1svg = findMilepost(m1.x, m1.y);
                var m2svg = findMilepost(m2.x, m2.y);
                drawSingleTrack(m1svg.x, m1svg.y, m2svg.x, m2svg.y, players[i].pid);
                otherPlayersEdgesBuilt.push({ x1: m1.x, y1: m1.y, x2: m2.x, y2: m2.y });
            }
        }
    } */
};

/** Redraw all other player's track */
var refreshTrack = function (track, players) {
    otherPlayersEdgesBuilt = [];
    for (var i = 0; i < players.length; ++i)    // empty out existing track drawing
        if (players[i].pid != pid)
            $('#pid' + players[i].pid).empty();
    for (var i = 0; i < track.globalTracks.length; ++i) {   // generate new track drawing
        var pair = track.globalTracks[i].pair;
        var m1svg = findMilepost(pair[0], pair[1]);
        var m2svg = findMilepost(pair[2], pair[3]);
        var pids = track.globalTracks[i].pids;
        if (pids.indexOf(pid) != -1)
            continue;
        if (pids.length > 1)
            drawMultiTrack(m1svg.x, m1svg.y, m2svg.x, m2svg.y, pids);
        else
            drawSingleTrack(m1svg.x, m1svg.y, m2svg.x, m2svg.y, pids[0]);
        otherPlayersEdgesBuilt.push({ x1: pair[0], y1: pair[1], x2: pair[2], y2: pair[3] });
    }
};

var refreshMoney = function (money) {
    $('#moneyTotalNumber').empty();
    $('#moneyTotalNumber').append('<span>' + money + '</span>');
}

var refreshMoneySpent = function (moneySpent) {
    $('#moneySpentNumber').empty();
    $('#moneySpentNumber').append('<span>' + moneySpent + '</span>');
    $('#moneySpent').show();
}