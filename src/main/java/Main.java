public class Main {
    public static void main(String[] args) throws Exception {
        final int port = args.length == 0 ? 6379 : Integer.parseInt(args[1]);
        new RedisServer(port).start();
    }
}
