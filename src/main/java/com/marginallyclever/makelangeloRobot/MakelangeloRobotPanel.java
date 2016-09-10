package com.marginallyclever.makelangeloRobot;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.marginallyclever.robotOverlord.CollapsiblePanel;
import com.marginallyclever.makelangeloRobot.ImageManipulator;
import com.marginallyclever.robotOverlord.Log;
import com.marginallyclever.robotOverlord.RobotOverlord;
import com.marginallyclever.robotOverlord.SoundSystem;
import com.marginallyclever.robotOverlord.Translator;
import com.marginallyclever.robotOverlord.actions.ActionSelectNumber;
import com.marginallyclever.makelangeloRobot.generators.ImageGenerator;
import com.marginallyclever.makelangeloRobot.loadAndSave.LoadAndSaveFileType;
import com.marginallyclever.makelangeloRobot.loadAndSave.LoadAndSaveGCode;
import com.marginallyclever.makelangeloRobot.settings.MakelangeloSettingsDialog;

/**
 * Control panel for a Makelangelo robot
 *
 * @author dan royer
 * @author Peter Colapietro
 * @since 7.1.4
 */
public class MakelangeloRobotPanel extends JPanel implements ActionListener, ItemListener {
	/**
	 *
	 */
	private static final long serialVersionUID = -4703402918904039337L;

	// god objects ?
	protected MakelangeloRobot robot;
	protected RobotOverlord gui;
	
	// machine options
	protected String lastFileIn = "";
	protected String lastFileOut = "";
	protected int generatorChoice=0;
	
	private String[] machineConfigurations;
	private JComboBox<String> machineChoices;
	private JButton openConfig;
	private JPanel machineNumberPanel;
	private JButton buttonOpenFile, buttonReopenFile, buttonNewFile, buttonGenerate, buttonSaveFile;
	protected JButton buttonStart, buttonStartAt, buttonPause, buttonHalt;

	// driving controls
	private JButton down100,down10,down1,up1,up10,up100;
	private JButton left100,left10,left1,right1,right10,right100;
	private JButton goHome,findHome,setHome;
	private JButton goPaperBorder,penUp,penDown;

	// speed
	private ActionSelectNumber feedRateTxt;
	private JButton setFeedRate;
	private JButton toggleEngagedMotor;

	private JButton about;
	
	public StatusBar statusBar;
	
	
	public MakelangeloRobotPanel(RobotOverlord gui, MakelangeloRobot robot) {
		this.gui = gui;
		this.robot = robot;
		
		this.setBorder(BorderFactory.createEmptyBorder());
		this.setLayout(new GridBagLayout());

		GridBagConstraints con1 = new GridBagConstraints();
		con1.gridx = 0;
		con1.gridy = 0;
		con1.weightx = 1;
		con1.weighty = 0;
		con1.fill = GridBagConstraints.HORIZONTAL;
		con1.anchor = GridBagConstraints.NORTHWEST;
		
		// settings
		machineNumberPanel = new JPanel(new GridBagLayout());
		updateMachineNumberPanel();
		this.add(machineNumberPanel, con1);
		con1.gridy++;

		this.add(createDriveControls(),con1);
		con1.gridy++;
		this.add(createCreativeControlPanel(), con1);
		con1.gridy++;
		this.add(createAnimationPanel(),con1);
		con1.gridy++;
		this.add(statusBar = new StatusBar(), con1);
		con1.gridy++;
		
		// lastly, set the button states
		updateButtonAccess();
	}


	protected List<LoadAndSaveFileType> loadFileSavers() {
		return new ArrayList<LoadAndSaveFileType>();
	}


	public JButton createTightJButton(String label) {
		JButton b = new JButton(label);
		b.setMargin(new Insets(0,0,0,0));
		b.setPreferredSize(new Dimension(60,20));
		b.addActionListener(this);
		return b;
	}


	public JButton createNarrowJButton(String label) {
		JButton b = new JButton(label);
		b.setMargin(new Insets(0,0,0,0));
		b.setPreferredSize(new Dimension(40,20));
		b.addActionListener(this);
		return b;
	}

	
	protected void closeConnection() {
		robot.setConnection(null);
	}
	
	protected void openConnection() {
		JPanel connectionList = new JPanel(new GridLayout(0, 1));
		connectionList.add(new JLabel(Translator.get("MenuConnect")));
		
		GridBagConstraints con1 = new GridBagConstraints();
		con1.gridx=0;
		con1.gridy=0;
		con1.weightx=1;
		con1.weighty=1;
		con1.fill=GridBagConstraints.HORIZONTAL;
		con1.anchor=GridBagConstraints.NORTH;

		JComboBox<String> connectionComboBox = new JComboBox<String>();
        connectionComboBox.addItemListener(this);
        connectionList.removeAll();
        connectionList.add(connectionComboBox);
	    
	    String recentConnection = "";
	    if(robot.getConnection()!=null) {
	    	recentConnection = robot.getConnection().getRecentConnection();
	    }

	    if(gui.getConnectionManager()!=null) {
			String [] portsDetected = gui.getConnectionManager().listConnections();
			int i;
		    for(i=0;i<portsDetected.length;++i) {
		    	connectionComboBox.addItem(portsDetected[i]);
		    	if(recentConnection.equals(portsDetected[i])) {
		    		connectionComboBox.setSelectedIndex(i+1);
		    	}
		    }
	    }
        
		int result = JOptionPane.showConfirmDialog(this.getRootPane(), connectionList, getName(), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (result == JOptionPane.OK_OPTION) {
			String connectionName = connectionComboBox.getItemAt(connectionComboBox.getSelectedIndex());
			robot.setConnection( gui.getConnectionManager().openConnection(connectionName) );
			//updateMachineNumberPanel();
			//updateButtonAccess();
		}
	}
	

	@Override
	public void itemStateChanged(ItemEvent e) {
		Object subject = e.getSource();

		if(subject == machineChoices && e.getStateChange()==ItemEvent.SELECTED) {
			updateMachineChoice();
		}
	}
	
	protected void updateMachineChoice() {
		int selectedIndex = machineChoices.getSelectedIndex();
		long newUID = Long.parseLong(machineChoices.getItemAt(selectedIndex));
		robot.getSettings().loadConfig(newUID);
	}

	
	private JPanel createAnimationPanel() {
		CollapsiblePanel animationPanel = new CollapsiblePanel(Translator.get("MenuAnimate"));
		JPanel drivePanel = animationPanel.getContentPane();
		
		drivePanel.setLayout(new GridLayout(4,1));
		buttonStart = new JButton(Translator.get("Start"));
		buttonStartAt = new JButton(Translator.get("StartAtLine"));
		buttonPause = new JButton(Translator.get("Pause"));
		buttonHalt = new JButton(Translator.get("Halt"));
		buttonStart.addActionListener(this);
		buttonStartAt.addActionListener(this);
		buttonPause.addActionListener(this);
		buttonHalt.addActionListener(this);

		drivePanel.add(buttonStart);
		drivePanel.add(buttonStartAt);
		drivePanel.add(buttonPause);
		drivePanel.add(buttonHalt);
		
		return animationPanel;
	}
	

	private JPanel createCreativeControlPanel() {
		CollapsiblePanel creativeControlPanel = new CollapsiblePanel(Translator.get("MenuCreativeControl"));
		JPanel panel = creativeControlPanel.getContentPane();
		
		GridBagConstraints con1 = new GridBagConstraints();
		con1.gridx=0;
		con1.gridy=0;
		con1.weightx=1;
		con1.weighty=1;
		con1.fill=GridBagConstraints.HORIZONTAL;
		con1.anchor=GridBagConstraints.NORTH;

		buttonNewFile = new JButton(Translator.get("MenuNewFile"));
		buttonNewFile.addActionListener(this);
		panel.add(buttonNewFile, con1);
		con1.gridy++;

		buttonOpenFile = new JButton(Translator.get("MenuOpenFile"));
		buttonOpenFile.addActionListener(this);
		panel.add(buttonOpenFile, con1);
		con1.gridy++;
		
		buttonReopenFile = new JButton(Translator.get("MenuReopenFile"));
		buttonReopenFile.addActionListener(this);
		panel.add(buttonReopenFile, con1);
		con1.gridy++;

		buttonGenerate = new JButton(Translator.get("MenuGenerate"));
		buttonGenerate.addActionListener(this);
		panel.add(buttonGenerate, con1);
		con1.gridy++;

		buttonSaveFile = new JButton(Translator.get("MenuSaveGCODEAs"));
		buttonSaveFile.addActionListener(this);
		panel.add(buttonSaveFile, con1);
		con1.gridy++;
		
		return creativeControlPanel;
	}
	
	
	private JPanel createDriveControls() {
		CollapsiblePanel drivePanel = new CollapsiblePanel(Translator.get("MenuDriveControls"));
		JPanel mainPanel = drivePanel.getContentPane();
		mainPanel.setLayout(new GridBagLayout());
		final GridBagConstraints cMain = new GridBagConstraints();
		cMain.fill=GridBagConstraints.HORIZONTAL;
		cMain.anchor=GridBagConstraints.NORTH;
		cMain.gridx=0;
		cMain.gridy=0;
		{
			// axis driving
			JPanel axisControl = new JPanel(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			mainPanel.add(axisControl,cMain);
			cMain.gridy++;
	
			setHome = createTightJButton(Translator.get("SetHome"));
		    setHome.setPreferredSize(new Dimension(100,20));
	
			down100 = createTightJButton("-100");
			down10 = createTightJButton("-10");
			down1 = createTightJButton("-1");
	
			up1 = createTightJButton("1");
			up10 = createTightJButton("10");
			up100 = createTightJButton("100");
	
			left100 = createNarrowJButton("-100");
			left10 = createNarrowJButton("-10");
			left1 = createNarrowJButton("-1");
			
			right1 = createNarrowJButton("1");
			right10 = createNarrowJButton("10");
			right100 = createNarrowJButton("100");
	
			c.fill=GridBagConstraints.BOTH;
			c.gridx=3;  c.gridy=6;  axisControl.add(down100,c);
			c.gridx=3;  c.gridy=5;  axisControl.add(down10,c);
			c.gridx=3;  c.gridy=4;  axisControl.add(down1,c);
			c.gridx=3;  c.gridy=3;  axisControl.add(setHome,c);
			c.gridx=3;  c.gridy=2;  axisControl.add(up1,c);
			c.gridx=3;  c.gridy=1;  axisControl.add(up10,c);
			c.gridx=3;  c.gridy=0;  axisControl.add(up100,c);
	
			c.gridx=0;  c.gridy=3;  axisControl.add(left100,c);
			c.gridx=1;  c.gridy=3;  axisControl.add(left10,c);
			c.gridx=2;  c.gridy=3;  axisControl.add(left1,c);
			c.gridx=4;  c.gridy=3;  axisControl.add(right1,c);
			c.gridx=5;  c.gridy=3;  axisControl.add(right10,c);
			c.gridx=6;  c.gridy=3;  axisControl.add(right100,c);
		}
		{
			// feed rate
			JPanel feedRateControl = new JPanel();
			mainPanel.add(feedRateControl,cMain);
			cMain.gridy++;
			feedRateControl.setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			feedRateTxt = new ActionSelectNumber(gui,Translator.get("Speed"),(float)robot.getSettings().getCurrentFeedRate());
			feedRateTxt.setPreferredSize(new Dimension(100,20));
			setFeedRate = new JButton(Translator.get("Set"));
			setFeedRate.addActionListener(this);

			c.gridx=4;  c.gridy=0;  feedRateControl.add(feedRateTxt,c);
			c.gridx=5;  c.gridy=0;  feedRateControl.add(new JLabel(Translator.get("Rate")),c);
			c.gridx=6;  c.gridy=0;  feedRateControl.add(setFeedRate,c);
		}
		{
			// quick drive to corners
			JPanel quickDriveOptions = new JPanel(new GridBagLayout());
			cMain.insets = new Insets(10,0,0,0);
			mainPanel.add(quickDriveOptions,cMain);
			
			GridBagConstraints con1 = new GridBagConstraints();
			con1.gridx=0;
			con1.gridy=0;
			con1.weightx=1;
			con1.weighty=1;
			con1.fill=GridBagConstraints.HORIZONTAL;
			con1.anchor=GridBagConstraints.NORTH;
	
			goPaperBorder = new JButton(Translator.get("GoPaperBorder"));
			goPaperBorder.setPreferredSize(new Dimension(80,20));
			
			penUp    = new JButton(Translator.get("PenUp"));		penUp   .setPreferredSize(new Dimension(100,20));
			penDown  = new JButton(Translator.get("PenDown"));		penDown .setPreferredSize(new Dimension(100,20));
			goHome   = new JButton(Translator.get("GoHome"));		goHome  .setPreferredSize(new Dimension(100,20));
			findHome = new JButton(Translator.get("FindHome"));		findHome.setPreferredSize(new Dimension(100,20));
			toggleEngagedMotor = new JButton(Translator.get("DisengageMotors"));
			toggleEngagedMotor.setPreferredSize(new Dimension(100,20));
			
			GridBagConstraints c = new GridBagConstraints();
			c.anchor=GridBagConstraints.WEST;
			c.fill=GridBagConstraints.BOTH;
			
			c.gridx=2;  c.gridy=0;  quickDriveOptions.add(goPaperBorder,c);
			c.gridx=2;  c.gridy=1;  quickDriveOptions.add(toggleEngagedMotor,c);
			
			c.gridx=4;  c.gridy=0;  quickDriveOptions.add(penUp,c);
			c.gridx=4;  c.gridy=1;  quickDriveOptions.add(penDown,c);

			c.gridx=3;  c.gridy=0;  quickDriveOptions.add(goHome,c);
			c.gridx=3;  c.gridy=1;  quickDriveOptions.add(findHome,c);
			
			goPaperBorder.addActionListener(this);
			penUp.addActionListener(this);
			penDown.addActionListener(this);
			goHome.addActionListener(this);
			findHome.addActionListener(this);
			toggleEngagedMotor.addActionListener(this);
	
			con1.weightx=1;
			con1.weighty=0;
			con1.fill=GridBagConstraints.HORIZONTAL;
			con1.anchor=GridBagConstraints.NORTHWEST;
	
			con1.gridy++;
		}

		about = new JButton(Translator.get("About this robot"));
		mainPanel.add(about,cMain);
		about.addActionListener(this);
		
		return drivePanel;
	}

	
	/**
	 * Refresh the list of available machine settings.  If we are connected to a machine, select that setting and disable the selection.
	 */
	public void updateMachineNumberPanel() {
		machineNumberPanel.removeAll();
		machineConfigurations = robot.getSettings().getKnownMachineNames();
		GridBagConstraints cMachine = new GridBagConstraints();
		cMachine.fill= GridBagConstraints.HORIZONTAL;
		cMachine.anchor = GridBagConstraints.CENTER;
		cMachine.gridx=0;
		cMachine.gridy=0;
		
		if( machineConfigurations.length>0 ) {
			machineChoices = new JComboBox<>(machineConfigurations);
			JLabel label = new JLabel(Translator.get("MachineNumber"));
			cMachine.insets = new Insets(0,0,0,5);
			machineNumberPanel.add(label,cMachine);
			cMachine.insets = new Insets(0,0,0,0);

			cMachine.gridx++;
			machineNumberPanel.add(machineChoices,cMachine);
			cMachine.gridx++;
			
			// if we're connected to a confirmed machine, don't let the user change the number panel or settings could get...weird.
			boolean state=false;
			if( robot.getConnection() == null ) state=true;
			else if( robot.getConnection().isOpen() == false ) state=true;
			else if( robot.isPortConfirmed() == false ) state=true;
			
			machineChoices.setEnabled( state );
			machineChoices.addItemListener(this);

			int index = robot.getSettings().getKnownMachineIndex();
			if( index<0 ) index=0;
			machineChoices.setSelectedIndex(index);

			// force the GUI to load the correct initial choice.
			updateMachineChoice();
		}

		openConfig = new JButton(Translator.get("configureMachine"));
		openConfig.addActionListener(this);
		openConfig.setPreferredSize(openConfig.getPreferredSize());
		machineNumberPanel.add(openConfig,cMachine);
		cMachine.gridx++;
	}
	
	// The user has done something. respond to it.
	public void actionPerformed(ActionEvent e) {
		Object subject = e.getSource();

		if (subject == openConfig) {
			MakelangeloSettingsDialog m = new MakelangeloSettingsDialog(gui, robot);
			m.run();
		}
		else if (subject == buttonNewFile) newFile();
		else if (subject == buttonOpenFile) loadFileDialog();
		else if (subject == buttonReopenFile) reopenFile();
		else if (subject == buttonGenerate) generateImage();
		else if (subject == buttonSaveFile) saveFileDialog();
		else if (subject == buttonStart) robot.startAt(0);
		else if (subject == buttonStartAt) {
			int lineNumber = getStartingLineNumber();
			if (lineNumber != -1) {
				robot.startAt(lineNumber);
			}
			return;
		}
		else if (subject == buttonPause) {
			// toggle pause
			if (robot.isPaused() == true) {
				buttonPause.setText(Translator.get("Pause"));
				robot.unPause();
				robot.sendFileCommand();
			} else {
				buttonPause.setText(Translator.get("Unpause"));
				robot.pause();
			}
			return;
		}
		else if (subject == buttonHalt) robot.halt();		
		else if (subject == goHome  ) robot.goHome();
		else if (subject == findHome) robot.findHome();
		else if (subject == setHome ) {
			robot.setHome();
			updateButtonAccess();
		}
		else if (subject == goPaperBorder) {
			robot.movePenToEdgeTop();
			robot.movePenToEdgeRight();
			robot.movePenToEdgeBottom();
			robot.movePenToEdgeLeft();
			robot.movePenToEdgeTop();
			robot.goHome();
		}
		else if (subject == penUp   ) robot.raisePen();
		else if (subject == penDown ) robot.lowerPen();
		else if (subject == setFeedRate) {
			// get the feed rate
			robot.setCurrentFeedRate(feedRateTxt.getValue());
			// update the input field				
			feedRateTxt.setValue((float)robot.getCurrentFeedRate());
		} else if (subject == toggleEngagedMotor) {
			if(robot.areMotorsEngaged() ) {
				disengageMotors();
			} else {
				engageMotors();
			}
		} else if ( subject == about ) {
			robot.doAbout();
		} else {
			float dx=0;
			float dy=0;
			
			if (subject == down100) dy = -100;
			if (subject == down10) dy = -10;
			if (subject == down1) dy = -1;
			if (subject == up100) dy = 100;
			if (subject == up10) dy = 10;
			if (subject == up1) dy = 1;
			
			if (subject == left100) dx = -100;
			if (subject == left10) dx = -10;
			if (subject == left1) dx = -1;
			if (subject == right100) dx = 100;
			if (subject == right10) dx = 10;
			if (subject == right1) dx = 1;

			if(dx!=0 || dy!=0) robot.movePenRelative(dx,dy);
		}
	}

	protected void disengageMotors() {
		robot.disengageMotors();
	}

	protected void engageMotors() {
		robot.engageMotors();
	}
	
	public void motorsHaveBeenDisengaged() {
		toggleEngagedMotor.setText(Translator.get("EngageMotors"));
	}
	public void motorsHaveBeenEngaged() {
		toggleEngagedMotor.setText(Translator.get("DisengageMotors"));
	}
	
	/**
	 * open a dialog to ask for the line number.
	 *
	 * @return <code>lineNumber</code> greater than or equal to zero if user hits ok.
	 */
	private int getStartingLineNumber() {
		final JPanel panel = new JPanel(new GridBagLayout());
		final JTextField starting_line = new JTextField("0", 8);
		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = 2;
		c.gridx = 0;
		c.gridy = 0;
		panel.add(new JLabel(Translator.get("StartAtLine")), c);
		c.gridwidth = 2;
		c.gridx = 2;
		c.gridy = 0;
		panel.add(starting_line, c);

		int result = JOptionPane.showConfirmDialog(null, panel, Translator.get("StartAt"), JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE);
		if (result == JOptionPane.OK_OPTION) {
			int lineNumber;
			try {
				lineNumber = Integer.decode(starting_line.getText());
			} catch (Exception e) {
				lineNumber = -1;
			}

			return lineNumber;
		}
		return -1;
	}

	/**
	 * the moment a robot is confirmed to have connected 
	 */
	public void onConnect() {
		updateMachineNumberPanel();
		updateButtonAccess();
		engageMotors();
	}
	
	public void updateButtonAccess() {
		boolean isConfirmed=false;
		boolean isRunning=false;
		boolean hasSetHome=false;
		
		if(robot!=null) {
			isConfirmed = robot.isPortConfirmed();
			isRunning = robot.isRunning();
			hasSetHome = robot.hasSetHome();
		}
		
		if (buttonGenerate != null)
			buttonGenerate.setEnabled(!isRunning);

		openConfig.setEnabled(!isRunning);

		buttonStart.setEnabled(isConfirmed && hasSetHome && !isRunning);
		buttonStartAt.setEnabled(isConfirmed && hasSetHome && !isRunning);
		buttonPause.setEnabled(isConfirmed && isRunning);
		buttonHalt.setEnabled(isConfirmed && isRunning);

		if (!isConfirmed) {
			buttonPause.setText(Translator.get("Pause"));
		}

		toggleEngagedMotor.setEnabled(isConfirmed && !isRunning);
		buttonNewFile.setEnabled(!isRunning);
		buttonOpenFile.setEnabled(!isRunning);
		buttonReopenFile.setEnabled(!isRunning && (!lastFileIn.isEmpty()));
		buttonGenerate.setEnabled(!isRunning);

		down100.setEnabled(isConfirmed && !isRunning);
		down10.setEnabled(isConfirmed && !isRunning);
		down1.setEnabled(isConfirmed && !isRunning);
		up1.setEnabled(isConfirmed && !isRunning);
		up10.setEnabled(isConfirmed && !isRunning);
		up100.setEnabled(isConfirmed && !isRunning);

		left100.setEnabled(isConfirmed && !isRunning);
		left10.setEnabled(isConfirmed && !isRunning);
		left1.setEnabled(isConfirmed && !isRunning);
		right1.setEnabled(isConfirmed && !isRunning);
		right10.setEnabled(isConfirmed && !isRunning);
		right100.setEnabled(isConfirmed && !isRunning);

		goPaperBorder.setEnabled(isConfirmed && !isRunning && hasSetHome);
		setHome.setEnabled(isConfirmed && !isRunning);
		goHome.setEnabled(isConfirmed && !isRunning && hasSetHome);
		findHome.setEnabled(isConfirmed && !isRunning && robot.getSettings().getHardwareProperties().canAutoHome());
		
		penUp.setEnabled(isConfirmed && !isRunning);
		penDown.setEnabled(isConfirmed && !isRunning);

		setFeedRate.setEnabled(isConfirmed && !isRunning);
		
		buttonSaveFile.setEnabled(robot!=null && robot.gCode != null && robot.gCode.isLoaded());
		
		this.validate();
	}

	public void newFile() {
		robot.setGCode(null);
		updateButtonAccess();
	}

	/**
	 * Creates a file open dialog. If you don't cancel it opens that file.
	 * Note: source for ExampleFileFilter can be found in FileChooserDemo, under the demo/jfc directory in the Java 2 SDK, Standard Edition.
	 */
	public void loadFileDialog() {
		// Is you machine not yet calibrated?
		if (robot.getSettings().isPaperConfigured() == false) {
			// Hey!  Come back after you calibrate! 
			JOptionPane.showMessageDialog(null, Translator.get("SetPaperSize"));
			return;
		}

		// list available loaders
		JFileChooser fc = new JFileChooser(new File(lastFileIn));
		ServiceLoader<LoadAndSaveFileType> imageLoaders = ServiceLoader.load(LoadAndSaveFileType.class);
		Iterator<LoadAndSaveFileType> i = imageLoaders.iterator();
		while(i.hasNext()) {
			LoadAndSaveFileType lft = i.next();
			if(lft.canLoad()) {
				FileFilter filter = lft.getFileNameFilter();
				fc.addChoosableFileFilter(filter);
			}
		}
		// no wild card filter, please.
		fc.setAcceptAllFileFilterUsed(false);
		// run the dialog
		if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			String selectedFile = fc.getSelectedFile().getAbsolutePath();
			FileFilter chosenFilter = fc.getFileFilter();
			i = imageLoaders.iterator();
			while(i.hasNext()) {
				LoadAndSaveFileType loader = i.next();
				if( !loader.getFileNameFilter().equals(chosenFilter)) continue;
				boolean success = openFileOnDemandWithLoader(selectedFile,loader);
				if(success) {
					lastFileIn = selectedFile;
				}
			}
		}
	}
	
	private void reopenFile() {
		openFileOnDemand(lastFileIn);
	}

	public void generateImage() {
		final JPanel panel = new JPanel(new GridBagLayout());

		ServiceLoader<ImageGenerator> imageGenerators = ServiceLoader.load(ImageGenerator.class);
		Iterator<ImageGenerator> ici = imageGenerators.iterator();
		int i=0;
		while(ici.hasNext()) {
			ici.next();
			i++;
		}
		
		String[] imageGeneratorNames = new String[i];
		
		i=0;
		ici = imageGenerators.iterator();
		while (ici.hasNext()) {
			ImageManipulator f = ici.next();
			imageGeneratorNames[i++] = f.getName();
		}

		final JComboBox<String> options = new JComboBox<String>(imageGeneratorNames);
		options.setSelectedIndex(generatorChoice);

		GridBagConstraints c = new GridBagConstraints();

		int y = 0;
		c.anchor = GridBagConstraints.EAST;
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = y;
		panel.add(new JLabel(Translator.get("ConversionStyle")), c);
		c.anchor = GridBagConstraints.WEST;
		c.gridwidth = 3;
		c.gridx = 1;
		c.gridy = y++;
		panel.add(options, c);

		int result = JOptionPane.showConfirmDialog(null, panel, Translator.get("ConversionOptions"),
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (result == JOptionPane.OK_OPTION) {
			generatorChoice = options.getSelectedIndex();

			ImageGenerator chosenGenerator = null; 
			ici = imageGenerators.iterator();
			i=0;
			while(ici.hasNext()) {
				chosenGenerator = ici.next();
				if(i==generatorChoice) {
					break;
				}
				i++;
			}
			
			robot.getSettings().saveConfig();
			robot.setDecorator(chosenGenerator);
			chosenGenerator.setRobot(robot);

			String destinationFile = System.getProperty("user.dir") + "/temp.ngc";;
			try (
					final OutputStream fileOutputStream = new FileOutputStream(destinationFile);
					final Writer out = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8)
					) {
				chosenGenerator.generate(gui,out);
				out.flush();
				out.close();
			} catch(IOException e) {
				e.printStackTrace();
			}
			robot.setDecorator(null);

			LoadAndSaveGCode loader = new LoadAndSaveGCode();
			try (final InputStream fileInputStream = new FileInputStream(destinationFile)) {
				loader.load(fileInputStream,robot);
			} catch(IOException e) {
				e.printStackTrace();
			}

			Log.message(Translator.get("Finished"));
			SoundSystem.playConversionFinishedSound();
			updateButtonAccess();
		}
	}

	public void saveFileDialog() {
		// list all the known savable file types.
		JFileChooser fc = new JFileChooser(new File(lastFileOut));
		ServiceLoader<LoadAndSaveFileType> imageSavers = ServiceLoader.load(LoadAndSaveFileType.class);
		Iterator<LoadAndSaveFileType> i = imageSavers.iterator();
		while(i.hasNext()) {
			LoadAndSaveFileType lft = i.next();
			if(lft.canSave()) {
				FileFilter filter = lft.getFileNameFilter();
				fc.addChoosableFileFilter(filter);
			}
		}
		// do not allow wild card (*.*) file extensions
		fc.setAcceptAllFileFilterUsed(false);
		// run the dialog
		if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			String selectedFile = fc.getSelectedFile().getAbsolutePath();
			FileFilter chosenFilter = fc.getFileFilter();
			
			// figure out which of the savers was requested.
			// TODO get rid of this stupid guessing game.
			i = imageSavers.iterator();
			while(i.hasNext()) {
				LoadAndSaveFileType lft = i.next();
				FileFilter filter = lft.getFileNameFilter();
				if( !chosenFilter.equals(filter) ) continue;
	
				// make sure a valid extension is added to the file.
				String selectedFileLC = selectedFile.toLowerCase();
				String[] exts = ((FileNameExtensionFilter)filter).getExtensions();
				boolean foundExtension=false;
				for(String ext : exts) {
					if (selectedFileLC.endsWith('.'+ext.toLowerCase())) {
						foundExtension=true;
						break;
					}
				}
				if(!foundExtension) {
					selectedFile+='.'+exts[0];
				}

				// try to save now.
				boolean success = false;
				try (final OutputStream fileOutputStream = new FileOutputStream(selectedFile)) {
					success=lft.save(fileOutputStream,robot);
				} catch(IOException e) {
					e.printStackTrace();
				}
				if(success==true) {
					lastFileOut = selectedFile;
					updateButtonAccess();
					break;
				}					
			}
		}
	}


	/**
	 * Open a file with a given LoadAndSaveFileType plugin.  
	 * The loader might spawn a new thread and return before the load is actually finished.
	 * @param filename absolute path of the file to load
	 * @param loader the plugin to use
	 * @return true if load is successful.
	 */
	public boolean openFileOnDemandWithLoader(String filename,LoadAndSaveFileType loader) {
		boolean success = false;
		try (final InputStream fileInputStream = new FileInputStream(filename)) {
			success=loader.load(fileInputStream,robot);
		} catch(IOException e) {
			e.printStackTrace();
		}

		// TODO don't rely on this to be true, load may not have finished yet.
		if (success == true) {
			Log.message(Translator.get("Finished"));
			SoundSystem.playConversionFinishedSound();
		}
		// TODO don't rely on this to be true, load may not have finished yet.
		updateButtonAccess();
		// TODO don't rely on this to be true, load may not have finished yet.
		statusBar.clear();
		
		return success;
	}
	
	/**
	 * User has asked that a file be opened.
	 * @param filename the file to be opened.
	 * @return true if file was loaded successfully.  false if it failed.
	 */
	public boolean openFileOnDemand(String filename) {
		Log.message(Translator.get("OpeningFile") + filename + "...");
		boolean success=false;
		boolean attempted=false;

		ServiceLoader<LoadAndSaveFileType> imageLoaders = ServiceLoader.load(LoadAndSaveFileType.class);
		Iterator<LoadAndSaveFileType> i = imageLoaders.iterator();
		while(i.hasNext()) {
			LoadAndSaveFileType loader = i.next();
			if(!loader.canLoad()) continue;
			if(!loader.canLoad(filename)) continue;
			
			attempted=true;
			success=openFileOnDemandWithLoader(filename,loader);
			if(success==true) break;
		}
		
		if(attempted == false) {
			Log.error(Translator.get("UnknownFileType"));
		}
		
		return success;
	}
}
