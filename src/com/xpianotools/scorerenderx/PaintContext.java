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

import android.graphics.Matrix;
import android.graphics.Paint;

public class PaintContext {
	public Matrix shaderMatrix = new Matrix();
	public Paint customColor = new Paint();
	
	public Paint glyphs;
	public Paint sideGlyphs;
	public Paint staffs;
	public Paint noteHeads;
	public Paint stems;
	public Paint flags;
	public Paint slurs;
	public Paint beams;
	public Paint accidentals;
	public Paint clefs;
	public Paint meters;
	public Paint keyAccidentals;
	public Paint rests;

	public Paint background;
	
	public PaintContext() {
		customColor.setAntiAlias(true);
	}
	
	public static Paint allocPaint(int color) {
		Paint paint = new Paint();
		paint.setColor(color);
		paint.setAntiAlias(true);
		return paint;
	}
	
	public static PaintContext create(Paint fore, Paint back) {
		PaintContext pc = new PaintContext();
		pc.setPaints(fore, back);
		return pc;
	}
	
	public static PaintContext create(int fore, int back) {
		Paint white = allocPaint(back);
		Paint black = allocPaint(fore);
		
		return create(black, white);
	}
	
	public void setPaints(Paint fore, Paint back) {
		glyphs  = sideGlyphs
				= staffs
				= noteHeads
				= stems
				= flags
				= slurs
				= beams
				= accidentals
				= clefs
				= meters
				= keyAccidentals
				= rests = fore;
		
		background = back;
	}
}
