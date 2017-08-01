package com.marginallyclever.robotOverlord.makelangeloRobot.settings;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.marginallyclever.robotOverlord.RobotOverlord;
import com.marginallyclever.robotOverlord.Translator;
import com.marginallyclever.robotOverlord.commands.UserCommandSelectNumber;
import com.marginallyclever.robotOverlord.makelangeloRobot.MakelangeloRobot;

public class PanelAdjustPaper
extends JPanel
implements ActionListener, PropertyChangeListener, ChangeListener {
	/**
	 * auto-generated serialVersionUID
	 */
	private static final long serialVersionUID = 519519372661103125L;

	protected MakelangeloRobot robot;

	private JComboBox<String> paperSizes;
	private UserCommandSelectNumber pw, ph;
	private JCheckBox isLandscape;
	private boolean beingModified;
	
	/**
	 * Convenience class for managing paper sizes. 
	 * @author Dan Royer
	 *
	 */
	class PaperSize {
		public String name;
		public int width;
		public int height;
		
		PaperSize(String name,int width,int height) {
			this.name = name;
			this.width = width;
			this.height = height;
		}
		
		public String toDescription() {
			return name+" ("+width+" x "+height+")";
		}
	}
	
	private PaperSize commonPaperSizes [] = {
		new PaperSize("4A0",1682,2378),
		new PaperSize("2A0",1189,1682),
		new PaperSize("A0",841,1189),
		new PaperSize("A1",594,841),
		new PaperSize("A2",420,594),
		new PaperSize("A3",297,420),
		new PaperSize("A4",210,297),
		new PaperSize("A5",148,210),
		new PaperSize("A6",105,148),
		new PaperSize("A7",74,105),
		new PaperSize("US Half Letter",140,216),
		new PaperSize("US Letter",216,279),
		new PaperSize("US Legal",216,356),
		new PaperSize("US Junior Legal",127,203),
		new PaperSize("US Ledger / Tabloid",279,432),
		new PaperSize("ANSI A",216,279),
		new PaperSize("ANSI B",279,432),
		new PaperSize("ANSI C",432,559),
		new PaperSize("ANSI D",559,864),
		new PaperSize("ANSI E",864,1118),
		new PaperSize("Arch A",229,305),
		new PaperSize("Arch B",305,457),
		new PaperSize("Arch C",457,610),
		new PaperSize("Arch D",610,914),
		new PaperSize("Arch E",914,1219),
		new PaperSize("Arch E1",762,1067)
		};

	private JSlider paperMargin;

	public PanelAdjustPaper(RobotOverlord gui, MakelangeloRobot robot) {
		this.robot = robot;
		
		beingModified=false;
		
	    this.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
	    this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
	    
	    
		JPanel p = new JPanel(new GridBagLayout());
		this.add(p);
		int y=0;

		GridBagConstraints c = new GridBagConstraints();
		GridBagConstraints d = new GridBagConstraints();
		d.anchor = GridBagConstraints.WEST;
		c.anchor = GridBagConstraints.EAST;
		c.ipadx=5;
		c.ipady=2;

		// common paper sizes
		String[] commonPaperNames = new String[commonPaperSizes.length+1];
		commonPaperNames[0]="---";
		int i;
		for(i=0;i<commonPaperSizes.length;++i) {
			commonPaperNames[i+1] = new String(commonPaperSizes[i].toDescription());
		}
		
		paperSizes = new JComboBox<>(commonPaperNames);
		c.gridx=0;  c.gridy=y;  p.add(new JLabel(Translator.get("PaperSize")),c);
		d.gridx=1;  d.gridy=y;  d.gridwidth=2;  p.add(paperSizes,d);
		y++;

		// landscape checkbox
		isLandscape = new JCheckBox(Translator.get("isLandscape"), false);
		c.fill = GridBagConstraints.HORIZONTAL;
		d.gridx=0;  d.gridy=y;  d.gridwidth=3;  p.add(isLandscape,d);
		y++;

		// manual paper size settings
		c.gridwidth=3;
		d.gridwidth=1;
		d.weightx=0;
		pw = new UserCommandSelectNumber(gui,Translator.get("Width"),0);
		c.gridx=0;  c.gridy=y;  p.add(pw,c);
		d.gridx=3;  d.gridy=y;  p.add(new JLabel(Translator.get("Millimeters")),d);
		y++;
		
		ph = new UserCommandSelectNumber(gui,Translator.get("Height"),0);
		c.gridx=0;  c.gridy=y;  p.add(ph,c);
		d.gridx=3;  d.gridy=y;  p.add(new JLabel(Translator.get("Millimeters")),d);
		y++;

		Dimension s = ph.getPreferredSize();
		s.width = 80;
		pw.setPreferredSize(s);
		ph.setPreferredSize(s);
		
		// paper margin
		JPanel marginPanel = new JPanel(new GridBagLayout());
		GridBagConstraints pm = new GridBagConstraints();
		paperMargin = new JSlider(JSlider.HORIZONTAL, 0, 50, 100 - (int) (robot.getSettings().getPaperMargin() * 100));
		paperMargin.setMajorTickSpacing(10);
		paperMargin.setMinorTickSpacing(5);
		paperMargin.setPaintTicks(false);
		paperMargin.setPaintLabels(true);
		
		pm.anchor = GridBagConstraints.WEST;
		JLabel marginLabel = new JLabel(Translator.get("PaperMargin"));
		marginLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
		//marginLabel.setBorder(BorderFactory.createLineBorder(new Color(0,0,0), 1));
		marginPanel.add(marginLabel,pm);
		pm.gridy=0;
		pm.gridwidth=2;
		pm.anchor = GridBagConstraints.EAST;
		//paperMargin.setBorder(BorderFactory.createLineBorder(new Color(0,0,0), 1));
		marginPanel.add(paperMargin,pm);

		this.add(marginPanel, c);

		paperSizes.addActionListener(this);
		isLandscape.addActionListener(this);
		pw.addPropertyChangeListener(this);
		ph.addPropertyChangeListener(this);
		paperMargin.addChangeListener(this);
		
		updateValues();
	}

	
	public void stateChanged(ChangeEvent e) {}


	/**
	 * Must match commonPaperSizes
	 * @param pw paper width
	 * @param ph paper height
	 * @return the index into commonPaperSizes of the current choice.  0 if no match found. 
	 */
	public int getCurrentPaperSizeChoice(double pw,double ph) {
		int i;
		for(i=0;i<commonPaperSizes.length;++i) {
			if(pw == commonPaperSizes[i].width && 
				ph == commonPaperSizes[i].height)
				return i+1;
				
		}

		return 0;
	}

	public void propertyChange(PropertyChangeEvent  e) {
		if(beingModified) return;

		beingModified=true;
		if( e.getSource() == pw || e.getSource() == ph ) {
			double w=0;
			double h=0;
			try {
				w = ((Number)pw.getValue()).doubleValue();
				h = ((Number)ph.getValue()).doubleValue();
			} catch(Exception err) {
				err.getMessage();
			}

			int i = getCurrentPaperSizeChoice( h, w );
			if(i!=0) {
				isLandscape.setSelected(true);
			} else {
				i = getCurrentPaperSizeChoice( w, h );
				isLandscape.setSelected(false);
			}
			paperSizes.setSelectedIndex(i);
		}
		beingModified=false;
	}


	@Override
	public void actionPerformed(ActionEvent e) {
		Object subject = e.getSource();
		if(beingModified) return;
		beingModified=true;
		
		if(subject == paperSizes) {
			final int selectedIndex = paperSizes.getSelectedIndex();
			if(selectedIndex!= 0) {
				String str = paperSizes.getItemAt(selectedIndex);
				String sw = str.substring(str.indexOf('(')+1, str.indexOf('x')).trim();
				String sh = str.substring(str.indexOf('x')+1, str.indexOf(')')).trim();
				if(isLandscape.isSelected()) {
					String temp = sw;
					sw = sh;
					sh = temp;
				}
				pw.setValue(Float.parseFloat(sw));
				ph.setValue(Float.parseFloat(sh));
			}
		}
		if( subject == isLandscape ) {
			float w = pw.getValue();
			float h = ph.getValue();
			pw.setValue(h);
			ph.setValue(w);
		}
		
		beingModified=false;
	}

	public void updateValues() {
		if(robot==null) return;
		
		double w = robot.getSettings().getPaperWidth()*10;
		double h = robot.getSettings().getPaperHeight()*10;

		beingModified=true;
		
		int i = getCurrentPaperSizeChoice( h, w );
		if(i!=0) {
			isLandscape.setSelected(true);
		} else {
			i = getCurrentPaperSizeChoice( w, h );
			isLandscape.setSelected(false);
		}
		paperSizes.setSelectedIndex(i);
		pw.setValue((float)w);
		ph.setValue((float)h);

		beingModified=false;
	}
	
	public void save() {
		double pwf = pw.getValue() / 10.0;
		double phf = ph.getValue() / 10.0;
		
		boolean data_is_sane=true;
		if( pwf<=0 ) data_is_sane=false;
		if( phf<=0 ) data_is_sane=false;

		if (data_is_sane) {
			MakelangeloRobotSettings s = robot.getSettings();
			s.setPaperSize(pwf,phf);

			double pm = (100 - paperMargin.getValue()) * 0.01;
			s.setPaperMargin(pm);
		}
	}
}
