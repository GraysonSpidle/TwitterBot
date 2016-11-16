package elements;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

public class EnterPinUI extends JFrame {
	
	private JPanel contentPane = new JPanel();
	private JTextField field = new JTextField();
	private JButton button = new JButton();
	public long PIN = 0;
	
	public EnterPinUI() {
		
		this.setResizable(false);
		this.setSize(250, 400);
		this.setTitle("Input the PIN");
		
		this.setLayout(new GridLayout(2, 1));
		
		button.setText("Submit");
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				PIN = Long.parseLong(field.getText());
				AuthenucationTest.pinReciever.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "Stuff"));
			}
			
		});
		
		
		contentPane.add(field);
		contentPane.add(button);
		this.setContentPane(contentPane);
		this.setLayout(new GridLayout(2, 1));
		
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
	}
	
}
