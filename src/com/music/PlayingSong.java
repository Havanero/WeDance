package com.music;

import java.io.IOException;
import java.net.URL;

import android.os.AsyncTask;
import android.util.Log;

public class PlayingSong extends AsyncTask<URL, Integer, String>{

	 String data = null;
	    IcyStreamMeta title;

	    // Invoked by execute() method of this object
	    @Override
	    protected String doInBackground(URL... url) {
	        try{
	           // data = downloadUrl(url[0]);
	        	 title = new IcyStreamMeta(url[0]);
	        }catch(Exception e){
	            Log.d("Background Task",e.toString());
	        }
	        return title.toString();
	    }

	    // Executed after the complete execution of doInBackground() method
	    @Override
	    protected void onPostExecute(String result){
	      //  ParserTask parserTask = new ParserTask();
	    	try {
				String artist=title.getArtist();
				String songTitle=title.getTitle();
				System.out.println("Exectuing PostExecute " + songTitle);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

	        // Start parsing the Google places in JSON format
	        // Invokes the "doInBackground()" method of the class ParseTask
	       // parserTask.execute(result);
	    }


}
