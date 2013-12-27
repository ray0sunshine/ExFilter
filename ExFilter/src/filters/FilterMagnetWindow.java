package filters;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.SparseArray;

import components.DataUtil;
import components.Stroke;

public class FilterMagnetWindow {
	private static final int filterID = 3;		//used to identify which filter this is
	private static boolean measureSet = false;
	private static float controlRad = 0.5f;	//how wide the controls are
	private static float controlRadSq;
	
	private static float controlRadNorm = 0.5f;	//how wide the controls are
	private static float controlRadNormSq;
	
	private static float controlRadBig = 1.25f;
	private static float controlRadBigSq;
	
	private static float cOffset;
	
	
	private static float eyeDefDim = 3.0f;
	private static float eyeDimMin = 1.0f;
	private float eyeTop, eyeBot, eyeLeft, eyeRight;
	
	private Paint filterPainter;
	private int dimH, dimW;
	SparseArray<Stroke> activeStroke;
	private long newViewTime;		//tracks the last time this view was renewed (calls setTime at start of a part)
	private StringBuilder sb;		//tracks the y position of the boundaries':top,bot,msTime'
	
	public FilterMagnetWindow(float ppc, SparseArray<Stroke> actives) {
		filterPainter = new Paint(Paint.ANTI_ALIAS_FLAG);
		filterPainter.setColor(Color.BLACK);
		filterPainter.setStyle(Paint.Style.FILL);
		filterPainter.setAlpha(128);
		
		setMeasure(ppc);
		activeStroke = actives;
	}
	
	private void setMeasure(float ppc){
		if(!measureSet){
			measureSet = true;
			controlRad *= ppc;
			controlRadSq = controlRad * controlRad;
			
			controlRadNorm *= ppc;
			controlRadNormSq = controlRadNorm * controlRadNorm;
			
			controlRadBig *= ppc;
			controlRadBigSq = controlRadBig * controlRadBig;
			
			eyeDefDim *= ppc;
			eyeDimMin *= ppc;
			cOffset = (float)(controlRad*Math.sin(Math.PI/4));
		}
	}
	
	//sets the dimension after the mainView instance is created
	public void setDim(int w, int h){
		dimW = w;
		dimH = h;
		eyeTop = (dimH - eyeDefDim)/2;
		eyeBot = eyeTop + eyeDefDim;
		eyeLeft = (dimW - eyeDefDim)/2;
		eyeRight = eyeLeft + eyeDefDim;
		sb = new StringBuilder();
		sb.append(Integer.toString(filterID));
		newViewTime = System.currentTimeMillis();
		sb.append(":"+Float.toString(eyeTop)+","+Float.toString(eyeBot)+","+Float.toString(eyeLeft)+","+Float.toString(eyeRight)+",0");
	}
	
	public void commit(){
		DataUtil.writeEnd(sb.toString());
	}
	
	public void reset(){
		eyeTop = (dimH - eyeDefDim)/2;
		eyeBot = eyeTop + eyeDefDim;
		eyeLeft = (dimW - eyeDefDim)/2;
		eyeRight = eyeLeft + eyeDefDim;
		
		sb = new StringBuilder();
		sb.append(Integer.toString(filterID));
		newViewTime = System.currentTimeMillis();
		sb.append(":"+Float.toString(eyeTop)+","+Float.toString(eyeBot)+","+Float.toString(eyeLeft)+","+Float.toString(eyeRight)+",0");
	}
	
	//carries out initial stroke rejections
	public boolean newStroke(float x, float y){
		if(x < eyeLeft || x > eyeRight || y < eyeTop || y > eyeBot){
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
	
	//Carries drawing and analysis
	public void drawing(Canvas canvas){
		boolean tr = false;
		boolean bl = false;
		PointF ntr = new PointF();
		PointF nbl = new PointF();
		for(int i=0; i<activeStroke.size(); i++){
			Stroke s = activeStroke.valueAt(i);
			if(nearControlSQ(eyeRight+cOffset, eyeTop-cOffset, s.pt.x, s.pt.y)){
				ntr.set(s.pt.x-cOffset,s.pt.y+cOffset);
				tr = true;
			}else if(nearControlSQ(eyeLeft-cOffset, eyeBot+cOffset, s.pt.x, s.pt.y)){
				nbl.set(s.pt.x+cOffset,s.pt.y-cOffset);
				bl = true;
			}
		}
		
		if(tr && bl){
			if(nbl.x + eyeDimMin < ntr.x && ntr.y + eyeDimMin < nbl.y){
				eyeLeft = nbl.x;
				eyeTop = ntr.y;
				eyeRight = ntr.x;
				eyeBot = nbl.y;
				controlRadSq = controlRadBigSq;
				controlRad = controlRadBig;
			}else{
				controlRadSq = controlRadNormSq;
				controlRad = controlRadNorm;
			}
		}else if(bl){
			eyeRight += nbl.x-eyeLeft;
			eyeTop += nbl.y-eyeBot;
			eyeLeft = nbl.x;
			eyeBot = nbl.y;
			controlRadSq = controlRadBigSq;
			controlRad = controlRadBig;
		}else if(tr){
			eyeLeft += ntr.x-eyeRight;
			eyeBot += ntr.y-eyeTop;
			eyeRight = ntr.x;
			eyeTop = ntr.y;
			controlRadSq = controlRadBigSq;
			controlRad = controlRadBig;
		}else{
			controlRadSq = controlRadNormSq;
			controlRad = controlRadNorm;
		}
		
		if(tr || bl){
			sb.append(":"+Float.toString(eyeTop)+","+Float.toString(eyeBot)+","+Float.toString(eyeLeft)+","+Float.toString(eyeRight)+","+Long.toString(System.currentTimeMillis() - newViewTime));
		}
		
		//do make sure that the bl corner can never be tl of tl corner
		
		canvas.drawRect(0,0,dimW,eyeTop,filterPainter);
		canvas.drawRect(0,eyeBot,dimW,dimH,filterPainter);
		canvas.drawRect(0,eyeTop,eyeLeft,eyeBot,filterPainter);
		canvas.drawRect(eyeRight,eyeTop,dimW,eyeBot,filterPainter);
		canvas.drawCircle(eyeLeft-cOffset, eyeBot+cOffset, controlRad, filterPainter);
		canvas.drawCircle(eyeRight+cOffset, eyeTop-cOffset, controlRad, filterPainter);
	}
	
	private boolean nearControlSQ(float x1, float y1, float x2, float y2){
		float dx = x1-x2;
		float dy = y1-y2;
		return dx*dx+dy*dy < controlRadSq;
	}
}
