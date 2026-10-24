package utb.fai;

public class App {

    public static void main(String[] args) {
        String ip = "127.0.0.1";
        int port = 23;

        // Parse IP address and port from command line arguments
        if (args.length > 0) {
            ip = args[0];
        }
        if (args.length > 1) {
            try {
                port = Integer.parseInt(args[1]);
                if (port < 1 || port > 65535) {
                    System.err.println("Chyba: Číslo portu musí být v rozsahu 1-65535.");
                    return;
                }
            } catch (NumberFormatException e) {
                System.err.println("Chyba: Neplatný formát portu: " + args[1]);
                return;
            }
        }

        // Run telnet client
        TelnetClient telnetClient = new TelnetClient(ip, port);
        telnetClient.run();
    }
}

