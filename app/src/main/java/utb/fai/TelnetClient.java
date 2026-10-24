package utb.fai;

import java.io.*;
import java.net.*;

public class TelnetClient {

    private String serverIp;
    private int port;
    private volatile boolean running = true;

    public TelnetClient(String serverIp, int port) {
        this.serverIp = serverIp;
        this.port = port;
    }

    public void run() {
        try {
            Socket socket = new Socket(serverIp, port);

            // Thread for receiving data from the server
            Thread receiverThread = new Thread(() -> {
                try {
                    InputStream in = socket.getInputStream();
                    byte[] buffer = new byte[1024];

                    while (running && !socket.isClosed()) {
                        // Non-blocking read: check available bytes before reading
                        if (in.available() > 0) {
                            int len = in.read(buffer);
                            if (len == -1) {
                                break;
                            }
                            System.out.write(buffer, 0, len);
                            System.out.flush();
                        } else {
                            // Reduce CPU load during active waiting
                            try {
                                Thread.sleep(20);
                            } catch (InterruptedException e) {
                                break;
                            }
                        }
                    }
                } catch (IOException e) {
                    // Socket closed on shutdown
                } finally {
                    running = false;
                }
            });

            // Thread for sending user input to the server
            Thread senderThread = new Thread(() -> {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                    OutputStream out = socket.getOutputStream();
                    String line;

                    while (running && (line = reader.readLine()) != null) {
                        // Check for quit command
                        if ("/QUIT".equalsIgnoreCase(line.trim())) {
                            break;
                        }
                        // Send entered text with CRLF line ending
                        out.write((line + "\r\n").getBytes());
                        out.flush();
                    }
                } catch (IOException e) {
                    // Error while sending data
                } finally {
                    running = false;
                    try {
                        if (!socket.isClosed()) {
                            socket.close();
                        }
                    } catch (IOException ignored) {
                    }
                }
            });

            receiverThread.start();
            senderThread.start();

            // Wait for sender thread to finish (e.g. on /QUIT or EOF)
            try {
                senderThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Ensure socket is closed and stop receiver thread
            try {
                if (!socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException ignored) {
            }

            receiverThread.interrupt();
            try {
                receiverThread.join(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

        } catch (UnknownHostException e) {
            System.err.println("Neznámý hostitel: " + serverIp);
        } catch (IOException e) {
            System.err.println("Chyba spojení: " + e.getMessage());
        }
    }
}

