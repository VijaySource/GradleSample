package com.rdio.android.api.example;

import java.io.Serializable;

import android.graphics.Bitmap;

// Our model for the metadata for a track that we care about
public class Track implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public String key;
	public String trackName;
	public String artistName;
	public String albumName;
	public String albumArt;
	public String duration;
	public long albumId;

	public Track(String k, String name, String artist, String album,
			String uri,String duration) {
		key = k;
		trackName = name;
		artistName = artist;
		albumName = album;
		albumArt = uri;
		this.duration = duration;
	}
	
	public Track(String k, String name, String artist, String album,
			String uri,String duration, long id) {
		key = k;
		trackName = name;
		artistName = artist;
		albumName = album;
		albumArt = uri;
		this.duration = duration;
		albumId = id;
	}
}