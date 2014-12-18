package com.music;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

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
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.music.visual.VisualizerView;
import com.music.visual.render.BarGraphRenderer;
import com.music.visual.render.CircleBarRenderer;
import com.music.visual.render.CircleRenderer;
import com.music.visual.render.LineRenderer;

public class WeDance extends Activity implements OnClickListener, OnTouchListener, OnCompletionListener, OnBufferingUpdateListener{
	
	private ImageButton buttonPlayPause;
	private SeekBar seekBarProgress;
	
    ProgressDialog dialog;
    private String type ="null";

	private ImageButton songListView;
	public EditText editTextSongURL;
	Spinner mSprPlaceType;
	String[] mPlaceType=null;
	String[] mPlaceTypeName=null;
	URL globalURL;
	private RecieveUpdates receivedFromService; 
	private int currentSongIndex = 0;
	PlayingSong songDetails;
	MusicList listAll= MusicList.getInstance();
	RelativeLayout bgColor;
	private int spinnerSelection,selectPosition=0;
	//FrameLayout bgColor;
	static final String PLAYER_STATE="playerState";
	static final String STATE_SELECTED = "linkPosition";
    final static String TAG = "ServiceMusic";
	TextView myText;
	    
	private MediaPlayer mediaPlayer;  
	
	private int mediaFileLengthInMilliseconds; // this value contains the song duration in milliseconds. Look at getDuration() method in MediaPlayer class
	
	private final Handler handler = new Handler();
	
	private ArrayList<HashMap<String, String>> songsList = new ArrayList<HashMap<String, String>>();
	private int pState=0;
   
	
	private VisualizerView mVisualizerView;
	private TextView mStatusTextView;
	
	
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
    	if (receivedFromService==null)receivedFromService=new RecieveUpdates();
    	IntentFilter intentFilter = new IntentFilter(ServiceMusic.LOADING);
        registerReceiver(receivedFromService,intentFilter);
        IntentFilter intentFilter1 = new IntentFilter(ServiceMusic.PLAYING);
        registerReceiver(receivedFromService,intentFilter1);
        IntentFilter intentStopped = new IntentFilter(ServiceMusic.STOPPED);
        registerReceiver(receivedFromService,intentStopped);
        Log.d(getClass().getName(),"Spinnner Selection is now " + selectPosition);
        Log.e(getClass().getName(),"Linking Player onResume " );
	    ServiceMusic.getInstance();

		if (!ServiceMusic.isMediaPlaying()){
        	Log.e("Dance","onResume Player not longer playing");

            //Intent intent = new Intent();
            //intent.setClassName("com.music.action.STOP","STOP");

            CreateExplicitFromImplicitIntent.createExplicitFromImplicitIntent(getApplicationContext(),new Intent(ServiceMusic.ACTION_STOP));
            CreateExplicitFromImplicitIntent.createExplicitFromImplicitIntent(getApplication(),new Intent(ServiceMusic.ACTION_PLAY));

       	//startService(new Intent(ServiceMusic.ACTION_STOP));
       	//startService(new Intent(ServiceMusic.ACTION_PLAY));
       	//startService(new Intent(ServiceMusic.ACTION_STOP));
         if (isFinishing() && mediaPlayer != null) {
        		 
        		 mediaPlayer.release();
        		 mediaPlayer = null;
     	      
     	       
     	    }
		}
        	
        	try{
        		//added new by caleb
        		mediaPlayer=ServiceMusic.getInstance().mPlayer;

        		if (mediaPlayer!=null){
        			Log.d("Dance","OK to Release - NOT a null player");
        			
        		}else{
        			Log.e("Dance","Not Releasing");
        			
        		}
        		mVisualizerView.release();
        		mVisualizerView.link(mediaPlayer);
        		Log.d("Dance","Successfully Linked");
        		addLineRenderer();
				addCircleBarRenderer();
				addBarGraphRenderers();
 	        }
 	        catch(Exception s){
 	        	
 	        	Log.e(getClass().getName(),"Error Linking on Resume " +s.toString());
 	        	
 	        }
     	  
        
		
    }
    
    @Override
    public void onPause(){
    	super.onPause();
    	 if (receivedFromService!=null)unregisterReceiver(receivedFromService);
    	  Log.d(getClass().getName(),"Spinnner Selection is now " + selectPosition);
    	  
    	  mediaPlayer=ServiceMusic.getInstance().mPlayer;
    	  if (mediaPlayer!=null){
    		  Log.e("Music","Media not null");
    		 // 	mediaPlayer.reset();
      		//	mediaPlayer.release();
      		//	mediaPlayer = null;
    	  }
    	  else{
    		  Log.e("Music","Media  null");
    	  }
    	 if (ServiceMusic.isMediaPlaying()){
         	Log.d("Dance","onPause Player isMediaPlaying");
         	//startService(new Intent(ServiceMusic.ACTION_STOP));
         	//mediaPlayer.reset();
         	//mediaPlayer.release();
	       // mediaPlayer = null;
         	
         	//startService(new Intent(ServiceMusic.ACTION_STOP));
         }else{
        	 Log.d("Dance","onPause Player NOT isMediaPlaying");
        	
        	 mediaPlayer=ServiceMusic.getInstance().mPlayer;
        	if (mediaPlayer!=null)
            {
              	CreateExplicitFromImplicitIntent.createExplicitFromImplicitIntent(getApplicationContext(),new Intent(ServiceMusic.ACTION_STOP));
                //Intent i=new Intent(ServiceMusic.ACTION_STOP);
                //startService(i);
                //startService(new Intent(ServiceMusic.ACTION_STOP));
        		//mediaPlayer.reset();
        		//mediaPlayer.release();
        		//mediaPlayer = null;
        	}
        	 
        	
         }
    	 	//mediaPlayer.release();
    	 //	mediaPlayer = null;
    	 //	try{
    	 	//	mVisualizerView.release(); //added new by caleb
    	 //  catch(Exception s){}
    	   /* if (isFinishing() && mediaPlayer != null) {
    	  
    	        mediaPlayer.release();
    	       mediaPlayer = null;
    	        try{
    	        mVisualizerView.release(); //added new by caleb
    	        }
    	        catch(Exception s){}
    	    }*/
    	   

    }
    
    @Override
    public void onBackPressed(){
    	super.onBackPressed();
    	 if (ServiceMusic.isMediaPlaying()){
          	Log.d("Dance","Back Button Pressed Disconnecting Player");
            //CreateExplicitFromImplicitIntent.createExplicitFromImplicitIntent(getApplicationContext(),new Intent(ServiceMusic.ACTION_STOP));
          	//startService(new Intent(ServiceMusic.ACTION_STOP));
             Intent i=new Intent(ServiceMusic.ACTION_STOP);
             startService(i);

         }
    	
    }
    /** This method initialise all the views in project*/
    private void initView() {
            
    	bgColor = (RelativeLayout)findViewById(R.id.bgID);
		buttonPlayPause = (ImageButton)findViewById(R.id.ButtonTestPlayPause);
		songListView=(ImageButton) findViewById(R.id.btnPlaylist);
		mPlaceType = getResources().getStringArray(R.array.stationAddress);
	        // Array of place type names
	    mPlaceTypeName = getResources().getStringArray(R.array.stationAddress_name);
		buttonPlayPause.setOnClickListener(this);
		seekBarProgress = (SeekBar)findViewById(R.id.SeekBarTestPlay);	
		seekBarProgress.setMax(99); // It means 100% .0-99
		seekBarProgress.setOnTouchListener(this);
		//editTextSongURL = (EditText)findViewById(R.id.EditTextSongURL);
		//editTextSongURL.setText(R.string.testsong_20_sec);
		myText=(TextView)findViewById(R.id.displayText);
		final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, mPlaceTypeName);
        // Getting reference to the Spinner
        mSprPlaceType = (Spinner) findViewById(R.id.spr_place_type);
        // Setting adapter on Spinner to set place types
        mSprPlaceType.setAdapter(adapter);
        mediaPlayer=ServiceMusic.mPlayer;
	//	mediaPlayer.setOnBufferingUpdateListener(this);
	//	mediaPlayer.setOnCompletionListener(this);
		mVisualizerView = (VisualizerView) findViewById(R.id.visualizerView);
		//mSprPlaceType.setOnItemSelectedListener(this);
		mSprPlaceType.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
	    	public void onItemSelected(AdapterView<?> parent, View view, int pos,
	    			long id) {
	    		// TODO Auto-generated method stub
	        	//mediaPlayer.release();
	        	Object val=parent.getItemAtPosition(pos);
	            type = mPlaceType[pos];
	        	if (pos==0){
	        		  return;
	        		
	        	}if(selectPosition==pos){
	        		return;
	        	}
	        	else{

	        	URL useURL = null;
				try {
					useURL = new URL(type);
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        	globalURL=useURL;
	        	Log.d(getClass().getName(),"Selected=="+val.toString() + "......URL==" + type);
	      //  if(!mediaPlayer.isPlaying()){
			//		mediaPlayer.start();
			// 	}
	        	//mVisualizerView.release();
	        	CreateExplicitFromImplicitIntent.createExplicitFromImplicitIntent(getApplicationContext(),new Intent(ServiceMusic.ACTION_PAUSE));
	        	//startService(new Intent(ServiceMusic.ACTION_PAUSE));
                //CreateExplicitFromImplicitIntent.createExplicitFromImplicitIntent(getApplicationContext(),new Intent(ServiceMusic.ACTION_URL));
                CreateExplicitFromImplicitIntent.createExplicitFromImplicitIntent(getApplicationContext(),new Intent(ServiceMusic.ACTION_URL));

                    Uri uri = Uri.parse(type);
                    Intent i=new Intent(ServiceMusic.ACTION_URL,uri,getApplicationContext(),ServiceMusic.class);
	        	    //Intent i = new Intent(ServiceMusic.ACTION_URL);
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
		/*	try {
				mediaPlayer.setDataSource(editTextSongURL.getText().toString()); // setup song from http://www.hrupin.com/wp-content/uploads/mp3/testsong_20_sec.mp3 URL to mediaplayer data source
				mediaPlayer.prepare(); // you must call this method after setup the datasource in setDataSource method. After calling prepare() the instance of MediaPlayer starts load data from URL to internal buffer. 
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			mediaFileLengthInMilliseconds = mediaPlayer.getDuration(); // gets the song length in milliseconds from URL
			
			if(!mediaPlayer.isPlaying()){
				mediaPlayer.start();
				buttonPlayPause.setImageResource(R.drawable.button_pause);
			}else {
				mediaPlayer.pause();
				mediaPlayer.reset();
				System.out.println("Releasing Player");
				buttonPlayPause.setImageResource(R.drawable.button_play);
			}
			
			primarySeekBarProgressUpdater();*/
		//	mediaPlayer =ServiceMusic.getInstance().mPlayer;
		//	mediaFileLengthInMilliseconds = mediaPlayer.getDuration();
			mediaPlayer =ServiceMusic.getInstance().mPlayer;
		
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

	@Override
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

	@Override
	public void onCompletion(MediaPlayer mp) {
		 /** MediaPlayer onCompletion event handler. Method which calls then song playing is complete*/
		this.mediaPlayer=mp;
        Log.d(this.getClass().getName(),"On COmpletion detected ");
		//buttonPlayPause.setImageResource(R.drawable.button_play);
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		/** Method which updates the SeekBar secondary progress by current song loading from URL position*/
		seekBarProgress.setSecondaryProgress(percent);
        Log.i(this.getClass().getName(),"buffering... pls wait..." + percent);
	}
	
	private class RecieveUpdates extends BroadcastReceiver{
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
				
				mediaPlayer =ServiceMusic.getInstance().mPlayer;
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
