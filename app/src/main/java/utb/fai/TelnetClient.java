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

            // Поток приёма данных от сервера
            Thread receiverThread = new Thread(() -> {
                try {
                    InputStream in = socket.getInputStream();
                    byte[] buffer = new byte[1024];

                    while (running && !socket.isClosed()) {
                        // Неблокирующее чтение: проверяем наличие доступных байт
                        if (in.available() > 0) {
                            int len = in.read(buffer);
                            if (len == -1) {
                                break;
                            }
                            System.out.write(buffer, 0, len);
                            System.out.flush();
                        } else {
                            // Снижение нагрузки на процессор при активном ожидании
                            try {
                                Thread.sleep(20);
                            } catch (InterruptedException e) {
                                break;
                            }
                        }
                    }
                } catch (IOException e) {
                    // Сокет был закрыт при выходе
                } finally {
                    running = false;
                }
            });

            // Поток отправки данных на сервер
            Thread senderThread = new Thread(() -> {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                    OutputStream out = socket.getOutputStream();
                    String line;

                    while (running && (line = reader.readLine()) != null) {
                        // Проверка команды завершения работы
                        if ("/QUIT".equalsIgnoreCase(line.trim())) {
                            break;
                        }
                        // Отправка введённого текста с переводом строки
                        out.write((line + "\r\n").getBytes());
                        out.flush();
                    }
                } catch (IOException e) {
                    // Ошибка при отправке
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

            // Ожидаем завершения потока отправки (по команде /QUIT или завершению ввода)
            try {
                senderThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Гарантируем закрытие сокета и завершение потока приёма
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

