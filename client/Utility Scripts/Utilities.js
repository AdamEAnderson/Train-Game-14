/// <reference path="../libraries/raphael.js" />
/// <reference path="http://code.jquery.com/jquery-2.0.0.js" /> 
/// <reference path="http://underscorejs.org/underscore.js" /> 
/// <reference path="http://code.jquery.com/ui/jquery-ui-1-9-git.js" /> 
/// <reference path="../libraries/raphael-pan-zoom.js" /> 
/// <reference path="../libraries/d3.js" /> 
/// <reference path="../Globals.js" /> 

// Process a game list
var processGames = function (data) {
    $('#gamePicker').empty();
    for (var i = 0; i < data.gids.length; i++) {
        $('#gamePicker').append('<option>' + data.gids[i] + '</option>').click(data.gids[i], function (e) {
        });
    }
    $('#gamePicker').menu('refresh');
};

//Draws an edge of track between x1,y1 and x2,y2 in the color of a player with ID PID
var drawLineBetweenMileposts = function (x1, y1, x2, y2, PID) {
    $('#pid' + PID).append($(document.createElementNS('http://www.w3.org/2000/svg', 'line')).attr({ x1: x1, y1: y1, x2: x2, y2: y2 }).css({ 'stroke-width': '4px', 'stroke': findPid(lastStatusMessage.players, PID).color }));
    if (PID == pid && justResumed == false) {
        edgesBuilt.push($('#pid' + PID + ' > line:last'))
    }
};

// Return true if the milepost is top left of major city.
// Top left will have preceding milepost not major city, following milepost is 
// major city, and two rows below is not major city.
// NOTE: incomplete major cities could mess this logic up
var firstMajorCityMilepost = function (mp) {
    return ((mp < 0 || gameData.mapData.orderedMileposts[mp - 1].type != 'MAJORCITY') &&
		 gameData.mapData.orderedMileposts[mp + 1].type == 'MAJORCITY' &&
		 mp + (gameData.mapData.mpWidth * 2) < gameData.mapData.orderedMileposts.length &&
		 gameData.mapData.orderedMileposts[mp + (gameData.mapData.mpWidth * 2)].type == 'MAJORCITY');
};

//Given an array of players out of the status message returns the object of the player with ID PID
var findPid = function (players, pid) {
    for (var i = 0; i < players.length; i++)
        if (players[i].pid == pid)
            return players[i];
    return null;
};

var findMilepost = function (x, y) {
    var mpsvg = { x: 0, y: 0 };
    var mpjQ = $(document.getElementById('milepost' + x + ',' + y));
    if (mpjQ.prop('tagName') == 'circle') {
        mpsvg.x = mpjQ.attr('cx');
        mpsvg.y = mpjQ.attr('cy');
    }
    else {
        var translate = mpjQ.attr('transform').replace(/\ scale\([0-9\.]+\)/, '').replace('translate(', '').replace(')', '').split(',');
        var bbox = mpjQ[0].getBBox();
        mpsvg.x = parseInt(translate[0]) + ((bbox.width / 2) * 0.035);
        mpsvg.y = parseInt(translate[1]) + ((bbox.height / 2) * 0.035);
    }
    return mpsvg;
};

//Sends a HTTP POST request to the server with data data and calls callback when complete
var post = function (data, callback) {
    $.ajax({
        type: "POST",
        url: server,
        data: JSON.stringify(data),
        success: callback,
        error: function (xhr, textStatus, errorThrown) {
            console.log("error " + textStatus + " " + errorThrown + " " + xhr.responseText);
        },
        dataType: 'json'
    });
};