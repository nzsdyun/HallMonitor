package org.durka.hallmonitor;

import java.io.IOException;

import android.app.Activity;
import android.appwidget.AppWidgetHostView;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

/**
 * This is the activity that is displayed by default - it is displayed for the configurable delay number of milliseconds when the case is closed,
 * it is also displayed when the power button is pressed when the case is already closed
 */
public class DefaultActivity extends Activity {
	private HMAppWidgetManager hmAppWidgetManager = Functions.hmAppWidgetManager;
	
	public static boolean on_screen;
	
	//we need to kill this activity when the screen opens
	private final BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
				
				Log.d("DA.onReceive", "Screen on event received.");
				
				if (Functions.Is.cover_closed(context)) {
					Log.d("DA.onReceive", "Cover is closed, display Default Activity.");
					//easiest way to do this is actually just to invoke the close_cover action as it does what we want
					Functions.Actions.close_cover(getApplicationContext());
				} else {
					Log.d("DA.onReceive", "Cover is open, stopping Default Activity.");
					
					// when the cover opens, the fullscreen activity goes poof				
					moveTaskToBack(true);
				}
			} else if (intent.getAction().equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
				if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_RINGING)) {
					Log.d("VCS", "call from " + intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER));
					if (Functions.Is.cover_closed(context)) {
						Log.d("VCS", "but the screen is closed. screen my calls");
						
						new Handler().postDelayed(new Runnable() {
							@Override
							public void run() {
								Process process;
								try {
									process = Runtime.getRuntime().exec(new String[]{ "su","-c","input keyevent 5"});
									process.waitFor();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							    
							}
						}, 500);
						
					}
				} else {
					Log.d("VCS", "phone state changed to " + intent.getStringExtra(TelephonyManager.EXTRA_STATE));
				}
			}
		}
	};

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//pass a reference back to the Functions class so it can finish us when it wants to
		//FIXME Presumably there is a 
		Functions.defaultActivity = this;
		
		Log.d("DA.onCreate", "onCreate of DefaultView.");
		
		//Remove title bar
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);

		//Remove notification bar
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.activity_default);
		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_SCREEN_ON);
		registerReceiver(receiver, filter);

	    RelativeLayout contentView = (RelativeLayout)findViewById(R.id.default_content);
	    
	    //if we have a default app widget to use then display that, if not then display our default clock screen
	    //(which is part of the default layout so will show anyway)
	    if (hmAppWidgetManager.doesWidgetExist("default")) {
	    	
	    	//remove the TextClock from the contentview
		    contentView.removeAllViews();
	    	
	    	//get the widget
		    AppWidgetHostView hostView = hmAppWidgetManager.getAppWidgetHostViewByType("default");
		    
		    //if the widget host view already has a parent then we need to detach it
		    ViewGroup parent = (ViewGroup)hostView.getParent();
		    if ( parent != null) {
		    	Log.d("DA.onCreate", "hostView had already been added to a group, detaching it.");
		    
		    	parent.removeView(hostView);
		    }    
		    
		    //add the widget to the view
		    contentView.addView(hostView);
	    }
	}  

	@Override
	protected void onStart() {
	    super.onStart();
	    on_screen = true;
	    //start our widget listening - FIXME this might need sorting out once using multiple app widgets
	    if (hmAppWidgetManager.doesWidgetExist("default")) hmAppWidgetManager.mAppWidgetHost.startListening();
	}
	@Override
	protected void onStop() {
	    super.onStop();
	    on_screen = false;
	    //stop our widget listening - FIXME this might need sorting out once using multiple app widgets
	    if (hmAppWidgetManager.doesWidgetExist("default")) hmAppWidgetManager.mAppWidgetHost.stopListening();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		//tidy up our receiver when we are destroyed
		unregisterReceiver(receiver);
	}
}
