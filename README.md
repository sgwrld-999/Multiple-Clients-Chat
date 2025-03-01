# Java Socket Chat Application

A real-time multi-client chat application implementing socket programming and multithreading in Java. This application allows multiple users to connect to a central server and exchange messages in a group chat environment.

![Chat Application Diagram](https://via.placeholder.com/800x400?text=Chat+Application+Architecture)

## 📋 Features

- **Multi-Client Support**: Concurrent handling of multiple client connections
- **Real-Time Messaging**: Instant message broadcasting to all connected clients
- **User Identification**: Unique username for each client
- **Connection Notifications**: Join and leave announcements
- **Thread-Based Concurrency**: Each client handled in a separate thread

## 🔧 System Requirements

- **Java Development Kit (JDK)**: Version 8 or higher
- **Operating Systems**: Compatible with Windows, macOS, and Linux
- **Network**: Local network or internet connection
- **Memory**: Minimal requirements (≈50MB RAM)

## 🚀 Getting Started

### Installation

1. Clone this repository or download the source code
   ```bash
   git clone https://github.com/yourusername/java-socket-chat.git
   cd java-socket-chat
   ```

### Running the Server

1. Compile the server components
   ```bash
   javac Server.java ClientHandler.java
   ```

2. Start the server
   ```bash
   java Server
   ```
   The server will start and listen for connections on port 1234.

### Running a Client

1. Compile the client
   ```bash
   javac Client.java
   ```

2. Launch the client application
   ```bash
   java Client
   ```

3. Enter your username when prompted and start chatting!

## 💬 Usage

### Basic Commands

- **Send Message**: Type your message and press Enter
- **Exit Chat**: Close the terminal/command prompt window

### Example Session

```
$ java Client
Enter your username: Alice
Connected to chat server!

Server: Bob has joined the chat
Bob: Hello everyone!
Alice: Hi Bob, welcome to the chat!
Server: Charlie has joined the chat
Charlie: Hey folks, what's up?
```

## 🔍 How It Works

### Architecture

The application follows a client-server architecture:

1. **Server**: The central component that accepts connections and manages clients
2. **ClientHandler**: Created for each client connection, handles message processing
3. **Client**: End-user application that connects to the server

### Data Flow

```
                  ┌─────────────┐
                  │   Server    │
                  │  (Port 1234)│
                  └──────┬──────┘
                         │
           ┌─────────────┼─────────────┐
           │             │             │
    ┌──────▼─────┐ ┌─────▼──────┐ ┌────▼───────┐
    │ClientHandler│ │ClientHandler│ │ClientHandler│
    │  (Thread 1) │ │  (Thread 2) │ │  (Thread 3) │
    └──────┬─────┘ └─────┬──────┘ └────┬───────┘
           │             │             │
    ┌──────▼─────┐ ┌─────▼──────┐ ┌────▼───────┐
    │   Client 1  │ │   Client 2  │ │   Client 3  │
    │    (Alice)  │ │     (Bob)   │ │  (Charlie)  │
    └────────────┘ └────────────┘ └────────────┘
```

## 🛠️ Technical Details

### Files and Components

- **Server.java**: Main server implementation, accepts client connections
- **ClientHandler.java**: Manages individual client communication
- **Client.java**: Client-side implementation for connecting to the server

### Threading Model

The application uses a thread-per-client model:
- The server runs in the main thread and spawns a new thread for each client
- Each client has one thread for receiving messages and uses the main thread for sending

## 🐞 Troubleshooting

### Common Issues

1. **"Connection refused" error**
   - Ensure the server is running before starting clients
   - Check if port 1234 is already in use by another application

2. **Multiple users with the same name**
   - Current implementation allows duplicate usernames
   - Choose a unique username to avoid confusion

3. **Server not terminating properly**
   - Use Ctrl+C to force terminate the server process
   - You may need to manually kill Java processes if they persist

## 🔒 Limitations and Security Notes

- Messages are transmitted as plain text (no encryption)
- No authentication mechanism implemented
- No message persistence (messages are lost when the server restarts)
- Limited to localhost by default (modify source to enable network-wide chat)

## 🔮 Future Development

- Implement message encryption for secure communication
- Add user authentication and registration
- Create a GUI client for better user experience
- Implement private messaging between users
- Add file sharing capabilities

## 📜 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 👥 Contributing

Contributions are welcome! Feel free to fork this project and submit pull requests.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request
