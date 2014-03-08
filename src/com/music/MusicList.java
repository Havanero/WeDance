package com.music;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.music.MusicRetriever.Item;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

public class MusicList  {
	
	private static final MusicList instance = new MusicList();
	final String TAG="MusicList";
	ContentResolver mContentResolver;
	
	private  ArrayList allMusic = new ArrayList();
	private  String myList,myIDList ;
	private ArrayList<HashMap<String, String>> songsList = new ArrayList<HashMap<String, String>>();
	 List<Item> mItems = new ArrayList<Item>();
 
	MusicRetriever mRetriever;
	private MusicList(){
	
		
		
	}
	
	public static MusicList getInstance(){
		return instance;
	}
	

	public  ArrayList<HashMap<String, String>> getMusic(){

		return songsList;
		
	}
	
public void setMusic(String result, String id){
	
		clearMusic();
		HashMap<String, String> song = new HashMap<String, String>();
		
		this.myList=result;
		this.myIDList=id;
		myList = myList.replaceAll("\\[", "").replaceAll("\\]","");
		song.put("songTitle",result);
		song.put("songPath",id);
		songsList.add(song);
		//System.out.println("setting Array for setMusic  class " + myList);
		
		
	}

public void clearMusic(){
			System.out.println("All Music Not Empty..Clearing");
			allMusic.clear();
			}

public void PrePare(){

    Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    Log.i(TAG, "Querying media...");
    Log.i(TAG, "URI: " + uri.toString());

    // Perform a query on the content resolver. The URI we're passing specifies that we
    // want to query for all audio media on external storage (e.g. SD card)
    Cursor cur = mContentResolver.query(uri, null,
            MediaStore.Audio.Media.IS_MUSIC + " = 1", null, null);
    Log.i(TAG, "Query finished. " + (cur == null ? "Returned NULL." : "Returned a cursor."));

    if (cur == null) {
        // Query failed...
       // Log.e(TAG, "Failed to retrieve music: cursor is null :-(");
        return;
    }
    if (!cur.moveToFirst()) {
        // Nothing to query. There is no music on the device. How boring.
        Log.e(TAG, "Failed to move cursor to first row (no query results).");
        return;
    }

    Log.i(TAG, "Listing...");

    // retrieve the indices of the columns where the ID, title, etc. of the song are
    int artistColumn = cur.getColumnIndex(MediaStore.Audio.Media.ARTIST);
    int titleColumn = cur.getColumnIndex(MediaStore.Audio.Media.TITLE);
    int albumColumn = cur.getColumnIndex(MediaStore.Audio.Media.ALBUM);
    int durationColumn = cur.getColumnIndex(MediaStore.Audio.Media.DURATION);
    int idColumn = cur.getColumnIndex(MediaStore.Audio.Media._ID);

    Log.i(TAG, "Title column index: " + String.valueOf(titleColumn));
    Log.i(TAG, "ID column index: " + String.valueOf(titleColumn));
    setMusic(String.valueOf(titleColumn), String.valueOf(idColumn));
    
    
    // add each song to mItems
    do {
        Log.i(TAG, "ID: " + cur.getString(idColumn) + " Title: " + cur.getString(titleColumn));
      
       
        mItems.add(new Item(
                cur.getLong(idColumn),
                cur.getString(artistColumn),
                cur.getString(titleColumn),
                cur.getString(albumColumn),
                cur.getLong(durationColumn)));
        //adding music here.
    } while (cur.moveToNext());

    Log.i(TAG, "Done querying media. MusicRetriever is ready.");

}
public static class Item {
    long id;
    String artist;
    String title;
    String album;
    long duration;

    public Item(long id, String artist, String title, String album, long duration) {
        this.id = id;
        this.artist = artist;
        this.title = title;
        this.album = album;
        this.duration = duration;
    }

    public long getId() {
        return id;
    }

    public String getArtist() {
        return artist;
    }

    public String getTitle() {
        return title;
    }

    public String getAlbum() {
        return album;
    }

    public long getDuration() {
        return duration;
    }

    public Uri getURI() {
        return ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
    }
}


}
