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

var endedGameHandler = function (data) {
    endedGame = true;
    $('#gameDisplay').hide().empty();
    $('#endGameDisplay').empty();
    $('#endGameDisplay').show();
    $('#endGameDisplay').append('<p>Game Over</p>')
    var headerRow = $('#endGameDisplay').append('<table/>').find('table:last').append('<tr/>').find('tr:last').append('<th/>');
    for (var i = 0; i < data.players.length; i++)
        headerRow.append('<th>' + data.players[i].pid + '</th>');
    var statsKeys = Object.keys(data.players[0].stats);
    for (var i = 0; i < statsKeys.length; i++) {
        var key = statsKeys[i];
        var lastChar = undefined;
        for (var j = 0; j < key.length; j++) {
            if (!lastChar) {
                lastChar = key[j];
                key = key[0].toUpperCase() + key.substring(1, key.length);
                continue;
            }
            if (lastChar == lastChar.toLowerCase() && key[j] == key[j].toUpperCase()) {
                key = key.substring(0, j) + ' ' + key[j].toUpperCase() + key.substring(j + 1, key.length);
                break;
            }
            lastChar = key[j];
        }
        var jQ = $('#endGameDisplay > table').append('<tr/>').find('tr:last').append('<th>' + key + '</th>');
        for (var j = 0; j < data.players.length; j++)
            jQ.append('<td>' + data.players[j].stats[statsKeys[i]] + '</td>');
    }
    var winner = ['', -Infinity];
    for (var i = 0; i < data.players.length; i++)
        if (data.players[i].stats.money > winner[1])
            winner = [data.players[i].pid, data.players[i].money];
    $('#endGameDisplay').append('<p>Winner is ' + winner[0] + '!</p>');
    $('#info').empty().remove();
    clearInterval(statusIntervalHandle);
    return;
};