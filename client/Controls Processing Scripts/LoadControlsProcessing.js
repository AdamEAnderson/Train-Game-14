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
/// <reference path="../Controls Processing Scripts/BuildControlsProcessing.js" /> 

var deliverClick = function (e) {
    var data = e.data;
    var deliveries = [];
    var player = findPid(lastStatusMessage.players,pid);
    for (var i = 0; i < player.hand.length; i++) {
        for (var j = 0; j < player.hand[i].trips.length; j++) {
            var trip = player.hand[i].trips[j];
            for(var l = 0; l < player.trains.length; l++){
            	var milepost = JSON.parse(player.trains[l].loc);
            	if (trip.dest == milepost.city.name) {
            		var train = player.trains[l];
                	for (var k = 0; k < train.loads.length; k++) {
                    	if (train.loads[k] == trip.load) {
                    	    deliveries.push({ train: k, load: trip.load, city: trip.dest, card: i });
                    	}
                	}
                }
            }
        }
    }
    $('#lobby').append('<div id="deliverDialog" title="Deliver a load" />').find('div:last').dialog({
        dialogClass: "no-close",
        buttons: [{
            text: "Cancel",
            click: function () {
                $(this).dialog('destroy');
                $('#deliverDialog').remove();
            }
        }]
    });
    if (deliveries.length == 1) {
        $('#deliverDialog').append('<p>Are you sure you want to deliver ' + deliveries[0].load.toLowerCase() + ' to ' + deliveries[0].city + '?</p>');
        var buttons = $('#deliverDialog').dialog('option', 'buttons');
        buttons.push({
            text: 'Yes',
            click: function () {
                var buttons = $('#deliverDialog').dialog('option', 'buttons');
                buttons.pop();
                $('#deliverDialog').dialog('option', 'buttons', buttons);
                deliverLoad(deliveries[0].train, deliveries[0].load, deliveries[0].card);
                $('#deliverDialog').dialog('destroy');
                $('#deliverDialog').empty().remove();
            }
        });
        $('#deliverDialog').dialog('option', 'buttons', buttons);
    }
    else {
        $('#deliverDialog').append('<ul/>');
        for (var i = 0; i < deliveries.length; i++) {
            $('#deliverDialog > ul').append('<li>Deliver ' + deliveries[i].load.toLowerCase() + ' to ' + deliveries[i].city + '</li>').find('li:last').click(function () {
                $('#deliverDialog > ul > li').removeClass('clicked');
                $(this).addClass('clicked');
            }).attr('id', 'delivery' + i);
        }
        var buttons = $('#deliverDialog').dialog('option', 'buttons');
        buttons.push({
            text: 'Deliver',
            click: function () {
                if ($('#deliverDialog > ul > li.clicked').length == 1) {
                    var buttons = $('#deliverDialog').dialog('option', 'buttons');
                    buttons.pop();
                    $('#deliverDialog').dialog('option', 'buttons', buttons);
                    var deliver = deliveries[parseInt($('#deliverDialog > ul > li.clicked').attr('id').replace('delivery', ''))];
                    deliverLoad(deliver.train, deliver.load, deliver.card);
                    $('#deliverDialog').dialog('destroy');
                    $('#deliverDialog').empty().remove();
                }
            }
        });
        $('#deliverDialog').dialog('option', 'buttons', buttons);
    }
};

var pickupClick = function (e) {
    var data = e.data;
    var player = findPid(lastStatusMessage.players, pid);
    var validTrains = [];
    for (var i = 0; i < player.trains.length; i++) {
        var milepost = JSON.parse(player.trains[i].loc);
        if (milepost.type == 'CITY' || milepost.type == 'MAJORCITY') {
            validTrains.push(player.trains[i]);
        }
    }
    if (validTrains.length == 0)
        return;
    $('#lobby').append('<div id="pickupDialog" title="Pickup a load" />').find('div:last').dialog({
        dialogClass: "no-close",
        buttons: [{
            text: "Cancel",
            click: function () {
                $(this).dialog("destroy");
                $('#pickupDialog').remove();
            }
        }]
    });
    if (validTrains.length == 1) {
        pickupDialogStageTwo(validTrains[0], true);
    }
    else if (validTrains.length > 1) {
        $('#pickupDialog').append('<ul/>');
        for (var i = 0; i < player.trains.length; i++) {
            if (!validTrains.contains(player.trains[i]))
                continue;
            $('#pickupDialog > ul').append('<li>Train ' + (i + 1) + '</li>').find('li:last').click(function () {
                $('#pickupDialog > ul > li').removeClass('clicked');
                $(this).addClass('clicked');
            });
        }
        var buttons = $('#pickupDialog').dialog('option', 'buttons');
        buttons.push({
            text: "OK",
            click: function () {
                $('#pickupDialog').empty();
                var buttons = $('#pickupDialog').dialog('option', 'buttons');
                buttons.pop();
                $('#pickupDialog').dialog('option', 'buttons', buttons);
                pickupDialogStageTwo(player.trains[parseInt($('#pickupDialog > ul > li.clicked').text().replace('Train ', '')) - 1], false);
            }
        });
        $('#pickupDialog').dialog('option', 'buttons', buttons);
    }
};

var dumpClick = function (e) {
    var data = e.data;
    var validTrains = [];
    var player = findPid(lastStatusMessage.players, pid);
    for (var i = 0; i < player.trains.length; i++) {
        for (var j = 0; j < player.trains[i].loads.length; j++) {
            if (player.trains[i].loads[j] != null) {
                validTrains[i] = player.trains[i];
            }
        }
    }
    if (validTrains.length == 0)
        return;
    $('#lobby').append('<div id="dumpDialog" title="Dump a load" />').find('div:last').dialog({
        dialogClass: "no-close",
        buttons: [{
            text: "Cancel",
            click: function () {
                $(this).dialog("destroy");
                $('#dumpDialog').remove();
            }
        }]
    });
    if (validTrains.length == 1) {
        dumpDialogStageTwo(validTrains[0], true);
    }
    else if (validTrains.length > 1) {
        $('#dumpDialog').append('<ul/>');
        for (var i = 0; i < player.trains.length; i++) {
            if (!validTrains.contains(player.trains[i]))
                continue;
            $('#dumpDialog > ul').append('<li>Train ' + (i + 1) + '</li>').find('li:last').click(function () {
                $('#dumpDialog > ul > li').removeClass('clicked');
                $(this).addClass('clicked');
            });
        }
        var buttons = $('#dumpDialog').dialog('option', 'buttons');
        buttons.push({
            text: "OK",
            click: function () {
                var buttons = $('#dumpDialog').dialog('option', 'buttons');
                buttons.pop();
                $('#dumpDialog').dialog('option', 'buttons', buttons);
                var train = player.trains[parseInt($('#dumpDialog > ul > li.clicked').text().replace('Train ', '')) - 1];
                $('#dumpDialog').empty();
                dumpDialogStageTwo(train, false);
            }
        });
        $('#dumpDialog').dialog('option', 'buttons', buttons);
    }
};