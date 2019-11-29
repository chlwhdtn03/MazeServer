package maze;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicScrollBarUI;

public class MazeUI extends JFrame {

	public JTextArea console = new JTextArea();
	public JScrollPane scroll = new JScrollPane(console);
	public JLabel leftLabel = new JLabel("00:00");
	public JButton startButton = new JButton("게임 시작");
	public JButton stopButton = new JButton("게임 정지");
	public static int timeleft = 90;
	public List<String> addresslist = new ArrayList<String>();
	int address_count = 0;

	public MazeUI() {
		setSize(400, 600);
		setLocationRelativeTo(null);
		setTitle("땅따먹기 서버패널");
		setLayout(new BorderLayout());
		setDefaultCloseOperation(EXIT_ON_CLOSE);

		JLabel titlelabel = new JLabel("땅따먹기 게임");
		titlelabel.setFont(new Font("맑은 고딕", Font.BOLD, 32));
		titlelabel.setHorizontalAlignment(SwingUtilities.CENTER);

		leftLabel.setFont(new Font("맑은 고딕", Font.BOLD, 32));
		leftLabel.setHorizontalAlignment(SwingUtilities.CENTER);

		startButton.setFont(new Font("맑은 고딕", Font.BOLD, 32));
		startButton.setContentAreaFilled(false);
		startButton.setFocusable(false);

		startButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				// 게임 시작을 클라이언트들에게 보내기
				if (!Maze.isStart) {
					
					// 이전판에 설치되었던 블럭 전부 삭제
					Iterator<Blockdata> iter = Maze.blocks.iterator();
					while(iter.hasNext()) {
						Blockdata block = iter.next();
						for(UserData user : Maze.clients) {
							user.ws.writeFinalTextFrame("block destory " + block.x + " " + block.y);
						}
					}
					
					// 플레이어들에게 남은시간 전송
					for(UserData user : Maze.clients) {
						user.ws.writeFinalTextFrame("timeleft " + timeleft);
					}
					
					Maze.isStart = true;
					Thread t = new Thread(new Runnable() {

						@Override
						public void run() {
							while (true) {
								try {
									if (timeleft <= 0) {
										leftLabel.setText(getTime(timeleft));
										Maze.isStart = false;
										Maze.gamend();
										timeleft = 90;
										return;
									}
									leftLabel.setText(getTime(--timeleft));
									Delay.sleep(1000);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						}
					});
					t.start();
				}
			}
		});

		JLabel connectDescription = new JLabel("이게 뭔 컴퓨터죠?");
		try {
			connectDescription = new JLabel("주소창에 '" + InetAddress.getLocalHost().getHostAddress() + "' 를 입력하세요");
			print("사용 가능한 IP를 확인합니다.");
			Enumeration<NetworkInterface> n = NetworkInterface.getNetworkInterfaces();
			
			NetworkInterface e;
			Enumeration<InetAddress> a;
			InetAddress addr;
			for (; n.hasMoreElements();) {
				e = n.nextElement();
				a = e.getInetAddresses();
				for (; a.hasMoreElements();) {
					addr = a.nextElement();
					print(e.getDisplayName() + " - " + addr.getHostAddress());		
					addresslist.add(addr.getHostAddress());
				}
			}
			
			connectDescription.setFont(new Font("맑은 고딕", Font.BOLD, 22));
		} catch (UnknownHostException | SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		connectDescription.setHorizontalAlignment(SwingUtilities.CENTER);
		
		connectDescription.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(address_count >= addresslist.size())
					address_count = 0;
				((JLabel) e.getSource()).setText("주소창에 '" + addresslist.get(address_count++) + "' 를 입력하세요");
			}
		});
		

		JPanel northPanel = new JPanel();
		northPanel.setLayout(new BorderLayout());
		northPanel.add(titlelabel, BorderLayout.NORTH);
		northPanel.add(connectDescription, BorderLayout.CENTER);
		northPanel.add(leftLabel, BorderLayout.SOUTH);

		scroll.setPreferredSize(new Dimension(200, 300));
		console.setEditable(false);
		console.setBackground(Color.blue);
		console.setForeground(Color.white);
		console.setWrapStyleWord(false);
		console.setLineWrap(true);
		console.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));

		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scroll.getVerticalScrollBar().setUI(new BasicScrollBarUI() {

			@Override
			protected JButton createDecreaseButton(int orientation) {
				return createZeroButton();
			}

			@Override
			protected JButton createIncreaseButton(int orientation) {
				return createZeroButton();
			}

			@Override
			protected void configureScrollBarColors() {
				this.thumbColor = new Color(0, 0, 0, 50);
				this.minimumThumbSize = new Dimension(0, 50);
				this.maximumThumbSize = new Dimension(0, 50);
				this.thumbDarkShadowColor = new Color(200, 200, 200);
			}

			private JButton createZeroButton() {
				JButton jbutton = new JButton();
				jbutton.setPreferredSize(new Dimension(0, 0));
				jbutton.setMinimumSize(new Dimension(0, 0));
				jbutton.setMaximumSize(new Dimension(0, 0));
				return jbutton;
			}

		});

		add(scroll, BorderLayout.SOUTH);
		add(northPanel, BorderLayout.NORTH);
		add(startButton, BorderLayout.CENTER);

		setVisible(true);

	}

	public void print(String msg) {
		console.append(msg + "\n");
		console.setCaretPosition(console.getDocument().getLength());
	}

	public static String getTime(int sec) {
		int minute = 0, second = sec;
		String str_min, str_sec;
		for (;;) {
			if (second >= 60) {
				second -= 60;
				minute++;
			} else {
				break;
			}
		}
		str_min = Integer.toString(minute);
		str_sec = second < 10 ? "0" + Integer.toString(second) : Integer.toString(second);
		return str_min + ":" + str_sec;
	}
}
