package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static final ConfigReader configReader = ConfigReader.getInstance();

    static Set<ClientHandler> clientHandlers = new HashSet<>();

    private static final Logger logger = LoggerFactory.getLogger(ChatServer.class);

    public static void main(String[] args) {
        logger.info("Initializing the server on the port: {}", configReader.getPort());

        try (ServerSocket serverSocket = new ServerSocket(configReader.getPort())) {
            logger.info("The server is running successfully.");
            logger.info("Waiting for new clients.");

            while (true) {
                Socket socket = serverSocket.accept();
                logger.info("New client connected.");
                ClientHandler clientHandler = new ClientHandler(socket);
                clientHandlers.add(clientHandler);
                clientHandler.start();
            }
        } catch (IOException ex) {
            logger.error("Unable to initialize the server. Error stack: {}", ex.getMessage());
        }
    }

    static void broadcast(String message, ClientHandler excludeUser) {
        for (ClientHandler clientHandler : clientHandlers) {
            if (clientHandler != excludeUser) {
                clientHandler.sendMessage(message);
            }
        }
    }

    //Поиск клиента по username
    static ClientHandler clientSearch(String username) {
        for (ClientHandler client : clientHandlers) {
            if(client.getUsername().equals(username)) {
                return client;
            }
        }
        //Если клиента с таким именем нет - возвращаем null
        return null;
    }

    static void sendMessageToUser(String message, ClientHandler recipient) {
        recipient.sendMessage(message);
    }

    static Set<ClientHandler> getClientsList() {
        return clientHandlers;
    }
}

class ClientHandler extends Thread {
    private final Socket socket;
    private PrintWriter writer;
    private String username;

    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try (InputStream input = socket.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(input));
             OutputStream output = socket.getOutputStream();
             PrintWriter writer = new PrintWriter(output, true)) {

            logger.info("New client added successfully. Sending identification request.");

            this.writer = writer;
            writer.println("Enter your username:");
            this.username = reader.readLine();
            logger.info("Client connected with username {}", username);

            ChatServer.broadcast(username + " connected to the chat.", this);

            String text;
            do {
                text = reader.readLine();

                if(text.equalsIgnoreCase("/users")) {
                    StringBuilder clientUserNames = new StringBuilder("Users in chat: ");

                    for (ClientHandler client : ChatServer.getClientsList()) {
                        clientUserNames.append(client.getUsername()).append(" ");
                    }
                    writer.println(clientUserNames);
                    logger.info("The user requested a list of users.");

                    continue;
                }

                if (text.startsWith("@")) {
                    String[] substrings = text.split(" ", 2);

                    if (substrings.length != 2) {
                        writer.println("Message contains no text.");
                        continue;
                    }

                    String recipientName = substrings[0].substring(1);
                    String message = substrings[1];

                    ClientHandler recipient = ChatServer.clientSearch(recipientName);

                    if(recipient == null) {
                        writer.println("User " + recipientName + " not found.");
                    }
                    else {
                        if (message.equals(""))
                            logger.warn("Null message received.");

                        logger.info("Message from {} to {}", username, recipientName);

                        ChatServer.sendMessageToUser(message, recipient);
                    }
                } else {
                    logger.info("Broadcast message {} from {}", text, username);

                    if (text.equals(""))
                        logger.warn("Null message received.");

                    ChatServer.broadcast(username + ": " + text, this);
                }
            } while (!text.equalsIgnoreCase("/exit"));

            socket.close();
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
        } finally {
            ChatServer.clientHandlers.remove(this);
            logger.info("Client {} disconnected", username);
        }
    }

    void sendMessage(String message) {
        if (writer != null) {
            writer.println(message);
        }
    }

    String getUsername() {
        return username;
    }
}
