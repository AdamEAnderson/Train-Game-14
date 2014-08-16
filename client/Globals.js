/// <reference path="libraries/raphael.js" />
/// <reference path="http://code.jquery.com/jquery-2.0.0.js" /> 
/// <reference path="http://underscorejs.org/underscore.js" /> 
/// <reference path="http://code.jquery.com/ui/jquery-ui-1-9-git.js" /> 
/// <reference path="libraries/raphael-pan-zoom.js" /> 
/// <reference path="libraries/d3.js" /> 

//Globals
var paper, panZoom; //Raphael svg and pan-zoom plug-in objects
//Polyfill for location.origin
if (!window.location.origin) {
    window.location.origin = window.location.protocol + "//" + window.location.hostname + (window.location.port ? ':' + window.location.port : '');
}
var server = location.origin.replace(/\:[0-9]+/, '') + ':8080'; //Defines java server URL
var pid, gid; //Player ID and game ID
var lastStatus; //Number that matches the transaction number of the last status message processed
var lastStatusMessage; //The last status message processed
var yourTurn; //Boolean for if it is your turn
var gameData; //Data that is sent from the server on newGame or joinGame that contains map and game information
var mapViewboxHeight, mapViewboxWidth; //Width and height of the background map svg
var mileposts = {}; //Object containing milepost svgs loaded through ajax on initialization
var milepostsNeeded = ['DESERT', 'MOUNTAIN', 'ALPINE', 'JUNGLE', 'FOREST', 'CHUNNEL']; //List of milepost svg needed
var geography; //The name of the map that is being played
var started = false; //Boolean for if the game has been started
var placedTrain = false; //Boolean for if the player has placed all trains
var moneySpent = 0; //Integer for money spent during the players turn
var moneySpentThisBuild = 0; //Integer for money spent while building
var verticesBuilt; //Array for mileposts built while building
var verticesBuiltFinal = []; //Array for mileposts built and confirmed by server
var milepostEdgesBuilt; //Array for edges built while building
var edgesBuiltFinal = []; //Array for edges built and confirmed by server
var otherPlayersEdgesBuilt = []; //Array for edges built by other players
var movesMade; //Array for all moves made while moving
var movesMadeThisTurn = []; //Array for al moves made this turn confirmed by server
var edgesBuilt; //Array of all jQuery elements of edges drawn
var placeTrainLocations; //Array of objects where index corresponds to train index and objects have a milepost and a train property
var trainLocations = []; //Array of objects where index corresponds to train index and object are mileposts

//Adds a clone function to the array prototype that deep-copies the array
Array.prototype.clone = function () {
    return this.slice(0);
};

//Join path URIs
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