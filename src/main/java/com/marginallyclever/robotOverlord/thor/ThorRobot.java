package com.marginallyclever.robotOverlord.thor;

import javax.swing.JPanel;
import javax.vecmath.Vector3d;
import com.jogamp.opengl.GL2;
import com.marginallyclever.communications.NetworkConnection;
import com.marginallyclever.convenience.MathHelper;
import com.marginallyclever.convenience.PrimitiveSolids;
import com.marginallyclever.convenience.StringHelper;
import com.marginallyclever.robotOverlord.*;
import com.marginallyclever.robotOverlord.material.Material;
import com.marginallyclever.robotOverlord.model.Model;
import com.marginallyclever.robotOverlord.model.ModelFactory;
import com.marginallyclever.robotOverlord.robot.Robot;
import com.marginallyclever.robotOverlord.robot.RobotKeyframe;
import com.marginallyclever.robotOverlord.thor.tool.ThorTool;
import com.marginallyclever.robotOverlord.thor.tool.ThorToolGripper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;

public class ThorRobot extends Robot {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3644731265897692399L;
	// machine ID
	protected long robotUID;
	protected final static String hello = "HELLO WORLD! I AM THOR #";
	public final static String ROBOT_NAME = "Thor 6DOF Arm";

	// machine dimensions from design software
	/**
	 * Base Global position (X,Y,Z): 0,0,0 Rotation axis: None Rotation axis
	 * local position: None Articulation 1 Global position (X,Y,Z): 0,0,49
	 * Rotation axis: Z Rotation axis local position: 0,0,0 Articulation 2
	 * Global position (X,Y,Z): 0,0,137 Rotation axis: Y Rotation axis local
	 * position: 0,0,65
	 * 
	 * Articulation 3 Global position (X,Y,Z): 0,0,295 Rotation axis: Y Rotation
	 * axis local position: 0,0,64 Articulation 4 Global position (X,Y,Z):
	 * 0,0,377,5 Rotation axis: Z Rotation axis local position: 0,0,0
	 * Articulation 5&6 Global position (X,Y,Z): 0,0,510 Art 5 Rotation axis: Y
	 * Art 5 Rotation axis local position: 0,0,47 Art 6 Rotation axis: Z Art 6
	 * Rotation axis local position: 0,0,0
	 */
	public final static double ANCHOR_TO_SHOULDER = 4.9;
	public final static double SHOULDER_TO_BICEP = 20.2;
	public final static double BICEP_TO_ELBOW = 16;
	public final static double ELBOW_TO_ULNA = 0;
	public final static double ULNA_TO_WRIST = 19.5;
	public final static double ELBOW_TO_WRIST = ELBOW_TO_ULNA+ULNA_TO_WRIST;
	public final static double WRIST_TO_TOOL = 6.715;

	// model files
	private transient Model anchorModel = null;
	private transient Model shoulderModel = null;
	private transient Model bicepModel = null;
	private transient Model elbowModel = null;
	private transient Model ulnaModel = null;
	private transient Model wristModel = null;
	private transient Model handModel = null;

	private Material matAnchor = new Material();
	private Material matShoulder = new Material();
	private Material matBicep = new Material();
	private Material matElbow = new Material();
	private Material matUlna = new Material();
	private Material matWrist = new Material();
	private Material matHand = new Material();

	// currently attached tool
	private ThorTool tool = null;

	// collision volumes
	Cylinder[] volumes = new Cylinder[6];

	// motion states
	protected ThorKeyframe motionNow = new ThorKeyframe();
	protected ThorKeyframe motionFuture = new ThorKeyframe();

	// keyboard history
	protected float aDir = 0.0f;
	protected float bDir = 0.0f;
	protected float cDir = 0.0f;
	protected float dDir = 0.0f;
	protected float eDir = 0.0f;
	protected float fDir = 0.0f;

	protected float xDir = 0.0f;
	protected float yDir = 0.0f;
	protected float zDir = 0.0f;
	protected float uDir = 0.0f;
	protected float vDir = 0.0f;
	protected float wDir = 0.0f;

	// machine logic states
	protected boolean armMoved = false;
	protected boolean isPortConfirmed = false;
	protected double speed = 2;

	// visual debugging
	protected boolean isRenderFKOn = false;
	protected boolean isRenderIKOn = false;

	// gui
	protected transient ThorControlPanel arm5Panel = null;

	public final static float EPSILON = 0.00001f;
	
	
	public ThorRobot() {
		super();

		setDisplayName(ROBOT_NAME);

		// set up bounding volumes
		for (int i = 0; i < volumes.length; ++i) {
			volumes[i] = new Cylinder();
		}
		volumes[0].setRadius(3.2f);
		volumes[1].setRadius(3.0f * 0.575f);
		volumes[2].setRadius(2.2f);
		volumes[3].setRadius(1.15f);
		volumes[4].setRadius(1.2f);
		volumes[5].setRadius(1.0f * 0.575f);

		rotateBase(0, 0);
		checkAngleLimits(motionNow);
		checkAngleLimits(motionFuture);
		forwardKinematics(motionNow);
		forwardKinematics(motionFuture);
		inverseKinematics(motionNow);
		inverseKinematics(motionFuture);

		matAnchor.setDiffuseColor(1, 0, 0, 1);
		matShoulder.setDiffuseColor(1, 0, 0, 1);
		matBicep.setDiffuseColor(1, 0, 0, 1);
		matElbow.setDiffuseColor(1, 0, 0, 1);
		matUlna.setDiffuseColor(1, 0, 0, 1);
		matWrist.setDiffuseColor(1, 0, 0, 1);
		matHand.setDiffuseColor(1, 1, 0, 1);

		tool = new ThorToolGripper();
		tool.attachTo(this);
	}

	@Override
	protected void loadModels(GL2 gl2) {
		try {
			anchorModel = ModelFactory.createModelFromFilename("/Thor/BaseThor.stl", 0.1f);
			shoulderModel = ModelFactory.createModelFromFilename("/Thor/Art1Thor.stl", 0.1f);
			bicepModel = ModelFactory.createModelFromFilename("/Thor/Art2Thor.stl", 0.1f);
			elbowModel = ModelFactory.createModelFromFilename("/Thor/Art3Thor.stl", 0.1f);
			ulnaModel = ModelFactory.createModelFromFilename("/Thor/Art4Thor.stl", 0.1f);
			wristModel = ModelFactory.createModelFromFilename("/Thor/Art5.stl", 0.1f);
			handModel = ModelFactory.createModelFromFilename("/Thor/Art6.stl", 0.1f);

			shoulderModel.adjustOrigin(new Vector3d(0, 0,  4.9f ));
			bicepModel   .adjustOrigin(new Vector3d(0, 0, -6.5f ));
			elbowModel   .adjustOrigin(new Vector3d(0, 0, -6.5f ));
			ulnaModel    .adjustOrigin(new Vector3d(0, 0,  1.26f));
			wristModel   .adjustOrigin(new Vector3d(0, 0, -4.7f ));
			handModel    .adjustOrigin(new Vector3d(0, 0, -1.4f ));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
		inputStream.defaultReadObject();
	}

	@Override
	public ArrayList<JPanel> getContextPanel(RobotOverlord gui) {
		ArrayList<JPanel> list = super.getContextPanel(gui);

		if (list == null)
			list = new ArrayList<JPanel>();

		arm5Panel = new ThorControlPanel(gui, this);
		list.add(arm5Panel);
		updateGUI();

		ArrayList<JPanel> toolList = tool.getContextPanel(gui);
		Iterator<JPanel> iter = toolList.iterator();
		while (iter.hasNext()) {
			list.add(iter.next());
		}

		return list;
	}

	public boolean isPortConfirmed() {
		return isPortConfirmed;
	}

	private void enableFK() {
		xDir = 0;
		yDir = 0;
		zDir = 0;
		uDir = 0;
		vDir = 0;
		wDir = 0;
	}

	private void disableFK() {
		aDir = 0;
		bDir = 0;
		cDir = 0;
		dDir = 0;
		eDir = 0;
		fDir = 0;
	}

	public void setSpeed(double newSpeed) {
		speed = newSpeed;
	}

	public double getSpeed() {
		return speed;
	}

	public void moveA(float dir) {
		aDir = dir;
		enableFK();
	}

	public void moveB(float dir) {
		bDir = dir;
		enableFK();
	}

	public void moveC(float dir) {
		cDir = dir;
		enableFK();
	}

	public void moveD(float dir) {
		dDir = dir;
		enableFK();
	}

	public void moveE(float dir) {
		eDir = dir;
		enableFK();
	}

	public void moveF(float dir) {
		fDir = dir;
		enableFK();
	}

	public void moveX(float dir) {
		xDir = dir;
		disableFK();
	}

	public void moveY(float dir) {
		yDir = dir;
		disableFK();
	}

	public void moveZ(float dir) {
		zDir = dir;
		disableFK();
	}

	public void moveU(float dir) {
		uDir = dir;
		disableFK();
	}

	public void moveV(float dir) {
		vDir = dir;
		disableFK();
	}

	public void moveW(float dir) {
		wDir = dir;
		disableFK();
	}

	/**
	 * update the desired finger location
	 * 
	 * @param delta
	 *            time since the last update. usually ~1/30s.
	 */
	protected void updateIK(double delta) {
		boolean changed = false;
		motionFuture.fingerPosition.set(motionNow.fingerPosition);
		double dp = speed;// * delta;

		double dX = motionFuture.fingerPosition.x;
		double dY = motionFuture.fingerPosition.y;
		double dZ = motionFuture.fingerPosition.z;

		if (xDir != 0) {
			dX += xDir * dp;
			changed = true;
			xDir = 0;
		}
		if (yDir != 0) {
			dY += yDir * dp;
			changed = true;
			yDir = 0;
		}
		if (zDir != 0) {
			dZ += zDir * dp;
			changed = true;
			zDir = 0;
		}
		// rotations
		double ru=motionFuture.ikU;
		double rv=motionFuture.ikV;
		double rw=motionFuture.ikW;
		boolean hasTurned=false;

		if (uDir!=0) {
			ru += uDir * dp;
			changed=true;
			hasTurned=true;
			uDir=0;
		}
		if (vDir!=0) {
			rv += vDir * dp;
			changed=true;
			hasTurned=true;
			vDir=0;
		}
		if (wDir!=0) {
			rw += wDir * dp;
			changed=true;
			hasTurned=true;
			wDir=0;
		}

		if(hasTurned) {
			// On a 3-axis robot when homed the forward axis of the finger tip is pointing downward.
			// More complex arms start from the same assumption.
			Vector3d forward = new Vector3d(0,0,1);
			Vector3d right = new Vector3d(1,0,0);
			Vector3d up = new Vector3d();
			
			up.cross(forward,right);
			
			Vector3d of = new Vector3d(forward);
			Vector3d or = new Vector3d(right);
			Vector3d ou = new Vector3d(up);
			
			motionFuture.ikU=ru;
			motionFuture.ikV=rv;
			motionFuture.ikW=rw;
			
			Vector3d result;

			result = MathHelper.rotateAroundAxis(forward,of,Math.toRadians(motionFuture.ikU));  // TODO rotating around itself has no effect.
			result = MathHelper.rotateAroundAxis(result ,or,Math.toRadians(motionFuture.ikV));
			result = MathHelper.rotateAroundAxis(result ,ou,Math.toRadians(motionFuture.ikW));
			motionFuture.fingerForward.set(result);

			result = MathHelper.rotateAroundAxis(right ,of,Math.toRadians(motionFuture.ikU));
			result = MathHelper.rotateAroundAxis(result,or,Math.toRadians(motionFuture.ikV));
			result = MathHelper.rotateAroundAxis(result,ou,Math.toRadians(motionFuture.ikW));
			motionFuture.fingerRight.set(result);
		}

		// if(changed==true && motionFuture.movePermitted()) {
		if (changed) {
			motionFuture.fingerPosition.x = dX;
			motionFuture.fingerPosition.y = dY;
			motionFuture.fingerPosition.z = dZ;
			if (!inverseKinematics(motionFuture))
				return;
			if (checkAngleLimits(motionFuture)) {
				// if(motionNow.fingerPosition.epsilonEquals(motionFuture.fingerPosition,0.1f)
				// == false) {
				armMoved = true;
				isRenderIKOn = true;
				isRenderFKOn = false;

				sendChangeToRealMachine();
				if (!this.isPortConfirmed()) {
					// live data from the sensors will update motionNow, so only
					// do this if we're unconnected.
					motionNow.set(motionFuture);
				}
				updateGUI();
			} else {
				motionFuture.set(motionNow);
			}
		}
	}

	protected void updateFK(double delta) {
		boolean changed = false;
		double velcd = speed; // * delta
		double velabe = speed; // * delta

		motionFuture.set(motionNow);

		double dF = motionFuture.angleF;
		double dE = motionFuture.angleE;
		double dD = motionFuture.angleD;
		double dC = motionFuture.angleC;
		double dB = motionFuture.angleB;
		double dA = motionFuture.angleA;

		if (fDir != 0) {
			dF += velabe * fDir;
			changed = true;
			fDir = 0;
		}

		if (eDir != 0) {
			dE += velabe * eDir;
			changed = true;
			eDir = 0;
		}

		if (dDir != 0) {
			dD += velcd * dDir;
			changed = true;
			dDir = 0;
		}

		if (cDir != 0) {
			dC += velcd * cDir;
			changed = true;
			cDir = 0;
		}

		if (bDir != 0) {
			dB += velabe * bDir;
			changed = true;
			bDir = 0;
		}

		if (aDir != 0) {
			dA += velabe * aDir;
			changed = true;
			aDir = 0;
		}

		if (changed) {
			motionFuture.angleA = dA;
			motionFuture.angleB = dB;
			motionFuture.angleC = dC;
			motionFuture.angleD = dD;
			motionFuture.angleE = dE;
			motionFuture.angleF = dF;
			if (checkAngleLimits(motionFuture)) {
				forwardKinematics(motionFuture);
				isRenderIKOn = false;
				isRenderFKOn = true;
				armMoved = true;

				sendChangeToRealMachine();
				if (!this.isPortConfirmed()) {
					// live data from the sensors will update motionNow, so only
					// do this if we're unconnected.
					motionNow.set(motionFuture);
				}
				updateGUI();
			} else {
				motionFuture.set(motionNow);
			}
		}
	}

	public void updateGUI() {
		Vector3d v = new Vector3d();
		v.set(motionNow.fingerPosition);
		// TODO rotate fingerPosition before adding position
		v.add(getPosition());
		arm5Panel.xPos.setText(Double.toString(MathHelper.roundOff3(v.x)));
		arm5Panel.yPos.setText(Double.toString(MathHelper.roundOff3(v.y)));
		arm5Panel.zPos.setText(Double.toString(MathHelper.roundOff3(v.z)));
		arm5Panel.uPos.setText(Double.toString(MathHelper.roundOff3(motionNow.ikU)));
		arm5Panel.vPos.setText(Double.toString(MathHelper.roundOff3(motionNow.ikV)));
		arm5Panel.wPos.setText(Double.toString(MathHelper.roundOff3(motionNow.ikW)));

		arm5Panel.a1.setText(Double.toString(MathHelper.roundOff3(motionNow.angleA)));
		arm5Panel.b1.setText(Double.toString(MathHelper.roundOff3(motionNow.angleB)));
		arm5Panel.c1.setText(Double.toString(MathHelper.roundOff3(motionNow.angleC)));
		arm5Panel.d1.setText(Double.toString(MathHelper.roundOff3(motionNow.angleD)));
		arm5Panel.e1.setText(Double.toString(MathHelper.roundOff3(motionNow.angleE)));
		arm5Panel.f1.setText(Double.toString(MathHelper.roundOff3(motionNow.angleF)));

		if (tool != null)
			tool.updateGUI();
	}

	protected void sendChangeToRealMachine() {
		if (!isPortConfirmed)
			return;

		String str = "";
		if (motionFuture.angleA != motionNow.angleA) {
			str += " A" + MathHelper.roundOff3(motionFuture.angleA);
		}
		if (motionFuture.angleB != motionNow.angleB) {
			str += " B" + MathHelper.roundOff3(motionFuture.angleB);
		}
		if (motionFuture.angleC != motionNow.angleC) {
			str += " C" + MathHelper.roundOff3(motionFuture.angleC);
		}
		if (motionFuture.angleD != motionNow.angleD) {
			str += " D" + MathHelper.roundOff3(motionFuture.angleD);
		}
		if (motionFuture.angleE != motionNow.angleE) {
			str += " E" + MathHelper.roundOff3(motionFuture.angleE);
		}
		if (motionFuture.angleF != motionNow.angleF) {
			str += " F" + MathHelper.roundOff3(motionFuture.angleF);
		}

		if (str.length() > 0) {
			this.sendLineToRobot("R0" + str);
		}
	}

	@Override
	public void prepareMove(double delta) {
		updateIK(delta);
		updateFK(delta);
		if (tool != null)
			tool.update(delta);
	}

	@Override
	public void finalizeMove() {
		// copy motion_future to motion_now
		motionNow.set(motionFuture);

		if (armMoved) {
			if (this.isReadyToReceive) {
				armMoved = false;
			}
		}
	}

	public void render(GL2 gl2) {
		super.render(gl2);

		gl2.glPushMatrix();
		// TODO rotate model

		gl2.glPushMatrix();
		Vector3d p = getPosition();
		gl2.glTranslated(p.x, p.y, p.z);
		renderModels(gl2);
		gl2.glPopMatrix();

		if (isRenderFKOn) {
			gl2.glPushMatrix();
			gl2.glDisable(GL2.GL_DEPTH_TEST);
			renderFK(gl2);
			gl2.glEnable(GL2.GL_DEPTH_TEST);
			gl2.glPopMatrix();
		}

		if (isRenderIKOn) {
			gl2.glPushMatrix();
			gl2.glDisable(GL2.GL_DEPTH_TEST);
			renderIK(gl2);
			gl2.glEnable(GL2.GL_DEPTH_TEST);
			gl2.glPopMatrix();
		}
		gl2.glPopMatrix();
	}

	/**
	 * Visualize the inverse kinematics calculations
	 * 
	 * @param gl2
	 *            openGL render context
	 */
	protected void renderIK(GL2 gl2) {
		boolean lightOn= gl2.glIsEnabled(GL2.GL_LIGHTING);
		boolean matCoOn= gl2.glIsEnabled(GL2.GL_COLOR_MATERIAL);
		gl2.glDisable(GL2.GL_LIGHTING);
		
		Vector3d ff = new Vector3d();
		ff.set(motionNow.fingerForward);
		ff.scale(5);
		ff.add(motionNow.fingerPosition);
		Vector3d fr = new Vector3d();
		fr.set(motionNow.fingerRight);
		fr.scale(15);
		fr.add(motionNow.fingerPosition);
		
		gl2.glColor4f(1,0,0,1);

		gl2.glBegin(GL2.GL_LINE_STRIP);
		gl2.glVertex3d(0,0,0);
		gl2.glVertex3d(motionNow.ikBase.x,motionNow.ikBase.y,motionNow.ikBase.z);
		gl2.glVertex3d(motionNow.ikShoulder.x,motionNow.ikShoulder.y,motionNow.ikShoulder.z);
		gl2.glVertex3d(motionNow.ikElbow.x,motionNow.ikElbow.y,motionNow.ikElbow.z);
		gl2.glVertex3d(motionNow.ikWrist.x,motionNow.ikWrist.y,motionNow.ikWrist.z);
		gl2.glVertex3d(motionNow.fingerPosition.x,motionNow.fingerPosition.y,motionNow.fingerPosition.z);
		gl2.glEnd();

		gl2.glBegin(GL2.GL_LINES);
		gl2.glColor4f(0,0.8f,1,1);
		gl2.glVertex3d(motionNow.fingerPosition.x,motionNow.fingerPosition.y,motionNow.fingerPosition.z);
		gl2.glVertex3d(ff.x,ff.y,ff.z);

		gl2.glColor4f(0,0,1,1);
		gl2.glVertex3d(motionNow.fingerPosition.x,motionNow.fingerPosition.y,motionNow.fingerPosition.z);
		gl2.glVertex3d(fr.x,fr.y,fr.z);
		gl2.glEnd();
		
		if(lightOn) gl2.glEnable(GL2.GL_LIGHTING);
		if(matCoOn) gl2.glEnable(GL2.GL_COLOR_MATERIAL);
	}

	/**
	 * Draw the arm without calling glRotate to prove forward kinematics are
	 * correct.
	 * 
	 * @param gl2
	 *            openGL render context
	 */
	protected void renderFK(GL2 gl2) {
		boolean lightOn = gl2.glIsEnabled(GL2.GL_LIGHTING);
		boolean matCoOn = gl2.glIsEnabled(GL2.GL_COLOR_MATERIAL);
		gl2.glDisable(GL2.GL_LIGHTING);

		Vector3d ff = new Vector3d();
		ff.set(motionNow.fingerPosition);
		ff.add(motionNow.fingerForward);
		Vector3d fr = new Vector3d();
		fr.set(motionNow.fingerPosition);
		fr.add(motionNow.fingerRight);

		gl2.glColor4f(1, 1, 1, 1);
		gl2.glBegin(GL2.GL_LINE_STRIP);

		gl2.glVertex3d(0, 0, 0);
		gl2.glVertex3d(motionNow.shoulder.x, motionNow.shoulder.y, motionNow.shoulder.z);
		gl2.glVertex3d(motionNow.bicep.x, motionNow.bicep.y, motionNow.bicep.z);
		gl2.glVertex3d(motionNow.elbow.x, motionNow.elbow.y, motionNow.elbow.z);
		gl2.glVertex3d(motionNow.wrist.x, motionNow.wrist.y, motionNow.wrist.z);
		gl2.glVertex3d(motionNow.fingerPosition.x, motionNow.fingerPosition.y, motionNow.fingerPosition.z);
		gl2.glVertex3d(ff.x, ff.y, ff.z);
		gl2.glVertex3d(motionNow.fingerPosition.x, motionNow.fingerPosition.y, motionNow.fingerPosition.z);
		gl2.glVertex3d(fr.x, fr.y, fr.z);

		gl2.glEnd();

		// finger tip
		gl2.glColor4f(1, 0.8f, 0, 1);
		PrimitiveSolids.drawStar(gl2, motionNow.fingerPosition);
		gl2.glColor4f(0, 0.8f, 1, 1);
		PrimitiveSolids.drawStar(gl2, ff);
		gl2.glColor4f(0, 0, 1, 1);
		PrimitiveSolids.drawStar(gl2, fr);

		if (lightOn)
			gl2.glEnable(GL2.GL_LIGHTING);
		if (matCoOn)
			gl2.glEnable(GL2.GL_COLOR_MATERIAL);
	}

	/**
	 * Draw the physical model according to the angle values in the motionNow
	 * state.
	 * 
	 * @param gl2
	 *            openGL render context
	 */
	protected void renderModels(GL2 gl2) {
		matAnchor.render(gl2);
		anchorModel.render(gl2);

		// double t = Calendar.getInstance().get(Calendar.MILLISECOND)*0.001;

		gl2.glRotated(motionNow.angleF, 0, 0, 1);
		matShoulder.render(gl2);
		shoulderModel.render(gl2);

		gl2.glTranslated(0, 0, SHOULDER_TO_BICEP);
		gl2.glRotated(motionNow.angleE, 0, 1, 0);
		matBicep.render(gl2);
		bicepModel.render(gl2);

		gl2.glTranslated(0, 0, BICEP_TO_ELBOW);
		gl2.glRotated(motionNow.angleD, 0, 1, 0);
		matElbow.render(gl2);
		elbowModel.render(gl2);

		gl2.glTranslated(0, 0, ELBOW_TO_ULNA);
		gl2.glRotated(motionNow.angleC, 0, 0, 1);
		matUlna.render(gl2);
		ulnaModel.render(gl2);

		gl2.glTranslated(0, 0, ULNA_TO_WRIST);
		gl2.glRotated(motionNow.angleB, 0, 1, 0);
		matWrist.render(gl2);
		wristModel.render(gl2);

		// tool holder
		gl2.glTranslated(0, 0, WRIST_TO_TOOL);
		gl2.glRotated(motionNow.angleA, 0, 0, 1);
		matHand.render(gl2);
		handModel.render(gl2);

		// gl2.glTranslated(-6, 0, 0);
		if (tool != null) {
			// tool.render(gl2);
		}
	}


	protected void drawBounds(GL2 gl2) {
		throw new UnsupportedOperationException();
	}

	public void setModeAbsolute() {
		if (connection != null)
			this.sendLineToRobot("G90");
	}

	public void setModeRelative() {
		if (connection != null)
			this.sendLineToRobot("G91");
	}

	@Override
	// override this method to check that the software is connected to the right
	// type of robot.
	public void dataAvailable(NetworkConnection arg0, String line) {
		if (line.contains(hello)) {
			isPortConfirmed = true;
			// finalizeMove();
			setModeAbsolute();
			this.sendLineToRobot("R1");

			String uidString = line.substring(hello.length()).trim();
			System.out.println(">>> UID=" + uidString);
			try {
				long uid = Long.parseLong(uidString);
				if (uid == 0) {
					robotUID = getNewRobotUID();
				} else {
					robotUID = uid;
				}
				arm5Panel.setUID(robotUID);
			} catch (Exception e) {
				e.printStackTrace();
			}

			setDisplayName(ROBOT_NAME + " #" + robotUID);
		}

		if (isPortConfirmed) {
			if (line.startsWith("A")) {
				String items[] = line.split(" ");
				if (items.length >= 5) {
					for (int i = 0; i < items.length; ++i) {
						if (items[i].startsWith("A")) {
							double v = StringHelper.parseNumber(items[i].substring(1));
							if (motionFuture.angleA != v) {
								motionFuture.angleA = v;
								arm5Panel.a1.setText(Double.toString(MathHelper.roundOff3(v)));
							}
						} else if (items[i].startsWith("B")) {
							double v = StringHelper.parseNumber(items[i].substring(1));
							if (motionFuture.angleB != v) {
								motionFuture.angleB = v;
								arm5Panel.b1.setText(Double.toString(MathHelper.roundOff3(v)));
							}
						} else if (items[i].startsWith("C")) {
							double v = StringHelper.parseNumber(items[i].substring(1));
							if (motionFuture.angleC != v) {
								motionFuture.angleC = v;
								arm5Panel.c1.setText(Double.toString(MathHelper.roundOff3(v)));
							}
						} else if (items[i].startsWith("D")) {
							double v = StringHelper.parseNumber(items[i].substring(1));
							if (motionFuture.angleD != v) {
								motionFuture.angleD = v;
								arm5Panel.d1.setText(Double.toString(MathHelper.roundOff3(v)));
							}
						} else if (items[i].startsWith("E")) {
							double v = StringHelper.parseNumber(items[i].substring(1));
							if (motionFuture.angleE != v) {
								motionFuture.angleE = v;
								arm5Panel.e1.setText(Double.toString(MathHelper.roundOff3(v)));
							}
						}
					}

					forwardKinematics(motionFuture);
					motionNow.set(motionFuture);
					updateGUI();
				}
			} else {
				System.out.print("*** " + line);
			}
		}
	}

	public void moveBase(Vector3d dp) {
		motionFuture.anchorPosition.set(dp);
	}

	public void rotateBase(float pan, float tilt) {
		motionFuture.base_pan = pan;
		motionFuture.base_tilt = tilt;

		motionFuture.baseForward.y = (float) Math.sin(pan * Math.PI / 180.0) * (float) Math.cos(tilt * Math.PI / 180.0);
		motionFuture.baseForward.x = (float) Math.cos(pan * Math.PI / 180.0) * (float) Math.cos(tilt * Math.PI / 180.0);
		motionFuture.baseForward.z = (float) Math.sin(tilt * Math.PI / 180.0);
		motionFuture.baseForward.normalize();

		motionFuture.baseUp.set(0, 0, 1);

		motionFuture.baseRight.cross(motionFuture.baseForward, motionFuture.baseUp);
		motionFuture.baseRight.normalize();
		motionFuture.baseUp.cross(motionFuture.baseRight, motionFuture.baseForward);
		motionFuture.baseUp.normalize();
	}

	public BoundingVolume[] getBoundingVolumes() {
		// shoulder joint
		Vector3d t1 = new Vector3d(motionFuture.baseRight);
		t1.scale(volumes[0].getRadius() / 2);
		t1.add(motionFuture.shoulder);
		Vector3d t2 = new Vector3d(motionFuture.baseRight);
		t2.scale(-volumes[0].getRadius() / 2);
		t2.add(motionFuture.shoulder);
		volumes[0].SetP1(getWorldCoordinatesFor(t1));
		volumes[0].SetP2(getWorldCoordinatesFor(t2));
		// bicep
		volumes[1].SetP1(getWorldCoordinatesFor(motionFuture.shoulder));
		volumes[1].SetP2(getWorldCoordinatesFor(motionFuture.elbow));
		// elbow
		t1.set(motionFuture.baseRight);
		t1.scale(volumes[0].getRadius() / 2);
		t1.add(motionFuture.elbow);
		t2.set(motionFuture.baseRight);
		t2.scale(-volumes[0].getRadius() / 2);
		t2.add(motionFuture.elbow);
		volumes[2].SetP1(getWorldCoordinatesFor(t1));
		volumes[2].SetP2(getWorldCoordinatesFor(t2));
		// ulna
		volumes[3].SetP1(getWorldCoordinatesFor(motionFuture.elbow));
		volumes[3].SetP2(getWorldCoordinatesFor(motionFuture.wrist));
		// wrist
		t1.set(motionFuture.baseRight);
		t1.scale(volumes[0].getRadius() / 2);
		t1.add(motionFuture.wrist);
		t2.set(motionFuture.baseRight);
		t2.scale(-volumes[0].getRadius() / 2);
		t2.add(motionFuture.wrist);
		volumes[4].SetP1(getWorldCoordinatesFor(t1));
		volumes[4].SetP2(getWorldCoordinatesFor(t2));
		// finger
		volumes[5].SetP1(getWorldCoordinatesFor(motionFuture.wrist));
		volumes[5].SetP2(getWorldCoordinatesFor(motionFuture.fingerPosition));

		return volumes;
	}

	Vector3d getWorldCoordinatesFor(Vector3d in) {
		Vector3d out = new Vector3d(motionFuture.anchorPosition);

		Vector3d tempx = new Vector3d(motionFuture.baseForward);
		tempx.scale(in.x);
		out.add(tempx);

		Vector3d tempy = new Vector3d(motionFuture.baseRight);
		tempy.scale(-in.y);
		out.add(tempy);

		Vector3d tempz = new Vector3d(motionFuture.baseUp);
		tempz.scale(in.z);
		out.add(tempz);

		return out;
	}

	/**
	 * based on http://www.exampledepot.com/egs/java.net/Post.html
	 */
	private long getNewRobotUID() {
		long new_uid = 0;

		try {
			// Send data
			URL url = new URL("https://marginallyclever.com/evil_minion_getuid.php");
			URLConnection conn = url.openConnection();
			try (final InputStream connectionInputStream = conn.getInputStream();
					final Reader inputStreamReader = new InputStreamReader(connectionInputStream,
							StandardCharsets.UTF_8);
					final BufferedReader rd = new BufferedReader(inputStreamReader)) {
				String line = rd.readLine();
				new_uid = Long.parseLong(line);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}

		// did read go ok?
		if (new_uid != 0) {
			// make sure a topLevelMachinesPreferenceNode node is created
			// tell the robot it's new UID.
			this.sendLineToRobot("UID " + new_uid);
		}
		return new_uid;
	}

	// TODO check for collisions with
	// http://geomalgorithms.com/a07-_distance.html#dist3D_Segment_to_Segment ?
	public boolean movePermitted(ThorKeyframe keyframe) {
		// don't hit floor?
		// don't hit ceiling?

		// check far limit
		// seems doable
		if (!inverseKinematics(keyframe))
			return false;
		// angle are good?
		if (!checkAngleLimits(keyframe))
			return false;

		// OK
		return true;
	}

	protected boolean checkAngleLimits(ThorKeyframe keyframe) {
		// machine specific limits

		return true;
	}

	/**
	 * Find the arm joint angles that would put the finger at the desired
	 * location.
	 * 
	 * @return false if successful, true if the IK solution cannot be found.
	 */
	protected boolean inverseKinematics(ThorKeyframe keyframe) {
		double n, ee, xx,yy;
		
		// rotation at finger, bend at wrist, rotation between wrist and elbow, then bends down to base.
		
		// find the wrist position
		Vector3d towardsFinger = new Vector3d(keyframe.fingerForward);
		n = (float)WRIST_TO_TOOL;
		towardsFinger.scale(n);
		
		keyframe.ikWrist = new Vector3d(keyframe.fingerPosition);
		keyframe.ikWrist.sub(towardsFinger);
		
		keyframe.ikBase = new Vector3d(0,0,0);
		keyframe.ikShoulder = new Vector3d(0,0,(float)(SHOULDER_TO_BICEP));

		// Find the facingDirection and planeNormal vectors.
		Vector3d facingDirection = new Vector3d(keyframe.ikWrist.x,keyframe.ikWrist.y,0);
		if(Math.abs(keyframe.ikWrist.x)<EPSILON && Math.abs(keyframe.ikWrist.y)<EPSILON) {
			// Wrist is directly above shoulder, makes calculations hard.
			// TODO figure this out.  Use previous state to guess elbow?
			return false;
		}
		facingDirection.normalize();
		Vector3d up = new Vector3d(0,0,1);
		Vector3d planarRight = new Vector3d();
		planarRight.cross(facingDirection, up);
		planarRight.normalize();
		
		// Find elbow by using intersection of circles.
		// http://mathworld.wolfram.com/Circle-CircleIntersection.html
		// x = (dd-rr+RR) / (2d)
		Vector3d v0 = new Vector3d(keyframe.ikWrist);
		v0.sub(keyframe.ikShoulder);
		double d = v0.length();
		double R = Math.abs(BICEP_TO_ELBOW);
		double r = Math.abs(ELBOW_TO_WRIST);
		if( d > R+r ) {
			// impossibly far away
			return false;
		}
		double x = (d*d - r*r + R*R ) / (2*d);
		if( x > R ) {
			// would cause Math.sqrt(a negative number)
			return false;
		}
		v0.normalize();
		keyframe.ikElbow.set(v0);
		keyframe.ikElbow.scale(x);
		keyframe.ikElbow.add(keyframe.ikShoulder);
		// v1 is now at the intersection point between ik_wrist and ik_boom
		Vector3d v1 = new Vector3d();
		double a = (float)( Math.sqrt( R*R - x*x ) );
		v1.cross(planarRight, v0);
		v1.scale(a);
		keyframe.ikElbow.add(v1);

		// angleF is the base
		// all the joint locations are now known.  find the angles.
		ee = Math.atan2(facingDirection.y, facingDirection.x);
		ee = MathHelper.capRotationRadians(ee);
		keyframe.angleF = Math.toDegrees(ee);

		// angleE is the shoulder
		Vector3d towardsElbow = new Vector3d(keyframe.ikElbow);
		towardsElbow.sub(keyframe.ikShoulder);
		towardsElbow.normalize();
		xx = towardsElbow.z;
		yy = facingDirection.dot(towardsElbow);
		ee = Math.atan2(yy, xx);
		ee = MathHelper.capRotationRadians(ee);
		keyframe.angleE = Math.toDegrees(ee);

		// angleD is the elbow
		Vector3d towardsWrist = new Vector3d(keyframe.ikWrist);
		towardsWrist.sub(keyframe.ikElbow);
		towardsWrist.normalize();
		xx = towardsElbow.dot(towardsWrist);
		v1.cross(planarRight,towardsElbow);
		yy = towardsWrist.dot(v1);
		ee = Math.atan2(yy, xx);
		ee = MathHelper.capRotationRadians(ee);
		keyframe.angleD = -Math.toDegrees(ee);
		
		// angleC is the ulna rotation
		v0.set(towardsWrist);
		v0.normalize();
		v1.cross(v0,planarRight);
		v1.normalize();
		Vector3d towardsFingerAdj = new Vector3d(keyframe.fingerForward);
		double tf = v0.dot(towardsFingerAdj);
		if(tf>=1-EPSILON) {
			// cannot calculate angle, leave as was
			return false;
		}
		// can calculate angle
		v0.scale(tf);
		towardsFingerAdj.sub(v0);
		towardsFingerAdj.normalize();
		xx = planarRight.dot(towardsFingerAdj);
		yy = v1.dot(towardsFingerAdj);
		ee = Math.atan2(yy, xx);
		ee = MathHelper.capRotationRadians(ee);
		keyframe.angleC = Math.toDegrees(ee)-90;
		
		// angleB is the wrist bend
		v0.set(towardsWrist);
		v0.normalize();
		xx = v0.dot(towardsFinger);
		yy = towardsFingerAdj.dot(towardsFinger);
		ee = Math.atan2(yy, xx);
		ee = MathHelper.capRotationRadians(ee);
		keyframe.angleB = Math.toDegrees(ee);
		
		// angleA is the hand rotation
		v0.cross(towardsFingerAdj,towardsWrist);
		v0.normalize();
		v1.cross(v0, towardsFinger);
		v1.normalize();
		
		xx = v0.dot(keyframe.fingerRight);
		yy = v1.dot(keyframe.fingerRight);
		ee = Math.atan2(yy, xx);
		ee = MathHelper.capRotationRadians(ee);
		keyframe.angleA = Math.toDegrees(ee);

		return true;
	}

	/**
	 * Calculate the finger location from the angles at each joint
	 * 
	 * @param state
	 */
	protected void forwardKinematics(ThorKeyframe keyframe) {
		double f = Math.toRadians(keyframe.angleF);
		double e = Math.toRadians(keyframe.angleE-90);
		double d = Math.toRadians(keyframe.angleD);
		double c = Math.toRadians(keyframe.angleC);
		double b = Math.toRadians(keyframe.angleB);
		double a = Math.toRadians(keyframe.angleA);
		
		Vector3d originToShoulder = new Vector3d(0,0,(float)ANCHOR_TO_SHOULDER);
		Vector3d facingDirection = new Vector3d(Math.cos(f),Math.sin(f),0);
		Vector3d up = new Vector3d(0,0,1);
		Vector3d planarRight = new Vector3d();
		planarRight.cross(facingDirection, up);
		planarRight.normalize();

		keyframe.shoulder.set(originToShoulder);
		Vector3d shoulderToBicep = new Vector3d(0,0,(float)SHOULDER_TO_BICEP);
		keyframe.bicep.set(shoulderToBicep);
		
		// boom to elbow
		Vector3d toElbow = new Vector3d(facingDirection);
		toElbow.scale( Math.cos(-e) );
		Vector3d v2 = new Vector3d(up);
		v2.scale( Math.sin(-e) );
		toElbow.add(v2);
		float n = (float)BICEP_TO_ELBOW;
		toElbow.scale(n);
		
		keyframe.elbow.set(toElbow);
		keyframe.elbow.add(keyframe.bicep);
		
		// elbow to wrist
		Vector3d towardsElbowOrtho = new Vector3d();
		towardsElbowOrtho.cross(toElbow, planarRight);
		towardsElbowOrtho.normalize();

		Vector3d elbowToWrist = new Vector3d(toElbow);
		elbowToWrist.normalize();
		elbowToWrist.scale( Math.cos(d) );
		v2.set(towardsElbowOrtho);
		v2.scale( Math.sin(d) );
		elbowToWrist.add(v2);
		n = (float)ULNA_TO_WRIST;
		elbowToWrist.scale(n);
		
		keyframe.wrist.set(elbowToWrist);
		keyframe.wrist.add(keyframe.elbow);

		// wrist to finger
		Vector3d wristOrthoBeforeUlnaRotation = new Vector3d();
		wristOrthoBeforeUlnaRotation.cross(elbowToWrist, planarRight);
		wristOrthoBeforeUlnaRotation.normalize();
		Vector3d wristOrthoAfterRotation = new Vector3d(wristOrthoBeforeUlnaRotation);
		
		wristOrthoAfterRotation.scale( Math.cos(-c) );
		v2.set(planarRight);
		v2.scale( Math.sin(-c) );
		wristOrthoAfterRotation.add(v2);
		wristOrthoAfterRotation.normalize();

		Vector3d towardsFinger = new Vector3d();

		towardsFinger.set(elbowToWrist);
		towardsFinger.normalize();
		towardsFinger.scale( (float)( Math.cos(b) ) );
		v2.set(wristOrthoAfterRotation);
		v2.scale( (float)( Math.sin(b) ) );
		towardsFinger.add(v2);
		towardsFinger.normalize();

		keyframe.fingerPosition.set(towardsFinger);
		n = (float)WRIST_TO_TOOL;
		keyframe.fingerPosition.scale(n);
		keyframe.fingerPosition.add(keyframe.wrist);

		// finger rotation
		Vector3d v0 = new Vector3d();
		Vector3d v1 = new Vector3d();
		v0.cross(towardsFinger,wristOrthoAfterRotation);
		v0.normalize();
		v1.cross(v0,towardsFinger);
		v1.normalize();
		
		keyframe.fingerRight.set(v0);
		keyframe.fingerRight.scale(Math.cos(a));
		v2.set(v1);
		v2.scale(Math.sin(a));
		keyframe.fingerRight.add(v2);

		keyframe.fingerForward.set(towardsFinger);
		keyframe.fingerForward.normalize();
	}

	@Override
	public RobotKeyframe createKeyframe() {
		return new ThorKeyframe();
	}
}
