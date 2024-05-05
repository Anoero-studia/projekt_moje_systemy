/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package echoserver;

/**
 *
 * @author dzelazny
 */
import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;



public class EchoServer {

    private static void loadUserData(CopyOnWriteArrayList<ArrayList<String>> logins) {

        Path path = Paths.get("baza.txt");
        try {
            if (!Files.exists(path)) {
                Files.createFile(path);
            }

            List<String> lines = Files.readAllLines(path);
            for (String line : lines) {
                String[] data = line.split(",");
                ArrayList<String> userDetails = new ArrayList<>(Arrays.asList(data));
                logins.add(userDetails);
            }
        } catch (IOException e) {
            System.out.println("Error reading user data: " + e);
        }

    }





    public static void main(String args[]) {
        CopyOnWriteArrayList<ArrayList<String>> logins = new CopyOnWriteArrayList<>();
        loadUserData(logins); // Wczytaj dane użytkowników z pliku na początku działania serwera

        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(6699);
            System.out.println("Socket initialized...");
            System.out.println("Socket parameters: " + serverSocket);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Connection received...");
                System.out.println("Connection parameters: " + socket);
                new Thread(new EchoServerThread(socket, logins)).start();
            }
        } catch (IOException e) {
            System.out.println("Error setting up server socket: " + e);
            System.exit(-1);
        }
    }
}

