package app.dto.messages;

public enum MessageType {
    // Client -> Server
    LOGIN_REQUEST,          // Client wants to log in
    MATCH_REQUEST,          // Client wants to play a random game
    CHALLENGE_REQUEST,      // Client wants to challenge specific player
    CHALLENGE_RESPONSE,     // Client's response to Accept/decline a challenge
    MOVE_REQUEST,           // Client wants to make a move
    CHAT_MESSAGE,           // Client wants to send a chat
    REMATCH_REQUEST,        // Client request a rematch after the game is over
    LEAVE_GAME_REQUEST,     // Client wants to leave the current game

    // Server -> Client
    LOGIN_RESPONSE,         // Provides the success or failure of trying to log in
    ERROR_RESPONSE,         // Error notification
    GAME_START,             // Notify that the game is starting
    GAME_UPDATE,            // Sends the updated board after a move has been made
    GAME_END,               // Notify that the Game has ended with the result
    CHAT_NOTIFICATION,      // Notify relevant players of a new chat message
    CHALLENGE_NOTIFICATION, // Notify player that they've been challenged
    REMATCH_NOTIFICATION,   // Notify player that their opponent want a rematch
    WAITING_FOR_PLAYERS,    // Notify client they are queued
    REMATCH_REQUEST_UPDATE, // Inform client that server got their request
    FORFEIT_NOTIFICATION,   // Inform client opponent left/forfeited
    SERVER_SHUTDOWN         // Notify players that the server is shutting down
}