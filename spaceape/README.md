Noughts Coding Test
===================

My solution to [Space Ape](http://spaceapegames.com/)'s backend developer technical test.

---

The task is to implement the server side of a multiplayer
[noughts and crosses or tic-tac-toe](http://en.wikipedia.org/wiki/Tic-tac-toe) game.  We've provided a skeleton
Dropwizard app that defines the API but lacks an implementation.  You will need to provide an implementation.
Please complete at least one of the extra tasks.  Leave anything you'd like us to know about your submission (setup
instructions, technical justifications, future improvements etc.) in the Candidate Comments at the bottom of this file.

**Please be aware**: we will verify the correctness of your submission using a suite of automated tests. Deviation from the API
described here will not be viewed favourably. If you have considerations about the structure of the API or how to improve
it then please leave them in the Candidate Comments section.

Rules
-----

Two players take turns to place a mark in one of the spaces on a 3x3 grid.  Each player cannot place a mark where either
player has placed one previously.  The first player who places three consecutive marks in a horizontal, vertical or
diagonal row wins the game.  If all of the spaces are taken and no player has succeeded in placing 3 marks in a row
then the game is a draw.

API
---

Please ensure that your application meets this API.  The NoughtsResource and NoughtsTest classes provided should make it
clear how to achieve this.  Please return appropriate http error codes to enforce both the rules described above and
the restrictions described below.

### Create a game ###

    method                : POST
    url                   : /game?player1Id=<id of player 1>&player2Id=<id of player 2>
    example response body : {"gameId": "<id of the game>"}

The client will provide the ids of the players. Players can create games against multiple different players concurrently
but an appropriate error code should be returned if a player tries to create a  new game against a player that they
currently have an unfinished game against.  The response should be a json object containing a single field, the id that
identifies the new game.

### Make a move ###

    method                : PUT
    url                   : /game/<id of the game>
    example request body  : {"playerId": "<id of player making the move>", "x": <column index to make a mark in>, , "y": <row index to make a mark in>}
    example response body : <empty>

The \<id of the game> will be the id of a game previously created via a call to the *Create a game* endpoint.  The player id
will be the id of the player making the move.  An error code should be returned if a player makes a move out of turn.
Assume that player 1 will always go first.

### Get the game state ###

    method                                   : GET
    url                                      : /game/<id of the game>
    example response body (game in progress) : {"winnerId": null, "gameOver": false}
    example response body (win)              : {"winnerId": "<id of the winning player>", "gameOver": true}
    example response body (draw)             : {"winnerId": null, "gameOver": true}

The \<id of the game> will be the id of a game previously created via a call the *Create a game* endpoint.  If the game
is still in progress the winnerId should be null and gameOver should be false.  If a player has won then then the
winnerId should be the the id of that player and gameOver should be true. If the game is complete and a draw then the
winnerId should be null and gameOver should be true.

### Concurrency handling ###

Ensure that your application runs efficiently and without error despite serving multiple players playing concurrently.
Assume that players will try and break the game by making concurrent requests against the same player.

For bonus points:

### Extend the tests ###

Extend the NoughtsTest class to cover more of the rules and restrictions described above.


Extra Tasks
-----------

### Persistence ###

Store the state of the games in an external data store of your choice.  Aim to handle 1000s of concurrent games on a
modern mid-range laptop.

### Leaderboard ###

Add a leaderboard to the game, allowing clients to get the top 10 player ids and scores. Players should get 1 point per
game won and no points for draws.

Candidate Comments
------------------

### Running and testing ###

I've added a couple of quick scripts which help with running and testing the application with
its database. To run the application you'll need docker and docker-compose. If I were to enhance
the suite further I'd ensure it brought up a disposable couchdb instance itself and then threw it
away, but doing so in the scope of this test seemed overkill.

Run the db with `docker-compose up -d` (served on `localhost:5984`)
If you need to drop the db, run `drop-db.sh` to throw out the container and recreate it

There's also a `quick-restart.sh` script to quickly package and run the jar, but I mostly wrote
that one for my own ease of developing.

I've added some extra integration tests to cover more of the scenarios described, and two suites of fairly
comprehensive unit tests for the game engine logic (making moves, deciding if the game's over)
