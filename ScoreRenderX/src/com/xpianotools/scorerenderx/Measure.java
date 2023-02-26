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
import java.util.HashSet;

import android.graphics.Canvas;
import android.graphics.RectF;

public class Measure extends FunctionValueCacher {
	RationalNumber offset;
	RationalNumber length;
	
	StaffGroup staffGroup = null;
	
	int index = 0;
	Notation notation = null;
	
	//static final float minimumPadding = 0.5f;
	
	static final float normalSpace = 2.5f;
	static final float lengthInc   = 1.1f;
	
	public Meter meter;
	
	public ArrayList<Clef> startClefs;
	public ArrayList<Clef> currentClefs;

	public Tonality startTonality;
	public Tonality currentTonality;
	
	public Measure(RationalNumber offset, RationalNumber length, Tonality tonality, ArrayList<Clef> clefs, Meter meter, int index) {
		this.offset = offset;
		this.length = length;
		
		this.meter = meter;
		
		startClefs = currentClefs = clefs;
		startTonality = currentTonality = tonality;
		
		this.index = index;
	}

	//sorted by time
	ArrayList<GraphicalMoment>   moments      = new ArrayList<GraphicalMoment>();
	//HashSet<StaffDrawableMarker> markers      = new HashSet<StaffDrawableMarker>();
	HashSet<StaffGlyph.Builder> glyphBuilders = new HashSet<StaffGlyph.Builder>(); 
	
	public int getUpperBound(RationalNumber time) {
		int idx1 = 0;
		int idx2 = moments.size();
		
		while( idx2 != idx1  ) {
			int idx = (idx1 + idx2) / 2;
			if( moments.get(idx).getTime().compareTo(time) <= 0 ) {
				idx1 = idx + 1;
			} else {
				idx2 = idx;
			}
		}
		
		return idx1;
	}
	
	public GraphicalMoment getMomentByTime(RationalNumber time) {
		int idx = getUpperBound(time);
		if( idx<=0 || moments.isEmpty() ||!moments.get(idx-1).getTime().equals(time) ) return null;
		return moments.get(idx-1);
	}
	
	public GraphicalMoment getMoment(int index) {
		if( index<0 || index>=moments.size() ) return null;
		return moments.get(index);
	}
	
	public int getMoments() {
		return moments.size();
	}
	
	public void addMoment(GraphicalMoment moment) {
		if( staffGroup != null ) {
			moment.setStaffs(staffGroup);
		}
		moments.add(getUpperBound(moment.getTime()), moment);
		moment.setMeasure(this);
		moment.setTonality(currentTonality);
		moment.setClefs(currentClefs);
		updated();
	}
	/*public void addMarker(StaffDrawableMarker marker) {
		markers.add(marker);
		updated();
	}*/
	public void addGlyphBuilder(StaffGlyph.Builder builder) {
		glyphBuilders.add(builder);
		updated();
	}
	
	public float getSideSize(boolean left) {
		if( moments.isEmpty() ) return 0;
		int l = left ? 0 : moments.size() - 1;
		return moments.get(l).getPadding(left) + getSideSize(l,left);
	}
	
	//TODO: add caching for minimumLength()
	public float calcGlue(RationalNumber length) {
		if( length.compareTo(0) <= 0 ) return 0; 
		double minLng = Math.min(1/8., minimumLength().getDoubleValue());
		double space = Math.log(length.getDoubleValue() / minLng) / Math.log(2);
		return (float)(space) * lengthInc + normalSpace;
	}
	
	public float getGlueAmount() {
		float glue = 0;
		for(int i=0;i<moments.size();++i ) {
			glue += calcGlue(getIthLength(i));
		}
		
		return glue;
	}
	
	//CachedValueF cNormalWidth = new CachedValueF();
	float normalWidth = 0;
	public float normalWidth() {
		return normalWidth;
	}
	
	private void calculateNormalWidth() {
		float wd = 0;
		
		for(int i=0;i<moments.size();++i ) {
			wd += Math.max(calcGlue(getIthLength(i)), getMinDst(i));
		}
		wd += getSideSize(true);
		
		normalWidth = wd;
	}

	float minimumWidth = 0;
	public float minimumWidth() {
		return minimumWidth;
	}
	
	private void calculateMinimumWidth() {
		float result = 0;
		for(int i=0;i<moments.size() - 1;++i) {
			result += getMinDst(i);
		}
		result += getSideSize(true);
		result += getSideSize(false);
		
		minimumWidth = result;
	}
	
	public void position(float x, float width, StaffGroup g) {
		if( moments.isEmpty() ) return;
			
		ArrayList<Float> offsets = new ArrayList<Float>(Collections.<Float>nCopies(moments.size(),null));
		float allGlue = getGlueAmount();
		
		float currentX = getSideSize(true);
		width -= currentX;

		float widthMinus = 0;
		float  glueMinus = 0;
		
		Rest rests[] = new Rest[g.staffs.size()];
		boolean onlyRests[] = new boolean[g.staffs.size()];
		
		//find items when demanded width is too small
		//and place them on minimum possible width (getMinDst)
		do {
			widthMinus = 0;
			glueMinus = 0;
			for(int i=0;i<moments.size();++i) {
				if( offsets.get(i) == null) {
					float glue = calcGlue(getIthLength(i));
					//float demandedWidth = width * glue / allGlue;
					float minWidth = getMinDst(i);
					if( width * glue <= minWidth * allGlue
							/*demandedWidth < minWidth*/ ) {
						widthMinus += minWidth;
						glueMinus  += glue;
						offsets.set(i,minWidth); 
					}
				}
			}
			width   -= widthMinus;
			allGlue -= glueMinus;
		} while (widthMinus > 0);
		
		//calc another items on demanded offset and place all chords
		for(int i=0;i<moments.size();++i) {
			for(int staff=0;staff<g.staffs.size();++staff) {
				if( onlyRests[staff] ) continue;
				
				Rest rest = moments.get(i).getOnlyRest(staff);
				if( rest!=null ) {
					if( rests[staff]!=null ) {
						onlyRests[staff] = true; continue;
					}
					rests[staff] = rest;
				} else if(!moments.get(i).isStaffEmpty(staff) ) {
					onlyRests[staff] = true;
				}
			}
			if( offsets.get(i) == null) {
				float glue = calcGlue(getIthLength(i));
				
				float demandedWidth = allGlue > 0 ? width / allGlue * glue : 0;
				offsets.set(i,demandedWidth);
			}
			moments.get(i).setX(currentX + x);
			
			if( i==moments.size()-1 ) {
				moments.get(i).setR(x + width);
				break;
			}
			currentX += offsets.get(i); 
			moments.get(i).setR(currentX);
		}
		for(int staff=0;staff<g.staffs.size();++staff ) {
			if( onlyRests[staff] || rests[staff]==null ) continue;
			rests[staff].setOffset(x + width/2 - rests[staff].getX());
		}
	}
	
	public RationalNumber getStartTime() {
		return this.offset; 
	}
	
	public RationalNumber getEndTime() {
		return getStartTime().add(this.length);
	}
	
	//get time of ith moment, or time of end of measure if i >= moments.size() 
	public RationalNumber getIthTime(int i) {
		if( i>=moments.size() ) {
			return getEndTime(); 
		}
		
		return moments.get(i).getTime();
	}
	
	//get width of one side of moment chord graphical 
	public float getSideSize(int i, boolean left) {
		RectF r = moments.get(i).getRelativeRect();
		return left ? - r.left : r.right;
	}
	
	// return minimum possible graphical distance between i'th and i+1'th moment 
	// i from 0 to size - 1
	public float getMinDst(int i) {
		return minDsts[i];
	}
	float minDsts[] = {};
	private void calculateMinDsts() {
		minDsts = new float[moments.size()];
		for(int i=0;i<minDsts.length-1;++i) {
			minDsts[i] = Math.max(moments.get(i).getPadding(false), moments.get(i+1).getPadding(true)) 
			+ getSideSize(i, false) + getSideSize(i+1,true);
		}
		if( moments.size()>0 )
			minDsts[moments.size()-1] = getSideSize(false);
	}
	
	//
	public RationalNumber getIthLength(int i) {
		return getIthTime(i+1).subtract(getIthTime(i));
	}
	
	//return minimum time between moments (getIthLength) in this measure 
	public RationalNumber minimumLength() {
		return minimumLength;
	}
	RationalNumber minimumLength;
	private void calculateMinimumLength() {
		//res equal time from last note to measure end:
		RationalNumber res = this.length;
		
		for(int i=0;i<moments.size();++i) {
			RationalNumber cdiff = getIthLength(i); 
			if( res.compareTo(cdiff) > 0 && cdiff.compareTo(0) > 0 ) {
				res = cdiff;
			}
		}
		
		minimumLength = res;
	}
	
	public float badness(float width) {
		float normal = normalWidth();
		float min    = minimumWidth();
		
		if( width<min ) return 10000;
		
		return (float) Math.abs(Math.log1p((width - normal) * 3 / (normal - min) / 4));
	}

	public void layout() {
		//FOR PERFORMANCE REASONS NOT for(GraphicalMoment m : moments) {
		for(int i=0;i<moments.size();++i) {
			GraphicalMoment m = moments.get(i);
			m.layout();
		}
		calculateMetrics();
	}

	public void draw(PaintContext paint, Canvas canvas) {
		//FOR PERFORMANCE REASONS NOT for(GraphicalMoment m : moments) {
		for(int i=0;i<moments.size();++i) {
			GraphicalMoment m = moments.get(i);
			m.draw(paint, canvas);
		}
		/*for(StaffDrawableMarker m : markers) {
			m.draw(paint, canvas);
		}*/
	}

	public void setStaffs(StaffGroup staffGroup) {
		this.staffGroup = staffGroup; 
		//FOR PERFORMANCE REASONS NOT for(GraphicalMoment m : moments) {
		for(int i=0;i<moments.size();++i) {
			GraphicalMoment m = moments.get(i);
			m.setStaffs(staffGroup);
		}
	}

	public void setMagFactor(float magFactor) {
		//FOR PERFORMANCE REASONS NOT for(GraphicalMoment m : moments) {
		for(int i=0;i<moments.size();++i) {
			GraphicalMoment m = moments.get(i);
			m.setMagFactor(magFactor);
		}
		for(StaffGlyph.Builder builder : glyphBuilders) {
			builder.setMagFactor(magFactor);
		}
	}
	
	public GraphicalMoment findNextNoteMomentOnStaff(RationalNumber time, int staff) {
		Measure next = this;
		while(next != null ) {
			for(int index = next.getUpperBound(time);index<next.moments.size();++index) {
				if( next.moments.get(index).hasNoteMomentOnStaff(staff) ) return next.moments.get(index); 
			}
			next = notation.getMeasure(next.index+1);
		}
		
		return null;
	}

	public float scaledWidth(float scale) {
		float cWidth    = normalWidth();
		float cMinWidth = minimumWidth();
		return cMinWidth + (cWidth - cMinWidth) * scale;
	}

	public void createGlyphs() {
		for(StaffGlyph.Builder builer : glyphBuilders) {
			builer.buildGlyphs();
		}
	}
	
	//can use normalWidth(), minimumWidth(), etc only after this call!
	private void calculateMetrics() {
		//calculateEndTime();
		//calculateIthLength();
		calculateMinimumLength();
		calculateMinDsts();
		calculateMinimumWidth();
		calculateNormalWidth();
	}

	public void changeTonality(Tonality tonality) {
		currentTonality = tonality;
	}

	public void changeClefs(ArrayList<Clef> clefs) {
		currentClefs = clefs;
	}
}
