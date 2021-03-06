package com.example.nova.musicplayer;

import java.util.List;
import java.util.Random;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

/*
 * This is demo code to accompany the Mobiletuts+ series:
 * Android SDK: Creating a Music Player
 * 
 * Sue Smith - February 2014
 */

public class MusicService extends Service implements 
MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
MediaPlayer.OnCompletionListener {

	//media player
	private MediaPlayer player;
	//song list
	private List<Song> songs;
	//current position
	private int songPosn;
	//binder
	private final IBinder musicBind = new MusicBinder();
	//title of current song
	private String songTitle="";
	//notification id
	private static final int NOTIFY_ID=1;
	//shuffle flag and random
	private boolean shuffle=false;
	private Random rand;

	public void onCreate(){
		//create the service
		super.onCreate();
		//initialize position
		songPosn=0;
		//random
		rand=new Random();
		//create player
		player = new MediaPlayer();
		//initialize
		initMusicPlayer();
	}




	public void initMusicPlayer(){
		//set player properties
		player.setWakeMode(getApplicationContext(), 
				PowerManager.PARTIAL_WAKE_LOCK);
		player.setAudioStreamType(AudioManager.STREAM_MUSIC);

		//set listeners
		player.setOnPreparedListener(this);
		player.setOnCompletionListener(this);
		player.setOnErrorListener(this);
	}



	private AudioManager.OnAudioFocusChangeListener focusChangeListener =
			new AudioManager.OnAudioFocusChangeListener() {
				public void onAudioFocusChange(int focusChange) {
					AudioManager am =(AudioManager)getSystemService(Context.AUDIO_SERVICE);
					switch (focusChange) {

						case (AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) :
							// Lower the volume while ducking.
							player.setVolume(0.2f, 0.2f);
							break;
						case (AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) :
							player.pause();
							break;

						case (AudioManager.AUDIOFOCUS_LOSS) :
							player.stop();
//							ComponentName component = new ComponentName();
//							am.unregisterMediaButtonEventReceiver(component);
							break;

						case (AudioManager.AUDIOFOCUS_GAIN) :
							// Return the volume to normal and resume if paused.
							player.setVolume(1f, 1f);
							player.start();
							break;
						default: break;
					}
				}
			};








	//pass song list
	public void setList(List<Song> theSongs){
		songs=theSongs;
	}

	//binder
	public class MusicBinder extends Binder {
		MusicService getService() { 
			return MusicService.this;
		}
	}

	//activity will bind to service
	@Override
	public IBinder onBind(Intent intent) {
		return musicBind;
	}

	//release resources when unbind
	@Override
	public boolean onUnbind(Intent intent){
		player.stop();
		player.release();
		return false;
	}

	//play a song
	public void playSong(){

		//set audio focus
		AudioManager am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

		// Request audio focus for playback
		int result = am.requestAudioFocus(focusChangeListener,
				// Use the music stream.
				AudioManager.STREAM_MUSIC,
				// Request permanent focus.
				AudioManager.AUDIOFOCUS_GAIN);

//		if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
//			// other app had stopped playing song now , so u can do u stuff now .

			//play
			player.reset();
			//get song
			Song playSong = songs.get(songPosn);
			//get title
			songTitle=playSong.getTitle();
			//get id
			long currSong = playSong.getId();
			//set uri
			Uri trackUri = ContentUris.withAppendedId(
					android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
					currSong);
			//set the data source
			try{
				player.setDataSource(getApplicationContext(), trackUri);
			}
			catch(Exception e){
				Log.e("MUSIC SERVICE", "Error setting data source", e);
			}
			player.prepareAsync();

//		}
//		else {
//			Log.e("MUSIC SERVICE", "Error, other Audio Stream playing");
//		}
	}

	//set the song
	public void setSong(int songIndex){
		songPosn=songIndex;	
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		//check if playback has reached the end of a track
		if(player.getCurrentPosition()>0){
			mp.reset();
			playNext();
		}
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		Log.v("MUSIC PLAYER", "Playback Error");
		mp.reset();
		return false;
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		//start playback
		mp.start();
		//notification
		Intent notIntent = new Intent(this, MainActivity.class);
		notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent pendInt = PendingIntent.getActivity(this, 0,
				notIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		Notification.Builder builder = new Notification.Builder(this);

		builder.setContentIntent(pendInt)
		.setSmallIcon(R.drawable.play)
		.setTicker(songTitle)
		.setOngoing(true)
		.setContentTitle("Playing")
		.setContentText(songTitle);
		Notification not = builder.build();
		startForeground(NOTIFY_ID, not);
	}

	//playback methods
	public int getPosn(){
		return player.getCurrentPosition();
	}

	public int getDur(){
		return player.getDuration();
	}

	public boolean isPng(){
		return player.isPlaying();
	}

	public void pausePlayer(){
		player.pause();
	}

	public void seek(int posn){
		player.seekTo(posn);
	}

	public void go(){
		player.start();
	}

	//skip to previous track
	public void playPrev(){
		songPosn--;
		if(songPosn<0) songPosn=songs.size()-1;
		playSong();
	}

	//skip to next
	public void playNext(){
		if(shuffle){
			int newSong = songPosn;
			while(newSong==songPosn){
				newSong=rand.nextInt(songs.size());
			}
			songPosn=newSong;
		}
		else{
			songPosn++;
			if(songPosn>=songs.size()) songPosn=0;
		}
		playSong();
	}

	@Override
	public void onDestroy() {
		stopForeground(true);
	}

	//toggle shuffle
	public void setShuffle(){
		if(shuffle) shuffle=false;
		else shuffle=true;
	}

}
