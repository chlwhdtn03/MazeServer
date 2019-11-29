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

	private static MazeUI getUI;
	boolean teamb = false;
	public static boolean isStart = false;

	public static void main(String[] args) {
		getUI = new MazeUI();
		new Maze();
	}

	@SuppressWarnings("unused")
	private HttpServer server;
	public static final List<UserData> clients = new ArrayList<>();
	public static final List<Blockdata> blocks = new ArrayList<>();
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
				req.response().setStatusCode(404).end();
			}
		}).websocketHandler(ws -> {
			ws.closeHandler(v -> {
				if (iscontains(ws)) {
					UserData user = getUserData(ws);
					System.out.println(user.name + "(" + ws.remoteAddress() + ")님의 연결이 해제되었습니다");
					getUI.print(user.name + "(" + ws.remoteAddress() + ")님의 연결이 해제되었습니다");
					clients.remove(user);
					clients.forEach((c) -> {
						c.ws.writeFinalTextFrame("leave " + user.name);
					});
				}
			});

			ws.frameHandler(frame -> {
				String[] cmd = frame.textData().split(" ");
				if (cmd[0].equals("join")) {

					String t;
					if (teamb)
						t = "blue";
					else
						t = "red";
					teamb = !teamb;
					
					System.out.println(cmd[3] + "(" + ws.remoteAddress() + ")님이 접속했습니다.");
					getUI.print(cmd[3] + "(" + ws.remoteAddress() + ")님이 접속했습니다.");
					clients.forEach((u) -> {
						u.ws.writeFinalTextFrame("join " + cmd[3] + " " + cmd[1] + " " + cmd[2] + " " + t);
						ws.writeFinalTextFrame("join " + u.name + " " + u.x + " " + u.y + " " + u.team);
					});
					blocks.forEach((block) -> {
						ws.writeFinalTextFrame("block place " + block.x + " " + block.y + " " + block.team);
					});
					clients.add(new UserData(ws, cmd[3], Integer.parseInt(cmd[1]), Integer.parseInt(cmd[2]), t));
					ws.writeFinalTextFrame("yourteam " + t);
					if(isStart) 
						ws.writeFinalTextFrame("timeleft " + MazeUI.timeleft);

				}
				if (cmd[0].equals("chat")) {
					UserData user = getUserData(ws);

					String msg = "";
					for (int i = 1; i < cmd.length; i++) {
						msg += " " + cmd[i];
					}
					if(cmd[1].equalsIgnoreCase("/setname")) {
						String origin = user.name;
						if(setName(user, cmd[2])) {
							clients.forEach((u) -> {
								u.ws.writeFinalTextFrame("setname done " + origin + " " + cmd[2]);
							});
							// 변경 성공
						} else {
							clients.forEach((u) -> {
								u.ws.writeFinalTextFrame("setname fail " + origin);
							});
							// 변경 실패
						}
						return;
					}
					
					
					getUI.print(user.name + "(" + ws.remoteAddress() + ") - " + msg.toString());
					for (UserData u : clients) {
						u.ws.writeFinalTextFrame("chat " + user.name + " " + msg.toString());
					}
				}
				if (cmd[0].equals("move")) {
					UserData user = getUserData(ws);
					user.x = Integer.parseInt(cmd[1]);
					user.y = Integer.parseInt(cmd[2]);
					clients.forEach((u) -> {
						u.ws.writeFinalTextFrame("move " + user.name + " " + user.x + " " + user.y);
					});
				}
				if (isStart) {
					if (cmd[0].equals("block")) {
						if (cmd[1].equals("place")) {
							UserData user = getUserData(ws);
							int[] pos = { Integer.parseInt(cmd[2]), Integer.parseInt(cmd[3]) };
							if (isBlockInList(pos) == true)
								blocks.remove(getBlockInList(pos));
							blocks.add(new Blockdata(pos[0], pos[1], user.team));
							clients.forEach((u) -> {
								u.ws.writeFinalTextFrame("block place " + cmd[2] + " " + cmd[3] + " " + user.team);
							});
						} else if (cmd[1].equals("destory")) {
							int[] pos = { Integer.parseInt(cmd[2]), Integer.parseInt(cmd[3]) };

							if (isBlockInList(pos)) {
								blocks.remove(getBlockInList(pos));
								clients.forEach((u) -> {
									u.ws.writeFinalTextFrame("block destory " + cmd[2] + " " + cmd[3]);
								});
							}
						}
					}
				}
			});
		}).listen(port, result -> {
			if (result.succeeded()) {
				System.out.println(port + " 포트로 개방 성공");
				getUI.print(port + " 포트로 게임 페이지를 개방했습니다.");
			} else {
				System.out.println(port + " 포트로 개방 실패");
				getUI.print(port + " 포트로 게임 페이지를 개방하지 못했습니다.");
			}
		});
	}

	public static void gamend() {
		clients.forEach(u -> {
			u.ws.writeFinalTextFrame("end");
		});
	}

	public Blockdata getBlockInList(final int[] candidate) {

		for (Blockdata block : blocks) {
			if (candidate[0] != block.x)
				continue;
			if (candidate[1] != block.y)
				continue;
			return block;
		}

		return null;
	}

	public boolean isBlockInList(final int[] candidate) {

		for (Blockdata block : blocks) {
			if (candidate[0] != block.x)
				continue;
			if (candidate[1] != block.y)
				continue;
			return true;
		}

		return false;
	}
	
	public boolean setName(UserData user, String changename) {
		if(changename.length() > 5)
			return false;
		for(UserData users : clients) {
			if(users.name.equals(changename))
				return false; // 겹치면 불가능
		}
		getUI.print(user.name + "의 닉네임을 " + changename + " 으로 변경했습니다.");
		user.name = changename;
		return true; // 안겹치면 바꿔쥼
	}

	public UserData getUserData(ServerWebSocket ws) {
		for (int i = 0; i < clients.size(); i++) {
			if (clients.get(i).ws.equals(ws)) {
				return clients.get(i);
			}
		}
		return null;
	}

	public boolean iscontains(ServerWebSocket ws) {
		for (int i = 0; i < clients.size(); i++) {
			if (clients.get(i).ws.equals(ws)) {
				return true;
			}
		}
		return false;
	}

}
