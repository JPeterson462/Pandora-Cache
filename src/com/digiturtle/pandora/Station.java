package com.digiturtle.pandora;

import java.io.Serializable;
import java.util.HashMap;

public class Station implements Comparable<Station>, Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private String id;
	
	private String idToken;
	
	private String name;
	
	private transient Song[] currentPlaylist;
	
	private transient PandoraRadio pandora;

	public Station(HashMap<String, Object> d, PandoraRadio instance) {
		this.id = ((String) d.get("stationId"));
		this.idToken = ((String) d.get("stationIdToken"));
		this.name = ((String) d.get("stationName"));
		this.pandora = instance;
	}

	public Song[] getPlaylist(boolean forceDownload) {
		return getPlaylist("aacplus", forceDownload);
	}

	public Song[] getPlaylist(String format, boolean forceDownload) {
		if ((forceDownload) || (this.currentPlaylist == null)) {
			return getPlaylist();
		}
		return this.currentPlaylist;
	}

	public Song[] getPlaylist(String format) {
		return getPlaylist();
	}

	public Song[] getPlaylist() {
		Song[] song_array = this.pandora.getPlaylist(this);
		if (song_array.length == 0) {
			this.pandora.disconnect();
		}
		return song_array;
	}

	public long getId() {
		try {
			return Long.parseLong(this.id);
		} catch (NumberFormatException ex) {}
		return this.id.hashCode();
	}

	public String getName() {
		return this.name;
	}

	public String getStationImageUrl() {
		getPlaylist(false);
		return this.currentPlaylist[0].getAlbumCoverUrl();
	}

	public int compareTo(Station another) {
		return getName().compareTo(another.getName());
	}

	public boolean equals(Station another) {
		return getName().equals(another.getName());
	}

	public String getStationId() {
		return this.id;
	}

	public String getStationIdToken() {
		return this.idToken;
	}

	public String toString() {
		return this.name;
	}
	
}
