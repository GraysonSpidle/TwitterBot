package elements;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.*;
import javax.swing.JFrame;

import main.Main;

public class UI extends JFrame {
	
	JPanel contentPane = new JPanel();
	JButton button = new JButton();
	
	JMenuBar menuBar = new JMenuBar();
	JMenu renew = new JMenu();
	JMenuItem renewAuthToken = new JMenuItem();
	public JTextField field = new JTextField();
	
	JScrollPane pane = new JScrollPane(field);
	
	public UI() {
		
		this.setJMenuBar(menuBar);
		menuBar.add(renew);
		renew.add(renewAuthToken);
		
		renew.setText("Renew");
		renewAuthToken.setText("Authenucation Token");
		
		contentPane.add(pane);
		pane.setSize(this.getSize());
				
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (Main.post) {
					Main.post = false;
					button.setText("Stop");
				}
				else if (!Main.post) {
					Main.post = true;
					button.setText("Start");
				}
			}
			
		});
		button.setText("Start");
		
		this.setContentPane(contentPane);
		this.addWindowListener(new WindowListener() {

			@Override
			public void windowActivated(WindowEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void windowClosed(WindowEvent arg0) {
				Main.exit();
			}

			@Override
			public void windowClosing(WindowEvent arg0) {
				Main.exit();
			}

			@Override
			public void windowDeactivated(WindowEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void windowDeiconified(WindowEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void windowIconified(WindowEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void windowOpened(WindowEvent arg0) {
				// TODO Auto-generated method stub
				
			}
			
		});
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setSize(200, 100);
		this.setLayout(new GridLayout(1, 1));
	}
	
}
