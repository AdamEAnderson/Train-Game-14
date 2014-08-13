//Global Variables
var paper, panZoom;
//var server = 'http://127.0.0.1:8080';
var server = location.origin.replace(/\:[0-9]+/,'') + ':8080';
var pid, gid;
var lastStatus;
var lastStatusMessage;
var yourTurn;
var gameData;
var mapHeight, mapWidth;
var mileposts = {};
var milepostsNeeded = ['DESERT', 'MOUNTAIN', 'ALPINE', 'JUNGLE'/*, 'FOREST'*/];
var geography;
var started = false;
var placedTrain = false;
var moneySpent = 0;
var moneySpentThisBuild = 0;
var verticesBuilt;
var edgesBuilt;

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

var upgradeDialogStageTwo = function(train,skippedStageOne){
	var upgrades = [];
	if(train.capacity < 3) {
		upgrades.push('Upgrade Capacity');
	}
	if(train.speed < 20) {
		upgrades.push('Upgrade Speed');
	}
	if(upgrades.length == 1) {
		if(skippedStageOne){
			$('#upgradeDialog').append('<p>Are you sure you want to upgrade?</p>');
			var buttons = $('#upgradeDialog').dialog('option','buttons');
			buttons.push({
				text:'Yes',
				click:function(){
					upgradedTrain(upgrades[0].replace('Upgrade ',''));
					if((upgrades[0] == 'Upgrade Speed' && train.speed == 16) || (upgrades[0] == 'Upgrade Capacity' && train.capacity == 2))
						$('#upgrade').hide();
					$('#upgradeDialog').dialog('destroy');
					$('#upgradeDialog').remove();
				}
			});
			$('#upgradeDialog').dialog('option','buttons',buttons);
		}
		else {
			upgradedTrain(upgrades[0].replace('Upgrade ',''));
			checkUpgradeHiding();
			$('#upgradeDialog').dialog('destroy');
			$('#upgradeDialog').remove();
		}
	}
	else if(upgrades.length == 2){
		$('#upgradeDialog').append('<ul/>');
		for(var i = 0; i < upgrades.length; i++) {
			$('#upgradeDialog > ul').append('<li>' + upgrades[i] + '</li>').find('li:last').click(function(){
				$('#upgradeDialog > ul > li').removeClass('clicked');
				$(this).addClass('clicked');
			});
		}
		var buttons = $('#upgradeDialog').dialog('option','buttons');
		buttons.push({
			text:'Upgrade',
			click:function(){
				if($('#upgradeDialog > ul > li.clicked').length == 1) {
					upgradedTrain($('#upgradeDialog > ul > li.clicked').text().replace('Upgrade ',''));
					$('#upgradeDialog').dialog('destroy');
					$('#upgradeDialog').remove();
				}
			}
		});
		$('#upgradeDialog').dialog('option','buttons',buttons);
	}
};

Array.prototype.clone = function() {
	return this.slice(0);
};

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
	post({messageType:'resumeGame', gid:GID, pid:handle}, function(data) {
		gameData = data;
		gid = GID; 
		pid = handle;
		for (var i = 0; i < gameData.mapData.orderedMileposts.length; ++i)
			gameData.mapData.orderedMileposts[i] = JSON.parse(gameData.mapData.orderedMileposts[i]);
		geography = data.geography;
		console.log("resume game: " + gid);
		enterLobby();
	});
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
	$('#lobby').append($('<div id="topBar"/>').append('<div id="players"/>','<div id="controls"/>'));
	$('#lobby').append('<div id="map"/>');
	$('#lobby').append('<div id="handAndTrains"><div id="hand"/><div id="trains"/><div id="money"/>');
	$('#lobby').append('<div id="mapControls"><a id="up" href="javascript:;"></a><a id="down" href="javascript:;"></a></div>');
	$('#lobbyMenuJUI').menu();
	$('#controls').append('<input id="startGame" type="checkbox"><label for="startGame">Start Game</label></input>');
	$('#controls').buttonset();
	$('#startGame').change(function(){
		startGame($('#startGame')[0].checked);
	});
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
					$('#milepostsGroup > circle:last').attr('id','milepost' + w.toString() + ',' + h.toString());
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
					$('#milepostsGroup > circle:last').attr('id','milepost' + w.toString() + ',' + h.toString());
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
					$('#milepostsGroup').find('g:last').attr('transform','translate(' + x + ',' + y + ') scale(' + scale + ')').attr('id','milepost' + w + ',' + h);
					break;
			}
			++mp;
		}
	}
	$('#milepostsGroup > *:not(path)').click(function(){
		console.log($(this).attr('id').replace('milepost',''));
		var currentMilepost = $(this).attr('id').replace('milepost','').split(',');
		console.log(gameData.mapData.orderedMileposts[(currentMilepost[1] * gameData.mapData.mpWidth) + parseInt(currentMilepost[0])]);
	});
}

//Tells server to start the game(from host)
var startGame = function(checked) {
	post({messageType:'startGame', gid:gid, pid:pid, ready:checked});
};

//Tells server we've built track
var builtTrack = function(vertices) {
	post({messageType:'buildTrack',pid:pid,gid:gid,mileposts:vertices});
};

//Tells server we've started our train
var startedTrain = function(position) {
	post({messageType:'startTrain',pid:pid,gid:gid,position:position});
};

//Tells server we've upgraded our train
var upgradedTrain = function(trainState) {
	moneySpent += 20;
	checkBuildMoney();
	post({messageType:'upgradeTrain',pid:pid,gid:gid,upgradeType:trainState});
};

//Tells server we're done with our turn
var endTurn = function() {
	post({messageType:'endTurn',pid:pid,gid:gid});
	moneySpent = 0;
	checkBuildMoney();
	$('#turnControls').buttonset('option','disabled',true);
};

//Tells server our game is done(from host)
var endGame = function(checked) {
	post({messageType:'endGame',pid:pid,gid:gid,ready:checked});
}

//Gets a status response from the server
var statusGet = function() {
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
		$('#players').append($('<input type="radio" id="' + i + '"><label for="' + i + '">' + players[i].pid + '</label></input>').attr({'readonly':'','disabled':''}));
	}
	$('#players').buttonset();
};

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
};

var refreshTrains = function(trains) {
	$('#trains').empty();
	for (var t = 0; t < trains.length; ++t) {
		$('#trains').append('<div class="train"/>');
		$('#trains').children().eq(t).append('<p><span>' + trains[t].speed + '</span></p>');
		for (var l = 0; l < trains[t].loads.length; ++l)
			$('#trains').children().eq(t).append('<p><span>' + trains[t].loads[l] + '</span></p>');
	}
};

var refreshMoney = function(money) {
	$('#money').empty();
	$('#money').append('<span>' + money + '</span>');
}

var findPid = function(players, pid) {
	for(var i = 0; i < players.length; i++)
		if (players[i].pid == pid)
			return players[i];
	return null;
};

var checkBuildMoney = function(){
	if(moneySpent > 0) {
		$('#upgrade').button('option','disabled',true);
	}
	else {
		$('#upgrade').button('option','disabled',false);
	}
	if(moneySpent == 20) {
		$('#build').button('option','disabled',true);
	}
};

var drawLineBetweenMileposts = function(x1, y1, x2, y2, PID) {
	$('#pid' + PID).append($(document.createElementNS('http://www.w3.org/2000/svg','line')).attr({x1:x1,y1:y1,x2:x2,y2:y2}).css({'stroke-width':'4px','stroke':findPid(lastStatusMessage.players,PID).color}));
	edgesBuilt.push($('#pid' + PID + ' > line:last'))
}

//Processes a status response from the server
var processStatus = function(data) {
	if (data.transaction != lastStatus) {
		if(!started && data.joinable == false) {
			started = true;
			console.log('Started Game!');
			$('#controls').buttonset('destroy');
			$('#controls').empty();
			$('#controls').append('<div id="turnControls"/>');
			$('#controls').append('<div id="endControls"/>');
			$('#controls').append('<div id="buildControls"/>').find('div:last').hide();
			$('#buildControls').append('<button id="acceptBuild">OK</button>')
			$('#buildControls').append('<button id="cancelBuild">Cancel</button>');
			$('#endControls').append('<input type="checkbox" id="endGame"><label for="endGame">End Game</label></input>');
			$('#endControls').append('<button id="resign">Resign</button>');
			$('#endGame').change(function() {
				endGame($('#endGame')[0].checked);
			});
			$('#resign').click(function() {
				//Open confirm box for them and make sure they want to resign
				//If they do, post to the server telling it to resign this player
			});
			$('#turnControls').append('<button id="build">Build</button>').find('button:last').click(function(){
				$('#turnControls').hide();
				$('#endControls').hide();
				$('#buildControls').show();
				moneySpentThisBuild = 0;
				verticesBuilt = [];
				edgesBuilt = []
				var milepostsClick = function(){
					if(verticesBuilt.length == 0) {
						var currentMilepost = $(this).attr('id').replace('milepost','').split(',');
						currentMilepost = gameData.mapData.orderedMileposts[(currentMilepost[1] * gameData.mapData.mpWidth) + parseInt(currentMilepost[0])];
						verticesBuilt.push({x:currentMilepost.x,y:currentMilepost.y});
						return;
					}
					var lastMilepost = verticesBuilt[verticesBuilt.length - 1];
					lastMilepost = gameData.mapData.orderedMileposts[(lastMilepost.y * gameData.mapData.mpWidth) + lastMilepost.x];
					var currentMilepost = $(this).attr('id').replace('milepost','').split(',');
					currentMilepost = gameData.mapData.orderedMileposts[(currentMilepost[1] * gameData.mapData.mpWidth) + parseInt(currentMilepost[0])];
					var isValidMilepost = false;
					var milepostCost;
					for(var i = 0; i < currentMilepost.edges.length; i++) {
						if(currentMilepost.edges[i].x == lastMilepost.x && currentMilepost.edges[i].y == lastMilepost.y) {
							isValidMilepost = true;
							milepostCost = currentMilepost.edges[i].cost;
							break;
						}
					}
					if(isValidMilepost == false)
						return;
					var lastX, lastY;
					if($(document.getElementById('milepost' + lastMilepost.x + ',' + lastMilepost.y)).prop("tagName") == 'circle') {
						lastX = $(document.getElementById('milepost' + lastMilepost.x + ',' + lastMilepost.y)).attr('cx');
						lastY = $(document.getElementById('milepost' + lastMilepost.x + ',' + lastMilepost.y)).attr('cy');
					}
					else if($(document.getElementById('milepost' + lastMilepost.x + ',' + lastMilepost.y)).prop("tagName") == 'g') {
						var translate = $(document.getElementById('milepost' + lastMilepost.x + ',' + lastMilepost.y)).attr('transform').replace(/\ scale\([0-9\.]+\)/,'').replace('translate(','').replace(')','').split(',');
						var bbox = $(document.getElementById('milepost' + lastMilepost.x + ',' + lastMilepost.y))[0].getBBox();
						lastX = parseInt(translate[0]) + ((bbox.width / 2) * 0.035);
						lastY = parseInt(translate[1]) + ((bbox.height / 2) * 0.035);
					}
					var currentX, currentY;
					if($(this).prop("tagName") == 'circle') {
						currentX = $(this).attr('cx');
						currentY = $(this).attr('cy');
					}
					else if($(this).prop("tagName") == 'g') {
						var translate = $(this).attr('transform').replace(/\ scale\([0-9\.]+\)/,'').replace('translate(','').replace(')','').split(',');
						var bbox = $(this)[0].getBBox();
						currentX = parseInt(translate[0]) + ((bbox.width / 2) * 0.035);
						currentY = parseInt(translate[1]) + ((bbox.height / 2) * 0.035);
					}
					if(!document.getElementById('milepost' + lastMilepost.x + ',' + lastMilepost.y))
						return;
					if(20 - (moneySpent + moneySpentThisBuild + milepostCost) < 0)
						return;
					drawLineBetweenMileposts(lastX,lastY,currentX,currentY,pid);
					verticesBuilt.push({x:currentMilepost.x,y:currentMilepost.y});
					moneySpentThisBuild += milepostCost;
					
				};
				$('#milepostsGroup > *:not(path)').click(milepostsClick);
				var acceptBuild = function(){
					builtTrack(verticesBuilt);
					$('#acceptBuild').off('click');
					$('#cancelBuild').off('click');
					$('#milepostsGroup > *:not(path)').off('click',milepostsClick);
					$('#buildControls').hide();
					$('#turnControls').show();
					$('#endControls').show();
					verticesBuilt = [];
					edgesBuilt = [];
					moneySpent += moneySpentThisBuild;
					moneySpentThisBuild = 0;
					checkBuildMoney();
				};
				$('#acceptBuild').click(acceptBuild);
				var cancelBuild = function(){
					$('#acceptBuild').off('click');
					$('#cancelBuild').off('click');
					$('#milepostsGroup > *:not(path)').off('click',milepostsClick);
					$('#buildControls').hide();
					$('#turnControls').show();
					$('#endControls').show();
					verticesBuilt = [];
					for(var i = 0; i < edgesBuilt.length; i++) {
						$(edgesBuilt[i]).remove();
					}
					edgesBuilt = [];
					moneySpentThisBuild = 0;
				};
				$('#cancelBuild').click(cancelBuild);
			});
			$('#turnControls').append('<button id="upgrade">Upgrade</button>').find('button:last').click(function(){
				for(var i = 0; i < lastStatusMessage.players.length; i++)
					if(lastStatusMessage.players[i].pid == pid){
						var player = lastStatusMessage.players[i];
						$('#lobby').append('<div id="upgradeDialog" title="Upgrade Your Train" />').find('div:last').dialog({
							dialogClass: "no-close",
							buttons: [{
								text: "Cancel",
								click: function() {
									$( this ).dialog( "destroy" );
									$('#upgradeDialog').remove();
								}
							}]
						});
						var trains = player.trains.clone();
						for(var i = 0; i < trains.length; i++){
							if(trains[i].capacity == 3 && trains[i].speed == 20){
								trains.splice(i,1);
							}
						}
						if(trains.length > 1) {
							$('#upgradeDialog').append('<ul/>');
							for(var i = 0; i < player.trains.length; i++){
								$('#upgradeDialog > ul').append('<li>Train ' + (i + 1) + '</li>').find('li:last').click(function(){
									$('#upgradeDialog > ul > li').removeClass('clicked');
									$(this).addClass('clicked');
								});
							}
							var buttons = $('#upgradeDialog').dialog('option','buttons');
							buttons.push({
								text:"OK",
								click:function(){
									$('#upgradeDialog').empty();
									var buttons = $('#upgradeDialog').dialog('option','buttons');
									buttons.pop();
									$('#upgradeDialog').dialog('option','buttons',buttons);
									upgradeDialogStageTwo(player.trains[parseInt($('#upgradeDialog > ul > li.clicked').text().replace('Train ',''))],false);
								}
							});
							$('#upgradeDialog').dialog('option','buttons',buttons);
						}
						else if(trains.length == 1) {
							upgradeDialogStageTwo(player.trains[0],true);
						}
						else if(trains.length == 0) {
							checkUpgradeHiding();
							$('#upgradeDialog').dialog('destroy');
							$('#upgradeDialog').remove();
						}
					}
			});
			$('#turnControls').append($('<button id="move">Move</button>').hide());
			$('#turnControls').append($('<button id="deliver">Deliver</button>').hide());
			$('#turnControls').append($('<button id="pickup">Pickup</button>').hide());
			$('#turnControls').append($('<button id="drop">Drop</button>').hide());
			$('#turnControls').append($('<button id="placeTrain">Place Train</button>').hide().click(function(){
				//Place Train Code Goes Here
				$('#move').show();
				$('#drop').show();
				$('#placeTrain').hide();
				placedTrain = true;
			}));
			$('#turnControls').append('<button id="endTurn">End Turn</button>').find('button:last').click(endTurn);
			$('#turnControls').buttonset();
			$('#turnControls').buttonset('option','disabled',data.activeid != pid)
			$('#endControls').buttonset();
			$('#buildControls').buttonset();
			yourTurn = data.activeid == pid;
			$('#map > svg').append($(document.createElementNS('http://www.w3.org/2000/svg','g')).attr('id','track'));
			for(var i = 0; i < data.players.length; i++) {
				$('#track').append($(document.createElementNS('http://www.w3.org/2000/svg','g')).attr('id','pid' + data.players[i].pid));
			}
		}
		if (!geography && data.geography) {
			geography = data.geography;
			initMap(data.geography);
		}
		if(data.turns == 3 && placedTrain == false){
			$('#placeTrain').show();
		}
		refreshPlayers(data.players);
		if(started)
			$('#0').next().addClass('ui-state-active');
		me = findPid(data.players, pid);
		refreshCards(me.hand);
		refreshTrains(me.trains);
		refreshMoney(me.money);
		if(data.activeid && data.activeid == pid) {
			yourTurn = true;
			$('#turnControls').buttonset('option','disabled',false);
			checkBuildMoney();
		}
		else if(data.activeid && data.activeid != pid) {
			yourTurn = false;
			$('#turnControls').buttonset('option','disabled',true);
		}
		lastStatusMessage = data;
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
