var paper;

$(document).ready(function(){
	//console.log('loaded');
	//$('body').append('<div id="button" style="height:100px;width=100px;background-color:#0000FF;"/>');
	paper = Raphael('display',$('body').width(),$('body').height());
	paper.circle(0,0,200);
	$('#button').click(function(){
		console.log('clicked');
		//post('hello world','text','http://www.posttestserver.com/post.php',function(data,status){console.log('data:',data);console.log('status: ',status);});
	});
});

var post = function(data,datatype,url,callback){
	$.ajax({
		type: "POST",
		url: url,
		data: data,
		success: callback,
		error: callback,
		dataType: datatype
	});
}
