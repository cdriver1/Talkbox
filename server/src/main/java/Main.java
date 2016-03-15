package talkbox.server;

public class Main {
	public static Server server;
	public static void main(String[] args) throws Exception {
		server = new Server(5476, null);
		server.run();
	}
}
