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

var upgradeDialogStageTwo = function (train, skippedStageOne) {
    var upgrades = [];
    if (train.capacity < 3) {
        upgrades.push('Upgrade Capacity');
    }
    if (train.speed < 20) {
        upgrades.push('Upgrade Speed');
    }
    if (upgrades.length == 1) {
        if (skippedStageOne) {
            $('#upgradeDialog').append('<p>Are you sure you want to upgrade?</p>');
            var buttons = $('#upgradeDialog').dialog('option', 'buttons');
            buttons.push({
                text: 'Yes',
                click: function () {
                    upgradedTrain(upgrades[0].replace('Upgrade ', ''));
                    if ((upgrades[0] == 'Upgrade Speed' && train.speed == 16) || (upgrades[0] == 'Upgrade Capacity' && train.capacity == 2))
                        $('#upgrade').hide();
                    $('#upgradeDialog').dialog('destroy');
                    $('#upgradeDialog').remove();
                }
            });
            $('#upgradeDialog').dialog('option', 'buttons', buttons);
        }
        else {
            upgradedTrain(upgrades[0].replace('Upgrade ', ''));
            checkUpgradeHiding();
            $('#upgradeDialog').dialog('destroy');
            $('#upgradeDialog').remove();
        }
    }
    else if (upgrades.length == 2) {
        $('#upgradeDialog').append('<ul/>');
        for (var i = 0; i < upgrades.length; i++) {
            $('#upgradeDialog > ul').append('<li>' + upgrades[i] + '</li>').find('li:last').click(function () {
                $('#upgradeDialog > ul > li').removeClass('clicked');
                $(this).addClass('clicked');
            });
        }
        var buttons = $('#upgradeDialog').dialog('option', 'buttons');
        buttons.push({
            text: 'Upgrade',
            click: function () {
                if ($('#upgradeDialog > ul > li.clicked').length == 1) {
                    upgradedTrain($('#upgradeDialog > ul > li.clicked').text().replace('Upgrade ', ''));
                    $('#upgradeDialog').dialog('destroy');
                    $('#upgradeDialog').remove();
                }
            }
        });
        $('#upgradeDialog').dialog('option', 'buttons', buttons);
    }
};

var pickupDialogStageTwo = function (train, skippedStageOne) {
    var milepost = JSON.parse(train.loc);
    var loads = milepost.city.loads;
    var player = findPid(lastStatusMessage.players, pid);
    var city = milepost.city;
    if (loads.length == 1) {
        if (skippedStageOne) {
            $('#pickupDialog').append('<p>Are you sure you want to pickup ' + loads[0].toLowerCase() + '?</p>');
            var buttons = $('#pickupDialog').dialog('option', 'buttons');
            buttons.push({
                text: 'Yes',
                click: function () {
                    $('#pickupDialog').empty();
                    var buttons = $('#pickupDialog').dialog('option', 'buttons');
                    buttons.pop();
                    $('#pickupDialog').dialog('option', 'buttons', buttons);
                    pickupDialogStageThree(train, loads[0]);
                }
            });
            $('#pickupDialog').dialog('option', 'buttons', buttons);
        }
        else {
            $('#pickupDialog').empty();
            var buttons = $('#pickupDialog').dialog('option', 'buttons');
            buttons.pop();
            $('#pickupDialog').dialog('option', 'buttons', buttons);
            pickupDialogStageThree(train, loads[0]);
        }
    }
    else if (loads.length > 1) {
        var importantIndexes = [];
        for (var i = 0; i < city.loads.length; i++)
            for (var j = 0; j < player.hand.length; j++)
                for (var k = 0; k < player.hand[j].trips.length; k++)
                    if (player.hand[j].trips[k].load == city.loads[i])
                        importantIndexes.push(i);
        $('#pickupDialog').append('<ul/>');
        for (var i = 0; i < loads.length; i++) {
            var jQ = $('#pickupDialog > ul').append('<li>' + loads[i] + '</li>').find('li:last').click(function () {
                $('#pickupDialog > ul > li').removeClass('clicked');
                $(this).addClass('clicked');
            });
            if (importantIndexes.indexOf(i) != -1)
                jQ.addClass('important');
        }
        var buttons = $('#pickupDialog').dialog('option', 'buttons');
        buttons.push({
            text: 'Pickup',
            click: function () {
                if ($('#pickupDialog > ul > li.clicked').length == 1) {
                    var buttons = $('#pickupDialog').dialog('option', 'buttons');
                    buttons.pop();
                    $('#pickupDialog').dialog('option', 'buttons', buttons);
                    var load = $('#pickupDialog > ul > li.clicked').text();
                    $('#pickupDialog').empty();
                    pickupDialogStageThree(train, load);
                }
            }
        });
        $('#pickupDialog').dialog('option', 'buttons', buttons);
    }
};

var pickupDialogStageThree = function (train, load) {
    if (train.loads.indexOf(null) != -1) {
        pickupLoad(train.index, load);
        $('#pickupDialog').dialog('destroy');
        $('#pickupDialog').empty().remove();
    }
    else {
        $('#pickupDialog').append('<ul/>');
        for (var i = 0; i < train.loads.length; i++) {
            $('#pickupDialog > ul').append('<li>Dump ' + train.loads[i] + '</li>').find('li:last').click(function () {
                $('#pickupDialog > ul > li').removeClass('clicked');
                $(this).addClass('clicked');
            });
        }
        var buttons = $('#pickupDialog').dialog('option', 'buttons');
        buttons.push({
            text: 'Dump & Pickup',
            click: function () {
                if ($('#pickupDialog > ul > li.clicked').length == 1) {
                    var buttons = $('#pickupDialog').dialog('option', 'buttons');
                    buttons.pop();
                    $('#pickupDialog').dialog('option', 'buttons', buttons);
                    var drop = $('#pickupDialog > ul > li.clicked').text().replace('Dump ', '');
                    dumpLoad(train.index, drop);
                    pickupLoad(train.index, load);
                    $('#pickupDialog').dialog('destroy');
                    $('#pickupDialog').empty().remove();
                }
            }
        });
        $('#pickupDialog').dialog('option', 'buttons', buttons);
    }
};

var dumpDialogStageTwo = function (train, skippedStageOne) {
    var loads = [];
    for (var i = 0; i < train.loads.length; i++) {
        if (train.loads[i] != null) {
            loads.push(train.loads[i]);
        }
    }
    if (loads.length == 0) {
        $('#dumpDialog').dialog('destroy');
        $('#dumpDialog').empty().remove();
        return;
    }
    if (loads.length == 1 && !skippedStageOne) {
        dumpLoad(train.index, loads[0]);
        $('#dumpDialog').dialog('destroy');
        $('#dumpDialog').empty().remove();
    }
    else if (loads.length == 1 && skippedStageOne) {
        $('#dumpDialog').append('<p>Are you sure you want to dump ' + loads[0].toLowerCase() + '?</p>');
        var buttons = $('#dumpDialog').dialog('option', 'buttons');
        buttons.push({
            text: 'Yes',
            click: function () {
                dumpLoad(train.index, loads[0]);
                $('#dumpDialog').dialog('destroy');
                $('#dumpDialog').empty().remove();
            }
        });
        $('#dumpDialog').dialog('option', 'buttons', buttons)
    }
    else {
        $('#dumpDialog').append('<ul/>');
        for (var i = 0; i < loads.length; i++) {
            $('#dumpDialog > ul').append('<li>Dump ' + train.loads[i] + '</li>').find('li:last').click(function () {
                $('#dumpDialog > ul > li').removeClass('clicked');
                $(this).addClass('clicked');
            });
        }
        var buttons = $('#dumpDialog').dialog('option', 'buttons');
        buttons.push({
            text: 'Dump',
            click: function () {
                if ($('#dumpDialog > ul > li.clicked').length == 1) {
                    var buttons = $('#dumpDialog').dialog('option', 'buttons');
                    buttons.pop();
                    $('#dumpDialog').dialog('option', 'buttons', buttons);
                    var drop = $('#dumpDialog > ul > li.clicked').text().replace('Dump ', '');
                    dumpLoad(train.index, drop);
                    $('#dumpDialog').dialog('destroy');
                    $('#dumpDialog').empty().remove();
                }
            }
        });
        $('#dumpDialog').dialog('option', 'buttons', buttons);
    }
};