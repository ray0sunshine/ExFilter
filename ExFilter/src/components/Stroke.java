package components;

import java.util.ArrayList;

import components.DataUtil;
import components.Ops;
import components.QuadQueue;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;

public class Stroke extends Path {
	//TODO the pressure is still in the stroke to if needed
	
	//commit line every 50 segments
	//using private bitmap causes performance decreases so just increase line commit wait
	private static final int maxLine = 50;
	
	//specific case for eliminating touchpoint merges, just stop the line if a super long segment detected
	//set to be a bit shorter than 1/2 the shortest edge of tablet (in portrait its width)
	private static float maxLen = 4; //centimeters
	private boolean drawing;
	
	//the smallest a cross section of the bounding box for a valid stroke can be
	private static float boxMin = 0.05f; //centimeters
	
	private ArrayList<PointF> pts;	//used to data tracking and recording
	private QuadQueue qq;			//calculates curves on the growing line
	private Path head;				//the quad curve used before we get enought slope info
	private StringBuilder sb;		//builds up the data string in segments':x,y,msTime'
	private long birth;				//records the time when this stroke starts, so we can offset the time thats recorded
	private long viewBirth;			//tracks when the view was created so we can calc the offset with the stroke creation
	
	private Canvas cSave;			//the main bitmap canvas
	private Paint painter;			//painter for lines
	
	private int len;				//tracks length of current segment
	private int init;				//state info used for initial couple of segments
	
	//these tracks the bounding box of the entire stroke, if too small -> invalid
	private PointF topLeft;			//the smallest x and y of all points
	private PointF botRight;		//the largest x and y of all points
	
	public Boolean valid;			//tracks if the line is a false touch, determined externally
	public Boolean control;			//if the stroke is actually a control move
	public PointF pt;				//most current point
	
	//if not valid, only records the points, replay program could use same curve algo to recreate
	public Stroke(float x, float y, Canvas saveCanvas, Paint linePainter, Boolean isValid, long vBirth) {
		birth = System.currentTimeMillis();
		viewBirth = birth - vBirth;
		sb = new StringBuilder();
		
		control = false;
		valid = isValid;
		drawing = true;
		
		moveTo(x,y);
		pt = new PointF(x,y);
		pts = new ArrayList<PointF>();
		pts.add(new PointF(x,y));
		cSave = saveCanvas;
		painter = linePainter;
		
		topLeft = new PointF();
		botRight = new PointF();
		topLeft.set(x,y);
		botRight.set(x,y);
		
		if(valid){
			head = new Path();
			head.moveTo(x,y);
			len = 0;
			qq = new QuadQueue();
			qq.push(new PointF(x,y));
			init = 2;
		}
	}
	
	public void curveTo(float x, float y, float press){
		
		//stop the line if there is a huge gap between the 2 points...most likely a long range pointer merge
		if(Ops.dist(pt.x, pt.y, x, y) > maxLen){
			drawing = false;
		}
		
		if(drawing){
			if(x > botRight.x){
				botRight.x = x;
			}else if(x < topLeft.x){
				topLeft.x = x;
			}
			
			if(y > botRight.y){
				botRight.y = y;
			}else if(y < topLeft.y){
				topLeft.y = y;
			}
			
			//add the new coordinate as string to data and record time
			sb.append(":"+Float.toString(x)+","+Float.toString(y)+","+Long.toString(System.currentTimeMillis() - birth));
			
			pts.add(new PointF(x,y));
			pt.set(x,y);
			if(valid){
				qq.push(new PointF(x,y));
				switch(init){
				case 0:
					head.rewind();
					head.moveTo(qq.p2.x, qq.p2.y);
					head.quadTo(qq.aQuad.x, qq.aQuad.y, x, y);
					cubicTo(qq.aCubeP3.x, qq.aCubeP3.y, qq.aCubeP2.x, qq.aCubeP2.y, qq.p2.x, qq.p2.y);
					len++;
					if(len > maxLine){
						cSave.drawPath(this, painter);
						rewind();
						moveTo(qq.p2.x,qq.p2.y);
						len = 0;
					}
					break;
				case 1:
					init = 0;
					head.rewind();
					head.moveTo(qq.p2.x, qq.p2.y);
					head.quadTo(qq.aQuad.x, qq.aQuad.y, x, y);
					quadTo(qq.aCubeP2.x, qq.aCubeP2.y, qq.p2.x, qq.p2.y);
					break;
				case 2:
					init = 1;
					head.lineTo(x,y);
					break;
				}
			}
		}
	}
	
	public void drawStroke(Canvas cBuff){
		if(valid){
			cBuff.drawPath(this, painter);
			cBuff.drawPath(head, painter);
		}
	}
	
	public void end(){
		if(Ops.dist(topLeft, botRight) < boxMin){
			valid = false;
		}
		
		if(valid){
			cSave.drawPath(this, painter);
			cSave.drawPath(head, painter);
		}
		
		if(pts.size() > 1){
			DataUtil.writeStroke(sb.toString(), valid, control, viewBirth);
		}
	}
	
	public void control(){
		control = true;
	}
	
	public void invalidate(){
		valid = false;
	}
	
	public static void setScale(float ppc){
		maxLen *= ppc;
		boxMin *= ppc;
	}
}

