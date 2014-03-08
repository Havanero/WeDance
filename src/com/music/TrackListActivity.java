package com.music;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;


public class TrackListActivity extends ListActivity implements OnItemSelectedListener, PrepareMusicRetrieverTask.MusicRetrieverPreparedListener{
	
	 ListView listv;
	 ProgressDialog proDialog;
	 HashMap<String, String> mapMuisc;
	 MusicRetriever mRetriever;
	 MusicList listAll= MusicList.getInstance();
     ArrayList<String> me=new ArrayList<String>();
     int songIndex =0;
	
	 ArrayList<HashMap<String, String>> trackList = new ArrayList<HashMap<String,String>>();
	 public ArrayList<HashMap<String, String>> songsList = new ArrayList<HashMap<String, String>>();
	 
	 public static String KEY_REFERENCE = "reference"; // id of the place
	 public static String KEY_NAME = "name"; // name of the place
	 
	 @Override
	    protected void onCreate(Bundle savedInstanceState) {
		 super.onCreate(savedInstanceState);
	        setContentView(R.layout.list_music);
	        
	        this.songsList=listAll.getMusic();
	        
	        for (int i=0;i<songsList.size();i++){
	        	HashMap<String, String> song = songsList.get(i);
	        
	        }
	    
	            
	        for (int j=0;j<songsList.size();j++){
	        	HashMap<String, String> song = songsList.get(j);
	        	
	        	trackList.add(song);
	        	
	        }
	        ListAdapter adapter = new SimpleAdapter(this, trackList,
					R.layout.playlist_item, new String[] { "songTitle" }, new int[] {
							R.id.songTitle });
	        setListAdapter(adapter);
	        ListView lv = getListView();
	        lv.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View view,
						int position, long id) {
					// getting listitem index
					 songIndex = position;
					
					// Starting new intent
					/*Intent in = new Intent(getApplicationContext(),
							AndroidBuildingMusicPlayerActivity.class);
					// Sending songIndex to PlayerActivity
					in.putExtra("songIndex", songIndex);
					setResult(100, in);
					// Closing PlayListView
					finish();*/
					System.out.println("Selecting SongIndex" + songIndex);
					
					
					Object val=parent.getItemAtPosition(songIndex).toString();
					System.out.println("Selected=="+val.toString() );
					
					//startService(new Intent(ServiceMusic.ACTION_STOP));
					
					Intent in = new Intent(getApplicationContext(),
							WeDance.class);
					// Sending songIndex to PlayerActivity
					in.putExtra("songIndex", songIndex);
					setResult(100, in);
					
					finish(); //end activity
					/*
					//startService(new Intent(ServiceMusic.ACTION_STOP));
					//Intent i = new Intent(ServiceMusic.ACTION_PLAY);
					//startService(i);
					String songPath=songsList.get(songIndex).get("songPath");
					
					Uri uri = Uri.parse(songPath);
					System.out.println(uri.toString());
					
					//i.setData(uri);
		            
		           // startService(i);
		            startService(new Intent(ServiceMusic.ACTION_SKIP));
					finish();*/
				}
			});
	 }
	 
@Override
public void onBackPressed(){
	super.onBackPressed();
	System.out.println("Back Button Pressed");
    //finish();
}
	@Override
	public void onItemSelected(AdapterView<?> arg0, View view, int arg2,
			long arg3) {
		// TODO Auto-generated method stub
	
	}
	


	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void onMusicRetrieverPrepared() {
		// TODO Auto-generated method stub
		 MusicRetriever.Item playingItem = null;
		
		//ArrayList<String> m = ((Songs) music).getSongs();
			
	        playingItem = mRetriever.getRandomItem();
	      //	System.out.println("songTitle " +music.songTitles.toString());      
	        System.out.println("Loading title from tracklist" +  playingItem.getTitle());
	        
	}
	 

}
