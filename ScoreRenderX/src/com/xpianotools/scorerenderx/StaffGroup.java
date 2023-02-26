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
import java.util.List;
import java.util.TreeSet;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

public class StaffGroup implements Cloneable {
	MusicDrawer musicDrawer;
	
	float minBetweenStaffs = 6;
	float width;

	//info about content
	private Measure measure0;
	private ArrayList<Measure> measures = new ArrayList<Measure>();
	private TreeSet<StaffGlyph> glyphs = new TreeSet<StaffGlyph>();
	
	private float measuresOffset;
	private float measuresScale;
	
	private float offsetX = 0;
	private float offsetY = 0;
	
	private float x = 0;
	private float y = 0;
	
	private int lineNumber;
	StaffFormat staffFormat;
	
	private float space = 1;
	
	private StaffGroup parent;

	public ArrayList<Staff> staffs;
	public StaffGroup(MusicDrawer musicDrawer, int staffc, float width, int lineNumber, StaffFormat staffFormat) {
		this.musicDrawer = musicDrawer;
		this.width = width;
		this.staffs = new ArrayList<Staff>(staffc);
		this.lineNumber = lineNumber;
		this.staffFormat = staffFormat;
		
		for(int i=0;i<staffc;++i)
			staffs.add(new Staff(i)); 
	}
	
	public StaffGroup(StaffGroup r) {
		this.parent = r;
		
		this.musicDrawer = r.musicDrawer;
		this.width = r.width;
		this.staffs = new ArrayList<Staff>(r.staffs.size());
		this.lineNumber = r.lineNumber;
		this.staffFormat = r.staffFormat;
		
		for(int i=0;i<r.staffs.size();++i) {
			staffs.add(new Staff(r.staffs.get(i))); 
		}
	}
	
	static float staffHeight() {
		return 4;
	}

	public static float lineWidth = 1/10.f;
	
	public class Staff implements Cloneable {
		private float x = 0;
		private float y = 0;
		
		//set clef before drawing
		public Clef clef = Clef.CLEF_F;
		public int index;
		
		private Staff base = null;
		
		public Staff(float x, float y, int index) {
			this.x = x;
			this.y = y;
			this.index = index;
		}
		
		public Staff(int index) {
			this.index = index;
		}
		
		public Staff(Staff staff) {
			this.index = staff.index;
			this.base = staff;
		}

		public float getY() {
			if( base==null ) return y * space;
			return base.y * space;
		}
		
		public float getX() {
			if( base==null ) return x * space;
			return base.x * space;
		}

		public float height() {
			return staffHeight();
		}
		
		public float width() {
			return width;
		}
		
		public float getLineY(float line) {
			return getY() + line*space;
		}
		
		public float topY() {
			return getLineY(0); 
		}
		
		public float bottomY() {
			return getLineY(4); 
		}
		
		public float getLineSpace() {
			return space;
		}
		
		public float getX(float offset) {
			return StaffGroup.this.getX(offset);
		}
		
		public void draw(PaintContext paint, Canvas canvas, float x0off, float x1off) {
			float pts[] = {
					getX() + x0off, getLineY(0), getX() + x1off, getLineY(0), 
					getX() + x0off, getLineY(1), getX() + x1off, getLineY(1),
					getX() + x0off, getLineY(2), getX() + x1off, getLineY(2),
					getX() + x0off, getLineY(3), getX() + x1off, getLineY(3),
					getX() + x0off, getLineY(4), getX() + x1off, getLineY(4)
			};
			paint.staffs.setStrokeWidth(lineWidth);
			canvas.drawLines(pts, paint.staffs);
			
			PaintLogger.logLines(canvas, pts, paint.staffs.getStrokeWidth());
		}

		public void drawAddLine(PaintContext paint, Canvas canvas, float x1, float x2, float line) {
			float w = x2-x1;
			float pts[] = {
					getX()+x1 - w/5, getLineY(line), getX()+x2 + w/5, getLineY(line)
			};
			paint.staffs.setStrokeWidth(lineWidth);
			canvas.drawLines(pts, paint.staffs);
			
			PaintLogger.logLines(canvas, pts, paint.staffs.getStrokeWidth());
		}
		
		public MusicDrawer.SvgResource.Transformed putOnStaff( MusicDrawer.SvgResource resource, float line0, float line1, float left ) {
			return resource.boundsTo(getX(left), getLineY(line0), getLineY(line1));
		}
		
		public float drawOnStaff(Paint paint, Canvas canvas, MusicDrawer.SvgResource resource, float line0, float line1, float left, Matrix shaderMatrix) {
			//equivalent to : return putOnStaff(resource, line0, line1, left).draw(paint, canvas, shaderMatrix) - x;
			return resource.boundsToDraw(getX(left), getLineY(line0), getLineY(line1), paint, canvas, shaderMatrix) - x;
		}
		
		public float drawOnStaff(Paint paint, Canvas canvas, MusicDrawer.SvgResource resource, float line0, float line1, float left) {
			//equivalent to : return putOnStaff(resource, line0, line1, left).draw(paint, canvas) - x;
			return resource.boundsToDraw(getX(left), getLineY(line0), getLineY(line1), paint, canvas) - x;
		}
		
		/*public RectF getRect(SvgResource resource, float line0, float line1, float left) {
			return resource.getRect(getX(left), getLineY(line0) - lineWidth/4, getLineY(line1) + lineWidth/4);
		}*/

		public MusicDrawer.Font getFont() {
			return musicDrawer.font;
		}
		public MusicDrawer getMusicDrawer() {
			return musicDrawer;
		}
		/*public Staff clone(float magFactor) {
			Staff n = new Staff(x,y * magFactor,index);
			return n;
		}*/

		public StaffGroup getStaffGroup() {
			return StaffGroup.this;
		}

		public void applyMagFactor(Canvas canvas, float mag) {
			canvas.translate( getX(0), getLineY(0));
			canvas.scale(mag, mag);
			canvas.translate(-getX(0),-getLineY(0));
		}
	};
	
	/*public StaffGroup clone() {
		StaffGroup n = new StaffGroup(musicDrawer, staffs.size(), width, lineNumber, staffFormat);
		n.staffs.clear();
		for(Staff staff : getStaffs()) {
			n.staffs.add(staff.clone());
		}
		return n;
	}*/
	
	public StaffGroup clone(float mag) {
		StaffGroup n = new StaffGroup(this);
		/*for(int i=0;i<staffs.size();++i) {
			n.staffs.get(i).base = staffs.get(i);
			n.staffs.get(i).space = 1/mag;
		}*/
		return n;
	}
	
	public Staff get(int i) {
		return staffs.get(i); 
	}
	public int size() {
		return staffs.size(); 
	}
	public List<Staff> getStaffs() {
		return staffs;
	}
	
	public float getX(float offset) {
		if( parent!=null ) return parent.getX(offset);
		return x + offsetX + offset;
	}
	
	public float getY(float offset) {
		if( parent!=null ) return parent.getY(offset);
		return y + offsetY + offset;
	}
	
	public float minHeight() {
		return (minBetweenStaffs + staffHeight()) * staffs.size();
	}
	
	public void setMeasureMetric(float measuresOffset, float measuresScale) {
		this.measuresOffset = measuresOffset;
		this.measuresScale = measuresScale;
	}

	public void setPosition(float x, float y) {
		this.x = x;
		this.y = y;
		
		float yoff = getY(minBetweenStaffs / 2);
		for(Staff staff : staffs) {
			staff.x = getX(0);
			staff.y = yoff;
			yoff += minBetweenStaffs + staffHeight();
		}
	}
	
	public void placeMeasures() {
		measure0.position(0, measure0.normalWidth(), this);
		
		float offset = measuresOffset; 
		//FOR PERFORMANCE REASONS NOT for(Measure measure : measures) {
		for(int i=0;i<measures.size();++i) {
			Measure measure = measures.get(i); 
			float width = measure.scaledWidth(measuresScale);
			measure.position(offset, width, this);
			offset += width;
		}
	}
	
	public void setOffset(float x, float y) {
		offsetX = x;
		offsetY = y;
	}
	
	public void layout() {
		for(StaffGlyph glyph : glyphs) {
			glyph.layout();
		}
		/* Layout measures automatically in Notation!
		 * for(int i=0;i<measures.size();++i) {
			Measure measure = measures.get(i); 
			measure.layout();
		}*/
	}
	
	//must be called after layout
	public void draw(PaintContext paint, Canvas canvas) {
		for(Staff staff : staffs) 
			staff.draw(paint, canvas, 0, width);

		staffFormat.drawOpening(this, canvas, paint, GraphicalMoments.BarLine.commonLineWidth);
		measure0.draw(paint, canvas);
		//FOR PERFORMANCE REASONS NOT for(Measure measure : measures) {
		for(int i=0;i<measures.size();++i) {
			Measure measure = measures.get(i); 
			measure.draw(paint, canvas);
		}
		for(StaffGlyph glyph : glyphs) {
			//glyph.layout();
			glyph.draw(paint, canvas);
		}
	}
	
	//can be called only after layout
	public Pair<Float, Float> verticalLimits() {
		float miny = minimumLineY(), maxy = maximumLineY();
		
		for(StaffGlyph glyph : glyphs) {
			miny = Math.min(glyph.   topY(),miny);
			maxy = Math.max(glyph.bottomY(),maxy);
		}
		
		return new Pair<Float, Float>(miny,maxy);
	}
	
	public float minimumLineY() {
		if( staffs.isEmpty() ) return 0;
		return staffs.get(0).getLineY(0);
	}
	
	public float maximumLineY() {
		if( staffs.isEmpty() ) return 0;
		return staffs.get(staffs.size() - 1).getLineY(4);
	}

	public void verticalLine(PaintContext paint, Canvas canvas, float x, int line0, int line1, float lineWidth) {
		float minY = get(line0).topY();
		float maxY = get(line1).bottomY();
		//if( minY==null || maxY==null ) return;
		//paint.staffs.setStrokeWidth(lineWidth);
		//canvas.drawLine(getX(x), minY, getX(x), maxY, paint.staffs);
		
		canvas.drawRect(getX(x), minY, getX(x) + lineWidth, maxY, paint.staffs);
		
		PaintLogger.logLine(canvas, getX(x), minY, getX(x), maxY, paint.staffs.getStrokeWidth());
	}
	public void drawBarline(PaintContext paint, Canvas canvas, float x, float lineWidth) {
		staffFormat.drawBarLine(x, this, canvas, paint, lineWidth);
	}

	public void setLeadingMeasure(Measure header) {
		measure0 = header;
		measure0.setStaffs(this);
	}

	public void addMeasure(Measure measure) {
		measures.add(measure);
		measure.setStaffs(this);
	}
	
	public Measure getMeasure0() {
		return measure0;
	}
	
	public Measure getMeasure(int absIndex) {
		if( measures.isEmpty() ) return null;
		int index = absIndex - measures.get(0).index;
		
		if( index<0 || index>=measures.size() ) return null;
		return measures.get(index);
	}
	
	public int firstMeasureIdx() {
		if( measures.isEmpty() ) return -1;
		return measures.get(0).index;
	}
	
	public int lastMeasureIdx() {
		if( measures.isEmpty() ) return -1;
		return firstMeasureIdx() + measures.size() - 1;
	}
	
	public int getMeasures() {
		return measures.size();
	}
	
	public int getLineNumber() {
		return lineNumber;
	}
	
	public void addGlyph(StaffGlyph glyph) {
		glyphs.add(glyph);
	}
	
	//apply all glyphs with smaller priority
	public void drawGlyph(PaintContext paint, Canvas canvas, StaffGlyph glyph) {
		for(StaffGlyph myGlyph : glyphs) {
			if( myGlyph.priority() >= glyph.priority() ) break;
			myGlyph.layout();
		}
		glyph.layout();
		glyph.draw(paint, canvas);
	}
}
