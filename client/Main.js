//Global Variables
var paper, panZoom;
//var server = 'http://127.0.0.1:8080';
var server = location.origin.replace(/\:[0-9]+/,'') + ':8080';
var pid, gid;
var lastStatus;
var gameData;
var mapHeight, mapWidth;
var mileposts = {};
var milepostsNeeded = ['DESERT', 'MOUNTAIN', 'ALPINE', 'JUNGLE'/*, 'FOREST'*/];
var geography;

//Loaded
$(document).ready(function(){
	//Init display object
	//paper = Raphael('display',$('body').width(),$('body').height());
	//paper.circle(0,0,200);
	
	$(function() {
		$("#radio").buttonset();
	});
	
	//Init main menu
	$('#mainMenu').append('<h3 id="joinGameText">Train Game</h3>');
	//$('#mainMenu').append('<ul id="mainMenuJUI"><li>Join</li><li>New</li><li>Resume</li></ul>');
	//for(var i = 0; i < 10; i++){
		//$('#mainMenuJUI').append('<li>'+i+'</li>');
	//}
	//$('#mainMenuJUI').menu();
	$('#mainMenu').append('<select id="actionPicker"><option>New</option><option>Join</option><option>Resume</option></select>');
	$('#mainMenu').append('<select id="gamePicker"/>');
	$('#mainMenu').append('<h4 style="margin:10px 0px 5px 75px;">Handle</h4>');
	$('#mainMenu').append('<input id="handlePicker" type="text" size="32"style="width:200px;"/>');
	$('#mainMenu').append('<h4 style="margin:10px 0px 5px 60px;">Game Color</h4>');
	$('#mainMenu').append('<select id="colorPicker"><option>aqua</option><option>black</option><option>blue</option><option>fuchsia</option><option>gray</option><option>green</option><option>lime</option><option>maroon</option><option>navy</option><option>olive</option><option>orange</option><option>purple</option><option>red</option><option>silver</option><option>teal</option><option>yellow</option></select>');
	$('#mainMenu').append('<h4 id="geographyPicker-label" style="margin:10px 0px 5px 60px;">Geography</h4>');
	$('#mainMenu').append('<select id="geographyPicker"><option>africa</option></select>');
	$('#actionPicker').selectmenu({
		change: function( event, data ) {
			if (data.item.label == "New") {
				$('#gamePicker').empty();
				$('#gamePicker-button').hide();
			}
			else {
				$('#gamePicker-button').show().css('display','block');
				$('#geographyPicker-label').hide();
				$('#geographyPicker-button').hide();
				if (data.item.label == "Join")
					gameOption = "joinable";
				else
					gameOption = "resumeable";
				//Populate games list menu
				requestData = {messageType:'list', listType: gameOption};
				$.ajax({
					type:"GET",
					url:server,
					data: JSON.stringify(requestData),
					dataType: 'json',
					success: function(responseData) {
						processGames(responseData);
					},
					error: function(xhr, textStatus, errorThrown) {
						console.log("error " + textStatus + " " + errorThrown);
					}
				});
			}
		}
     });
	$('#colorPicker').selectmenu();
	$('#geographyPicker').selectmenu();
	$('#gamePicker').selectmenu();
	$('#colorPicker').css('font-size','0.8em');
	$('#mainMenu').append('<br/>');
	$('#mainMenu').append('<button id="newGameButton">OK</button>');
	$('#newGameButton').css('margin-top','15px');
	$('#newGameButton').button().click(function(){
		if($('#handlePicker').val() && $('#handlePicker').val().length > 0){
			if (document.getElementById("actionPicker").value == "New") {
				newGame(document.getElementById("colorPicker").value,$('#handlePicker').val(), 
					document.getElementById("geographyPicker").value);
			} else if (document.getElementById("actionPicker").value == "Join") {
				joinGame(document.getElementById("gamePicker").value,
					document.getElementById("colorPicker").value, $('#handlePicker').val());
			} else if (document.getElementById("actionPicker").value == "Resume") {
			}
		}
	});
	$('#mainMenu').show();
	
	lastStatus = 0;
	//statusGet();
});

var join = function(/* path segments */) {
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
}

milepostsNeeded.forEach(function(e){
	$.ajax({
		url:location.origin + join(location.pathname, '../../data/mileposts/' + e.toLowerCase() + '.svg'),
		success:function(d){
			mileposts[e] = d;
		}
	});
});

//Tells server we've joined a game
var joinGame = function(GID,color,handle) {
	post({messageType:'joinGame', gid:GID, color:color, pid:handle}, function(data) {
		gameData = data;
		gid = GID; 
		pid = handle;
		for (var i = 0; i < gameData.mapData.orderedMileposts.length; ++i)
			gameData.mapData.orderedMileposts[i] = JSON.parse(gameData.mapData.orderedMileposts[i]);
		geography = data.geography;
		console.log("join game: " + gid);
		enterLobby();
	});
};

//Tells server to resume a game
var resumeGame = function(GID,handle) {
	post({messageType:'resumeGame', gid:GID, pid:handle}, function(data){gid = GID; $('#mainMenu').hide(); $('#lobby').show();});
	pid = handle;
};

var newGame = function(color, handle, gameGeo) {
	post({messageType:'newGame', color:color, pid:handle, gameType:gameGeo}, function(data) {
		gameData = data;
		for (var i = 0; i < gameData.mapData.orderedMileposts.length; ++i)
			gameData.mapData.orderedMileposts[i] = JSON.parse(gameData.mapData.orderedMileposts[i]);
		gid = data.gid;
		pid = handle;
		geography = gameGeo;
		console.log("new game: " + gid);
		enterLobby();
	});
}


var initMap = function(geography) {
	var mapFile = '../../data/' + geography + '/map.svg';
	$.ajax({
		url: location.origin + join(location.pathname, mapFile),
		method:'GET',
		type:'text/plain',
		success: function(d){
			var groups = $(d).find('g');
			for(var i = 0; i < groups.length; i++){
				$(paper.canvas).append($(groups[i]));
			}
			var viewBox = $(d).find('svg').attr('viewBox').split(' ');
			//$('#map > svg').attr('viewBox',$(d).find('svg').attr('viewBox'));
			mapWidth = viewBox[2];
			mapHeight = viewBox[3];
			paper.setViewBox(viewBox[0],viewBox[1],viewBox[2],viewBox[3]);
			paper.setSize($('#map').width(),$('#map').height());
			panZoom = paper.panzoom({ dragModifier:5, initialZoom: 0, initialPosition: { x: 0, y: 0}, width:viewBox[2], height:viewBox[3] });
			//panZoom.enable();
			$(paper.canvas).on('dblclick',function(){
				panZoom.zoomIn(1);
			});
			$('#up').click(function(){
				panZoom.zoomIn(1);
			});
			$('#down').click(function(){
				panZoom.zoomOut(1);
			});
			$('#mainMenu').hide();
			$('#lobby').show();
			panZoom.enable();
			drawMileposts();
			setInterval('statusGet()', 2000);
		},
		error: function(a,b,c){
			console.log('error:' + arguments.toString());
		}
	});
}

var enterLobby = function() {
	$('#lobby').append('<ul id="lobbyMenuJUI"/>');
	$('#lobby').append('<div id="players"/>');
	$('#lobby').append('<div id="map"/>');
	$('#lobby').append('<div id="handAndTrains"><div id="hand"/><div id="trains"/><div id="money"/>');
	$('#lobby').append('<div id="mapControls"><a id="up" href="javascript:;"></a><a id="down" href="javascript:;"></a></div>');
	$('#lobbyMenuJUI').menu();
	paper = new Raphael('map',$('#map').width(),$('#map').height());
	$('#map').resize(function(){
		paper.setSize($('#map').width(),$('#map').height());
	});
	if (geography) 
		initMap(geography);
	else {
		// join/resume: we'll make the map once we have gotten a status message 
		// and know the geography
		setInterval('statusGet()', 2000);
	}
}

// Return true if the milepost is top left of major city.
// Top left will have preceding milepost not major city, following milepost is 
// major city, and two rows below is not major city.
// NOTE: incomplete major cities could mess this logic up!!
var firstMajorCityMilepost = function(mp) {
	return ((mp < 0 || gameData.mapData.orderedMileposts[mp - 1].type != 'MAJORCITY') && 
		 gameData.mapData.orderedMileposts[mp + 1].type == 'MAJORCITY' && 
		 mp + (gameData.mapData.mpWidth * 2) < gameData.mapData.orderedMileposts.length && 
		 gameData.mapData.orderedMileposts[mp + (gameData.mapData.mpWidth * 2)].type == 'MAJORCITY');
}

var drawMileposts = function() {
	var xDelta = gameData.mapData.mapWidth / gameData.mapData.mpWidth;
	var yDelta = gameData.mapData.mapHeight / gameData.mapData.mpHeight;
	console.log("xDelta: " + xDelta + " yDelta: " + yDelta);
	var mp = 0;
	var oddRowOffset = xDelta / 2;
	var milepostsGroup = paper.group(0,[]);
 	$('#map > svg > g:last').attr('id','milepostsGroup');
	for (var h = 0; h < gameData.mapData.mpHeight; ++h) {
		for (var w = 0; w < gameData.mapData.mpWidth; ++w) {
			//if(gameData.mapData.orderedMileposts[mp].x != w || gameData.mapData.orderedMileposts[mp].y != h)
				//console.log('INCORRECT MILEPOST: ' + gameData.mapData.orderedMileposts[mp].x + ',' + gameData.mapData.orderedMileposts[mp].y);
			var x = w * xDelta + gameData.mapData.leftOffset;
			var y = h * yDelta + gameData.mapData.topOffset;
			x = h % 2 == 1 ? x + oddRowOffset : x;
			switch(gameData.mapData.orderedMileposts[mp].type) {
				case 'CITY':
					milepostsGroup.push(paper.circle(x, y, 9).attr({'fill':'#d00','stroke-width':'0px','stroke':'#d00'}));
					break;
				case 'MAJORCITY':
					// Draw the outline of the major city when we get to the first (top
					// left) milepost of the city, so the stroke will come out below the
					// mileposts. 
					if (firstMajorCityMilepost(mp)) {
						var pathString = "M" + x + " " + y + 
							"L" + (x + xDelta) + " " + y + 
							"L" + (x + (xDelta * 1.5)) + " " + (y + yDelta) +
							"L" + (x + xDelta) + " " + (y + (yDelta * 2)) + 
							"L" + x + " " + (y + (yDelta * 2)) + 
							"L" + (x - (xDelta/2)) + " " + (y + yDelta) +
							"L" + x + " " + y;
						milepostsGroup.push(paper.path(pathString));
						$('#milepostsGroup > path:last').attr({'stroke-width':'4px','stroke':'#d00'});
						}
					// fall through
				case 'NORMAL':
					milepostsGroup.push(paper.circle(x, y, 3).attr({'fill':'#000','stroke-width':'0px'}));
					break;
				case 'BLANK':
					break;
				default:
					var jQ = $($(mileposts[gameData.mapData.orderedMileposts[mp].type]).find('svg').children()).clone();
					$('#milepostsGroup').append($(document.createElementNS('http://www.w3.org/2000/svg','g')).append(jQ));
					var bbox = $('#milepostsGroup').find('g:last')[0].getBBox();//$(mileposts[gameData.mapData.orderedMileposts[mp].type]).find('svg').attr('viewBox').split(' ');
					var scale = 0.035;
					x -= (bbox.width / 2) * scale;
					y -= (bbox.height / 2) * scale;
					$('#milepostsGroup').find('g:last').attr('transform','translate(' + x + ',' + y + ') scale(' + scale + ')');
					break;
			}
			++mp;
		}
	}
}

//Tells server to start the game(from host)
var startGame = function() {
	post({messageType:'startGame', gid:gid, pid:pid},function(){});
};

//Tells server we've built track
var builtTrack = function(edges) {
	post({messageType:'trackBuilt',pid:pid,gid:gid,edgesBuilt:edges},processStatus);
};

//Tells server we've started our train
var startedTrain = function(position) {
	post({messageType:'startedTrain',pid:pid,gid:gid,position:position},processStatus);
};

//Tells server we've upgraded our train
var upgradedTrain = function(trainState) {
	post({messageType:'upgradedTrain',pid:pid,gid:gid,upgradeState:trainState},processStatus);
};

//Tells server we're done with our turn
var endTurn = function() {
	post({messageType:'endTurn',pid:pid,gid:gid},processStatus);
};

//Tells server our game is done(from host)
var endGame = function() {
	post({messageType:'endGame',pid:pid,gid:gid},processStatus);
	gid = undefined;
	pid = undefined;
}

//Gets a status response from the server
var statusGet= function() {
	setTimeout(200,statusGet);
	
	//Request status from server
	//requestData = gid ? {messageType:'statusUpdate', pid:pid, gid:gid} : {messageType:'statusUpdate', pid:pid};
	requestData ={messageType:'status', gid:gid, pid:pid};
	$.ajax({
		type:"GET",
		url:server,
		data: JSON.stringify(requestData),
		dataType: 'json',
		success: function(responseData) {
			processStatus(responseData);
		},
		error: function(xhr, textStatus, errorThrown) {
			console.log("error " + textStatus + " " + errorThrown);
		}
	});
};

var refreshPlayers = function(players) {
	$('#players').empty();
	for(var i = 0; i < players.length; i++){
		$('#players').append('<input type="radio" id="' + i + '"><label for="' + i + '">' + players[i].pid + '</label></input>');
	}
	$('#players').buttonset();
}

var refreshCards = function(cards) {
	$('#hand').empty();
	for(var c = 0; c < cards.length; ++c) {
		$('#hand').append('<div class="card"/>');
		card = cards[c];
		for (var t = 0; t < card.trips.length; ++t) {
			$('#hand').children().eq(c).append('<div class="trip"><table><tr><td style="width:42%">' + card.trips[t].load + '</td><td style="width:42%">' + card.trips[t].dest + '</td><td style="width:6%">' +  card.trips[t].cost + '</td></tr></table></div>');
			//if (t == card.trips.length - 1)
			//	$('#hand').children().eq(c).append('<div class="trip-last"><p><span>' + card.trips[t].load + '</span><br/><span>' + card.trips[t].dest + '</span><br/><span>' + card.trips[t].cost);
			//else
			//	$('#hand').children().eq(c).append('<div class="trip"><p><span>' + card.trips[t].load + '</span><br/><span>' + card.trips[t].dest + '</span><br/><span>' + card.trips[t].cost);
		}
	}
}

var refreshTrains = function(trains) {
	$('#trains').empty();
	for (var t = 0; t < trains.length; ++t) {
		$('#trains').append('<div class="train"/>');
		$('#trains').children().eq(t).append('<p><span>' + trains[t].speed + '</span></p>');
		for (var l = 0; l < trains[t].loads.length; ++l)
			$('#trains').children().eq(t).append('<p><span>' + trains[t].loads[l] + '</span></p>');
	}
}

var refreshMoney = function(money) {
	$('#money').empty();
	$('#money').append('<span>' + money + '</span>');
}

var findPid = function(players, pid) {
	for(var i = 0; i < players.length; i++)
		if (players[i].pid == pid)
			return players[i];
	return null;
}

//Processes a status response from the server
var processStatus = function(data) {
	if (data.transaction != lastStatus) {
		if (!geography && data.geography) {
			geography = data.geography;
			initMap(data.geography);
		}
		refreshPlayers(data.players);
		me = findPid(data.players, pid);
		refreshCards(me.hand);
		refreshTrains(me.trains);
		refreshMoney(me.money);
		/*
		if(data.events) {
			for(var i = 0; i < data.events.length; i++) {
				if(data.events[i].type == 'startGame') {
					//Init game here
				}
			}
		} */
		lastStatus = data.transaction;
	}
};

// Process a game list
var processGames = function(data) {
	$('#gamePicker').empty();
	for(var i = 0; i < data.gids.length; i++){
		$('#gamePicker').append('<option>' + data.gids[i] + '</option>').click(data.gids[i],function(e){
		});
	}
	$('#gamePicker').menu('refresh');
};


//Post
var post = function(data,callback){
	$.ajax({
		type: "POST",
		url: server,
		data: JSON.stringify(data),
		success: callback,
		error: function(xhr, textStatus, errorThrown) {
			console.log("error " + textStatus + " " + errorThrown);
		},
		dataType: 'json'
	});
};
