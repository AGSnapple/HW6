package org.example;

import java.io.*;
import java.net.*;

public class ChatClient {
    private static final ConfigReader reader = ConfigReader.getInstance();

    public static void main(String[] args) {
        try (Socket socket = new Socket(reader.getHost(), reader.getPort());
             BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("Connected to the chat server");

            System.out.print(serverReader.readLine() + " ");
            String username = consoleReader.readLine();
            writer.println(username);

            new Thread(() -> {
                String serverMessage;
                try {
                    while ((serverMessage = serverReader.readLine()) != null) {
                        System.out.println(serverMessage);
                    }
                } catch (IOException ex) {
                    System.out.println("Server connection closed: " + ex.getMessage());
                }
            }).start();

            String userInput;
            do {
                userInput = consoleReader.readLine();
                writer.println(userInput);
            } while (!userInput.equalsIgnoreCase("/exit"));

            System.out.println("You have left the chat.");

        } catch (UnknownHostException ex) {
            System.out.println("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("I/O error: " + ex.getMessage());
        }
    }
}

