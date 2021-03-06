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
	private HeartBeat heartBeat;
	private Thread countThread;

	private JPanel contentPane;
	private JTextField serverIP;
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
	private JTextField userNameTxt;
	private JTextArea logArea;
	private JPanel mySpace;
	private JLayeredPane mySpaceLayer;
	private JButton startBtn;
	private JButton deckBtn;
	private JLabel deckLabel;
	private JPanel penaltyArea;
	private JLabel loading;
	private JPanel prevUserSpace;
	private JPanel nextUserSpace;
	private JLabel myCardNumLabel;
	private JLabel myCardNum;
	private JLabel hourGlass;

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

	private class HeartBeat extends Thread {
		Socket hSocket = null;

		public void run() {
			try {
				hSocket = new Socket(serverIP.getText(), 5051);
				addLog("HeartBeat 연결 됨");
				ObjectOutputStream oos = new ObjectOutputStream(hSocket.getOutputStream());
				ObjectInputStream ois = new ObjectInputStream(hSocket.getInputStream());
				while (true) {
					try {
						oos.write(1);
						oos.flush();
						ois.read();
					} catch (IOException e) {
						try {
							oos.write(1);
							addLog("서버와 연결이 끊어졌습니다.");
							socket.close();
							hSocket.close();
							break;
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
				}
			} catch (UnknownHostException e2) {
				e2.printStackTrace();
			} catch (IOException e2) {

			}
		}
	}

	/**
	 * Create the frame.
	 */

	public PlayerFrame() {
		setTitle("PLAYER");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setResizable(false);
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

		userNameTxt = new JTextField();
		userNameTxt.setText("Username");
		userNameTxt.setColumns(10);
		userNameTxt.setBounds(95, 10, 78, 21);
		contentPane.add(userNameTxt);

		serverIP = new JTextField();
		serverIP.setText("localhost");
		serverIP.setBounds(245, 10, 116, 21);
		contentPane.add(serverIP);
		serverIP.setColumns(10);

		JButton btnConn = new JButton("CONNECT");
		btnConn.setBounds(365, 10, 115, 23);
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
						countThread.interrupt();
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
		prevUserSpace = new JPanel();
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

		prevUserLayer = new JLayeredPane();
		prevUserLayer.setBounds(-38, 48, 88, 192);
		prevUserSpace.add(prevUserLayer);

		nextUserSpace = new JPanel();
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

		nextUserLayer = new JLayeredPane();
		nextUserLayer.setBounds(65, 48, 88, 192);
		nextUserSpace.add(nextUserLayer);

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

		deckBtn = new JButton(getImgIcon("/onecard/img/back_org.png", 74, 125));
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

		mySpace = new JPanel();
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

		myCardNum = new JLabel("");
		myCardNum.setBounds(88, 5, 20, 15);
		mySpace.add(myCardNum);

		myCardNumLabel = new JLabel("내 카드 수 :");
		myCardNumLabel.setBounds(15, 5, 70, 15);
		mySpace.add(myCardNumLabel);
		myCardNumLabel.setVisible(false);

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
					userNameTxt.setEditable(false);
					serverIP.setEditable(false);
					myCards = new ArrayList<>();
					myCardBtnList = new HashMap<Card, JButton>();
					try {
						socket = new Socket(serverIP.getText(), 5050);
						addLog("서버와 연결되었습니다.");
						btnConn.setText("DISCONNECT");

						heartBeat = new HeartBeat();
						heartBeat.start();

						addLog("내 소켓번호 : " + socket.getLocalPort());
						ois = new ObjectInputStream(socket.getInputStream());
						oos = new ObjectOutputStream(socket.getOutputStream());
						newWindow = new SelectShapeWindow(contentPane, oos, myCardBtnList);
						oos.writeUTF(userNameTxt.getText());// 이름 전송
						oos.flush();

						Thread receiver = new Thread() {
							public void run() {
								try {
									game: while (true) {
										int code = ois.readInt();
										switch (code) {
										case 110:// 접속 유저 리스트 받기, 세팅
											getUserList();
											setIdx();
											drawOtherName();
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
											myCardNumLabel.setVisible(true);
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
						userNameTxt.setEditable(true);
						serverIP.setEditable(true);
						addLog("서버주소를 찾을 수 없거나 서버가 닫혀있습니다.(UnknownHostException)");
					} catch (IOException e1) {
						userNameTxt.setEditable(true);
						serverIP.setEditable(true);
						addLog("서버와 스트림 연결을 실패하였습니다.(IOException)");
					}
				} else {
					heartBeat.interrupt();
					startBtn.setVisible(false);
					btnConn.setText("CONNECT");
					userNameTxt.setEditable(true);
					serverIP.setEditable(true);
					try {
						heartBeat.hSocket.close();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
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

			addLog("받은 리스트! : " + userList);
			addLog("┌── 현재 접속자 ──┐");
			int count = 0;
			for (int i = 0; i < userList.size(); i++) {
				if (userList.get(i).getPort() != 0) {
					if (userList.get(i).getPort() == socket.getLocalPort()) {
						myIdx = i;
					}
					addLog("  " + (count + 1) + ". " + userList.get(i).getName());
					count++;
				}
			}
			nowUserNum = count;
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

	private void showNowPlayer() {
		int nowUserIdx;
		try {
			nowUserIdx = ois.readInt();
			if (nowUserIdx != myIdx) {
				loading.setText("[" + userList.get(nowUserIdx).getName() + "]님 카드 선택 중");
				s = new Spinner();
				s.start();
			}
			if (nowUserIdx == nextUserIdx) {
				mySpace.setBackground(new Color(240, 240, 240));
				prevUserSpace.setBackground(new Color(240, 240, 240));
				nextUserSpace.setBackground(new Color(255, 255, 153));
				contentPane.updateUI();
			} else if (nowUserIdx == prevUserIdx) {
				mySpace.setBackground(new Color(240, 240, 240));
				nextUserSpace.setBackground(new Color(240, 240, 240));
				prevUserSpace.setBackground(new Color(255, 255, 153));
				contentPane.updateUI();
			} else if (nowUserIdx == myIdx) {
				nextUserSpace.setBackground(new Color(240, 240, 240));
				prevUserSpace.setBackground(new Color(240, 240, 240));
				mySpace.setBackground(new Color(255, 255, 153));
				contentPane.updateUI();
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
				JButton otherCardBtn = new JButton(getImgIcon("/onecard/img/back.png", 88, 52));
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
				JButton otherCardBtn = new JButton(getImgIcon("/onecard/img/back.png", 88, 52));
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

//	private void drawOtherName(User u) {
//		String userName = u.getName();
//		int uIdx = userList.indexOf(u);
//		if (nextUserIdx == uIdx) {
//			nextUserLabel.setText(userName);
//			nextUserLabel.setForeground(new Color(0, 153, 0));
//		} else if (prevUserIdx == uIdx) {
//			prevUserLabel.setText(userName);
//			prevUserLabel.setForeground(new Color(0, 153, 0));
//		}
//	}

	private void drawOtherName() {
		for (int i = 0; i < userList.size(); i++) {
			String userName = userList.get(i).getName();
			if (nextUserIdx == i) {
				nextUserLabel.setText(userName);
				if (userList.get(i).getPort() != 0) {
					nextUserLabel.setForeground(new Color(0, 153, 0));
				}
			} else if (prevUserIdx == i) {
				prevUserLabel.setText(userName);
				if (userList.get(i).getPort() != 0) {
					prevUserLabel.setForeground(new Color(0, 153, 0));
				}
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

	private void sendTimeOut() {
		try {
			oos.writeInt(203);
			oos.flush();
			contentPane.remove(hourGlass);
			JOptionPane.showMessageDialog(contentPane, "시간초과!\n카드를 받고 차례를 넘깁니다.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void startCountDown() {
		countThread = new Thread() {
			public void run() {

				URL url = this.getClass().getResource("/onecard/img/timelimit.gif");
				ImageIcon myImgIcon = new ImageIcon(url);
				Image originImg = myImgIcon.getImage();
				Image changedImg = originImg.getScaledInstance(40, 40, Image.SCALE_DEFAULT);

				hourGlass = new JLabel(new ImageIcon(changedImg));
				hourGlass.setBounds(155, 63, 40, 40);
				contentPane.add(hourGlass);
				hourGlass.setVisible(true);

				loading.setVisible(false);
				for (int i = 10; i > 0; i--) {
					loading.setText("남은 시간 : " + i + "초");
					loading.setVisible(true);
					try {
						countThread.sleep(1000);
						if (i == 1) {
							loading.setVisible(false);
							sendTimeOut();
						}
					} catch (InterruptedException e) {
						addLog("시간내에 카드 제출함");
						contentPane.remove(hourGlass);
						break;
					}
				}
			}
		};
		countThread.start();
	}

	private void getTurn() {
		try {
			hasTurn = true;
			String text = ois.readUTF();
			addLog("[알림]" + text); // 당신의 차례입니다.
			startCountDown();
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
			myCardNum.setText(myCards.size() + "");
			addLog("내카드 수 출력");
			mySpace.updateUI();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void getCards() {
		// CODE 101
		int amountCards = 0;
		try {
			amountCards = ois.readInt();
			for (int i = 0; i < amountCards; i++) {
				Card c = (Card) ois.readObject();
				myCards.add(c);
				addMyCard(c);
			}
			oos.writeInt(myCards.size());
			oos.flush();
			myCardNum.setText(myCards.size() + "");
			mySpace.updateUI();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
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
		URL searchURL = getClass().getResource("/onecard/img/" + getFileName(c));
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
						countThread.interrupt();
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

		JButton spade = new JButton(getImgIcon("/onecard/img/S.png", 56, 84));
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

		JButton diamond = new JButton(getImgIcon("/onecard/img/D.png", 56, 84));
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

		JButton heart = new JButton(getImgIcon("/onecard/img/H.png", 56, 84));
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

		JButton clover = new JButton(getImgIcon("/onecard/img/C.png", 56, 84));
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