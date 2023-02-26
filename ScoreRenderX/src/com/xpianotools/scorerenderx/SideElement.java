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

import com.xpianotools.scorerenderx.StaffGroup.Staff;
import android.graphics.Canvas;

public abstract class SideElement {
	protected boolean leftHand;
	
	public SideElement(boolean leftHand) {
		this.leftHand = leftHand;
	}
	
	//has correct value only after layout() call before modification
	public abstract float getWholeWidth();
	public abstract void layout();
	public abstract void setPosition(Staff staff, float x);
	public abstract void draw(PaintContext pc, Canvas canvas);
	
	/*static class SideElementGroup extends SideElement {
		private float wholeWidth;
		private ArrayList<SideElement> elements = new ArrayList<SideElement>();
		
		public SideElementGroup(boolean leftHand) {
			super(leftHand);
		}
		
		public void addElement(SideElement element) {
			elements.add(element);
		}

		@Override public float getWholeWidth() {
			return wholeWidth;
		}

		@Override public void layout() {
			wholeWidth = 0;
			for(SideElement element : elements) {
				element.layout();
				wholeWidth += element.getWholeWidth(); 
			}
		}

		@Override public void draw(PaintContext pc, Canvas canvas, Staff staff, float x) {
			for(SideElement element : elements) {
				element.draw(pc,canvas,staff,x);
				x += (element.leftHand ? -1 : 1) * element.getWholeWidth();
			}
		}
	}*/
}
