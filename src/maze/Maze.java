package maze;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.ServerWebSocket;

public class Maze {
	
	public static void main(String[] args) {
		new Maze();
	}
	
	@SuppressWarnings("unused")
	private HttpServer server;
	public final List<UserData> clients = new ArrayList<>();
	public final List<int[]> blocks = new ArrayList<>();
	int port = 80;
	public Maze() {
		server = Vertx.vertx().createHttpServer(new HttpServerOptions().setPort(port)).requestHandler(req -> {

			try (InputStream in = getClass()
					.getResourceAsStream("/web" + (req.path().equals("/") ? "/index.html" : req.path()))) {
				byte[] data = new byte[1024];
				int size;
				Buffer buffer = Buffer.factory.buffer();
				while ((size = in.read(data)) != -1) {
					buffer.appendBytes(data, 0, size);
				}
				req.response().end(buffer);
			} catch (Exception e) {
				e.printStackTrace();
				req.response().setStatusCode(404).end();
			}
		}).websocketHandler(ws -> {
			ws.closeHandler(v -> {
				if(iscontains(ws)) {
					UserData user = getUserData(ws);
					System.out.println(user.name + "(" + ws.remoteAddress() + ")님의 연결이 해제되었습니다");
					clients.remove(user);
					clients.forEach((c) -> {
						c.ws.writeFinalTextFrame("leave " + user.name);
					});
				}
			});
			

			ws.frameHandler(frame -> {
				String[] cmd = frame.textData().split(" ");
				if(cmd[0].equals("join")) {
					System.out.println(cmd[3] + "(" + ws.remoteAddress() + ")님이 접속했습니다.");
					clients.forEach((u) -> {
						u.ws.writeFinalTextFrame("join " + cmd[3] + " " + cmd[1] + " " + cmd[2]);
						ws.writeFinalTextFrame("join " + u.name + " " + u.x + " " + u.y);
					});
					blocks.forEach((block) -> {
						ws.writeFinalTextFrame("block place " + block[0] + " " + block[1]);
					});
					clients.add(new UserData(ws, cmd[3], Integer.parseInt(cmd[1]), Integer.parseInt(cmd[2])));
					
				}
				if(cmd[0].equals("move")) { 
					UserData user = getUserData(ws);
					user.x = Integer.parseInt(cmd[1]);
					user.y = Integer.parseInt(cmd[2]);
					clients.forEach((u) -> {
						u.ws.writeFinalTextFrame("move " + user.name + " " + user.x + " " + user.y);
					});
				}
				if(cmd[0].equals("block")) { 
					if(cmd[1].equals("place")) {
						int[] pos = {
								Integer.parseInt(cmd[2]), Integer.parseInt(cmd[3])
						};
						if(isBlockInList(blocks, pos) == false) {
							blocks.add(pos);
							clients.forEach((u) -> {
								u.ws.writeFinalTextFrame("block place " + cmd[2] + " " + cmd[3]);
							});
						}
					} else if(cmd[1].equals("destory")) {
						int[] pos = {
								Integer.parseInt(cmd[2]), Integer.parseInt(cmd[3])
						};
						
						if(isBlockInList(blocks, pos)) {
							blocks.removeIf(a -> Arrays.equals(a, pos));
							clients.forEach((u) -> {
								u.ws.writeFinalTextFrame("block destory " + cmd[2] + " " + cmd[3]);
							});
						}
					}
				}
			});
		}).listen(port, result -> {
			if (result.succeeded()) {
				System.out.println(port + " 포트로 개방 성공");
			} else {
				System.out.println(port + " 포트로 개방 실패");
			}
		});
	}
	
	public static boolean isBlockInList(
	        final List<int[]> list, final int[] candidate) {

	    return list.stream().anyMatch(a -> Arrays.equals(a, candidate));
	}
	
	public UserData getUserData(ServerWebSocket ws) {
		for(int i = 0; i < clients.size(); i++) {
			if(clients.get(i).ws.equals(ws)) {
				return clients.get(i);
			}
		}
		return null;
	}
	
	public boolean iscontains(ServerWebSocket ws) {
		for(int i = 0; i < clients.size(); i++) {
			if(clients.get(i).ws.equals(ws)) {
				return true;
			}
		}
		return false;
	}
}
