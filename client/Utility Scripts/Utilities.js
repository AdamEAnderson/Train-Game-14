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
    for (var key in data.gidNames) {
        $('#gamePicker').append($('<option>' + data.gidNames[key] + '</option>').attr('data-gid', key));
    }
    $('#gamePicker').menu('refresh');
};

//Draws an edge of track between x1,y1 and x2,y2 in the color of a player with ID PID
var drawLineBetweenMileposts = function (x1, y1, x2, y2, PID) {
    var jQ = $(document.createElementNS('http://www.w3.org/2000/svg', 'line')).attr({ x1: x1, y1: y1, x2: x2, y2: y2 }).css({ 'stroke-width': '4px', 'stroke': findPid(lastStatusMessage.players, PID).color });
    $('#pid' + PID).append(jQ);
    if (PID == pid && justResumed == false) {
        edgesBuilt.push(jQ)
    }
    return jQ;
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
                if ($('#info').length == 0)
                    return;
                if (jQ.attr('data-index') == $('#info').children().last().attr('data-index'))
                    $('#info').animate({ bottom: -31 }, 150);
                jQ.remove();
            }, 2500);
        });
    }
    else {
        setTimeout(function () {
            if ($('#info').length == 0)
                return;
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
    else if (xhr.status == 400 && xhr.responseJSON == 'InvalidUpgrade')
        displayInfo('Error: Invalid upgrade', 'error');
    else if (xhr.status == 400 && xhr.responseJSON == 'InvalidMessageType')
        displayInfo('Error: Invalid message type', 'error');
    else if (xhr.status == 400 && xhr.responseJSON == 'TurnAlreadyStarted')
        displayInfo('Error: Turn already started', 'error');
    else if (xhr.status == 400 && xhr.responseJSON == 'BadMapData')
        displayInfo('Error: Bad map data', 'error');
    else if (xhr.status == 400 && xhr.responseJSON == 'BadCardData')
        displayInfo('Error: Bad card data', 'error');
    else if (xhr.status == 400 && xhr.responseJSON == 'ExceededAllowance')
        displayInfo('Error: Exceeded allowance', 'error');
    else if (xhr.status == 400 && xhr.responseJSON == 'InvalidCard')
        displayInfo('Error: Invalid card', 'error');
    else if (xhr.status == 400 && xhr.responseJSON == 'TrainAlreadyStarted')
        displayInfo('Error: Train already started', 'error');
    else if (xhr.status == 400 && xhr.responseJSON == 'RulesetNotFound')
        displayInfo('Error: Ruleset not found', 'error');
    else if (xhr.status == 400 && xhr.responseJSON == 'GameTypeNotFound')
        displayInfo('Error: Game type not found', 'error');
    else if (xhr.status == 400 && xhr.responseJSON == 'GameAlreadyStarted')
        displayInfo('Error: Game already started', 'error');
    else if (xhr.status == 400 && xhr.responseJSON == 'PlayerNotActive')
        displayInfo('Error: Player not active', 'error');
    else if (xhr.status == 400 && xhr.responseJSON == 'CityNotFound')
        displayInfo('Error: City not found', 'error');
    else if (xhr.status == 400 && xhr.responseJSON == 'InvalidLoad')
        displayInfo('Error: Invalid load', 'error');
    else if (xhr.status == 400 && xhr.responseJSON == 'TrainFull')
        displayInfo('Error: Train full', 'error');
    else if (xhr.status == 400 && xhr.responseJSON == 'InvalidDelivery')
        displayInfo('Error: Invalid delivery', 'error');
    else if (xhr.status == 400 && xhr.responseJSON == 'NothingToUndo')
        displayInfo('Error: Nothing to undo', 'error');
    else if (xhr.status == 400 && xhr.responseJSON == 'NothingToRedo')
        displayInfo('Error: Nothing to redo', 'error');
    else if(xhr.responseJSON)
        displayInfo('Error ' + xhr.responseJSON);
    else
        displayInfo('Error');
};