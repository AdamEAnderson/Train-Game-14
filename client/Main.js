//Global Variables
var paper;
var server = 'http://www.posttestserver.com/post.php';
var pid, gid;

//Loaded
$(document).ready(function(){
	//Init display object
	paper = Raphael('display',$('body').width(),$('body').height());
	paper.circle(0,0,200);
	
	//Handshake with server
	post({messageType:'handshake'}, function(data) {
		pid = data.pid;
		
		//Start status update feed
		statusGet();
	});
});

//Tells server we've joined a game
var joinGame = function(GID,color,handle) {
	post({messageType:'joinGame', game:GID, pid:pid, color:color, handle:handle}, function(data){gid = data.gid;});
};

//Tells server to start the game(from host)
var startGame = function() {
	post({messageType:'startGame', gid:gid, pid:pid}, function(data) {
		//Init game here
	});
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
	post({messageType:'endTurn',pid:pid,gid:gid},processStatus
};

//Tells server our game is done(from host)
var endGame = function() {
	post({
}

//Gets a status response from the server
var statusGet= function() {
	setTimeout(200,statusGet);
	
	//Request status from server
	requestData = gid ? {messageType:'statusUpdate', pid:pid, gid:gid} : {messageType:'statusUpdate', pid:pid};
	post(requestData, function(responseData) {
		processStatus(responseData);
	});
};

//Processes a status response from the server
var processStatus = function(data) {
	//Process status here
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
