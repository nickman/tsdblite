var wsurl = "ws://localhost:4242/ws";
var sessionId = null;
var wsSub = new WebSocket(wsurl);
var rid = 0;
wsSub.onclose = function(evt) {
	if(evt.wasClean) console.info("WebSocket Closed")
	else console.error("WebSocket Closed: [%s]", evt.reason);
}
wsSub.onopen = function(evt) {
	console.info("WebSocket Connected")	
}
wsSub.onmessage = function(messageEvent) {
	console.group("WebSocket Message:")
	console.dir(messageEvent);
	console.info("Data: [%s]", messageEvent.data);	
	window.foo = messageEvent;
	console.groupEnd();
	if(sessionId==null) {
		var s = JSON.parse(messageEvent.data);		
		console.group("Parsed Data : [" + (typeof s) + "]");
		console.dir(s);
		console.groupEnd();
		sessionId = s['session'];
		console.info("Acquired Session ID: [%s]", sessionId);
	}	
}
wsSub.onerror = function(err) {
	console.group("WebSocket Error:")
	console.error(err);
	console.groupEnd();	
}

