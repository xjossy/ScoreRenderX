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

import java.util.Comparator;
import java.util.HashSet;
import java.util.TreeSet;

import com.xpianotools.scorerenderx.StaffGroup.Staff;

import android.graphics.Canvas;
//import android.graphics.RectF;

public class GlyphSet extends SideElement {
	public abstract class Elem {
		public Elem(int priority) {
			this.priority = priority; 
		}
		public float x = 0;
		protected int priority;

		protected float pos;
		protected Staff staff;
		
		//measure functions correct only after layout
		public abstract float top();
		public abstract float bottom();
		public abstract float width();
		
		public abstract void layout();
		public abstract void draw(PaintContext paint, Canvas canvas);
		
		public void setPosition(Staff staff, float pos) {
			this.pos = pos;
			this.staff = staff;
		};
	}
	private class GlyphElem extends Elem implements Cloneable {
		public float cline;
		public DrawableOnStaff glyph;
		
		private GlyphElem(int priority, float line, DrawableOnStaff glyph) {
			super(priority);
			this.cline = line;
			this.glyph = glyph;
		}

		@Override protected GlyphElem clone() {
			return new GlyphElem(priority, cline,glyph);
		}

		@Override public float top() {
			return cline / scale + translate - glyph.getAboveHeight();
		}

		@Override public float bottom() {
			return cline / scale + translate + glyph.getBelowHeight();
		}
		
		public float getLine() {
			return cline / scale + translate;
		}
		
		@Override public float width() {
			return glyph.getWidth();
		}
		
		@Override public void layout() {}

		@Override public void draw(PaintContext paint, Canvas canvas) {
			if( scale!=1.f) {
				canvas.save();
				staff.applyMagFactor(canvas, scale);
			}
			glyph.draw(paint, canvas, staff, getLine(), pos);
			if( scale!=1.f) {
				canvas.restore();
			}
		}
	}
	
	//move all up or down for some lines
	public float translate = 0;
	public float scale = 1;
	
	private float firstLineWidth;
	private float wholeWidth;
	
	public static final float marginX = .2f;
	public static final float marginY = .1f;

	public static final int ENLARGER = 1;
	public static final int ACCIDENTAL = 2;
	public static final int GRACE = 3;
	
	private TreeSet<Elem> elems = new TreeSet<Elem>(new Comparator<Elem>() {
		@Override
		public int compare(Elem lhs, Elem rhs) {
			int cmp1 = Integer.signum(lhs.priority - rhs.priority);
			if( cmp1!=0 ) return cmp1;
			
			int cmp2 = Float.compare(lhs.top(),rhs.top());
			if( cmp2!=0 ) return cmp2;
			
			return Integer.valueOf(lhs.hashCode()).compareTo(rhs.hashCode()); 
		}
	});
	
	public void addGlyph(int priority, float line, DrawableOnStaff glyph) {
		elems.add(new GlyphElem(priority,line,glyph));
	}
	
	public void addGlyph(int priority, Elem glyph) {
		elems.add(glyph);
	}

	public void addAll(GlyphSet glyphSet) {
		elems.addAll(glyphSet.elems);
	}
	
	public GlyphSet(boolean leftHand) {
		super(leftHand);
	}
	
	public void layout() {
		HashSet<Elem> alreadyPlaced = new HashSet<Elem>();

		float currentX = 0;
		Float firstLineWidth = null;
		
		while( alreadyPlaced.size() < elems.size() ) {
			float minY = Float.NaN;
			float maxWidth = 0;
			for(Elem elem : elems) {
				if( alreadyPlaced.contains(elem) ) continue;
				
				elem.layout();
				if(!Float.isNaN(minY) && minY > elem.top() ) continue;
				
				elem.x = currentX - elem.width() / 2;
				alreadyPlaced.add(elem);
				
				maxWidth = Math.max(maxWidth, elem.width());
				minY = elem.bottom() + marginY;
			}
			currentX += (maxWidth + marginX) * (leftHand ? -1 : 1);
			if( firstLineWidth==null ) firstLineWidth = maxWidth;
		}
		
		this.firstLineWidth = firstLineWidth == null ? 0 : firstLineWidth;
		wholeWidth = currentX * (leftHand ? -1 : 1) + marginX;
		
		//return currentX;
	}
	
	public void setPosition(Staff staff, float x) {
		float offsetX = x + (firstLineWidth / 2 + marginX) * (leftHand ? -1 : 1);
		for(Elem elem : elems) {
			elem.setPosition(staff,elem.x + offsetX);
		}
	}

	public void draw(PaintContext paint, Canvas canvas) {
		for(Elem elem : elems) {
			elem.draw(paint,canvas);
		}
	}
	
	//has correct value only after place() call before modification
	public float getWholeWidth() {
		return wholeWidth;
	}
}
