var refreshPlayers = function (players) {
    $('#players').empty();
    for (var i = 0; i < players.length; i++) {
        $('#players').append($('<input type="radio" id="' + i + '"><label for="' + i + '">' + players[i].pid + '</label></input>').attr({ 'readonly': '', 'disabled': '' }));
    }
    $('#players').buttonset();
};

var refreshCards = function (cards) {
    $('#hand').empty();
    for (var c = 0; c < cards.length; ++c) {
        $('#hand').append('<div class="card"/>');
        card = cards[c];
        for (var t = 0; t < card.trips.length; ++t) {
            $('#hand').children().eq(c).append('<div class="trip"><table><tr><td style="width:42%">' + card.trips[t].load + '</td><td style="width:42%">' + card.trips[t].dest + '</td><td style="width:6%">' + card.trips[t].cost + '</td></tr></table></div>');
            //if (t == card.trips.length - 1)
            //	$('#hand').children().eq(c).append('<div class="trip-last"><p><span>' + card.trips[t].load + '</span><br/><span>' + card.trips[t].dest + '</span><br/><span>' + card.trips[t].cost);
            //else
            //	$('#hand').children().eq(c).append('<div class="trip"><p><span>' + card.trips[t].load + '</span><br/><span>' + card.trips[t].dest + '</span><br/><span>' + card.trips[t].cost);
        }
    }
};

var refreshTrains = function (trains) {
    $('#trains').empty();
    for (var t = 0; t < trains.length; ++t) {
        $('#trains').append('<div class="train"/>');
        $('#trains').children().eq(t).append('<p><span>' + trains[t].speed + '</span></p>');
        for (var l = 0; l < trains[t].loads.length; ++l)
            $('#trains').children().eq(t).append('<p><span>' + trains[t].loads[l] + '</span></p>');
    }
};

var refreshTrainLocations = function (players) {
    for (var i = 0; i < players.length; i++) {
        if (players[i].pid == pid)
            continue;
        for (var j = 0; j < players[i].trains.length; j++) {
            if (!players[i].trains[j].loc)
                continue;
            var milepost = JSON.parse(players[i].trains[j].loc);
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
            $('#trains' + players[i].pid).append($(document.createElementNS('http://www.w3.org/2000/svg', 'circle')).attr({ 'id': 'train' + players[i].pid + j, 'cx': mpsvg.x, 'cy': mpsvg.y, 'r': 10, 'fill': players[i].color }));
        }
    }
};

var refreshRails = function (players) {
    for (var i = 0; i < players.length; i++) {
        if (players[i].pid == pid)
            continue;
        $('#pid' + players[i].pid).empty();
        var rail = players[i].rail;
        for (var key in rail) {
            var builtEdges = rail[key];
            for (var k = 0; k < builtEdges.length; k++) {
                var m1 = builtEdges[k];
                var m2 = JSON.parse(key);
                var m1jQ = $(document.getElementById('milepost' + m1.x + ',' + m1.y));
                var m2jQ = $(document.getElementById('milepost' + m2.x + ',' + m2.y));
                var m1svg = { x: 0, y: 0 };
                var m2svg = { x: 0, y: 0 };
                if (m1jQ.prop('tagName') == 'circle') {
                    m1svg.x = m1jQ.attr('cx');
                    m1svg.y = m1jQ.attr('cy');
                }
                else {
                    var translate = m1jQ.attr('transform').replace(/\ scale\([0-9\.]+\)/, '').replace('translate(', '').replace(')', '').split(',');
                    var bbox = m1jQ.getBBox();
                    m1svg.x = parseInt(translate[0]) + ((bbox.width / 2) * 0.035);
                    m1svg.y = parseInt(translate[1]) + ((bbox.height / 2) * 0.035);
                }
                if (m2jQ.prop('tagName') == 'circle') {
                    m2svg.x = m2jQ.attr('cx');
                    m2svg.y = m2jQ.attr('cy');
                }
                else {
                    var translate = m2jQ.attr('transform').replace(/\ scale\([0-9\.]+\)/, '').replace('translate(', '').replace(')', '').split(',');
                    var bbox = m2jQ.getBBox();
                    m2svg.x = parseInt(translate[0]) + ((bbox.width / 2) * 0.035);
                    m2svg.y = parseInt(translate[1]) + ((bbox.height / 2) * 0.035);
                }
                drawLineBetweenMileposts(m1svg.x, m1svg.y, m2svg.x, m2svg.y, players[i].pid);
            }
        }
    }
};

var refreshMoney = function (money) {
    $('#money').empty();
    $('#money').append('<span>' + money + '</span>');
}