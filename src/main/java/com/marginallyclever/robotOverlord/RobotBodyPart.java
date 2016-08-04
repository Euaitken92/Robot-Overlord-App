package com.marginallyclever.robotOverlord;

import javax.vecmath.Vector3f;

import com.jogamp.opengl.GL2;
import com.marginallyclever.robotOverlord.model.Model;

/**
 * a location, an orientation, a model, and linkages to other body parts.
 * @author Admin
 *
 */
public class RobotBodyPart {
	Model model;
	Material material;
	
	Vector3f position;
	
	public void render(GL2 gl2) {
		gl2.glPushMatrix();
		gl2.glTranslatef(position.x, position.y, position.z);
		material.render(gl2);
		model.render(gl2);
		gl2.glPopMatrix();
	}
}
