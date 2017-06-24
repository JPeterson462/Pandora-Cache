package com.digiturtle.pandora;

import java.util.ArrayList;

public interface PandoraRadio {

	public String pandoraEncrypt(String data);

	public String pandoraDecrypt(String data);

	public void connect(String username, String password);

	public void sync();

	public void disconnect();

	public ArrayList<Station> getStations();

	public Station getStationById(long id);

	public Song[] getPlaylist(Station station);

	public boolean rate(Station station, Song song, boolean like);

	public boolean bookmarkSong(Station station, Song song);

	public boolean isAlive();

	public boolean bookmarkArtist(Station station, Song song);

	public boolean tired(Station station, Song song);

}
