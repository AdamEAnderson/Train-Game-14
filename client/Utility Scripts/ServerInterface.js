/// <reference path="../libraries/raphael.js" />
/// <reference path="http://code.jquery.com/jquery-2.0.0.js" /> 
/// <reference path="http://underscorejs.org/underscore.js" /> 
/// <reference path="http://code.jquery.com/ui/jquery-ui-1-9-git.js" /> 
/// <reference path="../libraries/raphael-pan-zoom.js" /> 
/// <reference path="../libraries/d3.js" /> 
/// <reference path="../Globals.js" /> 
/// <reference path="../Utility Scripts/Utilities.js" /> 
/// <reference path="../Utility Scripts/DOMChecker.js" /> 
/// <reference path="../Utility Scripts/DOMRefresh.js" /> 

//Tells server we've joined a game
var joinGame = function (GID, color, handle) {
    post({ messageType: 'joinGame', gid: GID, color: color, pid: handle }, function (data) {
        gameData = data;
        gid = GID;
        pid = handle;
        for (var i = 0; i < gameData.mapData.orderedMileposts.length; ++i)
            gameData.mapData.orderedMileposts[i] = JSON.parse(gameData.mapData.orderedMileposts[i]);
        geography = data.geography;
        console.log("join game: " + gid);
        enterLobby();
    });
};

//Tells server to resume a game
var resumeGame = function (GID, handle) {
    post({ messageType: 'resumeGame', gid: GID, pid: handle }, function (data) {
        gameData = data.gameData;
        gid = GID;
        pid = handle;
        justResumed = true;
        for (var i = 0; i < gameData.mapData.orderedMileposts.length; ++i)
            gameData.mapData.orderedMileposts[i] = JSON.parse(gameData.mapData.orderedMileposts[i]);
        geography = data.geography;
        console.log("resume game: " + gid);
        enterLobby();
    });
    pid = handle;
};

//Tells server to start the game(from host)
var startGame = function (checked) {
    post({ messageType: 'startGame', gid: gid, pid: pid, ready: checked });
};

//Tells server we've built track
var builtTrack = function (vertices, edges, cost, milepostEdges) {
    //var edges = edges.clone();
    var data = { messageType: 'buildTrack', pid: pid, gid: gid, mileposts: vertices };
    $.ajax({
        type: "POST",
        url: server,
        data: JSON.stringify(data),
        success: function () {
            Array.prototype.push.apply(verticesBuiltFinal, vertices);
            Array.prototype.push.apply(edgesBuiltFinal, milepostEdges);
        },
        error: function (xhr, textStatus, errorThrown) {
            if (xhr.responseText.toLowerCase().contains('invalidtrack')) {
                for (var i = 0; i < edges.length; i++) {
                    $(edges[i]).remove();
                }
                moneySpent -= cost;
                checkBuildMoney();
            }
            else
                console.log("error " + textStatus + " " + errorThrown + " " + xhr.responseText);
        },
        dataType: 'json'
    });
};
var pickupLoad = function (train, load) {
    post({ messageType: 'pickupLoad', train: train, load: load, pid: pid, gid: gid });
};

var dumpLoad = function (train, load) {
    post({ messageType: 'dumpLoad', train: train, load: load, pid: pid, gid: gid });
};

var moveTrain = function (train, vertices) {
    var data = { messageType: 'moveTrain', pid: pid, gid: gid, mileposts: vertices, train: train };
    $.ajax({
        type: "POST",
        url: server,
        data: JSON.stringify(data),
        success: function () {
            movesMadeThisTurn[train] += vertices.length;
            checkLoadButtons(lastStatusMessage.players);
            movesMade = undefined;
        },
        error: function (xhr, textStatus, errorThrown) {
            console.log('Move error: ' + xhr.responseText);
            if (xhr.responseText.toLowerCase().contains('invalidmove')) {
                var player = findPid(lastStatusMessage.players, pid);
                var milepost = vertices[-1];
                trainLocations[train] = milepost;
                var mpsvg = { x: 0, y: 0 };
                var mpjQ = $(document.getElementById('milepost' + milepost.x + ',' + milepost.y));
                if (mpjQ.prop('tagName') == 'circle') {
                    mpsvg.x = mpjQ.attr('cx');
                    mpsvg.y = mpjQ.attr('cy');
                }
                else {
                    var translate = mpjQ.attr('transform').replace(/\ scale\([0-9\.]+\)/, '').replace('translate(', '').replace(')', '').split(',');
                    var bbox = $(document.getElementById('milepost' + lastMilepost.x + ',' + lastMilepost.y))[0].getBBox();
                    mpsvg.x = parseInt(translate[0]) + ((bbox.width / 2) * 1);
                    mpsvg.y = parseInt(translate[1]) + ((bbox.height / 2) * 1);
                }
                $('#train' + pid + train).remove();
                $('#trains' + pid).append($(document.createElementNS('http://www.w3.org/2000/svg', 'circle')).attr({ 'id': 'train' + pid + train, 'cx': mpsvg.x, 'cy': mpsvg.y, 'r': 10, 'fill': player.color }));
                checkLoadButtons(lastStatusMessage.players);
                movesMade = undefined;
                checksMoveButton();
            }
            else
                console.log("error: " + textStatus + " " + errorThrown + " " + xhr.responseText);
        },
        dataType: 'json'
    });
};

var deliverLoad = function (train, load, card) {
    post({ messageType: 'deliverLoad', train: train, load: load, card: card, pid:pid, gid:gid });
};

//Tells server we've started our train
var placeTrain = function (train, milepost) {
    post({ messageType: 'placeTrain', pid: pid, gid: gid, train: train, where: milepost });
    trainLocations[train] = milepost;
};

//Tells server we've upgraded our train
var upgradedTrain = function (trainState) {
    moneySpent += 20;
    checkBuildMoney();
    post({ messageType: 'upgradeTrain', pid: pid, gid: gid, upgradeType: trainState });
};

//Tells server we're done with our turn
var endTurn = function () {
    post({ messageType: 'endTurn', pid: pid, gid: gid });
    moneySpent = 0;
    for (var i = 0; i < movesMadeThisTurn.length; i++)
        movesMadeThisTurn[i] = 0;
    checkBuildMoney();
    $('#turnControls').buttonset('option', 'disabled', true);
};

//Tells server I quit
var resignGame = function () {
    post({ messageType: 'resignGame', pid: pid, gid: gid }, function () {
        window.location.reload(true);
    });
};

//Tells server our game is done(from host)
var endGame = function (checked) {
    post({ messageType: 'endGame', pid: pid, gid: gid, ready: checked });
};

var newGame = function (color, handle, gameGeo) {
    post({ messageType: 'newGame', color: color, pid: handle, gameType: gameGeo }, function (data) {
        gameData = data;
        for (var i = 0; i < gameData.mapData.orderedMileposts.length; ++i) {
            gameData.mapData.orderedMileposts[i] = JSON.parse(gameData.mapData.orderedMileposts[i]);
        }
        gid = data.gid;
        pid = handle;
        geography = gameGeo;
        console.log("new game: " + gid);
        enterLobby();
    });
};

//Gets a status response from the server
var statusGet = function () {
    //setTimeout(200, statusGet);

    //Request status from server
    //requestData = gid ? {messageType:'statusUpdate', pid:pid, gid:gid} : {messageType:'statusUpdate', pid:pid};
    requestData = { messageType: 'status', gid: gid, pid: pid };
    $.ajax({
        type: "GET",
        url: server,
        data: JSON.stringify(requestData),
        dataType: 'json',
        success: function (responseData) {
            processStatus(responseData);
        },
        error: function (xhr, textStatus, errorThrown) {
            console.log("error " + textStatus + " " + errorThrown);
        }
    });
};