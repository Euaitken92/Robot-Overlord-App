package com.marginallyclever.robotOverlord.entity.scene.dhRobotEntity.solvers;

import javax.vecmath.Matrix4d;
import javax.vecmath.Vector3d;

import com.marginallyclever.convenience.MatrixHelper;
import com.marginallyclever.robotOverlord.entity.scene.dhRobotEntity.DHKeyframe;
import com.marginallyclever.robotOverlord.entity.scene.dhRobotEntity.DHLink;
import com.marginallyclever.robotOverlord.entity.scene.dhRobotEntity.DHRobotEntity;

/**
 * See https://www.alanzucconi.com/2017/04/10/gradient-descent/
 * and https://www.alanzucconi.com/2017/04/10/robotic-arms/
 * @author Dan Royer
 * @since 1.6.0
 *
 */
public class DHIKSolver_GradientDescent extends DHIKSolver {
	// For the sixi robot arm, the max reach is 800mm and the sensor resolution is 2^12 (4096).
	protected static final double SENSOR_RESOLUTION = 360.0/Math.pow(2,12);  // 0.087890625 degrees = 0.00153398079 radians
	// protected static final double MAX_REACH = 800; // mm
	// protected static final double DISTANCE_AT_MAX_REACH = Math.tan(Math.toRadians(SENSOR_RESOLUTION)) * MAX_REACH;  // 1.2272mm
	// But this is a generic solver that should work with any arm, so.

	protected static final int ITERATIONS = 10;
	// Scale the "handles" used in distanceToTarget().  Bigger scale, greater rotation compensation
	protected static final double CORRECTIVE_FACTOR = 100;
	// If distanceToTarget() score is within threshold, quit with success. 
	protected static final double THRESHOLD = 0.1;

	// how big a step to take with each partial descent?
	protected double [] samplingDistances = { 0,0,0,0,0,0 };
	// how much of that partial descent to actually apply?
	protected double learningRate=0;
	
	protected DHRobotEntity robot;
	protected Matrix4d targetMatrix;
	protected DHLink endEffector;
	
	/**
	 * @return the number of double values needed to store a valid solution from this DHIKSolver.
	 */
	public int getSolutionSize() {
		return 6;
	}
	
	public double distanceToTarget() {
		Matrix4d currentMatrix = endEffector.getPoseWorld();
		
		// linear difference in centers
		Vector3d c0 = new Vector3d();
		Vector3d c1 = new Vector3d();
		currentMatrix.get(c0);
		targetMatrix.get(c1);
		c1.sub(c0);
		double dC = c1.lengthSquared();
		
		// linear difference in X handles
		Vector3d x0 = MatrixHelper.getXAxis(targetMatrix);
		Vector3d x1 = MatrixHelper.getXAxis(currentMatrix);
		x1.scale(CORRECTIVE_FACTOR);
		x0.scale(CORRECTIVE_FACTOR);
		x1.sub(x0);
		double dX = x1.lengthSquared();
		
		// linear difference in Y handles
		Vector3d y0 = MatrixHelper.getYAxis(targetMatrix);
		Vector3d y1 = MatrixHelper.getYAxis(currentMatrix);
		y1.scale(CORRECTIVE_FACTOR);
		y0.scale(CORRECTIVE_FACTOR);
		y1.sub(y0);
		double dY = y1.lengthSquared();		

	    // now sum these to get the error term.
		return dC+dX+dY;
	}

	/**
	 * Adjust one link angle.  Is it better?  Also check if both directions are equally bad, which means we're near the minimum.
	 * 
	 * @param link
	 * @param i
	 * @return
	 */
	protected double partialDescent(DHLink link,int i) {
		double oldValue = link.getAdjustableValue();
		double Fx = distanceToTarget();

		link.setAdjustableValue(oldValue + samplingDistances[i]);
		link.refreshPoseMatrix();
		double FxPlusD = distanceToTarget();

		link.setAdjustableValue(oldValue - samplingDistances[i]);
		link.refreshPoseMatrix();
		double FxMinusD = distanceToTarget();

		link.setAdjustableValue(oldValue);
		link.refreshPoseMatrix();

		if( FxMinusD > Fx && FxPlusD > Fx ) {
			samplingDistances[i] *= 2.0/3.0;
			return 0;
		}
		
		double gradient = ( FxPlusD - Fx ) / samplingDistances[i];
		return gradient;
	}

	/**
	 * We're going to blindly jiggle the arm very slightly and see which jiggle gets us closer to the target.
	 * Eventually we get close enough and quit.
	 * We might not actually reach the target by the time we've done interating.
	 */
	@Override
	public SolutionType solveWithSuggestion(DHRobotEntity robot,Matrix4d targetMatrix,DHKeyframe keyframe,DHKeyframe suggestion) {
		this.robot = robot;
		this.targetMatrix = targetMatrix;
		
		// TODO get a better method of finding the end effector
		this.endEffector = (DHLink)robot.findByPath("./X/Y/Z/U/V/W/End Effector");

		// these need to be reset each run.
		learningRate=0.125;
		samplingDistances[0]=SENSOR_RESOLUTION; 
		samplingDistances[1]=SENSOR_RESOLUTION;
		samplingDistances[2]=SENSOR_RESOLUTION;
		samplingDistances[3]=SENSOR_RESOLUTION;
		samplingDistances[4]=SENSOR_RESOLUTION;
		samplingDistances[5]=SENSOR_RESOLUTION;
		

		double dtt=10;
		
		for(int iter=0;iter<ITERATIONS;++iter) {
			// seems to work better ascending than descending
			//for( int i=0; i<robot.getNumLinks(); ++i ) {
			for( int i=robot.getNumLinks()-1; i>=0; --i ) {
				DHLink link = robot.links.get(i);
				
				double oldValue = link.getAdjustableValue();
				double gradient = partialDescent( link, i );
				double newValue = oldValue - gradient * learningRate; 
				newValue = Math.max(Math.min(newValue, link.rangeMax.get()-1e-6), link.rangeMin.get()+1e-6);
				
				link.setAdjustableValue(newValue);
				link.refreshPoseMatrix();
		
				dtt=distanceToTarget();
				if(dtt<THRESHOLD) break;
			}
			if(dtt<THRESHOLD) break;
		}
		
		for( int i=0; i<robot.getNumLinks(); ++i ) {
			keyframe.fkValues[i] = robot.links.get(i).getAdjustableValue();
		}
		
		return SolutionType.ONE_SOLUTION;
	}
}
