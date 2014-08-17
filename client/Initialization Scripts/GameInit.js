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

var startedGame = function (data) {
    started = true;
    for (var i = 0; i < findPid(data.players, pid).trains.length; i++) {
        trainLocations[i] = [];
        movesMadeThisTurn[i] = 0;
    }
    $('#controls').buttonset('destroy');
    $('#controls').empty();
    $('#controls').append('<div id="turnControls"/>');
    $('#controls').append('<div id="endControls"/>');
    $('#controls').append('<div id="okControls"/>').find('div:last').hide();
    $('#controls').append('<div id="placeControls"/>').find('div:last').hide();
    $('#okControls').append('<button id="acceptBuild">OK</button>')
    $('#okControls').append('<button id="cancelBuild">Cancel</button>');
    for (var i = 0; i < findPid(data.players, pid).trains.length; i++) {
        $('#placeControls').append('<input type="radio" id="trainpicker' + i + '"><label for="trainpicker' + i + '">Train ' + (i + 1) + '</label></input>');
    }
    $('#endControls').append('<input type="checkbox" id="endGame"><label for="endGame">End Game</label></input>');
    $('#endControls').append('<button id="resign">Resign</button>');
    $('#endControls').append('<button id="loads">Loads</button>').find('button:last').click(data,loadsClick);
    
    $('#endGame').change(function () {
        endGame($('#endGame')[0].checked);
    });
    $('#resign').click(function () {
        //Open confirm box for them and make sure they want to resign
        //If they do, post to the server telling it to resign this player
        resignClick();
    });
    $('#turnControls').append('<button id="build">Build</button>').find('button:last').click(data, buildClick);
    $('#turnControls').append('<button id="upgrade">Upgrade</button>').find('button:last').click(data, upgradeClick);
    $('#turnControls').append($('<button id="move">Move</button>').hide().click(data, moveClick));
    $('#turnControls').append($('<button id="deliver">Deliver</button>').hide().click(data, deliverClick));
    $('#turnControls').append($('<button id="pickup">Pickup</button>').hide().click(data, pickupClick));
    $('#turnControls').append($('<button id="dump">Dump</button>').hide().click(data, dumpClick));
    $('#turnControls').append($('<button id="placeTrain">Place Train</button>').hide().click(data, placeTrainClick));
    $('#turnControls').append('<button id="endTurn">End Turn</button>').find('button:last').click(endTurn);
    $('#turnControls').buttonset();
    $('#turnControls').buttonset('option', 'disabled', data.activeid != pid)
    $('#endControls').buttonset();
    $('#okControls').buttonset();
    $('#placeControls').buttonset();
    yourTurn = data.activeid == pid;
    $('#map > svg').append($(document.createElementNS('http://www.w3.org/2000/svg', 'g')).attr('id', 'track'));
    $('#map > svg').append($(document.createElementNS('http://www.w3.org/2000/svg', 'g')).attr('id', 'trainsDisplay'));
    for (var i = 0; i < data.players.length; i++) {
        if (data.players[i].pid == pid)
            continue;
        $('#track').append($(document.createElementNS('http://www.w3.org/2000/svg', 'g')).attr('id', 'pid' + data.players[i].pid));
        $('#trainsDisplay').append($(document.createElementNS('http://www.w3.org/2000/svg', 'g')).attr('id', 'trains' + data.players[i].pid));
    }
    $('#track').append($(document.createElementNS('http://www.w3.org/2000/svg', 'g')).attr('id', 'pid' + pid));
    $('#trainsDisplay').append($(document.createElementNS('http://www.w3.org/2000/svg', 'g')).attr('id', 'trains' + pid));
};