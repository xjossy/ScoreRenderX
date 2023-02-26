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

import java.util.HashMap;
import java.util.Map;

import com.xpianotools.scorerenderx.StaffGroup.Staff;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;

public class MusicDrawer {
	//public  int mScale; // standard pixels in mm/10;
	public  static final float stemWidthFactor = 0.0945f;
	public  static final float stemPosFactor   = 0.6391f;
	public  static final float stemDefaultLength = 3f;
	private Context context;
	
	public  Font font;
	
	public MusicDrawer(Context context) {
		this.context = context;
		//mScale = (int) Math.ceil(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, 0.1f, 
        //        context.getResources().getDisplayMetrics()));
		
		//lineWidth = mScale;
		
		font = new Font(context.getResources());
		
		initizalizeHeadMap();
	}

	public static class NoteLength {
		public static final NoteLength L_DOUBLE        = new NoteLength(-1);
		public static final NoteLength L_WHOLE         = new NoteLength(0);
		public static final NoteLength L_HALF          = new NoteLength(1);
		public static final NoteLength L_QUATER        = new NoteLength(2);
		public static final NoteLength L_EIGHTH        = new NoteLength(3);
		public static final NoteLength L_SIXTEENTH     = new NoteLength(4);
		public static final NoteLength L_THIRTY_SECOND = new NoteLength(5);
		public static final NoteLength L_SIXTY_FOURTH  = new NoteLength(6);
		
		public static final NoteLength values[] = {
			L_DOUBLE       ,
			L_WHOLE        ,
			L_HALF         ,
			L_QUATER       ,
			L_EIGHTH       ,
			L_SIXTEENTH    ,
			L_THIRTY_SECOND,
			L_SIXTY_FOURTH 
		};
		
		public final int secondPow;
		
		public NoteLength(int secondPow) {
			this.secondPow = secondPow;
		}
		int getFlags() {
			return Math.max(0, secondPow - L_QUATER.secondPow);
		}
		public int getPower() {
			return secondPow;
		}
		static NoteLength fromInteger(int length) {
			int min = values[0].secondPow;
			int max = values[values.length - 1].secondPow;
			if( length<min || length>max ) return new NoteLength(length);
			return values[length - min];
		}
	}
	
	public class SvgResource {
		public RectF bounds, limits;
		//public Picture picture;
		FontEncoder.Glyph glyph;
		//Transformed defaultTransformed = new Transformed();
		
		private ThreadLocal<Transformed> defaultTransformed = new ThreadLocal<Transformed>(){
	        @Override protected Transformed initialValue() {
	            return new Transformed();
	        }
	    };
		
		public SvgResource(int resource) {
			glyph = FontEncoder.createFromResouce(context.getResources(), resource);
			
			bounds  = glyph.getRect("bounds");
			limits  = glyph.getRect();
			
			if( bounds==null ) bounds = limits;
		}
		
		/*public float draw(Canvas canvas, float x0, float y0, float y1) {
			return draw(canvas, x0, y0, y1, false, false);
		}
			
		public float draw(Canvas canvas, float x0, float y0, float y1, boolean flipx, boolean flipy) {
			canvas.save();
			float scale = (y1-y0) / bounds.height();
			
			RectF rect = getRect(x0, y0, y1);
			
			canvas.translate(rect.left, rect.top);
			canvas.scale(scale * (flipx ? -1 : 1), scale * (flipy ? -1 : 1));
			
			glyph.draw(canvas, paint);
			//canvas.drawPicture(picture);
			canvas.restore();
			
			return bounds.width() * scale + x0;
		}
		
		public RectF getRect(float x0, float y0, float y1) {
			return getRect(x0,y0,y1,false,false);
		}
		
		public RectF getRect(float x0, float y0, float y1, boolean flipx, boolean flipy) {
			float scale = (y1-y0) / bounds.height();
			
			float left = x0 - (bounds.left-limits.left)*scale * (flipx ? -1 : 1), 
				   top = y0 - (bounds.top -limits.top )*scale * (flipy ? -1 : 1);
			
			return new RectF(left, top, 
					left + bounds.width() * scale * (flipx ? -1 : 1), top + bounds.height() * scale * (flipy ? -1 : 1));
		}*/
		
		RectF getRect(String id) {
			return glyph.getRect(id);
		}
		
		//transform glyph to match transformed glyphRect with canvasRect
		//postcondition: ret.transform(glyphRect) equal to canvasRect
		Transformed rectTo(RectF glyphRect,RectF canvasRect) {
			return rectTo(glyphRect, canvasRect, new Transformed());
		}
		
		Transformed rectTo(RectF glyphRect,RectF canvasRect, Transformed transformed) {
			float scaleX = canvasRect.width () / glyphRect.width ();
			float scaleY = canvasRect.height() / glyphRect.height();
			float left = canvasRect.left - (glyphRect.left-limits.left)*scaleX, 
				   top = canvasRect.top  - (glyphRect.top -limits.top )*scaleY;
			transformed.set(scaleX, scaleY, left, top);
			return transformed;
		}
		
		Transformed rectTo(RectF glyphRect,float x0, float y0, float y1) {
			return rectTo(glyphRect, x0, y0, y1, new Transformed());
		}
		
		Transformed rectTo(RectF glyphRect,float x0, float y0, float y1, Transformed transformed) {
			float scale = (y1-y0) / glyphRect.height();
			float left = x0 - (glyphRect.left-limits.left)*scale, 
				   top = y0 - (glyphRect.top -limits.top )*scale;
			transformed.set(scale, scale, left, top);
			return transformed;
		}
		
		Transformed boundsTo(float x0, float y0, float y1) {
			return rectTo(bounds, x0, y0, y1);
		}
		
		Transformed boundsTo(float x0, float y0, float y1, Transformed transformed) {
			return rectTo(bounds, x0, y0, y1, transformed);
		}
		
		public float boundsToWidth(float x0, float y0, float y1) {
			return rectTo(bounds, x0, y0, y1, defaultTransformed.get()).getBounds().width();
		}
		
		public float boundsToHeight(float x0, float y0, float y1) {
			return rectTo(bounds, x0, y0, y1, defaultTransformed.get()).getBounds().height();
		}
		
		float boundsToDraw(float x0, float y0, float y1, Paint paint, Canvas canvas, Matrix shaderMatrix) {
			return rectTo(bounds, x0, y0, y1, defaultTransformed.get()).draw(paint, canvas, shaderMatrix);
		}
		
		float boundsToDraw(float x0, float y0, float y1, Paint paint, Canvas canvas) {
			return rectTo(bounds, x0, y0, y1, defaultTransformed.get()).draw(paint, canvas);
		}
		
		public class Transformed extends DrawableOnStaff {
			private float scaleX = 1, scaleY = 1;
			private float translateX = 0, translateY = 0;
			
			public Transformed(float scaleX, float scaleY, float translateX, float translateY) {
				set(scaleX, scaleY, translateX, translateY);
			}
			public void set(float scaleX, float scaleY, float translateX, float translateY) {
				this.scaleX = scaleX;
				this.scaleY = scaleY;
				this.translateX = translateX;
				this.translateY = translateY;
			}
			
			public Transformed() {
				// TODO Auto-generated constructor stub
			}

			public SvgResource getGlyph() {
				return SvgResource.this;
			}
			
			public RectF transformRect(RectF rect) {
				return transformRect(rect,new RectF());
			}
			
			public RectF transformRect(RectF rect, RectF result) {
				result.set(
						rect.left   * scaleX + translateX, 
						rect.top    * scaleY + translateY,
						rect.right  * scaleX + translateX, 
						rect.bottom * scaleY + translateY
						);
				return result;
			}
			
			RectF getRect(String id) {
				return transformRect(getRect(id));
			}
			
			RectF getLimits() {
				return transformRect(limits);
			}
			
			RectF getLimits(RectF rect) {
				return transformRect(limits, rect);
			}
			
			RectF getBounds() {
				return transformRect(bounds);
			}
			RectF getBounds(RectF rect) {
				return transformRect(bounds, rect);
			}
			float getRectWidth(RectF rect) {
				return (rect.right - rect.left) * scaleX;
			}
			float getRectHeight(RectF rect) {
				return (rect.bottom - rect.top) * scaleY;
			}

			public float draw(Paint paint, Canvas canvas) {
				return draw(paint, canvas, null);
			}
			public float draw(Paint paint, Canvas canvas, Matrix shaderMatrix) {
				canvas.save();
				canvas.translate(translateX, translateY);
				canvas.scale(scaleX, scaleY);
				
				if( shaderMatrix!=null ) {
					shaderMatrix.preTranslate(-translateX, -translateY);
					shaderMatrix.preScale(1/scaleX, 1/scaleY);
				}
				glyph.draw(canvas, paint, shaderMatrix);
				if( shaderMatrix!=null ) {
					shaderMatrix.preScale(scaleX, scaleY);
					shaderMatrix.preTranslate(translateX, translateY);
				}
				
				canvas.restore();
				
				return bounds.right * scaleX + translateX;
			}

			public void traslate(float x, float y) {
				translateX += x;
				translateY += y;
			}

			@Override public float getWidth() {
				return getRectWidth(bounds);
			}

			@Override
			public float getAboveHeight() {
				return getRectHeight(limits) / 2;
			}

			@Override
			public float getBelowHeight() {
				return getAboveHeight();
			}

			@Override
			public void draw(PaintContext paint, Canvas canvas, Staff staff, float line, float x) {
				canvas.save();
				canvas.translate(staff.getX(x), staff.getLineY(line) - getAboveHeight());
				canvas.scale(scaleX, scaleY);
				
				glyph.draw(canvas, paint.glyphs);
				
				canvas.restore();
			}
		}
	}
	
	enum Glyphs {
		CLEF_F (R.raw.clef_f),
		CLEF_G (R.raw.clef_g),
		
		FLAG_EIGHT (R.raw.flag_eight),
		FLAG_EIGHT_DOWN (R.raw.flag_eight_down),
		FLAG_COMMON (R.raw.flag_common),
		FLAG_COMMON_DOWN (R.raw.flag_common_down),
		
		NOTE_HALF  (R.raw.note_half),
		NOTE_QUATER(R.raw.note_quarter),
		NOTE_WHOLE (R.raw.note_whole), 
		NOTE_UNSUPPORTED (R.raw.note_unsupported),
		
		ACC_DOUBLE_SHARP (R.raw.acc_double_sharp),
		ACC_DOUBLE_FLAT  (R.raw.acc_double_flat),
		ACC_FLAT         (R.raw.acc_flat),
		ACC_NATURAL      (R.raw.acc_natural),
		ACC_SHARP        (R.raw.acc_sharp),

		REST_FLAG  (R.raw.rest_flag),
		REST_QUATER(R.raw.rest_quarter),
		REST_EIGHT (R.raw.rest_eight),
		
		TIME_MARK_C(R.raw.time_mark_c),
		TIME_MARK_C_CUT(R.raw.time_mark_c_cut),
		
		FIGURE     (R.raw.figure);
		
		int mRes;
		
		Glyphs(int res) {
			mRes = res;
		}
		
		public int getResource() {
			return mRes;
		}
	}
	
	public class Font {
		private Map<Glyphs,SvgResource> map = new HashMap<Glyphs, SvgResource>();
		
		Font(Resources res) {
			for(Glyphs glyph : Glyphs.values()) {
				map.put(glyph, new SvgResource(glyph.getResource()));
			}
		}
		
		SvgResource get(Glyphs g) {
			return map.get(g);
		}
	}
			
	
	public enum StemPos { 
		StemUp {
			public boolean isUp(float line) {
				return true;
			}
		}, 
		StemNo {
			public boolean isUp(float line) {
				return false;
			}
		}, 
		StemDown {
			public boolean isUp(float line) {
				return false;
			}
		}, 
		StemAuto {
			public boolean isUp(float line) {
				return line > 2;
			}
		};
		public boolean empty() {
			return this == StemNo;
		}
		public abstract boolean isUp(float line);
		public int dir(float line) {
			return empty() ? 0 : isUp(line) ? -1 : 1; 
		}
	}

	public enum NoteHeadGlyph {
		WHOLE  (Glyphs.NOTE_WHOLE ),
		HALF   (Glyphs.NOTE_HALF  ),
		QUATER (Glyphs.NOTE_QUATER), 
		UNSUPPORTED  (Glyphs.NOTE_UNSUPPORTED);
		
		public Glyphs glyph;
		NoteHeadGlyph(Glyphs g) {
			glyph = g;
		}
	}
	
	Map<NoteHeadGlyph, NoteHead> nodeGlyphsMap;
	public Paint testTextPaint = new Paint();
	
	void initizalizeHeadMap() {
		nodeGlyphsMap = new HashMap<NoteHeadGlyph, NoteHead>();
		for(NoteHeadGlyph glyph : NoteHeadGlyph.values()) {
			nodeGlyphsMap.put(glyph, new NoteHead(font.get(glyph.glyph)));
		}
	}
	
	public NoteHead getNoteHead(NoteHeadGlyph headType) {
		return nodeGlyphsMap.get(headType);
	}
	
	public static class NoteHead {
		public SvgResource mRes;
		public RectF rect;
		
		public float stemWidth;
		public float stemOff;
		
		NoteHead(SvgResource res) {
			mRes = res;
			rect = res.boundsTo(0, 0, normalNoteHeight()).getBounds();
			stemWidth = stemWidthFactor * width();
			stemOff   = stemPosFactor * height() - height() / 2;
		}
		
		/*public Note createNote(float line) {
			return new Note(this,line);
		}*/
		
		public float putOnStaff(Paint paint, Canvas canvas, Staff staff, float x, float line) {
			return staff.drawOnStaff(paint, canvas, mRes, line - 0.5f, line + 0.5f, x);
		}
		
		public float putOnStaff(Paint paint, Canvas canvas, Staff staff, float x, float line, Matrix matrix) {
			return staff.drawOnStaff(paint, canvas, mRes, line - 0.5f, line + 0.5f, x, matrix);
		}
		
		public float width() {
			return rect.width();
		}
		
		public float height() {
			return rect.height();
		}
		
		public float normalNoteHeight() {
			return 1;
		}
	}
	
	public float commonNoteWidth() {
		return getNoteHead(NoteHeadGlyph.QUATER).width();
	}
	public float commonStemWidth() {
		return getNoteHead(NoteHeadGlyph.QUATER).stemWidth;
	}
}
