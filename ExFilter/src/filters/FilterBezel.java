package filters;

import components.DataUtil;
import components.Stroke;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.SparseArray;

public class FilterBezel {
	private static final int filterID = 1;		//used to identify which filter this is
	
	private static boolean measureSet = false;
	private static float controlBound = 1.25f;	//how wide the controls are
	private static float expandBound = 0.75f;	//on a single click of the control, how big 1/2 the window should be
	private static float dragBoundSq = 0.7f;	//how large 1/2 the drag controls on the window top and bot are (squared since we don't bother sqRooting the value...saves time)
	private Paint filterPainter;
	private float ubound, lbound, hspace, uspace, lspace;
	private boolean spaceset;
	private int dimH, dimW;
	SparseArray<Stroke> activeStroke;
	private long newViewTime;		//tracks the last time this view was renewed (calls setTime at start of a part)
	private StringBuilder sb;		//tracks the y position of the boundaries':top,bot,msTime'
	
	//TODO keep track of own control data and write to data at the end of the experiment part, provide function for ending view so we can append the control data before the ending of a part
	public FilterBezel(float ppc, SparseArray<Stroke> actives) {
		filterPainter = new Paint(Paint.ANTI_ALIAS_FLAG);
		filterPainter.setColor(Color.BLACK);
		filterPainter.setStyle(Paint.Style.FILL);
		filterPainter.setAlpha(128);
		ubound = 0;
		lbound = 0;
		hspace = 0;
		
		setMeasure(ppc);
		
		activeStroke = actives;
		sb = new StringBuilder();
		sb.append(Integer.toString(filterID));
		newViewTime = System.currentTimeMillis();
		sb.append(":"+Float.toString(ubound)+","+Float.toString(lbound)+",0");
	}
	
	private void setMeasure(float ppc){
		if(!measureSet){
			measureSet = true;
			controlBound *= ppc;
			expandBound *= ppc;
			dragBoundSq *= ppc*ppc;
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
		ubound = 0;
		lbound = 0;
		hspace = 0;
		sb = new StringBuilder();
		sb.append(Integer.toString(filterID));
		newViewTime = System.currentTimeMillis();
		sb.append(":"+Float.toString(ubound)+","+Float.toString(lbound)+",0");
	}
	
	//carries out initial stroke rejections
	public boolean newStroke(float x, float y){
		if(y > lbound || y < ubound || x < controlBound){
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
		float nlbound = 0;
		float nubound = dimH;
		int fbound = 0;
		
		for(int i=0; i<activeStroke.size(); i++){
			Stroke s = activeStroke.valueAt(i);
			if(s.pt.x < controlBound){
				s.control();
				fbound++;
				if(s.pt.y > nlbound){
					nlbound = s.pt.y;
				}
				if(s.pt.y < nubound){
					nubound = s.pt.y;
				}
			}
		}
		
		//dont worry about the control bar, it gets disabled anyways		
		if(fbound > 1){
			lbound = nlbound;
			ubound = nubound;
			hspace = (lbound - ubound)/2;
			spaceset = false;
		}else if(fbound == 1){
			if(lbound == ubound){
				lbound = nlbound+expandBound;
				ubound = nubound-expandBound;
				hspace = (lbound - ubound)/2;
				spaceset = false;
			}else{
				float dl = (nlbound - lbound)*(nlbound - lbound);
				float du = (nubound - ubound)*(nubound - ubound);
				if(dl < du){
					if(dl < dragBoundSq){
						lbound = nlbound;
						hspace = (lbound - ubound)/2;
						spaceset = false;
					}else{
						if(nlbound > lbound){
							ubound = nlbound - hspace;
							lbound = nlbound + hspace;
							spaceset = false;
						}else{
							if(spaceset){
								ubound = nlbound - uspace;
								lbound = nlbound + lspace;
							}else{
								spaceset = true;
								uspace = nubound - ubound;
								lspace = lbound - nlbound;
							}
						}
					}
				}else{
					if(du < dragBoundSq){
						ubound = nubound;
						hspace = (lbound - ubound)/2;
						spaceset = false;
					}else{
						if(nubound < ubound){
							ubound = nubound - hspace;
							lbound = nubound + hspace;
							spaceset = false;
						}else{
							if(spaceset){
								ubound = nubound - uspace;
								lbound = nubound + lspace;
							}else{
								spaceset = true;
								uspace = nubound - ubound;
								lspace = lbound - nlbound;
							}
						}
					}
				}
			}
		}else{
			spaceset = false;
		}
		
		//only set the ender if the controls change
		if(fbound >= 1){
			sb.append(":"+Float.toString(ubound)+","+Float.toString(lbound)+","+Long.toString(System.currentTimeMillis() - newViewTime));
		}
		
		canvas.drawLine(controlBound, 0, controlBound, dimH, filterPainter);
		canvas.drawRect(0, 0, dimW, ubound, filterPainter);
		canvas.drawRect(0, lbound, dimW, dimH, filterPainter);
	}
}
