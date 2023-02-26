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
import android.graphics.Paint;

import com.xpianotools.scorerenderx.MusicDrawer.Glyphs;
import com.xpianotools.scorerenderx.MusicDrawer.SvgResource;
import com.xpianotools.scorerenderx.StaffGroup.Staff;

public class Accidental {
	static ArrayList<Glyphs> getGlyphs(int alter) {
		ArrayList<Glyphs> res = new ArrayList<Glyphs>(Collections.<Glyphs>nCopies((Math.abs(alter) + 1) / 2, null));
		if( alter==0 ) {
			res.add(Glyphs.ACC_NATURAL);
		} else for(int i=0;alter!=0;++i) {
			int index = res.size() - 1 - i;
			Glyphs resource;
			
			if( alter>=2 ) {
				resource = Glyphs.ACC_DOUBLE_SHARP;
				alter -= 2;
			} else if (alter<=-2) {
				resource = Glyphs.ACC_DOUBLE_FLAT;
				alter += 2;
			} else if (alter==1) {
				resource = Glyphs.ACC_SHARP;
				alter -= 1;
			} else {
				resource = Glyphs.ACC_FLAT;
				alter += 1;
			}
			
			res.set(index, resource);
		}
		return res;
	}
	static ArrayList<SvgResource> getResources(MusicDrawer musicDrawer, int alter) {
		ArrayList<Glyphs> glyphs = getGlyphs(alter);
		ArrayList<SvgResource> res = new ArrayList<SvgResource>(glyphs.size());
		for(Glyphs g : glyphs)
			res.add(musicDrawer.font.get(g));
		return res;
	}
	
	static float draw(PaintContext paint, Canvas canvas, Staff staff, Key key, float x) {
		return draw(paint, canvas, staff, key.alteration, staff.clef.signLine(key.base), x);
	}
	static float draw(Paint paint, Canvas canvas, Staff staff, Key key, float x) {
		return draw(paint, canvas, staff, key.alteration, staff.clef.signLine(key.base), x);
	}
	static float draw(PaintContext paint, Canvas canvas, Staff staff, int alteration, float line, float x) {
		return draw(paint.accidentals, canvas, staff, alteration, line, x); 
	}
	static float draw(Paint paint, Canvas canvas, Staff staff, int alteration, float line, float x) {
		for(SvgResource r : getResources(staff.getMusicDrawer(), alteration)) {
			x = staff.drawOnStaff(paint, canvas, r, line - .5f, line + .5f, x);
			//x += r.boundsTo(0, 0, 1).getBounds().width();
		}
		return x;
	}
	static float width(MusicDrawer musicDrawer, int alter) {
		float w = 0;
		for(SvgResource r : getResources(musicDrawer, alter)) {
			w += r.boundsTo(0, 0, 1).getBounds().width();
		}
		return w;
	}
}
