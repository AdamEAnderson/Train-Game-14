var checkLoadButtons = function (players) {
    var shownPickup = false;
    var shownDump = false;
    for (var i = 0; i < findPid(players, pid).trains.length; i++) {
        var train = findPid(players, pid).trains[i];
        if (!train.loc || !trainLocations[i])
            continue;
        for (var j = 0; j < train.loads.length; j++) {
            if (train.loads[j] != null) {
                $('#dump').show();
                shownDump = true;
            }
        }
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
};