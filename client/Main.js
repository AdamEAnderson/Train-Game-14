//Global Variables
var paper;
//var server = 'http://127.0.0.1:8080';
var server = 'http://localhost:8080';
var pid, gid;
var lastStatus;

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
	$('#actionPicker').selectmenu({
		change: function( event, data ) {
			if (data.item.label == "New") {
				$('#gamePicker').empty();
				$('#gamePicker-button').hide();
			}
			else {
				$('#gamePicker-button').show().css('display','block');
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
	$('#gamePicker').selectmenu();
	$('#colorPicker').css('font-size','0.8em');
	$('#mainMenu').append('<br/>');
	$('#mainMenu').append('<button id="newGameButton">OK</button>');
	$('#newGameButton').button().click(function(){
		if($('#handlePicker').val() && $('#handlePicker').val().length > 0){
			if (document.getElementById("actionPicker").value == "New") {
				newGame(document.getElementById("colorPicker").value,$('#handlePicker').val(), 
					"africa");
			} else if (document.getElementById("actionPicker").value == "Join") {
				joinGame(document.getElementById("gamePicker").value,
					document.getElementById("colorPicker").value, $('#handlePicker').val());
			} else if (document.getElementById("actionPicker").value == "Resume") {
			}
		}
	});
	$('#lobby').append('<ul id="lobbyMenuJUI"/>');
	$('#lobby').append('<div id="players"/>');
	$('#lobby').append('<div id="map"/>');
	$('#lobby').append('<div id="handAndTrains"><div id="hand"/><div id="trains"/><div id="money"/>');
	$('#lobbyMenuJUI').menu();
	paper = new Raphael('map',$('#map').width(),$('#map').height());
	$('#map').resize(function(){
		paper.setSize($('#map').width(),$('#map').height());
	});
	$('#mainMenu').show();
	
	lastStatus = 0;
	//statusGet();
});

//Tells server we've joined a game
var joinGame = function(GID,color,handle) {
	post({messageType:'joinGame', gid:GID, color:color, pid:handle}, function(data){
		gid = GID; 
		enterLobby();
		});
	pid = handle;
};

//Tells server to resume a game
var resumeGame = function(GID,handle) {
	post({messageType:'resumeGame', gid:GID, pid:handle}, function(data){gid = GID; $('#mainMenu').hide(); $('#lobby').show();});
	pid = handle;
};

var newGame = function(color, handle, gameGeo) {
	post({messageType:'newGame', color:color, pid:handle, gameType:gameGeo}, function(data) {
		gid = data.gid;
		console.log("new game: " + gid);
		enterLobby();
	});
	pid = handle;
}

var enterLobby = function() {
	$('#mainMenu').hide(); 
	$('#lobby').show();
	setInterval('statusGet()', 2000);
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
