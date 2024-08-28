# Java Socket Programming - Multiple Clients Chat

This project implements a group chat application using Java sockets, enabling communication between multiple clients and a server. It demonstrates the implementation of socket programming and multithreading in Java.

## Features

- Server-client architecture
- Support for multiple clients
- Real-time messaging
- Username identification
- Join and leave notifications

## Components

1. **Server**: Manages client connections and message broadcasting
2. **ClientHandler**: Handles individual client connections
3. **Client**: Connects to the server and sends/receives messages

## Prerequisites

- Java Development Kit (JDK) 8 or higher
- Basic understanding of Java programming
- Familiarity with socket programming and multithreading concepts

## Getting Started

### Running the Server

1. Compile the server:
   ```
   javac Server.java ClientHandler.java
   ```
2. Run the server:
   ```
   java Server
   ```
   The server will start and listen for connections on port 1234.

### Running a Client

1. Compile the client:
   ```
   javac Client.java
   ```
2. Run the client:
   ```
   java Client
   ```
3. Enter your username when prompted.
4. Start chatting!

## Usage

- Once connected, type your message and press Enter to send it to all other connected clients.
- To exit the chat, you can close the client application or use a designated exit command (e.g., type "exit" and press Enter).

## Project Structure

- `Server.java`: Contains the main server logic
- `ClientHandler.java`: Manages individual client connections
- `Client.java`: Implements the client-side application

## How It Works

1. The server starts and listens for incoming connections.
2. When a client connects, the server creates a new `ClientHandler` instance to manage that connection.
3. Each `ClientHandler` runs in its own thread, allowing multiple clients to be handled concurrently.
4. Clients can send messages, which are then broadcast to all other connected clients.
5. When a client disconnects, the server removes it from the list of active clients.

