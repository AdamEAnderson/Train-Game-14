//Processes a status response from the server
var processStatus = function (data) {
    if (endedGame) {
        clearInterval(statusIntervalHandle);
        return;
    }
    if (data.transaction != lastStatus) {
        if (!lastStatus)
            lastStatusMessage = data;
        if (data.ended) endedGameHandler(data);
        var justStarted = false;
        if (!started && data.turnData != null) {
            justStarted = true;
            startedGame(data);
        }
        if (justResumed)
            processResume(data);
        if (!geography && data.geography) {
            geography = data.geography;
            initMap(data.geography);
        }
        if (data.turns == 3 && placedTrain == false) {
            $('#placeTrain').show();
        }
        refreshPlayers(data.players);
        if (started) {
            $('#0').next().addClass('ui-state-active');
            refreshRails(data.players);
            refreshTrainLocations(data.players);
        }
        var me = findPid(data.players, pid);
        refreshCards(me.hand);
        refreshTrains(me.trains, data.turnData && data.turnData.pid == pid);
        var money = me.money;
        var shownMessage = false;
        if (data.turnData != null && data.turnData.pid == pid) {
            if (!yourTurn) {
                var message = justStarted ? 'Started game: It\'s your turn' : 'It\'s your turn';
                shownMessage = true;
                displayInfo(message, 'info');
            }

            yourTurn = true;
            $('#turnControls').buttonset('option', 'disabled', false);
            checkBuildMoney();
            checkLoadButtons(data.players);
            checkMoveButton();
            money = money + (data.turnData.moneyMade - data.turnData.moneySpent);
        }
        else if (data.turnData && data.turnData.pid != pid) {
            yourTurn = false;
            $('#turnControls').buttonset('option', 'disabled', true);
			$('#moneySpent').hide();
        }
        refreshMoney(money);

        if (justStarted)
            yourTurn = data.turnData.pid == pid;

        if (!shownMessage && justStarted)
            displayInfo('Started game', 'info');

        lastStatusMessage = data;
        lastStatus = data.transaction;

        if (loading) {
            $("#loadingBar .ui-progressbar-value").animate({ width: '100%' }, 'fast');
            $('#loading').hide();
            $('#loading').empty();
            $('#gameDisplay').show();
        }
    }
};