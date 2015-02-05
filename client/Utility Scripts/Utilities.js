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

//Draws an edge of track between x1,y1 and x2,y2 in the color of a *single* player with ID PID
var drawSingleTrack = function (x1, y1, x2, y2, PID) {
    var jQ = $(document.createElementNS('http://www.w3.org/2000/svg', 'line')).attr({ x1: x1, y1: y1, x2: x2, y2: y2 }).css({ 'stroke-width': '4px', 'stroke': findPid(lastStatusMessage.players, PID).color });
    $('#pid' + PID).append(jQ);
    if (PID == pid && justResumed == false) {
        edgesBuilt.push(jQ)
    }
    return jQ;
};

/* Draw multi-player "rainbow" track. 
*/
//var drawMultiTrack = function (x1, y1, x2, y2, pids) {
//    var totalStrokeWidth = 4;   // total width of track drawing
//    var strokeWidth = totalStrokeWidth / pids.length;   // width of stroke for each player
//    var slope = (y2 - y1) / (x2 - x1);  // slope of the track
//    var perpandicularSlope = (slope == 0) ? 1 : -(1 / slope);   // slope of the strokewidth
//    var a = Math.tan(perpandicularSlope);
//    var xDelta = strokeWidth * Math.cos(a);  // amount to adjust the drawing position after each player track in x-direction
//    var yDelta = strokeWidth * Math.sin(a);  // amount to adjust the drawing position after each player track in y-direction
//    var xOffset = xDelta / 2; // starting midpoint adjustment (stroke is centered)
//    var yOffset = yDelta / 2; // starting midpoint adjustment (stroke is centered)
//    
//    // (x1, y1) and (x2, y2) are midpoints of a single line -- they need to be converted to 
//    // the top left corner of the stroke.
//    var xTopLeftDelta = (totalStrokeWidth / 2) * Math.cos(a);
//    var yTopLeftDelta = (totalStrokeWidth / 2) * Math.sin(a);
//    x1 -= xTopLeftDelta;
//    y1 -= yTopLeftDelta;
//    x2 -= xTopLeftDelta;
//    y2 -= yTopLeftDelta;
//    for (var i = 0; i < pids.length; ++i) {
//        var PID = pids[i];
//        var x1Pos = x1 + xOffset;
//        var y1Pos = y1 + yOffset;
//        var x2Pos = x2 + xOffset;
//        var y2Pos = y2 + yOffset;   
//        var jQ = $(document.createElementNS('http://www.w3.org/2000/svg', 'line')).attr({ x1: x1Pos, y1: y1Pos, x2: x2Pos, y2: y2Pos }).css({ 'stroke-width': String(strokeWidth) + 'px', 'stroke': findPid(lastStatusMessage.players, PID).color });
//        $('#pid' + PID).append(jQ);
//        if (PID == pid && justResumed == false) 
//            edgesBuilt.push(jQ)
//        xOffset += xDelta;
//        yOffset += yDelta;
//   }
//};

var drawMultiTrack = function (x1, y1, x2, y2, pids) {
    var totalStrokeWidth = 4;   // total width of track drawing
    var strokeWidth = totalStrokeWidth / pids.length;   // width of stroke for each player
    var diffX = x1 - x2; //
    var diffY = y1 - y2; // Vector along the line
    var perpX = diffY; //
    var perpY = -diffX; // Vector perpendicular to the line
    var normLength = Math.sqrt(Math.pow(diffX,2) + Math.pow(diffY,2)); // Length of the vector perpendicular to the line
    var normX = perpX / normLength; //
    var normY = perpY / normLength; // Vector of length one perpendicular to the line
    var xDelta = normX * strokeWidth; //
    var yDelta = normY * strokeWidth; // Vector of length strokeWidth perpendicular to the line
    var xOffsets = []; //
    var yOffsets = []; // Array of offsets from normal line
    if(pids.length % 2 == 0){ // If there is an even number of pids then none go in the center
        for(var i = 0; i < pids.length/2; i++){ // Loop through and give each one its offsets
            xOffsets.push(i*xDelta + xDelta/2);
            xOffsets.push(-i*xDelta - xDelta/2);
            yOffsets.push(i*yDelta + yDelta/2);
            yOffsets.push(-i*yDelta - yDelta/2);
        }
    }
    else{ // If there is an odd number of pids then one gets to go in the center
        xOffsets.push(0); //
        yOffsets.push(0); // The center line has no offset
        for(var i = 1; i < (pids.length + 1)/2; i++){ // Loop through and give all the other lines offsets
            xOffsets.push(i*xDelta);
            xOffsets.push(-i*xDelta);
            yOffsets.push(i*yDelta);
            yOffsets.push(-i*yDelta);
        }
    }
    for (var i = 0; i < pids.length; ++i) { // Loop through the pids and draw each line
        var PID = pids[i];
        var x1Pos = x1 + xOffsets[i]; //
        var y1Pos = y1 + yOffsets[i]; //
        var x2Pos = x2 + xOffsets[i]; //
        var y2Pos = y2 + yOffsets[i]; // Add the original line coordinates to the offset to get the translated line
        var jQ = $(document.createElementNS('http://www.w3.org/2000/svg', 'line')).attr({ x1: x1Pos, y1: y1Pos, x2: x2Pos, y2: y2Pos }).css({ 'stroke-width': String(strokeWidth) + 'px', 'stroke': findPid(lastStatusMessage.players, PID).color }); // Draw the line
        $('#pid' + PID).append(jQ); // Add the line to the DOM
        if (PID == pid && justResumed == false) // If its us, and we didn't just resume
            edgesBuilt.push(jQ); // Add this new line element to the list of our rail elements
   }
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
        mpsvg.x = parseInt(mpjQ.attr('cx'));
        mpsvg.y = parseInt(mpjQ.attr('cy'));
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