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

import com.xpianotools.scorerenderx.MusicDrawer.StemPos;
import com.xpianotools.scorerenderx.StaffGroup.Staff;

public class Beam extends ChordBasedGlyphBuilder {
	StemPos mStem = MusicDrawer.StemPos.StemAuto;
	private StemPos calculatedStemPos = null;
	private float   calculatedSlant   = Float.NaN;
	private float   mag = 1; //magnification for grace note beams
	
	ThreadLocal<CustomPath> fullBeamPathLocal = new ThreadLocal<CustomPath>() {
		@Override protected CustomPath initialValue() { return new CustomPath(); }
	};
	ThreadLocal<CustomPath> leftBeamPathLocal = new ThreadLocal<CustomPath>() {
		@Override protected CustomPath initialValue() { return new CustomPath(); }
	};
	ThreadLocal<CustomPath> rightBeamPathLocal = new ThreadLocal<CustomPath>() {
		@Override protected CustomPath initialValue() { return new CustomPath(); }
	};

	@Override protected Glyph buildGlyph(int first, int last, StaffGroup staffs) {
		return new Glyph(first, last);
	}
	
	@Override public void addChord(Chord chord) {
		super.addChord(chord);
		chord.beam = this;
	}
	
	public void setMagFactor(float mag) {
		this.mag = mag;
	}
	
	class Glyph extends ChordBasedGlyphBuilder.Glyph {
		Glyph(int first, int last) {
			super(first,last);
		}
		
		void applyStemPos() {
			calculatedStemPos = null;
			calculatedSlant   = Float.NaN;
			
			for(Chord c : chords) {
				c.stem(calculatedStemPos() == StemPos.StemUp);
			}
		}
		
		//if cross staff, fill topStaff and bottomStaff
		public Pair<Staff, Staff> crossStaff() {
			Staff topStaff;
			Staff bottomStaff;
			
			Staff staff = null;
			for(Chord c : chords) {
				if( c.staff==null ) continue;
				if( staff==null ) staff = c.staff;
				if( staff!=c.staff ) {
					if( staff.index<c.staff.index ) {
						topStaff = staff;
						bottomStaff = c.staff;
					} else {
						topStaff = c.staff;
						bottomStaff = staff;
					} 
					return new Pair<Staff, Staff>(topStaff, bottomStaff);
				}
			}
			
			return null;
		}
		
		public float getMaxOffset() {
			float maxOffset = 0;
			for(Chord c : chords) {
				if( Math.abs(c.getMaxOffset()) > Math.abs(maxOffset) )
					maxOffset = c.getMaxOffset();
			}
			
			return maxOffset;
		}
	
		//can't return StemPos.StemAuto
		public StemPos calculatedStemPos() {
			if( mStem!=StemPos.StemAuto ) return mStem;
			if( calculatedStemPos!=null ) return calculatedStemPos;
			if(!chords.isEmpty() && chords.get(0).voice != Chord.Voice.SINGLE ) {
				return calculatedStemPos = 
					(chords.get(0).voice == Chord.Voice.FIRST ? StemPos.StemUp : StemPos.StemDown);
			}
			
			calculatedStemPos = getMaxOffset() > 0.25 ? StemPos.StemUp : StemPos.StemDown;
			return calculatedStemPos;
		}
		
		//spaces between left side of beam and right side
		public float getSlant() {
			if( chords.isEmpty() ) return 0;
			if(!Float.isNaN(calculatedSlant) ) return calculatedSlant; 
			
			Pair<Staff, Staff> staffs = crossStaff();
			
			if( staffs == null ) {
				//get left- and rightside low and hi note
				Note llo = null, lhi = null, rlo = null, rhi = null;
				
				for(int i=0; i<(chords.size() + 1) / 2;++i) {
					Chord l = chords.get(i);
					Chord r = chords.get(chords.size() - 1 - i);
					
					if(!l.isEmptyOrRest() ) {
						if( llo == null || l.getLowNote().getLine() > llo.getLine() ) llo = l.getLowNote();
						if( lhi == null || l.getHiNote().getLine()  < lhi.getLine() ) lhi = l.getHiNote();
					} 
					if(!r.isEmptyOrRest() ) {
						if( rlo == null || r.getLowNote().getLine() > rlo.getLine() ) rlo = r.getLowNote();
						if( rhi == null || r.getHiNote().getLine()  < rhi.getLine() ) rhi = r.getHiNote();
					}
				}
				
				if( llo == null || lhi == null || rlo == null || rhi == null ) return calculatedSlant = 0f;
				
				float defaultSlant = 
						(rlo.getLine() - llo.getLine() + rhi.getLine() - lhi.getLine()) / 4;
				
				if( llo.getLine() < rlo.getLine() && lhi.getLine() < rhi.getLine() ) {
					return calculatedSlant = Math.min( defaultSlant,  1.0f);
				}
				if( llo.getLine() > rlo.getLine() && lhi.getLine() > rhi.getLine() ) {
					return calculatedSlant = Math.max( defaultSlant, -1.0f);
				}
		
				return calculatedSlant = 0f;
			} else { // cross staff
				return calculatedSlant = (float) 
						Float.compare(chords.get(chords.size() - 1).staff.index,chords.get(0).staff.index);
			}
		}
		
		public float getSlantOffset(float x) {
			if( chords.size() <= 1 ) return 0;
			return getSlant() * (x - chords.get(0).getStemX()) / (chords.get(chords.size() - 1).getStemX() - chords.get(0).getStemX());
		}
		
		//natural beam position: center line of beam left side on this line
		//center line is top line y minus stemEnlarge()
		//algorithm: at least on stem at least natural size, no stem has less than minimum size
		public float calcNaturalLine() {
			if( chords.isEmpty() ) return 0;
			
			Pair<Staff, Staff> staffs = crossStaff();
			if( staffs!=null ) {
				return (staffs.first.bottomY() + staffs.second.topY()) / 2; 
			}
			
			float limitPos = Float.NaN; //max position if upstem min if downstem 
			                       //not to make stem less then minimum stem length
			float naturalStemRulePos = Float.NaN; //position to make at least one stem natural size
			StemPos stemPos = calculatedStemPos();
			
			for(Chord c : chords) {
				if( c.isEmptyOrRest() || c.staff == null ) continue;
				float slantOff = getSlantOffset(c.getStemX());
				float y = c.getLastNote().getShortestStemEndLine() - slantOff + c.staff.topY();
				if( Float.isNaN(limitPos) || 
				   (limitPos - y) * stemPos.dir(0) < 0 ) {
					limitPos = y;
				}
			}
			
			for(Chord c : chords) {
				if( c.isEmptyOrRest() || c.staff == null ) continue;
				float slantOff = getSlantOffset(c.getStemX());
				float y = - slantOff + c.getLastNote().getStemEndLine() + c.staff.topY();
				if( Float.isNaN(naturalStemRulePos) || 
				   (naturalStemRulePos - y) * stemPos.dir(0) > 0 ) {
					naturalStemRulePos = y;
				}
			}
			
			return stemPos.isUp(0) ? Math.min(naturalStemRulePos, limitPos) : 
				Math.max(naturalStemRulePos, limitPos);
		}
		
		public int getMaxFlags() {
			int max = 0;
			for( Chord c : chords ) {
				if( c.getFlags() > max ) max = c.getFlags(); 
			}
			return max;
		}
		
		public float getStemEnlarge() {
			//return getMaxFlags() <= 2 ? -.25f : ( getMaxFlags() - 2 - .5f);  
			return (getMaxFlags() <= 2 ? 0 : ( getMaxFlags() - 2)) * calculatedStemPos().dir(0);
		}
		
		public static final float beamWidth = 1/2.f;
		public static final float beamSpace = 1/4.f + beamWidth;
		public static final float sideEnlarge = 2;

		@Override public void layout() {
			sortChords();
			
			if( chords.isEmpty() ) return;
			Pair<Staff, Staff> staffs = crossStaff();
			StemPos calculatedStemPos = calculatedStemPos();
			
			for(Chord chord : chords) {
				Staff staff = chord.staff;
				if( staff==null ) continue;
				
				StemPos stemPos = staffs == null ? calculatedStemPos :
					(staff == staffs.first ? StemPos.StemDown : StemPos.StemUp);

				chord.stem(stemPos.isUp(0));
			}
			
			float stemEnlarge = getStemEnlarge();
			float naturalLine = calcNaturalLine();
			
			for(int i=0;i<chords.size();++i) {
				Chord chord = chords.get(i);
				Staff staff = chord.staff;
				if( staff==null ) continue;
				
				StemPos stemPos = staffs == null ? calculatedStemPos :
					(staff == staffs.first ? StemPos.StemDown : StemPos.StemUp);
				
				float beamWidthOffset = - stemPos.dir(0) * beamSpace / 2; 
				if( staffs!=null && staff == staffs.first ) {
					beamWidthOffset = (chord.getFlags() - .5f) * beamSpace;
				}

				float y = getSlantOffset(chord.getStemX()) + naturalLine + stemEnlarge - staff.topY();
				chord.setStemLine(y + beamWidthOffset);
				chord.hideFlag();
				
				if( stemPos.isUp(0) ) chord.setMaxV(y);
				else                  chord.setMinV(y);
			}
		}

		@Override
		public float priority() {
			return 5;
		}

		@Override
		public void draw(PaintContext paint, Canvas canvas) {
			if( chords.isEmpty() ) return;
			Pair<Staff, Staff> staffs = crossStaff();
			
			PaintLogger.setHash(Beam.this);
			
			float stemEnlarge = getStemEnlarge();
			float naturalLine = calcNaturalLine();
			StemPos calculatedStemPos = calculatedStemPos();
			
			Chord prevChord = null;
			//float prevY = 0;
			int prevFlags = 0;
			int prevPrevFlags = 0;
			
			for(int i=0;i<chords.size();++i) {
				Chord chord = chords.get(i);
				Staff staff = chord.staff;
				if( staff==null ) continue;
				
				canvas.save();
				staff.applyMagFactor(canvas,mag);

				StemPos stemPos = staffs == null ? calculatedStemPos :
					(staff == staffs.first ? StemPos.StemDown : StemPos.StemUp);
				
				float widthOffset = (staffs == null 
						? (- stemPos.dir(0) * beamWidth) 
						: beamWidth);
						
				float beamStep = (staffs == null 
						? (- stemPos.dir(0) * beamSpace * staff.getLineSpace()) 
						: beamSpace * staff.getLineSpace());
				
				if( prevChord!=null ) {
					int fullFlags = Math.min(prevChord.getFlags(), chord.getFlags());
					
					int  leftSizeFlags = prevChord.getFlags() - fullFlags;
					int rightSizeFlags =     chord.getFlags() - fullFlags;
					
					if( prevChord.getFlags()<=prevPrevFlags ) leftSizeFlags = 0;
					if( i != chords.size()-1 ) rightSizeFlags = 0;
					
					float x1 = prevChord.getStemX() - prevChord.stemWidth() / 2;
					float x2 =     chord.getStemX() +     chord.stemWidth() / 2;
					float y1 = getSlantOffset(x1) + naturalLine + stemEnlarge - staff.topY();
					float y2 = getSlantOffset(x2) + naturalLine + stemEnlarge - staff.topY();
					
					if( i==0 && firstChord > 0 ) x1 -= sideEnlarge;
					if( i+1==chords.size() && lastChord < chords.size() ) x2 += sideEnlarge;
					
					CustomPath fullBeamPath = fullBeamPathLocal.get();
					fullBeamPath.reset();
					fullBeamPath.moveTo(staff.getX(x1), staff.getLineY(y1));
					fullBeamPath.lineTo(staff.getX(x1), staff.getLineY(widthOffset + y1));
					fullBeamPath.lineTo(staff.getX(x2), staff.getLineY(widthOffset + y2));
					fullBeamPath.lineTo(staff.getX(x2), staff.getLineY(y2));
					
					canvas.save();
					for(int line=0;line<fullFlags;++line) {
						canvas.drawPath(fullBeamPath, paint.beams);
						PaintLogger.logPath(canvas,fullBeamPath);
						canvas.translate(0, beamStep);
					}
					
					if( leftSizeFlags>0 ) {
						CustomPath leftBeamPath = leftBeamPathLocal.get();
						leftBeamPath.reset();
						x2 = x1 + prevChord.maxNoteWidth();
						y2 = getSlantOffset(x2) + naturalLine + stemEnlarge - staff.topY();
						
						leftBeamPath.moveTo(staff.getX(x1), staff.getLineY(y1));
						leftBeamPath.lineTo(staff.getX(x1), staff.getLineY(widthOffset + y1));
						leftBeamPath.lineTo(staff.getX(x2), staff.getLineY(widthOffset + y2));
						leftBeamPath.lineTo(staff.getX(x2), staff.getLineY(y2));
						
						for(int line = 0;line<leftSizeFlags;++line) {
							canvas.drawPath(leftBeamPath, paint.beams);
							PaintLogger.logPath(canvas,leftBeamPath);
							canvas.translate(0, beamStep);
						}
					} else if (rightSizeFlags>0) {
						CustomPath rightBeamPath = rightBeamPathLocal.get();
						rightBeamPath.reset();
						x1 = x2 - chord.maxNoteWidth();
						y1 = getSlantOffset(x1) + naturalLine + stemEnlarge - staff.topY();
						
						rightBeamPath.moveTo(staff.getX(x1), staff.getLineY(y1));
						rightBeamPath.lineTo(staff.getX(x1), staff.getLineY(widthOffset + y1));
						rightBeamPath.lineTo(staff.getX(x2), staff.getLineY(widthOffset + y2));
						rightBeamPath.lineTo(staff.getX(x2), staff.getLineY(y2));
						
						for(int line = 0;line<rightSizeFlags;++line) {
							canvas.drawPath(rightBeamPath, paint.beams);
							PaintLogger.logPath(canvas,rightBeamPath);
							canvas.translate(0, beamStep);
						}
					} 
					canvas.restore();
				}
				
				prevPrevFlags = prevFlags;
				prevFlags = chord.getFlags();
				prevChord = chord;
				canvas.restore();
			}

			PaintLogger.setHash(null);
		}
	}
}
