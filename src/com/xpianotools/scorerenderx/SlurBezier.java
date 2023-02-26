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

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

public class SlurBezier {
	public void drawSlur(Canvas canvas, Paint paint) {
    	/*Path p = new Path();

    	p.moveTo(x[0], y1);
    	p.cubicTo(x[1],centerY + maxWidth / 2,x[2],centerY + maxWidth / 2,x[3],centerY + maxWidth / 2);
    	p.cubicTo(x[4],centerY + maxWidth / 2,x[5],centerY + maxWidth / 2,x[6],y2);
    	p.cubicTo(x[5],centerY - maxWidth / 2,x[4],centerY - maxWidth / 2,x[3],centerY - maxWidth / 2);
    	p.cubicTo(x[2],centerY - maxWidth / 2,x[1],centerY - maxWidth / 2,x[0],y1);
    	
    	paint.setStyle(Paint.Style.FILL_AND_STROKE);
    	paint.setStrokeWidth(maxWidth/1.5f);
    	
    	canvas.drawPath(p, paint);*/
		drawLeft(canvas,paint);
		drawRight(canvas,paint);
    }
	
	public void drawLeft(Canvas canvas, Paint paint) {
    	Path p = new Path();

    	p.moveTo(x[0], y1);
    	p.cubicTo(x[1],centerY + maxWidth / 2,x[2],centerY + maxWidth / 2,x[3],centerY + maxWidth / 2);
    	//p.cubicTo(x[4],centerY + maxWidth / 2,x[5],centerY + maxWidth / 2,x[6],y2);
    	//p.cubicTo(x[5],centerY - maxWidth / 2,x[4],centerY - maxWidth / 2,x[3],centerY - maxWidth / 2);
    	p.lineTo(x[3],centerY - maxWidth / 2);
    	p.cubicTo(x[2],centerY - maxWidth / 2,x[1],centerY - maxWidth / 2,x[0],y1);
    	
    	paint.setStyle(Paint.Style.FILL_AND_STROKE);
    	paint.setStrokeWidth(maxWidth/1.5f);
    	
    	canvas.drawPath(p, paint);
    }
	
	public void drawRight(Canvas canvas, Paint paint) {
    	Path p = new Path();

    	//p.cubicTo(x[1],centerY + maxWidth / 2,x[2],centerY + maxWidth / 2,x[3],centerY + maxWidth / 2);
    	p.moveTo(x[3],centerY + maxWidth / 2);
    	p.cubicTo(x[4],centerY + maxWidth / 2,x[5],centerY + maxWidth / 2,x[6],y2);
    	p.cubicTo(x[5],centerY - maxWidth / 2,x[4],centerY - maxWidth / 2,x[3],centerY - maxWidth / 2);
    	//p.cubicTo(x[2],centerY - maxWidth / 2,x[1],centerY - maxWidth / 2,x[0],y1);
    	
    	paint.setStyle(Paint.Style.FILL_AND_STROKE);
    	paint.setStrokeWidth(maxWidth/1.5f);
    	
    	canvas.drawPath(p, paint);
    }
	
	private float x1;
	private float y1;
	
	private float x2;
	private float y2;
	
	private float yRaise;
	private float maxWidth = .15f;
	
	private float centerY; 
	private float x[];
	
	public SlurBezier(float x1, float y1, float x2, float y2, float yRaise) {
		this.x1 = x1;
		this.x2 = x2;
		this.y1 = y1;
		this.y2 = y2;
		
		this.yRaise = yRaise;
		
		calculatePoints();
	}
	
	public float approxAt(float x) {
		if( x<this.x[0] ) return y1;
		if( x<this.x[3] ) return      y1 + (centerY - y1) * (x - this.x[0]) / (this.x[3] - this.x[0]);
		if( x<this.x[6] ) return centerY + (y2 - centerY) * (x - this.x[3]) / (this.x[6] - this.x[3]);
		return y2;
	}
	
	private void calculatePoints() {
    	centerY = yRaise + (yRaise > 0 ? Math.max(y1, y2) : Math.min(y1, y2));
    	
    	float xalter = Math.abs(yRaise) * (x2 - x1) / (Math.abs(2*yRaise) + Math.abs(y2 - y1));
    	float centerX;
    	if( (yRaise>0) == (y2 - y1>0) )
    		centerX = x2 - xalter;
    	else
    		centerX = x1 + xalter;
    	x = new float[]{x1,(2*x1 + 2*centerX)/4,(x1+4*centerX)/5,centerX,(x2+4*centerX)/5,(2*x2 + 2*centerX)/4,x2};
	}

	public void translate(float x, float y) {
		x1 += x;
		x2 += x;
		y1 += y;
		y2 += y;
		
		calculatePoints();
	}
}
