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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import org.xml.sax.Attributes;

import android.graphics.Color;
import android.util.Log;
import android.util.SparseArray;
//import android.util.Log;

public class MusicXMLreader extends XMLstateBasedParser.TParser<Composition> {
	static final String TAG = "MusicEncoder";
	
	@Override
	protected THandler createHandler() {
		return new MXLParser();
	}
	
	private class MXLParser extends THandler {
		class StartState extends ParserState {
			@Override 
			public ParserState getNextState(String namespaceURI, String localName, String qName, org.xml.sax.Attributes atts) {
				if( localName.equals("score-partwise") ) partwise = true;
				if( localName.equals("score-timewise") ) partwise = false;
				return sFirstLevelStructState;
			}
		}
		
		@Override
		protected StartState getStartState() {
			return new StartState();
		}
		
		private Composition composition = new Composition(0);
		
		private class Part {
			public int staffOffset = staffsNumber;
			int staffs = 0;
			Integer midiInstrument = null;
			Part() {
				setStaffs(1);
			}
			void setStaffs(int staffs) {
				staffsNumber += staffs - this.staffs;
				for(int i=this.staffs;i<staffs;++i) {
					writer.changeStaff(i);
					writer.setClef(Clef.CLEF_G);
				}
				this.staffs = staffs;
				composition.setStaffs(staffsNumber);
				if( midiInstrument!=null) setInstrument(midiInstrument);
			}
			void setInstrument(int midiProgram) {
				this.midiInstrument = midiProgram;
				composition.instrumentByStaff.ensureCapacity(staffOffset + staffs);
				while(composition.instrumentByStaff.size()<staffOffset) {
					composition.instrumentByStaff.add(-1);
				}
				for(int i=staffOffset;i<staffOffset + staffs;++i) {
					if( i<composition.instrumentByStaff.size() )
						composition.instrumentByStaff.set(i, midiProgram);
					else 
						composition.instrumentByStaff.add(midiProgram);
				}
			}
		}
		private TreeMap<String, Part> parts = new TreeMap<String, Part>();
		private ArrayList<Part> partsArray = new ArrayList<Part>();
		
		private class ReadingMeasure {
			SparseArray<TreeSet<Integer> > voicesByStaff = new SparseArray<TreeSet<Integer> >();
			HashSet<Composition.ChordList> chordLists  = new HashSet<Composition.ChordList>();
			
			void setVoicesMode() {
				for(int i=0;i<voicesByStaff.size();++i) {
					for(Composition.ChordList chordList : chordLists) {
						chordList.setVoiceMode(voicesByStaff.keyAt(i),voicesByStaff.valueAt(i));
					}
				}
			}
			void clear() {
				voicesByStaff.clear();
				chordLists.clear();
			}
			void addVoice(int staff, int voice) {
				TreeSet<Integer> set = voicesByStaff.get(staff);
				if( set==null ) {
					set = new TreeSet<Integer>();
					voicesByStaff.put(staff, set);
				}
				set.add(voice);
			}
			void addChordList(Composition.ChordList cl) {
				chordLists.add(cl);
			}
		}
		ReadingMeasure readingMeasure = new ReadingMeasure();
		
		int staffsNumber = 0;
		
		Part currentPart = null;
		//Measure currentMeasure = null;
		//int currentMeasure = 0;
		//Composition.Note currentNote = null;
		int divisions = 1;
		//RationalNumber measureStart = RationalNumber.Zero;
		//RationalNumber nextMeasure  = RationalNumber.Zero;
		
		//Composition.Part firstCompositionPart = composition.start();
		//Composition.Part currentCompositionPart = null;
		//ListIterator<Composition.Part> partIterator = composition.allParts.listIterator();
		Composition.Part.VoiceWriter writer = composition.start().startWriting(0);
		HashMap<Composition.Part.VoiceWriter, Integer> beamsByWriter = new HashMap<Composition.Part.VoiceWriter, Integer>(); 
		
		RationalNumber lastChordPosition = RationalNumber.Zero;
		RationalNumber lastChordPositionGrace = RationalNumber.Zero;
		
		//Integer beamNumber = null;
		
		private boolean partwise;
		
		private class PartMeasureStructState extends ParserState {
			boolean firstLevel;
			
			PartMeasureStructState(boolean firstLevel) {
				this.firstLevel = firstLevel;
			}
			
			@Override 
			public void end(String uri, String localName, String qName) {
				if( firstLevel ) for(Part part : partsArray) {
					composition.staffFormat.addPart(part.staffs);
				}
			}
			
			@Override
			public ParserState getNextState(String namespaceURI, String localName,
					String qName, Attributes atts) {
				if( localName.equals("part-list") && firstLevel ) {
					return new PartListState();
				} else if( localName.equals("part") && (partwise == firstLevel) ) {
					String id = atts.getValue("id");
					currentPart = parts.get(id);
					if( currentPart == null ) {
						currentPart = new Part();
						parts.put(id, currentPart);
						partsArray.add(currentPart);
					}
					if( partwise ) {
						//partIterator = composition.allParts.listIterator();
						//currentCompositionPart = null;
						//measureStart = RationalNumber.Zero;
						//currentMeasure = 0;
						if( writer!=null ) writer.resetMeasure();
					} else {
						if( writer!=null ) writer.toCurrentMeasure();
					}
				} else if( localName.equals("measure") && (partwise != firstLevel) ) {
					if( writer != null ) {
						/*writer.moveTo(measureStart);
						if( currentMeasure!=0 ) writer.advanceMeasure();*/
						writer.toNextMeasure();
						//measureStart = writer.getPosition();
					}
					//currentMeasure++;
				} else return null;
				
				if( firstLevel ) return sSecondLevelStructState;
				else {
					readingMeasure.clear();
					return sMusicData;
				}
			}
			
			@Override protected void returnedFrom(String localName) {
				if( localName.equals("measure") ) {
					writer.endMeasure();
				}
			}

			class PartListState extends ParserState {
				@Override
				public ParserState getNextState(String localName, Attributes atts) {
					if( localName.equals("score-part") ) {
						String id = atts.getValue("id");
						currentPart = parts.get(id);
						if( currentPart == null ) {
							currentPart = new Part();
							parts.put(id, currentPart);
							partsArray.add(currentPart);
						}
						return new ParserState() {
							@Override
							public ParserState getNextState(String localName, Attributes atts) {
								if( localName.equals("midi-instrument") ) {
									return new ParserState() {
										@Override
										public ParserState getNextState(String localName, Attributes atts) {
											if( localName.equals("midi-program") ) {
												return new IntegerState() {
													@Override protected void integer(int val) {
														currentPart.setInstrument(val);
													}
												};
											}
											return null;
										}
									};
								}
								return null;
							}
						};
					}
					return null;
				}
			}
		}
		public PartMeasureStructState sFirstLevelStructState = new PartMeasureStructState(true);
		public PartMeasureStructState sSecondLevelStructState = new PartMeasureStructState(false);
		
		private class MusicDataState extends ParserState {
			@Override
			public ParserState getNextState(String namespaceURI, String localName,
					String qName, Attributes atts) {
				if( localName.equals("attributes") ) return sAttributes;
				if( localName.equals("note")       ) return new NoteState();
				if( localName.equals("backup")     ) return sBackup;
				if( localName.equals("forward")    ) return sForward;
				if( localName.equals("direction")  ) return new DirectionState();
				return null;
			}

			@Override
			public void end(String uri, String localName, String qName) {
				readingMeasure.setVoicesMode();
			}
		}
		MusicDataState sMusicData = new MusicDataState();
		
		private class BackupState extends ParserState {
			boolean forward;
			int duration;
			
			BackupState(boolean forward) {
				this.forward = forward;
			}
			
			@Override
			public ParserState getNextState(String namespaceURI, String localName, String qName, Attributes atts) {
				Log.d("backup", "backup " + duration);
				
				if( localName.equals("duration")) return new IntegerState() {
					@Override
					protected void integer(int val) {
						duration = forward ? val : -val;
					}
				};
				return null;
			}
			@Override
			public void end(String uri, String localName, String qName) {
				//Log.d("backup", "backup " + duration);
				writer.advanceTime(new RationalNumber(duration,4*divisions));
			}
		}
		BackupState sBackup  = new BackupState(false);
		BackupState sForward = new BackupState(true );
		
		private class DirectionState extends ParserState {
			int staff = 0;
			String dynamics = null;
			String tempo = null; 
			@Override public ParserState getNextState(String localName, Attributes atts) {
				if( localName.equals("sound")) {
					tempo = atts.getValue("tempo");
					dynamics = atts.getValue("dynamics");
				} else if (localName.equals("staff")) {
					return new IntegerState() {
						@Override protected void integer(int val) {
							staff = val;
						}
					};
				}
				return null;
			}
			@Override protected void end(String localName) {
				if( tempo!=null ) writer.setTempo((int)Float.parseFloat(tempo));
				if( dynamics!=null ) {
					writer.setDynamics((int)Float.parseFloat(dynamics), staff - 1);
				}
			}
		}
		//DirectionState sDirection = new DirectionState();
		
		private class AttributesState extends ParserState {
			@Override
			public ParserState getNextState(String namespaceURI, String localName, String qName, Attributes atts) {
				if( localName.equals("divisions") ) return sDivisions;
				if( localName.equals("key") ) return sKey;
				if( localName.equals("time") ) {
					return new TimeState(atts.getValue("symbol"));
				}
				if( localName.equals("staves") ) {
					return sStaves;
				}
				if( localName.equals("clef") ) {
					String number = atts.getValue("number");
					return new ClefState(number != null ? Integer.parseInt(number) : 1);
				}
				return null;
			}

			@Override
			public void end(String uri, String localName, String qName) {}
			
			private class DivisitonsState extends IntegerState {
				@Override
				protected void integer(int val) {
					divisions = val;
				}
			}
			DivisitonsState sDivisions = new DivisitonsState();
			
			private class StavesState extends IntegerState {
				@Override
				protected void integer(int val) {
					currentPart.setStaffs(val);
				}
			}
			StavesState sStaves = new StavesState();
			
			private class KeyState extends ParserState {
				int fifths = 0;
				boolean major = true;
				@Override
				public ParserState getNextState(String namespaceURI, String localName, String qName, Attributes atts) {
					if( localName.equals("fifths") ) return new FifthsState();
					if( localName.equals("mode") ) return new ModeState();
					return null;
				}
				
				class FifthsState extends IntegerState {
					@Override
					protected void integer(int val) {
						fifths = val;
					}
				}
				
				private class ModeState extends StringState {
					@Override protected void string(String str) {
						major =!str.equals("minor");
					}
				}
				
				@Override
				protected void end(String uri, String localName, String qName) {
					Key majorKey = Key.Base.C.natural.altered(Key.Base.G.number * fifths, Key.Base.G.offset * fifths);
					Tonality tonality = majorKey.major;
					writer.setKey( major ? tonality : tonality.parallel() );
				}
			}
			KeyState sKey = new KeyState();
			
			private class TimeState extends ParserState {
				String symbol;
				int beats;
				int beatType;
				
				TimeState(String symbol) {
					this.symbol = symbol;
				}
				
				@Override
				public ParserState getNextState(String namespaceURI, String localName, String qName, Attributes atts) {
					if( localName.equals("beats") ) return new BeatsState();
					if( localName.equals("beat-type") ) return new BeatTypeState();
					return null;
				}
				
				class BeatsState extends IntegerState {
					@Override
					protected void integer(int val) {
						beats = val;
					}
				}
				class BeatTypeState extends IntegerState {
					@Override
					protected void integer(int val) {
						beatType = val;
					}
				}
				
				@Override
				protected void end(String uri, String localName, String qName) {
					/*if(!partIterator.hasNext() ) {
						currentCompositionPart = composition.startPart();
					} else currentCompositionPart = partIterator.next();*/
					
					Meter meter = new Meter(beats,beatType);
					if( symbol != null && symbol.equals("common")) meter.setSymbol(Meter.Symbol.C);
					if( symbol != null && symbol.equals("cut")) meter.setSymbol(Meter.Symbol.CCUT);
					//currentCompositionPart.setMeter(meter);
					
					writer.setMeter(meter);
					//writer = currentCompositionPart.startWritingAtBeginning();
				}
			}
			
			private class ClefState extends ParserState {
				Clef clef = Clef.CLEF_G;
				int staff;
				
				ClefState(int staff) {
					this.staff = staff;
				}
				
				@Override
				public ParserState getNextState(String namespaceURI, String localName, String qName, Attributes atts) {
					if( localName.equals("sign") ) return new SignState();
					return null;
				}
				
				private class SignState extends StringState {
					@Override protected void string(String str) {
						if( str.equals("F") ) clef = Clef.CLEF_F;
						else if( str.equals("G") ) clef = Clef.CLEF_G;
					}
				}
				
				@Override
				protected void end(String uri, String localName, String qName) {
					super.end(uri, localName, qName);
					
					writer.changeStaff(staff + currentPart.staffOffset - 1);
					writer.setClef(clef);
				}
			}
		}
		AttributesState sAttributes = new AttributesState();
		
		private class NoteState extends ParserState {
			private class PitchState extends ParserState {
				Key.Base step = Key.Base.C;
				int octave = 3;
				int alter = 0;
				
				private class StepState extends StringState {
					static final String keys = "CDEFGAB";
					@Override
					public void string(String str) {
						int index = keys.indexOf(str.charAt(0));
						if( index>=0 ) step = Key.Base.get(index);
					}
				}
				StepState sStep = new StepState();
				
				private class AlterState extends IntegerState {
					@Override
					public void integer(int arg) {
						alter = arg;
					}
				}
				AlterState sAlter = new AlterState();
				
				private class OctaveState extends IntegerState {
					@Override
					public void integer(int arg) {
						octave = arg;
					}
				}
				OctaveState sOctave = new OctaveState();
				
				@Override
				public ParserState getNextState(String namespaceURI, String localName, String qName, Attributes atts) {
					if( localName.equals("step"  ) ) return sStep;
					if( localName.equals("octave") ) return sOctave;
					if( localName.equals("alter" ) ) return sAlter;
					return null;
				}

				@Override
				public void end(String uri, String localName, String qName) {
					key = step.altered(alter);
					NoteState.this.octave = octave - 3;
				}
			}

			private class TypeState extends StringState {
				List<String> values = Arrays.asList(new String[]{"long", "breve", "whole", "half", "quarter", "eighth", 
						"16th", "32nd", "64th", "128th", "256th"});
				@Override
				public void string(String str) {
					int index = values.indexOf(str);
					if( index>=0 ) power2 = index - 2;
					else Log.e(TAG, "Unknown note type " + str);
				}
			}
			
			private class BeamState extends StringState {
				//int number;
				
				boolean start = false;
				boolean end   = false;
				
				BeamState(String number) {
					//this.number = number == null ? -1 : Integer.parseInt(number);
				}
				@Override
				public void string(String str) {
					if( str.equals("begin") ) this.start = true;
					if( str.equals("end")   ) this.end = true;
				}
				
				@Override
				public void end(String uri, String localName, String qName) {
					super.end(uri, localName, qName);
					
					/*if( beamNumber!=null && !beamNumber.equals(number) ) return;
					if( start && beamNumber==null ) {
						beamNumber = number;
						if( writer!=null ) beamStart = true;
					} else if( end && beamNumber!=null ) {
						beamNumber = null;
						if( writer!=null ) beamEnd = true;
					}*/
					
					if( start ) beams += 1;
					if( end   ) beams -= 1;
				}
			}
			
			private class DurationState extends IntegerState {
				int duration = 1;
				@Override
				public void integer(int arg) {
					duration = arg;
				}
				@Override
				public void end(String uri, String localName, String qName) {
					super.end(uri,localName, qName);
					NoteState.this.duration = new RationalNumber(duration,divisions * 4);
				}
			}
			private class StaffState extends IntegerState {
				@Override
				public void integer(int arg) {
					staff = arg ;
				}
			}
			private class VoiceState extends IntegerState {
				@Override
				public void integer(int arg) {
					voice = arg;
				}
			}
			boolean    chord  = false;
			int        staff  = 1;
			int        voice  = 1;
			Key     key = null;
			int     octave = 0;
			int     power2 = 0;
			int     dots = 0;
			int     color = 0xff000000;
			boolean tied = false;
			boolean rest = false;
			boolean grace = false;
			boolean graceSlash;
			RationalNumber duration = null;
			
			//boolean beamStart = false;
			//boolean beamEnd   = false;
			int beams = 0;
			
			@Override
			public void init() {
			}
			@Override
			public ParserState getNextState(String namespaceURI, String localName, String qName, Attributes atts) {
				if( localName.equals("chord") ) chord = true;
				if( localName.equals("pitch") ) return new PitchState();
				if( localName.equals("duration") ) return new DurationState();
				if( localName.equals("staff") ) return new StaffState();
				if( localName.equals("voice") ) return new VoiceState();
				if( localName.equals("rest" ) ) rest = true;
				if( localName.equals("dot"  ) ) dots++;
				if( localName.equals("type" ) ) return new TypeState();
				if( localName.equals("beam" ) ) return new BeamState(atts.getValue("number"));
				if( localName.equals("notehead" ) ) {
					String color = atts.getValue("color");
					if( color==null || color.charAt(0)!='#' ) return null;
					try {
						/*this.color = Integer.parseInt(color.substring(1),16);
						if( color.length()==3 ) 
							this.color = 
								((this.color & 0xF00) * (0x1100)) | 
								((this.color & 0x0F0) * (0x0110)) |
								((this.color & 0x00F) * (0x0011)) ;
						if( color.length()<8 ) this.color |= 0xff000000;*/
						this.color = Color.parseColor(color);
					} catch(IllegalArgumentException e) {}
				}
				if( localName.equals("tie"  ) ) {
					if( atts.getValue("type") != null && atts.getValue("type").equals("start") ) tied = true;
				}
				if( localName.equals("grace") ) {
					grace = true;
					String slash = atts.getValue("slash");
					graceSlash = slash == null || !slash.equals("no");
				}
				return null;
			}

			@Override
			public void end(String uri, String localName, String qName) {
				Composition.Part.VoiceWriter cWriter = !grace ? writer : composition.getGraceVoiceWriter(graceSlash);
				
				if( cWriter!=null ) {
					cWriter.changeVoice(voice);
					cWriter.changeStaff(staff + currentPart.staffOffset - 1);
					
					if( chord ) {
						RationalNumber pos = grace ? lastChordPositionGrace : lastChordPosition;
						cWriter.moveTo(pos);
					} else {
						if(!grace ) lastChordPosition = cWriter.getPosition();
						else lastChordPositionGrace = cWriter.getPosition();
					}

					Integer prevBeams = beamsByWriter.get(cWriter);
					if( prevBeams == null ) prevBeams = 0;
					int newBeams = prevBeams + beams;
					
					if( prevBeams <= 0 && newBeams > 0 ) {
						cWriter.startBeam();
					}
					if( rest ) {
						Composition.Rest rest = new Composition.Rest(power2, duration);
						rest.setDots(dots);
						rest.setColor(color);
						cWriter.write(rest);
					} else {
						Composition.Note note = new Composition.Note(key,octave,power2, duration);
						note.setDots(dots);
						note.setColor(color);
						if( tied ) note.tied();
						
						cWriter.write(note);
					}
					
					if(!grace ) {
						readingMeasure.addVoice(cWriter.getStaff(), cWriter.getVoice());
						readingMeasure.addChordList(cWriter.getLastChordList());
					}
					
					if( prevBeams > 0 && newBeams <= 0 ) {
						cWriter.endBeam();
					}
					
					beamsByWriter.put(cWriter, newBeams);
				}
			}
		}
		//NoteState sNote = new NoteState();

		@Override
		protected Composition getResult() {
			return composition;
		}
	}
}
