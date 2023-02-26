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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;
import java.util.Map.Entry;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.SparseArray;

import com.xpianotools.scorerenderx.StaffGroup.Staff;

//namespace-typed class
public class GraphicalMoments {
	public abstract static class OneStaffGraphicalMoment extends GraphicalMoment {
		Staff staff = null;
		
		GlyphSet sideGlyphsL = null;
		GlyphSet sideGlyphsR = null;
		
		RectF rect = new RectF();
		
		public OneStaffGraphicalMoment(RationalNumber time) {
			super(time);
		} 
		
		public GlyphSet getSideGlyphs(boolean leftSide) {
			GlyphSet glyphs = new GlyphSet(leftSide);
			ArrayList<Chord> chords = getChords();
			//FOR PERFORMANCE REASONS NOT for(Chord chord : chords) {
			for(int i=0;i<chords.size();++i) {
				Chord chord = chords.get(i);
				chord.appendGlyphs(glyphs,leftSide);
			}
			
			return glyphs;
		}
		
		public RectF getRelativeRect() {
			return rect;
		}
		
		@Override public void layout() {
			super.layout();
			
			sideGlyphsL = getSideGlyphs(true);
			sideGlyphsR = getSideGlyphs(false);
			
			sideGlyphsL.layout();
			sideGlyphsR.layout();
			
			rect = getHeadsRect();
			rect.left  -= sideGlyphsL.getWholeWidth();
			rect.right += sideGlyphsR.getWholeWidth();
		}
		
		@Override public void setX(float x) {
			this.x = x;
			RectF rect = getHeadsRect();
			if( staff!=null ) {
				sideGlyphsL.setPosition(staff,rect.left  + x + sideGlyphsL.getWholeWidth());
				sideGlyphsR.setPosition(staff,rect.right + x - sideGlyphsR.getWholeWidth());
			}
		}
		
		@Override public void draw(PaintContext paint, Canvas canvas) {
			sideGlyphsL.draw(paint, canvas);
			
			ArrayList<Chord> chords = getChords();
			//FOR PERFORMANCE REASONS NOT for(Chord chord : chords) {
			for(int i=0;i<chords.size();++i) {
				Chord chord = chords.get(i);
				chord.draw(paint, canvas);
			}
			
			sideGlyphsR.draw(paint, canvas);
		}
		@Override
		public void setStaffs(StaffGroup staffGroup) {
			staff = staffGroup.get(0);
			setStaff(staff);
		}
		public void setStaff(Staff staff) {
			super.setStaffs(staff.getStaffGroup());

			this.staff = staff;
			ArrayList<Chord> chords = getChords();
			for(Chord chord : chords) {
				chord.setStaff(staff);
			}
		}
		public boolean isEmpty() {
			ArrayList<Chord> chords = getChords();
			for(Chord chord : chords) {
				if(!chord.isEmpty() ) return false;
			}
			return true;
		}
		@Override
		public void setMeasure(Measure measure) {
			super.setMeasure(measure);
			
			ArrayList<Chord> chords = getChords();
			for(Chord chord : chords) {
				measure.addGlyphBuilder(chord);
			}
		}
		@Override 
		public boolean isStaffEmpty(int staff) {
			return this.staff.index!=staff || isEmpty();
		}
		public boolean hasNoteMomentOnStaff(int staff) {
			return this.staff.index == staff && !isEmpty();
		}

		@Override
		public Rest getOnlyRest(int staff) {
			if( this.staff.index!=staff ) return null;
			
			Rest res = null;
			
			ArrayList<Chord> chords = getChords();
			for(Chord chord : chords) {
				if( chord.isEmpty() ) continue;
				if( res!=null ) return null;
				if(!(chord instanceof Rest) ) return null;
				
				res = (Rest)chord;
			}
			
			return res;
		}
		
		@Override
		public boolean hasSound() {
			return true;
		}
		
		@Override
		public Note getNoteByPitch(int midiNote) {
			ArrayList<Chord> chords = getChords();
			for(Chord chord : chords) {
				Note note = chord.getNoteByPitch(midiNote);
				if( note!=null ) return note;
			}
			return null;
		}
	}

	public static class OneVoiceMoment extends OneStaffGraphicalMoment {
		private ArrayList<Chord> chords = new ArrayList<Chord>(Arrays.<Chord>asList((Chord)null));
		public ArrayList<Chord> getChords() {
			return chords;
		}
		
		OneVoiceMoment(RationalNumber time, Chord chord) {
			super(time);
			
			chords.set(0, chord);
			chord.moment = this;
			setX(0);
		}

		public void setX(float x) {
			chords.get(0).setX(x);
			this.x = x;
			super.setX(x);
		}
	}

	public static class MultipleVoiceMoment extends OneStaffGraphicalMoment {
		private ArrayList<Chord> chords;
		public ArrayList<Chord> getChords() {
			return chords;
		}
		
		MultipleVoiceMoment(RationalNumber time, Collection<Chord> chords) {
			this(time,new ArrayList<Chord>(chords));
		}
		MultipleVoiceMoment(RationalNumber time, ArrayList<Chord> chords) {
			super(time);
			this.chords = chords;
			for(Chord chord : this.chords) chord.moment = this;
			
			setX(0);
		}
		
		public void setX(float x) {
			int i;
			for(i=0;i<chords.size()/2;++i) {
				Chord.positionChords(chords.get(i*2), chords.get(i*2+1), x);	
			}
			for(;i<chords.size();++i) {
				chords.get(i).setX(x);
			}
			this.x = x;
			
			super.setX(x);
		}
	}

	public static class MultipleStaffMoment extends GraphicalMoment {
		private RectF rect = new RectF();
		
		private SparseArray<OneStaffGraphicalMoment> staffs = new SparseArray<OneStaffGraphicalMoment>();
		
		private ArrayList<Chord> chords = new ArrayList<Chord>();
		public  ArrayList<Chord> getChords() {
			return chords;
		}
		
		MultipleStaffMoment(RationalNumber time) {
			super(time);
		}
		
		MultipleStaffMoment addStaff(int staff, OneStaffGraphicalMoment moment) {
			staffs.put(staff,moment);
			chords.addAll(moment.getChords());
			
			return this;
		}

		@Override public void setStaffs(StaffGroup staffGroup) {
			super.setStaffs(staffGroup);
			
			for( int i=0; i<staffGroup.size(); ++i ) {
				OneStaffGraphicalMoment m = staffs.get(i); 
				if( m!=null ) m.setStaff(staffGroup.get(i));
			}
		}

		@Override public RectF getRelativeRect() {
			return rect;
		}
		
		@Override
		public void setMeasure(Measure measure) {
			super.setMeasure(measure);
			for(int i=0;i<staffs.size();++i) {
				OneStaffGraphicalMoment staff = staffs.valueAt(i);
				staff.setMeasure(measure);
			}
		}

		@Override
		public void setX(float x) {
			for(int i=0;i<staffs.size();++i) {
				OneStaffGraphicalMoment staff = staffs.valueAt(i);
				staff.setX(x);
			}
			this.x = x;
		}
		
		@Override public void layout() {
			super.layout();
			
			rect.set(0,0,0,0);
			for(int i=0;i<staffs.size();++i) {
				OneStaffGraphicalMoment staff = staffs.valueAt(i);
				staff.layout();
				
				RectF cur = staff.getRelativeRect();
				if( i==0 ) {
					rect.set(cur);
					continue;
				}
				rect.left  = Math.min(rect.left , cur.left );
				rect.right = Math.max(rect.right, cur.right);
			}
		}

		@Override
		public void draw(PaintContext paint, Canvas canvas) {
			for(int i=0;i<staffs.size();++i) {
				OneStaffGraphicalMoment staff = staffs.valueAt(i);
				staff.draw(paint, canvas);
			}
		}
		
		@Override
		public boolean hasNoteMomentOnStaff(int staff) {
			OneStaffGraphicalMoment moment = staffs.get(staff);
			if( moment == null ) return false;
			
			return moment.hasNoteMomentOnStaff(staff);
		}

		@Override 
		public boolean isStaffEmpty(int staff) {
			OneStaffGraphicalMoment moment = staffs.get(staff);
			if( moment == null ) return true;
			
			return moment.isStaffEmpty(staff);
		}
		
		@Override 
		public Rest getOnlyRest(int staff) {
			OneStaffGraphicalMoment moment = staffs.get(staff);
			if( moment == null ) return null;
			
			return moment.getOnlyRest(staff);
		}
		
		@Override
		public boolean hasSound() {
			return true;
		}

		@Override
		public Note getNoteByPitch(int midiNote) {
			for(int i=0;i<staffs.size();++i) {
				OneStaffGraphicalMoment staff = staffs.valueAt(i);
				Note note = staff.getNoteByPitch(midiNote);
				if( note!=null ) return note;
			}
			return null;
		}
	}

	public static class ClefMark extends GraphicalMoment {
		public MusicDrawer musicDrawer; 
		public TreeMap<Integer,Clef> clefs = new TreeMap<Integer,Clef>();
		private static ArrayList<Chord> emptyArray = new ArrayList<Chord>();
		
		boolean fullSize = false;
		float   smallClefOffset = .25f;
		
		private RectF rect = new RectF();
		
		ClefMark(RationalNumber time, MusicDrawer musicDrawer) {
			super(time);
			this.musicDrawer = musicDrawer;
		}
		
		public void setClef(int staff, Clef clef) {
			clefs.put(staff, clef);
		}
		
		@Override
		public void setX(float x) {
			this.x = x;
		}
		
		public float width() {
			return rect.width();
		}
		
		private float calculateWidth() {
			float width = 0;
			for(Clef clef : clefs.values()){
				width = Math.max(width, clef.width(musicDrawer) );
			}
			if(!fullSize ) width *= (1 - smallClefOffset);
			return width;
		}
		
		@Override public RectF getRelativeRect() {
			return rect;
		}
		
		public void layout() {
			super.layout();
			
			float w = calculateWidth();
			
			rect.left  = - w/2;
			rect.right = + w/2;
		}
		
		@Override
		public ArrayList<Chord> getChords() {
			return emptyArray;
		}
		
		@Override
		public void draw(PaintContext paint, Canvas canvas) {
			float smaller = fullSize ? 0 : smallClefOffset;
			for(Entry<Integer,Clef> entry : clefs.entrySet() ) {
				Staff staff = staffGroup.get(entry.getKey());
				staff.clef = entry.getValue(); 
				staff.drawOnStaff(
						paint.clefs, canvas, 
						entry.getValue().getResource(musicDrawer), 
						entry.getValue().line - 1 + smaller, 
						entry.getValue().line + 1 - smaller,
						x - entry.getValue().width(musicDrawer) / 2 
					);
			}
		}
		
		public RectF getClefRect(int line) {
			RectF rect = new RectF();

			Clef clef = clefs.get(line);
			Staff staff = staffGroup.get(line);
			
			if( clef!=null && staff!=null ) {
				float smaller = fullSize ? 0 : smallClefOffset;
				clef.getResource(musicDrawer).boundsTo(
						x - clef.width(musicDrawer) / 2 , 
						clef.line - 1 + smaller,
						clef.line + 1 - smaller).getLimits(rect);
				rect.left  += staff.getX();
				rect.right += staff.getX();
				rect.top    += staff.getY();
				rect.bottom += staff.getY();
			}

			return rect;
		}
		
		public boolean isEmpty() {
			return clefs.isEmpty();
		}
	}

	public static class TimeMark extends GraphicalMoment {
		private RectF rect = new RectF();
		private static ArrayList<Chord> emptyArray = new ArrayList<Chord>();
		
		public TimeMark(RationalNumber time, MusicDrawer musicDrawer, Meter meter) {
			super(time);
			this.musicDrawer = musicDrawer;
			this.meter = meter;
		}

		Meter meter;

		public MusicDrawer musicDrawer;
		
		public void setX(float x) {
			this.x = x;
		}
		
		public MusicDrawer.SvgResource getResource() {
			/*if( cMark() ) 
				return musicDrawer.font.get(MusicDrawer.Glyphs.TIME_MARK_C);
			else if ( cCutMark() ) 
				return musicDrawer.font.get(MusicDrawer.Glyphs.TIME_MARK_C_CUT);*/
			MusicDrawer.Glyphs glyph = meter.symbol.glyph;  
			
			if( glyph==null ) return null;
			return musicDrawer.font.get(glyph);
		}
		
		public float width() {
			return rect.width();
		}
		
		private float calculateWidth() {
			MusicDrawer.SvgResource res = getResource();
			if( res!=null ) return res.boundsTo(0, -1, 1).getBounds().width();
			
			musicDrawer.testTextPaint.setTextSize(34);
			musicDrawer.testTextPaint.setTypeface(Typeface.create(Typeface.SERIF,Typeface.BOLD));
			
			Rect bounds1 = new Rect(), bounds2 = new Rect();
			musicDrawer.testTextPaint.getTextBounds(meter.beatsText, 0, meter.beatsText.length(), bounds1);
			musicDrawer.testTextPaint.getTextBounds(meter.beatTypeText, 0, meter.beatTypeText.length(), bounds2);
			
			return Math.max(bounds1.width(), bounds2.width()) * 2.f / bounds1.height();
		}
		
		@Override
		public RectF getRelativeRect() {
			return rect;
		}
		
		@Override
		public ArrayList<Chord> getChords() {
			return emptyArray;
		}
		
		@Override
		public void layout() {
			super.layout();
			
			float w = calculateWidth();
			
			rect.left  = - w/2;
			rect.right = + w/2;
		}
		
		@Override
		public void draw(PaintContext paint, Canvas canvas) {
			MusicDrawer.SvgResource res = getResource();
			
			for(Staff staff : staffGroup.getStaffs()) {
				if( res!=null ) {
					staff.drawOnStaff(paint.meters, canvas, res, 1, 3, x - width() / 2);	
					continue;
				}
				printText(paint, canvas, staff, meter.beatsText   , 1);
				printText(paint, canvas, staff, meter.beatTypeText, 3);
			}
		}
		
		private void printText(PaintContext paint, Canvas canvas, Staff staff, String text, float line) {
			Rect bounds = new Rect();
			paint.meters.setTextSize(34);
			paint.meters.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
			paint.meters.getTextBounds(text, 0, text.length(), bounds);
			
			float s = 2.f / bounds.height();
			
			canvas.save();
			canvas.scale(s, s);
			canvas.drawText(text, staff.getX(x - bounds.width() * s / 2) / s, staff.getLineY(line + 1) / s, paint.meters);
			canvas.restore();
		}
	}

	public static class KeyMark extends GraphicalMoment {
		private RectF rect = new RectF();
		private static ArrayList<Chord> emptyArray = new ArrayList<Chord>();
		
		public final float signOffset = 1.f;
		public final float flatGroupPadding = .2f;
		
		public KeyMark(RationalNumber time, MusicDrawer musicDrawer, List<Key> keys) {
			super(time);
			this.musicDrawer = musicDrawer;
			this.keys = keys;
		}
		
		public boolean isEmpty() {
			return keys.isEmpty();
		}

		List<Key> keys;
		public MusicDrawer musicDrawer;
		
		public void setX(float x) {
			this.x = x;
		}
		
		public float width() {
			return rect.width();
		}
		
		public float calculateWidth() {
			float w = 0;
			for(Key key : keys) {
				w += Accidental.width(musicDrawer, key.alteration);
			}
			if(!keys.isEmpty() && keys.get(0).alteration == 0 && 
					keys.get(keys.size()-1).alteration != 0 ) w+=flatGroupPadding;
			return w;
			//return keys.size() * signOffset;
		}
		
		@Override
		public void layout() {
			super.layout();
			
			float w = calculateWidth();
			rect.left  = x;
			rect.right = x + w;
		}
		
		@Override
		public RectF getRelativeRect() {
			return rect;
		}
		
		@Override
		public ArrayList<Chord> getChords() {
			return emptyArray; 
		}
		
		@Override
		public void draw(PaintContext paint, Canvas canvas) {
			for(Staff staff : staffGroup.getStaffs()) {
				float xx = x;
				boolean prevFlat = false;
				for(Key k : keys) {
					if( prevFlat && k.alteration != 0 ) xx += flatGroupPadding;
					xx = Accidental.draw(paint.keyAccidentals, canvas, staff, k, xx);
					
					prevFlat = (k.alteration == 0);
				}
			}
		}
	}

	public static class BarLine extends GraphicalMoment {
		public static final int COMMON_LINE = 0;
		public static final int FINAL_END   = 1;
		
		public static float commonLineWidth = .2f;
		public static float boldLineWidth = .4f;
		public static float linesSep = .4f;
		
		private int style = COMMON_LINE;

		public BarLine(RationalNumber time) {
			super(time);
		}
		
		public void setX(float x) {
			this.x = x;
		}
		
		public float width() {
			if( style==FINAL_END ) return commonLineWidth + boldLineWidth + linesSep * 2;
			return commonLineWidth;
		}
		
		@Override
		public RectF getRelativeRect() {
			RectF rect = new RectF();
			
			return rect;
		}
		
		@Override
		public ArrayList<Chord> getChords() {
			return new ArrayList<Chord>(); 
		}
		
		@Override
		public void draw(PaintContext paint,Canvas canvas) {
			float lx = x - width() + StaffGroup.lineWidth / 2;
			//Paint paint = null;
			//float strokeWidth = 0;
			if( style==FINAL_END ) {
				staffGroup.drawBarline(paint,canvas, lx + linesSep,commonLineWidth);
				staffGroup.drawBarline(paint,canvas, lx + commonLineWidth + linesSep*2, boldLineWidth);
				return;
			}
			staffGroup.drawBarline(paint,canvas, lx, commonLineWidth);
		}
		
		@Override
		public float getPadding(boolean left) {
			return 0;
		}

		public void setStyle(int style) {
			this.style = style;
		}
	}
}
