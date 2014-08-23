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

var buildClick = function (e) {
    var data = e.data;
    $('#turnControls').hide();
    $('#endControls').hide();
    $('#okControls').show();
    moneySpentThisBuild = 0;
    verticesBuilt = [];
    edgesBuilt = [];
    edgesBuiltDOM = [];
    milepostEdgesBuilt = [];
    lastTrackBuilt = [];
    var milepostsClick = function () {
        var player = findPid(lastStatusMessage.players, pid);
        if (verticesBuilt.length == 0) {
            var currentMilepost = $(this).attr('id').replace('milepost', '').split(',');
            currentMilepost = gameData.mapData.orderedMileposts[(currentMilepost[1] * gameData.mapData.mpWidth) + parseInt(currentMilepost[0])];
            verticesBuilt.push({ x: currentMilepost.x, y: currentMilepost.y });
            var mpsvg = findMilepost(currentMilepost.x, currentMilepost.y);
            $('#map > svg').append($(document.createElementNS('http://www.w3.org/2000/svg', 'circle')).attr({ 'id': 'buildCursor', 'cx': mpsvg.x, 'cy': mpsvg.y, 'r': 2, 'fill': player.color }));
            testBuildTrack(verticesBuilt, edgesBuilt, moneySpentThisBuild, milepostEdgesBuilt);
            return;
        }
        var lastMilepost = verticesBuilt[verticesBuilt.length - 1];
        lastMilepost = gameData.mapData.orderedMileposts[(lastMilepost.y * gameData.mapData.mpWidth) + lastMilepost.x];
        var currentMilepost = $(this).attr('id').replace('milepost', '').split(',');
        currentMilepost = gameData.mapData.orderedMileposts[(currentMilepost[1] * gameData.mapData.mpWidth) + parseInt(currentMilepost[0])];
        var isValidMilepost = false;
        var milepostCost;
        for (var i = 0; i < lastMilepost.edges.length; i++) {
            if (lastMilepost.edges[i] == null)
                continue;
            if (lastMilepost.edges[i].x == currentMilepost.x && lastMilepost.edges[i].y == currentMilepost.y) {
                isValidMilepost = true;
                milepostCost = lastMilepost.edges[i].cost;
                break;
            }
        }
        if (isValidMilepost == false)
            return;
        var lastX, lastY;
        if ($(document.getElementById('milepost' + lastMilepost.x + ',' + lastMilepost.y)).prop("tagName") == 'circle') {
            lastX = $(document.getElementById('milepost' + lastMilepost.x + ',' + lastMilepost.y)).attr('cx');
            lastY = $(document.getElementById('milepost' + lastMilepost.x + ',' + lastMilepost.y)).attr('cy');
        }
        else if ($(document.getElementById('milepost' + lastMilepost.x + ',' + lastMilepost.y)).prop("tagName") == 'g') {
            var translate = $(document.getElementById('milepost' + lastMilepost.x + ',' + lastMilepost.y)).attr('transform').replace(/\ scale\([0-9\.]+\)/, '').replace('translate(', '').replace(')', '').split(',');
            lastX = parseInt(translate[0]) + ((milepostSize / 2) * 1);
            lastY = parseInt(translate[1]) + ((milepostSize / 2) * 1);
        }
        var currentX, currentY;
        if ($(this).prop("tagName") == 'circle') {
            currentX = $(this).attr('cx');
            currentY = $(this).attr('cy');
        }
        else if ($(this).prop("tagName") == 'g') {
            var translate = $(this).attr('transform').replace(/\ scale\([0-9\.]+\)/, '').replace('translate(', '').replace(')', '').split(',');
            currentX = parseInt(translate[0]) + ((milepostSize / 2) * 1);
            currentY = parseInt(translate[1]) + ((milepostSize / 2) * 1);
        }
        if (!document.getElementById('milepost' + lastMilepost.x + ',' + lastMilepost.y))
            return;
        if (20 - (moneySpent + moneySpentThisBuild + milepostCost) < 0)
            return;
        drawLineBetweenMileposts(lastX, lastY, currentX, currentY, pid);
        $(document.getElementById('buildCursor')).remove();
        $('#map > svg').append($(document.createElementNS('http://www.w3.org/2000/svg', 'circle')).attr({ 'id': 'buildCursor', 'cx': currentX, 'cy': currentY, 'r': 2, 'fill': player.color }));
        verticesBuilt.push({ x: currentMilepost.x, y: currentMilepost.y });
        milepostEdgesBuilt.push({ x1: lastMilepost.x, y1: lastMilepost.y, x2: currentMilepost.x, y2: currentMilepost.y });
        moneySpentThisBuild += milepostCost;
        refreshMoneySpent(moneySpent + moneySpentThisBuild);
        testBuildTrack(verticesBuilt, edgesBuilt, moneySpentThisBuild, milepostEdgesBuilt);
        console.log("moneySpentThisBuild " + moneySpentThisBuild + " milepostCost " + milepostCost);
    };
    var milepostsKeyUp = function (e) {
        if (verticesBuilt.length == 0)
            return;
        var key = e.key;
        var player = findPid(lastStatusMessage.players, pid);
        //var hexKeys = ['g', 'y', 'u', 'j', 'n', 'b'];
        var hexKeys = ['u', 'j', 'n', 'b', 'g', 'y'];
        //var hexKeyCodes = [71, 89, 85, 74, 78, 66];
        var hexKeyCodes = [85, 74, 78, 66, 71, 89];
        var index;
        if (key)
            index = hexKeys.indexOf(key);
        else
            index = hexKeyCodes.indexOf(e.which);
        if (index == -1)
            return;
        var milepost = verticesBuilt[verticesBuilt.length - 1];
        milepost = gameData.mapData.orderedMileposts[(parseInt(milepost.y) * gameData.mapData.mpWidth) + parseInt(milepost.x)];
        var edges = milepost.edges;

        /*-------------------------------------------------------------------------------*/

        var lastMilepost = milepost;
        var currentMilepost = edges[index];
        if (currentMilepost == undefined)
            return;
        currentMilepost = gameData.mapData.orderedMileposts[(parseInt(currentMilepost.y) * gameData.mapData.mpWidth) + parseInt(currentMilepost.x)];
        var isValidMilepost = false;
        var milepostCost;
        for (var i = 0; i < lastMilepost.edges.length; i++) {
            if (lastMilepost.edges[i] == null)
                continue;
            if (lastMilepost.edges[i].x == currentMilepost.x && lastMilepost.edges[i].y == currentMilepost.y) {
                isValidMilepost = true;
                milepostCost = lastMilepost.edges[i].cost;
                break;
            }
        }
        if (isValidMilepost == false)
            return;
        var lastSVG = findMilepost(lastMilepost.x, lastMilepost.y);
        var lastX = lastSVG.x;
        var lastY = lastSVG.y;
        var currentSVG = findMilepost(currentMilepost.x, currentMilepost.y);
        var currentX = currentSVG.x;
        var currentY = currentSVG.y;
        if (!document.getElementById('milepost' + lastMilepost.x + ',' + lastMilepost.y))
            return;
        if (20 - (moneySpent + moneySpentThisBuild + milepostCost) < 0)
            return;
        drawLineBetweenMileposts(lastX, lastY, currentX, currentY, pid);
        $(document.getElementById('buildCursor')).remove();
        $('#map > svg').append($(document.createElementNS('http://www.w3.org/2000/svg', 'circle')).attr({ 'id': 'buildCursor', 'cx': currentX, 'cy': currentY, 'r': 2, 'fill': player.color }));
        verticesBuilt.push({ x: currentMilepost.x, y: currentMilepost.y });
        milepostEdgesBuilt.push({ x1: lastMilepost.x, y1: lastMilepost.y, x2: currentMilepost.x, y2: currentMilepost.y });
        moneySpentThisBuild += milepostCost;
        refreshMoneySpent(moneySpent + moneySpentThisBuild);
        console.log("moneySpentThisBuild " + moneySpentThisBuild + " milepostCost " + milepostCost);
        testBuildTrack(verticesBuilt, edgesBuilt, moneySpentThisBuild, milepostEdgesBuilt);
    };
    var buildCursor = function () {
        if (!document.getElementById('buildCursor'))
            return;
        if ($(document.getElementById('buildCursor')).css('display') == 'none')
            $(document.getElementById('buildCursor')).show();
        else
            $(document.getElementById('buildCursor')).hide();
    };
    var interval = setInterval(buildCursor, 500);
    $(document).keyup(milepostsKeyUp);
    $('#milepostsGroup > *:not(path)').click(milepostsClick);
    var acceptBuild = function () {
        clearInterval(interval);
        $(document.getElementById('buildCursor')).remove();
        builtTrack(verticesBuilt, edgesBuilt, moneySpentThisBuild, milepostEdgesBuilt);
        $('#acceptBuild').off('click');
        $('#cancelBuild').off('click');
        $('#milepostsGroup > *:not(path)').off('click', milepostsClick);
        $(document).off('keyup', milepostsKeyUp);
        $('#okControls').hide();
        $('#turnControls').show();
        $('#endControls').show();
        verticesBuilt = [];
        milepostEdgesBuilt = [];
        lastTrackBuilt = undefined;
        edgesBuilt = [];
        moneySpent += moneySpentThisBuild;
        moneySpentThisBuild = 0;
        checkBuildMoney();
    };
    $('#acceptBuild').click(acceptBuild);
    var cancelBuild = function () {
        clearInterval(interval);
        $(document.getElementById('buildCursor')).remove();
        $('#acceptBuild').off('click');
        $('#cancelBuild').off('click');
        $('#milepostsGroup > *:not(path)').off('click', milepostsClick);
        $(document).off('keyup', milepostsKeyUp);
        $('#okControls').hide();
        $('#turnControls').show();
        $('#endControls').show();
        verticesBuilt = [];
        milepostEdgesBuilt = [];
        lastTrackBuilt = undefined;
        for (var i = 0; i < edgesBuilt.length; i++) {
            $(edgesBuilt[i]).remove();
        }
        edgesBuilt = [];
        moneySpentThisBuild = 0;
    };
    $('#cancelBuild').click(cancelBuild);
};