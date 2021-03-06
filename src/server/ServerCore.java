package server;

import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.logging.*;

import javax.swing.*;
import javax.swing.border.BevelBorder;

import common.*;
import net.miginfocom.swing.MigLayout;

@SuppressWarnings("serial")
public abstract class ServerCore extends JFrame {

	public static Logger logger = Logger.getLogger("SpaceDebris");
	public JTextArea taOut;
	public JTextField taIn; //fixed this input enter error needed textfield not textArea
	public boolean taOutChange;
	public ArrayList<String> prevInput;

	protected int currentID = -1;
	private SqlHelper sqlhelper;
	
	JFrame window;
	Dimension windDim;
	JScrollBar vert;
	JScrollPane scroll;
	JButton btnSend;
	String serverState = "init";
	CommandListener cmdList;
	Keyboard kbd;

	KeyListener upDownListener = new KeyListener(){

		@Override
		public void keyPressed(KeyEvent arg0) {
			// TODO Auto-generated method stub
			if(arg0.getKeyCode() == KeyEvent.VK_UP){
				if(currentID < (prevInput.size() -1)){
					currentID++;
					if(currentID > prevInput.size()){
						currentID = 0;
					}
					taIn.setText(prevInput.get(currentID));
				}
			}
			if(arg0.getKeyCode() == KeyEvent.VK_DOWN){
				if(currentID > -1){
					currentID--;
					if(currentID == -1){
						taIn.setText("");
					}else{
						taIn.setText(prevInput.get(currentID));
					}
				}
			}
		}

		@Override
		public void keyReleased(KeyEvent arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void keyTyped(KeyEvent arg0) {
			// TODO Auto-generated method stub
			
		}
		
	};
	
	public void run() {
		try {
			init();
			serverState = "setup";
			mainLoop();
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
            logger.log(Level.INFO, "Saving.");
			save();
            logger.log(Level.INFO, "Quitting.");
			System.exit(-1);
		}
	}

	private void save() {
		
	}
	
	public void init() {
        logger.log(Level.INFO, "Initialising main variables.");
		
        taOutChange = false;
        sqlhelper = new SqlHelper();
        prevInput = new ArrayList<String>();
		initWindow();
		getDbConnection();
		
		//variable initialization
		cmdList = new CommandListener();
		taOut.append("Commands Enabled. \n"
				+ "CommandListener listening.\n");
		
		dbConnect();
	}

	public void initWindow() {
        logger.log(Level.INFO, "Initialising window.");
		double scale = 1;
		//JFrame setup
		window = new JFrame();
		
		window.getContentPane().setBackground(Color.WHITE);
		windDim = new Dimension((int)(667/scale),(int)(332/scale));
		window.setMinimumSize(windDim);
		window.getContentPane().setLayout(new MigLayout("", "[grow][]", "[grow][]"));
		window.setTitle("Space Debris Server"); // Add a window Title Caption
		
		//Text output setup
		taOut = new JTextArea();
		taOut.setRequestFocusEnabled(false);
		taOut.setFocusTraversalKeysEnabled(false);
		taOut.setLineWrap(true);
		taOut.setEditable(false);
		window.getContentPane().add(taOut, "cell 0 0 2 1,grow");
		
		//scroll bar setup
		scroll = new JScrollPane(taOut);
		scroll.setFocusTraversalKeysEnabled(false);
		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setSize(new Dimension(50,50));
		vert = scroll.getVerticalScrollBar();
		scroll.setViewportView(taOut);
		window.getContentPane().add(scroll, "cell 0 0 2 1,grow");
		
		//text input setup
		taIn = new JTextField();
		taIn.setBackground(SystemColor.inactiveCaptionBorder);
		taOut.setRequestFocusEnabled(true);
		taIn.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		window.getContentPane().add(taIn, "cell 0 1, grow,aligny bottom");
		taIn.addKeyListener(upDownListener);
		
		//send button setup
		btnSend = new JButton("Send");
		window.getRootPane().setDefaultButton(btnSend);
		btnSend.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				btnSndList();
			}
		});
		window.getContentPane().add(btnSend, "cell 1 1");
		
		//window positioning
		int windowX = getGraphicsConfiguration().getDevice().getDisplayMode().getWidth();// get the monitor's width
		int windowY = getGraphicsConfiguration().getDevice().getDisplayMode().getHeight(); // get the monitor's height
		windowX = (int) (windowX * 0.5) - (int) (windDim.width * 0.5); // divide the screen and background image width by 2
		windowY = (int) (windowY * 0.5) - (int) (windDim.height * 0.5); // divide the screen and background image height by 2
		window.setBounds(windowX, windowY, windDim.width, windDim.height);
		
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		//show window
		window.setVisible(true);
	}

	public void mainLoop(){
		serverState = "running";
		while(!serverState.equalsIgnoreCase("quitting")){
			
			//any other code
			MoreCalls();

			//draw and finish
//			window.repaint();
			
			try{
				Thread.sleep(24);
			}catch(Exception ex){
				print(ex.getMessage());
			}
		}
	}
	
	static void print(String x){
		System.out.println(x);
	}
	void print(int x){
		System.out.println(x);
	}
	void print(double x){
		System.out.println(x);
	}
	void print(char x){
		System.out.println(x);
	}
	void print(boolean x){
		System.out.println(x);
	}
	
	private void btnSndList() {
		/* 
		 * we dont want to prefix anything on this string, or the commands wont work...
		 * we'll leave that for the "say" command 
		 * "Admin: " + 
		 */ 
		String txtIn = taIn.getText();
		sqlhelper.insert("server/Server", "INPUTLOG (HISTORY) ","'"+ txtIn +"'");
		taOutUpdate(txtIn);
	}
	
	public void taOutUpdate(String stp){
		//new command listener implementation
		taOut.append(stp+"\n");
		cmdList.getPrevInput(prevInput);
		if(cmdList.isEnabled()){
			if(stp.toLowerCase().startsWith("quit") || stp.toLowerCase().startsWith("exit")){
				serverState="quitting";
				taOut.append("Quitting!");
			}else if(stp.toLowerCase().startsWith("clear") || stp.toLowerCase().startsWith("cls")){
				taOut.setText("");
				taOut.append("Screen Cleared. \n");
			}else{
				taOut.append(cmdList.handleCommand(stp)+"\n");// handleCommand returns command's string
			}
		}else{
			if(stp.toLowerCase().startsWith("tenable")){
				taOut.append(cmdList.handleCommand(stp)+"\n");// handleCommand returns command's string
			}else{
				taOut.append(cmdList.handleCommand("say "+stp)+"\n");// handleCommand returns command's string
			}
		}
		taOut.setCaretPosition(taOut.getDocument().getLength());
		taIn.setText("");
		getDbConnection();
	}
	
	public void writeLog(){
		
	}
	
	public abstract void controls();
	
	public abstract void MoreCalls();

	public abstract void dbConnect();

	public abstract void getDbConnection();
	
	public abstract void getAllHistory() throws SQLException;
}
