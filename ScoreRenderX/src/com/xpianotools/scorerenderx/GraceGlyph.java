 /*************************************************************************
  * 
  * XPianoTools
  * http://www.xpianotools.com
  * 
  *  [2014] XPianoTools 
  *  All Rights Reserved.
  * 
  * NOTICE:  All information contained herein is, and remains
  * the property of XPianoTools and its suppliers,
  * if any.
  * 
  * Developer: Georgy Osipov
  * 	developer@xpianotools.com
  * 	gaosipov@gmail.com
  * 
  * 2014-15-08
  *************************************************************************/

package com.xpianotools.scorerenderx;

import com.xpianotools.scorerenderx.StaffGroup.Staff;

import android.graphics.Canvas;

public class GraceGlyph extends GlyphSet.Elem {
	boolean slash;
	Measure measure;
	private StaffGroup staffs = null;
	
	static float magFactor = 1.5f;
	
	public GraceGlyph(GlyphSet glyphSet, Measure measure, boolean slash) {
		glyphSet.super(GlyphSet.GRACE);
		this.slash = slash;
		this.measure = measure;
	}

	@Override public float top() {
		return 0;
	}

	@Override public float bottom() {
		return 4;
	}

	@Override public float width() {
		return measure.minimumWidth()/magFactor;
	}

	@Override public void draw(PaintContext paint, Canvas canvas) {
		//canvas.save();
		//staff.applyMagFactor(canvas,1/magFactor);
		//this.staff.getLineY(line)
		measure.draw(paint, canvas);
		//canvas.restore();
	}
	
	@Override public void setPosition(Staff staff, float x) {
		//staffs  = staff.getStaffGroup().clone(1/magFactor);
		//staffs  = staff.getStaffGroup().clone(1);
		this.staff  = staff;
		this.staffs = staff.getStaffGroup();
		
		measure.setStaffs(staffs);
		measure.position(x * magFactor, measure.minimumWidth(), staffs);
		measure.createGlyphs();
	}

	@Override public void layout() {
		measure.setMagFactor(1/magFactor);
		measure.layout();
	}
}
