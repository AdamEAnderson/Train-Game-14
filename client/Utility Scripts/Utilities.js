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
        mpsvg.x = parseInt(translate[0]) + ((milepostSize / 2) * 1);
        mpsvg.y = parseInt(translate[1]) + ((milepostSize / 2) * 1);
    }
    return mpsvg;
};

var getMilepost = function (x, y) {
    x = parseInt(x);
    y = parseInt(y);
    return gameData.mapData.orderedMileposts[(gameData.mapData.mpWidth * y) + x];
};

var drawTrain = function (train, PID, x, y) {
    $('#train' + PID + train).remove();
    $('#trains' + PID).append($(document.createElementNS('http://www.w3.org/2000/svg', 'circle')).attr({ 'id': 'train' + PID + train, 'cx': x, 'cy': y, 'r': 10, 'fill': findPid(lastStatusMessage.players, PID).color }).click(function () {
        if (trainLocations && trainLocations[train])
            $(document.getElementById('milepost' + trainLocations[train].x + ',' + trainLocations[train].y)).click();
    }));
};

var displayInfo = function (info, type) {
    if (!type || !infoColors[type] || !info || info == '')
        return;

    var children = $('#info').children();

    for (var i = 0; i < children.length; i++)
        if ($(children[i]).text() == info)
            return;

    var jQ = $('<p/>').text(info).css('color', infoColors[type]).attr('data-index', infoIndex);

    infoIndex++;

    $('#info').append(jQ);

    if ($('#info').css('bottom') != '0px' && $('#info').css('bottom') != 0) {
        $('#info').animate({ 'bottom': 0 }, 150, 'swing', function () {
            setTimeout(function () {
                if (jQ.attr('data-index') == $('#info').children().last().attr('data-index'))
                    $('#info').animate({ bottom: -31 }, 150);
                jQ.remove();
            }, 2500);
        });
    }
    else {
        setTimeout(function () {
            if (jQ.attr('data-index') == $('#info').children().last().attr('data-index'))
                $('#info').animate({ bottom: -31 }, 150);
            jQ.remove();
        }, 2500);
    }
};

//Sends a HTTP POST request to the server with data data and calls callback when complete
var post = function (data, callback, error) {
    $.ajax({
        type: "POST",
        url: server,
        data: JSON.stringify(data),
        success: callback,
        error: function (xhr, textStatus, errorThrown) {
            processAjaxErrors(xhr, textStatus, errorThrown);
            if (error)
                error.apply(this, arguments);
        },
        dataType: 'json'
    });
};

var processAjaxErrors = function (xhr, textStatus, errorThrown) {
    console.error("error " + textStatus + " " + errorThrown + " " + xhr.responseText);
    if (xhr.status == 404 && !xhr.responseJSON)
        displayInfo('Error: Server not found', 'error');
    else if (xhr.status == 400 && xhr.responseJSON == 'ColorNotAvailable')
        displayInfo('Error: Color not availible', 'error');
    else if (xhr.status == 400 && xhr.responseJSON == 'PlayerAlreadyJoined')
        displayInfo('Error: Player already joined', 'error');
    else if (xhr.status == 400 && xhr.responseJSON == 'GameNotFound')
        displayInfo('Error: Game not found', 'error');
    else if (xhr.status == 400 && xhr.responseJSON == 'PlayerNotFound')
        displayInfo('Error: Player not found', 'error');
    else if (xhr.status == 400 && xhr.responseJSON == 'InvalidMove')
        displayInfo('Error: Invalid move', 'error');
    else if (xhr.status == 400 && xhr.responseJSON == 'InvalidTrack')
        displayInfo('Error: Invalid track', 'error');
    else
        displayInfo('Error');
};