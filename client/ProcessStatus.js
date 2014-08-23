//Processes a status response from the server
var processStatus = function (data) {
    if (data.transaction != lastStatus) {
        if (!lastStatus)
            lastStatusMessage = data;
        var justStarted = false;
        if (!started && data.joinable == false) {
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
        refreshTrains(me.trains, data.activeid && data.activeid == pid);
        refreshMoney(me.money);
        var shownMessage = false;
        if (data.activeid && data.activeid == pid) {
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
        }
        else if (data.activeid && data.activeid != pid) {
            yourTurn = false;
            $('#turnControls').buttonset('option', 'disabled', true);
			$('#moneySpent').hide();
        }

        if (justStarted)
            yourTurn = data.activeid == pid;

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