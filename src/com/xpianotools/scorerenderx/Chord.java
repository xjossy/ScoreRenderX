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

import android.graphics.Canvas;
import android.graphics.RectF;

import com.xpianotools.scorerenderx.MusicDrawer.StemPos;
import com.xpianotools.scorerenderx.StaffGroup.Staff;

import static com.xpianotools.scorerenderx.ListReversed.reversed;

public class Chord implements StaffGlyph.Builder {	
	private StemPos mStem = StemPos.StemAuto;
	private StemPos calculatedStemPos = null;
	//private boolean manualStem = false;
	private float   x = 0f;
	//private Float   manualStemLine = null;
	private float   xOffset = 0f;
	
	private float magFactor = 1;
	
	public Staff staff = null;
	public GraphicalMoment moment = null;
	
	//beam field used only for paint logging
	public Beam beam = null;
	
	//sorted by line array of notes
	ArrayList<Note> notes = new ArrayList<Note>();
	ArrayList<DrawableOnStaff> overMarkers = new ArrayList<DrawableOnStaff>();
	
	Measure graceMeasure = null;
	boolean graceSlash;
	
	enum Voice {
		FIRST,
		SECOND,
		SINGLE
	}
	
	Voice voice = Voice.SINGLE;
	
	public void clear() {
		notes.clear();
		overMarkers.clear();
		mStem = StemPos.StemAuto;
		voice = Voice.SINGLE;
		calculatedStemPos = null;
		//manualStem = false;
		//manualStemLine = null;
	}
	
	public void setVoice(boolean first) {
		stem(first);
		voice = first ? Voice.FIRST : Voice.SECOND;
	}
	
	public void setStaff(Staff staff) {
		this.staff = staff;
	}
	
	public void add(Note note) {
		//notes.add(Collections.binarySearch(notes,note),note);
		
		//TODO: can be optimized
		//TODO: updated for note should update chord
		if( note==null ) return;
		
		notes.add(note);
		Collections.sort(notes);
		calculatedStemPos = null;
		applyStemPos();
		
		note.setChord(this);
		
		onUpdate();
	}
	
	public void onUpdate() {
		if( moment!=null ) moment.onUpdate();
	}
	
	public Chord stem(boolean up) {
		mStem = up ? StemPos.StemUp : StemPos.StemDown;
		calculatedStemPos = mStem;
		stem.up = up;
		
		applyStemPos();
		
		return this;
	}
	
	public Chord setX(float xx) {
		x = xx + xOffset;
		positionNotes();
		return this;
	}
	
	public void setOffset(float offset) {
		this.xOffset = offset;
		setX(x);
	}
	
	
	public float getX() {
		return x;
	}
	
	public float getXAbs() {
		return staff.getX(x);
	}
	
	//max offset to center line
	public float getMaxOffset() {
		float maxOffset = 0;
		//FOR PERFORMANCE REASONS NOT for( Note n : notes ) {
		for(int i=0;i<notes.size();++i) {
			Note n = notes.get(i);
			if( Math.abs(n.getLineOffset()) > Math.abs(maxOffset) )
				maxOffset = n.getLineOffset();
		}
		
		return maxOffset;
	}

	public void setMagFactor(float mag) {
		magFactor = mag;
		
		//FOR PERFORMANCE REASONS NOT for( Note n : notes ) {
		for(int i=0;i<notes.size();++i) {
			Note n = notes.get(i);
			n.setMagFactor(mag);
		}
	}
	
	//can't return StemPos.StemAuto
	public StemPos calculatedStemPos() {
		if( mStem!=StemPos.StemAuto ) return mStem;
		if( calculatedStemPos!=null ) return calculatedStemPos;
		
		calculatedStemPos = getMaxOffset() > 0.25 ? StemPos.StemUp : StemPos.StemDown;
		return calculatedStemPos;
	}
	
	public void applyStemPos() {
		//FOR PERFORMANCE REASONS NOT for( Note n : notes ) {
		for(int i=0;i<notes.size();++i) {
			Note n = notes.get(i);
			n.stem(calculatedStemPos(),false);
		}
		stem.up = calculatedStemPos().isUp(0);
	}
	
	public float stemOff() {
		if( notes.isEmpty() ) return 0;
		StemPos stemPos = calculatedStemPos();
	
		//MusicDrawer drawer = notes.get(0).getMusicDrawer();
		//return - (drawer.commonNoteWidth() - drawer.commonStemWidth())/2 * stemPos.dir(0);
		Note first = getFirstNote();
		return - (first.getNoteHead().width() - first.stemWidth())/2 * stemPos.dir(0);
	}
	
	public boolean needStem() {
		//FOR PERFORMANCE REASONS NOT for( Note note : notes ) {
		for(int i=0;i<notes.size();++i) {
			Note note = notes.get(i);
			if(!note.getStemPos().empty() ) return true;
		}
		return false;
	}
	
	public float getTopNoMarkers() {
		if( notes.isEmpty() ) return 2;
		if(!needStem() ||!calculatedStemPos().isUp(0) ) return notes.get(0).getTop();
		return getStemEndLine();
	}
	
	public float getBottomNoMarkers() {
		if( notes.isEmpty() ) return 2;
		if(!needStem() || calculatedStemPos().isUp(0) ) return notes.get(notes.size() - 1).getBottom();
		return getStemEndLine();
	}
	
	public float getYBoundNoMarkers(boolean top) {
		return top ? getTopNoMarkers() : getBottomNoMarkers();
	}
	
	//get top or bottom
	public float getYBound(boolean top) {
		return top != markersPosDown() ? getMarkersEndLine() :
			getYBoundNoMarkers(top);
	}

	//get top or bottom with staff offset	
	public float getYBoundAbs(boolean top) {
		return staff.getLineY(getYBound(top));
	}
	
	public float getStemEndLine() {
		Note last = getLastNote();
		if( last==null ) return 0;
		
		return last.stem(calculatedStemPos(),false).getEnlargedStemEndLine();
	}

	/*public void drawStem(Canvas canvas, boolean force) {
		if(!needStem() ) return;
		
		drawStem(canvas, getStemEndLine(), force);
	}
	
	public void drawStem(Canvas canvas) {
		drawStem(canvas,false);
	}
	
	private void drawStem(Canvas canvas, float toLine, boolean force) {
		if(!force && manualStem ) return;
		if(!needStem() || notes.isEmpty() ) return;
		StemPos stemPos = calculatedStemPos();
		
		Note first = getFirstNote();
		
		first.stem(stemPos,false).drawStem(canvas,staff.getX(x + stemOff()),toLine);
	}*/
	
	public float stemWidth() {
		if( isEmptyOrRest() ) return 0;
		return getFirstNote().stemWidth();
	}
	
	public float maxNoteWidth() {
		float max = 0;
		
		//FOR PERFORMANCE REASONS NOT for( Note n : notes ) {
		for(int i=0;i<notes.size();++i) {
			Note n = notes.get(i);
			if( n.width() > max )
				max = n.width();
		}
		
		return max;
	}

	public void addGraceMeasure(Measure measure, boolean graceSlash) {
		this.graceMeasure = measure;
		this.graceSlash = graceSlash;
	}
	
	//TODO: implement for rest
	public RectF getHeadsRect(RectF rect) {
		return getHeadsRect(x, rect);
	}
		
	public RectF getHeadsRect(float x, RectF rect) {
		if( isEmptyOrRest() ) {
			rect.set(x,0,x,0);
			return rect; 
		}
		float stemX = x + stemOff();
		
		ArrayList<Note>  leftNotes = oneSideNotes(true );
		ArrayList<Note> rightNotes = oneSideNotes(false);
		
		float minX = x, maxX = x;

		//FOR PERFORMANCE REASONS NOT for( Note note : leftNotes ) {
		for(int i=0;i<leftNotes.size();++i) {
			Note note = leftNotes.get(i);
			float noteX = stemX + note.stemWidth() / 2 - note.width();
			
			minX = Math.min(noteX, minX);
			maxX = Math.max(noteX + note.width(), maxX);
		}
		//FOR PERFORMANCE REASONS NOT for( Note note : rightNotes ) {
		for(int i=0;i<rightNotes.size();++i) {
			Note note = rightNotes.get(i);
			float noteX = stemX - note.stemWidth() / 2;

			minX = Math.min(noteX, minX);
			maxX = Math.max(noteX + note.width(), maxX);
		}
		
		float minY = notes.get(0).getTop();
		float maxY = notes.get(notes.size() - 1).getBottom();
		
		rect.set(minX, minY, maxX, maxY);
		return rect;
	}
	
	public float getStemX() {
		return x + stemOff();
	}
	
	public float getStemX(float mag) {
		return x + stemOff() * mag;
	}

	public Note getFirstNote() {
		StemPos stemPos = calculatedStemPos();
		return stemPos.isUp(0) ? getLowNote() : getHiNote();
	}
	
	public Note getLastNote() {
		StemPos stemPos = calculatedStemPos();
		return stemPos.isUp(0) ? getHiNote() : getLowNote();
	}
	
	public Note getHiNote() {
		if( notes.isEmpty() ) return null;
		return notes.get(0);
	}
	public Note getLowNote() {
		if( notes.isEmpty() ) return null;
		return notes.get(notes.size() - 1);
	}
	
	public boolean markersPosDown() {
		if( voice == Voice.FIRST  ) return false;
		else if( voice == Voice.SECOND ) return true ;
		else return calculatedStemPos().isUp(0);
	}
	
	public float getMarkersStartLine() {
		boolean down = markersPosDown();		
		
		float line = getYBoundNoMarkers(!down);//getFirstNote().getLine(); 
		if( down ) {
			if(line > 3.25f ) return line + 0.5f;
			return (float) (Math.ceil(line) + 0.5f);
		}
		else {
			if( line < 0.75f ) return line - 0.5f;
			return (float) (Math.floor(line) - 0.5f);
		}
	}
	
	public void drawOverMarkers(PaintContext paint, Canvas canvas) {
		float step = markersPosDown() ? 1 : -1;
		float cLine = getMarkersStartLine(); 
		//FOR PERFORMANCE REASONS NOT for(DrawableOnStaff marker : overMarkers) {
		for(int i=0;i<overMarkers.size();++i) {
			DrawableOnStaff marker = overMarkers.get(i);
			marker.draw(paint, canvas, staff, cLine, getX());
			cLine += step;
		}
	}
	
	public float getMarkersEndLine() {
		if( overMarkers.isEmpty() ) return getYBoundNoMarkers(!markersPosDown());
		return getMarkersStartLine() + 
				(overMarkers.size() - 0.5f) * (markersPosDown() ? 1 : -1); 
	}
	
	public void getTiedNotes(ArrayList<Note> res) {
		//FOR PERFORMANCE REASONS NOT for( Note note : notes ) {
		for(int i=0;i<notes.size();++i) {
			Note note = notes.get(i);
			if( note.isTied() ) res.add(note);
		}
	}
	
	public void positionNotes() {
		if( notes.isEmpty() ) return;
		
		float stemX = x + stemOff();
		
		ArrayList<Note>  leftNotes = oneSideNotes(true );
		ArrayList<Note> rightNotes = oneSideNotes(false);

		//FOR PERFORMANCE REASONS NOT for( Note note : leftNotes ) {
		for(int i=0;i<leftNotes.size();++i) {
			Note note = leftNotes.get(i);
			float noteX = stemX + note.stemWidth() / 2 - note.width();
			note.setX(noteX);
		}
		//FOR PERFORMANCE REASONS NOT for( Note note : rightNotes ) {
		for(int i=0;i<rightNotes.size();++i) {
			Note note = rightNotes.get(i);	
			float noteX = stemX - note.stemWidth() / 2;
			note.setX(noteX);
		}
	}
	
	public void drawHeads(PaintContext paint, Canvas canvas) {}
	
	public void draw(PaintContext paint, Canvas canvas) {
		drawHeads(paint, canvas);
		//drawStem (canvas);
		//drawFlag (canvas);
		//drawTies (canvas);
		drawOverMarkers(paint, canvas);
	}
	
	public void drawGrace(PaintContext paint, Canvas canvas, float mag, boolean slash) {
		/*Note note = getFirstNote();
		if( note==null || staff==null ) return;
		
		float y = staff.getLineY(note.getLine());
		
		heads.drawAddLines(paint, canvas);
		
		canvas.save();
		canvas.translate(staff.getX(0), y);
		canvas.scale(mag, mag);
		canvas.translate(-staff.getX(0),-y);
		
		drawHeads(paint, canvas);
		drawOverMarkers(paint, canvas);
		
		heads.drawHeads(paint, canvas);
		stem.draw(paint, canvas);
		flag.draw(paint, canvas);
		
		if( slash ) stem.drawSlash(paint, canvas);
		
		note.leftSideGlyphs.draw(paint, canvas, staff, x - note.width()/2);
		
		canvas.restore();*/
	}
	
	public ArrayList<Note> oneSideNotes(boolean left) {
		StemPos stemPos = calculatedStemPos();
		
		ArrayList<Note> result = new ArrayList<Note>();
		
		float prevNotePos = Float.NaN;
		int   prevSide = 0;
		
		for(Note note : stemPos.isUp(0) ? reversed(notes) : notes) {
			int noteSide = 0;
			if(!Float.isNaN(prevNotePos) && Math.abs(prevNotePos - note.getLine()) < .75f ) {
				noteSide = prevSide * -1;
			} else {
				noteSide = stemPos.dir(0);
			}

			if( noteSide == (left ? -1 : 1) ) {
				result.add(stemPos.isUp(0) ? 0 : result.size(), note);
			}
				
			prevNotePos = note.getLine();
			prevSide    = noteSide;
		}
		
		return result;
	}
	
	//this function overriden to false in Rest - in this case notes is empty
	public boolean isEmpty() {
		return notes.isEmpty();
	}
	
	final public boolean isEmptyOrRest() {
		return notes.isEmpty();
	}
	
	public GlyphSet getGlyphs(boolean leftHand) {
		GlyphSet result = new GlyphSet(leftHand);
		
		appendGlyphs(result,leftHand);
		
		return result;
	}
	
	public void appendGlyphs(GlyphSet result, boolean leftHand) {
		//FOR PERFOMANCE REASON NOT for(Note note : notes) {
		for(int i=0;i<notes.size();++i) {
			Note note = notes.get(i);
			result.addAll(leftHand ? note.leftSideGlyphs : note.rightSideGlyphs);
		}
		if( leftHand && graceMeasure!=null ) {
			result.addGlyph(GlyphSet.GRACE, new GraceGlyph(result,graceMeasure,graceSlash));
		}
	}
	
	static void positionChords(Chord voice1, Chord voice2, float x) {
		voice1.setVoice(true );
		voice2.setVoice(false);
		
		voice1.setMaxV( voice1.isEmpty() ? 2 : voice2.getYBound(true ));
		voice2.setMinV( voice2.isEmpty() ? 2 : voice1.getYBound(false));
		
		if( voice1.getLowNote() == null || voice2.getHiNote() == null ) {
			voice1.setX(x);
			voice2.setX(x);
			return;
		}
		
		//distance to move chords by x for no overlapping 
		float dence = 0;
		
		if( voice1.getLowNote().getLine() + 0.75f < voice2.getHiNote().getLine() ||
			Math.abs(voice1.getLowNote().getLine() - voice2.getHiNote().getLine()) < .25f) {
			//vertical difference 0 or 1+ between low note of 1st voice and hi note of 2nd one   
			
			dence = 0;
		} else if( voice1.getLowNote().getLine() > voice2.getHiNote().getLine() + .25f ) {
			//vertical difference < -0.5 between low note of 1st voice and hi note of 2nd one
			//move 1st voice chord right, 2nd left
			
			//find minimum distance between chords
			ArrayList<Note>  leftNotes = voice1.oneSideNotes(true );
			ArrayList<Note> rightNotes = voice2.oneSideNotes(false);
			
			float minDiff = Float.NaN;
			float maxWidthAtMin = Float.NaN;
			
			for(int i=0,j=0; i<leftNotes.size() && j<rightNotes.size();) {
				float curDiff = Math.abs(leftNotes.get(i).getLine() - rightNotes.get(j).getLine());
				if( Float.isNaN(minDiff) || curDiff < minDiff ) {
					minDiff = curDiff;
					maxWidthAtMin = Float.NaN;
				}
				
				float wdSum = leftNotes.get(i).width() + rightNotes.get(j).width();
				if( curDiff == minDiff && 
				  ( Float.isNaN(maxWidthAtMin) || maxWidthAtMin < wdSum ) ) {
					maxWidthAtMin = wdSum;
				}
				
				if( leftNotes.get(i).getLine() < rightNotes.get(j).getLine() ) i++; else j++;
			}
			
			if( minDiff > 3.75 ) dence = 0;
			else if( minDiff > 0.75 ) dence = maxWidthAtMin / 10;
			else if( minDiff > 0.25 ) dence = maxWidthAtMin * 2 / 5;
			else dence = maxWidthAtMin * 5 / 9;
		} else {
			//vertical difference = 0.5 between low note of 1st voice and hi note of 2nd one
			//move 1st voice chord left, 2nd right
			
			dence = - (voice1.getLowNote().width() + voice2.getHiNote().width()) / 2.5f;
		}

		voice1.setX(x + dence/2);
		voice2.setX(x - dence/2);
	}

	public int getFlags() {
		if( isEmptyOrRest() ) return 0;
		return getFirstNote().getFlags();
	}

	//next 2 functs overiden for rests
	public void setMinV(float v) {}
	public void setMaxV(float f) {}

	public void setStemLine(float f) {
		stem.endLine = f;
	}
	
	public void magStemLength(float mag) {
		if( getLastNote()==null ) return;
		stem.endLine = getLastNote().getLine() + (stem.endLine - getLastNote().getLine()) * mag;
	}
	
	public void hideFlag() {
		flag.visible = false;
	}

	public void addOverMarker(DrawableOnStaff marker) {
		overMarkers.add(marker);
	}

	public Note getNoteByPitch(int midiNote) {
		for(Note note : notes) {
			if( note.midiNote==midiNote ) return note;
		}
		return null;
	}
	
	public void createTieGlyphs() {
		ArrayList<Note> notes = new ArrayList<Note>();
		getTiedNotes(notes);
		if( notes.isEmpty() ) return;
		
		float lastLine = Float.NaN;
		
		for(int i=0;i<notes.size();++i) {
			float noteLine = notes.get(i).getLine();
			boolean dirUp;
			
			if( voice==Voice.FIRST ) dirUp = true;
			else if( voice==Voice.SECOND ) dirUp = false;
			else if( i==0 && notes.size() > 1 ) dirUp = true;
			else if( i==notes.size() - 1 && notes.size() > 1 ) dirUp = false;
			else dirUp = noteLine <= 2;
			
			float line = notes.get(i).getTieLine(dirUp);
			
			if( line - 0.25f < lastLine ) continue;
			notes.get(i).addTieGlyph(dirUp);

			lastLine  = line;
		}
	}
	
	//set glyphs pos, but do not add to staff
	public void prepaireGlyphs() {
		stem.up = calculatedStemPos().isUp(0);
		stem.endLine = getStemEndLine();
	}

	@Override public void buildGlyphs() {
		StaffGroup g = staff.getStaffGroup();
		prepaireGlyphs();
		createTieGlyphs();
		
		g.addGlyph(stem);
		g.addGlyph(flag);
		g.addGlyph(heads);
	}
	
	class StemGlyph extends StaffGlyph {
		private float endLine = 0;
		private boolean up;
		private boolean visible = true;
		
		@Override
		public void layout() {}
		
		@Override
		public void draw(PaintContext paint, Canvas canvas) {
			PaintLogger.setHash(beam == null ? Chord.this : beam);
			
			if(!visible ) return;
			if(!needStem() ) return;
			
			Note first = getFirstNote();

			canvas.save();
			staff.applyMagFactor(canvas,magFactor);
			first.stem(up).drawStem(paint, canvas,staff.getX(x + stemOff()),endLine);
			canvas.restore();

			PaintLogger.setHash(null);
		}
		
		public void drawSlash(PaintContext paint, Canvas canvas) {
			if(!visible ) return;
			if(!needStem() ) return;
			
			Note first = getFirstNote();
			first.stem(up).drawSlash(paint, canvas,staff.getX(x + stemOff()),endLine);
		}
		
		@Override
		public float priority() {
			return 15;
		}
		
		public float baseY() {
			if(!needStem() ) return 0;
			Note first = getFirstNote();
			return staff.getLineY(first.getLine());
		}
		
		public float endY() {
			return staff.getLineY(endLine);
		}
		
		@Override
		public float topY() {
			if(!needStem() ) return super.topY();
			return up ? endY() : baseY();
		}
		
		@Override
		public float bottomY() {
			if(!needStem() ) return super.bottomY();
			return !up ? endY() : baseY();
		}
	}
	StemGlyph stem = new StemGlyph();
	
	class FlagGlyph extends StaffGlyph {
		public boolean visible = true;
		
		@Override
		public void layout() {}
		
		@Override
		public void draw(PaintContext paint, Canvas canvas) {
			PaintLogger.setHash(beam == null ? Chord.this : beam);
			
			if(!visible ) return;
			if(!needStem() ) return;
			
			Note first = getFirstNote();

			canvas.save();
			staff.applyMagFactor(canvas,magFactor);
			first.stem(stem.up).drawFlag(paint, canvas,staff.getX(x + stemOff()),stem.endLine);
			canvas.restore();
			
			PaintLogger.setHash(null);
		}
		
		@Override
		public float priority() {
			return 14;
		}
	}
	FlagGlyph flag = new FlagGlyph();
	
	class HeadsGlyph extends StaffGlyph {
		public boolean visible = true;
		
		@Override
		public void layout() {}
		
		//x is center of general note head center line
		@Override
		public void draw(PaintContext paint, Canvas canvas) {
			PaintLogger.setHash(beam == null ? Chord.this : beam);
			
			drawHeads(paint,canvas);
			drawAddLines(paint,canvas);
			
			PaintLogger.setHash(null);
		}
		
		public void drawHeads(PaintContext paint, Canvas canvas) {
			//FOR PERFORMANCE REASONS NOT for( Note note : notes ) {
			for(int i=0;i<notes.size();++i) {
				Note note = notes.get(i);
				note.drawHead(paint, canvas);
			}
		}
		
		public void drawAddLines(PaintContext paint, Canvas canvas) {
			//FOR PERFORMANCE REASONS NOT for( Note note : notes ) {
			for(int i=0;i<notes.size();++i) {
				Note note = notes.get(i);
				note.drawAdditionalLines(paint, canvas);
			}
		}
		
		@Override
		public float priority() {
			return 20;
		}
	}
	HeadsGlyph heads = new HeadsGlyph();

	public float getMagFactor() {
		return magFactor;
	}
}
