package com.rdio.android.api.example;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.Prediction;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.devsmart.android.ui.HorizontalListView;
import com.rdio.android.api.OAuth1WebViewActivity;
import com.rdio.android.api.Rdio;
import com.rdio.android.api.RdioApiCallback;
import com.rdio.android.api.RdioListener;
import com.rdio.android.api.example.utils.ImageLoader;

public class RdioActivity extends Activity implements RdioListener,
GestureOverlayView.OnGesturePerformedListener {

	private static final String TAG = "RdioActivity";

	private ArrayList<Track> trackList;

	private ArrayList<Album> artistList;
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


	private final String CURRENT_USER_API = "currentUser";
	private final String SEARCH_SUGGESTIONS_ARTIST_API = "searchSuggestions";
	private final String SEARCH_ARTIST_API = "search";
	private final String GET_ALBUMS_FOR_ARTIST_API = "getAlbumsForArtist";
	private final String GET_HEAVY_ROTATION_API = "getHeavyRotation";
	private final String GET_PLAYLISTS_API = "getPlaylists";
	private final String GET_USER_PLAYLISTS_API = "getUserPlaylists";

	private DialogFragment getUserDialog;
	private DialogFragment getCollectionDialog;
	private DialogFragment getHeavyRotationDialog;

	private HorizontalListView mArtistHorizontalListView;
	private HorizontalListView mAlbumHorizontalListView;
	private HorizontalListView mPlaylistHorizontalListView;

	GestureOverlayView gestures;

	Timer timer;

	private boolean isGestureSearchEnabled = false;
	//	 MyTimerTask myTimerTask;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.rdio_main);

		albumList = new ArrayList<Album>();
		trackList = new ArrayList<Track>();
		artistList = new ArrayList<Album>();
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
		gestures.addOnGesturePerformedListener(RdioActivity.this);
		mCategoryType.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				gestures.addOnGesturePerformedListener(RdioActivity.this);


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

		// Initialize our Rdio object. If we have cached access credentials,
		// then use them - otherwise
		// Initialize w/ null values and the user will be prompted (if the Rdio
		// app is installed), or
		// we'll fallback to 30s samples.

		SharedPreferences settings = getPreferences(MODE_PRIVATE);
		accessToken = settings.getString(PREF_ACCESSTOKEN, null);
		accessTokenSecret = settings
				.getString(PREF_ACCESSTOKENSECRET, null);
		if (rdio == null) {

			rdio = new Rdio(appKey, appSecret, accessToken, accessTokenSecret,
					this, this);

			if (accessToken == null || accessTokenSecret == null) {
				// If either one is null, reset both of them
				accessToken = accessTokenSecret = null;
				Intent myIntent = new Intent(this,
						OAuth1WebViewActivity.class);
				myIntent.putExtra(OAuth1WebViewActivity.EXTRA_CONSUMER_KEY,
						appKey);
				myIntent.putExtra(OAuth1WebViewActivity.EXTRA_CONSUMER_SECRET,
						appSecret);
				this.startActivityForResult(myIntent, 1);

			} else {
				Log.d(TAG, "Found cached credentials:");
				Log.d(TAG, "Access token: " + accessToken);
				Log.d(TAG, "Access token secret: " + accessTokenSecret);
				rdio.prepareForPlayback();
			}

		}else
		{

			rdio = new Rdio(appKey, appSecret, accessToken, accessTokenSecret,
					this, this);
			if (accessToken != null && accessTokenSecret != null) {
				if(isNetworkOnline())
				{
					getUserKey();
				}
				else
					Toast.makeText(RdioActivity.this, "No Internet Connection!", Toast.LENGTH_SHORT).show();
			} else {
				if(isNetworkOnline())
				{
					doSomethingWithoutApp();
				}
				else
					Toast.makeText(RdioActivity.this, "No Internet Connection!", Toast.LENGTH_SHORT).show();
			}
		}

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
								RdioActivity.this,
								"Del Char",
								Toast.LENGTH_LONG).show();
					}
					/*					if(mGestureText.toString().equalsIgnoreCase(""))
					{

						hideEditTextkeyboard();
						listContainer.setVisibility(View.GONE);
						mCategoryType.setVisibility(View.GONE);
						mInfoText.setVisibility(View.VISIBLE);
						if(mArtistNameList != null && mArtistNameList.size() > 0)
						{
							mArtistNameList.clear();
						}

					}*/

				}
				return true;
			}
		});


		/*	mGestureTextView.setOnTouchListener(new OnSwipeTouchListener(this)
		{
			@Override
			public void onSwipeLeft() {
				Toast.makeText(
						RdioActivity.this,
						"Swipe Left || Del Char",
						Toast.LENGTH_LONG).show();
				mGestureText = mGestureTextView.getText().toString().substring(0, (mGestureTextView.getText().toString().length()-1));
				mGestureTextView.setText(mGestureText);
			}
		});*/


		mGestureEditText.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				//            	mGestureText = s.toString();
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

						if(isNetworkOnline())
						{
							showGetCollectionDialog("Getting Artist..");
							List<NameValuePair> args = new LinkedList<NameValuePair>();
							args.add(new BasicNameValuePair("query",s.toString()));
							args.add(new BasicNameValuePair("types","Artist"));
							args.add(new BasicNameValuePair("start","0"));
							args.add(new BasicNameValuePair("count","50"));
							args.add(new BasicNameValuePair("keys", collectionKey));//p3554448,c18977962
							search(args);
						}else
						{
							Toast.makeText(RdioActivity.this, "No Internet Connection!", Toast.LENGTH_SHORT).show();
						}
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


	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if(RdioActivity.rdio != null)
		{
			// Make sure to call the cleanup method on the API object
			RdioActivity.rdio.cleanup();
		}
		/*// If we allocated a player, then cleanup after it
		if(NativePlayerActivity.player != null)
		{

			PlayerFragment.player.reset();
			PlayerFragment.player.release();
			PlayerFragment.player = null;
		}*/
	}


	private void hideEditTextkeyboard()
	{
		InputMethodManager imm = (InputMethodManager)getSystemService(
				Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(mGestureTextView.getWindowToken(), 0);
	}

	private void getUserKey() {
		if (accessToken == null || accessTokenSecret == null) {
			doSomethingWithoutApp();
			return;
		}

		Log.i(TAG, "Getting current user");
		showGetUserDialog();

		// Get the current user so we can find out their user ID and get their
		// collection key
		List<NameValuePair> args = new LinkedList<NameValuePair>();
		args.add(new BasicNameValuePair("extras", "followingCount,followerCount,username,displayName,subscriptionType,trialEndDate,actualSubscriptionType"));

		rdio.apiCall(CURRENT_USER_API, args, new RdioApiCallback() {

			@Override
			public void onApiSuccess(JSONObject result) {
				dismissGetUserDialog();
				try {
					result = result.getJSONObject("result");
					Log.d(TAG, "currentUser responseJson is " + result);
					Log.i(TAG, result.toString(2));

					// c<userid> is the 'collection radio source' key
					collectionKey = result.getString("key").replace('s', 'c');
					
					showGetCollectionDialog("Getting Playlists..");
					fetchPlaylistForCurrentUser();

					Toast.makeText(RdioActivity.this, "User Information Successful, You can search for artists!",  Toast.LENGTH_LONG).show();

					// LoadMoreTracks();
				} catch (Exception e) {
					Log.e(TAG, "Failed to handle JSONObject: ", e);
				}
			}

			@Override
			public void onApiFailure(String methodName, Exception e) {
				dismissGetUserDialog();
				Log.e(TAG, "getCurrentUser failed. ", e);
				//				if (e instanceof RdioAuthorisationException)
				{
					doSomethingWithoutApp();
				}
			}
		});


	}
	
	private void fetchPlaylistForCurrentUser()
	{
		List<NameValuePair> args = new LinkedList<NameValuePair>();
		args.add(new BasicNameValuePair("ordered_list","true"));
		args.add(new BasicNameValuePair("extras","tracks"));
		args.add(new BasicNameValuePair("user", collectionKey.replace('c', 's')));//p3554448,c18977962
		Log.d(TAG, "Playlist User Key is "+collectionKey);
		rdio.apiCall(GET_PLAYLISTS_API, args, new RdioApiCallback() {
			
			@Override
			public void onApiSuccess(JSONObject result) {
				
				if(playList != null && playList.size() > 0)
				{
					playList.clear();
				}
				
				try
				{
					JSONArray albums = result.getJSONArray("result");
					for (int i = 0; i < albums.length(); i++) {
						JSONObject albumsObject = albums.getJSONObject(i);
						String key = albumsObject.getString("key");
						String name = albumsObject.getString("name");
						String albumArt = albumsObject.getString("icon");
						JSONArray trackArray = albumsObject.getJSONArray("tracks");
						Log.d(TAG,
								"Found search api playlist names : " + key + " => "
										+ name);
						ArrayList<Track> trackList = new ArrayList<Track>();
						for(int j=0; j< trackArray.length();j++)
						{
							JSONObject tracksObject = trackArray.getJSONObject(j);
							String trackKey = tracksObject.getString("key");
							String trackName = tracksObject.getString("name");
							String trackIcon = tracksObject.getString("icon");
							String trackArtist = tracksObject.getString("artist");
							String duration = tracksObject.getString("duration");
							trackList.add(new Track(trackKey, trackName, trackArtist, trackIcon, trackIcon,duration));
							Log.d(TAG,
									"Found search api playlist tracks : " + key + " => "
											+ tracksObject.getString("name"));
						}

						playList.add(new Album(key, name, "", name,
								albumArt,trackList));
//						mPlaylistHorizontalListView.setVisibility(View.VISIBLE);
						mPlaylistHorizontalListView.setAdapter(mPlaylistAdapter);
						mPlaylistAdapter.notifyDataSetChanged();
						mPlaylistHorizontalListView.setOnItemClickListener(new OnItemClickListener() {

							@Override
							public void onItemClick(AdapterView<?> arg0, View arg1,
									int arg2, long arg3) {
								Toast.makeText(RdioActivity.this, "Clicked Playlist name is  " + playList.get(arg2).albumName, Toast.LENGTH_SHORT).show();
								Intent intent = new Intent(RdioActivity.this, NativePlayerActivity.class);
								intent.putExtra("TrackList", playList.get(arg2).trackList);
								startActivity(intent);
							}
						});
					}
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
				
				dismissGetCollectionDialog();
			}
			
			@Override
			public void onApiFailure(String arg0, Exception arg1) {
				Log.d(TAG, " Error in Playlist api");
			}
		});
		
		
	}
	

	public void search(List<NameValuePair> args)
	{
		rdio.apiCall(SEARCH_SUGGESTIONS_ARTIST_API, args, new RdioApiCallback() {

			@Override
			public void onApiSuccess(JSONObject result) {

				if(albumList != null && albumList.size() > 0)
				{
					albumList.clear();
				}

				if(artistList != null && artistList.size() > 0)
				{
					artistList.clear();
				}

				try
				{
					//					mCategoryType.setVisibility(View.VISIBLE);

					hideEditTextkeyboard();
					String jsonResponse = result.toString();
					Log.d(TAG, " search api Index json is "+jsonResponse);
					//				result = result.getJSONObject("result");
					List<Album> trackKeys = new ArrayList<Album>();
					JSONArray tracks = result.getJSONArray("result");
					for (int i = 0; i < tracks.length(); i++) {
						JSONObject trackObject = tracks.getJSONObject(i);
						String key = trackObject.getString("key");
						String name = trackObject.getString("name");
						String albumArt = trackObject.getString("icon");
						if(name.toString().toLowerCase().startsWith(mGestureTextView.getText().toString().toLowerCase()))
						{
							Log.d(TAG,
									"Found search api artist: " + key + " => "
											+ trackObject.getString("name")+" name "+name);
							artistList.add(new Album(key, name, "", name,
									albumArt,null));

						}
					}

					dismissGetCollectionDialog();
					
					displayListContents();

					/*Intent intent = new Intent(RdioActivity.this, AlbumActivity.class);
					intent.putExtra("UserKey", collectionKey);
					intent.putExtra("ArtistList", artistList);
					startActivity(intent);*/

				}
				catch (Exception e) {
					Log.d(TAG, " Error in search api json parsing");
				}
			}

			@Override
			public void onApiFailure(String arg0, Exception arg1) {
				dismissGetCollectionDialog();
				Log.d(TAG, " Error in search api");
			}
		});
	}

	private void displayListContents() {
		removeGestureItems();
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
				List<NameValuePair> args = new LinkedList<NameValuePair>();
				args.add(new BasicNameValuePair("artist",artistList.get(pos).key));
				args.add(new BasicNameValuePair("extras","tracks"));
				args.add(new BasicNameValuePair("start","0"));
				args.add(new BasicNameValuePair("count","15"));
				args.add(new BasicNameValuePair("keys", collectionKey));//p3554448,c18977962

				RdioActivity.rdio.apiCall(GET_ALBUMS_FOR_ARTIST_API, args, new RdioApiCallback() {

					@Override
					public void onApiSuccess(JSONObject result) {

						if(albumList != null && albumList.size() > 0)
						{
							albumList.clear();
						}

						try {
							Log.i(TAG, result.toString(2));
							JSONArray albums = result.getJSONArray("result");
							for (int i = 0; i < albums.length(); i++) {
								JSONObject albumsObject = albums.getJSONObject(i);
								String key = albumsObject.getString("key");
								String name = albumsObject.getString("name");
								String albumArt = albumsObject.getString("icon");
								JSONArray trackArray = albumsObject.getJSONArray("tracks");
								ArrayList<Track> trackList = new ArrayList<Track>();
								for(int j=0; j< trackArray.length();j++)
								{
									JSONObject tracksObject = trackArray.getJSONObject(j);
									String trackKey = tracksObject.getString("key");
									String trackName = tracksObject.getString("name");
									String trackIcon = tracksObject.getString("icon");
									String trackArtist = tracksObject.getString("artist");
									String duration = tracksObject.getString("duration");
									trackList.add(new Track(trackKey, trackName, trackArtist, trackIcon, trackIcon,duration));
									Log.d(TAG,
											"Found search api album: " + key + " => "
													+ tracksObject.getString("name")+" name "+name);
								}

								albumList.add(new Album(key, name, "", name,
										albumArt,trackList));
							}
							mAlbumHorizontalListView.setVisibility(View.VISIBLE);
							mAlbumHorizontalListView.setAdapter(mAlbumAdapter);
							mAlbumAdapter.notifyDataSetChanged();
							mAlbumHorizontalListView.setOnItemClickListener(new OnItemClickListener() {

								@Override
								public void onItemClick(AdapterView<?> arg0, View arg1,
										int arg2, long arg3) {
									Toast.makeText(RdioActivity.this, "Clicked Album name is  " + albumList.get(arg2).albumName, Toast.LENGTH_SHORT).show();
									Intent intent = new Intent(RdioActivity.this, NativePlayerActivity.class);
									intent.putExtra("isNativeLibrary", false);
									intent.putExtra("TrackList", albumList.get(arg2).trackList);
									startActivity(intent);
								}
							});
						} catch (JSONException e) {
							e.printStackTrace();
						}
						dismissGetCollectionDialog();

					}

					@Override
					public void onApiFailure(String arg0, Exception arg1) {
						dismissGetCollectionDialog();
						Toast.makeText(RdioActivity.this, "Unable to get Albums, Please try after sometime!",  Toast.LENGTH_LONG).show();
					}
				});


			}
		});

	}


	@Override
	public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture) {

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

	/*
	private void playPause() {
		if (player != null) {
			if (player.isPlaying()) {
				player.pause();
				updatePlayPause(false);
			} else {
				player.start();
				updatePlayPause(true);
			}
		} else {
			//TODO: 
			//next(true,-1);
		}
	}*/

	private void updatePlayPause(boolean playing) {
		if (playing) {
			//TODO: 
			//playPause.setImageResource(R.drawable.pause);
		} else {
			//TODO: 
			//playPause.setImageResource(R.drawable.play);
		}
	}

	/*************************
	 * RdioListener Interface
	 *************************/

	/*
	 * Dispatched by the Rdio object when the Rdio object is done initializing,
	 * and a connection to the Rdio app service has been established. If
	 * authorized is true, then we reused our existing OAuth credentials, and
	 * the API is ready for use.
	 * 
	 * @see com.rdio.android.api.RdioListener#onRdioReady()
	 */
	@Override
	public void onRdioReadyForPlayback() {
		Log.i(TAG, "Rdio SDK is ready for playback");

		if (accessToken != null && accessTokenSecret != null) {
			if(isNetworkOnline())
				getUserKey();
			else
				Toast.makeText(RdioActivity.this, "No Internet Connection!", Toast.LENGTH_SHORT).show();
		} else {
			if(isNetworkOnline())
				doSomethingWithoutApp();
			else
				Toast.makeText(RdioActivity.this, "No Internet Connection!", Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void onRdioUserPlayingElsewhere() {
		Log.w(TAG, "Tell the user that playback is stopping.");
	}

	/*
	 * Dispatched by the Rdio object once the setTokenAndSecret call has
	 * finished, and the credentials are ready to be used to make API calls. The
	 * token & token secret are passed in so that you can save/cache them for
	 * future re-use.
	 * 
	 * @see com.rdio.android.api.RdioListener#onRdioAuthorised(java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public void onRdioAuthorised(String accessToken, String accessTokenSecret) {
		Log.i(TAG, "Application authorised, saving access token & secret.");
		Log.d(TAG, "Access token: " + accessToken);
		Log.d(TAG, "Access token secret: " + accessTokenSecret);

		SharedPreferences settings = getPreferences(MODE_PRIVATE);
		Editor editor = settings.edit();
		editor.putString(PREF_ACCESSTOKEN, accessToken);
		editor.putString(PREF_ACCESSTOKENSECRET, accessTokenSecret);
		editor.commit();
	}

	/*************************
	 * Activity overrides
	 *************************/
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1) {
			if (resultCode == RESULT_OK) {
				Log.v(TAG, "Login success");
				if (data != null) {
					accessToken = data.getStringExtra("token");
					accessTokenSecret = data.getStringExtra("tokenSecret");
					onRdioAuthorised(accessToken, accessTokenSecret);
					rdio.setTokenAndSecret(accessToken, accessTokenSecret);
				}
			} else if (resultCode == RESULT_CANCELED) {
				if (data != null) {
					String errorCode = data
							.getStringExtra(OAuth1WebViewActivity.EXTRA_ERROR_CODE);
					String errorDescription = data
							.getStringExtra(OAuth1WebViewActivity.EXTRA_ERROR_DESCRIPTION);
					Log.v(TAG, "ERROR: " + errorCode + " - " + errorDescription);
				}
				accessToken = null;
				accessTokenSecret = null;
			}
			rdio.prepareForPlayback();
		}
	}

	/**
	 * Get Rdio's site-wide heavy rotation and play 30s samples. Doesn't require
	 * auth or the Rdio app to be installed
	 */
	private void doSomethingWithoutApp() {
		Log.i(TAG, "Getting heavy rotation");

		showGetHeavyRotationDialog();

		List<NameValuePair> args = new LinkedList<NameValuePair>();
		args.add(new BasicNameValuePair("type", "albums"));
		rdio.apiCall(GET_HEAVY_ROTATION_API, args, new RdioApiCallback() {
			@Override
			public void onApiSuccess(JSONObject result) {
				try {
					// Log.i(TAG, "Heavy rotation: " + result.toString(2));
					JSONArray albums = result.getJSONArray("result");
					final ArrayList<String> albumKeys = new ArrayList<String>(
							albums.length());
					for (int i = 0; i < albums.length(); i++) {
						JSONObject album = albums.getJSONObject(i);
						String albumKey = album.getString("key");
						albumKeys.add(albumKey);
					}

					// Build our argument to pass to the get api
					StringBuffer keyBuffer = new StringBuffer();
					Iterator<String> iter = albumKeys.iterator();
					while (iter.hasNext()) {
						keyBuffer.append(iter.next());
						if (iter.hasNext()) {
							keyBuffer.append(",");
						}
					}
					Log.i(TAG, "album keys to fetch: " + keyBuffer.toString());
					dismissGetHeavyRotationDialog();
				} catch (Exception e) {
					Log.e(TAG, "Failed to handle JSONObject: ", e);
				} finally {
					dismissGetHeavyRotationDialog();
				}
			}

			@Override
			public void onApiFailure(String methodName, Exception e) {
				dismissGetHeavyRotationDialog();
				Log.e(TAG, "getHeavyRotation failed. ", e);
			}
		});
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
			String image_url = artistList.get(position).albumArt;

			// ImageLoader class instance
			ImageLoader imgLoader = new ImageLoader(getApplicationContext());

			// whenever you want to load an image from url
			// call DisplayImage function
			// url - image url to load
			// loader - loader image, will be displayed before getting image
			// image - ImageView
			imgLoader.DisplayImage(image_url, loader, image);
			TextView title = (TextView) retval.findViewById(R.id.title);
			title.setText(artistList.get(position).artistName);
			Log.d(TAG, " HSCRL VIEW :: Artist Name is "+artistList.get(position).artistName);
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

			// Image url
			String image_url = albumList.get(position).albumArt;

			// ImageLoader class instance
			ImageLoader imgLoader = new ImageLoader(getApplicationContext());

			// whenever you want to load an image from url
			// call DisplayImage function
			// url - image url to load
			// loader - loader image, will be displayed before getting image
			// image - ImageView
			imgLoader.DisplayImage(image_url, loader, image);
			TextView title = (TextView) retval.findViewById(R.id.title);
			title.setText(albumList.get(position).albumName);
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

			// Image url
			String image_url = playList.get(position).albumArt;

			// ImageLoader class instance
			ImageLoader imgLoader = new ImageLoader(getApplicationContext());

			// whenever you want to load an image from url
			// call DisplayImage function
			// url - image url to load
			// loader - loader image, will be displayed before getting image
			// image - ImageView
			imgLoader.DisplayImage(image_url, loader, image);
			TextView title = (TextView) retval.findViewById(R.id.title);
			title.setText(playList.get(position).albumName);
			Log.d(TAG, " HSCRL VIEW :: Album Name is "+playList.get(position).albumName);

			return retval;
		}

	};



}
