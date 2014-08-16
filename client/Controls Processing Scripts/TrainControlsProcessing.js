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
/// <reference path="../Initialization Scripts/Initialization.js" /> 
/// <reference path="../Initialization Scripts/GameInit.js" /> 
/// <reference path="../Controls Processing Scripts/DialogStages.js" /> 
/// <reference path="../Controls Processing Scripts/BuildControlsProcessing.js" /> 
/// <reference path="../Controls Processing Scripts/LoadControlsProcessing.js" /> 

var upgradeClick = function (e) {
    var data = e.data;
    for (var i = 0; i < lastStatusMessage.players.length; i++)
        if (lastStatusMessage.players[i].pid == pid) {
            var player = lastStatusMessage.players[i];
            $('#lobby').append('<div id="upgradeDialog" title="Upgrade Your Train" />').find('div:last').dialog({
                dialogClass: "no-close",
                buttons: [{
                    text: "Cancel",
                    click: function () {
                        $(this).dialog("destroy");
                        $('#upgradeDialog').remove();
                    }
                }]
            });
            var trains = player.trains.clone();
            for (var i = 0; i < trains.length; i++) {
                if (trains[i].capacity == 3 && trains[i].speed == 20) {
                    trains.splice(i, 1);
                }
            }
            if (trains.length == 1) {
                upgradeDialogStageTwo(trains[0], true);
            }
            else if (trains.length > 1) {
                $('#upgradeDialog').append('<ul/>');
                for (var i = 0; i < player.trains.length; i++) {
                    if (!trains.contains(player.trains[i]))
                        continue;
                    $('#upgradeDialog > ul').append('<li>Train ' + (i + 1) + '</li>').find('li:last').click(function () {
                        $('#upgradeDialog > ul > li').removeClass('clicked');
                        $(this).addClass('clicked');
                    });
                }
                var buttons = $('#upgradeDialog').dialog('option', 'buttons');
                buttons.push({
                    text: "OK",
                    click: function () {
                        $('#upgradeDialog').empty();
                        var buttons = $('#upgradeDialog').dialog('option', 'buttons');
                        buttons.pop();
                        $('#upgradeDialog').dialog('option', 'buttons', buttons);
                        upgradeDialogStageTwo(player.trains[parseInt($('#upgradeDialog > ul > li.clicked').text().replace('Train ', '')) - 1], false);
                    }
                });
                $('#upgradeDialog').dialog('option', 'buttons', buttons);
            }
        }
};

var moveClick = function (e) {
    var data = e.data;
    $('#turnControls').hide();
    $('#endControls').hide();
    $('#okControls').show();
    movesMade = [];
    for (var i = 0; i < findPid(data.players, pid).trains.length; i++) {
        movesMade[i] = [];
    }
    if (findPid(data.players, pid).trains.length > 1)
        $('#placeControls').show();
    $(document).keyup(function (e) {
        if ($('#placeControls > .ui-state-active').length == 0 && findPid(data.players, pid).trains.length > 1)
            return;

        var key = e.key;
        var player = findPid(lastStatusMessage.players, pid);
        var hexKeys = ['g', 'y', 'u', 'j', 'n', 'b'];
        var index = hexKeys.indexOf(key);
        if (index == -1)
            return;
        var train = findPid(data.players, pid).trains.length == 1 ? 0 : parseInt($($('#placeControls > .ui-state-active').children()).first().text().replace('Train ', '')) - 1;
        if (movesMadeThisTurn[train] + movesMade[train].length >= player.trains[train].speed)
            return;
        var milepost = trainLocations[train];
        milepost = gameData.mapData.orderedMileposts[(parseInt(milepost.y) * gameData.mapData.mpWidth) + parseInt(milepost.x)];
        //var edge = milepost.edges[index];
        var edges = [];
        if (milepost.y % 2 == 0) {
            edges[0] = { x: milepost.x - 1, y: milepost.y };
            edges[1] = { x: milepost.x - 1, y: milepost.y - 1 };
            edges[2] = { x: milepost.x, y: milepost.y - 1 };
            edges[3] = { x: milepost.x + 1, y: milepost.y };
            edges[4] = { x: milepost.x, y: milepost.y + 1 };
            edges[5] = { x: milepost.x - 1, y: milepost.y + 1 };
        }
        else {
            edges[0] = { x: milepost.x - 1, y: milepost.y };
            edges[1] = { x: milepost.x, y: milepost.y - 1 };
            edges[2] = { x: milepost.x + 1, y: milepost.y - 1 };
            edges[3] = { x: milepost.x + 1, y: milepost.y };
            edges[4] = { x: milepost.x + 1, y: milepost.y + 1 };
            edges[5] = { x: milepost.x, y: milepost.y + 1 };
        }
        for (var i = 0; i < edges.length; i++) {
            if (!document.getElementById('milepost' + milepost.x + ',' + milepost.y))
                edges[i] = undefined;
        }
        var edge = edges[index];
        edge = gameData.mapData.orderedMileposts[(parseInt(edge.y) * gameData.mapData.mpWidth) + parseInt(edge.x)];
        if (!edge)
            return;
        var valid = false;
        for (var j = 0; j < edgesBuiltFinal.length; j++) {
            var edgeBuilt = edgesBuiltFinal[j];
            if ((milepost.x == edgeBuilt.x1 && milepost.y == edgeBuilt.y1 && edgeBuilt.x2 == edge.x && edgeBuilt.y2 == edge.y) || (edge.x == edgeBuilt.x1 && edge.y == edgeBuilt.y1 && edgeBuilt.x2 == milepost.x && edgeBuilt.y2 == milepost.y))
                valid = true;
        }
        for (var k = 0; k < otherPlayersEdgesBuilt.length; k++) {
            var edgeBuilt = otherPlayersEdgesBuilt[k];
            if ((milepost.x == edgeBuilt.x1 && milepost.y == edgeBuilt.y1 && edgeBuilt.x2 == edge.x && edgeBuilt.y2 == edge.y) || (edge.x == edgeBuilt.x1 && edge.y == edgeBuilt.y1 && edgeBuilt.x2 == milepost.x && edgeBuilt.y2 == milepost.y))
                valid = true;
        }
        if (milepost.type == 'MAJORCITY' && edge.type == 'MAJORCITY')
            valid = true;
        if (!valid)
            return;
        if (movesMade[train].length == 0) {
            movesMade[train][-1] = { x: milepost.x, y: milepost.y };
        }
        movesMade[train].push({ x: edge.x, y: edge.y });
        trainLocations[train] = { x: edge.x, y: edge.y };
        var mpsvg = { x: 0, y: 0 };
        var mpjQ = $(document.getElementById('milepost' + edge.x + ',' + edge.y));
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
        $('#train' + pid + train).remove();
        $('#trains' + pid).append($(document.createElementNS('http://www.w3.org/2000/svg', 'circle')).attr({ 'id': 'train' + pid + train, 'cx': mpsvg.x, 'cy': mpsvg.y, 'r': 10, 'fill': player.color }));
    });
    $('#acceptBuild').click(function () {
        for (var i = 0; i < movesMade.length; i++) {
            moveTrain(i, movesMade[i]);
        }
        checkMoveButton();
        $(this).off('click');
        $('#acceptBuild').off('click');
        $(document).off('keyup');
        $('#okControls').hide();
        $('#placeControls').hide();
        $('#turnControls').show();
        $('#endControls').show();
    });
    $('#cancelBuild').click(function () {
        var player = findPid(lastStatusMessage.players, pid);
        for (var i = 0; i < player.trains.length; i++) {
            if (movesMade[i].length > 0) {
                var milepost = movesMade[i][-1];
                trainLocations[i] = milepost;
                var train = i;
                var mpsvg = { x: 0, y: 0 };
                var mpjQ = $(document.getElementById('milepost' + milepost.x + ',' + milepost.y));
                if (mpjQ.prop('tagName') == 'circle') {
                    mpsvg.x = mpjQ.attr('cx');
                    mpsvg.y = mpjQ.attr('cy');
                }
                else {
                    var translate = mpjQ.attr('transform').replace(/\ scale\([0-9\.]+\)/, '').replace('translate(', '').replace(')', '').split(',');
                    var bbox = $(document.getElementById('milepost' + lastMilepost.x + ',' + lastMilepost.y))[0].getBBox();
                    mpsvg.x = parseInt(translate[0]) + ((bbox.width / 2) * 0.035);
                    mpsvg.y = parseInt(translate[1]) + ((bbox.height / 2) * 0.035);
                }
                $('#train' + pid + train).remove();
                $('#trains' + pid).append($(document.createElementNS('http://www.w3.org/2000/svg', 'circle')).attr({ 'id': 'train' + pid + train, 'cx': mpsvg.x, 'cy': mpsvg.y, 'r': 10, 'fill': player.color }));
            }
        }
        movesMade = undefined;
        checkMoveButton();
        $(this).off('click');
        $('#acceptBuild').off('click');
        $(document).off('keyup');
        $('#okControls').hide();
        $('#placeControls').hide();
        $('#turnControls').show();
        $('#endControls').show();
    });
};

var placeTrainClick = function (e) {
    var data = e.data;
    placeTrainLocations = [];
    var milepostClick = function () {
        if (findPid(data.players, pid).trains.length > 1 && $('#placeControls > .ui-state-active').length == 0)
            return;
        var location = $(this).attr('id').replace('milepost', '').split(',');
        var milepost = { x: location[0], y: location[1] };
        var train = findPid(data.players, pid).trains.length == 1 ? 0 : parseInt($($('#placeControls > .ui-state-active').children()).first().text().replace('Train ', '')) - 1;
        if ($('#train' + pid + train).length > 0)
            return;
        placeTrainLocations[train] = { train: train, milepost: milepost };
        var mpsvg = { x: 0, y: 0 };
        var mpjQ = $(this);
        if (mpjQ.prop('tagName') == 'circle') {
            mpsvg.x = mpjQ.attr('cx');
            mpsvg.y = mpjQ.attr('cy');
        }
        else {
            var translate = mpjQ.attr('transform').replace(/\ scale\([0-9\.]+\)/, '').replace('translate(', '').replace(')', '').split(',');
            var bbox = $(document.getElementById('milepost' + lastMilepost.x + ',' + lastMilepost.y))[0].getBBox();
            mpsvg.x = parseInt(translate[0]) + ((bbox.width / 2) * 0.035);
            mpsvg.y = parseInt(translate[1]) + ((bbox.height / 2) * 0.035);
        }
        $('#trains' + pid).append($(document.createElementNS('http://www.w3.org/2000/svg', 'circle')).attr({ 'id': 'train' + pid + train, 'cx': mpsvg.x, 'cy': mpsvg.y, 'r': 10, 'fill': findPid(data.players, pid).color }));
        if (placeTrainLocations.length == findPid(data.players, pid).trains.length)
            $('#acceptBuild').button('option', 'disabled', false);
        else
            $('#acceptBuild').button('option', 'disabled', true);
    };
    $('#milepostsGroup > *:not(path)').click(milepostClick);
    $('#acceptBuild').click(function () {
        if (placeTrainLocations.length != findPid(data.players, pid).trains.length)
            return;
        for (var i = 0; i < placeTrainLocations.length; i++) {
            placeTrain(placeTrainLocations[i].train, placeTrainLocations[i].milepost);
        }
        $('#move').show();
        $('#drop').show();
        $('#placeTrain').hide();
        placedTrain = true;
        placeTrainLocations = [];
        $('#turnControls').show();
        $('#endControls').show();
        $('okControls').hide();
        $('#placeControls').hide();
        $('#okControls').buttonset('option', 'disabled', false);
        $('#acceptBuild').off('click');
        $('#cancelBuild').off('click');
    });
    $('#cancelBuild').click(function () {
        placeTrainLocations = [];
        $('#turnControls').show();
        $('#endControls').show();
        $('okControls').hide();
        $('#placeControls').hide();
        $('#okControls').buttonset('option', 'disabled', false);
        $('#acceptBuild').off('click');
        $('#cancelBuild').off('click');
        $('#trains' + pid).empty();
    });
    $('#turnControls').hide();
    $('#endControls').hide();
    $('#okControls').show();
    $('#acceptBuild').button('option', 'disabled', true);
    if (findPid(data.players, pid).trains.length > 1) {
        $('#placeControls').show();
    }
};