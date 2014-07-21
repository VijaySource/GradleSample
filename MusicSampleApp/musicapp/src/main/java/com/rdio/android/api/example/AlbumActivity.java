package com.rdio.android.api.example;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.devsmart.android.ui.HorizontalListView;
import com.rdio.android.api.RdioApiCallback;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class AlbumActivity extends Activity {
	
	private String TAG = "AlbumActivity";
	
	private final String GET_ALBUMS_FOR_ARTIST_API = "getAlbumsForArtist";
	
	private DialogFragment getCollectionDialog;
	
	private ArrayList<Album> albumList;
	private ArrayList<Album> artistList;
	
	private HorizontalListView mPlaylistHorizontalListView;
	private HorizontalListView mArtistHorizontalListView;
	private HorizontalListView mAlbumHorizontalListView;
	
	private String mCollectionKey = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.rdio_list);
		
		mCollectionKey = getIntent().getStringExtra("UserKey");
		artistList = (ArrayList<Album>) getIntent().getSerializableExtra("ArtistList");
		
		
		mPlaylistHorizontalListView = (HorizontalListView) findViewById(R.id.playlist_hs_listview);
		
		mArtistHorizontalListView = (HorizontalListView) findViewById(R.id.artist_hs_listview);

		mAlbumHorizontalListView = (HorizontalListView) findViewById(R.id.album_hs_listview);
		
		displayListContents();
		
	}
	
	
	private void displayListContents() {
		mArtistHorizontalListView.setVisibility(View.VISIBLE);	
		mArtistHorizontalListView.setAdapter(mArtistAdapter);
		mArtistAdapter.notifyDataSetChanged();
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
				args.add(new BasicNameValuePair("keys", mCollectionKey));//p3554448,c18977962

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
									Toast.makeText(AlbumActivity.this, "Clicked Album name is  " + albumList.get(arg2).albumName, Toast.LENGTH_SHORT).show();
									Intent intent = new Intent(AlbumActivity.this, NativePlayerActivity.class);
									intent.putExtra("TrackList", albumList.get(arg2).trackList);
									intent.putExtra("isNativeLibrary", false);
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
						Toast.makeText(AlbumActivity.this, "Unable to get Albums, Please try after sometime!",  Toast.LENGTH_LONG).show();
					}
				});


			}
		});
		
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
			TextView title = (TextView) retval.findViewById(R.id.title);
			title.setText(albumList.get(position).albumName);
			Log.d(TAG, " HSCRL VIEW :: Album Name is "+albumList.get(position).albumName);

			return retval;
		}

	};
	

}
