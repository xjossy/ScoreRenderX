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
import android.graphics.Paint;
import android.graphics.Shader;

import com.xpianotools.scorerenderx.MusicDrawer.Glyphs;
import com.xpianotools.scorerenderx.MusicDrawer.NoteHead;
import com.xpianotools.scorerenderx.MusicDrawer.StemPos;
import com.xpianotools.scorerenderx.StaffGroup.Staff;

public class Note implements Comparable<Note> {
	private float mLine;
	//private float y;  // = mLine for non scaled notes
	private float mag = 1;
	private int mFlags     = 0;
	private StemPos mStem  = StemPos.StemNo;
	
	private NoteHead mHead      = null;
	private MusicDrawer mDrawer = null;
	
	public GlyphSet  leftSideGlyphs = new GlyphSet(true );
	public GlyphSet rightSideGlyphs = new GlyphSet(false);
	
	private float x;
	//private Staff staff = null;
	private Chord chord = null;
	
	public Note tieDst = null;
	
	private Shader shader = null;
	private int color;// = Color.BLACK;
	private boolean colorCustom = false;
	
	public  int midiNote = -1; 
	public  int id = -1; 
	
	private boolean tied = false;
	
	/*public void setStaff(Staff staff) {
		this.staff = staff;
	}*/
	public void setChord(Chord chord) {
		this.chord = chord;
	}
	public void setX(float x) {
		this.x = x;
	}
	public void setTied(boolean tied) {
		this.tied = tied;
	}
	public boolean isTied() {
		return this.tied;
	}
	public void setTieDst(Note dst) {
		this.tieDst = dst;
	}
	public void setColor(int color, boolean custom) {
		this.color = color;
		this.colorCustom = custom;
		if( custom ) shader = null;
	}
	public void setShader(Shader shader) {
		this.shader = shader;
	}
	public boolean isColorCustom() {
		return this.colorCustom;
	}
	public int getColor() {
		return color;
	}
	
	public Staff getStaff() {
		return chord.staff;
	}
	
	public StaffGroup getStaffGroup() {
		if( chord == null || chord.staff == null ) return null;
		return chord.staff.getStaffGroup();
	}
	
	public Chord getChord() {
		return chord;
	}
	
	public GraphicalMoment getMoment() {
		return chord!=null ? chord.moment : null;
	}
	
	public Note(MusicDrawer context, NoteHead head, float line) {
		mDrawer = context;
		mLine = line;
		mHead = head;
	}
	
	public Note(MusicDrawer context) {
		mDrawer = context;
	}
	
	Note(int id) {
		this.id = id;
	}
	
	public void setMusicDrawer(MusicDrawer drawer) {
		mDrawer = drawer;
	}
	
	public void setHead(NoteHead head) {
		mHead = head;
	}
	
	public void setLine(float line) {
		mLine = line;
	}
	
	public NoteHead getNoteHead() {
		return mHead;
	}
	
	/*public MusicDrawer getMusicDrawer() {
		return mDrawer;
	}*/
	
	public Note flags(int flags) {
		if( mStem.empty() ) stem();
		mFlags = flags;
		
		return this;
	}
	
	public Note line(int line) {
		mLine = line;
		
		return this;
	}
	
	public Note stem() {
		mStem = StemPos.StemAuto;
		
		return this;
	}
	
	public Note stem(boolean stemUp) {
		mStem = stemUp ? StemPos.StemUp : StemPos.StemDown;
		
		return this;
	}
	
	public Note stem(StemPos stem, boolean forced) {
		if( forced || mStem != StemPos.StemNo ) mStem = stem;
		
		return this;
	}
	
	public float getR(float x) {
		return x + mHead.width();
	}
	
	public void drawAdditionalLines(PaintContext paint, Canvas canvas) {
		drawAdditionalLines(paint,canvas,getStaff());
	}
	public void drawAdditionalLines(PaintContext paint, Canvas canvas, Staff staff) {
		if( mLine>= -.5f && mLine<=4.5f) return;
		
		float r = getR(x);
		
		Chord chord = this.chord;
		if( chord!=null ) {
			canvas.save();
			staff.applyMagFactor(canvas,chord.getMagFactor());
		}
		
		if( mLine<-.5f) {
			for(int lx = -1;lx>mLine - .25f;lx--) {
				staff.drawAddLine(paint, canvas, x, r, lx);
			}
		} else if( mLine>4.5f) {
			for(int lx = 5;lx<mLine + .25f;lx++) {
				staff.drawAddLine(paint, canvas, x, r, lx);
			}
		}
		
		if( chord!=null ) {
			canvas.restore();
		}
	}
	
	public float getRealY() {
		return mLine / mag;
	}
	
	static final float normalStemLength = 3.5f;
	static final float shortStemLength  = 2.5f;
	static final float normalStemLengthHiZoneStart = 1.25f;
	static final float normalStemLengthHiZoneEnd = -2.5f;
	static final float normalStemLengthLoZoneStart = 4.5f;
	static final float normalStemLengthLoZoneEnd = 7.25f;
	
	public float getStemEndLine() {
		if( mStem.empty() ) return mLine;
		
		if( mStem.isUp(mLine) && mLine > 5.25f ||
		   !mStem.isUp(mLine) && mLine <-1.25f) return 2;
		
		float stemLength = normalStemLength;
		
		if( mStem.isUp(mLine) ) {
			if( mLine <  normalStemLengthHiZoneEnd ) stemLength = shortStemLength;
			if( mLine >= normalStemLengthHiZoneEnd && mLine < normalStemLengthHiZoneStart ) 
				stemLength = shortStemLength + 
				  (mLine - normalStemLengthHiZoneEnd) * 
				  (normalStemLength - shortStemLength) / 
				  (normalStemLengthHiZoneStart - normalStemLengthHiZoneEnd) ; 
		} else {
			if( mLine >  normalStemLengthLoZoneEnd ) stemLength = shortStemLength;
			if( mLine <= normalStemLengthLoZoneEnd && mLine > normalStemLengthLoZoneStart ) 
				stemLength = shortStemLength + 
				  (mLine - normalStemLengthLoZoneEnd) * 
				  (normalStemLength - shortStemLength) / 
				  (normalStemLengthLoZoneStart - normalStemLengthLoZoneEnd) ;
		}
		
		return getRealY() + stemLength * mStem.dir(mLine);
	}
	
	public float getEnlargedStemEndLine() {
		return getStemEndLine() + mStem.dir(mLine) * getStemEnlarge();
	}
	
	public float getShortestStemEndLine() {
		return getRealY() + shortStemLength * mStem.dir(mLine);
	}
	
	public float stemOffset() {
		return mStem.isUp(mLine) 
				? (mHead.width() - mHead.stemWidth / 2)
				: (mHead.stemWidth / 2);
	}
	
	public float getStemStartY() {
		return getStaff().getLineY(getRealY()) + mStem.dir(mLine) * mHead.stemOff;
	}
	
	public float getStemEnlarge() {
		return Math.max(0, mFlags - 2);
	}
	
	public void drawStem(PaintContext drawer, Canvas canvas) {
		drawStem(drawer, canvas, getEnlargedStemEndLine() );
	}
	
	public float getStemX() {
		return getStaff().getX(stemOffset() + x);
	}
	
	public void drawStem(PaintContext drawer, Canvas canvas, float toLine) {
		drawStem(drawer, canvas,getStemX(),toLine);
	}
		
	public void drawStem(PaintContext drawer, Canvas canvas, float stemX, float toLine) {
		if( mStem.empty() ) return;
		
		drawer.stems.setStrokeWidth(mHead.stemWidth);
		
		float stemY1 = getStemStartY();
		float stemY2 = getStaff().getLineY(toLine);

		canvas.drawLine(stemX, stemY1, stemX, stemY2, drawer.stems);
		PaintLogger.logLine(canvas, stemX, stemY1, stemX, stemY2, drawer.stems.getStrokeWidth());
		//canvas.drawCircle(stemX, stemY2, mHead.stemWidth/2, mDrawer.paint);
	}
	
	public void drawSlash(PaintContext drawer, Canvas canvas, float stemX, float toLine) {
		if( mStem.empty() ) return;
		
		drawer.stems.setStrokeWidth(mHead.stemWidth);
		
		float stemY1 = getStemStartY();
		float stemY2 = getStaff().getLineY(toLine);

		canvas.drawLine(stemX - width()/2, (stemY1 + stemY2)/2 + .75f, stemX + width(), (stemY1 + stemY2)/2 - .25f, drawer.stems);
		PaintLogger.logLine(canvas, stemX - width()/2, (stemY1 + stemY2)/2 + .75f, stemX + width(), (stemY1 + stemY2)/2 - .25f, 
				drawer.stems.getStrokeWidth());
	}
	
	public void drawFlag(PaintContext drawer, Canvas canvas) {
		drawFlag(drawer, canvas, getEnlargedStemEndLine());
	}
	
	public void drawFlag(PaintContext drawer, Canvas canvas, float stemEndLine) {
		drawFlag(drawer, canvas, getStemX(), stemEndLine);
	}
	
	public void drawFlag(PaintContext drawer, Canvas canvas, float stemX, float stemEndLine) {
		if( mFlags == 0 ) return;
		
		float flagX  = stemX - mHead.stemWidth/2;
		
		if( mFlags == 1 ) {
			if( mStem.isUp(mLine) ) {
				float flagY1 = getStaff().getLineY(stemEndLine +  .5f); 
				float flagY2 = getStaff().getLineY(stemEndLine + 3.5f);
				
				mDrawer.font.get(Glyphs.FLAG_EIGHT).boundsToDraw(flagX, flagY1, flagY2, drawer.flags, canvas);
			} else {
				float flagY1 = getStaff().getLineY(stemEndLine - 3f); 
				float flagY2 = getStaff().getLineY(stemEndLine - 1f);
				
				mDrawer.font.get(Glyphs.FLAG_EIGHT_DOWN).boundsToDraw(flagX, flagY1, flagY2,drawer.flags, canvas);
			}
		} else {
			for( int flag = 0; flag < mFlags; ++flag ) {
				if( mStem.isUp(mLine) ) {
					float flagY1 = -.5f + getStaff().getLineY(stemEndLine + flag); 
					float flagY2 = -.5f + getStaff().getLineY(stemEndLine + flag + 3.5f);
					
					mDrawer.font.get(Glyphs.FLAG_COMMON).
						boundsToDraw(flagX, flagY1, flagY2,drawer.flags, canvas);
				} else {
					float flagY1 = .5f + getStaff().getLineY(stemEndLine - flag - 3.5f); 
					float flagY2 = .5f + getStaff().getLineY(stemEndLine - flag);
					
					mDrawer.font.get(Glyphs.FLAG_COMMON_DOWN).
						boundsToDraw(flagX, flagY1, flagY2, drawer.flags, canvas);
				}
			}
		}
	}
	
	public void drawHead(PaintContext drawer, Canvas canvas) {
		drawHead(drawer, canvas, getStaff());
	}
	public void drawHead(PaintContext drawer, Canvas canvas, Staff staff) {
		Chord chord = this.chord;
		if( chord!=null ) {
			canvas.save();
			staff.applyMagFactor(canvas,chord.getMagFactor());
		}
		Paint paint = drawer.noteHeads;
		if( colorCustom ) {
			paint = drawer.customColor; 
			paint.setColor(color);
		}
		if( shader!=null ) {
			paint = drawer.customColor;
			paint.setShader(shader);
			drawer.shaderMatrix.reset();
			drawer.shaderMatrix.setTranslate(staff.getX(x) + mHead.mRes.bounds.width() / 2, staff.getLineY(getRealY()) + mHead.mRes.bounds.height() / 2 );
		}
		mHead.putOnStaff(paint, canvas, staff, x, getRealY(), drawer.shaderMatrix);
		if( shader!=null ) {
			paint.setShader(null);
		}
		if( chord!=null ) {
			canvas.restore();
		}
	}
	
	public float drawAll(PaintContext drawer, Canvas canvas) {
		drawAdditionalLines(drawer, canvas);
		drawHead(drawer, canvas);
		drawStem(drawer, canvas);
		drawFlag(drawer, canvas);
		
		return getR(x);
	}

	public float getLineOffset() {
		return mLine - 2;
	}
	
	public int compareTo(Note c) {
		return (int)((mLine - c.mLine) * 4);
	}

	public float getLine() {
		return mLine;
	}
	
	public float getYAbs() {
		return getStaff().getLineY(getRealY());
	}
	
	public float getRAbs() {
		return getStaff().getX(getR(x));
	}
	
	public float getTop() {
		return getRealY() - 0.5f;
	}
	
	public float getBottom() {
		return getRealY() + 0.5f;
	}

	public float width() {
		return mHead.width();
	}
	public float stemWidth() {
		return mHead.stemWidth;
	}

	public StemPos getStemPos() {
		return mStem;
	}

	public int getFlags() {
		return mFlags;
	}
	
	//not between lines
	public boolean isOnLine() {
		return Math.abs(mLine - Math.round(mLine)) < .25;
	}
	
	public void addEnlarger(boolean upper) {
		float line = mLine;
		if( isOnLine() ) line += upper ? -.5f : .5f; 
		rightSideGlyphs.addGlyph(GlyphSet.ENLARGER, line, new Dot());
	}
	
	public static class Dot extends DrawableOnStaff {
		static final float cirkRadius = .20f;
		private float offset;
		
		public Dot(float offset) {
			this.offset = offset;
		}
		
		public Dot() {
			this.offset = getWidth() / 2;
		}
		
		@Override
		public float getWidth() {
			return cirkRadius * 3;
		}

		@Override
		public float getAboveHeight() {
			return cirkRadius;
		}

		@Override
		public float getBelowHeight() {
			return cirkRadius;
		}

		@Override
		public void draw(PaintContext paint, Canvas canvas, Staff staff, float line, float x) {
			canvas.drawCircle(staff.getX(x + offset), staff.getLineY(line), cirkRadius, paint.sideGlyphs);
		}
	}

	public float getTieLine(boolean dirUp) {
		float noteLine = getLine();
		return (float) (dirUp ? Math.floor(noteLine - 0.5) : Math.ceil(noteLine + 0.5));
	}
	
	private void addTieGlyph(boolean dirUp, float x1, float x2) {
		getStaff().getStaffGroup().addGlyph(new TieGlyph(dirUp, x1, x2));
	}

	public static float newLineSlurLength = 1.5f;
	public void addTieGlyph(boolean dirUp) {
		if( tieDst==null || tieDst.chord==null || tieDst.chord.moment == null || tieDst.getStaff()==null ) return;
		
		GraphicalMoment nextMoment = tieDst.chord.moment;
		Staff nextStaff = tieDst.getStaff();
		
		float x1 = getStaff().getX(chord.moment.x + chord.moment.getHeadsRect().right);
		float x2 = nextStaff.getX(nextMoment.x + nextMoment.getHeadsRect().left);
		
		if( getStaff().getStaffGroup() == nextMoment.getStaffs() ) {
			addTieGlyph(dirUp, x1,x2);
		} else {
			addTieGlyph(dirUp, x1,getStaff().getX(getStaff().width() - 0.5f));
			tieDst.addTieGlyph(dirUp, x2 - newLineSlurLength,x2);
		}
	}
	
	class TieGlyph extends StaffGlyph {
		public boolean visible = true;
		//private boolean dirUp;
		
		private float line;
		public SlurBezier bezier = null;
		
		public TieGlyph(boolean dirUp, float x1, float x2) {
			int coeff = dirUp ? -1 : 1;
			bezier = new SlurBezier(x1, coeff * 0.2f, x2, coeff * 0.2f, coeff * 0.5f);
			this.line = getTieLine(dirUp);
		}
		
		@Override
		public void layout() {}
		
		//x is center of general note head center line
		@Override
		public void draw(PaintContext paint, Canvas canvas) {
			float off = getStaff().getLineY(line);
			bezier.translate(0, off);
			bezier.drawSlur(canvas, paint.slurs);
			bezier.translate(0,-off);
		}
		
		@Override
		public float priority() {
			return 10;
		}
	}
	public void setMagFactor(float mag) {
		this.mag = mag;
		this. leftSideGlyphs.scale = mag;
		this.rightSideGlyphs.scale = mag;
	}
}