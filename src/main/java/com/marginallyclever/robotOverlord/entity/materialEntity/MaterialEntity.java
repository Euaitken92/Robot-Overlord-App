package com.marginallyclever.robotOverlord.entity.materialEntity;

import java.io.IOException;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;
import com.marginallyclever.convenience.FileAccess;
import com.marginallyclever.robotOverlord.entity.Entity;
import com.marginallyclever.robotOverlord.entity.basicDataTypes.BooleanEntity;
import com.marginallyclever.robotOverlord.entity.basicDataTypes.ColorEntity;
import com.marginallyclever.robotOverlord.entity.basicDataTypes.DoubleEntity;
import com.marginallyclever.robotOverlord.entity.basicDataTypes.StringEntity;


/**
 * Material properties (surface finish, color, texture, etc) of something displayed in the world.
 * @author Dan Royer
 *
 */
public class MaterialEntity extends Entity {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7313230310466105159L;
	
	private ColorEntity diffuse    = new ColorEntity("Diffuse" ,1.00,1.00,1.00,1.00);
	private ColorEntity specular   = new ColorEntity("Specular",0.85,0.85,0.85,1.00);
	private ColorEntity emission   = new ColorEntity("Emission",0.01,0.01,0.01,1.00);
	private ColorEntity ambient    = new ColorEntity("Ambient" ,0.01,0.01,0.01,1.00);
	private DoubleEntity shininess = new DoubleEntity("Shininess",10.0);
	private BooleanEntity isLit    = new BooleanEntity("On",true);
	
	private Texture texture = null;
	private StringEntity textureFilename = new StringEntity("Texture","");
	private transient boolean textureDirty;
		
	public MaterialEntity() {
		super();
		this.setName("Material");

		addChild(diffuse);
		addChild(specular);
		addChild(emission);
		addChild(ambient);
		addChild(shininess);
		addChild(isLit);
		
		addChild(textureFilename);
		
		textureDirty=true;
	}
	
	public void render(GL2 gl2) {
		gl2.glColor4d(diffuse.getR(),diffuse.getG(),diffuse.getB(),diffuse.getA());
		gl2.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, diffuse.getFloatArray(),0);
	    gl2.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, specular.getFloatArray(),0);
	    gl2.glMaterialfv(GL2.GL_FRONT, GL2.GL_EMISSION, emission.getFloatArray(),0);
	    gl2.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT, ambient.getFloatArray(),0);
	    gl2.glMaterialf(GL2.GL_FRONT, GL2.GL_SHININESS, shininess.get().floatValue());
	    gl2.glColorMaterial(GL2.GL_FRONT,GL2.GL_AMBIENT_AND_DIFFUSE );
	    
	    //boolean isColorEnabled = gl2.glIsEnabled(GL2.GL_COLOR_MATERIAL);
		//gl2.glDisable(GL2.GL_COLOR_MATERIAL);
		
		gl2.glShadeModel(GL2.GL_SMOOTH);
		
	    if(isLit()) gl2.glEnable(GL2.GL_LIGHTING);
	    else gl2.glDisable(GL2.GL_LIGHTING);

		if(textureDirty) {
			// texture has changed, load the new texture.
			String tName = textureFilename.get();
			try {
				if(tName == null || tName.length()==0) texture = null;
				else {
					texture = TextureIO.newTexture(FileAccess.open(tName), false, tName.substring(tName.lastIndexOf('.')+1));
				}
			} catch(IOException e) {
				e.printStackTrace();
			}
			textureDirty=false;
		}
	    if(texture==null) {
			gl2.glDisable(GL2.GL_TEXTURE_2D);
	    } else {
			gl2.glEnable(GL2.GL_TEXTURE_2D);
	    	texture.bind(gl2);
	    }
	    
	    //if(isColorEnabled) gl2.glEnable(GL2.GL_COLOR_MATERIAL);
	}
	

	public void setShininess(double arg0) {
		arg0 = Math.min(Math.max(arg0, 0), 128);
		shininess.set(arg0);
	}
	public double getShininess() {
		return shininess.get();
	}
	
	public void setDiffuseColor(float r,float g,float b,float a) {
		diffuse.set(r,g,b,a);
	}
	
	public void setSpecularColor(float r,float g,float b,float a) {
		specular.set(r,g,b,a);
	}
	
	public void setEmissionColor(float r,float g,float b,float a) {
		emission.set(r,g,b,a);
	}
	

	public void setAmbientColor(float r,float g,float b,float a) {
		ambient.set(r,g,b,a);
	}
    
	public float[] getDiffuseColor() {
		return diffuse.getFloatArray();
	}

	public float[] getAmbientColor() {
		return ambient.getFloatArray();
	}
	
	public float[] getSpecular() {
		return specular.getFloatArray();
	}
	
	
	public void setTextureFilename(String arg0) {
		textureFilename.set(arg0);
		textureDirty = true;
	}
	public String getTextureFilename() {
		return textureFilename.get();
	}

	public boolean isLit() {
		return isLit.get();
	}

	public void setLit(boolean isLit) {
		this.isLit.set(isLit);
	}
}
