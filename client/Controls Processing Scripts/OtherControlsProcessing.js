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
/// <reference path="../Controls Processing/DialogStages.js" /> 

var loadsClick = function(e) {
	var data = e.data;
    $('#lobby').append('<div id="loadsDialog" title="Loads Key" />').find('div:last').dialog({
        buttons: [{
            text: "Close",
            click: function () {
                $(this).dialog('destroy');
                $('#loadsDialog').remove();
            }
        }],
        close: function() {
        	$(this).dialog('destroy');
            $('#loadsDialog').remove();
        },
        width:'60%',
        height:500
    });
    var iconPath = location.origin + join(location.pathname, '../../data/icons');
    $('#loadsDialog').append('<table/>');
    for (var key in gameData.loadset) {
    	iconPNG =  iconPath + '/' + key + '.png';
    	$('#loadsDialog > table').append('<tr><td><img width=30px height=30px src=' + iconPNG + '/><td>' + key + '</td><td>' + gameData.loadset[key].join(', ') + '</td></tr>');
    }
};

var resignClick = function() {
    $('#turnControls').hide();
    $('#endControls').hide();
    $('#okControls').show();
    $('#acceptBuild').click(function() {
		resignGame();
		$('#okControls').hide();
	});
		$('#cancelBuild').click(function() {
		$('#okControls').hide();
	});
};

