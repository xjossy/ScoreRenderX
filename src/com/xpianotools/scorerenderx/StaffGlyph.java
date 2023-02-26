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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.graphics.Canvas;

//This class represents some glyph, that can be drawn on staff group (one line) probably cross measure and cross staff
//staff glyph created after spreading measures over lines
public abstract class StaffGlyph implements Comparable<StaffGlyph> {
	public interface Builder {
		void buildGlyphs();
		void setMagFactor(float magFactor);
	}

	protected StaffGroup staffs;
	public void setStaffs(StaffGroup staffs) {
		this.staffs = staffs;
	}
	
	//do all modifications to allow all others glyphs with higher priority to draw correctly
	public abstract void layout();

	//draw on current staff line should be called only after apply()
	public abstract void draw(PaintContext paint, Canvas canvas);
	
	//it's guaranteed that if draw() called that there was apply() call before 
	//for all glyphs on this line with lesser priority()
	public abstract float priority();
	
	public float topY() {
		return Float.POSITIVE_INFINITY;
	}
	public float bottomY() {
		return Float.NEGATIVE_INFINITY;
	}
	
	@Override
	public int compareTo(StaffGlyph r) {
		int res = Float.compare(priority(), r.priority());
		return res != 0 ? res : Integer.valueOf(hashCode()).compareTo(r.hashCode());
	}
}

//for set of chords create one glyph per each line containing chords from the list
//list of corresponding chords 
abstract class ChordBasedGlyphBuilder implements StaffGlyph.Builder {
	protected ArrayList<Chord> chords = new ArrayList<Chord>();
	public void addChord(Chord chord) {
		/*if( chord.moment==null ) return;
		
		int min = 0, max = chords.size();
		RationalNumber time = chord.moment.getTime();
		
		while(min!=max) {
			int pos = (min + max) / 2;
			if( time.compareTo(chords.get(pos).moment.getTime()) < 0 ) {
				max = pos;
			} else {
				min = pos + 1;
			}
		}
		
		chords.add(min,chord);*/
		chords.add(chord);
	}
	
	static final Comparator<Chord> chordComparator = new Comparator<Chord>() {
		@Override public int compare(Chord arg0, Chord arg1) {
			if( arg0.staff != null && arg1.staff != null && arg0.staff.getStaffGroup() == arg1.staff.getStaffGroup() )
				return Integer.signum(arg0.staff.getStaffGroup().getLineNumber() 
						- arg1.staff.getStaffGroup().getLineNumber());
			
			return Float.compare(arg0.getX(),arg1.getX());
		}
	};
	
	public void sortChords() {
		Collections.sort(chords,chordComparator);
	}
	
	public void buildGlyphs() {
		if( chords.isEmpty() ) return;
		
		int lineStart = 0;
		int cIndex = 0;
		StaffGroup prevLine = null;
		
		for(Chord chord : chords) {
			try {
				if( chord.staff == null ) continue;
				StaffGroup staffs = chord.staff.getStaffGroup();
				if( prevLine==null ) {
					prevLine = staffs; 
				}
				if( prevLine!=staffs ) {
					prevLine.addGlyph(buildGlyph(lineStart, cIndex, prevLine));
					lineStart = cIndex + 1;
					
					prevLine = staffs;
				}
			} finally {
				cIndex++;
			}
		}
		if( lineStart!=cIndex && prevLine!=null )
			prevLine.addGlyph(buildGlyph(lineStart, cIndex, prevLine));
	}
	
	protected abstract StaffGlyph buildGlyph(int first, int last, StaffGroup staffs);
	protected abstract class Glyph extends StaffGlyph {
		protected int firstChord; // index of first chord on corresponding line
		protected int  lastChord; // index of last  chord on corresponding line
		protected List<Chord> chords;
		
		protected boolean isFirst() {
			return firstChord == 0;
		}
		
		protected boolean isLast() {
			return lastChord == ChordBasedGlyphBuilder.this.chords.size();
		}
		
		public Glyph(int first, int last) {
			firstChord = first;
			lastChord = last;
			
			chords = new ArrayList<Chord>(ChordBasedGlyphBuilder.this.chords.subList(firstChord, lastChord));
		}
	}
}