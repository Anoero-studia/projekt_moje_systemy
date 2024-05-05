package client;

import java.net.*;
import java.io.*;

public class Client {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 6699;

        try (Socket clientSocket = new Socket(host, port);
             DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
             BufferedReader brSockInp = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             BufferedReader brLocalInp = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("Połączono z " + clientSocket);

            // Tworzymy wątek do odbierania odpowiedzi z serwera
            Thread listenerThread = new Thread(() -> {
                try {
                    String inputLine;
                    while ((inputLine = brSockInp.readLine()) != null) {
                        System.out.println("Otrzymano: " + inputLine);
                    }
                } catch (IOException e) {
                    System.out.println("Błąd wejścia-wyjścia podczas odbierania danych: " + e);
                }
            });
            listenerThread.start();

            // Obsługa konsoli użytkownika
            System.out.println("Możesz teraz wpisywać komendy.");
            String line;
            while ((line = brLocalInp.readLine()) != null) {
                if ("quit".equals(line)) {
                    System.out.println("Kończenie pracy...");
                    break;
                }
                out.writeBytes(line + "\r\n");  // wysyłamy komendy do serwera
                out.flush();
            }

        } catch (IOException e) {
            System.out.println("Błąd: " + e.getMessage());
            System.exit(-1);
        }
    }
}
