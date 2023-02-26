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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.content.res.AssetManager;
import android.content.res.Resources;

public abstract class XMLstateBasedParser extends DefaultHandler {
	public static interface Source {
		public InputStream createSource() throws IOException;
	}
	public static class ResourceSource implements Source {
		Resources resources; int id;
		public ResourceSource(Resources resources, int id) {
			this.resources = resources;
			this.id = id;
		}
		@Override public InputStream createSource() {
			return resources.openRawResource(id);
		}
	}
	public static class AssetSource implements Source {
		AssetManager assets; String path;
		public AssetSource(AssetManager assets, String path) {
			this.assets = assets;
			this.path   = path;
		}
		@Override public InputStream createSource() throws IOException {
			return assets.open(path);
		}
	}
	public static class FileSource implements Source {
		File file;
		public FileSource(File file) {
			this.file = file;
		}
		@Override public InputStream createSource() {
			try {
				return new BufferedInputStream(new FileInputStream(file));
			} catch (FileNotFoundException e) {}
			return null;
		}
	}
	public static class InputStreamSource implements Source {
		InputStream stream;
		public InputStreamSource(InputStream stream) {
			this.stream = stream;
		}
		@Override public InputStream createSource() {
			return stream;
		}
	}
	public static abstract class TParser<T> {
		public T parseMetainf(Source source) {
			return metainfParser(source).parse();
		}
			
		public T parseCompressed(Source source, String file) {
			return compressedParser(source, file).parse();
		}
		
		public T parse(Source source) {
			return parser(source).parse();
		}
		
		public ParserWithResult metainfParser(Source source) {
			String file = null;
			try {
				file = getMetainfFile(source.createSource());
			} catch (IOException e) {}
			if( file==null ) return null;
			return compressedParser(source, file);
		}
			
		public ParserWithResult compressedParser(Source source, String file) {
			ZipInputStream zip = null;
			
			if( source != null && file != null ) {
				try {
					zip = new ZipInputStream(source.createSource());
					if(!zipGoto(file, zip) ) zip = null;
				} catch (IOException e) { }
			}
	        
	        return new ParserWithResult(zip);
		}
		
		public ParserWithResult parser(Source source) {
			InputStream stream = null;
	        try {
	        	stream = source.createSource();
			} catch (IOException e) {}
			return new ParserWithResult(stream);
		}
		
		protected abstract THandler createHandler();
		protected abstract class THandler extends XMLstateBasedParser {
			protected abstract T getResult();
		}
		
		public class ParserWithResult {
			private InputStream source;
			THandler handler = createHandler();
			
			public ParserWithResult(InputStream source) {
				this.source = source;
			}
			
			public InputStream getSource() {
				return source;
			}
			
			public T parse() {
				if( source==null || handler==null ) return null;
				
				T result = null;
				try {
					handler.parse(source);
					result = handler.getResult();
				} catch (ParserConfigurationException | SAXException | IOException e) {
					e.printStackTrace();
				}
				
				return result;
			}
			
			public void interrupt() {
				handler.interrupt();
			}
		}
	}
	public XMLstateBasedParser() {
		states.add(getStartState());
	}
	
	public static String getMetainfFile(InputStream source) {
		if( source==null ) return null;
		ZipInputStream zip;
		
		try {
			zip = new ZipInputStream(source);
			if(!zipGoto("META-INF/container.xml", zip) ) return null;

			MetainfContainerParser parser = new MetainfContainerParser();
			
			XMLReader xr = null;
			SAXParserFactory spf = SAXParserFactory.newInstance();
	        SAXParser sp = spf.newSAXParser();
	        xr = sp.getXMLReader();
	        xr.setContentHandler(parser);
	        xr.parse(new InputSource(zip));
	        
	        return parser.fileName;
		} catch (ParserConfigurationException e) {
		} catch (SAXException e) {
		} catch (IOException e) {
		}
		return null;
	}
	
	private static class MetainfContainerParser extends DefaultHandler {
		public String fileName = null;
		
		@Override
		public void startElement(String namespaceURI, String localName, String qName, Attributes atts) { 
			if( localName=="rootfile" && fileName==null ) {
				fileName = atts.getValue("full-path");
			}
		}
	}
	
	/*public void parseCompressed(InputStream source, String fileName) throws IOException, ParserConfigurationException, SAXException {
		if( source == null || fileName == null ) throw new NullPointerException();
		ZipInputStream zip;
		zip = new ZipInputStream(source);
        
        if(!zipGoto(fileName, zip) ) throw new IOException();
        
        parse(zip);
	}*/

	private static boolean zipGoto(String entryName, ZipInputStream zip) throws IOException {
		for(;;) {
			ZipEntry entry = zip.getNextEntry();
			if( entry==null ) break;
			if( entry.getName().equals(entryName) ) return true;
		}
		return false;
	}
	
	public void parse(InputStream source) throws ParserConfigurationException, SAXException, IOException {
		if( source == null ) throw new NullPointerException();
		
		XMLReader xr = null;
		SAXParserFactory spf = SAXParserFactory.newInstance();
        SAXParser sp = spf.newSAXParser();
        xr = sp.getXMLReader();
        xr.setContentHandler(this);
        xr.parse(new InputSource(source));
	}
	
	protected abstract ParserState getStartState();

	protected abstract class ParserState {
		private int nestedCounter = 0;
		
		protected void characters(char[] ch,int start,int length) {}
		
		protected ParserState getNextState(String namespaceURI, String localName, String qName, Attributes atts) { 
			return getNextState(localName, atts);
		}
		protected ParserState getNextState(String localName, Attributes atts) { 
			return null;
		}
		protected void end(String uri, String localName, String qName) {
			end(localName);
		}
		protected void end(String localName) {}
		protected void returnedFrom(String uri, String localName, String qName) {
			returnedFrom(localName);
		}
		protected void returnedFrom(String localName) {}
		
		protected void init() {}
		
		public final void startElement(String namespaceURI, String localName, String qName, Attributes atts) {
			if( nestedCounter==0 ) {
				ParserState next = getNextState(namespaceURI, localName, qName, atts);
				if( next==null ) {
					nestedCounter++;	
				} else {
					next.init();
					states.push(next);
				}
			} else {
				nestedCounter++;
			}
		}
		public final void endElement(String uri, String localName, String qName) {
			if( nestedCounter==0 ) {
				end(uri,localName,qName);
				states.pop();
				if(!states.empty() ) {
					states.peek().returnedFrom(uri, localName, qName);
				}
				return;
			}
			nestedCounter--;
		}
	}

	protected abstract class StringState extends ParserState {
		private String string = new String();
		@Override
		protected void characters(char[] ch,int start,int length) {
			string += new String(ch,start,length);
		}
		@Override
		protected void end(String uri, String localName, String qName) {
			string(string.trim());
		}
		protected abstract void string(String string);
	}
	
	protected abstract class IntegerState extends StringState {
		@Override
		protected void string(String str) {
			if( str.isEmpty() ) return;
			try {
				integer(Integer.parseInt(str));
			} catch( NumberFormatException e) {}
		}
		protected abstract void integer(int val);
	}
	
	protected abstract class FloatState extends StringState {
		@Override
		protected void string(String str) {
			if( str.isEmpty() ) return;
			try {
				value(Float.parseFloat(str));
			} catch( NumberFormatException e) {}
		}
		protected abstract void value(float val);
	}
	
	private Stack<ParserState> states = new Stack<ParserState>();
	private volatile boolean interrupted = false;
	
	public void interrupt() {
		interrupted = true;
	}

	@Override
    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
		if( interrupted ) throw new SAXException();
		states.peek().startElement(namespaceURI, localName, qName, atts);
	}
	@Override
	public void endElement(java.lang.String uri, java.lang.String localName, java.lang.String qName) throws SAXException {
		if( interrupted ) throw new SAXException();
		states.peek().endElement(uri, localName, qName);
	}
	
	@Override
	public void characters(char[] ch,int start,int length) throws SAXException {
		if( interrupted ) throw new SAXException();
		states.peek().characters(ch, start, length);
	}
}
