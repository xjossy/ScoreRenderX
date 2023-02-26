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

import com.xpianotools.scorerenderx.MusicDrawer.SvgResource;

public enum Clef {
	CLEF_G(MusicDrawer.Glyphs.CLEF_G, Key.Base.G, 3, 1f, 1),
	CLEF_F(MusicDrawer.Glyphs.CLEF_F, Key.Base.F, 1, 2f, 0);
	
	MusicDrawer.Glyphs glyph;
	float line, signesCenterLine;
	Key.Base baseNote;
	int octave;
	
	Clef(MusicDrawer.Glyphs glyph, Key.Base baseNote, float line, float signesCenterLine, int octave) {
		this.glyph = glyph;
		this.line = line;
		this.signesCenterLine = signesCenterLine;
		this.baseNote = baseNote;
		this.octave = octave;
	}
	
	public SvgResource getResource(MusicDrawer musicDrawer) {
		return musicDrawer.font.get(glyph);
	}
	
	public float width(MusicDrawer musicDrawer) {
		return getResource(musicDrawer).boundsToWidth(0, -1, 1);
	}
	
	public float signLine(Key.Base step) {
		float sLine = getLine(step);
		
		//now select octave
		while( sLine >= signesCenterLine + 1.75f ) sLine -= 3.5f;
		while( sLine <= signesCenterLine - 1.75f ) sLine += 3.5f;
		
		return sLine;
	}

	//get line of note in the same octave as this clef pointer 
	public float getLine(Key.Base step) {
		return line - (step.number - baseNote.number) / 2.f;
	}
	
	public float getLine(Key.Base step, int octave) {
		return getLine(step) - (octave - this.octave) * 3.5f;
	}
}