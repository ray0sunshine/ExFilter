package com.exfilter;

/*
 * Controls the experiment by holding the image asset references and controlling the ordering
 * Reads in a config file uses that to define the ordering of the techniques in the experiment
 * Each technique is used on the same set of trials
 */

//TODO make the experiment tests
public class ExControl {
	static int modes = 3;
	static int pics = 4;
	
	static int[] modeID = {0,1,2}; //order this for different people
	
	static int curMode = 0;
	static int curPic = 0;
	
	static MainActivity ma;
	
	public static void setMainAct(MainActivity mAct){
		ma = mAct;
	}
	
	public static int getMode(){
		return modeID[curMode];
	}
	
	public static int getPic(){
		return curPic;
	}
	
	public static void nextPart(){
		curPic++;
		if(curPic >= pics){
			curPic = 0;
			curMode++;
		}
		
		if(curMode >= modes){
			ma.onPause();
		}
	}
}
