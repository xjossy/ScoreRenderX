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

public class Meter {
	public int beats;
	public int beatType;
	public RationalNumber length;
	
	public enum Symbol {
		COMMON (null),
		C      (MusicDrawer.Glyphs.TIME_MARK_C),
		CCUT   (MusicDrawer.Glyphs.TIME_MARK_C_CUT);
		MusicDrawer.Glyphs glyph;
		Symbol(MusicDrawer.Glyphs glyph) {
			this.glyph = glyph;
		}
	}
	Symbol symbol = Symbol.COMMON; 
	
	public Meter(int beats, int beatType) {
		this.beats = beats;
		this.beatType = beatType;
		this.beatsText = Integer.toString(beats);
		this.beatTypeText = Integer.toString(beatType);
		this.length = new RationalNumber(beats, beatType);
		this.partial = this.length; 
	}
	
	public Meter(int beats, int beatType, RationalNumber partial) {
		this(beats, beatType);
		this.partial = partial;
	}
	
	public Meter setSymbol(Symbol s) {
		symbol = s;
		return this;
	}
	
	static Meter c = new Meter(4,4).setSymbol(Symbol.C);
	static Meter cCut = new Meter(2,2).setSymbol(Symbol.CCUT);
	
	RationalNumber partial;
	
	String beatsText;
	String beatTypeText;

	public boolean equals(int beats, int beatType) {
		return this.beats == beats &&
			   this.beatType == beatType;
	}
}
