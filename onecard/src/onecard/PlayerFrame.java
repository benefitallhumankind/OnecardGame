package onecard;

import java.awt.EventQueue;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JTextField;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import java.awt.Font;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.Label;
import java.awt.Canvas;
import java.awt.Color;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

public class PlayerFrame extends JFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7690529047291024586L;

	private Socket socket = null;
	private List<Card> myCards;
	private ObjectInputStream ois = null;
	private ObjectOutputStream oos = null;
	private List<User> userList = null;
	private Card topCard = null;
	private int maxUser = -1;
	private int nowUserNum = -1;
	private boolean hasTurn = false;
	private Map<Card, JButton> myCardBtnList = null;
	private int zOrder = 0;
	int topCardCount = 0;
	private int myIdx = -1;
	private int nextUserIdx;
	private int prevUserIdx;
	private Spinner s;

	private JPanel contentPane;
	private JTextField ServerIP;
	private JLayeredPane myCardLayer;
	private JPanel openCardPane;
	private JLayeredPane openCardLayer;
	private JButton getCard;
	private JLayeredPane nextUserLayer;
	private JLayeredPane prevUserLayer;
	private JLabel nextUserLabel;
	private JLabel nextUserCardNum;
	private JLabel prevUserLabel;
	private JLabel prevUserCardNum;
	private JLabel penaltyNum;
	private SelectShapeWindow newWindow;
	private JTextField UsernameTxt;
	private JTextArea logArea;
	private JLayeredPane mySpaceLayer;
	private JButton startBtn;
	private JButton deckBtn;
	private JLabel deckLabel;
	private JPanel penaltyArea;
	private JLabel loading;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					PlayerFrame frame = new PlayerFrame();
					frame.setVisible(true);
					frame.setAlwaysOnTop(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */

	public PlayerFrame() {

		setTitle("PLAYER");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(512, 573);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		Dimension frameSize = this.getSize(); // 프레임 사이즈
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize(); // 모니터 사이즈
		this.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2); // 화면 중앙

		Label nameLabel = new Label("User name :");
		nameLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
		nameLabel.setBounds(11, 10, 80, 23);
		contentPane.add(nameLabel);

		UsernameTxt = new JTextField();
		UsernameTxt.setText("Username");
		UsernameTxt.setColumns(10);
		UsernameTxt.setBounds(95, 10, 78, 21);
		contentPane.add(UsernameTxt);

		ServerIP = new JTextField();
		ServerIP.setText("localhost");
		ServerIP.setBounds(259, 10, 116, 21);
		contentPane.add(ServerIP);
		ServerIP.setColumns(10);

		JButton btnConn = new JButton("CONNECT");
		btnConn.setBounds(387, 10, 97, 23);
		contentPane.add(btnConn);

		getCard = new JButton("<html>카드받기</html>");
		getCard.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
		getCard.setBounds(398, 349, 86, 49);
		contentPane.add(getCard);
		getCard.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (hasTurn) {
					try {
						oos.writeInt(202);
						oos.flush();
						hasTurn = false;
						JOptionPane.showMessageDialog(contentPane, "카드를 받았습니다. 차례를 넘깁니다.");
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
		});

		startBtn = new JButton("게 임 시 작");
		startBtn.setBounds(115, 149, 266, 72);
		contentPane.add(startBtn);
		startBtn.setForeground(new Color(255, 255, 255));
		startBtn.setBackground(new Color(255, 153, 153));
		startBtn.setFont(new Font("맑은 고딕", Font.PLAIN, 25));
		startBtn.setVisible(false);
		startBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					oos.writeInt(1);
					oos.flush();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				startBtn.setVisible(false);
			}
		});
		JPanel prevUserSpace = new JPanel();
		prevUserSpace.setBounds(0, 63, 115, 240);
		contentPane.add(prevUserSpace);
		prevUserSpace.setLayout(null);

		prevUserLabel = new JLabel("");
		prevUserLabel.setForeground(new Color(128, 128, 128));
		prevUserLabel.setBounds(10, 5, 105, 15);
		prevUserSpace.add(prevUserLabel);

		prevUserCardNum = new JLabel("");
		prevUserCardNum.setBounds(10, 25, 106, 15);
		prevUserSpace.add(prevUserCardNum);

		JPanel prevUserPane = new JPanel();
		prevUserPane.setBounds(-28, 47, 88, 192);
		prevUserSpace.add(prevUserPane);
		prevUserPane.setLayout(null);

		prevUserLayer = new JLayeredPane();
		prevUserLayer.setBounds(0, 0, 88, 192);
		prevUserPane.add(prevUserLayer);

		JPanel nextUserSpace = new JPanel();
		nextUserSpace.setBounds(381, 63, 115, 240);
		contentPane.add(nextUserSpace);
		nextUserSpace.setLayout(null);

		nextUserLabel = new JLabel("");
		nextUserLabel.setForeground(new Color(128, 128, 128));
		nextUserLabel.setBounds(0, 5, 105, 15);
		nextUserSpace.add(nextUserLabel);
		nextUserLabel.setHorizontalAlignment(SwingConstants.RIGHT);

		nextUserCardNum = new JLabel("");
		nextUserCardNum.setBounds(0, 25, 103, 15);
		nextUserSpace.add(nextUserCardNum);
		nextUserCardNum.setHorizontalAlignment(SwingConstants.RIGHT);

		JPanel nextUserPane = new JPanel();
		nextUserPane.setBounds(60, 47, 88, 192);
		nextUserSpace.add(nextUserPane);
		nextUserPane.setLayout(null);

		nextUserLayer = new JLayeredPane();
		nextUserLayer.setBounds(0, 0, 88, 192);
		nextUserPane.add(nextUserLayer);

		openCardPane = new JPanel();
		openCardPane.setBounds(115, 126, 176, 125);
		contentPane.add(openCardPane);
		openCardPane.setLayout(null);

		openCardLayer = new JLayeredPane();
		openCardLayer.setBounds(0, 0, 176, 125);
		openCardPane.add(openCardLayer);
		openCardPane.setVisible(true);

		deckLabel = new JLabel("53장");
		deckLabel.setFont(new Font("맑은 고딕", Font.BOLD, 20));
		deckLabel.setForeground(new Color(255, 255, 255));
		deckLabel.setHorizontalAlignment(SwingConstants.CENTER);
		deckLabel.setBounds(307, 170, 74, 30);
		contentPane.add(deckLabel);
		deckLabel.setVisible(false);

		deckBtn = new JButton(getImgIcon("/onecard/png/back_org.png", 74, 125));
		deckBtn.setBounds(307, 126, 74, 125);
		contentPane.add(deckBtn);
		deckBtn.addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
			}

			@Override
			public void mouseExited(MouseEvent e) {
				deckLabel.setVisible(false);
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				deckLabel.setVisible(true);
			}

			@Override
			public void mouseClicked(MouseEvent e) {
			}
		});
		deckBtn.setFocusPainted(false);

		penaltyArea = new JPanel();
		penaltyArea.setBounds(170, 261, 134, 30);
		contentPane.add(penaltyArea);
		penaltyArea.setLayout(null);
		penaltyArea.setVisible(false);

		penaltyNum = new JLabel("0");
		penaltyNum.setBounds(102, 5, 20, 15);
		penaltyArea.add(penaltyNum);
		penaltyNum.setHorizontalAlignment(SwingConstants.RIGHT);

		JLabel penalty = new JLabel("공격 카드 수 :");
		penalty.setHorizontalAlignment(SwingConstants.RIGHT);
		penalty.setBounds(0, 5, 95, 15);
		penaltyArea.add(penalty);

		Canvas canvas = new Canvas();
		canvas.setBackground(Color.LIGHT_GRAY);
		canvas.setBounds(10, 42, 472, 1);
		contentPane.add(canvas);

		JPanel mySpace = new JPanel();
		mySpace.setBounds(0, 310, 496, 224);
		contentPane.add(mySpace);
		mySpace.setLayout(null);

		mySpaceLayer = new JLayeredPane();
		mySpaceLayer.setBounds(0, 0, 496, 224);
		mySpace.add(mySpaceLayer);

		JLayeredPane myCardPane = new JLayeredPane();
		myCardPane.setBounds(12, 0, 375, 155);
		mySpaceLayer.add(myCardPane);

		myCardLayer = new JLayeredPane();
		myCardLayer.setBounds(0, 0, 375, 155);
		myCardPane.add(myCardLayer);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(12, 100, 472, 114);
		mySpaceLayer.add(scrollPane);
		scrollPane.setEnabled(false);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		logArea = new JTextArea();
		logArea.setEditable(false);
		logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

		scrollPane.setViewportView(logArea);
		mySpaceLayer.setLayer(scrollPane, 9999);

		loading = new JLabel("카드를 섞는 중");
		loading.setFont(new Font("맑은 고딕", Font.BOLD, 15));
		loading.setHorizontalAlignment(SwingConstants.CENTER);
		loading.setBounds(115, 63, 266, 40);
		contentPane.add(loading);
		loading.setVisible(false);

		btnConn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (btnConn.getText().equals("CONNECT")) {
					addLog("서버에 연결합니다.");
					UsernameTxt.setEditable(false);
					myCards = new ArrayList<>();
					myCardBtnList = new HashMap<Card, JButton>();
					try {
						socket = new Socket(ServerIP.getText(), 5050);
						addLog("서버와 연결되었습니다.");
						btnConn.setText("CLOSE");
						addLog("내 소켓번호 : " + socket.getLocalPort());
						ois = new ObjectInputStream(socket.getInputStream());
						oos = new ObjectOutputStream(socket.getOutputStream());
						newWindow = new SelectShapeWindow(contentPane, oos, myCardBtnList);
						oos.writeUTF(UsernameTxt.getText());// 이름 전송
						oos.flush();
						getUserList(); // 접속한 유저 목록 받기
						setIdx();
						drawOtherName();

						// 접속한 유저, 빈자리 출력
						addLog("최대 플레이어 " + maxUser + "명 방입니다.");

						Thread receiver = new Thread() {
							public void run() {
								try {
									game: while (true) {
										int code = ois.readInt();
										switch (code) {
										case 110:// 추가 접속 유저 받기
											drawOtherName(setNewUser());
											break;
										case 111:
											showStartBtn();
											break;
										case 112:
											getDeckNum();
											break;
										case 113: // 카드 수 확인, 그리기
											getUserCardNum();
											break;
										case 114:
											switchShuffle();
											break;
										case 115:
											if (s != null) {
												s.interrupt();
												s = null;
											}
											break;
										case 101:// 카드 받기
											getCards();
											contentPane.updateUI();
											break;
										case 102:// 턴 받기
											getTurn();
											break;
										case 103:// 알림 수신
											getAnnounce();
											break;
										case 104:// topCard 확인
											getTopCard();
											break;
										case 105:
											endGame();
											break game;
										case 106:
											resetUsedCard();
											break;
										case 107:
											showNowPlayer();
											break;
										case 108:
											checkPenalty();
											break;
										case 109:
											setChoice();
											hasTurn = false;
											break;
										}
									}
								} catch (IOException e) {
									addLog("서버와 연결이 끊어졌습니다.(IOException)");
								}
							}

						};
						receiver.start();

					} catch (UnknownHostException e1) {
						addLog("서버주소를 찾을 수 없거나 서버가 닫혀있습니다.(UnknownHostException)");
					} catch (IOException e1) {
						addLog("서버와 스트림 연결을 실패하였습니다.(IOException)");
					}
				} else {
					System.exit(0);
				}
			}
		});
	}

	private void getUserCardNum() {
		try {
			User user = userList.get(ois.readInt());
			user.setCardNum(ois.readInt());
			drawHand(user);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void getDeckNum() {
		try {
			deckLabel.setText(ois.readInt() + "장");
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void showStartBtn() {
		startBtn.setVisible(true);
	}

	private void addLog(String str) {
		logArea.append(str + "\n");
		logArea.setCaretPosition(logArea.getDocument().getLength());
	}

	@SuppressWarnings("unchecked")
	private void getUserList() {
		try {
			userList = (List<User>) ois.readObject();
			addLog("┌── 현재 접속자 ──┐");
			for (int i = 0; i < userList.size(); i++) {
				addLog("  " + (i + 1) + ". " + userList.get(i));
				if (userList.get(i).getPort() == socket.getLocalPort()) {
					myIdx = i;
					nowUserNum = i + 1; // 현재 접속유저 수
					break;
				}
			}
			maxUser = userList.size();
			addLog("└───────────┘ 현재 인원: " + nowUserNum + "명/" + maxUser + "명");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void checkPenalty() {
		penaltyArea.setVisible(true);
		try {
			int penalty = ois.readInt();
			penaltyNum.setText(penalty + "");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private User setNewUser() {
		try {
			User u = (User) ois.readObject();
			userList.set(nowUserNum, u);
			addLog("[" + u.getName() + "]님이 접속하셨습니다.");
			addLog("┌── 현재 접속자 ──┐");
			for (int i = 0; i <= nowUserNum; i++) {
				addLog("  " + (i + 1) + ". " + userList.get(i));
			}
			nowUserNum++; // 현재 접속유저 수
			addLog("└───────────┘ 현재 인원: " + nowUserNum + "명/" + maxUser + "명");
			return u;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

//	private void drawInit(int num) {
//		nextUserCardNum.setText(num + "장");
//		prevUserCardNum.setText(num + "장");
//		for (int i = 0; i < num; i++) {
//			JButton otherCardBtn = new JButton(getImgIcon("/onecard/png/back.png", 88, 52));
//			nextUserLayer.setLayer(otherCardBtn, i);
//			otherCardBtn.setBounds(0, 0 + (7 * i), 88, 52);
//			nextUserLayer.add(otherCardBtn);
//
//			otherCardBtn.setBorderPainted(false);
//			otherCardBtn.setFocusPainted(false);
//			otherCardBtn.setContentAreaFilled(false);
//
//			JButton otherCardBtn2 = new JButton(getImgIcon("/onecard/png/back.png", 88, 52));
//			prevUserLayer.setLayer(otherCardBtn2, i);
//			otherCardBtn2.setBounds(0, 0 + (7 * i), 88, 52);
//			prevUserLayer.add(otherCardBtn2);
//
//			otherCardBtn2.setBorderPainted(false);
//			otherCardBtn2.setFocusPainted(false);
//			otherCardBtn2.setContentAreaFilled(false);
//		}
//	}
	private void showNowPlayer() {
		int nowUserIdx;
		try {
			nowUserIdx = ois.readInt();
			if (nowUserIdx != myIdx) {
				loading.setText("[" + userList.get(nowUserIdx).getName() + "]님 카드 선택 중");
				s = new Spinner();
				s.start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void switchShuffle() {
		loading.setText("카드를 섞는 중");
		loading.setVisible(true);

		String org = loading.getText();
		for (int i = 0; i < 8; i++) {
			String tmp = loading.getText();
			if (i % 4 == 0) {
				tmp = org;
			} else {
				tmp += ".";
			}
			loading.setText(tmp);
			try {
				Thread.sleep(400);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}
		loading.setVisible(false);
	}

	private void drawHand(User user) {
		int uIdx = userList.indexOf(user);
		if (uIdx == nextUserIdx) {
			nextUserCardNum.setText(user.getCardNum() + "장");
			nextUserLayer.removeAll();
			for (int i = 0; i < user.getCardNum(); i++) {
				JButton otherCardBtn = new JButton(getImgIcon("/onecard/png/back.png", 88, 52));
				nextUserLayer.setLayer(otherCardBtn, i);
				otherCardBtn.setBounds(0, 0 + (7 * i), 88, 52);
				nextUserLayer.add(otherCardBtn);

				otherCardBtn.setBorderPainted(false);
				otherCardBtn.setFocusPainted(false);
				otherCardBtn.setContentAreaFilled(false);
				prevUserLayer.updateUI();
			}
		} else if (uIdx == prevUserIdx) {
			prevUserCardNum.setText(user.getCardNum() + "장");
			prevUserLayer.removeAll();
			for (int i = 0; i < user.getCardNum(); i++) {
				JButton otherCardBtn = new JButton(getImgIcon("/onecard/png/back.png", 88, 52));
				prevUserLayer.setLayer(otherCardBtn, i);
				otherCardBtn.setBounds(0, 0 + (7 * i), 88, 52);
				prevUserLayer.add(otherCardBtn);

				otherCardBtn.setBorderPainted(false);
				otherCardBtn.setFocusPainted(false);
				otherCardBtn.setContentAreaFilled(false);
				prevUserLayer.updateUI();
			}
		}
	}

	private void setIdx() {
		if (maxUser > 2) {
			nextUserIdx = myIdx + 1;
			prevUserIdx = myIdx - 1;
			int maxIdx = maxUser - 1;
			if (nextUserIdx > maxIdx) {
				nextUserIdx = 0;
			}
			if (prevUserIdx < 0) {
				prevUserIdx = maxIdx;
			}
		}
	}

	private void drawOtherName(User u) {
		String userName = u.getName();
		int uIdx = userList.indexOf(u);
		if (nextUserIdx == uIdx) {
			nextUserLabel.setText(userName);
		} else if (prevUserIdx == uIdx) {
			prevUserLabel.setText(userName);
		}
	}

	private void drawOtherName() {
		for (int i = 0; i < userList.size(); i++) {
			String userName = userList.get(i).getName();
			if (nextUserIdx == i) {
				nextUserLabel.setText(userName);
			} else if (prevUserIdx == i) {
				prevUserLabel.setText(userName);
			}
		}
	}


	private void resetUsedCard() {
		topCardCount = 0;
		openCardLayer.removeAll();

		JButton Card = new JButton(getImgIcon(topCard, 74, 125));
		openCardLayer.setLayer(Card, topCardCount);
		Card.setBounds(0 + (topCardCount * 2), 0, 74, 125);
		openCardLayer.add(Card);
		Card.setBorderPainted(false);
		Card.setFocusPainted(false);
		Card.setContentAreaFilled(false);

		contentPane.updateUI();
		topCardCount++;

	}

	private void endGame() {
		try {
			String winner = ois.readUTF();
			JOptionPane.showMessageDialog(contentPane, "[" + winner + "]님이 승리하였습니다!!");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void getTopCard() {
		try {
			topCard = (Card) ois.readObject();
			System.out.println("현재 topCard : " + topCard);
			JButton Card = new JButton(getImgIcon(topCard, 74, 125));
			openCardLayer.setLayer(Card, topCardCount);
			Card.setBounds(0 + (topCardCount * 2), 0, 74, 125);
			openCardLayer.add(Card);
			Card.setBorderPainted(false);
			Card.setFocusPainted(false);
			Card.setContentAreaFilled(false);

			contentPane.updateUI();
			topCardCount++;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void getAnnounce() {
		try {
			String text = ois.readUTF();
			addLog(text);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void getTurn() {
		try {
			hasTurn = true;
			String text = ois.readUTF();
			addLog("[알림]" + text); // 당신의 차례입니다.
			JOptionPane.showMessageDialog(contentPane, "당신의 차례입니다.");
			List<Card> recCards = getRecommend();

			if (recCards.size() == 0) {
				System.out.println("낼 수 있는 카드가 없습니다.");
				JOptionPane.showMessageDialog(contentPane, "낼 수 있는 카드가 없으므로\n카드받기를 눌러주세요.");
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private List<Card> getRecommend() {
		List<Card> recCards = new ArrayList<Card>();
		if (topCard.number == 0) {
			recCards = myCards;
			return recCards;
		}

		for (Card c : myCards) {
			if (c.shape.equals(topCard.shape) || c.number == topCard.number || c.number == 0) {
				recCards.add(c);
			}
		}
		return recCards;
	}

	private void giveCard(Card c) {
		try {
			oos.writeInt(201);
			oos.writeObject(c);
			oos.flush();
			myCards.remove(c);
			System.out.println(c + "를 제출 했습니다.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void getCards() {
		// CODE 101
		int amountCards = 0;
		try {
			amountCards = ois.readInt();
			System.out.println("받은 카드 " + amountCards + "장 :");
			for (int i = 0; i < amountCards; i++) {
				Card c = (Card) ois.readObject();
				myCards.add(c);
				System.out.println(c);

				addMyCard(c);
			}
			oos.writeInt(myCards.size());
			oos.flush();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		System.out.println("보유 카드 : " + myCards);
	}

	public String getFileName(Card c) {
		String filename = "";
		switch (c.number) {
		case 1:
			filename += "A";
			break;
		case 2:
		case 3:
		case 4:
		case 5:
		case 6:
		case 7:
		case 8:
		case 9:
		case 10:
			filename += c.number;
			break;
		case 11:
			filename += "J";
			break;
		case 12:
			filename += "Q";
			break;
		case 13:
			filename += "K";
			break;
		default:
			filename += "";
			break;
		}
		switch (c.shape) {
		case "SPADE":
		case "DIAMOND":
		case "HEART":
		case "CLOVER":
			filename += c.shape.charAt(0);
			break;
		case "JOKER":
			filename += "joker";
			break;
		}
		filename += ".png";
		return filename;
	}

	public ImageIcon getImgIcon(Card c, int width, int height) {
		URL searchURL = getClass().getResource("/onecard/png/" + getFileName(c));
		ImageIcon imgIcon = new ImageIcon(searchURL);
		Image originImg = imgIcon.getImage();
		Image changedImg = originImg.getScaledInstance(width, height, Image.SCALE_SMOOTH);// 이미지 변경 세팅
		return imgIcon = new ImageIcon(changedImg);
	}

	public ImageIcon getImgIcon(String src, int width, int height) {
		URL searchURL = getClass().getResource(src);
		ImageIcon imgIcon = new ImageIcon(searchURL);
		Image originImg = imgIcon.getImage();
		Image changedImg = originImg.getScaledInstance(width, height, Image.SCALE_SMOOTH);// 이미지 변경 세팅
		return imgIcon = new ImageIcon(changedImg);
	}

	public void addMyCard(Card c) {
		JButton Card = new JButton(getImgIcon(c, 74, 125));
		myCardLayer.setLayer(Card, zOrder);
		int thisZOrder = zOrder;
		Card.setBounds(0 + (15 * (myCards.size() - 1)), 30, 74, 125);
		myCardLayer.add(Card);
		Card.addMouseListener(new MouseListener() {

			@Override
			public void mouseReleased(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
			}

			@Override
			public void mouseExited(MouseEvent e) {
				JButton thisBtn = (JButton) e.getSource();
				myCardLayer.setLayer(thisBtn, thisZOrder);
				thisBtn.setLocation(thisBtn.getLocation().x, 30);
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				JButton thisBtn = (JButton) e.getSource();
				myCardLayer.setLayer(thisBtn, zOrder);
				thisBtn.setLocation(thisBtn.getLocation().x, 0);
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				if (hasTurn) {
					if (topCard.number == 0 || c.shape.equals(topCard.shape) || c.number == topCard.number
							|| c.number == 0) {

						if (!penaltyNum.getText().equals("0")) {
							if (topCard.number == 0) {
								JOptionPane.showMessageDialog(contentPane, "JOKER를 막을 수 없습니다.\n드십시오.");
								return;
							} else if (c.number != 1 && c.number != 2 && c.number != 0) {
								JOptionPane.showMessageDialog(contentPane,
										"공격카드(A,2,JOKER)로만 방어가 가능합니다." + "\n없으시면 드셔야합니다.");
								return;
							}
						}
						giveCard(c);
						myCardBtnList.remove(c);
						resetCardList();
						if (c.number == 7) {
							return;
						}
						if (c.number == 13) {
							return;
						}
						try {
							oos.writeInt(myCardBtnList.size());
							oos.flush();
							hasTurn = false;
							JOptionPane.showMessageDialog(contentPane, "카드를 제출했습니다. 차례를 넘깁니다.");
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					} else {
						JOptionPane.showMessageDialog(contentPane, "모양이나 숫자가 다른 카드입니다.");
					}
				} else {
					JOptionPane.showMessageDialog(contentPane, "아직 차례가 아닙니다.");
				}
			}
		});
		zOrder++;
		myCardBtnList.put(c, Card);
		Card.setBorderPainted(false);
		Card.setFocusPainted(false);
		Card.setContentAreaFilled(false);

		contentPane.updateUI();
	}

	public void setChoice() {
		newWindow.setLocation(getX() + (getWidth() - newWindow.getWidth()) / 2,
				getY() + (getHeight() - newWindow.getHeight()) / 2);
		newWindow.setVisible(true);
	}

	public void checkBtnList() {
		for (Card c : myCardBtnList.keySet()) {
			System.out.print(c + " ");
		}
		System.out.println();
	}

	public void resetCardList() {
		myCardLayer.removeAll();
		int count = 0;
		for (Card c : myCardBtnList.keySet()) {
			addMyCard(c);
			myCardBtnList.get(c).setBounds(0 + (15 * count), 30, 74, 125);
			count++;
		}
		for (Card m : myCardBtnList.keySet()) {
			System.out.print(m + " ");
		}
		System.out.println();
		contentPane.updateUI();
	}

	class Spinner extends Thread {
		public void run() {
			loading.setVisible(true);
			String org = loading.getText();
			int i = 0;
			while (true) {
				String tmp = loading.getText();
				if (i % 4 == 0) {
					tmp = org;
				} else {
					tmp += ".";
				}
				loading.setText(tmp);
				i++;
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					break;
				}
			}
			loading.setVisible(false);
		}
	}
}

class SelectShapeWindow extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;

	public ImageIcon getImgIcon(String src, int width, int height) {
		URL searchURL = getClass().getResource(src);
		ImageIcon imgIcon = new ImageIcon(searchURL);
		Image originImg = imgIcon.getImage();
		Image changedImg = originImg.getScaledInstance(width, height, Image.SCALE_SMOOTH);// 이미지 변경 세팅
		return imgIcon = new ImageIcon(changedImg);
	}

	public SelectShapeWindow(Component c, ObjectOutputStream oos, Map<Card, JButton> myCardBtnList) {
		setTitle("다음 모양 결정");
		contentPane = new JPanel();
		setSize(300, 180);
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		JLabel label = new JLabel("다음 카드의 모양을 결정해주세요.");
		label.setHorizontalAlignment(SwingConstants.CENTER);
		label.setBounds(12, 10, 260, 15);
		contentPane.add(label);

		JButton spade = new JButton(getImgIcon("/onecard/png/S.png", 56, 84));
		spade.setBounds(12, 50, 56, 84);
		contentPane.add(spade);
		spade.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					oos.writeUTF("SPADE");
					oos.flush();
					setVisible(false);
					oos.writeInt(myCardBtnList.size());
					oos.flush();
					JOptionPane.showMessageDialog(c, "SPADE로 설정되었습니다.");
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});

		JButton diamond = new JButton(getImgIcon("/onecard/png/D.png", 56, 84));
		diamond.setBounds(80, 50, 56, 84);
		contentPane.add(diamond);
		diamond.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					oos.writeUTF("DIAMOND");
					oos.flush();
					setVisible(false);
					oos.writeInt(myCardBtnList.size());
					oos.flush();
					JOptionPane.showMessageDialog(c, "DIAMOND로 설정되었습니다.");
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});

		JButton heart = new JButton(getImgIcon("/onecard/png/H.png", 56, 84));
		heart.setBounds(148, 50, 56, 84);
		contentPane.add(heart);
		heart.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					oos.writeUTF("HEART");
					oos.flush();
					setVisible(false);
					oos.writeInt(myCardBtnList.size());
					oos.flush();
					JOptionPane.showMessageDialog(c, "HEART로 설정되었습니다.");
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});

		JButton clover = new JButton(getImgIcon("/onecard/png/C.png", 56, 84));
		clover.setBounds(216, 50, 56, 84);
		contentPane.add(clover);
		clover.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					oos.writeUTF("CLOVER");
					oos.flush();
					setVisible(false);
					oos.writeInt(myCardBtnList.size());
					oos.flush();
					JOptionPane.showMessageDialog(c, "CLOVER로 설정되었습니다.");
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		setVisible(false);
		setAlwaysOnTop(true);
	}
}