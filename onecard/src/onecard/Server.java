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
	private boolean win = false;

	private JPanel contentPane;
	private JTextArea textArea;

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
			pl.add(new User(null, 0, "대기중...", null, null));
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
		setBounds(100, 100, 420, 460);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		JPanel panel = new JPanel();
		panel.setBounds(12, 10, 269, 401);
		contentPane.add(panel);
		panel.setLayout(null);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(0, 0, 281, 421);
		panel.add(scrollPane);

		textArea = new JTextArea();
		scrollPane.setViewportView(textArea);

		JButton btnOnOff = new JButton("ON");
		btnOnOff.setBounds(295, 10, 97, 35);
		contentPane.add(btnOnOff);
		btnOnOff.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				textArea.setText("서버 대기중 ...\n");
				btnOnOff.setText("OFF");
				Thread ServerThread = new Thread() {
					int idx = 0;

					public void run() {
						ServerSocket serverSocket = null;
						try {
							serverSocket = new ServerSocket(5050);
							while (idx < PLAYER_NUM) {
								Socket s = serverSocket.accept();
								ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
								ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
								String username = ois.readUTF();// 이름 받음
								User u = new User(s, s.getPort(), username, oos, ois);
								playerList.set(idx, u);
								addLog(username + " (" + s.getInetAddress() + ":" + s.getPort() + ") 접속");
								oos.writeObject(playerList); // playerList전송
								oos.flush();
								for (int i = 0; i < idx; i++) {
									ObjectOutputStream oos2 = (ObjectOutputStream) (playerList.get(i)).getOos();
									oos2.writeInt(110); // 추가 접속 목록 전송코드
									oos2.writeObject(u);
									oos2.flush();
								}
								idx++;
								if (idx > PLAYER_NUM) {
									idx = PLAYER_NUM;
								}
							}

							// 1번 유저가 스타트 누르면 시작
							ObjectOutputStream oos1 = playerList.get(0).getOos();
							ObjectInputStream ois1 = playerList.get(0).getOis();
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
									}
								} catch (Exception e) {
									continue;
								}
							}
							newDeck(); // 새덱 생성
							shuffle(); // 덱 셔플
							Thread.sleep(2000);
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
								ObjectInputStream ois = user.getOis();
								giveTurn(user);
								try {
									turn: while (true) {
										int answer = ois.readInt();
										switch (answer) {
										case 203: // 게임시작요청 받음
											break;
										case 201: // 플레이어로부터 카드 1장 받기
											getCard(ois);
											sendPenalty();
											break turn;
										case 202: // 플레이어가 카드 받아야함
											giveCard(penaltyNum, user);
											break turn;
										}
									}
									getUserRemain(user);
									if (win) {
										break;
									}
									turn += turnOpt;
									currPlayer = turn % PLAYER_NUM;

								} catch (IOException e) {
									addLog("IOException 발생");
									e.printStackTrace();
									continue;
								}
							}
							addLog("----------- 게임 종료 -----------");
							sendObject(105, "String", playerList.get(currPlayer).getName());
						} catch (IOException e) {
							e.printStackTrace();
						} catch (InterruptedException e) {
							e.printStackTrace();
						} finally {
							try {
								serverSocket.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
				};
				ServerThread.start();

			}
		});

		setAlwaysOnTop(true);
		setVisible(true);

	}

	// 모든 플레이어에게 code와 해당 content를 보내는 메서드
	private void sendObject(int code, String type, Object content) {
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
				default:
					break;
				}
				oos.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void getEnoughDeck(int num) {
		if (deck.size() < num) {
			announce("덱에 카드가 없으므로 사용한 카드를 섞어 보충합니다.");
			List<Card> temp = new ArrayList<Card>();
			temp.addAll(usedCards);
			temp.remove(topCard);
			deck.addAll(temp);
			usedCards.clear();
			usedCards.add(topCard);
			shuffle();
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
		sendObject(108, "int", penaltyNum);
	}

	private void getUserRemain(User user) {
		try {
			int code = 107;
			int cardNum = user.getOis().readInt();
			if (cardNum == 0) {
				addLog("[" + user.getName() + "]가 승리하였습니다 !!");
				win = true;
			} else {
				addLog("[" + user.getName() + "]의 남은 카드 수 : " + cardNum);
				for (User other : playerList) {
					ObjectOutputStream oos = (ObjectOutputStream) other.getOos();
					if (other.equals(user)) {
						continue;
					} else {
						oos.writeInt(code);
						oos.writeInt(playerList.indexOf(user));
						oos.writeInt(cardNum);
						oos.flush();
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
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
		user.setCardNum(user.getCardNum() + num); // playerList 카드 수 추가
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
			}
			penaltyNum = 0;
			sendPenalty();
		} catch (IOException e) {
			e.printStackTrace();
		}
		sendDeckNum();
		sendUserCardNum();
		addLog("[" + user.getName() + "]님이 카드를" + num + "장 먹었습니다.\nDECK에 남은 카드 수 : " + deck.size());
		announce("[" + user.getName() + "]님이 카드를" + num + "장 먹었습니다.\nDECK에 남은 카드 수 : " + deck.size());
	}

	private void sendUserCardNum() {
		int code = 113;
		ObjectOutputStream oos = playerList.get(currPlayer).getOos();
		try {
			oos.writeInt(code);
			oos.writeInt(currPlayer);
			oos.writeInt(playerList.get(currPlayer).getCardNum());
			oos.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void sendDeckNum() {
		sendObject(112, "int", deck.size());
	}

	private void shuffle() {
		Collections.shuffle(deck);
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
		sendObject(104, "Object", topCard);
	}

	private void sendTopCard(String shape) {
		sendObject(104, "Object", new Card(shape));
	}

	private void addLog(String str) {
		textArea.append(str + "\n");
		textArea.setCaretPosition(textArea.getDocument().getLength());
	}

	private void announce(String text) {
		sendObject(103, "String", "[알림]" + text);
	}
}