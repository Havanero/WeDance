/*   
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.music;


import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.RemoteControlClient;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;

/**
 * Service that handles media playback. This is the Service through which we perform all the media
 * handling in our application. Upon initialization, it starts a {@link MusicRetriever} to scan
 * the user's media. Then, it waits for Intents (which come from our main activity,
 * {@link MainActivity}, which signal the service to perform specific operations: Play, Pause,
 * Rewind, Skip, etc.
 */
public class ServiceMusic extends Service implements OnCompletionListener, OnPreparedListener,
                OnErrorListener, MusicFocusable,
                PrepareMusicRetrieverTask.MusicRetrieverPreparedListener, OnBufferingUpdateListener,OnInfoListener {

    // The tag we put on debug messages
    final static String TAG = "ServiceMusic";
    String myURL=null;
    
    public static String fullPlay;
    boolean running=true;
    String stationNames;
    String getStationName=null;
    Worker  w = new Worker();
    Worker  s = new Worker();
    Thread stop = new Thread(s);
    //NetworkTask getStreamInfo = new NetworkTask();
  
    String titlePlaing,artistPlaying="";
    private static ServiceMusic instance = new ServiceMusic();
    
    // These are the Intent actions that we are prepared to handle. Notice that the fact these
    // constants exist in our class is a mere convenience: what really defines the actions our
    // service can handle are the <action> tags in the <intent-filters> tag for our service in
    // AndroidManifest.xml.
    public static final String ACTION_TOGGLE_PLAYBACK ="com.music.action.TOGGLE_PLAYBACK";
    public static final String ACTION_PLAY = "com.music.action.PLAY";
    public static final String ACTION_PAUSE = "com.music.action.PAUSE";
    public static final String ACTION_STOP = "com.music.action.STOP";
    public static final String ACTION_SKIP = "com.music.action.SKIP";
    public static final String ACTION_REWIND = "com.music.action.REWIND";
    public static final String ACTION_URL = "com.music.action.URL";
    public static final String LOADING="(Loading)";
    public static final String PLAYING="(playing...)";
    public static final String STOPPED="STOPPED";
    public static final String VISUALIZE="Visualizing";
    
    // The volume we set the media player to when we lose audio focus, but are allowed to reduce
    // the volume instead of stopping playback.
    public static final float DUCK_VOLUME = 0.1f;


    
    // our media player
  public static MediaPlayer mPlayer; //works
   
   // MediaPlayer mPlayer = new MediaPlayer();    
    public static  ServiceMusic getInstance() {
    	if(instance==null){
    		instance= new ServiceMusic();
    	}
        return instance;
    }


    // our AudioFocusHelper object, if it's available (it's available on SDK level >= 8)
    // If not available, this will be null. Always check for null before using!
    AudioFocusHelper mAudioFocusHelper = null;

    // indicates the state our service:
    enum State {
        Retrieving, // the MediaRetriever is retrieving music
        Stopped,    // media player is stopped and not prepared to play
        Preparing,  // media player is preparing...
        Playing,    // playback active (media player ready!). (but the media player may actually be
                    // paused in this state if we don't have audio focus. But we stay in this state
                    // so that we know we have to resume playback once we get focus back)
        Paused      // playback paused (media player ready!)
    };

    State mState = State.Retrieving;

    // if in Retrieving mode, this flag indicates whether we should start playing immediately
    // when we are ready or not.
    boolean mStartPlayingAfterRetrieve = false;

    // if mStartPlayingAfterRetrieve is true, this variable indicates the URL that we should
    // start playing when we are ready. If null, we should play a random song from the device
    Uri mWhatToPlayAfterRetrieve = null;

    enum PauseReason {
        UserRequest,  // paused by user request
        FocusLoss,    // paused because of audio focus loss
    };

    // why did we pause? (only relevant if mState == State.Paused)
    PauseReason mPauseReason = PauseReason.UserRequest;

    // do we have audio focus?
    enum AudioFocus {
        NoFocusNoDuck,    // we don't have audio focus, and can't duck
        NoFocusCanDuck,   // we don't have focus, but can play at a low volume ("ducking")
        Focused           // we have full audio focus
    }
    AudioFocus mAudioFocus = AudioFocus.NoFocusNoDuck;

    // title of the song we are currently playing
    String mSongTitle = "";

    // whether the song we are playing is streaming from the network
    boolean mIsStreaming = false;

    // Wifi lock that we hold when streaming files from the internet, in order to prevent the
    // device from shutting off the Wifi radio
    WifiLock mWifiLock;

    // The ID we use for the notification (the onscreen alert that appears at the notification
    // area at the top of the screen as an icon -- and as text as well if the user expands the
    // notification area).
    final int NOTIFICATION_ID = 1;

    // Our instance of our MusicRetriever, which handles scanning for media and
    // providing titles and URIs as we need.
    MusicRetriever mRetriever;

    // our RemoteControlClient object, which will use remote control APIs available in
    // SDK level >= 14, if they're available.
    RemoteControlClientCompat mRemoteControlClientCompat;

    // Dummy album art we will pass to the remote control (if the APIs are available).
    Bitmap mDummyAlbumArt;

    // The component name of MusicIntentReceiver, for use with media button and remote control
    // APIs
    ComponentName mMediaButtonReceiverComponent;

    AudioManager mAudioManager;
    NotificationManager mNotificationManager;

    Notification mNotification = null;

    /**
     * Makes sure the media player exists and has been reset. This will create the media player
     * if needed, or reset the existing media player if one already exists.
     */
    void createMediaPlayerIfNeeded() {
    	
        if (mPlayer == null) {
            mPlayer = new MediaPlayer();
            
            // Make sure the media player will acquire a wake-lock while playing. If we don't do
            // that, the CPU might go to sleep while the song is playing, causing playback to stop.
            //
            // Remember that to use this, we have to declare the android.permission.WAKE_LOCK
            // permission in AndroidManifest.xml.
            mPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

            // we want the media player to notify us when it's ready preparing, and when it's done
            // playing:
            mPlayer.setOnPreparedListener(this);
            mPlayer.setOnCompletionListener(this);
            mPlayer.setOnErrorListener(this);
            mPlayer.setOnBufferingUpdateListener(this);
            mPlayer.setOnInfoListener(this);
            
        }
        else
            mPlayer.reset();
        	
    }

    @Override
    public void onCreate() {
    	 
    	
        Log.i(TAG, "debug: Creating service");
        

        // Create the Wifi lock (this does not acquire the lock, this just creates it)
        mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                        .createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");
        

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        // Create the retriever and start an asynchronous task that will prepare it.
        mRetriever = new MusicRetriever(getContentResolver());
        (new PrepareMusicRetrieverTask(mRetriever,this)).execute();
       
        // create the Audio Focus Helper, if the Audio Focus feature is available (SDK 8 or above)
        if (android.os.Build.VERSION.SDK_INT >= 8)
            mAudioFocusHelper = new AudioFocusHelper(getApplicationContext(), this);
        else
            mAudioFocus = AudioFocus.Focused; // no focus feature, so we always "have" audio focus

       // mDummyAlbumArt = BitmapFactory.decodeResource(getResources(), R.drawable.dummy_album_art);

        mMediaButtonReceiverComponent = new ComponentName(this, ReceiveIntent.class);
       
    }

    /**
     * Called when we receive an Intent. When we receive an intent sent to us via startService(),
     * this is the method that gets called. So here we react appropriately depending on the
     * Intent's action, which specifies what is being requested of us.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        System.out.println("Action Found is ..." + action);
        if (action.equals(ACTION_TOGGLE_PLAYBACK)) processTogglePlaybackRequest();
        else if (action.equals(ACTION_PLAY)) processPlayRequest();
        else if (action.equals(ACTION_PAUSE)) processPauseRequest();
        else if (action.equals(ACTION_SKIP)) processSkipRequest();
        else if (action.equals(ACTION_STOP)) processStopRequest();
        else if (action.equals(ACTION_REWIND)) processRewindRequest();
        else if (action.equals(ACTION_URL)) processAddRequest(intent);

        return START_NOT_STICKY; // Means we started the service, but don't want it to
                                 // restart in case it's killed.
    }

    void processTogglePlaybackRequest() {
        if (mState == State.Paused || mState == State.Stopped) {
            processPlayRequest();
        } else {
            processPauseRequest();
        }
    }

    void processPlayRequest() {
        if (mState == State.Retrieving) {
            // If we are still retrieving media, just set the flag to start playing when we're
            // ready
            mWhatToPlayAfterRetrieve = null; // play a random song
            mStartPlayingAfterRetrieve = true;
            
          
            return;
        }

        tryToGetAudioFocus();

        // actually play the song

        if (mState == State.Stopped) {
            // If we're stopped, just go ahead to the next song and start playing
        	Log.e(TAG,"Music has stopped should play next song..");
        	
        	playNextSong(null);
        }
        else if (mState == State.Paused) {
            // If we're paused, just continue playback and restore the 'foreground service' state.
            mState = State.Playing;
            setUpAsForeground(mSongTitle + " (playing)");
            configAndStartMediaPlayer();
            Log.i(TAG," pause playing" +mSongTitle);
            Log.e(TAG,"Music has paused");
        }

        // Tell any remote controls that our playback state is 'playing'.
        if (mRemoteControlClientCompat != null) {
            mRemoteControlClientCompat
                    .setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
        }
    }

    void processPauseRequest() {
        if (mState == State.Retrieving) {
            // If we are still retrieving media, clear the flag that indicates we should start
            // playing when we're ready
            mStartPlayingAfterRetrieve = false;
            return;
        }

        if (mState == State.Playing) {
        	 Log.e(TAG,"Music is still Playing");
            // Pause media player and cancel the 'foreground service' state.
            mState = State.Paused;
            mPlayer.pause();
           
            relaxResources(false); // while paused, we always retain the MediaPlayer
            // do not give up audio focus
        }

        // Tell any remote controls that our playback state is 'paused'.
        if (mRemoteControlClientCompat != null) {
            mRemoteControlClientCompat
                    .setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
        }
    }

    void processRewindRequest() {
        if (mState == State.Playing || mState == State.Paused)
            mPlayer.seekTo(0);
    }

    void processSkipRequest() {
        if (mState == State.Playing || mState == State.Paused) {
            tryToGetAudioFocus();
            playNextSong(null);
        }
    }

    void processStopRequest() {
    	w.done = true;
    	processStopRequest(false);
    
    	sendBroadcast(new Intent(ServiceMusic.STOPPED));
    	
    	
        
    }

    void processStopRequest(boolean force) {
        if (mState == State.Playing || mState == State.Paused || force) {
            mState = State.Stopped;

            // let go of all resources...
            relaxResources(true);
            giveUpAudioFocus();

            // Tell any remote controls that our playback state is 'paused'.
            if (mRemoteControlClientCompat != null) {
                mRemoteControlClientCompat
                        .setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
            }

            // service is no longer necessary. Will be started again if needed.
            Log.d(TAG,"StoppingSelf() Called");
            stopSelf();
        }
    }

    /**
     * Releases resources used by the service for playback. This includes the "foreground service"
     * status and notification, the wake locks and possibly the MediaPlayer.
     *
     * @param releaseMediaPlayer Indicates whether the Media Player should also be released or not
     */
    void relaxResources(boolean releaseMediaPlayer) {
        // stop being a foreground service
        stopForeground(true);

        // stop and release the Media Player, if it's available
        if (releaseMediaPlayer && mPlayer != null) {
            mPlayer.reset();
            mPlayer.release();
            mPlayer = null;
            
        }

        // we can also release the Wifi lock, if we're holding it
        if (mWifiLock.isHeld()) mWifiLock.release();
    }

    void giveUpAudioFocus() {
        if (mAudioFocus == AudioFocus.Focused && mAudioFocusHelper != null
                                && mAudioFocusHelper.abandonFocus())
            mAudioFocus = AudioFocus.NoFocusNoDuck;
    }

    /**
     * Reconfigures MediaPlayer according to audio focus settings and starts/restarts it. This
     * method starts/restarts the MediaPlayer respecting the current audio focus state. So if
     * we have focus, it will play normally; if we don't have focus, it will either leave the
     * MediaPlayer paused or set it to a low volume, depending on what is allowed by the
     * current focus settings. This method assumes mPlayer != null, so if you are calling it,
     * you have to do so from a context where you are sure this is the case.
     */
    void configAndStartMediaPlayer()   {
    	
    	try{
        if (mAudioFocus == AudioFocus.NoFocusNoDuck) {
            // If we don't have audio focus and can't duck, we have to pause, even if mState
            // is State.Playing. But we stay in the Playing state so that we know we have to resume
            // playback once we get the focus back.
            if (mPlayer.isPlaying()) mPlayer.pause();
            if (!mPlayer.isPlaying()){
            	Log.e(TAG,"Not Playing");
            	playNextSong(null);
            }
            return;
        }
        else if (mAudioFocus == AudioFocus.NoFocusCanDuck)
            mPlayer.setVolume(DUCK_VOLUME, DUCK_VOLUME);  // we'll be relatively quiet
       
        else
        	
        		mPlayer.setVolume(1.0f, 1.0f); // we can be loud
        	
        	

        if (!mPlayer.isPlaying()) mPlayer.start();
    	}catch(Exception s){
    		Log.e(TAG,"Error configAndStartMediaPlayer ");
    		  
    	}
    }

    void processAddRequest(Intent intent) {
        // user wants to play a song directly by URL or path. The URL or path comes in the "data"
        // part of the Intent. This Intent is sent by {@link MainActivity} after the user
        // specifies the URL/path via an alert box.
    	getStationName=intent.getStringExtra("StationName");
    	Log.e(TAG,"Getting Intent" + intent.getStringExtra("StationName"));
        if (mState == State.Retrieving) {
            // we'll play the requested URL right after we finish retrieving
            mWhatToPlayAfterRetrieve = intent.getData();
            mStartPlayingAfterRetrieve = true;

        }
        else if (mState == State.Playing || mState == State.Paused || mState == State.Stopped) {
        	
            Log.i(TAG, "Playing from URL/path: " + intent.getData().toString());
            tryToGetAudioFocus();
            playNextSong(intent.getData().toString());
              
            
        }
        
    }

    void tryToGetAudioFocus() {
        if (mAudioFocus != AudioFocus.Focused && mAudioFocusHelper != null
                        && mAudioFocusHelper.requestFocus())
            mAudioFocus = AudioFocus.Focused;
    }

    /**
     * Starts playing the next song. If manualUrl is null, the next song will be randomly selected
     * from our Media Retriever (that is, it will be a random song in the user's device). If
     * manualUrl is non-null, then it specifies the URL or path to the song that will be played
     * next.
     */
   
	void playNextSong(String manualUrl) {
        mState = State.Stopped;
        relaxResources(false); // release everything except MediaPlayer
      
        Log.e(TAG,"PlayNextSong running...for " + manualUrl);
        try {
            MusicRetriever.Item playingItem = null;
           
            
            if (manualUrl != null) {
                // set the source of the media player to a manual URL or path
                createMediaPlayerIfNeeded();
                mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mPlayer.setDataSource(manualUrl);
                
                mIsStreaming = manualUrl.startsWith("http:") || manualUrl.startsWith("https:");
                Log.i(TAG,"Playing Manual URL:" + mIsStreaming);
                
              
               /* Toast.makeText(getBaseContext(), stationNames, Toast.LENGTH_LONG)
                .show();*/
                playingItem = new MusicRetriever.Item(0, null, manualUrl, null, 0);
               
               
            }
            else {
                mIsStreaming = false; // playing a locally available song
                Log.e(TAG,"We Are NOT Streaming ");
                playingItem = mRetriever.getRandomItem();
                
                
                if (playingItem == null) {
                    Toast.makeText(this,
                            "No available music to play. Place some music on your external storage "
                            + "device (e.g. your SD card) and try again.",
                            Toast.LENGTH_LONG).show();
                   
                    processStopRequest(true); // stop everything!
                    return;
                }
                s.done=true;
                Log.e(TAG,"playingItem = mRetriever.getRandomItem() " + playingItem.getTitle());
                // set the source of the media player a a content URI
                createMediaPlayerIfNeeded();
                mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mPlayer.setDataSource(getApplicationContext(), playingItem.getURI());
                
            }

            mSongTitle=playingItem.getTitle();
            
            
            artistPlaying=playingItem.getArtist();
            
            mState = State.Preparing;
            setUpAsForeground(mSongTitle + " (loading)");
          
            sendBroadcast(new Intent(ServiceMusic.LOADING));
            s.done=true;
            
            try{
                	stop.start();
                }
                catch(java.lang.IllegalThreadStateException tError){
                	s.done=true;
                	Log.e(TAG,"Thread already started");
                	
                }

   			
            // Use the media button APIs (if available) to register ourselves for media button
            // events

            MediaButtonHelper.registerMediaButtonEventReceiverCompat(
                    mAudioManager, mMediaButtonReceiverComponent);

            // Use the remote control APIs (if available) to set the playback state

            if (mRemoteControlClientCompat == null) {
                Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
                intent.setComponent(mMediaButtonReceiverComponent);
                mRemoteControlClientCompat = new RemoteControlClientCompat(
                        PendingIntent.getBroadcast(this /*context*/,
                                0 /*requestCode, ignored*/, intent /*intent*/, 0 /*flags*/));
                RemoteControlHelper.registerRemoteControlClient(mAudioManager,
                        mRemoteControlClientCompat);
            }

            mRemoteControlClientCompat.setPlaybackState(
                    RemoteControlClient.PLAYSTATE_PLAYING);

            mRemoteControlClientCompat.setTransportControlFlags(
                    RemoteControlClient.FLAG_KEY_MEDIA_PLAY |
                    RemoteControlClient.FLAG_KEY_MEDIA_PAUSE |
                    RemoteControlClient.FLAG_KEY_MEDIA_NEXT |
                    RemoteControlClient.FLAG_KEY_MEDIA_STOP);

            // Update the remote controls
            mRemoteControlClientCompat.editMetadata(true)
                    .putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, playingItem.getArtist())
                    .putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, playingItem.getAlbum())
                    .putString(MediaMetadataRetriever.METADATA_KEY_TITLE, playingItem.getTitle())
                    .putLong(MediaMetadataRetriever.METADATA_KEY_DURATION,
                            playingItem.getDuration())
                    // TODO: fetch real item artwork
                    .putBitmap(
                            RemoteControlClientCompat.MetadataEditorCompat.METADATA_KEY_ARTWORK,
                            mDummyAlbumArt)
                    .apply();
          
            

            // starts preparing the media player in the background. When it's done, it will call
            // our OnPreparedListener (that is, the onPrepared() method on this class, since we set
            // the listener to 'this').
            //
            // Until the media player is prepared, we *cannot* call start() on it!
            mPlayer.prepareAsync();

            // If we are streaming from the internet, we want to hold a Wifi lock, which prevents
            // the Wifi radio from going to sleep while the song is playing. If, on the other hand,
            // we are *not* streaming, we want to release the lock if we were holding it before.
            if (mIsStreaming) mWifiLock.acquire();
                 
           // else if (mWifiLock.isHeld()) mWifiLock.release();
            if (mWifiLock.isHeld()) mWifiLock.release();
            
            if (!mIsStreaming){
            	Log.e(TAG,"NO Longer STREAMING"); //added  by Caleb for testing
            	 
            }
        }
        catch (IOException ex) {
            Log.e("MusicService", "IOException playing next song: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /** Called when media player is done playing current song. */
    public void onCompletion(MediaPlayer player) {
        // The media player finished playing the current song, so we go ahead and start the next.
    	
    	this.mPlayer=player;
    	Log.e(TAG,"Should start next song");
    	
        playNextSong(null);
    }

    /** Called when media player is done preparing. */
    public void onPrepared(MediaPlayer player) {
        // The media player is done preparing. That means we can start playing!
        mState = State.Playing;
        updateNotification(mSongTitle + " (playing)");
        
        //sendBroadcast(new Intent(ServiceMusic.PLAYING));
        Log.i(TAG,"playing...." +mSongTitle);
        myURL=mSongTitle;
        Intent intent = new Intent(ServiceMusic.PLAYING);
        if (getStationName!=null){
        	
        	artistPlaying="Streaming";
        	intent.putExtra(PLAYING, getStationName + " ["+ artistPlaying+ " ]");
        }else{
        	intent.putExtra(PLAYING, mSongTitle + " ["+ artistPlaying+ " ]");
        }
	    sendBroadcast(intent);
	    
        //myURL=mSongTitle;
        configAndStartMediaPlayer();
        
     /* if (!mSongTitle.startsWith("http")||(!mSongTitle.startsWith("https"))){
    	   w.done=true; 
       }*/
    //    w.done=true;
        Thread t2 = new Thread(w);
        t2.start();    
        
        
    }
    
    private class Worker  implements Runnable{
    	boolean done;
    	
    		 @Override
    		public void run() {
    			while(!done){
    			     try {
						Thread.sleep(5000);
						if(titlePlaing != null){
							 Intent intent = new Intent(ServiceMusic.PLAYING);
						     intent.putExtra(PLAYING, titlePlaing + " ["+ artistPlaying+ " ]");
						     sendBroadcast(intent);
							 }
					
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    			     getDetails();
    			         			    
    			}
    			
    		}
    }
    
 public void getDetails(){
	   if (!myURL.startsWith("http")){
		   Log.e(TAG,"Cancelling Thread URL not good");
		   w.done=true; 
	   }
	   System.out.println("url is " + myURL);
	   new NetworkTask().execute(myURL); 
	   

   }

    /** Updates the notification. */
    void updateNotification(String text) {
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), WeDance.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
        mNotification.setLatestEventInfo(getApplicationContext(), "CubanoFM -Oh Yeah!", text, pi);
        mNotificationManager.notify(NOTIFICATION_ID, mNotification);
       
    }

    /**
     * Configures service as a foreground service. A foreground service is a service that's doing
     * something the user is actively aware of (such as playing music), and must appear to the
     * user as a notification. That's why we create the notification here.
     */
    void setUpAsForeground(String text) {
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), WeDance.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
        mNotification = new Notification();
        mNotification.tickerText = text;
        mNotification.icon = R.drawable.ic_stat_playing;
       
        mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
        mNotification.setLatestEventInfo(getApplicationContext(), "CubanoFM -Oh Yeah!",
                text, pi);
        startForeground(NOTIFICATION_ID, mNotification);
        
        Log.e(TAG,"Playing this.."+text);
        
    }
    
    

    /**
     * Called when there's an error playing media. When this happens, the media player goes to
     * the Error state. We warn the user about the error and reset the media player.
     */
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Toast.makeText(getApplicationContext(), "Media player error! Resetting.",
            Toast.LENGTH_SHORT).show();
        Log.e(TAG, "Error: what=" + String.valueOf(what) + ", extra=" + String.valueOf(extra));
        Intent stop=new Intent(ServiceMusic.STOPPED);
        stop.putExtra(STOPPED, "Error");
        //sendBroadcast(new Intent(ServiceMusic.STOPPED));
        sendBroadcast(stop);
        mState = State.Stopped;
        relaxResources(true);
        giveUpAudioFocus();
        stopService(new Intent(this, ServiceMusic.class));
        w.done=true;
        
        return true; // true indicates we handled the error
    }

    public void onGainedAudioFocus() {
        Toast.makeText(getApplicationContext(), "gained audio focus.", Toast.LENGTH_SHORT).show();
        mAudioFocus = AudioFocus.Focused;

        // restart media player with new focus settings
        if (mState == State.Playing)
            configAndStartMediaPlayer();
    }

    public void onLostAudioFocus(boolean canDuck) {
        Toast.makeText(getApplicationContext(), "lost audio focus." + (canDuck ? "can duck" :
            "no duck"), Toast.LENGTH_SHORT).show();
        mAudioFocus = canDuck ? AudioFocus.NoFocusCanDuck : AudioFocus.NoFocusNoDuck;

        // start/restart/pause media player with new focus settings
        if (mPlayer != null && mPlayer.isPlaying())
            configAndStartMediaPlayer();
    }
  public void  OnAudioFocusChangeListener(){
	  Log.e(TAG,"Changed focus");
  }
    

    public void onMusicRetrieverPrepared() {
        // Done retrieving!
        mState = State.Stopped;

        // If the flag indicates we should start playing after retrieving, let's do that now.
        if (mStartPlayingAfterRetrieve) {
            tryToGetAudioFocus();
           
            playNextSong(mWhatToPlayAfterRetrieve == null ?
                    null : mWhatToPlayAfterRetrieve.toString());
        }
    }


    @Override
    public void onDestroy() {
        // Service is being killed, so make sure we release our resources
        mState = State.Stopped;
        relaxResources(true);
        giveUpAudioFocus();
        w.done=true;
        
    }

    @Override
    public IBinder onBind(Intent arg0) {
    	
    	Log.d(TAG,"Biding into " +arg0);
        return null;
    }
    
    private class NetworkTask extends AsyncTask<String,String, String>{
    	 
        String data = null;
        IcyStreamMeta title;
        
    	URL url;
 
        @Override
        protected String doInBackground(String... param) {
            try{

            	 url = new URL(param[0]);
            	 System.out.println("Processing URL" + url);
            	 title = new IcyStreamMeta(url);
            	// title.refreshMeta();
            	 System.out.println("extracting getTitle" + title.getTitle());
            	 System.out.println("extracting Artist" + title.getArtist());
            	 titlePlaing=title.getTitle();
            	 artistPlaying=title.getArtist();
            }catch(Exception e){
                Log.e("Background Task",e.toString());
                return null;
                
            }
          
            return titlePlaing+ " [ " + artistPlaying + "]";
        }
 
        // Executed after the complete execution of doInBackground() method
        @Override
        protected void onPostExecute(String result){
            Log.i(TAG,"Executing post Get Artists " + result);
        }
    }
 
@Override
public void onBufferingUpdate(MediaPlayer mp, int percent) {
	// TODO Auto-generated method stub
	//Log.d(TAG,"buffering on Service Call line 758==" +percent);
		
	}

@Override
public boolean onInfo(MediaPlayer player, int what, int extra) {
	// TODO Auto-generated method stub
	Log.e(TAG,"Buffering " + what + " and Buffering" + extra);
	
	if (what==player.MEDIA_INFO_BUFFERING_START){
		Toast.makeText(this,"MEDIA_INFO_BUFFERING...", Toast.LENGTH_LONG).show();
		
	}
	if (what==player.MEDIA_INFO_BAD_INTERLEAVING){
		Log.e(TAG,"MEDIA_INFO_BAD_INTERLEAVING connections");
	}
	if (what==player.MEDIA_INFO_BUFFERING_END){
		Log.e(TAG,"MEDIA_INFO_BUFFERING_END connections");
		Toast.makeText(this,"MEDIA_INFO_BUFFERING....", Toast.LENGTH_LONG).show();
	}
	
	return false;
}
public static boolean isMediaPlaying(){
	if (mPlayer!=null){
		try{
		return mPlayer.isPlaying();
		}
		catch(Exception e){}
	}
	return false;
}
}
