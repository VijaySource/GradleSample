package com.rdio.android.api.example;

import java.util.ArrayList;
import java.util.Random;

import android.app.Service;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;

import com.rdio.android.api.example.utils.Utils;

public class MediaPlayerService extends Service implements OnCompletionListener,MediaPlayer.OnPreparedListener,SeekBar.OnSeekBarChangeListener,AudioManager.OnAudioFocusChangeListener {

	private MediaPlayer player;

	private final String TAG = "MediaPlayerService";

	public static String TRACK_INDEX = "TRACK_INDEX";

	private ArrayList<Track> trackList;

	private boolean isNativeLibrary = false;

	private int currentSongIndex = 0;

	private Handler mHandler = new Handler();

	private static int mPlayerCurrentPosition = -1;

	Utils utils;

	private LocalBroadcastManager mBroadcastManager;
	
	public SeekBar songProgressBar;

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		utils = new Utils();
		mBroadcastManager = LocalBroadcastManager.getInstance(this);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		boolean isPlaying = intent.getBooleanExtra("Play", false);
		if(isPlaying)
		{
		trackList = (ArrayList<Track>) intent.getSerializableExtra("TrackList");
		isNativeLibrary = intent.getBooleanExtra("isNativeLibrary",false);
		currentSongIndex = intent.getIntExtra(MediaPlayerService.TRACK_INDEX, 0);

		playSong(currentSongIndex);
		}else
		{
			if (player != null) {
				player.stop();	
			}
		}


		return START_STICKY;
	}

	private void playSong(int trackIndex) {
		
		 LayoutInflater mySeekBarInflate = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		    View layout = mySeekBarInflate.inflate(R.layout.player, null);
		    songProgressBar = (SeekBar) layout.findViewById(R.id.songProgressBar);
		    songProgressBar.setOnSeekBarChangeListener(this);
		    songProgressBar.setProgress(0);
			songProgressBar.setMax(100);
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
				//				if(getActivity() != null)
				{
					//TODO: Update title here
					//					getActivity().getActionBar().setTitle(track.trackName);
					sendTitleBroadcast(track.trackName);
					NativePlayerActivity.mDrawerList.setItemChecked(trackIndex, true);
					NativePlayerActivity.mDrawerList.setSelection(trackIndex);
					NativePlayerActivity.mTitle = track.trackName;
					AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
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
							player.setDataSource(MediaPlayerService.this, contentUri);
						}else
						{
							// Play Rdio Music
							//							if(player == null)
							player = RdioActivity.rdio.getPlayerForTrack(track.key,
									null, true);
						}
						player.prepareAsync();
						player.setOnPreparedListener(new OnPreparedListener() {

							@Override
							public void onPrepared(MediaPlayer mp) {
								Log.d(TAG, "Media Player Prepared !! ");
								mp.start();
								updateProgressBar();
							}
						});
						player.setOnErrorListener(new OnErrorListener() {

							@Override
							public boolean onError(MediaPlayer mp, int what, int extra) {
								mp.reset();
								return false;
							}
						});
						player.setOnCompletionListener(MediaPlayerService.this); 
						//						player.start();
						/*if(mPlayerCurrentPosition != -1)
							{
								player.seekTo(mPlayerCurrentPosition);
								final int progress = (int)(utils.getProgressPercentage(mPlayerCurrentPosition, player.getDuration()));
								songProgressBar.setProgress(progress);
							}else*/




					} catch (Exception e) {
						e.printStackTrace();
					}
					return track;
				}

				@Override
				protected void onPostExecute(Track track) {
					//TODO: UpdatePlayPauseHere
					sendPlayPauseBroadcast(true);
					//					updatePlayPause(true);

				}
			};
			task.execute(track);
			//TODO: Update AlbumArt here
			sendAlbumArtBroadcast(null);
			//TODO: Update Current Song Index
			sendSongIndexBroadcast("Track: " + (currentSongIndex+1));
			//songCurrentTrackLabel.setText("Track: " + (currentSongIndex+1));
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void onDestroy() {
		if (player.isPlaying()) {
			player.stop();
		}
		player.release();
	}

	public void onCompletion(MediaPlayer _mediaPlayer) {

		// check for repeat is ON or OFF
		if(NativePlayerFragment.isRepeat){
			// repeat is on play same song again
			playSong(currentSongIndex);
		} else if(NativePlayerFragment.isShuffle){
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
	public void onPrepared(MediaPlayer mp) {


	}

	@Override
	public void onAudioFocusChange(int focusChange) {

		switch (focusChange) {
		case AudioManager.AUDIOFOCUS_GAIN:
			Log.d(TAG, "Media Player AudioManager.AUDIOFOCUS_GAIN !! ");
			// resume playback
			if (player == null) 
			{
				playSong(currentSongIndex);
			}
			else if (!player.isPlaying()) player.start();
			player.setVolume(1.0f, 1.0f);
			//TODO: UpdatePlayPauseHere
			sendPlayPauseBroadcast(true);
			//			updatePlayPause(true);
			break;

		case AudioManager.AUDIOFOCUS_LOSS:
			Log.d(TAG, "Media Player AudioManager.AUDIOFOCUS_LOSS !! ");
			mPlayerCurrentPosition = player.getCurrentPosition();
			// Lost focus for an unbounded amount of time: stop playback and release media player
			if (player.isPlaying()) player.stop();
			//TODO: UpdatePlayPauseHere
			sendPlayPauseBroadcast(false);
			//			updatePlayPause(false);
			//            player.release();
			//            player = null;
			break;

		case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
			Log.d(TAG, "Media Player AudioManager.AUDIOFOCUS_LOSS_TRANSIENT !! ");
			// Lost focus for a short time, but we have to stop
			// playback. We don't release the media player because playback
			// is likely to resume
			if (player.isPlaying()) player.pause();
			//TODO: UpdatePlayPauseHere
			sendPlayPauseBroadcast(false);
			//			updatePlayPause(false);
			break;

		case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
			Log.d(TAG, "Media Player AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK !! ");
			// Lost focus for a short time, but it's ok to keep playing
			// at an attenuated level
			if (player.isPlaying()) player.setVolume(0.1f, 0.1f);
			break;
		}




	}

	private void sendPlayPauseBroadcast(boolean isPlaying) {
		Intent playIntent = new Intent(NativePlayerFragment.ACTION_PLAYPAUSE_RECEIVED);
		playIntent.putExtra("isMediaPlaying", isPlaying);
		mBroadcastManager.sendBroadcast(playIntent);

	}
	private void sendTitleBroadcast(String trackName) {
		Intent titleIntent = new Intent(NativePlayerFragment.ACTION_TITLE_RECEIVED);
		titleIntent.putExtra("TitleName", trackName);
		mBroadcastManager.sendBroadcast(titleIntent);
	}

	private void sendAlbumArtBroadcast(Bitmap bitmap) {
		Intent albumIntent = new Intent(NativePlayerFragment.ACTION_ALBUM_RECEIVED);
		albumIntent.putExtra("AlbumBitmap", bitmap);
		mBroadcastManager.sendBroadcast(albumIntent);
	}

	private void sendSongIndexBroadcast(String index) {

		Intent songIndexIntent = new Intent(NativePlayerFragment.ACTION_SONGINDEX_RECEIVED);
		songIndexIntent.putExtra("SongIndex", index);
		mBroadcastManager.sendBroadcast(songIndexIntent);
	}

	protected void sendDurationBroadcast(String totalDuartion, String currentDuration) {
		Intent durationIntent = new Intent(NativePlayerFragment.ACTION_DURATION_RECEIVED);
		durationIntent.putExtra("TotalDuration", totalDuartion);
		durationIntent.putExtra("CurrentDuration", currentDuration);
		mBroadcastManager.sendBroadcast(durationIntent);
	}

	protected void sendSeekbarBroadcast(int progress) {
		Intent seekBarIntent = new Intent(NativePlayerFragment.ACTION_SEEKBAR_RECEIVED);
		seekBarIntent.putExtra("SeekProgress", progress);
		mBroadcastManager.sendBroadcast(seekBarIntent);
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
			if(player != null)
			{
				long totalDuration = player.getDuration();
				long currentDuration = player.getCurrentPosition();

				//TODO: Update Duration here
				// Displaying Total Duration time
				//				songTotalDurationLabel.setText(""+utils.milliSecondsToTimer(totalDuration));
				// Displaying time completed playing
				//				songCurrentDurationLabel.setText(""+utils.milliSecondsToTimer(currentDuration));
				sendDurationBroadcast(""+utils.milliSecondsToTimer(totalDuration),""+utils.milliSecondsToTimer(currentDuration));
				// Updating progress bar
				final int progress = (int)(utils.getProgressPercentage(currentDuration, totalDuration));
				Log.d(TAG, "Progress is "+progress);
				//TODO: Update Seekbar here
				songProgressBar.setProgress(progress);
//				sendSeekbarBroadcast(progress);

				// Running this thread after 100 milliseconds
				mHandler.postDelayed(this, 100);
			}
		}
	};

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		// TODO Auto-generated method stub

	}

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
		if(player != null)
		{
			int totalDuration = player.getDuration();
			int currentPosition = utils.progressToTimer(songProgressBar.getProgress(), totalDuration);

			// forward or backward to certain seconds
			player.seekTo(currentPosition);

			// update timer progress again
			updateProgressBar();
		}
	}
}
