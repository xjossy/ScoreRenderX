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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;

import com.xpianotools.scorerenderx.FontEncoder.Glyph;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.Log;

public class PaintLogger implements Serializable {
	private static final String TAG = "PaintLogger";
	private static final long serialVersionUID = 3705105534699779993L;
	private static Matrix preMatrixInv;
	
	private PaintLogger(Matrix preMatrix) {
		preMatrixInv = new Matrix();
		preMatrix.invert(preMatrixInv);
	}

	public static class CompositionLogger implements Serializable {
		private static final long serialVersionUID = 7269029789476459474L;
		private int width;
		private int lineHeight;
		private ArrayList<Pair<Integer,PaintLogger> > lines = new
				ArrayList<Pair<Integer,PaintLogger> >();
		
		public CompositionLogger() {
			//this.width = width;
		}
		
		@SuppressWarnings("deprecation")
		public void startLine(Canvas canvas, int lineHeight, int width) {
			this.width = width;
			this.lineHeight = lineHeight;
			PaintLogger.startLogging(canvas.getMatrix());
		}
		
		public void endLine() {
			Log.d(TAG, "Log line " + logger.eventCount + " paint events");
			PaintLogger logger = PaintLogger.endLogging();
			lines.add(new Pair<Integer,PaintLogger>(lineHeight, logger));
		}
		
		public void init(Resources res) {;
			for(int i=0;i<lines.size();++i) {
				Pair<Integer,PaintLogger> line = lines.get(i);
				line.second.init(res);
			}
		}
		
		public void paint(Canvas canvas, Paint paint, int width, int height) {
			canvas.save();
			float scale = (float)width / this.width;
			canvas.scale(scale, scale);
			
			for(int i=0;i<lines.size();++i) {
				Pair<Integer,PaintLogger> line = lines.get(i);
				line.second.draw(canvas, paint);
				canvas.translate(0,line.first);
			}
			canvas.restore();
		}
		
		public void paint(Canvas canvas, Paint paint, int width, int height, int line, int hash) {
			canvas.save();
			float scale = (float)width / this.width;
			canvas.scale(scale, scale);
			lines.get(line).second.draw(canvas, paint, hash);
			canvas.restore();
		}
		
		public int linesCount() {
			return lines.size();
		}
		
		public int randomHash(int line, Random random) {
			return lines.get(line).second.randomHash(random);
		}
		
		public float getLineHeight(int line, int width) {
			return lines.get(line).first / this.width * width; 
		}
	}
	@SuppressLint("UseSparseArrays") 
	HashMap<Integer, ArrayList<PaintEvent> > allPaintEvents = 
			new HashMap<Integer, ArrayList<PaintEvent> >();
	transient int eventCount = 0;
	transient int hash = 0;
	transient HashSet<Integer> existingHashes = new HashSet<Integer>();
	transient ArrayList<Integer> hashes = null;
	
	private static PaintLogger logger = null;
	
	private static class SerializableMatrix implements Serializable {
		private static final long serialVersionUID = -1824130415826455985L;
		private float values[] = new float[9];
		public SerializableMatrix(Matrix matrix) {
			matrix.getValues(values);
		}
		public void toMatrix(Matrix m) {
			m.setValues(values);
		}
		/*public int realHash() {
			return Objects.hash(values);
		}*/
	}
	
	static abstract class PaintEvent implements Serializable {
		private static final long serialVersionUID = 7507607231098866491L;

		@SuppressWarnings("deprecation")
		public PaintEvent(Canvas canvas) {
			Matrix m = canvas.getMatrix();
			m.postConcat(preMatrixInv);
			canvasMatrix = new SerializableMatrix(m);
		}
		
		protected abstract void drawObjects(Canvas canvas, Paint paint); 
		
		public void draw(Canvas canvas, Paint paint) {
			canvas.save();
			canvas.concat(matrix);
			drawObjects(canvas, paint);
			canvas.restore();
		}
		
		public void init(Resources res) {}
		
		private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		    ois.defaultReadObject();
		    matrix = new Matrix();
		    canvasMatrix.toMatrix(matrix);
		}

		transient Matrix matrix = null;
		SerializableMatrix canvasMatrix = null;

		/*public int realHash() {
			return canvasMatrix.realHash();
		}*/
	}
	
	static class LineDrawEvent extends PaintEvent {
		private static final long serialVersionUID = -8952331119244577671L;
		public LineDrawEvent(Canvas canvas, PointF p1, PointF p2, float strokeWidth) {
			super(canvas);
			this.x1 = p1.x;
			this.y1 = p1.y;

			this.x2 = p2.x;
			this.y2 = p2.y;
			
			this.strokeWidth = strokeWidth;
		}
		float x1,y1;
		float x2,y2;
		float strokeWidth;
		
		@Override protected void drawObjects(Canvas canvas, Paint paint) {
			paint.setStrokeWidth(strokeWidth);
			canvas.drawLine(x1, y1, x2, y2, paint);
		}
		
		/*@Override public int realHash() {
			return Objects.hash(serialVersionUID, x1,y1,x2,y2,strokeWidth,super.realHash());
		}*/
	}
	
	static class GlyphDrawEvent extends PaintEvent {
		private static final long serialVersionUID = -5121057821108318786L;
		public GlyphDrawEvent(Canvas canvas, String layer, Glyph glyph) {
			super(canvas);
			this.layer = layer;
			this.glyphResource = glyph.resourceId;
		}
		
		@Override public void init(Resources res) {
			if( glyphResource==-1 ) return;
			glyph = FontEncoder.createFromResouce(res, glyphResource);
		}
		
		@Override protected void drawObjects(Canvas canvas, Paint paint) {
			if( glyph==null ) return;
			glyph.draw(canvas, paint);
		}

		transient Glyph glyph = null;
		int glyphResource;
		String layer;
		
		/*@Override public int realHash() {
			return Objects.hash(serialVersionUID, glyphResource, layer, super.realHash());
		}*/
	}
	
	static class PathDrawEvent extends PaintEvent {
		private static final long serialVersionUID = -8802129295335683541L;
		CustomPath path;

		public PathDrawEvent(Canvas canvas, CustomPath path) {
			super(canvas);
			this.path = path;
		}

		@Override protected void drawObjects(Canvas canvas, Paint paint) {
			canvas.drawPath(path, paint);
		}
		
		/*@Override public int realHash() {
			return Objects.hash(serialVersionUID, glyphResource, layer, super.realHash());
		}*/
	}

	static class RectDrawEvent extends PaintEvent {
		private static final long serialVersionUID = -5691339959249090339L;
		public RectDrawEvent(Canvas canvas, float x1, float y1, float x2, float y2) {
			super(canvas);
			this.x1 = x1;
			this.x2 = x2;
			this.y1 = y1;
			this.y2 = y2;
		}
		float x1;
		float y1;
		float x2;
		float y2;

		@Override protected void drawObjects(Canvas canvas, Paint paint) {
			canvas.drawRect(x1, y1, x2, y2, paint);
		}

		/*@Override public int realHash() {
			return Objects.hash(serialVersionUID, x1, y1, x2, y2, super.realHash());
		}*/
	}
	
	private static int toHash( Serializable o ) {
		try {
	        ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        ObjectOutputStream oos = new ObjectOutputStream( baos );
	        oos.writeObject( o );
	        oos.close();
	        return baos.toByteArray().hashCode();
		} catch(Exception e) {}
		
		return -1;
    }
	
	private void log(PaintEvent event) {
		int hash = toHash(event);
		if( existingHashes.contains(hash) ) return;
		
		ArrayList<PaintEvent> events = allPaintEvents.get(this.hash);
		if( events==null ) {
			events = new ArrayList<PaintEvent>();
			allPaintEvents.put(this.hash, events);
		}
		
		eventCount++;
		events.add(event);
		existingHashes.add(hash);
	}
	
	private void init(Resources res) {
		for(Map.Entry<Integer, ArrayList<PaintEvent> > entry : allPaintEvents.entrySet()) {
			for(int i=0;i<entry.getValue().size();++i) {
				PaintEvent event = entry.getValue().get(i);
				event.init(res);
			}
		}
		this.hashes = new ArrayList<Integer>(allPaintEvents.keySet());
	}
	
	private void draw(Canvas canvas, Paint paint) {
		for(Map.Entry<Integer, ArrayList<PaintEvent> > entry : allPaintEvents.entrySet()) {
			paint.setColor(entry.getKey());
			for(int i=0;i<entry.getValue().size();++i) {
				PaintEvent event = entry.getValue().get(i);
				event.draw(canvas, paint);
			}
		}
	}
	
	private void draw(Canvas canvas, Paint paint, int hash) {
		ArrayList<PaintEvent> arr = allPaintEvents.get(hash);
		if( arr==null ) return;
		for(int i=0;i<arr.size();++i) {
			PaintEvent event = arr.get(i);
			event.draw(canvas, paint);
		}
	}
	
	private int randomHash(Random random) {
		if( hashes.size()<=1 ) return 0; 
		for(;;) {
			int hash = hashes.get(random.nextInt(hashes.size()));
			if( hash!=0 ) return hash;
		}
	}
	
	public static void startLogging(Matrix preMatrix) {
		logger = new PaintLogger(preMatrix);
	}
	
	public static PaintLogger endLogging() {
		PaintLogger res = logger;
		logger = null;
		
		return res;
	}

	public static void logGlyph(Canvas canvas, String layer, FontEncoder.Glyph glyph) {
		if( logger==null ) return;
		logger.log(new GlyphDrawEvent(canvas, layer, glyph));
	}

	public static void logLines(Canvas canvas, float[] pts, float strokeWidth) {
		if( logger==null ) return;
		for(int i=1;i<pts.length/2;i+=2) {
			logger.log(new LineDrawEvent(canvas, 
					new PointF(pts[(i-1) * 2], pts[(i-1) * 2 + 1]),
					new PointF(pts[(i  ) * 2], pts[(i  ) * 2 + 1]), strokeWidth ));
		}
	}

	public static void logLine(Canvas canvas, float x1, float y1, float x2,
			float y2, float strokeWidth) {
		if( logger==null ) return;
		float pts[] = new float[]{x1,y1,x2,y2};
		logLines(canvas,pts, strokeWidth);
	}

	public static void logPath(Canvas canvas, CustomPath path) {
		if( logger==null ) return;
		logger.log(new PathDrawEvent(canvas, path));
	}

	public static void logRect(Canvas canvas, float x1, float y1, float x2,
			float y2) {
		if( logger==null ) return;
		logger.log(new RectDrawEvent(canvas, x1,y1,x2,y2));
	}

	public static void setHash(Object object) {
		if( logger==null ) return;
		logger.hash = object==null ? 0 : object.hashCode();
	}
}
