package com.rdio.android.api.example;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.Prediction;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.devsmart.android.ui.HorizontalListView;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.rdio.android.api.Rdio;
import com.rdio.android.api.example.utils.ImageLoader;
import com.rdio.android.api.example.utils.Utils;

public class MusicActivty extends Activity implements GestureOverlayView.OnGesturePerformedListener {

	private static final String TAG = "MusicActivty";

	private ArrayList<Track> trackList;

	private ArrayList<String> artistList;
	private ArrayList<Album> albumList;
	private ArrayList<Album> playList;

	public static Rdio rdio;

	// TODO CHANGE THIS TO YOUR APPLICATION KEY AND SECRET
	private static final String appKey = "s3a6g4ptu9rsv32rxy6kzg23";
	private static final String appSecret = "NHQsKjucag";

	private static String accessToken = null;
	private static String accessTokenSecret = null;

	private static final String PREF_ACCESSTOKEN = "prefs.accesstoken";
	private static final String PREF_ACCESSTOKENSECRET = "prefs.accesstokensecret";

	public static String collectionKey = null;

	private GestureLibrary mLibrary;

	private EditText mGestureEditText = null;
	private TextView mGestureTextView = null;
	private TextView mCategoryType = null;
	private TextView mInfoText = null;
	private Button mClearGestureButton = null;

	private String mGestureText = ""; 

	private final Uri EXTERNAL_MEDIA_URI = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;
	private final String PLAYLIST_URI = "content://com.google.android.music.MusicContent/playlists";


	private DialogFragment getUserDialog;
	private DialogFragment getCollectionDialog;
	private DialogFragment getHeavyRotationDialog;

	private HorizontalListView mArtistHorizontalListView;
	private HorizontalListView mAlbumHorizontalListView;
	private HorizontalListView mPlaylistHorizontalListView;

	GestureOverlayView gestures;

	Timer timer;

	private boolean isGestureSearchEnabled = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.rdio_main);

		albumList = new ArrayList<Album>();
		trackList = new ArrayList<Track>();
		artistList = new ArrayList<String>();
		playList = new ArrayList<Album>();

		mGestureTextView = (TextView)findViewById(R.id.gesture_text_view);
		mGestureEditText = (EditText)findViewById(R.id.gesture_text);

		mPlaylistHorizontalListView = (HorizontalListView) findViewById(R.id.playlist_hs_view);

		mArtistHorizontalListView = (HorizontalListView) findViewById(R.id.searchList1);

		mAlbumHorizontalListView = (HorizontalListView) findViewById(R.id.searchList2);

		mInfoText = (TextView)findViewById(R.id.Info_text);

		mCategoryType = (TextView)findViewById(R.id.search_gesture);
		mCategoryType.setText("Search");


		mLibrary = GestureLibraries.fromRawResource(this, R.raw.gestures);// fromFile(loc);
		if (!mLibrary.load()) {
			finish();
		}

		gestures = (GestureOverlayView) findViewById(R.id.gestures);
		gestures.setGestureColor(Color.YELLOW);
		gestures.addOnGesturePerformedListener(MusicActivty.this);
		mCategoryType.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				gestures.addOnGesturePerformedListener(MusicActivty.this);


			}
		});

		findViewById(R.id.clearButton).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				dismissGetCollectionDialog();
				mGestureTextView.setText("");
				mGestureEditText.setText("");
				mGestureText = "";

			}
		});


		findViewById(R.id.search_button).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if(!isGestureSearchEnabled)
				{
					setGestureItems();	
				}
				else
				{

					removeGestureItems();
				}

			}
		});

		mGestureTextView.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction() == MotionEvent.ACTION_UP)
				{

					if(mGestureTextView.getText().length() > 0)
					{
						mGestureText = mGestureTextView.getText().toString().substring(0, (mGestureTextView.getText().toString().length()-1));
						mGestureTextView.setText(mGestureText);
						mGestureEditText.setText(mGestureText);
						Toast.makeText(
								MusicActivty.this,
								"Del Char",
								Toast.LENGTH_LONG).show();
					}
				}
				return true;
			}
		});

		mGestureEditText.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void afterTextChanged(Editable s) {
				mGestureTextView.setText(s.toString());
				if(!s.toString().equalsIgnoreCase(""))
				{
					if((s.length() >=  3))
					{

						showGetCollectionDialog("Getting Artist..");
						getArtistsFromMedia(s.toString());
						mGestureEditText.setSelection(mGestureEditText.length());
					}
				}else
				{
					hideEditTextkeyboard();
					mPlaylistHorizontalListView.setVisibility(View.GONE);	
					mArtistHorizontalListView.setVisibility(View.GONE);
					mAlbumHorizontalListView.setVisibility(View.GONE);

					//					mCategoryType.setVisibility(View.GONE);
					mInfoText.setVisibility(View.VISIBLE);
					gestures.setVisibility(View.VISIBLE);

				}

			}
		});

		mGestureEditText.setOnKeyListener(new OnKeyListener() {                 
			@Override
			public boolean onKey(View arg0, int arg1, KeyEvent arg2) {
				if(arg1 == KeyEvent.KEYCODE_DEL){  
					mGestureText = mGestureEditText.getText().toString();
				}
				return false;
			}
		});
	}

	private void getArtistsFromMedia(String artistKey)
	{
		if(artistList != null && artistList.size() > 0)
		{
			artistList.clear();
			artistList = new ArrayList<String>();
		}

		String[] columns = { android.provider.MediaStore.Audio.Albums._ID,
				android.provider.MediaStore.Audio.Media.ALBUM,android.provider.MediaStore.Audio.Media.ARTIST };
		
		String[] proj = { MediaStore.Audio.AudioColumns.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media._ID };
		Uri gMusicUri = Uri.parse("content://com.google.android.music.MusicContent/audio");

		String where1 = android.provider.MediaStore.Audio.Media.ARTIST + " LIKE ?";

		String whereVal1[] = { artistKey+"%" };

		Cursor cursor = getApplicationContext().getContentResolver().query(
				/*MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI*/gMusicUri, proj, where1,
				whereVal1, null);

		if (cursor.moveToFirst()) {
			do {
				String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
				//Long albumId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));
				Log.v(TAG," MEDIA MUSIC ARTIST "+ artist);
				Album album = new Album("", artist, "", "", "", null);
				if(artistList == null)artistList = new ArrayList<String>();
				{
					if(!artistList.contains(artist))
						artistList.add(artist);
				}
			} while (cursor.moveToNext());
		}

		cursor.close();

		gestures.setVisibility(View.GONE);
		mPlaylistHorizontalListView.setVisibility(View.VISIBLE);
		mArtistHorizontalListView.setVisibility(View.VISIBLE);	
		mArtistHorizontalListView.setAdapter(mArtistAdapter);
		mArtistAdapter.notifyDataSetChanged();
		mAlbumHorizontalListView.setVisibility(View.GONE);
		if(albumList != null && albumList.size() > 0)
		{
			albumList.clear();
		}
		mAlbumAdapter.notifyDataSetChanged();
		mArtistHorizontalListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1,
					int pos, long arg3) {
				showGetCollectionDialog("Getting Albums");
				getAlbumsOfArtist(artistList.get(pos).toString());
			}
		});


		dismissGetCollectionDialog();

		getPlaylists();
	}
	

	private void getPlaylists() {
		
		if(playList != null && playList.size() > 0)
		{
			playList.clear();
		}
		
		Uri gMusicUri = Uri.parse(PLAYLIST_URI);
		Cursor cursor = getContentResolver().query(gMusicUri, 
				new String[] {"_id", "playlist_name","playlist_id","playlist_art_url" }, 
				null, null, null);
		if (cursor == null) {
			// query failed, handle error.
			Log.e(TAG,"onCreate(): cursor is null, query failed");
		}else
		{
			Log.d(TAG,"onCreate(): cursor is not null, query success");

			while (cursor.moveToNext()) {
				long id = cursor.getLong(cursor.getColumnIndex("_id"));
				long PlayList_ID = cursor.getLong(cursor.getColumnIndex("playlist_id"));
				String PlayListName = cursor.getString(cursor.getColumnIndex("playlist_name"));
				String PlayList_url = cursor.getString(cursor.getColumnIndex("playlist_art_url"));
				Log.d(TAG, "Playlist PlayList_ID  "+PlayList_ID+"... Name "+PlayListName+" id= "+id+" PlayList_url "+PlayList_url);
				Album album = new Album("", PlayListName, "", "", "", trackList, PlayList_ID);
				if(playList == null)playList = new ArrayList<Album>();
				playList.add(album);
			}
			cursor.close();
		}
		
		mPlaylistHorizontalListView.setVisibility(View.VISIBLE);
		mPlaylistHorizontalListView.setAdapter(mPlaylistAdapter);
		mPlaylistAdapter.notifyDataSetChanged();
		mPlaylistHorizontalListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1,
					int pos, long arg3) {
				playTrackFromPlaylist1(playList.get(pos).bitmapId);
			}
		});
		
	}

	public void playTrackFromPlaylist1(final long playListID) {
		if(trackList != null && trackList.size() > 0)
		{
			trackList.clear();
		}
		String[] proj2 = { "SourceId",MediaStore.Audio.Playlists.Members.TITLE,
				MediaStore.Audio.Playlists.Members.AUDIO_ID,MediaStore.Audio.Media.DURATION };
		String playListRef = PLAYLIST_URI+"/"
				+ playListID + "/members";
		Uri songUri = Uri.parse(playListRef);
		Cursor songCursor = getContentResolver().query(songUri, proj2, null,
				null, null);
		if(songCursor != null)
		{
			long audioId = -1;
			while (songCursor.moveToNext()) {
				audioId = songCursor.getLong(songCursor.getColumnIndex("SourceId"));
				long id = songCursor.getLong(songCursor.getColumnIndex(MediaStore.Audio.Playlists.Members.AUDIO_ID));
				String PlayListName = songCursor.getString(songCursor.getColumnIndex(MediaStore.Audio.Playlists.Members.TITLE));
				String duration = songCursor.getString(songCursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
				Log.d(TAG, "Playlist PlayList_ID  "+audioId+"... Name "+PlayListName+" id= "+id);
				Track track = new Track(""+audioId, PlayListName, null, PlayListName, null, duration,audioId);
				trackList.add(track);
			}
		}
		songCursor.close();
		Intent intent = new Intent(MusicActivty.this, NativePlayerActivity.class);
		intent.putExtra("TrackList", trackList);
		intent.putExtra("isNativeLibrary", true);
		startActivity(intent);
		
	}



	private void getAlbumsOfArtist(String key)
	{
		if(albumList != null && albumList.size() > 0)
		{
			albumList.clear();
		}

		String[] columns = { android.provider.MediaStore.Audio.Albums._ID,
				android.provider.MediaStore.Audio.Media.ALBUM,android.provider.MediaStore.Audio.Media.ARTIST };
		
		String[] proj = { android.provider.MediaStore.Audio.Albums._ID,android.provider.MediaStore.Audio.Media.ALBUM_ID,
				android.provider.MediaStore.Audio.Media.ALBUM,android.provider.MediaStore.Audio.Media.ARTIST };
		Uri gMusicUri = Uri.parse("content://com.google.android.music.MusicContent/audio");

		String where1 = android.provider.MediaStore.Audio.Media.ARTIST + " LIKE ?";

		String whereVal1[] = { "%"+key+"%"  };

		Log.v(TAG," MEDIA MUSIC ALBUM KEY "+key);

		Cursor cursor = getApplicationContext().getContentResolver().query(
				/*MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI*/gMusicUri, proj, where1,
				whereVal1, null);

		if (cursor.moveToFirst()) {
			do {
				final long albumId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
				final long album_Id = cursor.getLong(cursor.getColumnIndex(android.provider.MediaStore.Audio.Albums._ID));
				String albumName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
				String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
				long albId = getAlbumId(albumName);
				Log.v(TAG," MEDIA MUSIC ALBUM "+albumName+" artist "+artist+" albumId "+albumId+"  album_Id "+albId);
				Album album = new Album("", albumName, "", "", "", null,albId);
				if(albumList == null)albumList = new ArrayList<Album>();
				albumList.add(album);
			} while (cursor.moveToNext());
		}

		cursor.close();

		mAlbumHorizontalListView.setVisibility(View.VISIBLE);
		mAlbumHorizontalListView.setAdapter(mAlbumAdapter);
		mAlbumAdapter.notifyDataSetChanged();
		mAlbumHorizontalListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1,
					int pos, long arg3) {
				fetchTracksForAlbum(albumList.get(pos).artistName,albumList.get(pos).bitmapId);
			}
		});
		dismissGetCollectionDialog();
		//		fetchTracksForAlbum("Rab Ne Bana Di Jodi");


	}


	private long getAlbumId(String albumName) {
		long id = -1;
		
		final Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        final String[] cursor_cols = { MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION };
        
        String where = android.provider.MediaStore.Audio.Media.ALBUM + "=?";

		String whereVal[] = { albumName };
		
//		final String where = MediaStore.Audio.Media.IS_MUSIC + "=1";
        final Cursor cursor = getContentResolver().query(uri,
                cursor_cols, where, whereVal, null);
        
        cursor.moveToFirst();
        	id = cursor.getLong(cursor
                    .getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));
        cursor.close();
		
		return id;
	}

	private void fetchTracksForAlbum(String album,long bitmapId) {
		if(trackList != null && trackList.size() > 0)
		{
			trackList.clear();
		}

		// I want to list down song in album Rolling Papers (Deluxe Version)

		final Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        final String[] cursor_cols = { MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION };

		String where = android.provider.MediaStore.Audio.Media.ALBUM + "=?";

		String whereVal[] = { album };
		
//		final String where = MediaStore.Audio.Media.IS_MUSIC + "=1";
        final Cursor cursor = getContentResolver().query(uri,
                cursor_cols, where, whereVal, null);

		String orderBy = android.provider.MediaStore.Audio.Media.TITLE;

		/*Cursor cursor = getApplicationContext().getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
				column, where, whereVal, orderBy);*/
		long audioId = -1;
	/*	if (cursor.moveToFirst()) {
			do {
				Log.v(TAG,"MEDIA MUSIC "+album+" "+
						cursor.getString(cursor
								.getColumnIndex(MediaStore.Audio.Media.TITLE)));
				audioId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
				String name = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
				String duration = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
				Track track = new Track(""+audioId, name, null, album, null, duration,bitmapId);
				trackList.add(track);
			} while (cursor.moveToNext());
		}*/
		while (cursor.moveToNext()) {
            String artist = cursor.getString(cursor
                    .getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
            String albumName = cursor.getString(cursor
                    .getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
            String track = cursor.getString(cursor
                    .getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
            String data = cursor.getString(cursor
                    .getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
            Long trackId = cursor.getLong(cursor
                    .getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
            Long albumId = cursor.getLong(cursor
                    .getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));

            int duration = cursor.getInt(cursor
                    .getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
            Track tracks = new Track(""+trackId, track, null, albumName, null, ""+duration,albumId);
			trackList.add(tracks);
			Log.v(TAG," MEDIA MUSIC ALBUM TARCKS "+albumName+" artist "+artist+" track "+track+"  album_Id "+albumId);
			
/*            Uri sArtworkUri = Uri
                    .parse("content://media/external/audio/albumart");
            Uri albumArtUri = ContentUris.withAppendedId(sArtworkUri, albumId);

            Log.d(TAG," Album Art "+albumArtUri.toString());
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(
                        getContentResolver(), albumArtUri);
                bitmap = Bitmap.createScaledBitmap(bitmap, 30, 30, true);

            } catch (FileNotFoundException exception) {
                exception.printStackTrace();
                bitmap = BitmapFactory.decodeResource(getResources(),
                        R.drawable.icon);
            } catch (IOException e) {

                e.printStackTrace();
            }*/
		}
		
		cursor.close();

		Intent intent = new Intent(MusicActivty.this, NativePlayerActivity.class);
		intent.putExtra("TrackList", trackList);
		intent.putExtra("isNativeLibrary", true);
		startActivity(intent);

		/*	MediaPlayer mpObject = new MediaPlayer();
		try {
			if (audioId > 0) {
				Uri contentUri = ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Long.valueOf(audioId));
				mpObject.setAudioStreamType(AudioManager.STREAM_MUSIC);
				mpObject.setDataSource(this, contentUri);
				mpObject.prepare();
				mpObject.start();

			}
		} catch (Exception e) {
			e.printStackTrace();
		}*/

		dismissGetCollectionDialog();

	}

	private void setGestureItems()
	{
		((Button)findViewById(R.id.search_button)).setText("Back");
		isGestureSearchEnabled = true;
		gestures.setVisibility(View.VISIBLE);
		mInfoText.setVisibility(View.VISIBLE);
		mPlaylistHorizontalListView.setVisibility(View.GONE);
		mArtistHorizontalListView.setVisibility(View.GONE);
		mAlbumHorizontalListView.setVisibility(View.GONE);
	}

	private void removeGestureItems()
	{
		((Button)findViewById(R.id.search_button)).setText("Search");
		isGestureSearchEnabled = false;
		gestures.setVisibility(View.GONE);
		mInfoText.setVisibility(View.GONE);
		mPlaylistHorizontalListView.setVisibility(View.VISIBLE);
		mArtistHorizontalListView.setVisibility(View.VISIBLE);
		mAlbumHorizontalListView.setVisibility(View.VISIBLE);
	}



	public boolean isNetworkOnline() {
		boolean status=false;
		try{
			ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo netInfo = cm.getNetworkInfo(0);
			if (netInfo != null && netInfo.getState()==NetworkInfo.State.CONNECTED) {
				status= true;
			}else {
				netInfo = cm.getNetworkInfo(1);
				if(netInfo!=null && netInfo.getState()==NetworkInfo.State.CONNECTED)
					status= true;
			}
		}catch(Exception e){
			e.printStackTrace();  
			return false;
		}
		return status;

	}

	private void hideEditTextkeyboard()
	{
		InputMethodManager imm = (InputMethodManager)getSystemService(
				Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(mGestureTextView.getWindowToken(), 0);
	}

	@Override
	public void onGesturePerformed(GestureOverlayView arg0, Gesture gesture) {
		ArrayList<Prediction> predictions = mLibrary.recognize(gesture);
		Log.d(TAG, "Prediction Score is "+predictions.get(0).score);
		if (predictions.size() > 0 && predictions.get(0).score > 1.5 && predictions.get(0).score < 13.0) {
			String result = predictions.get(0).name;
			mInfoText.setVisibility(View.GONE);
			if(albumList != null && albumList.size() > 0)
			{
				albumList.clear();
			}

			if(artistList != null && artistList.size() > 0)
			{
				artistList.clear();
			}

			mAlbumHorizontalListView.setVisibility(View.GONE);


			Toast.makeText(this, "Letter " + result, Toast.LENGTH_SHORT).show();
			mGestureTextView.setText(mGestureText+""+result);
			mGestureEditText.setText(mGestureText+""+result);
			mGestureText = mGestureText+result;
			//			showGetCollectionDialog();
			/*	List<NameValuePair> args = new LinkedList<NameValuePair>();
			args.add(new BasicNameValuePair("query",mGestureTextView.getText().toString()));
			args.add(new BasicNameValuePair("types","Track"));
			args.add(new BasicNameValuePair("start","0"));
			args.add(new BasicNameValuePair("count","15"));
			args.add(new BasicNameValuePair("keys", collectionKey));//p3554448,c18977962
//			args.add(new BasicNameValuePair("extras", "tracks,Track.playCount"));
			search(args);*/

			//			loadListData(result);
		}
	}

	/*************************
	 * Dialog helpers
	 *************************/
	private void showGetUserDialog() {
		if (getUserDialog == null) {
			getUserDialog = new RdioProgress();
		}

		if (getUserDialog.isAdded()) {
			return;
		}

		Bundle args = new Bundle();
		args.putString("message",
				getResources().getString(R.string.getting_user));

		getUserDialog.setArguments(args);
		getUserDialog.show(getFragmentManager(), "getUserDialog");
	}

	private void dismissGetUserDialog() {
		if (getUserDialog != null) {
			getUserDialog.dismiss();
		}
	}

	private void showGetCollectionDialog(String message) {
		if (getCollectionDialog == null) {
			getCollectionDialog = new RdioProgress();
		}

		if (getCollectionDialog.isAdded()) {
			return;
		}

		Bundle args = new Bundle();
		args.putString("message",message);

		getCollectionDialog.setArguments(args);
		getCollectionDialog.show(getFragmentManager(), "getCollectionDialog");
	}

	private void showGetCollectionDialog() {
		if (getCollectionDialog == null) {
			getCollectionDialog = new RdioProgress();
		}

		if (getCollectionDialog.isAdded()) {
			return;
		}

		Bundle args = new Bundle();
		args.putString("message",
				getResources().getString(R.string.getting_collection));

		getCollectionDialog.setArguments(args);
		getCollectionDialog.show(getFragmentManager(), "getCollectionDialog");
	}

	private void dismissGetCollectionDialog() {
		if (getCollectionDialog != null) {
			getCollectionDialog.dismiss();
		}
	}

	private void showGetHeavyRotationDialog() {
		if (getHeavyRotationDialog == null) {
			getHeavyRotationDialog = new RdioProgress();
		}

		if (getHeavyRotationDialog.isAdded()) {
			return;
		}

		Bundle args = new Bundle();
		args.putString("message",
				getResources().getString(R.string.getting_heavy_rotation));

		getHeavyRotationDialog.setArguments(args);
		getHeavyRotationDialog.show(getFragmentManager(),
				"getHeavyRotationDialog");
	}

	private void dismissGetHeavyRotationDialog() {
		if (getHeavyRotationDialog != null) {
			getHeavyRotationDialog.dismiss();
		}
	}


	private BaseAdapter mArtistAdapter = new BaseAdapter() {


		@Override
		public int getCount() {
			if(artistList != null )
				return artistList.size();
			else return 0;
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View retval = LayoutInflater.from(parent.getContext()).inflate(R.layout.viewitem, null);
			// Loader image - will be shown before loading image
			int loader = R.drawable.icon;

			// Imageview to show
			ImageView image = (ImageView) retval.findViewById(R.id.image);

			// Image url
			String image_url = null;

			// ImageLoader class instance
			ImageLoader imgLoader = new ImageLoader(getApplicationContext());

			// whenever you want to load an image from url
			// call DisplayImage function
			// url - image url to load
			// loader - loader image, will be displayed before getting image
			// image - ImageView
			//			imgLoader.DisplayImage(image_url, loader, image);
			TextView title = (TextView) retval.findViewById(R.id.title);
			title.setText(artistList.get(position).toString());
			Log.d(TAG, " HSCRL VIEW :: Artist Name is "+artistList.get(position).toString());
			return retval;
		}

	};

	private BaseAdapter mAlbumAdapter = new BaseAdapter() {


		@Override
		public int getCount() {
			if(albumList != null )
				return albumList.size();
			else return 0;
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View retval = LayoutInflater.from(parent.getContext()).inflate(R.layout.viewitem, null);
			// Loader image - will be shown before loading image
			int loader = R.drawable.icon;

			// Imageview to show
			ImageView image = (ImageView)retval.findViewById(R.id.image);
			image.setImageBitmap(Utils.getAlbumArt(MusicActivty.this, albumList.get(position).bitmapId,true));

			// Image url
			String image_url = albumList.get(position).albumArt;

			// ImageLoader class instance
			ImageLoader imgLoader = new ImageLoader(getApplicationContext());

			// whenever you want to load an image from url
			// call DisplayImage function
			// url - image url to load
			// loader - loader image, will be displayed before getting image
			// image - ImageView
			//			imgLoader.DisplayImage(image_url, loader, image);
			TextView title = (TextView) retval.findViewById(R.id.title);
			title.setText(albumList.get(position).artistName);
			Log.d(TAG, " HSCRL VIEW :: Album Name is "+albumList.get(position).albumName);

			return retval;
		}

	};
	private BaseAdapter mPlaylistAdapter = new BaseAdapter() {


		@Override
		public int getCount() {
			if(playList != null )
				return playList.size();
			else return 0;
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View retval = LayoutInflater.from(parent.getContext()).inflate(R.layout.viewitem, null);
			// Loader image - will be shown before loading image
			int loader = R.drawable.icon;

			// Imageview to show
			ImageView image = (ImageView)retval.findViewById(R.id.image);
			image.setImageBitmap(Utils.getAlbumArt(MusicActivty.this, playList.get(position).bitmapId,true));

			// Image url
			String image_url = playList.get(position).albumArt;

			// ImageLoader class instance
			ImageLoader imgLoader = new ImageLoader(getApplicationContext());

			// whenever you want to load an image from url
			// call DisplayImage function
			// url - image url to load
			// loader - loader image, will be displayed before getting image
			// image - ImageView
			//			imgLoader.DisplayImage(image_url, loader, image);
			TextView title = (TextView) retval.findViewById(R.id.title);
			title.setText(playList.get(position).artistName);
			Log.d(TAG, " HSCRL VIEW :: Playlist Name is "+playList.get(position).albumName);

			return retval;
		}

	};
	
	  private static int sArtId = -2;
	    private static byte [] mCachedArt;
	    private static Bitmap mCachedBit = null;
	// get album art for specified file
    private static final String sExternalMediaUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString();
    private Bitmap getArtworkFromFile(Context context, Uri uri, int albumid) {
        Bitmap bm = null;
        byte [] art = null;
        String path = null;

/*        if (sArtId == albumid) {
            //Log.i("@@@@@@ ", "reusing cached data", new Exception());
            if (mCachedBit != null) {
                return mCachedBit;
            }
            art = mCachedArt;
        } else {
            // try reading embedded artwork
            if (uri == null) {
                try {
                    int curalbum = sService.getAlbumId();
                    if (curalbum == albumid || albumid < 0) {
                        path = sService.getPath();
                        if (path != null) {
                            uri = Uri.parse(path);
                        }
                    }
                } catch (RuntimeException ex) {
                }
            }
            if (uri == null) {
                if (albumid >= 0) {
                    Cursor c = query(context,MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            new String[] { MediaStore.Audio.Media._ID, MediaStore.Audio.Media.ALBUM },
                            MediaStore.Audio.Media.ALBUM_ID + "=?", new String [] {String.valueOf(albumid)},
                            null);
                    if (c != null) {
                        c.moveToFirst();
                        if (!c.isAfterLast()) {
                            int trackid = c.getInt(0);
                            uri = ContentUris.withAppendedId(
                                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, trackid);
                        }
                        if (c.getString(1).equals(MediaFile.UNKNOWN_STRING)) {
                            albumid = -1;
                        }
                        c.close();
                    }
                }
            }
            if (uri != null) {
                MediaScanner scanner = new MediaScanner(context);
                ParcelFileDescriptor pfd = null;
                try {
                    pfd = context.getContentResolver().openFileDescriptor(uri, "r");
                    if (pfd != null) {
                        FileDescriptor fd = pfd.getFileDescriptor();
                        art = scanner.extractAlbumArt(fd);
                    }
                } catch (IOException ex) {
                } catch (SecurityException ex) {
                } finally {
                    try {
                        if (pfd != null) {
                            pfd.close();
                        }
                    } catch (IOException ex) {
                    }
                }
            }
        }*/
        // if no embedded art exists, look for AlbumArt.jpg in same directory as the media file
        if (art == null && path != null) {
            if (path.startsWith(sExternalMediaUri)) {
                // get the real path
                Cursor c = MusicActivty.this.getContentResolver().query(Uri.parse(path),
                        new String[] { MediaStore.Audio.Media.DATA},
                        null, null, null);
                if (c != null) {
                    c.moveToFirst();
                    if (!c.isAfterLast()) {
                        path = c.getString(0);
                    }
                    c.close();
                }
            }
            int lastSlash = path.lastIndexOf('/');
            if (lastSlash > 0) {
                String artPath = path.substring(0, lastSlash + 1) + "AlbumArt.jpg";
                File file = new File(artPath);
                if (file.exists()) {
                    art = new byte[(int)file.length()];
                    FileInputStream stream = null;
                    try {
                        stream = new FileInputStream(file);
                        stream.read(art);
                    } catch (IOException ex) {
                        art = null;
                    } finally {
                        try {
                            if (stream != null) {
                                stream.close();
                            }
                        } catch (IOException ex) {
                        }
                    }
                } else {
                    // TODO: try getting album art from the web
                }
            }
        }
        
        if (art != null) {
            try {
                // get the size of the bitmap
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                opts.inSampleSize = 1;
                BitmapFactory.decodeByteArray(art, 0, art.length, opts);
                
                // request a reasonably sized output image
                // TODO: don't hardcode the size
                while (opts.outHeight > 320 || opts.outWidth > 320) {
                    opts.outHeight /= 2;
                    opts.outWidth /= 2;
                    opts.inSampleSize *= 2;
                }
                
                // get the image for real now
                opts.inJustDecodeBounds = false;
                bm = BitmapFactory.decodeByteArray(art, 0, art.length, opts);
                if (albumid != -1) {
                    sArtId = albumid;
                }
                mCachedArt = art;
                mCachedBit = bm;
            } catch (Exception e) {
            }
        }
        return bm;
    }

}
