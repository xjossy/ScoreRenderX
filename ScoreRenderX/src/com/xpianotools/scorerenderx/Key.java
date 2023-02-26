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

//import java.util.Arrays;

//import android.graphics.Canvas;
//import android.graphics.Rect;

public class Key implements Cloneable {
	static int realMod(int x, int y) {
		if( y<0 ) y = -y;
		return (x >= 0) ? x % y : y - (((-x-1) % y) + 1);
	}
	
	static Base C = Base.C;
	static Base D = Base.D;
	static Base E = Base.E;
	static Base F = Base.F;
	static Base G = Base.G;
	static Base A = Base.A;
	static Base H = Base.H;
	
	public enum Base {
		C  (0, 0),
		D  (1, 2),
		E  (2, 4),
		F  (3, 5),
		G  (4, 7),
		A  (5, 9),
		H  (6,11);
		//static Base values[] = {C, D, E, F, G, A, H};
		public int number; 
		public int offset;
		
		public final Key sharp   = altered( 1);
		public final Key flat    = altered(-1);
		public final Key natural = altered( 0);
		
		Base(int number, int offset) {
			this.number = number;
			this.offset = offset;
		}
		
		public static Base get(int number) {
			return values()[realMod(number, values().length)];
		}

		public Key altered(int i) {
			return new Key(this,i);
		}
	}
	public enum Engarmonic {
		C  ( 0, false, Base.C),
		Cis( 1, true , Base.C),
		D  ( 2, false, Base.D),
		Dis( 3, true , Base.D),
		E  ( 4, false, Base.E),
		F  ( 5, false, Base.F),
		Fis( 6, true , Base.F),
		G  ( 7, false, Base.G),
		Gis( 8, true , Base.G),
		A  ( 9, false, Base.A),
		B  (10, true , Base.A),
		H  (11, false, Base.H);
		
		public int offset; 
		public boolean black;
		public Base lesserBase;
		
		Engarmonic(int offset, boolean black, Base lesserBase) {
			this.offset = offset;
			this.black  = black ;
			this.lesserBase = lesserBase;
		}
		
		public static Engarmonic get(int offset) {
			return values()[realMod(offset, values().length)];
		}
	}
	
	public Key alter(int steps, int semitones) {
		int newEngarmonic = base.offset + alteration + semitones;
		int newStep = base.number + steps;
		base = Base.get(newStep);
		alteration = newEngarmonic - base.offset - (newStep / 7) * 12;
		if( newStep < 0) alteration += 12;
		
		return this;
	}
	
	public Key altered(int steps, int semitones) {
		return clone().alter(steps,semitones);
	}
	
	public Base base = Base.C;
	
	public final Tonality major = new Tonality(this,true );
	public final Tonality minor = new Tonality(this,false);
	
	//semitone alteration: positive value - sharps, neg - flats 
	int alteration=0;
	
	public Engarmonic getEngarmonic() {
		return Engarmonic.get(base.offset + alteration);
	}
	
	public boolean isWhite() {
		return !getEngarmonic().black;
	}
	
	public int getOffset() {
		return base.offset + alteration;
	}
	
	public Key clone() {
		return new Key(base,alteration);
	}
	
	public Key() {
		this(Base.C);
	}
	public Key(Base base) {
		this(base,0);
	}
	public Key(Base base, int alteration) {
		this.base = base;
		this.alteration = alteration;
	}
	
	public boolean equals(Key r) {
		return base == r.base && alteration == r.alteration;
	}
}

//public class Key {
//	public static final int C   = 0;
//	public static final int Cis = 1;
//	public static final int D   = 2;
//	public static final int Dis = 3;
//	public static final int E   = 4;
//	public static final int F   = 5;
//	public static final int Fis = 6;
//	public static final int G   = 7;
//	public static final int Gis = 8;
//	public static final int A   = 9;
//	public static final int B   = 10;
//	public static final int H   = 11;
//	public static final int Count = 12;
//	
//	public static final int whiteKeys[] = {C,D,E,F,G,A,H};
//	
//	public static final String[] noteNamesUpper = {"C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "B", "H"};
//	public static final String[] noteNamesLower = {"c", "c♯", "d", "d♯", "e", "e", "f♯", "g", "g♯", "a", "b", "h"};
//	
//	public static final int keyWidth = 100;
//	public static final int octaveWidth = keyWidth * Count;
//	public static final int labelPadding = 10;
//	
//	private Key(int number) { 
//		this.number = number;
//	}
//	
//	public static final Key keys[] = {
//		new Key(C  ),new Key(Cis),new Key(D  ),new Key(Dis),new Key(E),new Key(F),
//		new Key(Fis),new Key(G  ),new Key(Gis),new Key(A  ),new Key(B),new Key(H)
//	};
//
//	public int number;
//	
//	//return < 0 if black
//	int whiteIndex() {
//		return Arrays.binarySearch(whiteKeys, number);
//	}
//	
//	boolean isBlack() {
//		return whiteIndex() < 0; 
//	}
//	
//	String getName(int absOctave) {
//		int signes = 0;
//		if( absOctave >= 0 ) signes = absOctave + 1;
//		if( absOctave <=-2 ) signes =-absOctave - 2;
//		
//		char[] chars = new char[signes];
//		Arrays.fill(chars, absOctave < - 1 ? ',' : '\'');
//		
//		String names[] = absOctave < - 1 ? noteNamesUpper : noteNamesLower;
//		return names[number] + new String(chars); 
//	}
//	/*
//	void draw(Canvas canvas, KeyPainter painter, int octaveOffset, int octave) {
//		int w = painter.getWidth(), h = painter.getHeight();
//		int blackEndY = h/3;
//		
//		int l = getLeftOffset() + octaveOffset;
//		int r = getRightOffset() + octaveOffset;
//		int c = (l+r)/2;
//		
//		if( r < 0 || l > w ) return;
//		
//		String name = getName(octave);
//		
//		if( isBlack() ) {
//			canvas.drawRect(l, blackEndY, r, h, painter.blackPainter);
//			
//			Rect bounds = new Rect();
//			painter.whitePainter.getTextBounds(name, 0, name.length(), bounds);
//			
//			canvas.drawText(name, c - bounds.width() / 2 , 
//					blackEndY + bounds.height() + labelPadding, painter.whitePainter);
//			
//		} else {
//			canvas.drawLine(r, 0, r, h, painter.blackPainter);
//			
//			if( number == H ) {
//				canvas.drawLine(r+1, 0, r+1, h, painter.blackPainter);
//				canvas.drawLine(r-1, 0, r-1, h, painter.blackPainter);
//			}
//			
//			Rect bounds = new Rect();
//			painter.blackPainter.getTextBounds(name, 0, name.length(), bounds);
//			
//			canvas.drawText(name, c - bounds.width() / 2 , bounds.height() +  + labelPadding, painter.blackPainter);
//		}
//
//		int areaCenter = octaveOffset + getAreaCenter();
//		canvas.drawLine(areaCenter, h, areaCenter, h - painter.getScaleHeight(), isBlack() ? painter.whitePainter : painter.blackPainter);
//	}*/
//	int getLeftOffset() {
//		int idx = whiteIndex();
//		if( idx < 0 ) return keyWidth * number;
//		if( number < F ) return 5 * keyWidth * idx / 3;
//		
//		return 5 * keyWidth + 7 * keyWidth * (idx - 3) / 4;
//	}
//	int getRightOffset() {
//		int idx = whiteIndex();
//		if( idx < 0 ) return keyWidth * (number + 1);
//		if( number < F ) return 5 * keyWidth * (idx + 1) / 3;
//		
//		return 5 * keyWidth + 7 * keyWidth * (idx + 1 - 3) / 4;
//	}
//	int getAreaCenter() {
//		return keyWidth * (2*number + 1) / 2;
//	}
//}
