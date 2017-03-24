package com.marginallyclever.robotOverlord.makelangeloRobot.loadAndSave;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ProgressMonitor;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.marginallyclever.robotOverlord.makelangeloRobot.ImageManipulator;
import com.marginallyclever.robotOverlord.Log;
import com.marginallyclever.robotOverlord.makelangeloRobot.TransformedImage;
import com.marginallyclever.robotOverlord.Translator;
import com.marginallyclever.robotOverlord.makelangeloRobot.converters.ImageConverter;
import com.marginallyclever.robotOverlord.makelangeloRobot.MakelangeloRobot;
import com.marginallyclever.robotOverlord.makelangeloRobot.MakelangeloRobotPanel;
import com.marginallyclever.robotOverlord.makelangeloRobot.generators.Generator_Text;
import com.marginallyclever.util.PreferencesHelper;

/**
 * LoadImage uses an InputStream of data to create gcode. 
 * @author Dan Royer
 *
 */
public class LoadAndSaveImage extends ImageManipulator implements LoadAndSaveFileType {
	
	@SuppressWarnings("deprecation")
	private Preferences prefs = PreferencesHelper
			.getPreferenceNode(PreferencesHelper.MakelangeloPreferenceKey.LEGACY_MAKELANGELO_ROOT);

	private ServiceLoader<ImageConverter> converters;
	
	/**
	 * Set of image file extensions.
	 */
	private static final Set<String> IMAGE_FILE_EXTENSIONS;

	static {
		IMAGE_FILE_EXTENSIONS = new HashSet<>();
		IMAGE_FILE_EXTENSIONS.add("jpg");
		IMAGE_FILE_EXTENSIONS.add("jpeg");
		IMAGE_FILE_EXTENSIONS.add("png");
		IMAGE_FILE_EXTENSIONS.add("wbmp");
		IMAGE_FILE_EXTENSIONS.add("bmp");
		IMAGE_FILE_EXTENSIONS.add("gif");
	}
	private static FileNameExtensionFilter filter = new FileNameExtensionFilter(Translator.get("FileTypeImage"),
			IMAGE_FILE_EXTENSIONS.toArray(new String[IMAGE_FILE_EXTENSIONS.size()]));

	
	@Override
	public FileNameExtensionFilter getFileNameFilter() {
		return filter;
	}

	@Override
	public boolean canLoad(String filename) {
		final String filenameExtension = filename.substring(filename.lastIndexOf('.') + 1);
		return IMAGE_FILE_EXTENSIONS.contains(filenameExtension.toLowerCase());
	}


	protected boolean chooseImageConversionOptions(MakelangeloRobot robot) {
		final JPanel panel = new JPanel(new GridBagLayout());

		Iterator<ImageConverter> ici = converters.iterator();
		int i=0;
		while(ici.hasNext()) {
			ici.next();
			i++;
		}
				
		String[] imageConverterNames = new String[i];

		i=0;
		ici = converters.iterator();
		while (ici.hasNext()) {
			ImageManipulator f = ici.next();
			imageConverterNames[i++] = f.getName();
		}

		final JComboBox<String> inputDrawStyle = new JComboBox<String>(imageConverterNames);
		inputDrawStyle.setSelectedIndex(getPreferredDrawStyle());

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
		panel.add(inputDrawStyle, c);

		int result = JOptionPane.showConfirmDialog(null, panel, Translator.get("ConversionOptions"),
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (result == JOptionPane.OK_OPTION) {
			setPreferredDrawStyle(inputDrawStyle.getSelectedIndex());
			robot.getSettings().saveConfig();

			return true;
		}

		return false;
	}
	


	/**
	 * Load and convert the image in the chosen style
	 * @return false if loading cancelled or failed.
	 */
	public boolean load(InputStream in,final MakelangeloRobot robot) {
		final TransformedImage img;
		try {
			img = new TransformedImage( ImageIO.read(in) );
		} catch (IOException e1) {
			e1.printStackTrace();
			return false;
		}
		
		// scale image to fit paper, same behaviour as before.
		if( robot.getSettings().getPaperWidth() > robot.getSettings().getPaperHeight() ) {
			if(robot.getSettings().getPaperWidth()*10.0f < img.getSourceImage().getWidth()) {
				float f = (float)( robot.getSettings().getPaperWidth()*10.0f / img.getSourceImage().getWidth() );
				img.setScaleX(img.getScaleX() * f);
				img.setScaleY(img.getScaleY() * f);
			}
		} else {
			if(robot.getSettings().getPaperHeight()*10.0f < img.getSourceImage().getHeight()) {
				float f = (float)( robot.getSettings().getPaperHeight()*10.0f / img.getSourceImage().getHeight() );
				img.setScaleX(img.getScaleX() * f);
				img.setScaleY(img.getScaleY() * f);
			}
		}
		
		// where to save temp output file?
		final String destinationFile = System.getProperty("user.dir") + "/temp.ngc";

		converters = ServiceLoader.load(ImageConverter.class);
		if (!chooseImageConversionOptions(robot)) return false;

		final ProgressMonitor pm = new ProgressMonitor(null, Translator.get("Converting"), "", 0, 100);
		pm.setProgress(0);
		pm.setMillisToPopup(0);

		final SwingWorker<Void, Void> s = new SwingWorker<Void, Void>() {
			@Override
			public Void doInBackground() {
				try (OutputStream fileOutputStream = new FileOutputStream(destinationFile);
						Writer out = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8)) {
					// read in image
					Log.message(Translator.get("Converting") + " " + destinationFile);
					// convert with style

					ImageConverter converter = null;
					int preferredIndex = getPreferredDrawStyle();
					int i=0;
					Iterator<ImageConverter> ici = converters.iterator();
					while(ici.hasNext()) {
						converter = ici.next();
						if(i==preferredIndex) break;
						++i;
					}
					converter.setParent(this);
					converter.setProgressMonitor(pm);
					converter.setRobot(robot);
					robot.setDecorator(converter);
					converter.convert(img, out);
					robot.setDecorator(null);

					if (robot.getSettings().shouldSignName()) {
						// Sign name
						Generator_Text ymh = new Generator_Text();
						ymh.setRobot(robot);
						ymh.signName(out);
					}
				} catch (IOException e) {
					Log.error(Translator.get("Failed") + e.getLocalizedMessage());
				}

				// out closed when scope of try() ended.

				pm.setProgress(100);
				return null;
			}

			@Override
			public void done() {
				pm.close();
				LoadAndSaveGCode loader = new LoadAndSaveGCode();
				try (final InputStream fileInputStream = new FileInputStream(destinationFile)) {
					loader.load(fileInputStream,robot);
					MakelangeloRobotPanel panel = robot.getControlPanel();
					if(panel!=null) panel.updateButtonAccess();
				} catch(IOException e) {
					e.printStackTrace();
				}
			}
		};

		s.addPropertyChangeListener(new PropertyChangeListener() {
			// Invoked when task's progress property changes.
			public void propertyChange(PropertyChangeEvent evt) {
				if (Objects.equals("progress", evt.getPropertyName())) {
					int progress = (Integer) evt.getNewValue();
					pm.setProgress(progress);
					String message = String.format("%d%%.\n", progress);
					pm.setNote(message);
					if (s.isDone()) {
						Log.message(Translator.get("Finished"));
					} else if (s.isCancelled() || pm.isCanceled()) {
						if (pm.isCanceled()) {
							s.cancel(true);
						}
						Log.message(Translator.get("Cancelled"));
					}
				}
			}
		});

		s.execute();

		return true;
	}
	
	private void setPreferredDrawStyle(int style) {
		prefs.putInt("Draw Style", style);
	}

	private int getPreferredDrawStyle() {
		return prefs.getInt("Draw Style", 0);
	}
	
	public boolean canSave(String filename) {
		return false;
	}
	
	public boolean save(OutputStream outputStream,MakelangeloRobot robot) {
		return false;
	}

	@Override
	public boolean canLoad() {
		return true;
	}

	@Override
	public boolean canSave() {
		return false;
	}
}
