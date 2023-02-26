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

public class Slur extends ChordBasedGlyphBuilder {
	@Override
	protected StaffGlyph buildGlyph(int first, int last, StaffGroup staffs) {
		return new Glyph(first, last);
	}
	
	private float mag;
	public void setMagFactor(float mag) {
		this.mag = mag;
	}
	
	class Glyph extends ChordBasedGlyphBuilder.Glyph {
		SlurBezier bezier = null;
		public static final float sideEnlarge = 2;
		
		Glyph(int first, int last) {
			super(first,last);
		}
		
		private Chord lastChord() {
			return chords.get(chords.size() - 1);
		}
		private Chord firstChord() {
			return chords.get(0);
		}
		
		//TODO: cache this!
		private boolean isUp() {
			if( chords.isEmpty() ) return false;
			return !lastChord().markersPosDown();
		}
		private float x1() {
			float x1 = isUp() != firstChord().calculatedStemPos().isUp(0) ?
					firstChord().getXAbs() : firstChord().getXAbs() + .2f;
					
			return isFirst() ? x1 : x1 - sideEnlarge;
		}
		private float x2() {
			float x2 = lastChord().getXAbs();
			return isLast() ? x2 : x2 + sideEnlarge;
		}
		
		static final float minRaise = 1;
		static final float maxRaise = 4;
		
		private float raise() {
			float off = isUp() ? -minRaise : minRaise;
			
			if( chords.size() <= 2 ) return off;
			
			float startY = ( isUp()
					? Math.min(firstChord().getYBoundAbs(isUp()), lastChord().getYBoundAbs(isUp()))
					: Math.max(firstChord().getYBoundAbs(isUp()), lastChord().getYBoundAbs(isUp()))
				);
			
			for(int i=1;i<chords.size() - 1;++i) {
				Chord chord = chords.get(i);
				float cOff = chord.getYBoundAbs(isUp()) - startY;
				off = isUp() ? Math.min(off, cOff) : Math.max(off, cOff); 
			}
			off = isUp() ? Math.max(-maxRaise,off) : Math.min(maxRaise,off);
			
			return off;
		}
		
		static final float minTranslate = .2f;
		
		private void makeCurve() {
			if( chords.size() < 1 ) return;
			bezier = new SlurBezier(
					x1(),firstChord().getYBoundAbs(isUp()), 
					x2(), lastChord().getYBoundAbs(isUp()), 
				raise());
			
			float translate = isUp() ? -minTranslate : minTranslate;
			for(int i=1;i<chords.size() - 1;++i) {
				Chord chord = chords.get(i);
				float cTranslate = chord.getYBoundAbs(isUp()) - bezier.approxAt(chord.getXAbs());
				translate = isUp() ? Math.min(translate, cTranslate) : Math.max(translate, cTranslate); 
			}
			
			bezier.translate(0f,translate);
		}
		
		@Override
		public void draw(PaintContext paint, Canvas canvas) {
			if( chords.size() < 1 ) return;
			
			makeCurve();

			canvas.save();
			canvas.scale(mag, mag);
			bezier.drawSlur(canvas, paint.slurs);
			canvas.restore();
		}

		@Override
		public void layout() {
			sortChords();
		}

		@Override
		public float priority() {
			return 10;
		}
	}
}
