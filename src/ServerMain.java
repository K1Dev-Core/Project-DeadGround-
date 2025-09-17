import server.GameServer;

public class ServerMain {
    public static void main(String[] args) {
        System.out.println("=== Project-DeadGround Server ===");
        System.out.println("Starting server...");

        GameServer server = new GameServer(8888);
        server.start();
    }
}
