# Train Game

This is a board game where you start with a map, build track
and move your train around the board picking up and delivering loads. For
instructions on how to play it, see GameInstructions. This document explains
how to set it up a server for the game.

## How to Run the Game

The game has a server component in Java, and a client
component in JavaScript. To use it, you need an http server that can serve up
the JavaScript files, and you also need to run the Java code. Any http server
should work; for instance, the node http server at <a
href="https://github.com/nodeapps/http-server">https://github.com/nodeapps/http-server</a>.
To start it up, I cd to the root Train-Server-14 directory and run the
following command line: 

http-server -p 3000

To start the Java server, cd to trainserver/bin and do:

java -jar trains.jar

Once that’s done you can go to a browser window and bring up the
game by going to:

<yourserverip>:3000/client/

