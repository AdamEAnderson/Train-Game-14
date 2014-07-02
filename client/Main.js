//Global Variables
var paper;
var server = 'http://www.posttestserver.com/post.php';
var pid, gid;

//Loaded
$(document).ready(function(){
	//Init display object
	//paper = Raphael('display',$('body').width(),$('body').height());
	//paper.circle(0,0,200);
	
	//Init main menu
	$('#mainMenu').append('<h3 id="joinGameText">Join Game</h3>');
	$('#mainMenu').append('<ul id="mainMenuJUI"/>');
	//for(var i = 0; i < 10; i++){
		//$('#mainMenuJUI').append('<li>'+i+'</li>');
	//}
	$('#mainMenuJUI').menu();
	$('#mainMenu').append('<h4 style="margin:10px 0px 5px 25px;">Handle</h4>');
	$('#mainMenu').append('<input id="handlePicker" type="text" size="32"style="width:100px;"/>');
	$('#mainMenu').append('<h4 style="margin:10px 0px 5px 10px;">Game Color</h4>');
	$('#mainMenu').append('<select id="colorPicker"><option>aqua</option><option>black</option><option>blue</option><option>fuchsia</option><option>gray</option><option>green</option><option>lime</option><option>maroon</option><option>navy</option><option>olive</option><option>orange</option><option>purple</option><option>red</option><option>silver</option><option>teal</option><option>yellow</option></select>');
	$('#colorPicker').selectmenu();
	$('#colorPicker').css('font-size','0.8em');
	$('#mainMenu').append('<br/>');
	$('#mainMenu').append('<button id="newGameButton">New Game</button>');
	$('#newGameButton').button().click(function(){
		if($('#handlePicker').value() && $('#handlePicker').value().length > 0){
			newGame(document.getElementById("colorPicker").value,$('#handlePicker').value());
		}
	});
	$('#lobby').append('<h3 id="lobbyText">Lobby</h3>');
	$('#lobby').append('<ul id="lobbyMenuJUI"/>');
	$('#lobbyMenuJUI').menu();
	$('#mainMenu').show();
	
	//for(var i = 0; i < 10; i++){
		//$('#mainMenuJUI').append('<li>'+i+'</li>');
	//}
	//$('#mainMenuJUI').menu('refresh');

	statusGet();
});

//Tells server we've joined a game
var joinGame = function(GID,color,handle) {
	post({messageType:'joinGame', game:GID, color:color, pid:handle}, function(data){gid = GID; $('#mainMenu').hide(); $('#lobby').show();});
	pid = handle;
};

//Tells server to start the game(from host)
var startGame = function() {
	post({messageType:'startGame', gid:gid, pid:pid},function(){});
};

var newGame = function(color, handle) {
	post({messageType:'newGame', color:color, pid:handle}, function(data) {gid = data.GID; $('#mainMenu').hide(); $('#lobby').show();});
	pid = handle;
}

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
	$.ajax({
		type:"GET",
		url:server,
		success: function(responseData) {
			processStatus(responseData);
		}
	});
};

//Processes a status response from the server
var processStatus = function(data) {
	if(data.phase == 'pregame') {
		$('#mainMenuJUI').clear();
		for(var i = 0; i < data.games.length; i++){
			$('#mainMenuJUI').append('<li id="' + data.games[i].gid + '" >' + data.games[i].name + '</li>').click(data.games[i].gid,function(e){
				if($('#handlePicker').value() && $('#handlePicker').value().length > 0){
					joinGame(e.eventData, document.getElementById("colorPicker").value,$('#handlePicker').value());
				}
			});
		}
		$('#mainMenuJUI').menu('refresh');
	}
	
	else if(data.phase == 'lobby') {
		$('#mainMenuJUI').clear();
		for(var i = 0; i < data.players.length; i++){
			$('#mainMenuJUI').append('<li id="' + data.players[i] + '" >' + data.players[i] + '</li>');
		}
		$('#mainMenuJUI').menu('refresh');
	}
	
	if(data.events) {
		for(var i = 0; i < data.events.length; i++) {
			if(data.events[i].type == 'startGame') {
				//Init game here
			}
		}
	}
};

//Post
var post = function(data,url,callback){
	$.ajax({
		type: "POST",
		url: server,
		data: data,
		success: callback,
		error: callback,
		dataType: 'json'
	});
};
