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

import android.graphics.Canvas;

import com.xpianotools.scorerenderx.StaffGroup.Staff;

public abstract class DrawableOnStaff {
	public abstract float getWidth();
	public abstract float getAboveHeight();
	public abstract float getBelowHeight();
	public float getHeight() {
		return getAboveHeight() + getBelowHeight(); 
	}
	
	public abstract void draw(PaintContext paint, Canvas canvas, Staff staff, float line, float x);
}
