package maze;

import io.vertx.core.http.ServerWebSocket;

public class UserData {
	
	public UserData(ServerWebSocket ws, String name, int x, int y, String team) {
		this.ws = ws;
		this.name = name;
		this.x = x;
		this.y = y;
		this.team = team;
	}
	public ServerWebSocket ws;
	public String name;
	public int x, y;
	public String team;

}
