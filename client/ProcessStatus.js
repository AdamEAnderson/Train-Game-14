//Processes a status response from the server
var processStatus = function (data) {
    if (data.transaction != lastStatus) {
        if (!started && data.joinable == false) {
            startedGame(data);
        }
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
        me = findPid(data.players, pid);
        refreshCards(me.hand);
        refreshTrains(me.trains);
        refreshMoney(me.money);
        if (data.activeid && data.activeid == pid) {
            yourTurn = true;
            $('#turnControls').buttonset('option', 'disabled', false);
            checkBuildMoney();
            checkLoadButtons(data.players);
        }
        else if (data.activeid && data.activeid != pid) {
            yourTurn = false;
            $('#turnControls').buttonset('option', 'disabled', true);
        }
        lastStatusMessage = data;
        lastStatus = data.transaction;
    }
};