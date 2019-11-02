package maze;

import io.vertx.core.http.ServerWebSocket;

public class UserData {
	
	public UserData(ServerWebSocket ws, String name, int x, int y) {
		this.ws = ws;
		this.name = name;
		this.x = x;
		this.y = y;
	}
	public ServerWebSocket ws;
	public String name;
	public int x, y;

}
