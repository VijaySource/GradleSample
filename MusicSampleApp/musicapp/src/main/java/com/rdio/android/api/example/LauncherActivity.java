package com.rdio.android.api.example;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;

public class LauncherActivity extends Activity{
	
	private ViewGroup hiddenPanel;
	private ViewGroup mainScreen;
	private boolean isPanelShown;
	private ViewGroup root;

	int screenHeight = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.launcher);
		
		Button rdioButton = (Button)findViewById(R.id.launch_rdio);
		rdioButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(LauncherActivity.this, RdioActivity.class);
				startActivity(intent);
				
			}
		});
		
		Button musicButton = (Button)findViewById(R.id.launch_native_music);
		musicButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(LauncherActivity.this, MusicActivty.class);
				startActivity(intent);
				
			}
		});
		
		mainScreen = (ViewGroup)findViewById(R.id.main_screen);
	    ViewTreeObserver vto = mainScreen.getViewTreeObserver(); 
	    vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() { 
	        @Override 
	        public void onGlobalLayout() { 
	            screenHeight = mainScreen.getHeight();
	            mainScreen.getViewTreeObserver().removeGlobalOnLayoutListener(this); 
	        } 
	    }); 

//	    root = (ViewGroup)findViewById(R.id.root);

	    hiddenPanel = (ViewGroup)getLayoutInflater().inflate(R.layout.hidden_pannel, mainScreen, false);
	    hiddenPanel.setVisibility(View.INVISIBLE);

	    mainScreen.addView(hiddenPanel);

	    isPanelShown = false;
	}
	
	public void slideUpDown(final View view) {
	    if(!isPanelShown) {
	        // Show the panel
	        mainScreen.layout(mainScreen.getLeft(),
	                          mainScreen.getTop() - (screenHeight * 25/100), 
	                          mainScreen.getRight(),
	                          mainScreen.getBottom() - (screenHeight * 25/100));



	        hiddenPanel.layout(mainScreen.getLeft(), mainScreen.getBottom(), mainScreen.getRight(), screenHeight);
	        hiddenPanel.setVisibility(View.VISIBLE);

	        Animation bottomUp = AnimationUtils.loadAnimation(this,
	                R.anim.bottum_up);

	        hiddenPanel.startAnimation(bottomUp);

	        isPanelShown = true;
	    }
	    else {
	        isPanelShown = false;

	        // Hide the Panel
	        Animation bottomDown = AnimationUtils.loadAnimation(this,
	                R.anim.bottum_down);
	        bottomDown.setAnimationListener(new AnimationListener() {

	            @Override
	            public void onAnimationStart(Animation arg0) {
	                // TODO Auto-generated method stub

	            }

	            @Override
	            public void onAnimationRepeat(Animation arg0) {
	                // TODO Auto-generated method stub

	            }

	            @Override
	            public void onAnimationEnd(Animation arg0) {
	                isPanelShown = false;

	                mainScreen.layout(mainScreen.getLeft(),
	                          mainScreen.getTop() + (screenHeight * 25/100), 
	                          mainScreen.getRight(),
	                          mainScreen.getBottom() + (screenHeight * 25/100));

	                hiddenPanel.layout(mainScreen.getLeft(), mainScreen.getBottom(), mainScreen.getRight(), screenHeight);
	            }
	        });
	        hiddenPanel.startAnimation(bottomDown);
	    }
	}

}
