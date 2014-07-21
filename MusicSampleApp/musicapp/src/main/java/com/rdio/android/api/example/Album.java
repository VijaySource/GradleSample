package com.rdio.android.api.example;

import java.util.ArrayList;

import android.graphics.Bitmap;

public class Album {

	public String key;
	public String artistName;
	public String duration;
	public String albumName;
	public String albumArt;
	public long bitmapId;
	public ArrayList<Track> trackList;

	public Album(String k, String name, String duration, String album,
			String uri,ArrayList<Track> trackList) {
		key = k;
		artistName = name;
		this.duration = duration;
		albumName = album;
		albumArt = uri;
		if(trackList != null)
		this.trackList = trackList;
	}
	
	public Album(String k, String name, String duration, String album,
			String uri,ArrayList<Track> trackList,long bitmapId) {
		key = k;
		artistName = name;
		this.duration = duration;
		albumName = album;
		albumArt = uri;
		if(trackList != null)
		this.trackList = trackList;
		this.bitmapId = bitmapId;
	}


}
