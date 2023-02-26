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

class StaffFormat {
	class PartElem {
		private int staffs;
		private StaffOpening opening;
		public PartElem(int staffs, StaffOpening opening) {
			this.staffs  = staffs;
			this.opening = opening;
		}
	}
	
	private ArrayList<PartElem> allParts = new ArrayList<PartElem>();
	
	public void addPart(int staffs) {
		allParts.add(new PartElem(staffs, staffs>1 ? new StaffOpeningBrace() : new StaffOpeningNone()));
	}
	
	public void drawBarLine(float x, StaffGroup staffs, Canvas canvas, PaintContext paint, float lineWidth) {
		int offset = 0;
		//FOR PERFORMANCE REASONS NOT for(PartElem elem:allParts)
		for(int i=0; i< allParts.size();++i) {
			PartElem elem = allParts.get(i);
			staffs.verticalLine(paint, canvas, x, offset, offset+elem.staffs - 1, lineWidth);
			
			offset += elem.staffs;
		}
	}
	
	public void drawOpening(StaffGroup staffs, Canvas canvas, PaintContext paint, float lineWidth) {
		int offset = 0;
		//FOR PERFORMANCE REASONS NOT for(PartElem elem:allParts)
		for(int i=0; i< allParts.size();++i) {
			PartElem elem = allParts.get(i);
			elem.opening.draw(paint, canvas, staffs, offset, offset+elem.staffs - 1);
			
			offset += elem.staffs;
		}
		staffs.verticalLine(paint, canvas, 0, 0, offset - 1, lineWidth);
	}
	
	static interface StaffOpening {
		public void draw(PaintContext paint, Canvas canvas, StaffGroup staffGroup, int line0, int line1);
	}
	static class StaffOpeningNone implements StaffOpening {
		static final StaffOpeningNone inst = new StaffOpeningNone();
		@Override
		public void draw(PaintContext paint, Canvas canvas, StaffGroup staffGroup, int line0, int line1) {}
	}
	static class StaffOpeningLine implements StaffOpening {
		static final StaffOpeningLine inst = new StaffOpeningLine();
		@Override
		public void draw(PaintContext paint, Canvas canvas, StaffGroup staffGroup, int line0, int line1) {
			staffGroup.verticalLine(paint, canvas, 0, line0, line1, GraphicalMoments.BarLine.commonLineWidth);
		}
	}
	static class StaffOpeningBrace extends StaffOpeningNone {
		static final StaffOpeningBrace inst = new StaffOpeningBrace();
		@Override
		public void draw(PaintContext paint, Canvas canvas, StaffGroup staffGroup, int line0, int line1) {
			super.draw(paint, canvas, staffGroup, line0, line1);
			if( staffGroup.staffs.size() <= 1 ) return;
			
			MusicDrawer.SvgResource.Transformed glyph = staffGroup.musicDrawer.font.get(
					MusicDrawer.Glyphs.FIGURE).boundsTo(staffGroup.getX(0), 
					staffGroup.get(line0).topY(), staffGroup.get(line1).bottomY()
				);
			glyph.traslate(-glyph.getWidth() - .4f, 0);
			glyph.draw(paint.staffs, canvas);
		}
	}
	StaffOpening opening = StaffOpeningBrace.inst;
}
