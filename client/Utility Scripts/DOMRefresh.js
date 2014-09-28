﻿/// <reference path="../libraries/raphael.js" />
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
            $('#hand').append($('<div class="card"/>').draggable());
        card = cards[c];
        for (var t = 0; t < card.trips.length; ++t) {
            var iconPNG = iconPath +  "/" + card.trips[t].load + '.png';
            $('#hand').children().eq(c).append('<div class="trip"><table><tr>' + '<td style="width:6%"><img width=30px height=30px src="' + iconPNG + '" /></td>' + '<td style="width:39%">' + card.trips[t].load + '</td>' + '</td><td style="width:39%">' + card.trips[t].dest + '</td><td style="width:6%">' + card.trips[t].cost + '</td></tr></table></div>');
        }
    }
};

var refreshMovesRemaining = function (trains) {
    var countMovesMade = 0;
    for (var t = 0; t < movesMadeThisTurn.length; ++t) {
        countMovesMade = movesMadeThisTurn[t];
        if (movesMade != undefined)
            countMovesMade += movesMade[t].length;
        if ($('#trains').children().eq(t).children().length < 2) // move counter not yet created
            $('#trains').children().eq(t).append('<div class="moveCounter"/>');
        var moveCounterJS = $('#trains').children().eq(t).children().eq(1);
        moveCounterJS.empty();
        var movesRemaining = trains[t].speed - countMovesMade;
        moveCounterJS.append('<p><span>' + movesRemaining + '</span></p>');
    }
}

var refreshTrains = function (trains, myturn) {
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
        var trainCard = train.children().eq(0);
        if(trains.length - 1 > 0)
            trainCard.append('<p><span>#' + (t + 1) + ' - ' + trains[t].speed + '</span></p>');
        else
            trainCard.append('<p><span>' + trains[t].speed + '</span></p>');
        for (var l = 0; l < trains[t].loads.length; ++l) {
            var load = trains[t].loads[l];
            if (!load) {
                load = " ";
                trainCard.append('<p><span>' + load + '</span></p>');
            }
            else {
                var iconPNG = iconPath + "/" + load + '.png';
                trainCard.append('<p><span>' + trains[t].loads[l] + '</span><img width=30px height=30px src="' + iconPNG + '" /></p>');
            }
        }
    }
    if (myturn) 
        refreshMovesRemaining(trains);
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
    otherPlayersEdgesBuilt = [];
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
                drawLineBetweenMileposts(m1svg.x, m1svg.y, m2svg.x, m2svg.y, players[i].pid);
                otherPlayersEdgesBuilt.push({ x1: m1.x, y1: m1.y, x2: m2.x, y2: m2.y });
            }
        }
    }
};

var refreshMoney = function(money) {
    $('#moneyTotalNumber').empty();
    $('#moneyTotalNumber').append('<span>' + money + '</span>');
}

var refreshMoneySpent = function(moneySpent) {
    $('#moneySpentNumber').empty();
    $('#moneySpentNumber').append('<span>' + moneySpent + '</span>');
    $('#moneySpent').show();
}