package com.marginallyclever.robotOverlord.swingInterface.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditEvent;

import com.marginallyclever.robotOverlord.RobotOverlord;
import com.marginallyclever.robotOverlord.entity.basicDataTypes.IntEntity;
import com.marginallyclever.robotOverlord.swingInterface.FocusTextField;
import com.marginallyclever.robotOverlord.swingInterface.actions.ActionChangeInt;

/**
 * Panel to alter a number parameter.  There is no way at present to limit the input options (range, step size, etc)
 * @author Dan Royer
 *
 */
public class ViewElementInt extends ViewElement implements DocumentListener, Observer {
	private JTextField field;
	private IntEntity e;
	private ReentrantLock lock = new ReentrantLock();
	
	public ViewElementInt(RobotOverlord ro,IntEntity e) {
		super(ro);
		this.e=e;
		
		e.addObserver(this);
		
		field = new FocusTextField(8);
		field.addActionListener(new AbstractAction() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				conditionalChange();
			}
		});
		field.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {}

			@Override
			public void focusLost(FocusEvent e) {
				conditionalChange();
			}
		});
		field.getDocument().addDocumentListener(this);
		field.setHorizontalAlignment(SwingConstants.RIGHT);
		field.setText(e.get().toString());
		field.addFocusListener(this);

		JLabel label=new JLabel(e.getName(),JLabel.LEADING);
		label.setLabelFor(field);
		
		//this.setBorder(new LineBorder(Color.RED));
		panel.setLayout(new BorderLayout());
		panel.add(label,BorderLayout.LINE_START);
		panel.add(field,BorderLayout.LINE_END);
	}
	
	protected void conditionalChange() {
		int newNumber;
		
		try {
			newNumber = Integer.valueOf(field.getText());
			field.setForeground(UIManager.getColor("Textfield.foreground"));
		} catch(NumberFormatException e1) {
			field.setForeground(Color.RED);
			newNumber = e.get();
		}
		
		if(lock.isLocked()) return;
		lock.lock();

		if(newNumber != e.get()) {
			ro.undoableEditHappened(new UndoableEditEvent(this,new ActionChangeInt(e, newNumber) ) );
		}
		lock.unlock();
	}
	
	protected void validateField() {
		try {
			Integer.valueOf(field.getText());
			field.setForeground(UIManager.getColor("Textfield.foreground"));
		} catch(NumberFormatException e1) {
			field.setForeground(Color.RED);
		}
	}
	
	/**
	 * panel changed, poke entity
	 */
	@Override
	public void changedUpdate(DocumentEvent arg0) {
		validateField();
	}

	@Override
	public void insertUpdate(DocumentEvent arg0) {
		validateField();
	}

	@Override
	public void removeUpdate(DocumentEvent arg0) {
		validateField();
	}
	
	@Override
	public void setReadOnly(boolean arg0) {
		field.setEnabled(!arg0);
	}

	@Override
	public void update(Observable o, Object arg) {
		if(lock.isLocked()) return;
		lock.lock();
		Integer i = (Integer)arg;
		field.setText(i.toString());
		lock.unlock();
	}
}
