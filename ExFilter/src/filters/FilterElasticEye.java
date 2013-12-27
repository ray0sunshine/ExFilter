package filters;

import java.util.ArrayList;

import components.DataUtil;
import components.Ops;
import components.Stroke;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.SparseArray;

public class FilterElasticEye {
	private static final int filterID = 2;
	private static final int tapDelay = 200;
	private static final float div = 4;
	private static final long coolDown = 1000;
	
	private static boolean measureSet = false;
	private static float eyeRad = 2.5f;		//radius of the circular window
	private static float eyeRadSq = 2.5f;	//radius squared for efficient distance checking
	
	private PointF eyeHeart;		//the center of the drawing eye
	private PointF eyeBase;			//the heart returns here is not drawing
	private Boolean eyeOpen;		//the status of whether we have a eye to draw in
	private long eyeCoolDown;
	
	private Paint filterPainter;
	
	private int dimH, dimW;
	SparseArray<Stroke> activeStroke;
	ArrayList<Tap> taps;
	private long newViewTime;		//tracks the last time this view was renewed (calls setTime at start of a part)
	private StringBuilder sb;		//tracks the position eye':x,y,msTime'
	
	public FilterElasticEye(float ppc, SparseArray<Stroke> actives) {
		filterPainter = new Paint(Paint.ANTI_ALIAS_FLAG);
		filterPainter.setColor(Color.RED);
		filterPainter.setStyle(Paint.Style.STROKE);
		filterPainter.setStrokeWidth(5);
		filterPainter.setAlpha(128);
		
		eyeOpen = false;
		eyeHeart = new PointF();
		eyeBase = new PointF();
		eyeCoolDown = 0;
		
		setMeasure(ppc);
		
		activeStroke = actives;
		
		sb = new StringBuilder();
		sb.append(Integer.toString(filterID));
		newViewTime = System.currentTimeMillis();
		
		taps = new ArrayList<Tap>();
	}
	
	private void setMeasure(float ppc){
		if(!measureSet){
			measureSet = true;
			eyeRad *= ppc;
			eyeRadSq = eyeRadSq*eyeRadSq*ppc*ppc;
			Tap.tapDistSq = Tap.tapDistSq*Tap.tapDistSq*ppc*ppc;
		}
	}
	
	//sets the dimension after the mainView instance is created
	public void setDim(int w, int h){
		dimW = w;
		dimH = h;
	}
	
	public void commit(){
		DataUtil.writeEnd(sb.toString());
	}
	
	public void reset(){
		eyeOpen = false;
		sb = new StringBuilder();
		sb.append(Integer.toString(filterID));
		newViewTime = System.currentTimeMillis();
	}
	
	//carries out initial stroke rejections
	//constantly checking for double clicks to trigger the eye
	public boolean newStroke(float x, float y){
		//locking tap events for all events to the right and lower corner when eyeOpen, only unlock is only 1 active touch
		
		//removes expired touches
		int old = 0;
		long curTime = System.currentTimeMillis();
		for(int i=0; i<taps.size(); i++){
			if(taps.get(i).time+tapDelay > curTime){
				old = i;
				i = taps.size();
			}
		}
		
		//clear the front segment of the taps
		if(old > 0){
			taps.subList(0,old).clear();
		}
		
		//first check new against taps
		ArrayList<PointF> dt = new ArrayList<PointF>();
		for(Tap t : taps){
			if(t.inRange(x,y)){
				dt.add(new PointF(x,y));
			}
		}
		
		//add new to taps
		taps.add(new Tap(x,y));
		
		//take the left most of all double taps detected
		//set a keep alive time for the current eye
		//if no active strokes, reset the time
		PointF dd = null;
		for(PointF d : dt){
			if(dd == null){
				dd = d;
			}else if(d.x < dd.x){
				dd = d;
			}
		}
		
		//set eye
		if(dd != null){
			if(dd.x < eyeHeart.x || dd.y < eyeHeart.y || (activeStroke.size() == 0 && curTime > eyeCoolDown) || !eyeOpen){
				eyeHeart.set(dd);
				eyeBase.set(dd);
				eyeOpen = true;
			}
		}
		
		//carry out the line rejection for strokes starting outside of the eye
		if(eyeOpen){
			//pretty much the same thing except our bound is a radius
			if(Ops.distSq(x, y, eyeHeart.x, eyeHeart.y) > eyeRadSq){
				return false;
			}
			
			boolean valid = true;
			for(int i=0; i<activeStroke.size(); i++){
				Stroke s = activeStroke.valueAt(i);
				if(s.valid){
					if(s.pt.x < x){
						valid = false;
					}else{
						s.invalidate();
					}
				}
			}
			return valid;
		}
		
		return false;
	}
	
	public void drawing(Canvas canvas){
		//move heart towards only active stroke, if none, do nothing or move towards an old pos, which needs to be save at double click detection
		boolean novalid = true;
		for(int i=0; i<activeStroke.size(); i++){
			Stroke s = activeStroke.valueAt(i);
			if(s.valid){
				novalid = false;
				i = activeStroke.size();
				eyeHeart.x += (s.pt.x - eyeHeart.x)/div;
				eyeHeart.y += (s.pt.y - eyeHeart.y)/div;
			}
		}
		
		if(novalid){
			eyeHeart.set(eyeBase);
		}
		
		if(eyeOpen){
			canvas.drawCircle(eyeHeart.x, eyeHeart.y, eyeRad, filterPainter);
		}
		
		eyeCoolDown = System.currentTimeMillis() + coolDown;
	}
	
	private static class Tap{
		public static float tapDistSq = 0.2f;
		public float x,y;
		public long time;
		public Tap(float xPos, float yPos){
			x = xPos;
			y = yPos;
			time = System.currentTimeMillis();
		}
		
		public boolean inRange(float x2, float y2){
			//return (x-x2)*(x-x2)+(y-y2)*(y-y2) <= tapDistSq;
			return Ops.distSq(x, y, x2, y2) <= tapDistSq;
		}
	}
}
