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
/// <reference path="../Utility Scripts/ServerInterface.js" /> 

//Loaded
$(document).ready(function () {
    //Init display object
    //paper = Raphael('display',$('body').width(),$('body').height());
    //paper.circle(0,0,200);

    $(function () {
        $("#radio").buttonset();
    });

    //Init main menu
    $('#mainMenu').append('<h3 id="joinGameText">Train Game</h3>');
    $('#loading').append('<div id="loadingBar" />');
    $('#loadingBar').progressbar();
    $('#mainMenu').append('<select id="actionPicker"><option>New</option><option>Join</option><option>Resume</option></select>');
    $('#mainMenu').append('<select id="gamePicker"/>');
    $('#mainMenu').append('<h4 style="margin:10px 0px 5px 75px;">Handle</h4>');
    $('#mainMenu').append('<input id="handlePicker" type="text" size="32"style="width:200px;"/>');
    $('#mainMenu').append('<h4 style="margin:10px 0px 5px 60px;">Game Color</h4>');
    $('#mainMenu').append('<select id="colorPicker"><option>aqua</option><option>black</option><option>blue</option><option>fuchsia</option><option>gray</option><option>green</option><option>lime</option><option>maroon</option><option>navy</option><option>olive</option><option>orange</option><option>purple</option><option>red</option><option>silver</option><option>teal</option><option>yellow</option></select>');
    $('#mainMenu').append('<h4 id="geographyPicker-label" style="margin:10px 0px 5px 60px;">Geography</h4>');
    $('#mainMenu').append('<select id="geographyPicker"><option>africa</option></select>');
    $('#actionPicker').selectmenu({
        change: function (event, data) {
            if (data.item.label == "New") {
                $('#gamePicker').empty();
                $('#gamePicker-button').hide();
                $('#colorPicker').prev().show();
                $('#colorPicker').next().show();
                $('#geographyPicker-label').show();
                $('#geographyPicker-button').show();
            }
            else {
                $('#gamePicker-button').show().css('display', 'block');
                $('#geographyPicker-label').hide();
                $('#geographyPicker-button').hide();
                $('#colorPicker').prev().show();
                $('#colorPicker').next().show();
                if (data.item.label == "Join")
                    gameOption = "joinable";
                else {
                    gameOption = "resumeable";
                    $('#colorPicker').prev().hide();
                    $('#colorPicker').next().hide();
                }
                //Populate games list menu
                requestData = { messageType: 'list', listType: gameOption };
                $.ajax({
                    type: "GET",
                    url: server,
                    data: JSON.stringify(requestData),
                    dataType: 'json',
                    success: function (responseData) {
                        processGames(responseData);
                    },
                    error: function (xhr, textStatus, errorThrown) {
                        processAjaxErrors(xhr, textStatus, errorThrown);
                    }
                });
            }
        }
    });
    $('#colorPicker').selectmenu();
    $('#geographyPicker').selectmenu();
    $('#gamePicker').selectmenu();
    $('#colorPicker').css('font-size', '0.8em');
    $('#mainMenu').append('<br/>');
    $('#mainMenu').append('<button id="newGameButton">OK</button>');
    $('#newGameButton').css('margin-top', '15px');
    $('#newGameButton').button().click(function () {
        if ($('#handlePicker').val() && $('#handlePicker').val().length > 0) {
            if (document.getElementById("actionPicker").value == "New") {
                newGame(document.getElementById("colorPicker").value, $('#handlePicker').val(),
					document.getElementById("geographyPicker").value);
            } else if (document.getElementById("actionPicker").value == "Join" && $('#gamePicker')[0].value && $('#gamePicker')[0].value != '') {
                joinGame(document.getElementById("gamePicker").value,
					document.getElementById("colorPicker").value, $('#handlePicker').val());
            } else if (document.getElementById("actionPicker").value == "Resume" && $('#gamePicker')[0].value && $('#gamePicker')[0].value != '') {
                resumeGame(document.getElementById("gamePicker").value, $('#handlePicker').val());
            }
        }
    });
    $('#mainMenu').show();

    lastStatus = 0;
    //statusGet();
});

var initMap = function (geography) {
    var mapFile = '../../data/' + geography + '/map.svg';
    $.ajax({
        url: location.origin + join(location.pathname, mapFile),
        method: 'GET',
        type: 'text/plain',
        success: function (d) {
            var groups = $(d).find('g');
            for (var i = 0; i < groups.length; i++) {
                $(paper.canvas).append($(groups[i]));
            }
            var viewBox = $(d).find('svg').attr('viewBox').split(' ');
            //$('#map > svg').attr('viewBox',$(d).find('svg').attr('viewBox'));
            mapViewboxWidth = viewBox[2];
            mapViewboxHeight = viewBox[3];
            paper.setViewBox(viewBox[0], viewBox[1], viewBox[2], viewBox[3]);
            paper.setSize($('#map').width(), $('#map').height());
            panZoom = paper.panzoom({ dragModifier: 5, initialZoom: 0, initialPosition: { x: 0, y: 0 }, width: viewBox[2], height: viewBox[3] });
            //panZoom.enable();
            $(paper.canvas).on('dblclick', function () {
                panZoom.zoomIn(1);
            });
            $('#up').click(function () {
                panZoom.zoomIn(1);
            });
            $('#down').click(function () {
                panZoom.zoomOut(1);
            });
            $('#mainMenu').hide();
            panZoom.enable();
            drawMileposts();
            setInterval('statusGet()', 250);
        },
        error: function (a, b, c) {
            processAjaxErrors(a, b, c);
        }
    });
}

var enterGame = function () {
    $('#mainMenu').empty();
    $('#gameDisplay').append($('<div id="topBar"/>').append('<div id="players"/>', '<div id="controls"/>'));
    $('#gameDisplay').append('<div id="map"/>');
    var moneyPNG = location.origin + join(location.pathname, '../../data/' + geography + '/money.png');
    $('#gameDisplay').append('<div id="handAndTrains"><div id="hand"/><div id="trains"/><div id="money"><div id="moneyTotal"><img id="moneyTotalIcon" src="' + moneyPNG + '"/><div id="moneyTotalNumber"/></div><div id="moneySpent" style="display:none"><img id="moneySpentIcon" src="' + moneyPNG + '"/><div id="moneySpentNumber"/></div></div>');
    $('#moneyTotal').draggable();
    $('#moneySpent').draggable();
    $('#gameDisplay').append('<div id="mapControls"><a id="up" href="javascript:;"></a><a id="down" href="javascript:;"></a></div>');
    $('#controls').append('<input id="startGame" type="checkbox"><label for="startGame">Start Game</label></input>');
    $('#controls').buttonset();
    $('#startGame').change(function () {
        startGame($('#startGame')[0].checked);
    });
    paper = new Raphael('map', $('#map').width(), $('#map').height());
    $('#map').resize(function () {
        paper.setSize($('#map').width(), $('#map').height());
    });
    $("#loadingBar .ui-progressbar-value").css({ width: '33%' });
    if (geography)
        initMap(geography);
    else {
        // join/resume: we'll make the map once we have gotten a status message 
        // and know the geography
        setInterval('statusGet()', 250);
    }
}

var drawMileposts = function () {
    var xDelta = gameData.mapData.mapWidth / gameData.mapData.mpWidth;
    var yDelta = gameData.mapData.mapHeight / gameData.mapData.mpHeight;
    console.log("xDelta: " + xDelta + " yDelta: " + yDelta);
    var mp = 0;
    var oddRowOffset = xDelta / 2;
    var milepostsGroup = paper.group(0, []);
    $('#map > svg > g:last').attr('id', 'milepostsGroup');
    for (var h = 0; h < gameData.mapData.mpHeight; ++h) {
        for (var w = 0; w < gameData.mapData.mpWidth; ++w) {
            //if(gameData.mapData.orderedMileposts[mp].x != w || gameData.mapData.orderedMileposts[mp].y != h)
            //console.log('INCORRECT MILEPOST: ' + gameData.mapData.orderedMileposts[mp].x + ',' + gameData.mapData.orderedMileposts[mp].y);
            var x = w * xDelta + gameData.mapData.leftOffset;
            var y = h * yDelta + gameData.mapData.topOffset;
            x = h % 2 == 1 ? x + oddRowOffset : x;
            switch (gameData.mapData.orderedMileposts[mp].type) {
                case 'CITY':
                    milepostsGroup.push(paper.circle(x, y, 9).attr({ 'fill': '#d00', 'stroke-width': '0px', 'stroke': '#d00' }));
                    $('#milepostsGroup > circle:last').attr('id', 'milepost' + w.toString() + ',' + h.toString());
                    break;
                case 'MAJORCITY':
                    // Draw the outline of the major city when we get to the first (top
                    // left) milepost of the city, so the stroke will come out below the
                    // mileposts. 
                    if (firstMajorCityMilepost(mp)) {
                        var pathString = "M" + x + " " + y +
							"L" + (x + xDelta) + " " + y +
							"L" + (x + (xDelta * 1.5)) + " " + (y + yDelta) +
							"L" + (x + xDelta) + " " + (y + (yDelta * 2)) +
							"L" + x + " " + (y + (yDelta * 2)) +
							"L" + (x - (xDelta / 2)) + " " + (y + yDelta) +
							"L" + x + " " + y;
                        milepostsGroup.push(paper.path(pathString));
                        $('#milepostsGroup > path:last').attr({ 'stroke-width': '4px', 'stroke': '#d00' });
                    }
                    // fall through
                case 'NORMAL':
                    milepostsGroup.push(paper.circle(x, y, 3).attr({ 'fill': '#000', 'stroke-width': '0px' }));
                    $('#milepostsGroup > circle:last').attr('id', 'milepost' + w.toString() + ',' + h.toString());
                    break;
                case 'FERRY':
                case 'BLANK':
                    break;
                default:
                    var size = milepostSize;
                    //var jQ = $($(mileposts[gameData.mapData.orderedMileposts[mp].type]).find('svg').children()).clone();
                    var path = location.origin + join(location.pathname, '../../data/mileposts/' + gameData.mapData.orderedMileposts[mp].type.toLowerCase() + '.png');
                    var jQ = $(document.createElementNS('http://www.w3.org/2000/svg', 'image')).attr({ 'x': 0, 'y': 0, 'width': size, 'height': size });
                    //var pathAttr = document.createAttributeNS('http://www.w3.org/1999/xlink', 'href');
                    //pathAttr.value = path;
                    //jQ[0].attributes.setNamedItemNS(pathAttr);
                    jQ[0].setAttributeNS('http://www.w3.org/1999/xlink', 'href', path);
                    var mpjQ = $(document.createElementNS('http://www.w3.org/2000/svg', 'g'));
                    $('#milepostsGroup').append(mpjQ.append(jQ));
                    //var bbox = $('#milepostsGroup').find('g:last')[0].getBBox();//$(mileposts[gameData.mapData.orderedMileposts[mp].type]).find('svg').attr('viewBox').split(' ');
                    var scale = 1;
                    x -= (size / 2) * scale;
                    y -= (size / 2) * scale;
                    //mpjQ.attr('transform', 'translate(' + x + ',' + y + ') scale(' + scale + ')').attr('id', 'milepost' + w + ',' + h);
                    mpjQ.attr('transform', 'translate(' + x + ',' + y + ') scale(' + scale + ')').attr('id', 'milepost' + w + ',' + h);
                    break;
            }
            ++mp;
        }
    }
    $('#milepostsGroup > *:not(path)').click(function () {
        console.log($(this).attr('id').replace('milepost', ''));
        var currentMilepost = $(this).attr('id').replace('milepost', '').split(',');
        console.log(gameData.mapData.orderedMileposts[(currentMilepost[1] * gameData.mapData.mpWidth) + parseInt(currentMilepost[0])]);
    });
    $("#loadingBar .ui-progressbar-value").animate({ width: '66%' }, 'fast');
};

var processResume = function (data) {
    lastStatusMessage = data;
    var player = findPid(data.players, pid);
    for (var i = 0; i < player.trains.length; i++) {
        movesMadeThisTurn[i] = player.movesMade[i];
        if ($('#train' + pid + i).length != 0 || !player.trains[i].loc || player.trains[i].loc == '')
            continue;
        $('#move').show();
        $('#drop').show();
        var milepost = JSON.parse(player.trains[i].loc);
        trainLocations[i] = { x: milepost.x, y: milepost.y };
        var mpsvg = findMilepost(milepost.x, milepost.y);
        drawTrain(i, pid, milepost.x, milepost.y);
    }
    moneySpent = player.spendings;
    checkBuildMoney();
    $('#pid' + pid).empty();
    var rail = player.rail;
    for (var key in rail) {
        var builtEdges = rail[key];
        for (var k = 0; k < builtEdges.length; k++) {
            var m1 = builtEdges[k];
            var m2 = JSON.parse(key);
            var m1svg = findMilepost(m1.x, m1.y);
            var m2svg = findMilepost(m2.x, m2.y);
            drawLineBetweenMileposts(m1svg.x, m1svg.y, m2svg.x, m2svg.y, pid);
            edgesBuiltFinal.push({ x1: m1.x, y1: m1.y, x2: m2.x, y2: m2.y });
            verticesBuiltFinal.push(m1, m2);
        }
    }
    
    checkMoveButton();
    checkLoadButtons(lastStatusMessage.players, true);
    justResumed = false;
};