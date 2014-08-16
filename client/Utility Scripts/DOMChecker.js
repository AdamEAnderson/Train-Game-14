/// <reference path="../libraries/raphael.js" />
/// <reference path="http://code.jquery.com/jquery-2.0.0.js" /> 
/// <reference path="http://underscorejs.org/underscore.js" /> 
/// <reference path="http://code.jquery.com/ui/jquery-ui-1-9-git.js" /> 
/// <reference path="../libraries/raphael-pan-zoom.js" /> 
/// <reference path="../libraries/d3.js" /> 
/// <reference path="../Globals.js" /> 
/// <reference path="../Utility Scripts/Utilities.js" /> 

//Checks for whether or not to hide the button for deliver, pickup, and dump
var checkLoadButtons = function (players) {
    var shownPickup = false;
    var shownDump = false;
    //Loops through all of the players trains
    for (var i = 0; i < findPid(players, pid).trains.length; i++) {
        var train = findPid(players, pid).trains[i];
        if (!train.loc || !trainLocations[i])
            continue;
        //If the player has a load, show the dump button
        for (var j = 0; j < train.loads.length; j++) {
            if (train.loads[j] != null) {
                $('#dump').show();
                shownDump = true;
            }
        }
        //If the player is in a city, check for deliver and pickup
        var milepost = gameData.mapData.orderedMileposts[(parseInt(trainLocations[i].y) * gameData.mapData.mpWidth) + parseInt(trainLocations[i].x)];
        if (milepost.type == 'MAJORCITY' || milepost.type == 'CITY') {
            if (milepost.city.loads) {
                $('#pickup').show();
                shownPickup = true;
            }
            checkDeliver(milepost, findPid(players, pid), train);
        }
    }
    if (!shownPickup) {
        $('#pickup').hide();
    }
    if (!shownDump) {
        $('#dump').hide();
    }
}

//Check for whether the player can deliver a load, and if so shows a button for it
var checkDeliver = function (milepost, player, train) {
    for (var i = 0; i < player.hand.length; i++) {
        for (var j = 0; j < player.hand[i].trips.length; j++) {
            var trip = player.hand[i].trips[j];
            if (trip.dest == milepost.city.name) {
                for (var k = 0; k < train.loads.length; k++) {
                    if (train.loads[k] == trip.load) {
                        $('#deliver').show();
                        return;
                    }
                }
            }
        }
    }
    $('#deliver').hide();
};

//Enables or disables the move button based on the speed of your train and the moves the player has made this turn
var checkMoveButton = function () {
    var player = findPid(lastStatusMessage.players, pid);
    var shown = false;
    for (var i = 0; i < player.trains.length; i++) {
        if (movesMade && movesMade[i].length + movesMadeThisTurn[i] < player.trains[i].speed) {
            $('#move').button('option', 'disabled', false);
            shown = true;
        }
        else if (!movesMade && movesMadeThisTurn < player.trains[i].speed) {
            $('#move').button('option', 'disabled', false);
            shown = true;
        }
    }
    if (!shown)
        $('#move').button('option', 'disabled', true);
};

//Checks whether or not to enable the build button based on the money the player has spent this turn
var checkBuildMoney = function () {
    if (moneySpent > 0) {
        $('#upgrade').button('option', 'disabled', true);
    }
    else {
        $('#upgrade').button('option', 'disabled', false);
    }
    if (moneySpent == 20) {
        $('#build').button('option', 'disabled', true);
    }
    else {
        $('#build').button('option', 'disabled', false);
    }
	refreshMoneySpent(moneySpent + moneySpentThisBuild);
};