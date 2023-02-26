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

public class Tonality {
	private Key key;
	private boolean major;
	private int[] alterations = new int[7];
	
	public Tonality(Key key, boolean major) {
		this.key = key;
		this.major = major;
	}
	
	public Tonality() {
		this(Key.Base.C.natural, true);
	}
	
	public int getAlteration(Key.Base note) {
		//steps from base note
		int step = (note.number + 7 - key.base.number) % 7;
		//base key of tonality without any alterations and same scale
		Key.Base scaleBase = major ? Key.Base.C : Key.Base.A;
		int offset = (Key.Base.get(step + scaleBase.number).offset - scaleBase.offset + 12) % 12;
		return key.altered(step,offset).alteration;
	}
	
	//get alterations in the correct order as at clef
	public ArrayList<Key> getAlterations() {
		if(!this.major ) return parallel().getAlterations();
		
		for(int i=0;i<alterations.length;++i) {
			alterations[(i + key.base.number) % alterations.length] = 
					key.altered(i,Key.Base.get(i).offset).alteration;
		}
		
		ArrayList<Key> res = new ArrayList<Key>();
		Key.Base firstSign;
		int stepSteps;
		if( alterations[Key.Base.F.number] > 0 ) {
			firstSign = Key.Base.F;
			stepSteps = Key.Base.C.number - firstSign.number;
		} else {
			firstSign = Key.Base.H;
			stepSteps = Key.Base.E.number - firstSign.number;
		}
		
		for(Key.Base sign = firstSign;alterations[sign.number]!=0;) {
			res.add(new Key(sign, alterations[sign.number]));
			sign = Key.Base.get(sign.number + stepSteps);
			if( sign.number == firstSign.number ) break;
		}
		
		return res;
	}
	
	public boolean equals(Tonality r) {
		return r.key.equals(key) && (r.major == major);
	}

	public ArrayList<Key> getAlterationsTo(Tonality r) {
		ArrayList<Key> keys = new ArrayList<Key>();
		
		ArrayList<Key> oldAcc =   getAlterations();
		ArrayList<Key> newAcc = r.getAlterations();
			
		//add naturals
		for(Key key : oldAcc) {
			if( r.getAlteration(key.base) != key.alteration ) 
				keys.add(key.base.natural);
		}

		//if not same set of accidentals
		if(!keys.isEmpty() || oldAcc.size() != newAcc.size() )
			keys.addAll(r.getAlterations());
		
		return keys;
	}

	public Tonality parallel() {
		if(!major ) return new Tonality(key.altered(-Key.A.number,-Key.A.offset), true);
		return new Tonality(key.altered(Key.A.number,Key.A.offset), false);
	}
}
