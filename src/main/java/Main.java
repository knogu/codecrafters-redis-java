public class Main {
    public static void main(String[] args) throws Exception {
        String masterHostname = "";
        Integer masterPort = -1;
        int port = 6379;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("--replicaof") && i + 1 < args.length) {
                String[] masterServerAddr = args[i + 1].split(" ");
                assert masterServerAddr.length == 2;
                masterHostname = masterServerAddr[0];
                masterPort = Integer.valueOf(masterServerAddr[1]);
                i++;
            }
            if (args[i].equalsIgnoreCase("--port") && i + 1 < args.length) {
                port = Integer.parseInt(args[i + 1]);
                i++;
            }
        }
        new RedisServer(port, masterHostname, masterPort).start();
    }
}
