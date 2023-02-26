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
import java.util.TreeMap;

public class SoundMap {
	private long globalEndTime = 0;
	
	public static class Event {
		public Event(RationalNumber time, Note noteObj, int note, int velo, int color, long duration, int instrument) {
			this.time = time;
			this.note = note;
			this.velo = velo;
			this.color = color;
			this.duration = duration;
			this.instrument = instrument;
			this.noteObj = noteObj;
		}
		public boolean isSame(Event event) {
			return 
					note == event.note && 
					instrument == event.instrument;
		}
		public void supremum(Event event) {
			duration = Math.max(duration, event.duration);
			velo     = Math.max(velo, event.velo);
		}
		public RationalNumber time;
		public int note;
		public int velo;
		public int color;
		public int instrument;
		public long duration;
		public Note noteObj;
	}
	
	public static class Chord extends ArrayList<Event> {
		private static final long serialVersionUID = -7540009031411091719L;
		
		public boolean hasColor(int color) {
			for(Event event : this) {
				if( event.color == color ) return true;
			}
			return false;
		}

		public void uniqueAdd(Event res) {
			for(int i=0;i<size();++i) {
				if( res.isSame(get(i)) ) {
					get(i).supremum(res);
					res.velo = 0;
					break;
				}
			}
			add(res);
		}
	}
	
	public TreeMap<Long, Chord> soundsByMillis = new TreeMap<Long, Chord>();

	public SoundMap() {
		// TODO Auto-generated constructor stub
	}
	
	static int midiC0 = 60;
	static int getMidiNumber(Key key, int octave) {
		return midiC0 + (octave - 1) * 12 + key.getOffset();
	}

	public Event addEvent(RationalNumber time, Note noteObj, long millis, int note, int velo, int color, long duration, int instrument) {
		globalEndTime = Math.max(globalEndTime, millis + duration);
		Chord chord = soundsByMillis.get(millis);
		if( chord == null ) {
			chord = new Chord();
			soundsByMillis.put(millis, chord);
		}
		Event res = new Event(time, noteObj, note, velo, color, duration, instrument);
		chord.uniqueAdd(res);
		return res;
	}
	
	public long getEndTime() {
		return globalEndTime;
	}
}
