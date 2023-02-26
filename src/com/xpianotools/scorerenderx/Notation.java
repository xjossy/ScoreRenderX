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

public class Notation {
	float width;
	float firstListOffset;
	float fullWidth;
	int staffs;
	MusicDrawer musicDrawer;

	public static final float leftOffset  = 3;
	public static final float rightOffset = 1.5f;
	
	//information about wrapping measuress by lines
	SpreadContext spreadContext = null;
	StaffFormat   staffFormat;
	
	class Signature {
		Tonality tonality;
		ArrayList<Clef> clefs;
		Meter meter;
		
		Signature(Tonality tonality, ArrayList<Clef> clefs, Meter meter) {
			this.tonality = tonality;
			this.clefs = clefs;
			this.meter = meter;
		}
		
		GraphicalMoments.ClefMark clefChange(Signature r,RationalNumber time) {
			GraphicalMoments.ClefMark result = new GraphicalMoments.ClefMark(time, musicDrawer);
			for(int i=0;i < r.clefs.size();++i) {
				if( i >= clefs.size() || clefs.get(i) != r.clefs.get(i) ) {
					result.setClef(i, r.clefs.get(i));
				}
			}
			
			return result;
		}
		GraphicalMoments.KeyMark keyChange(Signature r,RationalNumber time) {
			return new GraphicalMoments.KeyMark(time, musicDrawer,tonality.getAlterationsTo(r.tonality));
		}
	}
	
	Signature lastElemEndSig = null;
	NotationElem lastElem = null;
	
	class NotationElem {
		//TimeMark meterChange = null;
		Measure measure;
		//boolean noBreak;
		
		/*ArrayList<Clef> clefsStart;
		ArrayList<Clef> clefsEnd;
		
		Tonality tonalityStart;
		Tonality tonalityEnd;*/
		Signature signature;
		boolean insertedMeterChange = false;
		GraphicalMoments.BarLine barLine;
		
		NotationElem(Measure measure, Signature signature, int measureIndex) {
			this.measure = measure;
			this.signature = signature;
			
			measure.notation = Notation.this;
			measure.index = measureIndex;
		}
		
		public float normalWidth() {
			return measure.normalWidth();// + (meterChange!=null ? meterChange.width() : 0);  
		}
	}
	
	public Notation(MusicDrawer md, int staffs, float firstLineOffset, StaffFormat staffFormat) {
		this.musicDrawer = md;
		this.staffs = staffs;
		this.firstListOffset = firstLineOffset;
		this.staffFormat = staffFormat;
	}
	//can modify measure!
	void pushMeasure(Measure measure) {
		Signature sig = new Signature(measure.startTonality, measure.startClefs, measure.meter);
		NotationElem elem = new NotationElem(measure, sig, measures.size()); 
		measures.add(elem);
		
		if( lastElemEndSig!=null ) {
			GraphicalMoments.ClefMark clef = lastElemEndSig.clefChange(sig, measure.offset);
			if(!clef.isEmpty() ) lastElem.measure.addMoment(clef);
			lastElem.barLine = new GraphicalMoments.BarLine(measure.offset);
			lastElem.measure.addMoment(lastElem.barLine);
			GraphicalMoments.KeyMark key = lastElemEndSig.keyChange(sig, measure.offset);
			if(!key.isEmpty() ) lastElem.measure.addMoment(key);
			if(!lastElemEndSig.meter.equals(measure.meter) ) {
				lastElem.measure.addMoment(new GraphicalMoments.TimeMark(measure.offset,musicDrawer,measure.meter));
				lastElem.insertedMeterChange = true;
			}
		}
		
		lastElem = elem;
		lastElemEndSig = new Signature(measure.currentTonality, measure.currentClefs, measure.meter);
	}

	public void pushFinalMeasure() {
		if( lastElem==null ) return;
		//NotationElem elem = measures.get(measures.size() - 1);
		lastElem.barLine = new GraphicalMoments.BarLine(lastElem.measure.getEndTime());
		lastElem.measure.addMoment(lastElem.barLine);
		lastElem.barLine.setStyle(GraphicalMoments.BarLine.FINAL_END);
	}
	
	ArrayList<StaffGroup>   lines = new ArrayList<StaffGroup>();
	ArrayList<NotationElem> measures = new ArrayList<NotationElem>();
	
	float allMeasuresWidth() {
		float res = 0;
		for(NotationElem measure : measures ) {
			res += measure.normalWidth();
		}
		return res;
	}
	class SpreadContext {
		boolean firstLine    = true;
		boolean needTimeMark = true;
		float badness = 0;
		float remainingMeasureWidth;
		//float currentLineOffset;
		int lines;
		int measure = 0;
		
		int totalLines;
		
		GraphicalMoments.ClefMark currentClef = null;
		GraphicalMoments.KeyMark  currentKey  = null;
		GraphicalMoments.TimeMark currentTime = null;
		
		Measure header = null;
		
		ArrayList<Integer> lineMeasureOffset = new ArrayList<Integer>();
		ArrayList<Float  > lineMeasureScale  = new ArrayList<Float>();
		
		SpreadContext(int lines, float measuresWidth) {
			this.lines = lines;
			this.totalLines = lines;
			this.remainingMeasureWidth = measuresWidth;
		}
		
		void startStaffCreation() {
			lines = 0;
			measure  = 0;
			needTimeMark = true;
			firstLine = true;
		}
		void toNextLine() {
			measure = nextMeasure();
			firstLine = false;
			needTimeMark = measures.get(measure - 1).insertedMeterChange;
			++lines;
		}
		int nextMeasure() {
			if( lines>=lineMeasureOffset.size() ) return measures.size();
			return lineMeasureOffset.get(lines);
		}
		float getMeasureScale() {
			return lineMeasureScale.get(lines);
		}
		
		float remainingLineWidth() {
			return lines * (width - header.normalWidth())
					- (firstLine?firstListOffset:0);
		}
		float lineHeaderWidth() {
			return header.normalWidth();
		}
		float linePlace() {
			return lineWidth() - lineHeaderWidth();
		}
		float lineWidth() {
			return width - lineOffset();
		}
		float lineOffset() {
			return (firstLine?firstListOffset:0);
		}
		
		boolean atEnd() {
			return measure >= measures.size();
		}
		
		boolean lastLine() {
			return lines == 1;
		}
		
		void updateMarks() {
			Signature elem = measures.get(measure).signature;
			if( atEnd() ) return;
			currentClef = new GraphicalMoments.ClefMark(RationalNumber.Zero, musicDrawer);
			currentKey  = new GraphicalMoments.KeyMark(RationalNumber.Zero, 
					musicDrawer, elem.tonality.getAlterations());
			currentTime = new GraphicalMoments.TimeMark(RationalNumber.Zero, 
					musicDrawer, elem.meter);
			for(int i=0;i<elem.clefs.size();++i) {
				currentClef.setClef(i, elem.clefs.get(i));
			}
			currentClef.fullSize = true;
			
			header = new Measure(RationalNumber.Zero,RationalNumber.Zero,elem.tonality,elem.clefs,elem.meter,-1);
			header.addMoment(currentClef);
			header.addMoment(currentKey);
			if( needTimeMark ) {
				header.addMoment(currentTime);
			}
			header.layout();
		}
	}
	
	static final int maxAttempts = 10;
	
	private boolean doneMeasureLayout = false; 
	private void calculateMeasureMetrics() {
		if( doneMeasureLayout ) return;
		doneMeasureLayout = true;
		
		for(NotationElem measure: measures) {
			measure.measure.layout();
		}
	}
	
	private void spread() {
		float measuresWidth = allMeasuresWidth();
		
		//SpreadContext context = null;
		int minLines = (int) (measuresWidth / width);
		int maxLines = Math.min(measures.size(),(minLines + 1) * 2);
		minLines = Math.min(maxLines, minLines);
		
		SpreadContext lContext = spreadFor(minLines,measuresWidth);
		SpreadContext rContext = spreadFor(maxLines,measuresWidth);
		
		while(maxLines - minLines > 0) {
			if( compaireContexts(lContext,rContext) >= 0 ) {
				minLines = (minLines + maxLines + 1) / 2;
				lContext = spreadFor(minLines,measuresWidth);
			} else {
				maxLines = (minLines + maxLines   ) / 2; 
				rContext = spreadFor(maxLines,measuresWidth);
			}
		}
		
		spreadContext = lContext;
		
		/*for(int lines = firstLines, attempts = 0; attempts<maxAttempts ; lines+= (attempts + 1) / 2, attempts++) {
			SpreadContext curContext = spreadFor(lines,measuresWidth);
			if( curContext==null ) continue;
			if( context!= null && curContext.badness >= context.badness && context.badness < 10000 ) break;
			if( context== null || curContext.badness <= context.badness )
				context = curContext;
		}*/
		
		//spreadContext = (compaireContexts(lContext,rContext) >= 0) ? rContext : lContext;
	}
	private int compaireContexts(SpreadContext lContext, SpreadContext rContext) {
		if( lContext==null ) return rContext == null ? 0 : 1;
		if( rContext==null ) return -1;
		return Float.compare(lContext.badness, rContext.badness);
	}
	SpreadContext spreadFor(int lines, float measureWidth) {
		//Float badness = 0f;
		SpreadContext c;
		for(c = new SpreadContext(lines, measureWidth);!c.atEnd();) {
			if( c.lines==0 ) return null;
			
			c.updateMarks();
			
			float avgScale = c.remainingLineWidth() / c.remainingMeasureWidth;
			float linePlace = c.linePlace();
			
			if( linePlace < measures.get(c.measure).measure.minimumWidth() ) {
				int nextMeasure = c.lastLine() ? measures.size() : c.measure + 1;
				
				//float scale = 0;
				float width = 0;
				
				for(int i=c.measure;i<nextMeasure;++i) {
					Measure m = measures.get(c.measure).measure; 
					//scale = Math.max(m.minimumWidth() / m.normalWidth(), scale);
					width += m.normalWidth();
				}
						
				c.lineMeasureOffset.add(nextMeasure);
				c.lineMeasureScale.add(0f);

				c.lines--;

				c.badness += 10000 * (nextMeasure - c.measure - 1);
				c.measure = nextMeasure;
				
				c.firstLine = false;
				c.needTimeMark = measures.get(nextMeasure - 1).insertedMeterChange;
				c.remainingMeasureWidth -= width;
				
				continue;
			}
			
			//now find amount of measures for next line
			int lastMeasure;
			float currentWidth;
			float prevDistance = linePlace;
			float currentMinsize = 0;
			
			for(lastMeasure = c.measure, currentWidth = 0; lastMeasure < measures.size();++lastMeasure) {
				float cWidth = measures.get(lastMeasure).measure.normalWidth();
				float cMinWidth = measures.get(lastMeasure).measure.minimumWidth();
				
				float distance = Math.abs(avgScale * (currentWidth + cWidth) - linePlace);
				if( (distance > prevDistance || measures.size() - lastMeasure < c.lines || 
						currentMinsize + cMinWidth > linePlace )
						&& lastMeasure != c.measure && !c.lastLine() ) break;
	
				currentWidth += cWidth;
				currentMinsize += cMinWidth; 
				
				prevDistance = distance;// * 3 / 4; //it is preferable to wrap measure to next line
			}
			
			//not check if all measures minimum width not exeeded
			float realScale = (linePlace - currentMinsize) / (currentWidth - currentMinsize);
			float sumBaddnes = 0;
			if( realScale<0 ) {
				sumBaddnes += (-realScale) * 10000;   
				realScale = 0;
			}
			for(int measure = c.measure; measure<lastMeasure;++measure) {
				float demandedWidth = measures.get(measure).measure.scaledWidth(realScale);
				sumBaddnes += measures.get(measure).measure.badness(demandedWidth); 
			}
			
			//now update context
			c.lines--;
			c.measure = lastMeasure;
			
			c.lineMeasureOffset.add(lastMeasure);
			c.lineMeasureScale.add(realScale);
			
			c.firstLine = false;
			c.needTimeMark = measures.get(lastMeasure - 1).insertedMeterChange;
			c.remainingMeasureWidth -= currentWidth;

			c.badness += sumBaddnes;
		}
		
		if( c.lines!=0 ) return null;
		
		return c;
	}

	void createStaffs() {
		lines.clear();
		if( spreadContext==null ) return;
		
		spreadContext.startStaffCreation();
		
		for(int i = 0; i<spreadContext.totalLines;++i) {
			StaffGroup s = new StaffGroup(musicDrawer,staffs,spreadContext.lineWidth(),i, staffFormat);
			
			spreadContext.updateMarks();
			s.setLeadingMeasure(spreadContext.header);
			
			s.setMeasureMetric(spreadContext.lineHeaderWidth(), spreadContext.getMeasureScale());
			for(int measure = spreadContext.measure; measure<spreadContext.nextMeasure();++measure) {
				s.addMeasure(measures.get(measure).measure);
			}
			
			s.setOffset(spreadContext.lineOffset() + leftOffset,0);
			s.placeMeasures();
			
			spreadContext.toNextLine();
			lines.add(s);
		}
	}
	
	private void createGlyphs() {
		for(NotationElem measure : measures) {
			measure.measure.createGlyphs();
		}
	}
	
	public boolean compile(float width) {
		this.fullWidth = width;
		this.width = width - leftOffset - rightOffset;
		
		calculateMeasureMetrics();
		spread();
		createStaffs();
		createGlyphs();
		
		for(StaffGroup staffs : lines) {
			staffs.layout();
		}
		
		return spreadContext!=null;
	}
	
	public float getMaximumMeasureWidth() {
		float result = 0;
		
		calculateMeasureMetrics();
		for(int i=0;i<measures.size();++i) {
			Measure measure = measures.get(i).measure;
			result = Math.max(result, measure.minimumWidth());
		}
		
		return result;
	}
	
	public static float getHorizontalPadding() {
		return leftOffset + rightOffset;
	}
	
	public int getLines() {
		return spreadContext == null ? 0 
				: spreadContext.totalLines;
	}
	
	public StaffGroup getLine(int line) {
		return lines.get(line);
	}
	
	public void drawLine(PaintContext context, int line, Canvas canvas, float xoff, float yoff) {
		StaffGroup cline = getLine(line);
		cline.setPosition(xoff, yoff);
		cline.draw(context, canvas);
	}
	public Measure getMeasure(int i) {
		if( i<0 || i>=measures.size() ) return null;
		return measures.get(i).measure;
	}
	private int getLowerBound(RationalNumber time) {
		int idx1 = -1;
		int idx2 = measures.size()-1;
		
		while( idx2 != idx1  ) {
			int idx = (idx1 + idx2 + 1) / 2;
			if( measures.get(idx).measure.getStartTime().compareTo(time) > 0 ) {
				idx2 = idx - 1;
			} else {
				idx1 = idx;
			}
		}
		return idx1;
	}
	public GraphicalMoment getMomentByTime(RationalNumber time) {
		int idx = getLowerBound(time);
		if( idx>=measures.size() || idx<0 ) return null;
		return measures.get(idx).measure.getMomentByTime(time);
	}
	public MusicDrawer getMusicDrawer() {
		return musicDrawer;
	}
}
