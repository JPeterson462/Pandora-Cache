package com.digiturtle.pandora;

public class Song {

	private String album;

	private String artist;

	private String audioUrl;

	private String title;

	private String artRadio;

	private String trackToken;

	private Integer rating;

	private long playlistTime;

	public Song(String album, String artist, String audioUrl, String title, String albumDetailUrl, String artRadio,
			String trackToken, Integer rating, String stationId) {
		this.album = album;
		this.artist = artist;
		this.audioUrl = audioUrl;
		this.title = title;
		this.artRadio = artRadio;
		this.trackToken = trackToken;
		this.rating = rating;
		playlistTime = System.currentTimeMillis() / 1000L;
	}

	public String getTrackToken() {
		return this.trackToken;
	}

	public void setTrackToken(String trackToken) {
		this.trackToken = trackToken;
	}

	public void setAlbum(String album) {
		this.album = album;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public boolean isStillValid() {
		return System.currentTimeMillis() / 1000L - this.playlistTime < 10800L;
	}

	public boolean isLoved() {
		if (this.rating.intValue() == 1) {
			return true;
		}
		return false;
	}

	public String getAudioUrl() {
		return this.audioUrl;
	}

	public String getAlbumCoverUrl() {
		return this.artRadio;
	}

	public String getTitle() {
		return this.title;
	}

	public String getArtist() {
		return this.artist;
	}

	public String getAlbum() {
		return this.album;
	}

}
