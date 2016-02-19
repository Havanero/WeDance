package com.music;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.*;
import android.widget.AdapterView.OnItemSelectedListener;
import com.music.visual.VisualizerView;
import com.music.visual.render.BarGraphRenderer;
import com.music.visual.render.CircleBarRenderer;
import com.music.visual.render.CircleRenderer;
import com.music.visual.render.LineRenderer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class WeDance extends Activity implements OnClickListener, OnTouchListener, OnCompletionListener{
	
	private ImageButton buttonPlayPause;
	private SeekBar seekBarProgress;
	
    ProgressDialog dialog;
    private String type ="null";

	Spinner mSprPlaceType;
	String[] mPlaceType=null;
	String[] mPlaceTypeName=null;
	URL globalURL;
	private ReceiveUpdates receivedFromService;
	private int currentSongIndex = 0;
	MusicList listAll= MusicList.getInstance();
	RelativeLayout bgColor;
	private int selectPosition=0;
	static final String STATE_SELECTED = "linkPosition";
	TextView myText;
	    
	private MediaPlayer mediaPlayer;  
	
	private int mediaFileLengthInMilliseconds; // this value contains the song duration in milliseconds. Look at getDuration() method in MediaPlayer class
	
	private final Handler handler = new Handler();
	
	private ArrayList<HashMap<String, String>> songsList = new ArrayList<HashMap<String, String>>();


	private VisualizerView mVisualizerView;

	


	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (savedInstanceState != null) {
            // Restore value of members from saved state
            selectPosition = savedInstanceState.getInt(STATE_SELECTED);
            Log.e(getClass().getName(),"Restoring previous position==" + selectPosition);
        } 

        setContentView(R.layout.main);
        dialog = new ProgressDialog(this);
        initView();
    }
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current game state
        savedInstanceState.putInt(STATE_SELECTED, selectPosition); 
        Log.d(getClass().getName(),"Saving intance State" + selectPosition);
        
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }
    @Override
    public void onResume(){
    	super.onResume();
    	if (receivedFromService==null)receivedFromService=new ReceiveUpdates();
    	IntentFilter intentFilter = new IntentFilter(ServiceMusic.LOADING);
        registerReceiver(receivedFromService,intentFilter);
        IntentFilter intentFilter1 = new IntentFilter(ServiceMusic.PLAYING);
        registerReceiver(receivedFromService,intentFilter1);
        IntentFilter intentStopped = new IntentFilter(ServiceMusic.STOPPED);
        registerReceiver(receivedFromService,intentStopped);
        Log.d(getClass().getName(),"Spinnner Selection is now " + selectPosition);
        Log.d(getClass().getName(), "Linking Player onResume ");
	    ServiceMusic.getInstance();

		if (!ServiceMusic.isMediaPlaying()) {
			Log.d(getClass().getName(), "onResume Player not longer playing");
			CreateExplicitFromImplicitIntent.createExplicitFromImplicitIntent(getApplicationContext(),
					new Intent(ServiceMusic.ACTION_STOP));
			CreateExplicitFromImplicitIntent.createExplicitFromImplicitIntent(getApplication(),
					new Intent(ServiceMusic.ACTION_PLAY));
			if (isFinishing() && mediaPlayer != null) {
				mediaPlayer.release();
				mediaPlayer = null;
			}
		}
		try
		{
			ServiceMusic.getInstance();
			mediaPlayer= ServiceMusic.mPlayer;
        	if (mediaPlayer!=null){
        			Log.d(getClass().getName(),"OK to Release - NOT a null player");
				}
			else{
        			Log.d(getClass().getName(),"Not Releasing");
				}
		}
		catch(NullPointerException s)
		{
			Log.e(getClass().getName(),"Error Linking on Resume " +s.getMessage());
		}

    }
    
    @Override
    public void onPause(){
    	super.onPause();
    	 if (receivedFromService!=null)unregisterReceiver(receivedFromService);
    	  Log.d(getClass().getName(),"Spinnner Selection is now " + selectPosition);

		mediaPlayer= ServiceMusic.mPlayer;
    	  if (mediaPlayer!=null)
		  {
    		  Log.e(getClass().getName(),"Media not null");
    	  }
    	  else{
    		  Log.e("Music","Media  null");
    	  }
    	 if (ServiceMusic.isMediaPlaying()){
			 Log.d("Dance","onPause Player isMediaPlaying");

		 }
		 else
		 {
			 Log.d("Dance","onPause Player NOT isMediaPlaying");
			 mediaPlayer= ServiceMusic.mPlayer;

			 if (mediaPlayer!=null)
			 {
              	CreateExplicitFromImplicitIntent.createExplicitFromImplicitIntent(getApplicationContext(),new Intent(ServiceMusic.ACTION_STOP));
			 }

         }
    }
    
    @Override
    public void onBackPressed(){
    	super.onBackPressed();
    	 if (ServiceMusic.isMediaPlaying()){
          	 Log.d("Dance","Back Button Pressed Disconnecting Player");
			 Intent i=CreateExplicitFromImplicitIntent.createExplicitFromImplicitIntent(getApplicationContext(),new Intent(ServiceMusic.ACTION_STOP));
             startService(i);

         }
    	
    }
    /** This method initialise all the views in project*/
    private void initView() {
            
    	bgColor = (RelativeLayout)findViewById(R.id.bgID);
		buttonPlayPause = (ImageButton)findViewById(R.id.ButtonTestPlayPause);
		mPlaceType = getResources().getStringArray(R.array.stationAddress);
	    mPlaceTypeName = getResources().getStringArray(R.array.stationAddress_name);
		buttonPlayPause.setOnClickListener(this);
		seekBarProgress = (SeekBar)findViewById(R.id.SeekBarTestPlay);	
		seekBarProgress.setMax(99); // It means 100% .0-99
		seekBarProgress.setOnTouchListener(this);

		myText=(TextView)findViewById(R.id.displayText);
		final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, mPlaceTypeName);

        mSprPlaceType = (Spinner) findViewById(R.id.spr_place_type);

        mSprPlaceType.setAdapter(adapter);
        mediaPlayer=ServiceMusic.mPlayer;

		mVisualizerView = (VisualizerView) findViewById(R.id.visualizerView);
		mSprPlaceType.setOnItemSelectedListener(new OnItemSelectedListener() {

	    	public void onItemSelected(AdapterView<?> parent, View view, int pos,
	    			long id) {
	        	Object val=parent.getItemAtPosition(pos);
	            type = mPlaceType[pos];
	        	if (pos==0){
	        		  return;
	        		
	        	}
				if (selectPosition != pos) {

                URL useURL = null;
                try {
                    useURL = new URL(type);
                } catch (MalformedURLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                globalURL=useURL;
                Log.d(getClass().getName(),"Selected=="+val.toString() + "......URL==" + type);
                CreateExplicitFromImplicitIntent.createExplicitFromImplicitIntent(getApplicationContext(),new Intent(ServiceMusic.ACTION_PAUSE));
                CreateExplicitFromImplicitIntent.createExplicitFromImplicitIntent(getApplicationContext(),new Intent(ServiceMusic.ACTION_URL));
					Uri uri = Uri.parse(type);
					Intent i=new Intent(ServiceMusic.ACTION_URL,uri,getApplicationContext(),ServiceMusic.class);
					i.putExtra("StationName", val.toString());
					i.setData(uri);
                stopService(i);
                startService(i);
                selectPosition=pos;
            }

			}		
	    	@Override
	    	public void onNothingSelected(AdapterView<?> arg0) {
	    		// TODO Auto-generated method stub

	    	}});

	}

    public void runList(View view){
    	Log.i(WeDance.class.getName(),"I've been clicked");
    	Intent list = new Intent(getApplicationContext(), TrackListActivity.class);
    	songsList=listAll.getMusic();
    	System.out.println(songsList.size());
    	startActivityForResult(list, 100);	
    	
    	
    	
    }

 
	/** Method which updates the SeekBar primary progress by current song playing position*/
    private void primarySeekBarProgressUpdater() {
    	mediaPlayer =ServiceMusic.getInstance().mPlayer;
    	seekBarProgress.setProgress((int)(((float)mediaPlayer.getCurrentPosition()/mediaFileLengthInMilliseconds)*100)); // This math construction give a percentage of "was playing"/"song length"
		if (mediaPlayer.isPlaying()) {
			Runnable notification = new Runnable() {
		        public void run() {
		        	primarySeekBarProgressUpdater();
				}
		    };
		    handler.postDelayed(notification,1000);
    	}
    }

	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.ButtonTestPlayPause){
			 /** ImageButton onClick event handler. Method which start/pause mediaplayer playing */

			ServiceMusic.getInstance();
			mediaPlayer = ServiceMusic.mPlayer;

		try{
				if (!mediaPlayer.isPlaying()){
					//mediaPlayer.stop();
					Log.e("Music","on PLay mode");
					mediaPlayer.start();
					//startService(new Intent(ServiceMusic.ACTION_PLAY));
					buttonPlayPause.setImageResource(R.drawable.button_pause);

				}else{
					mediaPlayer.pause();
					//mediaPlayer.reset();
					System.out.println("Releasing Player");
					buttonPlayPause.setImageResource(R.drawable.button_play);
				}
				startService(new Intent(ServiceMusic.ACTION_PAUSE));
				buttonPlayPause.setImageResource(R.drawable.button_play);
			}
		catch(java.lang.NullPointerException s){
				Toast.makeText(this,"Not Playing!", Toast.LENGTH_SHORT).show();
            }
		}
	}

	public boolean onTouch(View v, MotionEvent event) {
		if(v.getId() == R.id.SeekBarTestPlay){
			/** Seekbar onTouch event handler. Method which seeks MediaPlayer to seekBar primary progress position*/
			if(mediaPlayer.isPlaying()){
		    	SeekBar sb = (SeekBar)v;
				int playPositionInMillisecconds = (mediaFileLengthInMilliseconds / 100) * sb.getProgress();
				Log.d("Music","Seeking Audio" + playPositionInMillisecconds);
				mediaPlayer.seekTo(playPositionInMillisecconds);
			}
		}
		return false;
	}
	public void onCompletion(MediaPlayer mp) {
		 /** MediaPlayer onCompletion event handler. Method which calls then song playing is complete*/
		this.mediaPlayer=mp;
        Log.d(this.getClass().getName(),"On COmpletion detected ");
		//buttonPlayPause.setImageResource(R.drawable.button_play);
	}


	private class ReceiveUpdates extends BroadcastReceiver{
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			
				String msg=intent.getAction();
				Log.i(this.getClass().getName(),"BroadCasting is " +msg);
				String s = intent.getStringExtra(ServiceMusic.PLAYING);
				String stop = intent.getStringExtra(ServiceMusic.STOPPED);
				
				Log.d(this.getClass().getName(),"Recieving EXTRA BROADCAST  ==" + s + "or" + stop);
		
			if (intent.getAction().equals(ServiceMusic.LOADING)){
				Log.i(this.getClass().getName(),"Hello from service" + ServiceMusic.LOADING);
				myText.setText("Loading...");
				//bgColor.setBackgroundResource(R.drawable.newback);
				bgColor.setBackgroundColor(Color.BLACK);
				dialog = new ProgressDialog(context);
				dialog.setTitle("Cubano FM");
				dialog.setMessage("Loading ...");
				dialog.setIndeterminate(true);
				dialog.setCancelable(false);
				dialog.show();

				ServiceMusic.getInstance();
				mediaPlayer = ServiceMusic.mPlayer;
				try{
					Log.i(this.getClass().getName(),"AUDIO Id==" + mediaPlayer.getAudioSessionId());
					mVisualizerView.link(mediaPlayer);
					addLineRenderer();
					addCircleBarRenderer();
					addBarGraphRenderers();
					}
				catch(Exception r){
					Log.e("WeDance","Error with Player");
					dialog.dismiss();
					}
				
				
			}
			if (intent.getAction().equals(ServiceMusic.PLAYING)){
				Log.i(this.getClass().getName(),"starting playing..." + ServiceMusic.PLAYING);
				//bgColor.setBackgroundColor(Color.BLUE);
				bgColor.setBackgroundResource(R.drawable.au);
				buttonPlayPause.setImageResource(R.drawable.button_pause);
			    myText.setText("");
                Log.i(this.getClass().getName(),s);
					if(s!= null){
						myText.setHighlightColor(Color.BLUE);
						myText.setBackgroundColor(Color.BLUE);
						myText.setText(s);
						}
				dialog.dismiss();
			}
			if (intent.getAction().equals(ServiceMusic.STOPPED)){
				myText.setText("STOPPED!!!");
				//mediaPlayer.stop();
				if (stop!=null){
					myText.setText("Error With Connection-Check station link!...");
				}
			    mVisualizerView.clearRenderers();
				bgColor.setBackgroundColor(Color.BLACK);
				dialog.dismiss();
			}
			
		
		
		}
	}
	/**
	 * Receiving song index from playlist view
	 * and play the song
	 * */
	@Override
    protected void onActivityResult(int requestCode,
                                     int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.i("WeDance", "Result Code is " + resultCode);
        if(resultCode == 100){
         	 currentSongIndex = data.getExtras().getInt("songIndex");
         	 // play selected song

         	 Log.i("We Dance","Receiving Song ID from TrackList Activity" +currentSongIndex);
             playSong(currentSongIndex);
        }

    }


	/**
	 * Function to play a song
	 * @param songIndex - index of song
	 * */
	public void  playSong(int songIndex){
		// Play song
		try {
			mediaPlayer.reset();
			//mediaPlayer.setDataSource(songsList.get(songIndex).get("songPath"));
			
			
			mediaPlayer.setDataSource(songsList.get(songIndex).get("songPath"));
			
			mediaPlayer.prepare();
			mediaPlayer.start();
			
			String songTitle = songsList.get(songIndex).get("songTitle");
			myText.setText(songTitle);
			

			
			// Displaying Song title
		/*	String songTitle = songsList.get(songIndex).get("songTitle");
        	songTitleLabel.setText(songTitle);
			
        	// Changing Button Image to pause image
			btnPlay.setImageResource(R.drawable.btn_pause);
			
			// set Progress bar values
			songProgressBar.setProgress(0);
			songProgressBar.setMax(100);
			
			// Updating progress bar
			updateProgressBar();		*/	
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	 // Methods for adding renderers to visualizer
	  private void addBarGraphRenderers()
	  {
	    Paint paint = new Paint();
	    paint.setStrokeWidth(50f);
	    paint.setAntiAlias(true);
	    paint.setColor(Color.argb(200, 56, 138, 252));
	    BarGraphRenderer barGraphRendererBottom = new BarGraphRenderer(16, paint, false);
	    mVisualizerView.addRenderer(barGraphRendererBottom);

	    Paint paint2 = new Paint();
	    paint2.setStrokeWidth(12f);
	    paint2.setAntiAlias(true);
	    paint2.setColor(Color.argb(200, 181, 111, 233));
	    BarGraphRenderer barGraphRendererTop = new BarGraphRenderer(4, paint2, true);
	    mVisualizerView.addRenderer(barGraphRendererTop);
	  }

	  private void addCircleBarRenderer()
	  {
	    Paint paint = new Paint();
	    paint.setStrokeWidth(8f);
	    paint.setAntiAlias(true);
	    paint.setXfermode(new PorterDuffXfermode(Mode.LIGHTEN));
	    paint.setColor(Color.argb(255, 222, 92, 143));
	    CircleBarRenderer circleBarRenderer = new CircleBarRenderer(paint, 32, true);
	    mVisualizerView.addRenderer(circleBarRenderer);
	  }

	  private void addCircleRenderer()
	  {
	    Paint paint = new Paint();
	    paint.setStrokeWidth(3f);
	    paint.setAntiAlias(true);
	    paint.setColor(Color.argb(255, 222, 92, 143));
	    CircleRenderer circleRenderer = new CircleRenderer(paint, true);
	    mVisualizerView.addRenderer(circleRenderer);
	  }

	  private void addLineRenderer()
	  {
	    Paint linePaint = new Paint();
	    linePaint.setStrokeWidth(1f);
	    linePaint.setAntiAlias(true);
	    linePaint.setColor(Color.argb(88, 0, 128, 255));

	    Paint lineFlashPaint = new Paint();
	    lineFlashPaint.setStrokeWidth(5f);
	    lineFlashPaint.setAntiAlias(true);
	    lineFlashPaint.setColor(Color.argb(188, 255, 255, 255));
	    LineRenderer lineRenderer = new LineRenderer(linePaint, lineFlashPaint, true);
	    mVisualizerView.addRenderer(lineRenderer);
	  }
	  
	  public void stopPressed(View view)
	  {
		  mediaPlayer.stop();
	  }
	
	  
}
