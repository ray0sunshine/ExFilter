package com.exfilter;

import components.DataUtil;

import components.Stroke;
import filters.FilterBezel;
import filters.FilterElasticEye;
import filters.FilterMagnetWindow;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;

/*
 * The MainView takes care of handling the tracking of tough events and the main control bar at the top
 * It also handles switch between the different filter techniques
 */
public class MainView extends View {
	//Debug mode, set for allowing several bar functions like clear and tech swap
	private static final boolean debug = false;
	private static final int techs = 3;		//define the number of techniques available so we can cycle through them
	
	//pre-defined constants for size, it's in centimeters
	private float controlBarHeight = 0.5f;
	private float filterNameX = 0.15f;
	private float filterNameY = 0.35f;
	private float barFont = 0.3f;
	
	//screen constants calculated at run
	private float ppc;			//pixels per centimeter
	private int dimH, dimW; 	//dimension of screen space available
	
	//drawing resources
	private Paint pathPainter;
	private Paint bmPainter;
	private Paint filterPainter;
	private Paint barPainter;
	private Bitmap bm;
	private Canvas saveCanvas;
	
	//touch tracker
	private SparseArray<Stroke> activeStroke;
	private int filterMode;			//the index of the currently used filter, starts at 0 for none
	
	//timing vars
	private long startTime;
	
	//filters
	private FilterBezel bezelFilter;
	//private FilterElasticEye elasticFilter;
	private FilterMagnetWindow magnetFilter;

	//TODO make sure this is only init once for the sake of the *= ops
	public MainView(Context context, AttributeSet attrs) {
		//init basics
		super(context, attrs);
		ppc = context.getResources().getDisplayMetrics().xdpi/2.54f;
		activeStroke = new SparseArray<Stroke>();
		filterMode = ExControl.getMode();
		
		//init scaled constants;
		controlBarHeight *= ppc;
		filterNameX *= ppc;
		filterNameY *= ppc;
		barFont *= ppc;
		Stroke.setScale(ppc);
		
		//init painters
		pathPainter = new Paint(Paint.ANTI_ALIAS_FLAG);
		pathPainter.setColor(Color.BLACK);
		pathPainter.setStyle(Paint.Style.STROKE);
		pathPainter.setStrokeWidth(2);
		pathPainter.setStrokeJoin(Paint.Join.ROUND);
		
		filterPainter = new Paint(Paint.ANTI_ALIAS_FLAG);
		filterPainter.setColor(Color.BLACK);
		filterPainter.setStyle(Paint.Style.FILL);
		filterPainter.setAlpha(128);
		
		bmPainter = new Paint(Paint.ANTI_ALIAS_FLAG);
		bmPainter.setColor(Color.BLACK);
		bmPainter.setStyle(Paint.Style.FILL);
		
		barPainter = new Paint(Paint.ANTI_ALIAS_FLAG);
		barPainter.setColor(Color.WHITE);
		barPainter.setStyle(Paint.Style.STROKE);
		barPainter.setTextSize(barFont);
		
		initFilters();
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent e) {
		int ptrIdx = e.getActionIndex();
		int ptrId = e.getPointerId(ptrIdx);
		int mAction = e.getActionMasked();
		
		float x = e.getX(ptrIdx);
		float y = e.getY(ptrIdx);
		
		//TODO the top control bar functions should be implemented in this switch
		//TODO done part, clear, and maybe swap technique, they are independent of the actual strokes
		//TODO if you let go on a bar button, that aint my problem
		switch(mAction){
		case MotionEvent.ACTION_DOWN:
		case MotionEvent.ACTION_POINTER_DOWN:
			//do stuff for down touch
			//TODO basically initially deciding if a line is valid, control or bad
			
			//have a switch for the filter and push in the result stroke from the filter method
			switch(filterMode){
			case 0:
				activeStroke.put(ptrId,  new Stroke(x,y,saveCanvas,pathPainter,true,startTime));
				break;
			case 1:
				activeStroke.put(ptrId,  new Stroke(x,y,saveCanvas,pathPainter,bezelFilter.newStroke(x,y),startTime));
				break;
			case 2:
				//activeStroke.put(ptrId,  new Stroke(x,y,saveCanvas,pathPainter,elasticFilter.newStroke(x,y),startTime));
				activeStroke.put(ptrId,  new Stroke(x,y,saveCanvas,pathPainter,magnetFilter.newStroke(x,y),startTime));
				break;
			}
			break;
		case MotionEvent.ACTION_MOVE:
			int size = e.getPointerCount();
			for(int i=0; i<size; i++){
				activeStroke.get(e.getPointerId(i)).curveTo(e.getX(i), e.getY(i), e.getPressure(i));
			}
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_POINTER_UP:
		case MotionEvent.ACTION_CANCEL:
			activeStroke.get(ptrId).end();
			activeStroke.remove(ptrId);
			
			if(y < controlBarHeight){
				if(x > dimW - ppc){
					//TODO done goto the next part
					//TODO mimic whats done below
					switch(filterMode){
					case 0:
						DataUtil.writeEmptyEnd();
						break;
					case 1:
						bezelFilter.commit();
						break;
					case 2:
						//elasticFilter.commit();
						magnetFilter.commit();
						break;
					}
					DataUtil.endPart();
					ExControl.nextPart();
					clearScreen();
					filterMode = ExControl.getMode();
					
					String headFilter = null, headPic = null;
					switch(filterMode){
					case 0:
						headFilter = "no-filter";
						break;
					case 1:
						headFilter = "bezel-filter";
						break;
					case 2:
						headFilter = "window-filter";
						break;
					}
					
					switch(ExControl.getPic()){
					case 0:
						headPic = "Fox";
						break;
					case 1:
						headPic = "Math";
						break;
					case 2:
						headPic = "Pattern";
						break;
					case 3:
						headPic = "Text";
						break;
					}
					
					DataUtil.writeHead(headFilter,headPic);
					
					bezelFilter.reset();
					//elasticFilter.reset();
					magnetFilter.reset();
				}else if(debug){//just for testing stuff, clear and changing modes both cleans the canvas, ends current part and starts a new part
					if(x < ppc){
						clearScreen();
						switch(filterMode){
						case 0:
							DataUtil.writeEmptyEnd();
							break;
						case 1:
							bezelFilter.commit();
							break;
						case 2:
							//elasticFilter.commit();
							magnetFilter.commit();
							break;
						}
						DataUtil.endPart();
						DataUtil.writeHead("derpingr","???");
						filterMode = (filterMode+1)%techs;	//cycle through techniques
						bezelFilter.reset();
						//elasticFilter.reset();
						magnetFilter.reset();
					}else if(x > dimW/2 && x < dimW/2+ppc){
						clearScreen();
						switch(filterMode){
						case 0:
							DataUtil.writeEmptyEnd();
							break;
						case 1:
							bezelFilter.commit();
							break;
						case 2:
							//elasticFilter.commit();
							magnetFilter.commit();
							break;
						}
						DataUtil.endPart();
						DataUtil.writeHead("derpingr","???");
						bezelFilter.reset();
						//elasticFilter.reset();
						magnetFilter.reset();
					}
				}
			}
			break;
		}
		
		invalidate();
		return true;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		canvas.drawBitmap(bm, 0, 0, bmPainter);
		
		int size = activeStroke.size();
		for(int i=0; i<size; i++){
			Stroke s = activeStroke.valueAt(i);
			s.drawStroke(canvas);
		}
		
		//draw the control bar base
		canvas.drawRect(0, 0, dimW, controlBarHeight, bmPainter);
		
		int count = ExControl.curMode*ExControl.pics+ExControl.curPic+1;
		int total = ExControl.modes*ExControl.pics;
		String s = " [" + Integer.toString(count) + "/" + Integer.toString(total) + "]";
		
		//do filtering and control drawing here, filters should have their painters
		switch(filterMode){
		case 0:
			canvas.drawText("0:No filter"+s, filterNameX, filterNameY, barPainter);
			break;
		case 1:
			bezelFilter.drawing(canvas);
			canvas.drawText("1:Bezel Filter"+s, filterNameX, filterNameY, barPainter);
			break;
		case 2:
			//elasticFilter.drawing(canvas);
			//canvas.drawText("2:Eye Filter", filterNameX, filterNameY, barPainter);
			magnetFilter.drawing(canvas);
			canvas.drawText("2:Window Filter"+s, filterNameX, filterNameY, barPainter);
			break;
		}
		
		canvas.drawText("DONE", dimW-ppc, filterNameY, barPainter);
		if(debug){
			canvas.drawText("CLEAR", dimW/2, filterNameY, barPainter);
		}
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		dimH = h;
		dimW = w;
		
		String headFilter = null, headPic = null;
		switch(filterMode){
		case 0:
			headFilter = "no-filter";
			break;
		case 1:
			headFilter = "bezel-filter";
			break;
		case 2:
			headFilter = "window-filter";
			break;
		}
		
		switch(ExControl.getPic()){
		case 0:
			headPic = "Fox";
			break;
		case 1:
			headPic = "Math";
			break;
		case 2:
			headPic = "Pattern";
			break;
		case 3:
			headPic = "Text";
			break;
		}
		
		DataUtil.writeHead(headFilter,headPic);
		
		bm =  Bitmap.createBitmap(w,h,Bitmap.Config.RGB_565);
		saveCanvas = new Canvas(bm);
		clearScreen();
		dimFilters();
	}
	
	//TODO this should be called when we start a new part of the experiment to clear all things, So it should be the one to write a new parts header
	//TODO at the end of the experiment, a filter should be responsible for writing it's own filter data to the data file
	//TODO load the next image base into the bitmap
	//TODO mainView should be built for the sake of experimenting, so it should take care of initial wait for start screen
	private void clearScreen(){
		bm.eraseColor(Color.WHITE);
		
		Bitmap b = null;
		switch(ExControl.getPic()){
		case 0:
			b = BitmapFactory.decodeResource(getResources(), R.drawable.fox);
			break;
		case 1:
			b = BitmapFactory.decodeResource(getResources(), R.drawable.calc);
			break;
		case 2:
			b = BitmapFactory.decodeResource(getResources(), R.drawable.patt);
			break;
		case 3:
			b = BitmapFactory.decodeResource(getResources(), R.drawable.text);
			break;
		}
		
		saveCanvas.drawBitmap(b,0,0,bmPainter);
		startTime =  System.currentTimeMillis();
	}
	
	//TODO initializes the filter instances by giving them screen dimensions and consts like control bar height also the painters
	private void initFilters(){
		bezelFilter = new FilterBezel(ppc, activeStroke);
		//elasticFilter = new FilterElasticEye(ppc, activeStroke);
		magnetFilter = new FilterMagnetWindow(ppc, activeStroke);
	}
	
	//TODO since the size change gets called after the constructor
	private void dimFilters(){
		bezelFilter.setDim(dimW,dimH);
		//elasticFilter.setDim(dimW,dimH);
		magnetFilter.setDim(dimW, dimH);
	}
}
