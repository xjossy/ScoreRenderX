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

import android.graphics.Canvas;
import android.graphics.RectF;

//This class describes set of notes (chords) according to one moment of time 
public abstract class GraphicalMoment {
	RationalNumber time;
	float x = 0;
	float r = 0; // end of place, corresponding to this moment
	double tempoFactor = 0;
	public Measure measure;
	long millisTime = 0;
	
	private RectF headsRect = null;
	
	public Tonality tonality = null;
	public ArrayList<Clef> clefs = null;

	public GraphicalMoment(RationalNumber time) {
		this.time = time;
	}
	
	protected final RectF getHeadsRect() {
		return headsRect;
	}
	
	//public abstract float getHeadsSidePadding(boolean leftSide);
	public abstract ArrayList<Chord> getChords();
	public abstract RectF getRelativeRect();
	
	protected StaffGroup staffGroup;
	
	public void layout() {
		RectF tmpRect = new RectF();
		headsRect = new RectF();
		
		ArrayList<Chord> chords = getChords();
		headsRect.set(0,0,0,0);
		
		if( chords.isEmpty() ) return;
		
		//FOR PERFORMANCE REASONS NOT for(Chord chord : chords) {
		for(int i=0;i<chords.size();++i) {
			Chord chord = chords.get(i);
			
			RectF cRect = chord.getHeadsRect(chord.getX() - x, tmpRect);
			if( i==0 ) {
				headsRect.set(cRect);
				continue;
			}
			headsRect.left   = Math.min(headsRect.left  , cRect.left  );
			headsRect.right  = Math.max(headsRect.right , cRect.right );
			headsRect.top    = Math.min(headsRect.top   , cRect.top   );
			headsRect.bottom = Math.max(headsRect.bottom, cRect.bottom);
		}
	}
	public abstract void draw(PaintContext paint, Canvas canvas);
	public abstract void setX(float x);
	
	public float getAbsoluteX() {
		return staffGroup.getX(x);
	}
	
	public void setStaffs(StaffGroup staffGroup) {
		this.staffGroup = staffGroup;
	}
	
	public StaffGroup getStaffs() {
		return staffGroup;
	}

	public void setR(float r) {
		this.r = r;
	}

	public RationalNumber getTime() {
		return time;
	}

	public float getPadding(boolean left) {
		return .5f;
	}

	public boolean hasNoteMomentOnStaff(int staff) {
		return false;
	}

	public void setMeasure(Measure measure) {
		this.measure = measure;
	}

	public boolean isStaffEmpty(int staff) {
		return true;
	}
	
	//return rest if there are nothing but rest on specified staff
	public Rest getOnlyRest(int staff) {
		return null;
	}
	
	public long getMillisTime() {
		return millisTime;
	}
	
	public double getTempoFactor() {
		return tempoFactor;
	}

	public void setMillis(long millis, double tempoFactor) {
		this.millisTime = millis;
		this.tempoFactor = tempoFactor; 
	}
	
	public boolean hasSound() {
		return false;
	}
	
	public Note getNoteByPitch(int midiNote) {
		return null;
	}
	
	public void onUpdate() {
		if( measure!=null ) measure.updated();
	}

	public void setTonality(Tonality currentTonality) {
		this.tonality = currentTonality;
	}
	
	public void setClefs(ArrayList<Clef> clefs) {
		this.clefs = clefs;
	}

	public void setMagFactor(float mag) {
		ArrayList<Chord> chords = getChords();
		//FOR PERFORMANCE REASONS NOT for(Chord chord : chords) {
		for(int i=0;i<chords.size();++i) {
			Chord chord = chords.get(i);
			chord.setMagFactor(mag);
		}
	}
}