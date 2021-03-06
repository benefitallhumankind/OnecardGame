package onecard;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class Server extends JFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1105760893450832691L;

	private static final int PLAYER_NUM = 3; // 게임 참여 인원
	private static final int INIT_CARD_NUM = 7; // 시작시 받을 카드 수
	private static int turn = 0;
	private static int currPlayer = 0;
	private List<Card> deck; // 나눠줄 카드들
	private List<Card> usedCards; // 낸 카드들
	private List<User> playerList = null;
	private Card topCard = null;
	private int penaltyNum = 0;
	private int turnOpt = 1;
	private HeartBeat heartBeat;
	private ServerSocket hServerSocket = null;
	private ServerSocket serverSocket = null;
	private User roomMaster = null;

	private JPanel contentPane;
	private JTextArea textArea;
	private User emptyUser = new User(null, 0, "대기중...", null, null);
	private JTextField playerNumTxt;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Server frame = new Server();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	private List<User> createUserList() {
		List<User> pl = new ArrayList<>();
		for (int i = 0; i < PLAYER_NUM; i++) {
			pl.add(emptyUser);
		}
		return pl;
	}

	/**
	 * Create the frame.
	 */
	public Server() {

		playerList = createUserList();
		usedCards = new ArrayList<>();

		setTitle("SERVER");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setResizable(false);
		setBounds(100, 100, 420, 460);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		JPanel panel = new JPanel();
		panel.setBounds(12, 52, 269, 359);
		contentPane.add(panel);
		panel.setLayout(null);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(0, 0, 269, 359);
		panel.add(scrollPane);

		textArea = new JTextArea();
		scrollPane.setViewportView(textArea);

		JButton btnOnOff = new JButton("ON");
		btnOnOff.setBounds(295, 10, 97, 35);
		contentPane.add(btnOnOff);

		JLabel playerNumLabel = new JLabel("플레이어 수 :");
		playerNumLabel.setBounds(12, 20, 81, 15);
		contentPane.add(playerNumLabel);

		playerNumTxt = new JTextField();
		playerNumTxt.setText("3");
		playerNumTxt.setBounds(90, 17, 20, 21);
		contentPane.add(playerNumTxt);
		playerNumTxt.setColumns(10);
		btnOnOff.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				textArea.setText("서버 대기중 ...\n");
				btnOnOff.setText("OFF");
				Thread serverThread = new ServerThread();
				serverThread.start();

			}
		});

		setAlwaysOnTop(true);
		setVisible(true);

	}

	public int emptySpaceIdx() {
		int idx = -1;
		for (User u : playerList) {
			if (u.getSocket() == null) {
				idx = playerList.indexOf(u);
				break;
			}
		}
		return idx;
	}

	private boolean hasCard(User u) {
		if (u.getCardNum() == 0) {
			addLog("[" + u.getName() + "]님이 승리하였습니다 !!");
			return false;
		} else {
			addLog("[" + u.getName() + "]님의 남은 카드 수 : " + u.getCardNum());
		}
		return true;
	}

	// 모든 플레이어에게 code와 해당 content를 보내는 메서드
	private void sendSth(int code, String type, Object content) {
		for (User u : playerList) {
			ObjectOutputStream oos = u.getOos();
			try {
				oos.writeInt(code);
				switch (type) {
				case "String":
					oos.writeUTF((String) content);
					break;
				case "int":
					oos.writeInt((int) content);
					break;
				case "Object":
					oos.writeObject(content);
					break;
				case "boolean":
					oos.writeBoolean((boolean) content);
					break;
				default:
					break;
				}
				oos.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void sendSth(int code) {
		for (User u : playerList) {
			ObjectOutputStream oos = u.getOos();
			try {
				oos.writeInt(code);
				oos.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void getEnoughDeck(int num) {
		if (deck.size() < num) {
			announce("덱에 카드가 없으므로 사용한 카드를 섞어 보충합니다.");
			shuffle();
			List<Card> temp = new ArrayList<Card>();
			temp.addAll(usedCards);
			temp.remove(topCard);
			deck.addAll(temp);
			usedCards.clear();
			usedCards.add(topCard);
			for (User u : playerList) {
				ObjectOutputStream oos = u.getOos();
				try {
					oos.writeInt(106);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void sendPenalty() {
		sendSth(108, "int", penaltyNum);
	}

	class ServerThread extends Thread {

		ObjectOutputStream oos2;

		public void run() {
			try {
				serverSocket = new ServerSocket(5050);
				hServerSocket = new ServerSocket(5051);// 연결상태 확인하는 신호 소켓
				int emptyIdx;
				while ((emptyIdx = emptySpaceIdx()) != -1) {
					Socket s = serverSocket.accept();
					emptyIdx = emptySpaceIdx();
					ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
					ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
					String username = ois.readUTF();// 이름 받음
					User u = new User(s, s.getPort(), username, oos, ois);

					playerList.set(emptyIdx, u);
					addLog(username + " (" + s.getInetAddress() + ":" + s.getPort() + ") 접속");

					heartBeat = new HeartBeat(hServerSocket, emptyIdx);
					heartBeat.start();

					for (User p : playerList) {
						oos2 = (ObjectOutputStream) p.getOos();
						if (p.getPort() != 0) {
							oos2.writeInt(110);
							oos2.writeObject(playerList); // 추가 접속 userList 전송
							oos2.flush();
							oos2.reset(); // ! 중요 스트림 초기화를 하지않으면 같은 객체를 보낼시 이전 객체를 보냄(캐싱) !
						}
					}
				}
				if (emptySpaceIdx() == -1) { // 풀방이면,
					// 가장 먼저 들어온 유저가 스타트 누르면 시작
					roomMaster = playerList.get(0);
					for (int i = 1; i < playerList.size() - 1; i++) {
						if (roomMaster.getConnectTime() > playerList.get(i).getConnectTime()) {
							roomMaster = playerList.get(i);
						}
					}
					ObjectOutputStream oos1 = roomMaster.getOos();
					ObjectInputStream ois1 = roomMaster.getOis();
					oos1.writeInt(111);
					oos1.flush();
					addLog("풀방입니다.\n게임 시작 응답 대기 중 ...");
					announce("방장이 게임시작을 누르면 시작합니다.");
					
					while (true) {
						try {
							int startCode = ois1.readInt();
							if (startCode == 1) {
								addLog("게임을 시작합니다.");
								announce("게임을 시작합니다.");
								break;
							} else {
								//다시 유저 받기
							}
						} catch (Exception e) {
							continue;
						}
					}
				}

				newDeck(); // 새덱 생성
				shuffle(); // 덱 셔플
				for (User u : playerList) {
					giveCard(INIT_CARD_NUM, u); // 시작카드 분배
				}
				setTopCard();
				sendTopCard();
				sendPenalty();
				while (true) {
					if (turn < PLAYER_NUM) {
						turn += PLAYER_NUM;
					}
					User user = playerList.get(currPlayer);
					sendSth(107, "int", currPlayer);// []님이 카드를 선택중... 신호보내기
					ObjectInputStream ois = user.getOis();
					giveTurn(user);
					try {
						turn: while (true) {
							int answer = ois.readInt();
							switch (answer) {
							case 201: // 플레이어로부터 카드 1장 받기
								getCard(ois);
								sendPenalty();
								break turn;
							case 202: // 플레이어에게 카드 주기
								giveCard(penaltyNum, user);
								break turn;
							case 203: // 제한 시간 종료
								giveCard(penaltyNum, user);
								announce("[" + currPlayer + "]님이 시간초과로 차례를 넘깁니다.");
								break turn;
							}
						}
						if (!hasCard(user)) {
							break;
						}
						turn += turnOpt;
						currPlayer = turn % PLAYER_NUM;
						sendSth(115);// []님이 카드를 선택중... 스레드 종료명령

					} catch (IOException e) {
						addLog("IOException 발생");
						e.printStackTrace();
					}
				}
				addLog("----------- 게임 종료 -----------");
				sendSth(105, "String", playerList.get(currPlayer).getName());
			} catch (IOException e) {
				heartBeat.interrupt();
				e.printStackTrace();
			} finally {
				try {
					serverSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void giveTurn(User user) {
		int code = 102;
		try {
			ObjectOutputStream oos = user.getOos();
			oos.writeInt(code);
			for (User u : playerList) {
				ObjectOutputStream os = u.getOos();
				if (oos == os) {
					os.writeUTF("당신의 차례입니다.");
					os.flush();
					continue;
				}
				os.writeInt(103);
				os.writeUTF("[알림][" + user.getName() + "]님의 차례입니다.");
				os.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		addLog("[" + user.getName() + "]님의 차례입니다.");
	}

	private void getCard(ObjectInputStream ois) {
		Card card;
		try {
			card = (Card) ois.readObject();
			setTopCard(card);
			sendTopCard();
			if (card.number == 12) { // Q카드
				announce("Q카드이므로 방향을 바꿉니다.");
				turnOpt *= -1;
			} else if (card.number == 11) { // J카드
				announce("J카드이므로 한명을 건너뜁니다.");
				if (turnOpt == 1) {
					turn++;
				} else { // turnOpt == -1 일경우
					turn--;
				}
			} else if (card.number == 13) {// K카드
				announce("K카드이므로 턴을 한번 더 받습니다.");
				if (turnOpt == 1) {
					turn--;
				} else { // turnOpt == -1 일경우
					turn++;
				}
			} else if (card.number == 1) {// A카드
				if (card.shape.equals("SPADE")) { // 스페이드 A
					announce("스페이드 A카드이므로 4장 공격합니다.");
					penaltyNum += 4;
				} else {
					announce("A카드이므로 3장 공격합니다.");
					penaltyNum += 3;
				}
			} else if (card.number == 2) {
				announce("2카드이므로 2장 공격합니다.");
				penaltyNum += 2;
			} else if (card.number == 7) {
				announce("7카드이므로 다음 모양을 설정합니다.");
				giveChoice();
			} else if (card.number == 0) { // 조커
				announce("JOKER카드이므로 5장 공격합니다.");
				penaltyNum += 5;
			}
			playerList.get(currPlayer).setCardNum(playerList.get(currPlayer).getCardNum() - 1);
			sendUserCardNum(currPlayer, playerList.get(currPlayer).getCardNum());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void giveChoice() {
		int code = 109;
		User u = playerList.get(currPlayer);
		ObjectOutputStream oos = u.getOos();
		ObjectInputStream ois = u.getOis();
		try {
			oos.writeInt(code);
			oos.flush();
			String nextShape = ois.readUTF();
			sendTopCard(nextShape);
			addLog("다음 카드 설정 됨 : " + nextShape);
			announce("다음 카드의 모양은 " + nextShape + " 입니다.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void newDeck() { // 새덱 생성 (2~10,J,Q,K,A,JOKER) 53장
		deck = new ArrayList<Card>();
		for (int i = 1; i < 14; i++) {
			for (int j = 0; j < 4; j++) {
				String shape = "";
				switch (j) {
				case 0:
					shape = "SPADE";
					break;
				case 1:
					shape = "DIAMOND";
					break;
				case 2:
					shape = "HEART";
					break;
				case 3:
					shape = "CLOVER";
					break;
				}
				Card card = new Card(i, shape);
				deck.add(card);
			}
		}
		deck.add(new Card(0, "JOKER"));
	}

	private void giveCard(int num, User user) { // 유저에게 카드를 num장 줌
		// code 101
		if (num == 0) {
			num = 1;
		}
		getEnoughDeck(num); // 덱에 num장 이상 있는지 체크
		int code = 101;
		ObjectOutputStream oos = user.getOos();
		try {
			oos.writeInt(code);
			oos.writeInt(num);
			oos.flush();
			for (int j = 0; j < num; j++) {
				oos.writeObject(deck.get(0));
				deck.remove(0);
				oos.flush();
				user.setCardNum((user.getCardNum() + 1)); // playerList 카드 수 추가
			}
			penaltyNum = 0;
			sendPenalty();
		} catch (IOException e) {
			e.printStackTrace();
		}
		sendDeckNum();
		sendUserCardNum(playerList.indexOf(user), user.getCardNum());
		addLog("" + user.getCardNum());
		addLog("[" + user.getName() + "]님이 카드를" + num + "장 먹었습니다.\nDECK에 남은 카드 수 : " + deck.size());
		announce("[" + user.getName() + "]님이 카드를" + num + "장 먹었습니다.\nDECK에 남은 카드 수 : " + deck.size());
	}

	private void sendUserCardNum(int userIdx, int userCardNum) {
		int code = 113;
		for (User u : playerList) {
			ObjectOutputStream oos = u.getOos();
			try {
				oos.writeInt(code);
				oos.writeInt(userIdx);
				oos.writeInt(userCardNum);
				oos.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void sendDeckNum() {
		sendSth(112, "int", deck.size());
	}

	private void shuffle() {
		Collections.shuffle(deck);
		sendSth(114);
		try {
			Thread.sleep(2800);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void setTopCard(Card card) {// 플레이어가 제출한 카드 세팅
		usedCards.add(card);
		topCard = card;
		addLog("플레이어가 카드를 냄. topCard : " + topCard);
		addLog("낸카드목록 : " + usedCards);
		announce("[" + playerList.get(currPlayer).getName() + "]님이 카드를 냈습니다." + "\n현재 오픈된 카드 - " + topCard);
	}

	private void setTopCard() { // 첫 카드 세팅
		usedCards.add(deck.get(0));
		deck.remove(0);
		topCard = usedCards.get(usedCards.size() - 1);
		addLog("topCard 세팅 완료 : " + topCard);
		announce("현재 오픈된 카드 - " + topCard + "\nDECK에 남은 카드 수 : " + deck.size());
		sendDeckNum();
	}

	private void sendTopCard() {
		sendSth(104, "Object", topCard);
	}

	private void sendTopCard(String shape) {
		sendSth(104, "Object", new Card(shape));
	}

	private void addLog(String str) {
		textArea.append(str + "\n");
		textArea.setCaretPosition(textArea.getDocument().getLength());
	}

	private void announce(String text) {
		sendSth(103, "String", "[알림]" + text);
	}

	private class HeartBeat extends Thread {
		int uIdx;
		ServerSocket socket;
		Socket hSocket = null;

		public HeartBeat(ServerSocket socket, int uIdx) {
			this.socket = socket;
			this.uIdx = uIdx;
		}

		public void run() {
			try {
				hSocket = socket.accept();
				addLog("HeartBeat 연결 됨");
				ObjectOutputStream oos = new ObjectOutputStream(hSocket.getOutputStream());
				ObjectInputStream ois = new ObjectInputStream(hSocket.getInputStream());

				while (true) {
					try {
						oos.write(1);
						oos.flush();
						ois.read();
					} catch (IOException e) {
						addLog("[" + playerList.get(uIdx).getName() + "]님 연결 종료");
						hSocket.close();
						playerList.get(uIdx).getSocket().close();
						playerList.set(uIdx, emptyUser);
						break;
					}
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

			} catch (IOException e1) {
				e1.printStackTrace();
			}

		}

	}
}