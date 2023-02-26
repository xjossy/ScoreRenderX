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
import android.graphics.RectF;

import com.xpianotools.scorerenderx.MusicDrawer.Glyphs;
import com.xpianotools.scorerenderx.MusicDrawer.SvgResource;
import com.xpianotools.scorerenderx.Note.Dot;

public class Rest extends Chord {
	private MusicDrawer mDrawer;
	MusicDrawer.NoteLength mLength;
	GlyphSet leftGlyphs  = new GlyphSet(true );
	GlyphSet rightGlyphs = new GlyphSet(false);
	
	@Override
	public GlyphSet getGlyphs(boolean leftHand) {
		return leftHand ? leftGlyphs : rightGlyphs;
	}
	
	Float minV = null;
	Float maxV = null;
	
	Rest(MusicDrawer drawer, MusicDrawer.NoteLength length) {
		mLength = length;
		mDrawer = drawer;
		translateGlyphs();
	}
	
	@Override
	public int getFlags() {
		return mLength.getFlags();
	}
	
	static float wholeRestWidth() {
		return 6.f / 5;
	}
	
	@Override
	public boolean isEmpty() {
		return false;
	}
	
	@Override
	public void drawHeads(PaintContext paint, Canvas canvas) {
		float centerLine = getCenterLine();
		if( mLength.getPower() == MusicDrawer.NoteLength.L_WHOLE.getPower() ) {
			float line = centerLine - 0.5f;
			
			canvas.drawRect(
				staff.getX(getX() - wholeRestWidth() / 2),
				staff.getLineY(line),
				staff.getX(getX() + wholeRestWidth() / 2),
				staff.getLineY(line + 2.f/3),
				paint.rests);
			
			PaintLogger.logRect(canvas,
					staff.getX(getX() - wholeRestWidth() / 2),
					staff.getLineY(line),
					staff.getX(getX() + wholeRestWidth() / 2),
					staff.getLineY(line + 2.f/3));
		} else if( mLength.getPower() == MusicDrawer.NoteLength.L_HALF.getPower() ) {
			float line = centerLine + 0.5f;
			
			canvas.drawRect(
				staff.getX(getX() - wholeRestWidth() / 2),
				staff.getLineY(line),
				staff.getX(getX() + wholeRestWidth() / 2),
				staff.getLineY(line - 2.f/3),
				paint.rests);
			
			PaintLogger.logRect(canvas,
					staff.getX(getX() - wholeRestWidth() / 2),
					staff.getLineY(line),
					staff.getX(getX() + wholeRestWidth() / 2),
					staff.getLineY(line - 2.f/3));
		} else if( mLength.getPower() == MusicDrawer.NoteLength.L_QUATER.getPower() || 
				   mLength.getPower() == MusicDrawer.NoteLength.L_EIGHTH.getPower() ) {
			float line = centerLine + 0.5f;
			
			SvgResource pause = staff.getFont().get(
				(mLength.getPower() == MusicDrawer.NoteLength.L_QUATER.getPower()) 
					? Glyphs.REST_QUATER : Glyphs.REST_EIGHT
			);
			SvgResource.Transformed glyph = staff.putOnStaff(pause, line - 1, line + 1, getX());
			glyph.traslate( - glyph.getBounds().width() / 2, 0f);
			glyph.draw(paint.rests, canvas);
		} else if( mLength.getPower() > MusicDrawer.NoteLength.L_EIGHTH.getPower() ) {
			int flags = 1 + mLength.getPower() - MusicDrawer.NoteLength.L_EIGHTH.getPower();
			float line = centerLine + flags + .5f;
			
			SvgResource.Transformed glyph = staff.putOnStaff(
					staff.getFont().get(Glyphs.REST_EIGHT), line - 2, line, getX());
			glyph.traslate( - glyph.getBounds().width() / 2, 0f);
			glyph.draw(paint.rests, canvas);
			
			SvgResource glyphFlag = staff.getFont().get(Glyphs.REST_FLAG);
			
			for(int f = 1; f<flags;++f) {
				glyph = glyphFlag.rectTo(glyphFlag.getRect("original"), glyph.getBounds());
				glyph.draw(paint.rests, canvas);
			}
		}
	}
	
	@Override public RectF getHeadsRect(float x, RectF rect) {
		if( mLength.getPower() == MusicDrawer.NoteLength.L_WHOLE.getPower() ) {
			rect.set(x - wholeRestWidth() / 2,0,x + wholeRestWidth() / 2,5);
		} else if( mLength.getPower() == MusicDrawer.NoteLength.L_HALF.getPower() ) {
			rect.set(x - wholeRestWidth() / 2,0,x + wholeRestWidth() / 2,5);
		} else if( mLength.getPower() == MusicDrawer.NoteLength.L_QUATER.getPower() || 
				   mLength.getPower() == MusicDrawer.NoteLength.L_EIGHTH.getPower() ) {
			SvgResource pause = mDrawer.font.get(
				(mLength.getPower() == MusicDrawer.NoteLength.L_QUATER.getPower()) 
					? Glyphs.REST_QUATER : Glyphs.REST_EIGHT
			);
			SvgResource.Transformed glyph = pause.boundsTo(x, - 1, + 1);
			glyph.traslate( - glyph.getBounds().width() / 2, 0f);
			return glyph.getBounds(rect);
		} else if( mLength.getPower() > MusicDrawer.NoteLength.L_EIGHTH.getPower() ) {
			int flags = 1 + mLength.getPower() - MusicDrawer.NoteLength.L_EIGHTH.getPower();
			
			SvgResource.Transformed glyph = 
					mDrawer.font.get(Glyphs.REST_EIGHT).boundsTo(x, - 2, 0);
			glyph.traslate( - glyph.getBounds().width() / 2, 0f);
			float left = glyph.getBounds().left; 
			
			SvgResource glyphFlag = mDrawer.font.get(Glyphs.REST_FLAG);
			
			for(int f = 1; f<flags;++f) {
				glyph = glyphFlag.rectTo(glyphFlag.getRect("original"), glyph.getBounds());
			}
			float right = glyph.getBounds().right;
			rect.set(left,0,right,5);
		} else rect.set(x,0,x,5);
		return rect;
	}
	
	public float getCenterLine() {
		if( mLength.getPower() == MusicDrawer.NoteLength.L_WHOLE.getPower() ) {
			float line = 1;
			if( minV!=null ) line = (float)Math.ceil(Math.max(minV + 1, 4));
			if( maxV!=null ) line = (float)Math.floor(Math.min(maxV - 1, 0));
			
			return line + 0.5f;
		} else if( mLength.getPower() == MusicDrawer.NoteLength.L_HALF.getPower() ) {
			float line = 2;
			if( minV!=null ) line = (float)Math.ceil(Math.max(minV + 1, 4));
			if( maxV!=null ) line = (float)Math.floor(Math.min(maxV - 1, 0));
			
			return line - 0.5f;
		} else if( mLength.getPower() == MusicDrawer.NoteLength.L_QUATER.getPower() || 
				   mLength.getPower() == MusicDrawer.NoteLength.L_EIGHTH.getPower() ) {
			float line = 2; //center line of drawing
			if( minV!=null ) line = (float)Math.ceil (Math.max(minV + 1.0,  4));
			if( maxV!=null ) line = (float)Math.floor(Math.min(maxV - 1.0f, 0));
			
			return line - .5f;
		} else if( mLength.getPower() > MusicDrawer.NoteLength.L_EIGHTH.getPower() ) {
			int flags = 1 + mLength.getPower() - MusicDrawer.NoteLength.L_EIGHTH.getPower();
			float line = 3 + flags / 2; //low line of drawing
			if( minV!=null ) line = (float)Math.ceil(Math.max(minV + flags + 1,  3 + flags));
			if( maxV!=null ) line = (float)Math.floor(Math.min(maxV, 2));
			
			return line - flags - 0.5f;
		}
		return 0;
	}
	
	@Override
	public void setMinV(float v) {
		minV = v;
		translateGlyphs();
	}
	
	@Override
	public void setMaxV(float v) {
		maxV = v;
		translateGlyphs();
	}
	
	private void translateGlyphs() {
		leftGlyphs.translate = getCenterLine();
		rightGlyphs.translate = getCenterLine();
	}

	public void addEnlarger() {
		rightGlyphs.addGlyph(GlyphSet.ENLARGER, 0, new Dot());
	}
}
