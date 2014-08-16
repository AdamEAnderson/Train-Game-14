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
    milepostEdgesBuilt = [];
    var milepostsClick = function () {
        if (verticesBuilt.length == 0) {
            var currentMilepost = $(this).attr('id').replace('milepost', '').split(',');
            currentMilepost = gameData.mapData.orderedMileposts[(currentMilepost[1] * gameData.mapData.mpWidth) + parseInt(currentMilepost[0])];
            verticesBuilt.push({ x: currentMilepost.x, y: currentMilepost.y });
            return;
        }
        var lastMilepost = verticesBuilt[verticesBuilt.length - 1];
        lastMilepost = gameData.mapData.orderedMileposts[(lastMilepost.y * gameData.mapData.mpWidth) + lastMilepost.x];
        var currentMilepost = $(this).attr('id').replace('milepost', '').split(',');
        currentMilepost = gameData.mapData.orderedMileposts[(currentMilepost[1] * gameData.mapData.mpWidth) + parseInt(currentMilepost[0])];
        var isValidMilepost = false;
        var milepostCost;
        for (var i = 0; i < lastMilepost.edges.length; i++) {
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
            var bbox = $(document.getElementById('milepost' + lastMilepost.x + ',' + lastMilepost.y))[0].getBBox();
            lastX = parseInt(translate[0]) + ((bbox.width / 2) * 0.035);
            lastY = parseInt(translate[1]) + ((bbox.height / 2) * 0.035);
        }
        var currentX, currentY;
        if ($(this).prop("tagName") == 'circle') {
            currentX = $(this).attr('cx');
            currentY = $(this).attr('cy');
        }
        else if ($(this).prop("tagName") == 'g') {
            var translate = $(this).attr('transform').replace(/\ scale\([0-9\.]+\)/, '').replace('translate(', '').replace(')', '').split(',');
            var bbox = $(this)[0].getBBox();
            currentX = parseInt(translate[0]) + ((bbox.width / 2) * 0.035);
            currentY = parseInt(translate[1]) + ((bbox.height / 2) * 0.035);
        }
        if (!document.getElementById('milepost' + lastMilepost.x + ',' + lastMilepost.y))
            return;
        if (20 - (moneySpent + moneySpentThisBuild + milepostCost) < 0)
            return;
        drawLineBetweenMileposts(lastX, lastY, currentX, currentY, pid);
        verticesBuilt.push({ x: currentMilepost.x, y: currentMilepost.y });
        milepostEdgesBuilt.push({ x1: lastMilepost.x, y1: lastMilepost.y, x2: currentMilepost.x, y2: currentMilepost.y });
        moneySpentThisBuild += milepostCost;
        refreshMoneySpent(moneySpent + moneySpentThisBuild);
        console.log("moneySpentThisBuild " + moneySpentThisBuild + " milepostCost " + milepostCost);
    };
    $('#milepostsGroup > *:not(path)').click(milepostsClick);
    var acceptBuild = function () {
        builtTrack(verticesBuilt, edgesBuilt, moneySpentThisBuild, milepostEdgesBuilt);
        $('#acceptBuild').off('click');
        $('#cancelBuild').off('click');
        $('#milepostsGroup > *:not(path)').off('click', milepostsClick);
        $('#okControls').hide();
        $('#turnControls').show();
        $('#endControls').show();
        verticesBuilt = [];
        milepostEdgesBuilt = [];
        edgesBuilt = [];
        moneySpent += moneySpentThisBuild;
        moneySpentThisBuild = 0;
        checkBuildMoney();
    };
    $('#acceptBuild').click(acceptBuild);
    var cancelBuild = function () {
        $('#acceptBuild').off('click');
        $('#cancelBuild').off('click');
        $('#milepostsGroup > *:not(path)').off('click', milepostsClick);
        $('#okControls').hide();
        $('#turnControls').show();
        $('#endControls').show();
        verticesBuilt = [];
        milepostEdgesBuilt = [];
        for (var i = 0; i < edgesBuilt.length; i++) {
            $(edgesBuilt[i]).remove();
        }
        edgesBuilt = [];
        moneySpentThisBuild = 0;
    };
    $('#cancelBuild').click(cancelBuild);
};