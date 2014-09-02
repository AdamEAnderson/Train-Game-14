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
    $('#newGameButton').button('option', 'disabled', true);
    post({ messageType: 'joinGame', gid: GID, color: color, pid: handle }, function (data) {
        $('#loading').show();
        loading = true;
        gameData = data;
        gid = GID;
        pid = handle;
        geography = data.geography;
        console.log("join game: " + gid);
        enterGame();
    }, function () {
        $('#newGameButton').button('option', 'disabled', false);
    });
};

//Tells server to resume a game
var resumeGame = function (GID, handle) {
    $('#newGameButton').button('option', 'disabled', true);
    post({ messageType: 'resumeGame', gid: GID, pid: handle }, function (data) {
        $('#loading').show();
        loading = true;
        gameData = data;
        gid = GID;
        pid = handle;
        justResumed = true;
        geography = gameData.geography;
        console.log("resume game: " + gid);
        enterGame();
    }, function () {
        $('#newGameButton').button('option', 'disabled', false);
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
            processAjaxErrors(xhr, textStatus, errorThrown);
            if (xhr.responseJSON && xhr.responseJSON.toLowerCase().indexOf('invalidtrack') != -1) {
                for (var i = 0; i < edges.length; i++) {
                    $(edges[i]).remove();
                }
                moneySpent -= cost;
                checkBuildMoney();
            }
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
            lastSuccessfulMove = undefined;
        },
        error: function (xhr, textStatus, errorThrown) {
            processAjaxErrors(xhr, textStatus, errorThrown);
            if (xhr.responseJSON && xhr.responseJSON.toLowerCase().indexOf('invalidmove') != -1) {
                var player = findPid(lastStatusMessage.players, pid);
                var milepost = vertices[-1];
                trainLocations[train] = milepost;
                var mpsvg = findMilepost(milepost.x, milepost.y);
                drawTrain(train, pid, mpsvg.x, mpsvg.y);
                checkLoadButtons(lastStatusMessage.players);
                movesMade = undefined;
                lastSuccessfulMove = undefined;
                checksMoveButton();
            }
        },
        dataType: 'json'
    });
};

var testMoveTrain = function (train, vertices) {
    var data = { messageType: 'testMoveTrain', pid: pid, gid: gid, mileposts: vertices, train: train };
    $.ajax({
        type: 'POST',
        url: server,
        data: JSON.stringify(data),
        success: function () {
            if (lastSuccessfulMove) {
                lastSuccessfulMove[train] = vertices.clone();
                if (vertices[-1]) {
                    lastSuccessfulMove[train][-1] = JSON.parse(JSON.stringify(vertices[-1]));
                }
            }
        },
        error: function (xhr, textStatus, errorThrown) {
            processAjaxErrors(xhr, textStatus, errorThrown);
            if (!movesMade || !movesMade[train])
                return;
            if (lastSuccessfulMove && !lastSuccessfulMove[train])
                movesMade[train] = [];
            else {
                movesMade[train] = lastSuccessfulMove[train].clone();
                if (lastSuccessfulMove[train][-1])
                    movesMade[train][-1] = JSON.parse(JSON.stringify(lastSuccessfulMove[train][-1]));
            }
            var mpsvg = findMilepost(movesMade[train][movesMade[train].length - 1].x, movesMade[train][movesMade[train].length - 1].y);
            drawTrain(train, pid, mpsvg.x, mpsvg.y);
            trainLocations[train] = movesMade[train][movesMade[train].length - 1] || undefined;
            refreshMovesRemaining(findPid(lastStatusMessage.players, pid).trains);
        }
    });
};

var testBuildTrack = function (vertices, edges, cost, milepostEdges) {
    var data = { messageType: 'testBuildTrack', pid: pid, gid: gid, mileposts: vertices };
    $.ajax({
        type: 'POST',
        url: server,
        data: JSON.stringify(data),
        success: function () {
            lastTrackBuilt = { vertices: vertices.clone(), edges: milepostEdges.clone(), DOMEdges: edges.clone(), cost: cost };
        },
        error: function (xhr, textStatus, errorThrown) {
            processAjaxErrors(xhr, textStatus, errorThrown);
            if ($('#turnControls').css('display') != 'none')
                return;
            verticesBuilt = lastTrackBuilt.vertices || [];
            milepostEdgesBuilt = lastTrackBuilt.edges || [];
            for (var i = 0; i < edgesBuilt.length; i++)
                edgesBuilt[i].remove();
            edgesBuilt = lastTrackBuilt.DOMEdges || [];
            if ($('#buildCursor').length > 0)
                $('#buildCursor').remove();
            if (verticesBuilt.length > 0) {
                var mpsvg = findMilepost(verticesBuilt[verticesBuilt.length - 1].x, verticesBuilt[verticesBuilt.length - 1].y);
                $('#map > svg').append($(document.createElementNS('http://www.w3.org/2000/svg', 'circle')).attr({ 'id': 'buildCursor', 'cx': mpsvg.x, 'cy': mpsvg.y, 'r': 2, 'fill': findPid(lastStatusMessage.players, pid).color }));
            }
            for (var j = 0; j < milepostEdgesBuilt.length; j++) {
                var mpsvg1 = findMilepost(milepostEdgesBuilt[j].x1, milepostEdgesBuilt[j].y1);
                var mpsvg2 = findMilepost(milepostEdgesBuilt[j].x2, milepostEdgesBuilt[j].y2);
                drawLineBetweenMileposts(mpsvg1.x, mpsvg1.y, mpsvg2.x, mpsvg2.y, pid);
            }
            moneySpentThisBuild = cost;
            refreshMoneySpent(moneySpent + moneySpentThisBuild);
        }
    });
};

var deliverLoad = function (train, load, card) {
    post({ messageType: 'deliverLoad', train: train, load: load, card: card, pid: pid, gid: gid });
};

//Tells server we've started our train
var placeTrain = function (train, milepost) {
    post({ messageType: 'placeTrain', pid: pid, gid: gid, train: train, where: milepost });
    trainLocations[train] = milepost;
    checkLoadButtons(lastStatusMessage.players, true);
};

var listColors = function () {
    if (!gamePicked)
        return;
    post({ messageType: 'listColors', gid: gamePicked }, function (d) {
        $('#colorPicker').empty();
        for (var i = 0; i < colors.length; i++) {
            if (d.indexOf(colors[i]) != -1)
                continue;
            $('#colorPicker').append('<option>' + colors[i] + '</option>');
            $('#colorPicker').selectmenu('refresh');
        }
    });
};

//Tells server we've upgraded our train
var upgradedTrain = function (trainState, train) {
    moneySpent += 20;
    checkBuildMoney();
    post({ messageType: 'upgradeTrain', pid: pid, gid: gid, upgradeType: trainState, train: train });
};

//Tells server to undo the last action
var undo = function () {
    post({ messageType: 'undo', gid: gid, pid: pid }, function () { justResumed = true; });
};

//Tells server to redo the last undone action
var redo = function () {
    post({ messageType: 'redo', gid: gid, pid: pid }, function () { justResumed = true; });
};

//Tells server we're done with our turn
var endTurn = function () {
    post({ messageType: 'endTurn', pid: pid, gid: gid });
    moneySpent = 0;
    for (var i = 0; i < movesMadeThisTurn.length; i++)
        movesMadeThisTurn[i] = 0;
    checkBuildMoney();
    $('#turnControls').buttonset('option', 'disabled', true);
    yourTurn = false;
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

var newGame = function (color, handle, gameGeo, gameName, handSize, startingMoney, numTrains) {
    $('#newGameButton').button('option', 'disabled', true);
    var data = { messageType: 'newGame', color: color, pid: handle, gameType: gameGeo, name: gameName };
    data["ruleSet"] = {};
    data.ruleSet["handSize"] = handSize || 4;
    data.ruleSet["startingMoney"] = startingMoney || 70;
    data.ruleSet["numTrains"] = numTrains || 1;
    post(data, function (data) {
        $('#loading').show();
        loading = true;
        gameData = data;
        gid = data.gid;
        pid = handle;
        geography = gameGeo;
        console.log("new game: " + gid);
        enterGame();
    }, function () {
        $('#newGameButton').button('option', 'disabled', false);
    });
};

//Gets a status response from the server
var statusGet = function () {
    //setTimeout(200, statusGet);

    //Request status from server
    //requestData = gid ? {messageType:'statusUpdate', pid:pid, gid:gid} : {messageType:'statusUpdate', pid:pid};
    var requestData = { messageType: 'status', gid: gid, pid: pid };
    $.ajax({
        type: "GET",
        url: server,
        data: JSON.stringify(requestData),
        dataType: 'json',
        success: function (responseData) {
            processStatus(responseData);
        },
        error: function (xhr, textStatus, errorThrown) {
            processAjaxErrors(xhr, textStatus, errorThrown);
        }
    });
};