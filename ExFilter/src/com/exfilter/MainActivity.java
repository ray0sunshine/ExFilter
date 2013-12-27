package com.exfilter;

import components.DataUtil;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ExControl.setMainAct(this);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_main);
	}

	@Override
	protected void onResume() {
		super.onResume();
		DataUtil.reset();
		
		//testing
		DataUtil.masterHeader("RAILGUN");
		DisplayMetrics metrics = this.getResources().getDisplayMetrics();
		DataUtil.writeDim(metrics.widthPixels, metrics.heightPixels);
	}
	
	@Override
	protected void onPause() {
		DataUtil.dataCommit();
		super.onPause();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public void startAct(Intent intent){
		super.startActivity(intent);
	}
}
