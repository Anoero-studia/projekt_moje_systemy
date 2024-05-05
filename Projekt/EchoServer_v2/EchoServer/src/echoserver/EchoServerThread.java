package echoserver;

import java.net.*;
import java.io.*;
import java.nio.Buffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class EchoServerThread implements Runnable {
    protected Socket socket;
    CopyOnWriteArrayList<ArrayList<String>> logins;
    private int userIndex = -1;  // Index of the logged in user



    public EchoServerThread(Socket clientSocket, CopyOnWriteArrayList<ArrayList<String>> logins) {
        this.socket = clientSocket;
        this.logins = logins;
    }

    public void run() {
        BufferedReader brinp = null;
        DataOutputStream out = null;
        String threadName = Thread.currentThread().getName();

        try {
            brinp = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            System.out.println(threadName + "| Error creating streams: " + e);
            return;
        }

        String line;
        while (true) {
            try {
                line = brinp.readLine();
                if (line == null) {
                    break;  // Client has disconnected
                }
                System.out.println(threadName + "| Line read: " + line);

                switch (line) {
                    case "login":
                        handleLogin(brinp, out, threadName);
                        break;
                    case "register":
                        handleRegistration(brinp, out, threadName);
                        break;
                    case "list":
                        listUsers(out);
                        break;
                    case "deposit":
                        handleDeposit(brinp, out);
                        break;
                    case "withdraw":
                        handleWithdrawal(brinp, out);
                        break;
                    case "transfer":
                        handleTransfer(brinp, out);
                    default:
                        // Echo back to the client
                        out.writeBytes(line + "\r\n");
                        System.out.println(threadName + "| Line echoed back: " + line);
                        break;
                }
            } catch (IOException e) {
                System.out.println(threadName + "| I/O error: " + e);
                break;  // Exit the loop on I/O error
            }
        }

        try {
            socket.close();
        } catch (IOException e) {
            System.out.println(threadName + "| Error closing socket: " + e);
        }
    }

    private void handleLogin(BufferedReader brinp, DataOutputStream out, String threadName) throws IOException {
        boolean loginPass = false;
        out.writeBytes("Login and password?\r\n");
        while (!loginPass) {
            String credentialsLine = brinp.readLine();
            if (credentialsLine == null) return;  // Handle disconnection
            String[] credentials = credentialsLine.split(" ");
            for (int i = 0; i < logins.size(); i++) {
                if (logins.get(i).get(0).equals(credentials[0]) && logins.get(i).get(1).equals(credentials[1])) {
                    userIndex = i;
                    loginPass = true;
                    out.writeBytes("Correct login and password. Your balance: " + logins.get(i).get(3) + "\r\n");
                    break;
                }
            }
            if (!loginPass) {
                out.writeBytes("Incorrect login or password. Try again.\r\n");
            }
        }
    }

    private void handleRegistration(BufferedReader brinp, DataOutputStream out, String threadName) throws IOException {
        out.writeBytes("Login, password, and nickname?\r\n");
        String line = brinp.readLine();
        if (line == null) return;  // Handle disconnection
        String[] parts = line.split(" ");
        for (ArrayList<String> userData : logins) {
            if (userData.get(0).equals(parts[0])) {  // Check for existing login
                out.writeBytes("Login already in use. Try another.\r\n");
                return;
            }
        }
        ArrayList<String> newUser = new ArrayList<>();
        newUser.add(parts[0]);  // Login
        newUser.add(parts[1]);  // Password
        newUser.add(parts[2]);  // Nickname
        newUser.add(parts[3]);    // Initial balance
        logins.add(newUser);
        saveUserData();  // Save after registering a new user
        out.writeBytes("User registered successfully.\r\n");
    }

    private void listUsers(DataOutputStream out) throws IOException {
        StringBuilder userList = new StringBuilder();
        for (ArrayList<String> userData : logins) {
            userList.append(userData.get(2)).append("\r\n");  // Append nicknames to the list
        }
        out.writeBytes(userList.toString());  // Send the complete list as a single message
        out.flush();  // Make sure all data is sent immediately
    }



    private void handleDeposit(BufferedReader brinp, DataOutputStream out) throws IOException {
        if (userIndex == -1) {
            out.writeBytes("Please log in first.\r\n");
            return;
        }
        out.writeBytes("Enter amount to deposit:\r\n");
        String amountStr = brinp.readLine();
        double amount = Double.parseDouble(amountStr);
        double currentBalance = Double.parseDouble(logins.get(userIndex).get(3));
        currentBalance += amount;
        logins.get(userIndex).set(3, String.format("%.2f", currentBalance));
        saveUserData();
        out.writeBytes("New balance: " + currentBalance + "\r\n");
    }

    private void handleTransfer(BufferedReader brinp, DataOutputStream out) throws IOException {
        if (userIndex == -1) {
            out.writeBytes("Please log in first.\r\n");
            return;
        }
        out.writeBytes("Enter recipient's login and amount to transfer: \r\n");
        String line = brinp.readLine();
        if (line == null) return; // Handle disconnection

        String[] transferDetails = line.split(" ");
        if (transferDetails.length < 2) {
            out.writeBytes("Invalid transfer details.\r\n");
            return;
        }

        String recipientLogin = transferDetails[0];
        double amount;
        try {
            amount = Double.parseDouble(transferDetails[1]);
        } catch (NumberFormatException e) {
            out.writeBytes("Invalid amount format. Please enter a valid number.\r\n");
            return;
        }

        int recipientIndex = -1;
        for (int i = 0; i < logins.size(); i++) {
            if (logins.get(i).get(0).equals(recipientLogin)) {
                recipientIndex = i;
                break;
            }
        }

        if (recipientIndex != -1 && amount <= Double.parseDouble(logins.get(userIndex).get(3))) {
            double senderBalance = Double.parseDouble(logins.get(userIndex).get(3)) - amount;
            double recipientBalance = Double.parseDouble(logins.get(recipientIndex).get(3)) + amount;
            logins.get(userIndex).set(3, String.format("%.2f", senderBalance));
            logins.get(recipientIndex).set(3, String.format("%.2f", recipientBalance));
            saveUserData();
            out.writeBytes("Transfer successful. New balance: " + senderBalance + "\r\n");
        } else {
            out.writeBytes("Transfer failed. Insufficient funds or recipient not found.\r\n");
        }
    }


    private void saveUserData() {
        Path path = Paths.get("baza.txt");
        List<String> lines = new ArrayList<>();
        for (ArrayList<String> userDetails : logins) {
            String line = String.join(",", userDetails);
            lines.add(line);
        }
        try {
            Files.write(path, lines);
        } catch (IOException e) {
            System.out.println("Error writing user data: " + e);
        }
    }
    private void handleWithdrawal(BufferedReader brinp, DataOutputStream out) throws IOException {
        if (userIndex == -1) {
            out.writeBytes("Please log in first.\r\n");
            return;
        }
        out.writeBytes("Enter amount to withdraw:\r\n");
        String amountStr = brinp.readLine();
        double amount = Double.parseDouble(amountStr);
        double currentBalance = Double.parseDouble(logins.get(userIndex).get(3));
        if (amount <= currentBalance) {
            currentBalance -= amount;
            logins.get(userIndex).set(3, String.format("%.2f", currentBalance));
            saveUserData();
            out.writeBytes("New balance: " + currentBalance + "\r\n");
        } else {
            out.writeBytes("Insufficient funds.\r\n");
        }


    }
}
