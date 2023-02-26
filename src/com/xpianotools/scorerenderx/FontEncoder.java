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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.Log;

public class FontEncoder {
	static final String TAG = "FontEncoder";
	
	public static class Transform {
		float x = 0;
		float y = 0;
		
		public Transform clone() {
			return new Transform().translate(x,y);
		}
		public void apply(Canvas canvas) {
			canvas.translate(x, y);
		}
		public void modifyRect(RectF rect) {
			rect.left   += x;
			rect.top    += y;
			rect.right  += x;
			rect.bottom += y;
		}
		Transform translate(float x, float y) {
			this.x += x;
			this.y += y;
			return this;
		}
		public void from(Transform trans) {
			this.x = trans.x;
			this.y = trans.y;
		}
		public void apply(Matrix shaderMatrix) {
			if( shaderMatrix==null ) return;
			shaderMatrix.preTranslate(x, y);
		}
		public void applyInverce(Matrix shaderMatrix) {
			if( shaderMatrix==null ) return;
			shaderMatrix.preTranslate(-x, -y);
		}
	}
	
	public static class Glyph {
		/*private class Path {
			android.graphics.Path path;
			
			void draw(Canvas canvas, Paint paint) {
				canvas.drawPath(path, paint);
			}
		}*/
		public int resourceId = -1;
		
		public class Layer {
			private ArrayList<Path>  paths  = new ArrayList<Path>();
			private ArrayList<Layer> nested = new ArrayList<Layer>();
			private Transform trans = new Transform();
			public final String id;
			
			Layer(String id) {
				this.id = id;
			}
			
			public Transform getTransform() {
				return trans;
			}
			
			public Layer createLayer(String id) {
				Layer layer = new Layer(id);
				layer.trans.from(trans);
				nested.add(layer);
				if(!layers.containsKey(id) )
					layers.put(id, layer);
				
				return layer;
			}
			
			public void addPath(Path p) {
				if( p==null ) return; 
				paths.add(p);
			}
			
			void draw(Canvas canvas, Paint paint) {
				draw(canvas,paint,null);
			}

			public void draw(Canvas canvas, Paint paint, Matrix shaderMatrix) {
				canvas.save();
				trans.apply(canvas);
				trans.applyInverce(shaderMatrix);
				//FOR PERFOMANCE REASON NOT for(Path path : paths) {
				for(int i=0;i<paths.size();++i) {
					Path path = paths.get(i);
					Shader shader = paint.getShader();
					if( shader!=null && shaderMatrix!=null )
						shader.setLocalMatrix(shaderMatrix);
					canvas.drawPath(path, paint);
				}
				trans.apply(shaderMatrix);
				canvas.restore();
				//FOR PERFOMANCE REASON NOT for(Layer layer : nested) {
				for(int i=0;i<nested.size();++i) {
					Layer layer = nested.get(i);
					layer.draw(canvas, paint, shaderMatrix);
				}
			}
		}
		
		private Map<String, Layer> layers = new HashMap<String, Layer>();
		private Map<String, RectF> rects  = new HashMap<String, RectF>();
		
		{
			layers.put("", new Layer(""));
			rects .put("", new RectF());
		}
		
		public float getX() { return getRect().left; }
		public float getY() { return getRect().top ; }
		
		public float getWidth () { return getRect().width (); }
		public float getHeight() { return getRect().height(); }
		
		public RectF getRect(String id) {
			return rects.get(id);
		}
		
		public RectF getRect() {
			return rects.get("");
		}
		public void setRect(String id, RectF rect) {
			rects.put(id, rect);
		}
		
		public void draw(Canvas canvas, Paint paint, Matrix shaderMatrix) {
			PaintLogger.logGlyph(canvas, "", this);
			getMasterLayer().draw(canvas, paint, shaderMatrix);
		}
		
		public void draw(Canvas canvas, Paint paint) {
			PaintLogger.logGlyph(canvas, "", this);
			getMasterLayer().draw(canvas, paint);
		}
		
		public boolean drawLayer(String layerId, Canvas canvas, Paint paint) {
			Layer layer = layers.get(layerId);
			if( layer==null ) return false;
			layer.draw(canvas, paint);
			return true;
		}
		
		public boolean drawLayer(String layerId, Canvas canvas, Paint paint, Matrix shaderMatrix) {
			Layer layer = layers.get(layerId);
			if( layer==null ) return false;
			layer.draw(canvas, paint, shaderMatrix);
			return true;
		}
		
		public Layer getMasterLayer() {
			return layers.get("");
		}
	}
	
	//BELLOW CODE IS PART OF svg-android-2
	/*

	   Licensed to the Apache Software Foundation (ASF) under one or more
	   contributor license agreements.  See the NOTICE file distributed with
	   this work for additional information regarding copyright ownership.
	   The ASF licenses this file to You under the Apache License, Version 2.0
	   (the "License"); you may not use this file except in compliance with
	   the License.  You may obtain a copy of the License at

	       http://www.apache.org/licenses/LICENSE-2.0

	   Unless required by applicable law or agreed to in writing, software
	   distributed under the License is distributed on an "AS IS" BASIS,
	   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	   See the License for the specific language governing permissions and
	   limitations under the License.

	   Changes Copyright 2011 Google Inc.
	*/
	/**
     * This is where the hard-to-parse paths are handled.
     * Uppercase rules are absolute positions, lowercase are relative.
     * Types of path rules:
     * <p/>
     * <ol>
     * <li>M/m - (x y)+ - Move to (without drawing)
     * <li>Z/z - (no params) - Close path (back to starting point)
     * <li>L/l - (x y)+ - Line to
     * <li>H/h - x+ - Horizontal ine to
     * <li>V/v - y+ - Vertical line to
     * <li>C/c - (x1 y1 x2 y2 x y)+ - Cubic bezier to
     * <li>S/s - (x2 y2 x y)+ - Smooth cubic bezier to (shorthand that assumes the x2, y2 from previous C/S is the x1, y1 of this bezier)
     * <li>Q/q - (x1 y1 x y)+ - Quadratic bezier to
     * <li>T/t - (x y)+ - Smooth quadratic bezier to (assumes previous control point is "reflection" of last one w.r.t. to current point)
     * </ol>
     * <p/>
     * Numbers are separate by whitespace, comma or nothing at all (!) if they are self-delimiting, (ie. begin with a - sign)
     *
     * @param s the path string from the XML
     */
    private static Path doPath(String s) {
        int n = s.length();
        SVGParserHelper ph = new SVGParserHelper(s, 0);
        ph.skipWhitespace();
        Path p = new Path();
        p.setFillType(Path.FillType.EVEN_ODD);
        
        float lastX = 0;
        float lastY = 0;
        float lastX1 = 0;
        float lastY1 = 0;
        float contourInitialX = 0;
        float contourInitialY = 0;
        RectF r = new RectF();
        char cmd = 'x';
        while (ph.pos < n) {
                char next = s.charAt(ph.pos);
                if (!Character.isDigit(next) && !(next == '.') && !(next == '-')) {
                        cmd = next;
                        ph.advance();
                } else if (cmd == 'M') { // implied command
                        cmd = 'L';
                } else if (cmd == 'm') { // implied command
                        cmd = 'l';
                } else { // implied command
                        // Log.d(TAG, "Implied command: " + cmd);
                }
                p.computeBounds(r, true);
                // Log.d(TAG, "  " + cmd + " " + r);
                // Util.debug("* Commands remaining: '" + path + "'.");
                boolean wasCurve = false;
                switch (cmd) {
                case 'M':
                case 'm': {
                        float x = ph.nextFloat();
                        float y = ph.nextFloat();
                        if (cmd == 'm') {
                                p.rMoveTo(x, y);
                                lastX += x;
                                lastY += y;
                        } else {
                                p.moveTo(x, y);
                                lastX = x;
                                lastY = y;
                        }
                        contourInitialX = lastX;
                        contourInitialY = lastY;
                        break;
                }
                case 'Z':
                case 'z': {
                		/// p.lineTo(contourInitialX, contourInitialY);
				        p.close();
				        lastX = contourInitialX;
				        lastY = contourInitialY;
                        break;
                }
                case 'L':
                case 'l': {
                        float x = ph.nextFloat();
                        float y = ph.nextFloat();
                        if (cmd == 'l') {
                                p.rLineTo(x, y);
                                lastX += x;
                                lastY += y;
                        } else {
                                p.lineTo(x, y);
                                lastX = x;
                                lastY = y;
                        }
                        break;
                }
                case 'H':
                case 'h': {
                        float x = ph.nextFloat();
                        if (cmd == 'h') {
                                p.rLineTo(x, 0);
                                lastX += x;
                        } else {
                                p.lineTo(x, lastY);
                                lastX = x;
                        }
                        break;
                }
                case 'V':
                case 'v': {
                        float y = ph.nextFloat();
                        if (cmd == 'v') {
                                p.rLineTo(0, y);
                                lastY += y;
                        } else {
                                p.lineTo(lastX, y);
                                lastY = y;
                        }
                        break;
                }
                case 'C':
                case 'c': {
                        wasCurve = true;
                        float x1 = ph.nextFloat();
                        float y1 = ph.nextFloat();
                        float x2 = ph.nextFloat();
                        float y2 = ph.nextFloat();
                        float x = ph.nextFloat();
                        float y = ph.nextFloat();
                        if (cmd == 'c') {
                                x1 += lastX;
                                x2 += lastX;
                                x += lastX;
                                y1 += lastY;
                                y2 += lastY;
                                y += lastY;
                        }
                        p.cubicTo(x1, y1, x2, y2, x, y);
                        lastX1 = x2;
                        lastY1 = y2;
                        lastX = x;
                        lastY = y;
                        break;
                }
                case 'S':
                case 's': {
                        wasCurve = true;
                        float x2 = ph.nextFloat();
                        float y2 = ph.nextFloat();
                        float x = ph.nextFloat();
                        float y = ph.nextFloat();
                        if (cmd == 's') {
                                x2 += lastX;
                                x += lastX;
                                y2 += lastY;
                                y += lastY;
                        }
                        float x1 = 2 * lastX - lastX1;
                        float y1 = 2 * lastY - lastY1;
                        p.cubicTo(x1, y1, x2, y2, x, y);
                        lastX1 = x2;
                        lastY1 = y2;
                        lastX = x;
                        lastY = y;
                        break;
                }
                case 'A':
                case 'a': {
                        float rx = ph.nextFloat();
                        float ry = ph.nextFloat();
                        float theta = ph.nextFloat();
                        int largeArc = (int) ph.nextFloat();
                        int sweepArc = (int) ph.nextFloat();
                        float x = ph.nextFloat();
                        float y = ph.nextFloat();
                        if (cmd == 'a') {
                                x += lastX;
                                y += lastY;
                        }
                        drawArc(p, lastX, lastY, x, y, rx, ry, theta, largeArc == 1, sweepArc == 1);
                        lastX = x;
                        lastY = y;
                        break;
                }
                case 'T':
                case 't': {
                    wasCurve = true;
                    float x = ph.nextFloat();
                    float y = ph.nextFloat();
                    if (cmd == 't') {
                        x += lastX;
                        y += lastY;
                    }
                    float x1 = 2 * lastX - lastX1;
                    float y1 = 2 * lastY - lastY1;
                    p.cubicTo( lastX, lastY, x1, y1, x, y );
                    lastX = x;
                    lastY = y;
                    lastX1 = x1;
                    lastY1 = y1;
                    break;
                }
                case 'Q':
                case 'q': {
                    wasCurve = true;
                    float x1 = ph.nextFloat();
                    float y1 = ph.nextFloat();
                    float x = ph.nextFloat();
                    float y = ph.nextFloat();
                    if (cmd == 'q') {
                        x += lastX;
                        y += lastY;
                        x1 += lastX;
                        y1 += lastY;
                    }
                    p.cubicTo( lastX, lastY, x1, y1, x, y );
                    lastX1 = x1;
                    lastY1 = y1;
                    lastX = x;
                    lastY = y;
                    break;
                }
                default:
                        Log.w(TAG, "Invalid path command: " + cmd);
                        ph.advance();
                }
                if (!wasCurve) {
                        lastX1 = lastX;
                        lastY1 = lastY;
                }
                ph.skipWhitespace();
        }
        return p;
    }
    
    /**
     * Elliptical arc implementation based on the SVG specification notes
     * Adapted from the Batik library (Apache-2 license) by SAU
     */

    private static void drawArc(Path path, double x0, double y0, double x, double y, double rx,
                    double ry, double angle, boolean largeArcFlag, boolean sweepFlag) {
        double dx2 = (x0 - x) / 2.0;
        double dy2 = (y0 - y) / 2.0;
        angle = Math.toRadians(angle % 360.0);
        double cosAngle = Math.cos(angle);
        double sinAngle = Math.sin(angle);

        double x1 = (cosAngle * dx2 + sinAngle * dy2);
        double y1 = (-sinAngle * dx2 + cosAngle * dy2);
        rx = Math.abs(rx);
        ry = Math.abs(ry);

        double Prx = rx * rx;
        double Pry = ry * ry;
        double Px1 = x1 * x1;
        double Py1 = y1 * y1;

        // check that radii are large enough
        double radiiCheck = Px1 / Prx + Py1 / Pry;
        if (radiiCheck > 1) {
                rx = Math.sqrt(radiiCheck) * rx;
                ry = Math.sqrt(radiiCheck) * ry;
                Prx = rx * rx;
                Pry = ry * ry;
        }

        // Step 2 : Compute (cx1, cy1)
        double sign = (largeArcFlag == sweepFlag) ? -1 : 1;
        double sq = ((Prx * Pry) - (Prx * Py1) - (Pry * Px1))
        / ((Prx * Py1) + (Pry * Px1));
        sq = (sq < 0) ? 0 : sq;
        double coef = (sign * Math.sqrt(sq));
        double cx1 = coef * ((rx * y1) / ry);
        double cy1 = coef * -((ry * x1) / rx);

        double sx2 = (x0 + x) / 2.0;
        double sy2 = (y0 + y) / 2.0;
        double cx = sx2 + (cosAngle * cx1 - sinAngle * cy1);
        double cy = sy2 + (sinAngle * cx1 + cosAngle * cy1);

        // Step 4 : Compute the angleStart (angle1) and the angleExtent (dangle)
        double ux = (x1 - cx1) / rx;
        double uy = (y1 - cy1) / ry;
        double vx = (-x1 - cx1) / rx;
        double vy = (-y1 - cy1) / ry;
        double p, n;

        // Compute the angle start
        n = Math.sqrt((ux * ux) + (uy * uy));
        p = ux; // (1 * ux) + (0 * uy)
        sign = (uy < 0) ? -1.0 : 1.0;
        double angleStart = Math.toDegrees(sign * Math.acos(p / n));

        // Compute the angle extent
        n = Math.sqrt((ux * ux + uy * uy) * (vx * vx + vy * vy));
        p = ux * vx + uy * vy;
        sign = (ux * vy - uy * vx < 0) ? -1.0 : 1.0;
        double angleExtent = Math.toDegrees(sign * Math.acos(p / n));
        if (!sweepFlag && angleExtent > 0) {
                angleExtent -= 360f;
        } else if (sweepFlag && angleExtent < 0) {
                angleExtent += 360f;
        }
        angleExtent %= 360f;
        angleStart %= 360f;

        RectF oval = new RectF((float) (cx - rx), (float) (cy - ry), (float) (cx + rx), (float) (cy + ry));
        path.addArc(oval, (float) angleStart, (float) angleExtent);
    }
	
	private static class ParseHandler extends DefaultHandler {
		private Glyph mRes;
		private Stack<Glyph.Layer> layerStack = new Stack<Glyph.Layer>();
		//private Stack<Transform> transformStack = new Stack<Transform>();
		
		public ParseHandler(Glyph result) {
			mRes = result;
			layerStack.add(mRes.getMasterLayer());
			//transformStack.add(new Transform());
		}
		
		@Override
        public void startDocument() { }

        @Override
        public void endDocument() { }
        
        static float getFloatAttr(org.xml.sax.Attributes atts, String name, float byDefault) {
        	String v = atts.getValue(name);
        	if( v==null ) return byDefault;
        	if( v.endsWith("px") ) v = v.substring(0, v.length() - 2);
        	
        	return Float.valueOf(v);
        }
        
        private static void applyTransform(String transformSpec, Transform transform) {
        	if( transformSpec==null ) return;
        	
        	SVGParserHelper parser = new SVGParserHelper(transformSpec,0);
			parser.skipWhitespace();
			if( parser.skipString("translate") ) {
				parser.skipWhitespace();
				parser.skipString("(");parser.skipWhitespace();
				float x = parser.parseFloat();parser.skipWhitespace();
				parser.skipString(",");parser.skipWhitespace();
				float y = parser.parseFloat();
				
				transform.translate(x, y);
			}
        }
        
        @Override
        public void startElement(String namespaceURI, String localName, String qName, org.xml.sax.Attributes atts) {
        	if ( localName.equals("path") ) {
        		String d = atts.getValue("d");
        		if( d!=null ) {
            		Path p = doPath(d);
        			layerStack.peek().addPath(p);
        			
        			String id = atts.getValue("id");
        			if( id!=null ) {
        				Glyph.Layer layer = layerStack.peek().createLayer(id);
                		applyTransform(atts.getValue("transform"), layer.getTransform());
        				layer.addPath(p);
        			}
        		}
        	} else if ( localName.equals("rect") || localName.equals("svg")) {
        		float x = getFloatAttr(atts, "x", 0f), y = getFloatAttr(atts, "y", 0f),
				w = getFloatAttr(atts, "width", 0f), h = getFloatAttr(atts, "height", 0f);
        		
        		RectF rect = new RectF(x,y,x+w,y+h);
        		
        		Transform rectTransform = new Transform();
        		rectTransform.from(layerStack.peek().getTransform());
        		applyTransform(atts.getValue("transform"), rectTransform);
        		rectTransform.modifyRect(rect);
        		
        		mRes.setRect(layerStack.peek().id, rect);
        		if( atts.getValue("id")!=null ) mRes.setRect(atts.getValue("id"), rect);
        		
        	} else if ( localName.equals("g") ) {
        		//Transform transformElem = transformStack.peek().clone();
        		
        		String id = atts.getValue("id");
        		
        		if( id==null ) id = "";
        		Glyph.Layer layer = layerStack.peek().createLayer(id);
        		layerStack.push(layer);
        		applyTransform(atts.getValue("transform"), layer.getTransform());
        		
        		//transformStack.push(transformElem);
        	}
        }
        
        @Override
        public void endElement(java.lang.String uri, java.lang.String localName, java.lang.String qName) {
        	if( localName.equals("g") ) {
        		layerStack.pop();
        		//transformStack.pop();
        	}
        }
	}
	
	public static Glyph createFromResouce(Resources resources, int resource) {
		Glyph res = parse(resources.openRawResource(resource));
		res.resourceId = resource;
		
		return res;
	}
	
	public static Glyph parse(InputStream in) {
		XMLReader xr = null;
		
        Glyph result = new Glyph();
        ParseHandler handler = new ParseHandler(result);
        
		try {
			SAXParserFactory spf = SAXParserFactory.newInstance();
	        SAXParser sp = spf.newSAXParser();
	        xr = sp.getXMLReader();
	        xr.setContentHandler(handler);
	        xr.parse(new InputSource(in));
	    } catch (Exception e) {
	        Log.d(TAG, e.toString());
	        e.printStackTrace();
	    }
        
        return result;
	}

}
