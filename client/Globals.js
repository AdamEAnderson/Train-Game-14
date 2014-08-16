/// <reference path="libraries/raphael.js" />
/// <reference path="http://code.jquery.com/jquery-2.0.0.js" /> 
/// <reference path="http://underscorejs.org/underscore.js" /> 
/// <reference path="http://code.jquery.com/ui/jquery-ui-1-9-git.js" /> 
/// <reference path="libraries/raphael-pan-zoom.js" /> 
/// <reference path="libraries/d3.js" /> 

var paper, panZoom;
if (!window.location.origin) {
    window.location.origin = window.location.protocol + "//" + window.location.hostname + (window.location.port ? ':' + window.location.port : '');
}
var server = location.origin.replace(/\:[0-9]+/, '') + ':8080';
var pid, gid;
var lastStatus;
var lastStatusMessage;
var yourTurn;
var gameData;
var mapHeight, mapWidth;
var mileposts = {};
var milepostsNeeded = ['DESERT', 'MOUNTAIN', 'ALPINE', 'JUNGLE', 'FOREST', 'CHUNNEL'];
var geography;
var started = false;
var placedTrain = false;
var moneySpent = 0;
var moneySpentThisBuild = 0;
var verticesBuilt;
var verticesBuiltFinal = [];
var milepostEdgesBuilt;
var edgesBuiltFinal = [];
var movesMade;
var movesMadeThisTurn = [];
var edgesBuilt;
var placeTrainLocations;
var trainLocations = [];

Array.prototype.clone = function () {
    return this.slice(0);
};

//Join paths
var join = function (/* path segments */) {
    // Split the inputs into a list of path commands.
    var parts = [];
    for (var i = 0, l = arguments.length; i < l; i++) {
        parts = parts.concat(arguments[i].split("/"));
    }
    // Interpret the path commands to get the new resolved path.
    var newParts = [];
    for (i = 0, l = parts.length; i < l; i++) {
        var part = parts[i];
        // Remove leading and trailing slashes
        // Also remove "." segments
        if (!part || part === ".") continue;
        // Interpret ".." to pop the last segment
        if (part === "..") newParts.pop();
            // Push new path segments.
        else newParts.push(part);
    }
    // Preserve the initial slash if there was one.
    if (parts[0] === "") newParts.unshift("");
    // Turn back into a single string path.
    return newParts.join("/") || (newParts.length ? "/" : ".");
};

//Post
var post = function (data, callback) {
    $.ajax({
        type: "POST",
        url: server,
        data: JSON.stringify(data),
        success: callback,
        error: function (xhr, textStatus, errorThrown) {
            console.log("error " + textStatus + " " + errorThrown + " " + xhr.responseText);
        },
        dataType: 'json'
    });

};