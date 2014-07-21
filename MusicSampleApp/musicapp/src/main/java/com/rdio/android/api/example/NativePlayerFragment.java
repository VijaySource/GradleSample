package com.rdio.android.api.example;

import java.util.ArrayList;
import java.util.Random;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.gesture.GestureLibrary;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.rdio.android.api.example.utils.ImageLoader;
import com.rdio.android.api.example.utils.Utils;

public class NativePlayerFragment extends Fragment implements OnCompletionListener, SeekBar.OnSeekBarChangeListener,AudioManager.OnAudioFocusChangeListener /*implements GestureOverlayView.OnGesturePerformedListener*/ {

	private String TAG = "NativePlayerActivity";

	private ImageButton btnPlay;
	private ImageView albumArt;
	//	private ImageButton btnForward;
	//	private ImageButton btnBackward;
	private ImageButton btnNext;
	private ImageButton btnPrevious;
	//	private ImageButton btnPlaylist;
	private ImageButton btnRepeat;
	private ImageButton btnShuffle;
	public SeekBar songProgressBar;
	private TextView songCurrentDurationLabel;
	private TextView songTotalDurationLabel;
	private TextView songCurrentTrackLabel;
	private TextView songTotalTracksLabel;
	// Handler to update UI timer, progress bar etc,.
	private int seekForwardTime = 5000; // 5000 milliseconds
	private int seekBackwardTime = 5000; // 5000 milliseconds
	private int currentSongIndex = 0; 
	public static boolean isShuffle = false;
	public static boolean isRepeat = false;


	public MediaPlayer player;

	private GestureLibrary mLibrary;
	private ArrayList<Track> trackList;
	// Handler to update UI timer, progress bar etc,.
	private Handler mHandler = new Handler();

	private boolean isNativeLibrary = true;

	private static int mPlayerCurrentPosition = -1;

	Utils utils;

	private LocalBroadcastManager broadcastManager;

	public NativePlayerFragment() {
	}

	@SuppressWarnings("unchecked")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.player, container, false);


		trackList = (ArrayList<Track>) getArguments().getSerializable("TrackList");
		currentSongIndex = getArguments().getInt("TrackNo");
		isNativeLibrary = getArguments().getBoolean("isNativeLibrary");

		// All player buttons
		btnPlay = (ImageButton) rootView.findViewById(R.id.btnPlay);
		//		btnForward = (ImageButton) rootView.findViewById(R.id.btnForward);
		//		btnBackward = (ImageButton) rootView.findViewById(R.id.btnBackward);
		btnNext = (ImageButton) rootView.findViewById(R.id.btnNext);
		btnPrevious = (ImageButton) rootView.findViewById(R.id.btnPrevious);
		//		btnPlaylist = (ImageButton) rootView.findViewById(R.id.btnPlaylist);
		btnRepeat = (ImageButton) rootView.findViewById(R.id.btnRepeat);
		btnShuffle = (ImageButton) rootView.findViewById(R.id.btnShuffle);
		songProgressBar = (SeekBar) rootView.findViewById(R.id.songProgressBar);
		songCurrentDurationLabel = (TextView) rootView.findViewById(R.id.songCurrentDurationLabel);
		songTotalDurationLabel = (TextView) rootView.findViewById(R.id.songTotalDurationLabel);
		songCurrentTrackLabel = (TextView) rootView.findViewById(R.id.songCurrentTrackLabel);
		songTotalTracksLabel = (TextView) rootView.findViewById(R.id.songTotalTrackLabel);
		albumArt = (ImageView)rootView.findViewById(R.id.albumArt);

		utils = new Utils();
		songProgressBar.setOnSeekBarChangeListener(this);

		isShuffle = false;
		isRepeat = false;

		if (trackList != null)
			songTotalTracksLabel.setText("Total Tracks:  " + trackList.size());
		else
			songTotalTracksLabel.setVisibility(View.GONE);

		broadcastManager = LocalBroadcastManager.getInstance(getActivity());

		mSeekBarProgressReciever = new SeekBarProgressReciever();
		mActioBarTitleReciever = new ActioBarTitleReciever();
		mAlbumArtReciever = new AlbumArtReciever();
		mCurrentSongIndexReciever = new CurrentSongIndexReciever();
		mDurationReciever = new DurationReciever();
		mPlayPauseReciever = new PlayPauseReciever();

		broadcastManager.registerReceiver(mSeekBarProgressReciever,new IntentFilter(ACTION_SEEKBAR_RECEIVED));
		broadcastManager.registerReceiver(mActioBarTitleReciever,new IntentFilter(ACTION_TITLE_RECEIVED));
		broadcastManager.registerReceiver(mAlbumArtReciever,new IntentFilter(ACTION_ALBUM_RECEIVED));
		broadcastManager.registerReceiver(mCurrentSongIndexReciever,new IntentFilter(ACTION_SONGINDEX_RECEIVED));
		broadcastManager.registerReceiver(mDurationReciever,new IntentFilter(ACTION_DURATION_RECEIVED));
		broadcastManager.registerReceiver(mPlayPauseReciever,new IntentFilter(ACTION_PLAYPAUSE_RECEIVED));


		rootView.findViewById(R.id.player_main).setOnTouchListener(
				new OnSwipeTouchListener(getActivity()) {
					@Override
					public void onSwipeLeft() {
						Log.d(TAG,"Swipe Left || Previous ");
						// play previous
						if (currentSongIndex > 0 && currentSongIndex <= (trackList.size()-1)) {
							currentSongIndex--;
						} else {
							currentSongIndex = (trackList.size()-1);
						}
						playSong(currentSongIndex);
					}

					@Override
					public void onSwipeRight() {
						Log.d(TAG,"Swipe Right || Next ");
						// play next
						if (currentSongIndex >= 0 && currentSongIndex < (trackList.size()-1)) {
							currentSongIndex++;
						} else {
							currentSongIndex = 0;
						}
						playSong(currentSongIndex);

					}

					public void onSwipeBottom() {
						Log.d(TAG,"Swipe Bottom || Pause ");
						// play
						playPause();
					};

					public void onSwipeTop() {
						Log.d(TAG,"Swipe Top || Play ");
						// pause
						playPause();


					};
				});

		btnNext.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// play next
				if (currentSongIndex >= 0 && currentSongIndex < (trackList.size()-1)) {
					currentSongIndex++;
				} else {
					currentSongIndex = 0;
				}
				playSong(currentSongIndex);
			}
		});

		btnPrevious.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// play previous
				if (currentSongIndex > 0 && currentSongIndex <= (trackList.size()-1)) {
					currentSongIndex--;
				} else {
					currentSongIndex = (trackList.size()-1);
				}
				playSong(currentSongIndex);
			}
		});

		btnPlay.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				playPause();
			}
		});

		/**
		 * Button Click event for Repeat button
		 * Enables repeat flag to true
		 * */
		btnRepeat.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if(isRepeat){
					isRepeat = false;
					Toast.makeText(getActivity().getApplicationContext(), "Repeat is OFF", Toast.LENGTH_SHORT).show();
					btnRepeat.setImageResource(R.drawable.btn_repeat);
				}else{
					// make repeat to true
					isRepeat = true;
					Toast.makeText(getActivity().getApplicationContext(), "Repeat is ON", Toast.LENGTH_SHORT).show();
					// make shuffle to false
					isShuffle = false;
					btnRepeat.setImageResource(R.drawable.btn_repeat_focused);
					btnShuffle.setImageResource(R.drawable.btn_shuffle);
				}	
			}
		});

		/**
		 * Button Click event for Shuffle button
		 * Enables shuffle flag to true
		 * */
		btnShuffle.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if(isShuffle){
					isShuffle = false;
					Toast.makeText(getActivity().getApplicationContext(), "Shuffle is OFF", Toast.LENGTH_SHORT).show();
					btnShuffle.setImageResource(R.drawable.btn_shuffle);
				}else{
					// make repeat to true
					isShuffle= true;
					Toast.makeText(getActivity().getApplicationContext(), "Shuffle is ON", Toast.LENGTH_SHORT).show();
					// make shuffle to false
					isRepeat = false;
					btnShuffle.setImageResource(R.drawable.btn_shuffle_focused);
					btnRepeat.setImageResource(R.drawable.btn_repeat);
				}	
			}
		});

		playSong(currentSongIndex);

		return rootView;
	}

	/**
	 * Start a service and play media in service, so that player keeps running in the background.
	 * @param trackIndex
	 */
	private void playSong(int trackIndex)
	{

		/*Intent intent = new Intent(getActivity(),                
				MediaPlayerService.class);
		intent.putExtra(MediaPlayerService.TRACK_INDEX, trackIndex);
		intent.putExtra("TrackList", trackList);
		intent.putExtra("isNativeLibrary", isNativeLibrary);
		intent.putExtra("Play", true);
		getActivity().startService(intent);*/
		nextSong(trackIndex);
	}



	private void nextSong(int trackIndex) {
		if (player != null) {
			player.stop();
			if(isNativeLibrary)
				player.reset();
			player.release();
			//			player = null;
		}
		try {
			final Track track = trackList.get(trackIndex);
			if (track == null) {
				Log.e(TAG, "Track is null!  Size of queue: " + trackList.size());
				return;
			}
			try
			{
				if(getActivity() != null)
				{
					getActivity().getActionBar().setTitle(track.trackName);
					NativePlayerActivity.mDrawerList.setItemChecked(trackIndex, true);
					NativePlayerActivity.mDrawerList.setSelection(trackIndex);
					NativePlayerActivity.mTitle = track.trackName;
					AudioManager audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
					int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
							AudioManager.AUDIOFOCUS_GAIN);
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			// Load the next track in the background and prep the player (to
			// start
			// buffering)
			// Do this in a bkg thread so it doesn't block the main thread in
			// .prepare()
			AsyncTask<Track, Void, Track> task = new AsyncTask<Track, Void, Track>() {
				@Override
				protected Track doInBackground(Track... params) {
					Track track = params[0];
					//write code for player
					try {
						Uri contentUri = null;
						if(isNativeLibrary)
						{
							// Play Native Music
							contentUri = ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Long.valueOf(track.key));
							//							if(player == null)
							player = new MediaPlayer();
							player.reset();
							player.setAudioStreamType(AudioManager.STREAM_MUSIC);
							player.setDataSource(getActivity(), contentUri);
						}else
						{
							// Play Rdio Music
							//							if(player == null)
							player = RdioActivity.rdio.getPlayerForTrack(track.key,
									null, true);
						}
						player.prepare();
						player.setOnPreparedListener(new OnPreparedListener() {

							@Override
							public void onPrepared(MediaPlayer mp) {
								Log.d(TAG, "Media Player Prepared !! ");
								if(isNativeLibrary)
								{
									mp.start();
									songProgressBar.setProgress(0);
									songProgressBar.setMax(100);
									updateProgressBar();
								}
							}
						});
						player.setOnErrorListener(new OnErrorListener() {

							@Override
							public boolean onError(MediaPlayer mp, int what, int extra) {
								mp.reset();
								return false;
							}
						});
						player.setOnCompletionListener(NativePlayerFragment.this); 
						//						player.start();
						/*if(mPlayerCurrentPosition != -1)
							{
								player.seekTo(mPlayerCurrentPosition);
								final int progress = (int)(utils.getProgressPercentage(mPlayerCurrentPosition, player.getDuration()));
								songProgressBar.setProgress(progress);
							}else*/
						if(!isNativeLibrary)
						{
							player.start();
							songProgressBar.setProgress(0);
							songProgressBar.setMax(100);
							updateProgressBar();
						}


					} catch (Exception e) {
						e.printStackTrace();
					}
					return track;
				}

				@Override
				protected void onPostExecute(Track track) {
					updatePlayPause(true);

				}
			};
			task.execute(track);
			albumArt.setAdjustViewBounds(true);
			albumArt.setScaleType(ScaleType.FIT_XY);
			if(isNativeLibrary)
			{
				try
				{
					Bitmap bitmap = Utils.getAlbumArt(getActivity().getApplicationContext(), track.albumId,false);
					int width = bitmap.getWidth();
					int height = bitmap.getHeight();
					Log.d(TAG, "Player ICON width is "+width+" height "+height);
					albumArt.setImageBitmap(bitmap);
					getActivity().getActionBar().setLogo(new BitmapDrawable(bitmap));
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}else
			{
				// Loader image - will be shown before loading image
				int loader = R.drawable.blank_album_art;

				// Image url
				String artworkUrl = track.albumArt.replace(
						"square-200", "square-400");

				// ImageLoader class instance
				ImageLoader imgLoader = new ImageLoader(getActivity().getApplicationContext());

				// whenever you want to load an image from url
				// call DisplayImage function
				// url - image url to load
				// loader - loader image, will be displayed before getting image
				// image - ImageView
				Bitmap bitmap = imgLoader.DisplayImage(artworkUrl, loader, albumArt);
				if(bitmap != null)
					getActivity().getActionBar().setLogo(new BitmapDrawable(bitmap));
			}

			songCurrentTrackLabel.setText("Track: " + (currentSongIndex+1));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private SeekBarProgressReciever mSeekBarProgressReciever;
	private ActioBarTitleReciever mActioBarTitleReciever;
	private DurationReciever mDurationReciever;
	private CurrentSongIndexReciever mCurrentSongIndexReciever;
	private AlbumArtReciever mAlbumArtReciever;
	private PlayPauseReciever mPlayPauseReciever;

	public static final String ACTION_SEEKBAR_RECEIVED = "com.rdio.android.api.example.ACTION_SEEKBAR_RECEIVED";
	public static final String ACTION_TITLE_RECEIVED = "com.rdio.android.api.example.ACTION_TITLE_RECEIVED";
	public static final String ACTION_DURATION_RECEIVED = "com.rdio.android.api.example.ACTION_DURATION_RECEIVED";
	public static final String ACTION_SONGINDEX_RECEIVED = "com.rdio.android.api.example.ACTION_SONGINDEX_RECEIVED";
	public static final String ACTION_ALBUM_RECEIVED = "com.rdio.android.api.example.ACTION_ALBUM_RECEIVED";
	public static final String ACTION_PLAYPAUSE_RECEIVED = "com.rdio.android.api.example.ACTION_PLAYPAUSE_RECEIVED";

	/**
	 * Listens for SeekBarProgressReciever
	 */
	private class SeekBarProgressReciever extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			int progress = intent.getIntExtra("SeekProgress", 0);
			updateSeekbar(progress);
		}
	}

	/**
	 * Listens for ActioBarTitleReciever
	 */
	private class ActioBarTitleReciever extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String title = intent.getStringExtra("TitleName");
			updateTitle(title);
		}
	}

	/**
	 * Listens for ActioBarTitleReciever
	 */
	private class DurationReciever extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String currentDurationText = intent.getStringExtra("CurrentDuration");
			String totalDurationText = intent.getStringExtra("TotalDuration");
			updateDurationText(totalDurationText,currentDurationText);
		}
	}

	/**
	 * Listens for CurrentSongIndexReciever
	 */
	private class CurrentSongIndexReciever extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String currentSongIndex = intent.getStringExtra("SongIndex");
			updateCurrentSongIndex(currentSongIndex);
		}
	}

	/**
	 * Listens for AlbumArtReciever
	 */
	private class AlbumArtReciever extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {

		}
	}

	/**
	 * Listens for PlayPauseReciever
	 */
	private class PlayPauseReciever extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {

			boolean isPlaying = intent.getBooleanExtra("isMediaPlaying", false);
			updatePlayPause(isPlaying);

		}
	}



	private void updateTitle(String title)
	{
		// update title track here
		try
		{
			if(getActivity() != null)
				getActivity().getActionBar().setTitle(title);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	private void updateSeekbar(int progress)
	{

		// update seekbar here
		songProgressBar.setProgress(progress);
	}

	private void updateDurationText(String currentDurationText, String totalDurationText)
	{
		// update durationText here
		// Displaying Total Duration time
		songTotalDurationLabel.setText(totalDurationText);
		// Displaying time completed playing
		songCurrentDurationLabel.setText(currentDurationText);
	}

	private void updateCurrentSongIndex(String index)
	{
		// update current song index here
		songCurrentTrackLabel.setText(index);
	}

	private void updateCurrentAlbumArt()
	{
		// update album art here
	}

	/**
	 * Update timer on seekbar
	 * */
	public void updateProgressBar() {
		mHandler.postDelayed(mUpdateTimeTask, 100);
	} 

	/**
	 * Background Runnable thread
	 * */
	private Runnable mUpdateTimeTask = new Runnable() {
		public void run() {
			try
			{
			if(player != null && player.isPlaying())
			{
				long totalDuration = player.getDuration();
				long currentDuration = player.getCurrentPosition();

				// Displaying Total Duration time
				songTotalDurationLabel.setText(""+utils.milliSecondsToTimer(totalDuration));
				// Displaying time completed playing
				songCurrentDurationLabel.setText(""+utils.milliSecondsToTimer(currentDuration));

				// Updating progress bar
				final int progress = (int)(utils.getProgressPercentage(currentDuration, totalDuration));
				Log.d(TAG, "Progress is "+progress);
				songProgressBar.setProgress(progress);


				// Running this thread after 100 milliseconds
				mHandler.postDelayed(this, 100);
			}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	};

	@Override
	public void onDestroy() {
		Log.i(TAG, "Cleaning up..");
		broadcastManager.unregisterReceiver(mActioBarTitleReciever);
		broadcastManager.unregisterReceiver(mSeekBarProgressReciever);
		broadcastManager.unregisterReceiver(mCurrentSongIndexReciever);
		broadcastManager.unregisterReceiver(mDurationReciever);
		broadcastManager.unregisterReceiver(mAlbumArtReciever);
		broadcastManager.unregisterReceiver(mPlayPauseReciever);
		super.onDestroy();
	}

	private void playPause() {
		try
		{
		if (player != null) {
			if (player.isPlaying()) {
				player.pause();
				updatePlayPause(false);
			} else {
				player.start();
				updatePlayPause(true);
			}
		} else {
			playSong(currentSongIndex);
		}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	private void updatePlayPause(boolean playing) {
		if (playing) {
			btnPlay.setImageResource(R.drawable.pause);
		} else {
			btnPlay.setImageResource(R.drawable.play);
		}
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
		Log.d(TAG, "fromTouch is "+fromTouch);
		if(fromTouch)
		{
			// user changed the seek position
			// forward or backward to certain seconds
			//			player.seekTo(progress);
			//			songProgressBar.setProgress(progress);
			//			seekBar.setProgress(progress);

			// update timer progress again
			//			updateProgressBar();
		}else
		{
			// update timer progress again
			//			player.seekTo(progress);
			//			updateProgressBar();
		}
	}

	/**
	 * When user starts moving the progress handler
	 * */
	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// remove message Handler from updating progress bar
		mHandler.removeCallbacks(mUpdateTimeTask);
	}

	/**
	 * When user stops moving the progress hanlder
	 * */
	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		mHandler.removeCallbacks(mUpdateTimeTask);
		try
		{
		if(player != null)
		{
			int totalDuration = player.getDuration();
			int currentPosition = utils.progressToTimer(seekBar.getProgress(), totalDuration);

			// forward or backward to certain seconds
			player.seekTo(currentPosition);

			// update timer progress again
			updateProgressBar();
		}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		// check for repeat is ON or OFF
		if(isRepeat){
			// repeat is on play same song again
			playSong(currentSongIndex);
		} else if(isShuffle){
			// shuffle is on - play a random song
			Random rand = new Random();
			currentSongIndex = rand.nextInt((trackList.size() - 1));
			playSong(currentSongIndex);
		}else
		{

			if (currentSongIndex < (trackList.size()-1)) {
				currentSongIndex++;
			} else {
				currentSongIndex = 0;
			}
			playSong(currentSongIndex);

		}

	}

	@Override
	public void onAudioFocusChange(int focusChange) {
		switch (focusChange) {
		case AudioManager.AUDIOFOCUS_GAIN:
			Log.d(TAG, "Media Player AudioManager.AUDIOFOCUS_GAIN !! ");
			// resume playback
			try
			{
			if (player == null) 
			{
				playSong(currentSongIndex);
			}
			else if (!player.isPlaying()) player.start();
			player.setVolume(1.0f, 1.0f);
			updatePlayPause(true);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			break;

		case AudioManager.AUDIOFOCUS_LOSS:
			Log.d(TAG, "Media Player AudioManager.AUDIOFOCUS_LOSS !! ");
			try
			{
			mPlayerCurrentPosition = player.getCurrentPosition();
			// Lost focus for an unbounded amount of time: stop playback and release media player
			if (player.isPlaying()) player.stop();
			updatePlayPause(false);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			//            player.release();
			//            player = null;
			break;

		case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
			Log.d(TAG, "Media Player AudioManager.AUDIOFOCUS_LOSS_TRANSIENT !! ");
			// Lost focus for a short time, but we have to stop
			// playback. We don't release the media player because playback
			// is likely to resume
			try
			{
			if (player.isPlaying()) player.pause();
			updatePlayPause(false);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			break;

		case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
			Log.d(TAG, "Media Player AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK !! ");
			// Lost focus for a short time, but it's ok to keep playing
			// at an attenuated level
			try
			{
			if (player.isPlaying()) player.setVolume(0.1f, 0.1f);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			break;
		}


	}

}

