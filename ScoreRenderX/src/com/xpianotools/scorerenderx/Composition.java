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

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import android.util.SparseArray;

//import com.xpianotools.scorerenderx.Composition.Note;
import com.xpianotools.scorerenderx.MusicDrawer.NoteHeadGlyph;
import com.xpianotools.scorerenderx.MusicDrawer.StemPos;

public class Composition {
	public int staffs;
	public ArrayList<Part> allParts = new ArrayList<Part>();
	
	public ArrayList<Integer> instrumentByStaff = new ArrayList<Integer>();
	
	private TreeMap<RationalNumber, Integer> tempoByTime = new TreeMap<RationalNumber, Integer>();
	public StaffFormat staffFormat = new StaffFormat();
	
	private static abstract class CompositionElem {
		static class ElemId implements Comparable<ElemId> {
			final int priority;
			protected RationalNumber time;
			ElemId(int priority, RationalNumber time) {
				this.priority = priority;
				this.time = time;
			}

			@Override
			public int compareTo(ElemId ce) {
				int res = time.compareTo(ce.time);
				if( res!=0 ) return res;
				return Integer.valueOf(priority).compareTo(ce.priority);
			}
		}
		ElemId id;
		//protected RationalNumber ctime;
		
		CompositionElem(RationalNumber time) {
			this.id = new ElemId(priority(),time);
		}
		void setPosition(RationalNumber time) {
			this.id.time = time;
		}
		
		abstract void reset();
		abstract int priority(); 
		abstract void placeToMeasure(CompileContext c, Measure measure);
		abstract boolean needMeasure();
		public void writeSound(CompileContext c, SoundMap map) {}
		public void writeGraceSound(CompileContext c, SoundMap map, int durationDenum, long startTime) {}
	}
	
	private class CompileContext {
		MusicDrawer musicDrawer;
		int staff;
		
		Clef clef;
		ArrayList<Clef> clefs = new ArrayList<Clef>(staffs);
		ArrayList<Integer> dynamicsByStaff = new ArrayList<Integer>(staffs);
		static final int dynamicsDefault = 100;

		int measureIndex = 0;
		
		Tonality tonality = Key.Base.C.natural.major;
		Voice voice;
		
		ColorMap colorMap;
		float tempoMultiplier = 1;
		
		int dynamics = dynamicsDefault;
		int noteId = 0;
		
		float tempo = 70; //quarters per minute
		RationalNumber tempoStart = RationalNumber.Zero;
		long tempoStartMillis = 0;

		public RationalNumber time;
		
		long getMillis(RationalNumber time) {
			return tempoStartMillis + 
					getMillisInterval(time.subtract(tempoStart));
		}

		long getMillisInterval(RationalNumber time) {
			return (long) time.multiply(new RationalNumber(4*60*1000,(int)(tempo*tempoMultiplier))).getDoubleValue();
		}
		
		double getTempoFactor() {
			return 4*60*1000. / (int)(tempo*tempoMultiplier);
		}
		
		void setStaff(int staff) {
			this.staff = staff;
			
			if( staff >= clefs.size()  || staff<0 || clefs.get(staff) == null )
				this.clef = staff < staffs / 2 ? Clef.CLEF_G : Clef.CLEF_F;
			else this.clef = clefs.get(staff);
			
			this.dynamics = getDynamics(staff);
		}
		
		int getDynamics(int staff) {
			Integer dynamics = dynamicsByStaff.get(staff);
			if( dynamics==null ) return dynamicsDefault;
			return dynamics;
		}
		
		TreeMap<Integer, TreeMap<Integer, Integer> > measureAlterByLineAndStaff = new TreeMap<Integer, TreeMap<Integer, Integer> >();
		public long currentTimeMillis = 0;
		
		void resetMeasure() {
			measureAlterByLineAndStaff.clear();
		}
		int getMeasureAlter(float line) {
			TreeMap<Integer, Integer> byStaff = measureAlterByLineAndStaff.get(staff);
			if( byStaff==null ) return 0;
			Integer entry = byStaff.get(Math.round(line * 2));
			if( entry==null ) return 0;
			
			return entry;
		}
		void setMeasureAlter(float line, int alter) {
			TreeMap<Integer, Integer> byStaff = measureAlterByLineAndStaff.get(staff);
			if( byStaff==null ) {
				byStaff = new TreeMap<Integer, Integer>();
				measureAlterByLineAndStaff.put(staff,byStaff);
			}
			byStaff.put(Math.round(line * 2), alter);
		}
		
		public CompileContext() {
			this.colorMap = ColorMap.defaultInst;
			for(int i=0;i<staffs;++i) {
				clefs.add(i < staffs / 2 ? Clef.CLEF_G : Clef.CLEF_F);
				dynamicsByStaff.add(null);
			}
			tempo = tempoByTime.isEmpty() ? tempo : tempoByTime.firstEntry().getValue();
		}
		
		public CompileContext(ColorMap colorMap) {
			this();
			this.colorMap = colorMap; 
		}
		
		public CompileContext(float tempoMultiplier) {
			this();
			this.tempoMultiplier = tempoMultiplier; 
		}
		
		TreeMap<RationalNumber,ArrayList<Note> > tiesByTime = new TreeMap<RationalNumber,ArrayList<Note> >();
		public SoundMap.Event tryTie(RationalNumber time, Note note) {
			while(!tiesByTime.isEmpty() && tiesByTime.firstKey().compareTo(time) < 0 ) tiesByTime.pollFirstEntry();
			if( tiesByTime.isEmpty() || tiesByTime.firstKey().compareTo(time) != 0 ) return null;
			
			ArrayList<Note> events = tiesByTime.firstEntry().getValue();
			for(Iterator<Note> eventIter = events.iterator(); eventIter.hasNext(); ) {
				Note cnote = eventIter.next();
				if( cnote.getMidiNote()==note.getMidiNote() && 
						cnote.staff == note.staff ) {
					if( cnote.event!=null ) cnote.event.duration += note.getDuration(this);
					cnote.setTiedNote(note, this);
					eventIter.remove();
					return cnote.event;
				}
			}
			return null;
		}
		public void addTiedNote(RationalNumber time, Note note) {
			RationalNumber afterTime = time.add(note.getLength());
			ArrayList<Note> events = tiesByTime.get(afterTime);
			if( events == null ) {
				events = new ArrayList<Note>();
				tiesByTime.put(afterTime, events);
			}
			events.add(note);
		}
		
		public int getInstrument() {
			if( staff>=instrumentByStaff.size() || staff<0 ) return 0;
			return instrumentByStaff.get(staff);
		}

		public void setDynamics(int staff, int dynamics) {
			dynamicsByStaff.set(staff, dynamics);
			if( staff == this.staff ) this.dynamics = dynamics;
		}

		public int nextId() {
			return noteId++;
		}
	}
	
	public static class ColorMap {
		//default implementation
		public int noteColor(Note note) {
			return note.color;
		}
		public static ColorMap defaultInst = new ColorMap();
	}
	
	public static class NoteLength {
		//0 for whole note, 1 for half, 2 for quarter, -1 for breve etc.
		//this param determines form of note head and beam 
		public int beatPower;
		
		//Multiplier for irrational time
		public RationalNumber times = new RationalNumber(1,1);
		
		//how many dots at right of the note should be placed
		public int enlongers = 0;
		
		public NoteLength(int beatPower) {
			this.beatPower = beatPower;
		}
		
		public NoteLength(NoteLength length) {
			beatPower = length.beatPower;
			times     = length.times;
			enlongers = length.enlongers;
		}

		public RationalNumber getLength() {
			RationalNumber base = beatPower > 0 ? 
					new RationalNumber(1,1 << beatPower) : new RationalNumber(1 << beatPower,1);
					
			base.multiplyThis(times);
			
			RationalNumber adding = base.clone(); 
			for(int i=0;i<enlongers;++i) {
				adding.divideThis(2);
				base.addThis(adding);
			}
			
			return base;
		}
	}
	
	public static class Note {
		private NoteLength length;

		//if accidentals is manually set
		boolean showForcedAlter = false;
		//int forcedAlter = 0;
		
		Key pitch;
		int octave;
		boolean tied = false;
		
		int color = 0xff000000;
		boolean manualColor = false;
		//int velocity = 127;

		int staff;
		RationalNumber time;
		
		RationalNumber duration;
		
		//ArrayList<Note> graces = null;
		//boolean gracesSlash;
		
		public RationalNumber getLength() {
			return duration != null ? duration : length.getLength();
		}
		
		public void setTiedNote(Note dst, CompileContext c) {
			getCompiled(c).setTieDst(dst.getCompiled(c));
			dst.tieEnd = true;
		}

		//example new Note(Key.Base.D.sharp, 2, 4) for 2'nd octave dis 1/16
		public Note(Key pitch, int octave, int beatPower) {
			this(pitch, octave, beatPower, null);
		}
				
		public Note(Key pitch, int octave, int beatPower, RationalNumber duration) {
			this.pitch = pitch;
			this.octave = octave;
			this.length = new NoteLength(beatPower);
			this.duration = duration;
		}
		public Note dot() {
			length.enlongers++;
			return this;
		}
		public Note mesureTimes(RationalNumber r) {
			length.times = length.times.multiply(r);
			return this;
		}
		public void tied() {
			this.tied = true;
		}
		
		public Note setColor(int color) {
			this.color = color;
			manualColor = true;
			return this;
		}
		
		public int getColor() {
			return this.color;
		}
		
		/*public Note setVelocity(int vel) {
			this.velocity = vel;
			return this;
		}*/
		
		/*public void addGrace(Note note, boolean slash) {
			if( graces==null ) graces = new ArrayList<Note>();
			graces.add(note);
			gracesSlash = slash;
		}*/
		
		NoteHeadGlyph getNoteHeadGlyph() {
			if( length.beatPower==0 ) return NoteHeadGlyph.WHOLE;
			if( length.beatPower==1 ) return NoteHeadGlyph.HALF;
			if( length.beatPower>=2 ) return NoteHeadGlyph.QUATER;
			
			return NoteHeadGlyph.UNSUPPORTED;
		}
		
		void prepaireCompilation(CompileContext c) {
			getCompiled(c);
			this.staff = c.staff;
			this.time = c.time;
			
			event = c.tryTie(c.time, this);
			if( tied ) {
				c.addTiedNote(c.time, this);
			}
		}
		
		com.xpianotools.scorerenderx.Note getCompiled(CompileContext c) {
			if( compiled==null )
				compiled = new com.xpianotools.scorerenderx.Note(c.nextId());
			
			return compiled;
		}
		
		void reset() {
			compiled = null;
		}
		
		com.xpianotools.scorerenderx.Note compile(CompileContext c) {
			prepaireCompilation(c);
			
			float line = c.clef.getLine(pitch.base,octave);
			compiled.setMusicDrawer(c.musicDrawer);
			compiled.setHead(c.musicDrawer.getNoteHead(getNoteHeadGlyph()));
			compiled.setLine(line);
			compiled.flags(Math.max(0, (length.beatPower - 2)));
			compiled.stem(length.beatPower > 0 ? StemPos.StemAuto : StemPos.StemNo,true);
			compiled.setColor(c.colorMap.noteColor(this),manualColor);
			compiled.midiNote = getMidiNote();
			if( showForcedAlter || 
				pitch.alteration != c.tonality.getAlteration(pitch.base) + c.getMeasureAlter(line) ) {
				showCompileAlterations(c);
				c.setMeasureAlter(line, pitch.alteration - c.tonality.getAlteration(pitch.base));
			}
			for(int i=0; i<length.enlongers;++i) {
				compiled.addEnlarger(c.voice != Voice.VoiceTwo);
			}
			if( tied ) compiled.setTied(true);
			/*if( graces!=null ) for(Note grace : graces) {
				compiled.addGrace(grace.compile(c), gracesSlash);
			}*/
			return compiled;
		}
		void showCompileAlterations(CompileContext c) {
			for(MusicDrawer.SvgResource glyph : Accidental.getResources(c.musicDrawer, pitch.alteration)) {
				compiled.leftSideGlyphs.addGlyph(GlyphSet.ACCIDENTAL, compiled.getLine(), glyph.boundsTo(0, -.5f, .5f));
			}
		}

		public void writeSound(CompileContext c, SoundMap map, int graceOff) {
			prepaireCompilation(c);
			if(!tieEnd ) {
				event = map.addEvent(
						c.time, this.compiled, c.currentTimeMillis + graceOff, 
						getMidiNote(), c.dynamics, color, 
						getDuration(c) - graceOff, c.getInstrument());
			}
		}

		com.xpianotools.scorerenderx.Note compiled = null;
		SoundMap.Event event = null;
		boolean tieEnd = false;

		public void setDots(int dots) {
			length.enlongers = dots;
		}
		
		public int getMidiNote() {
			return SoundMap.getMidiNumber(pitch, octave);
		}
		
		public Key getPitch() {
			return pitch;
		}
		
		public long getDuration(CompileContext c) {
			return c.getMillisInterval(getLength());
		}

		public void writeGraceSound(CompileContext c, SoundMap map,
				int durationDenum, long startTime) {
			event = map.addEvent(
					c.time, this.compiled, startTime, 
					getMidiNote(), c.dynamics, color, 
					getDuration(c) / durationDenum, c.getInstrument());
		}
		
		public RationalNumber getTime() {
			return time;
		}
	}
	
	//represents rest or chord
	public abstract static class VoiceElem implements Cloneable {
		public abstract void unionWith(VoiceElem chord);
		public abstract com.xpianotools.scorerenderx.Chord compile(CompileContext c);
		public abstract VoiceElem clone();
		public abstract void clearNotes();
		public abstract void writeSound(CompileContext c, SoundMap map);
		public abstract void writeGraceSound(CompileContext c, SoundMap map, int durationDenum, long startTime);
		public abstract RationalNumber getLength();
		public abstract void reset();
	}
	
	public static class Rest extends VoiceElem {
		NoteLength length;
		RationalNumber duration = null;
		int color = 0;
		
		public Rest(int beatPower, RationalNumber duration) {
			this(beatPower);
			this.duration = duration;
		}
		public Rest(int beatPower) {
			this.length = new NoteLength(beatPower);
		}
		public Rest(NoteLength length) {
			this.length = length;
		}
		
		public void unionWith(VoiceElem elem) {
			return;
		}

		public Rest dot() {
			length.enlongers++;
			return this;
		}
		
		public Rest clone() {
			return new Rest(new NoteLength(length));
		}
		public RationalNumber getLength() {
			return duration != null ? duration : length.getLength();
		}
		
		public com.xpianotools.scorerenderx.Rest compile(CompileContext c) {
			if( compiled!=null ) return compiled;
			
			com.xpianotools.scorerenderx.Rest obj = new com.xpianotools.scorerenderx.Rest(
					c.musicDrawer,
					MusicDrawer.NoteLength.fromInteger(length.beatPower)
				);
			for(int i=0; i<length.enlongers;++i) {
				obj.addEnlarger();
			}
			return obj;
		}
		com.xpianotools.scorerenderx.Rest compiled = null;
		public void clearNotes() {}
		public void setDots(int dots) {
			length.enlongers = dots;
		}
		public void writeSound(CompileContext c, SoundMap map) {}
		public void writeGraceSound(CompileContext c, SoundMap map, int durationDenum, long startTime) {}
		public void setColor(int color) {
			this.color = color;
		}
		public void reset() {
			compiled = null;
		}
	}
	
	public static class Chord extends VoiceElem implements Cloneable {
		ArrayList<Note> notes = new ArrayList<Note>();
		RationalNumber length;
		boolean staccato = false;
		
		public Chord(RationalNumber length, ArrayList<Note> notes) {
			this(length);
			this.notes = notes; 
		}
		public Chord(RationalNumber length) {
			this.length = length;
		}
		public Chord() {
			this(RationalNumber.Zero);
		}
		public Chord add(Note note) {
			notes.add(note);
			return this;
		}
		public Chord clone() {
			return new Chord(length, new ArrayList<Note>(notes));
		}
		public RationalNumber getLength() {
			RationalNumber length = RationalNumber.Zero;
			for(Note note : notes) {
				if( note.getLength().compareTo(length) > 0 ) {
					length = note.getLength();
				}
			}
			return length;
		}
		public void unionWith(VoiceElem elem) {
			if(!(elem instanceof Chord)) return;
			Chord chord = (Chord)elem;
			
			length = (length.compareTo(chord.length) > 0) ? length : chord.length;
			notes.addAll(chord.notes);
			
			if( chord.grace!=null ) {
				grace = chord.grace;
				graceSlash = chord.graceSlash;
			}
		 	
			staccato = staccato || chord.staccato;
		}
		public boolean isEmpty() {
			return notes.isEmpty();
		}
		
		//always compile to the same object!
		public com.xpianotools.scorerenderx.Chord compile(CompileContext c) {
			if( compiled != null ) return compiled; 
			compiled = new com.xpianotools.scorerenderx.Chord();
			
			compiled.clear();
			
			for(Note note : notes) {
				compiled.add(note.compile(c));
			}
			if( staccato ) {
				compiled.addOverMarker(new com.xpianotools.scorerenderx.Note.Dot(0));
			}

			if( grace!=null ) {
				compiled.addGraceMeasure(grace.compileGraceMeasure(c), graceSlash);
			}
			
			return compiled;
		}
		
		public void reset() {
			compiled = null;
			for(Note note : notes) {
				note.reset();
			}
		}

		Part grace = null;
		boolean graceSlash;
		
		private void setGrace(Part gracePart, boolean graceSlash) {
			this.grace = gracePart;
			this.graceSlash = graceSlash;
		}
		
		public void writeGraceSound(CompileContext c, SoundMap map, int durationDenum, long startTime) {
			for(Note note : notes) {
				note.writeGraceSound(c, map, durationDenum, startTime);
			}
		}
		
		public void writeSound(CompileContext c, SoundMap map) {
			int curGraceOff = 0;
			if( grace!=null ) {
				int durationDenum = graceSlash ? 4 : 1;
				curGraceOff = (int)c.getMillis(grace.realLength) / durationDenum;
				long startTime = c.currentTimeMillis;
				if( graceSlash ) {
					startTime -= curGraceOff;
					curGraceOff = 0;
				}
				grace.writeGraceSound(c, map, durationDenum, startTime);
			}
			
			for(Note note : notes) {
				note.writeSound(c, map, curGraceOff);
			}
		}
		
		public void clearNotes() {
			notes.clear();
			staccato = false;
		}
		
		public void staccato() {
			staccato = true;
		}
		
		com.xpianotools.scorerenderx.Chord compiled = null;
	}
	
	public class ClefSetting extends CompositionElem implements Cloneable {
		TreeMap<Integer,Clef> clefSettings = new TreeMap<Integer,Clef>();
		static final int priority = 0;
		
		ClefSetting(RationalNumber ctime) {
			super(ctime);
		}
		
		void set(int staff, Clef c) {
			clefSettings.put(staff, c);
		}
		public Clef get(int staff) {
			return clefSettings.get(staff);
		}
		@Override void reset() {}
		@Override
		public void placeToMeasure(CompileContext c, Measure measure) {
			c.clefs = new ArrayList<Clef>(c.clefs);
			
			for(Map.Entry<Integer,Clef> entry : clefSettings.entrySet()) {
				if( entry.getKey()>=c.clefs.size() || entry.getKey()<0 ) continue;
				c.clefs.set(entry.getKey(), entry.getValue());
			}
			
			if( measure!=null ) {
				GraphicalMoments.ClefMark mark = new GraphicalMoments.ClefMark(id.time,c.musicDrawer);
				for(Map.Entry<Integer,Clef> set : clefSettings.entrySet() ) {
					mark.setClef(set.getKey(), set.getValue());
				}
				measure.addMoment(mark);
				measure.changeClefs(c.clefs);
			}
		}
		@Override
		public boolean needMeasure() {
			return false;
		}
		@Override
		public int priority() {
			return priority;
		}
	}
	
	class TonalitySetting extends CompositionElem {
		public static final int priority = 1;

		public TonalitySetting(Tonality key, RationalNumber ctime) {
			super(ctime);
			tonality = key;
		}
		Tonality tonality;

		@Override
		public void placeToMeasure(CompileContext c, Measure measure) {
			if( measure!=null ) {
				measure.addMoment(new GraphicalMoments.KeyMark(id.time, c.musicDrawer, 
						c.tonality.getAlterationsTo(tonality)));
				measure.changeTonality(tonality);
			}
			
			c.tonality = tonality;
		}

		@Override
		public boolean needMeasure() {
			return false;
		}
		
		@Override void reset() {}

		@Override
		public int priority() {
			return priority;
		}
	}
	
	class DynamicsSetting extends CompositionElem {
		public static final int priority = 10;
		
		int dynamics;
		int staff;

		public DynamicsSetting(int dynamics, int staff, RationalNumber ctime) {
			super(ctime);
			this.dynamics = dynamics;
			this.staff = staff;
		}
		
		private void set(CompileContext c) {
			c.setDynamics(staff, dynamics);
		}

		@Override public void placeToMeasure(CompileContext c, Measure measure) {
			set(c);
		}
		
		@Override public void writeSound(CompileContext c, SoundMap map) {
			set(c);
		}
		@Override public void writeGraceSound(CompileContext c, SoundMap map, int durationDenum, long startTime) {
			
		}

		@Override void reset() {}
		
		@Override
		public boolean needMeasure() {
			return false;
		}

		@Override
		public int priority() {
			return priority;
		}
	}

	public enum Voice {
		SingleVoice (0),
		VoiceOne    (1),
		VoiceTwo    (2);
		int index;
		Voice(int i) {
			index = i;
		}
	}
	
	public static class ChordList extends CompositionElem {
		public static final int priority = 15;
		ChordList(RationalNumber ctime) {
			super(ctime);
		}
		class StaffChords {
			AbstractCollection<Integer> voiceMap = null;
			class VoiceChords {
				ArrayList<VoiceElem> chords = new ArrayList<VoiceElem>();
				VoiceElem union = new Chord();
				
				com.xpianotools.scorerenderx.Chord compile(CompileContext c) {
					if( compiled!=null ) return compiled;
						
					VoiceElem result = union.clone();
					result.unionWith(union);
					result.clearNotes();
					for(VoiceElem chord : chords) {
						result.unionWith(chord);
					}
					
					compiled = result.compile(c); 
					return compiled;
				}
				
				public void reset() {
					compiled = null;
					union.reset();
					for(VoiceElem chord : chords) {
						chord.reset();
					}
				}

				public void writeSound(CompileContext c, SoundMap map) {
					for(VoiceElem chord : chords) {
						chord.writeSound(c,map);
					}
				}
				
				public void writeGraceSound(CompileContext c, SoundMap map, int durationDenum, long startTime) {
					for(VoiceElem chord : chords) {
						chord.writeGraceSound(c,map,durationDenum,startTime);
					}
				}
				
				public VoiceElem add(VoiceElem chord) {
					if( chord instanceof Rest ) {
						union = chord.clone();
					} else {
						chords.add(chord);
					}
					return union;
				}
				
				public ChordList getChordList() {
					return ChordList.this;
				}
				
				com.xpianotools.scorerenderx.Chord compiled = null;
			}
			
			TreeMap<Integer,VoiceChords> voices = new TreeMap<Integer,VoiceChords>();
			int staff;
			
			public StaffChords(int staff) {
				this.staff = staff;
			}
			
			GraphicalMoments.OneStaffGraphicalMoment compile(CompileContext c) {
				c.setStaff(staff);
				
				if( compiled!=null ) return compiled;
				if( voiceMap!=null ) {
					ArrayList<com.xpianotools.scorerenderx.Chord> chords = 
							new ArrayList<com.xpianotools.scorerenderx.Chord>(voiceMap.size());
					for(int voice : voiceMap) {
						VoiceChords voiceChords = voices.get(voice);
						chords.add(voiceChords != null 
								? voiceChords.compile(c) 
								: new com.xpianotools.scorerenderx.Chord()
						);
					}
					compiled = new GraphicalMoments.MultipleVoiceMoment(id.time, chords);
				} else if( voices.size() > 1 ) {
					c.voice = Voice.VoiceOne;
					com.xpianotools.scorerenderx.Chord v1 = voices.firstEntry().getValue() != null 
							? voices.firstEntry().getValue().compile(c)
							: new com.xpianotools.scorerenderx.Chord();
					c.voice = Voice.VoiceTwo;
					com.xpianotools.scorerenderx.Chord v2 = voices.lastEntry().getValue() != null 
							? voices.lastEntry().getValue().compile(c)
							: new com.xpianotools.scorerenderx.Chord();
					compiled = new GraphicalMoments.MultipleVoiceMoment(id.time, 
							Arrays.asList(new com.xpianotools.scorerenderx.Chord[]{v1, v2}));
				} else if( voices.size() > 0 ) {
					c.voice = Voice.SingleVoice;
					compiled = new GraphicalMoments.OneVoiceMoment(id.time, 
							voices.firstEntry().getValue().compile(c));
				} else {
					compiled = new GraphicalMoments.OneVoiceMoment(id.time,new com.xpianotools.scorerenderx.Chord());
				}
				return compiled;
			}

			void reset() {
				for(Map.Entry<Integer,VoiceChords> entry : voices.entrySet() ) {
					entry.getValue().reset();
				}
				compiled = null;
			}

			public void writeSound(CompileContext c, SoundMap map) {
				c.setStaff(staff);
				for(Map.Entry<Integer,VoiceChords> entry : voices.entrySet() ) {
					entry.getValue().writeSound(c,map);
				}
			}

			public void writeGraceSound(CompileContext c, SoundMap map,
					int durationDenum, long startTime) {
				c.setStaff(staff);
				for(Map.Entry<Integer,VoiceChords> entry : voices.entrySet() ) {
					entry.getValue().writeGraceSound(c,map, durationDenum, startTime);
				}
			}
			
			GraphicalMoments.OneStaffGraphicalMoment compiled = null;

			public ChordList.StaffChords.VoiceChords add(int voice, VoiceElem chord) {
				VoiceChords vchord = voices.get(voice);
				if( vchord==null ) {
					vchord = new VoiceChords();
					voices.put(voice, vchord);
				}
				vchord.add(chord);
				return vchord; 
			}
		}
		TreeMap<Integer,StaffChords> byStaff = new TreeMap<Integer,StaffChords>();
		
		//returns union chord
		ChordList.StaffChords.VoiceChords addChord(int staff, int voice, VoiceElem chord) {
			StaffChords staffChords = byStaff.get(staff);
			if( staffChords == null ) {
				staffChords = new StaffChords(staff);
				byStaff.put(staff, staffChords);
			}
			return staffChords.add(voice,chord); 
			//staffChords.chords.get(voice.index).add(chord);
			//staffChords.check();
		}
		
		GraphicalMoments.MultipleStaffMoment compile(CompileContext c) {
			c.time = id.time;
			compiled = new GraphicalMoments.MultipleStaffMoment(id.time);
			for(Map.Entry<Integer,StaffChords> staffs : byStaff.entrySet()) {
				c.setStaff(staffs.getKey());
				compiled.addStaff(staffs.getKey(), staffs.getValue().compile(c));	
			}
			return compiled;
		}
		GraphicalMoments.MultipleStaffMoment compiled = null;
		
		@Override
		public void reset() {
			for(Map.Entry<Integer,StaffChords> staffs : byStaff.entrySet()) {
				staffs.getValue().reset();	
			}
			compiled = null;
		}
		
		@Override
		public boolean needMeasure() {
			return true;
		}
		@Override
		public void placeToMeasure(CompileContext c, Measure measure) {
			GraphicalMoments.MultipleStaffMoment moment = compile(c);
			moment.setMillis(c.getMillis(id.time), c.getTempoFactor());
			measure.addMoment(moment);
		}
		
		@Override
		public void writeSound(CompileContext c, SoundMap map) {
			c.currentTimeMillis = c.getMillis(id.time);
			c.time = id.time;
			for(Map.Entry<Integer,StaffChords> staffs : byStaff.entrySet()) {
				c.setStaff(staffs.getKey());
				staffs.getValue().writeSound(c, map);	
			}
		}
		
		@Override
		public void writeGraceSound(CompileContext c, SoundMap map, int durationDenum, long startTime) {
			long time = c.getMillis(id.time);
			for(Map.Entry<Integer,StaffChords> staffs : byStaff.entrySet()) {
				c.setStaff(staffs.getKey());
				staffs.getValue().writeGraceSound(c, map, durationDenum, startTime + time / durationDenum);	
			}
		}

		@Override
		public int priority() {
			return priority;
		}

		public void setVoiceMode(int staff, AbstractCollection<Integer> voiceMap) {
			StaffChords staffChords = byStaff.get(staff);
			if( staffChords==null ) return;
			staffChords.voiceMap = voiceMap;
		}
	}
	
	private static abstract class CompositionChordList extends CompositionElem {
		CompositionChordList(RationalNumber ctime) {
			super(ctime);
		}
		protected ArrayList<ChordList.StaffChords.VoiceChords> elems = new ArrayList<ChordList.StaffChords.VoiceChords>();
		public void addElem(ChordList.StaffChords.VoiceChords elem) {
			elems.add(elem);
		}
		
		public abstract StaffGlyph.Builder compile(CompileContext c);

		@Override
		void placeToMeasure(CompileContext c, Measure measure) {
			measure.addGlyphBuilder(compile(c));
		}
	}

	private static class Beam extends CompositionChordList {
		Beam(RationalNumber ctime) {
			super(ctime);
		}
		
		public com.xpianotools.scorerenderx.Beam compile(CompileContext c) {
			com.xpianotools.scorerenderx.Beam res = new com.xpianotools.scorerenderx.Beam();
			for(ChordList.StaffChords.VoiceChords elem : elems) {
				res.addChord(elem.compile(c));
			}
			return res;
		}
		
		@Override void reset() {}

		@Override
		int priority() {
			return 20;
		}
		@Override
		boolean needMeasure() {
			return true;
		}
	}
	
	private static class Slur extends CompositionChordList {
		Slur(RationalNumber ctime) {
			super(ctime);
		}
		
		public com.xpianotools.scorerenderx.Slur compile(CompileContext c) {
			com.xpianotools.scorerenderx.Slur res = new com.xpianotools.scorerenderx.Slur();
			for(ChordList.StaffChords.VoiceChords elem : elems) {
				res.addChord(elem.compile(c));
			}
			return res;
		}

		@Override void reset() {}

		@Override
		int priority() {
			return 20;
		}

		@Override
		boolean needMeasure() {
			return false;
		}
	}
	
	private boolean graceSlash;
	private Part gracePart = null;
	private Part.VoiceWriter graceWriter = null;
	Part.VoiceWriter getGraceVoiceWriter(boolean graceSlash) {
		if( gracePart==null ) {
			gracePart = new Part();
			graceWriter = gracePart.startWriting(0,0);
			this.graceSlash = graceSlash;
		}
		return graceWriter;
	}
	
	public class Part {
		//Meter meter;
		Part  prev = null;
		
		//RationalNumber offset;
		//RationalNumber length;
		RationalNumber realLength;
		RationalNumber currentPosition;
		
		public class VoiceWriter {
			private RationalNumber currentPosition;
			private int staff;
			private int voice;
			private Beam beam = null;
			private Stack<Slur> slurs = new Stack<Slur>();
			private ChordList.StaffChords.VoiceChords elem = null;
			private int measureIdx = 0;
			private RationalNumber currentMeasureMax;
			
			private VoiceWriter(int staff, int voice) {
				this.staff = staff;
				this.voice = voice;
				this.currentPosition = Part.this.currentPosition;
			}
			
			public void changeVoice(int voice) {
				this.voice = voice;
			}
			
			public void advanceTime(RationalNumber time) {
				currentPosition = currentPosition.add(time);
				positionUpdated();
			}
			
			public Chord write(Note note) {
				return write(new Chord(note.getLength()).add(note));
			}
			
			public Chord write(Chord chord) {
				elem = addChord(currentPosition,staff,voice,chord);
				currentPosition = currentPosition.add(chord.length);
				setPosition(currentPosition);
				if( beam!=null ) beam.addElem(elem);
				if(!slurs.isEmpty() ) slurs.peek().addElem(elem);
				if( gracePart!=null && Part.this!=gracePart ) {
					chord.setGrace(gracePart, graceSlash);
					gracePart = null;
					graceWriter = null;
				}
				
				positionUpdated();
				
				return chord;
			}
			
			private void positionUpdated() {
				if( currentMeasureMax!=null && currentPosition.compareTo(currentMeasureMax) > 0) {
					currentMeasureMax = currentPosition;
				}
			}

			public Rest write(Rest rest) {
				elem = addChord(currentPosition,staff,voice,rest);
				currentPosition = currentPosition.add(rest.getLength());
				setPosition(currentPosition);
				positionUpdated();
				
				return rest;
			}
			
			public void resetMeasure() {
				measureIdx = 0;
			}
			
			public void toCurrentMeasure() {
				currentPosition = getMeasureStartTime(measureIdx-1);
			}
			
			public void toNextMeasure() {
				currentPosition = getMeasureStartTime(measureIdx++);
				currentMeasureMax = currentPosition;
				beginMeasure(currentPosition);
			}

			public void endMeasure() {
				if( currentMeasureMax!=null ) breakMeasure(currentMeasureMax);
			}
			
			public void startBeam() {
				assert(beam==null);
				beam = new Beam(currentPosition);
			}
			
			public void endBeam() {
				assert(beam!=null);
				
				beam.setPosition(currentPosition);
				addCommonElement(beam);
				beam=null;
			}
			
			public void startSlur() {
				Slur slur = new Slur(currentPosition);
				slurs.add(slur);
			}
			
			public void endSlur() {
				assert(!slurs.isEmpty());
				Slur slur = slurs.pop();
				slur.setPosition(currentPosition);
				addCommonElement(slur);
			}
			
			public VoiceWriter setClef(Clef clef) {
				Part.this.setClef(staff,clef,currentPosition);
				return this;
			}
			
			public VoiceWriter setKey(Tonality key) {
				Part.this.setKey(key,currentPosition);
				return this;
			}
			
			public VoiceWriter changeStaff(int staff) {
				this.staff = staff;
				return this;
			}
			/*public VoiceWriter changeVoice(Voice voice) {
				this.voice = voice;
				return this;
			}*/

			public void moveTo(RationalNumber time) {
				currentPosition = time;
				setPosition(currentPosition);
				positionUpdated();
			}

			/*public void advanceMeasure() {
				currentPosition = currentPosition.add(meter.length);
				setPosition(currentPosition);
				positionUpdated();
			}*/

			public RationalNumber getPosition() {
				return currentPosition;
			}

			public int getStaff() {
				return staff;
			}
			public int getVoice() {
				return voice;
			}
			public ChordList getLastChordList() {
				return elem==null ? null : elem.getChordList();
			}

			public void setTempo(int tempo) {
				tempoByTime.put(currentPosition, tempo);
			}

			public void setMeter(Meter meter) {
				measureMeters.put(measureIdx - 1, meter);
			}

			public void setDynamics(int dynamics, int staff) {
				ArrayList<CompositionElem> elems = getElem(currentPosition, DynamicsSetting.priority );
				elems.add(new DynamicsSetting(dynamics,staff, currentPosition));
			}
		}
		
		void setPosition(RationalNumber newPos) {
			currentPosition = newPos;
			/*while( endTime().compareTo(newPos) < 0 ) 
				enlargeOneMeasure();*/
		}

		/*void enlargeOneMeasure() {
			length = length.add(meter.length);
		}*/
		
		public RationalNumber endTime() {
			return realLength;
		}
		
		void resetPosition() {
			this.currentPosition = RationalNumber.Zero;
		}
		
		public Part(/*Part prev, Meter meter*/) {
			//this.prev  = prev;
			//this.meter = meter;
			//this.offset = RationalNumber.Zero;//prev == null ? RationalNumber.Zero : prev.endTime();
			this.currentPosition = RationalNumber.Zero;
			//this.length = meter != null ? meter.partial : RationalNumber.Zero;
			//this.length = RationalNumber.Zero;
			this.realLength = new RationalNumber(0,1);
			
			/*if( prev==null ) {
				keySettings.put(offset, new Tonality());
				//clefSettings.put(offset, new ClefSetting());
			}*/
		}
		
		public void setMeter(Meter meter) {
			//this.meter = meter;
			//this.currentPosition = this.offset;
			//this.length = meter.partial;
			//this.elements.clear();
		}
		
		/*public Part(Meter meter) {
			this(null,meter);
		}*/

		/*public Part(Part lastPart) {
			this(lastPart,null);
		}*/

		//start writing for single voice on this staff
		public VoiceWriter startWriting(int staff) {
			return startWriting(staff, 0);
		}
		public VoiceWriter startWriting(int staff, int voice) {
			//assert(voice >=0 && voice < 2);
			return new VoiceWriter(staff, voice);
		}
		/*public VoiceWriter startWritingAtBeginning() {
			currentPosition = this.offset;
			return startWriting(0);
		}*/
		
		/*Clef getClefAt(int staff, RationalNumber pos) {
			for(Part p = this;p!=null;p = p.prev) {
				Map.Entry<RationalNumber, ClefSetting> res = p.clefSettings.floorEntry(pos);
				if( res!=null ) {
					Clef clef = res.getValue().get(staff);
					if( clef != null )return clef;
				}
			}
			return staff >= staffs / 2 ?  Clef.CLEF_F : Clef.CLEF_G;
		}
		
		Tonality getTonality(RationalNumber pos) {
			for(Part p = this;p!=null;p = p.prev) {
				Map.Entry<RationalNumber, Tonality> res = p.keySettings.floorEntry(pos);
				if( res!=null ) return res.getValue();
			}
			return Key.Base.C.natural.major;
		}*/
		
		void setClef(int staff, Clef clef) {
			setClef(staff,clef,currentPosition);
		}
		
		void setClef(int staff, Clef clef, RationalNumber time) {
			ArrayList<CompositionElem> elems = getElem(time, ClefSetting.priority);
			ClefSetting setting;
			if( elems.isEmpty() || !(elems.get(0) instanceof ClefSetting)) {
				setting = new ClefSetting(time);
				elems.add(setting);
			} else {
				setting = (ClefSetting)(elems.get(0));
			}
			setting.set(staff, clef);
		}
		void setKey(Tonality key) {
			setKey(key,currentPosition);
		}
		void setKey(Tonality key, RationalNumber time) {
			ArrayList<CompositionElem> elems = getElem(time, TonalitySetting.priority );
			elems.clear();
			elems.add(new TonalitySetting(key,time));
		}
		
		ChordList.StaffChords.VoiceChords addChord(RationalNumber time, int staff, int voice, VoiceElem chord) {
			ArrayList<CompositionElem> elems = getElem(time, ChordList.priority);
			ChordList array;
			if( elems.isEmpty() || !(elems.get(0) instanceof ChordList)) {
				array = new ChordList(time);
				elems.add(array);
			} else {
				array = (ChordList)(elems.get(0));
			}
			
			RationalNumber endTimeAbs = time.add(chord.getLength());//.subtract(offset);
			if( endTimeAbs.compareTo(realLength) > 0 ) {
				realLength = endTimeAbs;
			}
			
			return array.addChord(staff, voice, chord);
		}
		
		void addCommonElement(CompositionElem elem) {
			getElem(elem.id.time,elem.priority()).add(elem);
		}
		
		private ArrayList<CompositionElem> getElem(RationalNumber time, int priority) {
			CompositionElem.ElemId id = new CompositionElem.ElemId(priority, time);
			ArrayList<CompositionElem> res = elements.get(id);
			if( res==null ) {
				res = new ArrayList<CompositionElem>();
				elements.put(id, res);
			}
			return res;
		}
		
		void writeSound(CompileContext c, SoundMap map) {
			for(Iterator<Map.Entry<CompositionElem.ElemId, ArrayList<CompositionElem> > > iter = 
					elements.entrySet().iterator();iter.hasNext();) {
				Map.Entry<CompositionElem.ElemId, ArrayList<CompositionElem> > entry = iter.next();
				
				for(CompositionElem element : entry.getValue()) {
					element.writeSound(c, map);
				}
			}
		}
		
		public void writeGraceSound(CompileContext c, SoundMap map,
				int durationDenum, long startTime) {
			for(Iterator<Map.Entry<CompositionElem.ElemId, ArrayList<CompositionElem> > > iter = 
					elements.entrySet().iterator();iter.hasNext();) {
				Map.Entry<CompositionElem.ElemId, ArrayList<CompositionElem> > entry = iter.next();
				
				for(CompositionElem element : entry.getValue()) {
					element.writeGraceSound(c, map,durationDenum,startTime);
				}
			}
		}
		
		Measure compileGraceMeasure(CompileContext c) {
			Measure measure = new Measure(RationalNumber.Zero,realLength,c.tonality,c.clefs,Meter.c,c.measureIndex - 1);
			
			for(Iterator<Map.Entry<CompositionElem.ElemId, ArrayList<CompositionElem> > > iter = 
					elements.entrySet().iterator();iter.hasNext();) {
				Map.Entry<CompositionElem.ElemId, ArrayList<CompositionElem> > entry = iter.next();
				for(CompositionElem element : entry.getValue()) {
					element.placeToMeasure(c, measure);
				}
			}
			
			return measure;
		}
		
		void beginMeasure(RationalNumber time) {
			//currentMeasureMax = time;
		}
		
		void breakMeasure(RationalNumber time) {
			int res = Collections.binarySearch(measureBreaks, time);
			if( res>=0 ) return;
			measureBreaks.add(-res - 1, time);
		}
		
		RationalNumber getMeasureStartTime(int measureIdx) {
			if( measureIdx==0 || measureBreaks.isEmpty() ) return RationalNumber.Zero;
			return measureIdx - 1 < measureBreaks.size() ? 
					measureBreaks.get(measureIdx - 1) : measureBreaks.get(measureBreaks.size() - 1);
		}
		
		RationalNumber getMeasureEndTime(int measureIdx) {
			return measureIdx < measureBreaks.size() ? 
					measureBreaks.get(measureIdx) : RationalNumber.MAX_VALUE;
		}
		
		void reset() {
			for(Map.Entry<CompositionElem.ElemId, ArrayList<CompositionElem> > element : elements.entrySet()) {
				for(CompositionElem elem : element.getValue())
					elem.reset();
			}
		}
		
		void compileTo(CompileContext c, Notation n) {
			//RationalNumber endTime = endTime();
			int measureNumber = 0;
			
			RationalNumber thisMeasure = RationalNumber.Zero;
			RationalNumber nextMeasure = getMeasureEndTime(measureNumber++);//offset.add(meter.partial);
			
			Measure measure = null;
			Meter cMeter = measureMeters.get(0);
			if( cMeter==null ) cMeter = Meter.c;
			
			for(Iterator<Map.Entry<CompositionElem.ElemId, ArrayList<CompositionElem> > > iter = 
					elements.entrySet().iterator();iter.hasNext();) {
				Map.Entry<CompositionElem.ElemId, ArrayList<CompositionElem> > entry = iter.next();
				RationalNumber ctime = entry.getKey().time;
				
				//if end of measure
				while( ctime.compareTo(nextMeasure) >= 0 && measureNumber < measureBreaks.size()) {
					if( measure!=null)
						n.pushMeasure(measure);
					
					c.resetMeasure();
					measure = null;
					thisMeasure = nextMeasure;
					
					if( measureMeters.get(measureNumber) != null ) {
						cMeter = measureMeters.get(measureNumber);
					}
					nextMeasure = getMeasureEndTime(measureNumber++);
				}
				
				for(CompositionElem element : entry.getValue()) {	
					if( measure==null && element.needMeasure() ) {
						measure = new Measure(thisMeasure,nextMeasure.subtract(thisMeasure),c.tonality,c.clefs,cMeter, c.measureIndex++);
					}
					
					element.placeToMeasure(c, measure);
				}
			}
			
			if( measure!=null)
				n.pushMeasure(measure);
		}
		
		TreeMap<CompositionElem.ElemId, ArrayList<CompositionElem> > elements = 
				new TreeMap<CompositionElem.ElemId, ArrayList<CompositionElem> >();
		
		ArrayList<RationalNumber> measureBreaks = new ArrayList<RationalNumber>();
		SparseArray<Meter> measureMeters = new SparseArray<Meter>(); 
	}
	
	public Composition(int staffs) {
		this.staffs = staffs;
	}
	
	Part lastPart() {
		return allParts.isEmpty() ? null : allParts.get(allParts.size() - 1);
	}
	/*public Part start(int beats, int beatType, int partialBeats) {
		return start(new Meter(beats, beatType, new RationalNumber(partialBeats, beatType)));
	}*/
	
	/*public Part startPart() {
		Part obj = new Part(lastPart());
		allParts.add(obj);
		
		return obj;
	}*/
	
	public Part start() {
		Part obj = new Part();
		allParts.add(obj);
		
		return obj;
	}
	
	public void setStaffs(int staffs) {
		this.staffs = staffs;
	}
	
	public void reset() {
		for(Part part : allParts) {
			part.reset();
		}
	}
	
	public Notation compile(MusicDrawer md, float firstLineOff) {
		return compile(md, firstLineOff, ColorMap.defaultInst);
	}
	
	public Notation compile(MusicDrawer md, float firstLineOff, ColorMap colorMap) {
		Notation notation = new Notation(md, staffs, firstLineOff, staffFormat);
		CompileContext cc = new CompileContext(colorMap);
		cc.musicDrawer = md;
		
		for(Part part : allParts) {
			part.compileTo(cc, notation);
		}
		
		notation.pushFinalMeasure();
		
		return notation;
	}
	
	public SoundMap createSoundMap(float tempoMultiplyer) {
		SoundMap res = new SoundMap();
		CompileContext cc = new CompileContext(tempoMultiplyer);
		
		for(Part part : allParts) {
			part.writeSound(cc, res);
		}
		
		return res;
	}
}
