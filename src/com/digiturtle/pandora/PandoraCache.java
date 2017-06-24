package com.digiturtle.pandora;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;

public class PandoraCache {
	
	public static void main(String[] args) throws Exception {
		PandoraCache cache = new PandoraCache(new JSONPandoraRadio());
		cache.readConfiguration(new FileInputStream("sample.cfg"));
		cache.downloadSongs();
	}
	
	private String username, password, stationName;
	
	private int songs;

	private PandoraRadio radio;
	
	private Station station;
	
	public PandoraCache(PandoraRadio radio) {
		this.radio = radio;
	}
	
	public void readConfiguration(InputStream source) {
		HashMap<String, String> properties = new HashMap<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(source))) {
			reader.lines().forEach(line -> {
				properties.put(line.substring(0, line.indexOf(":")).toLowerCase().trim(), line.substring(line.indexOf(":") + 1).trim());
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
		username = properties.get("username");
		password = properties.get("password");
		stationName = properties.get("station");
		songs = Integer.parseInt(properties.get("songs"));
		radio.connect(username, password);
		ArrayList<Station> stations = radio.getStations();
		for (int i = 0; i < stations.size(); i++) {
			Station station = stations.get(i);
			if (station.getName().equalsIgnoreCase(stationName)) {
				this.station = station;
			}
		}
	}
	
	public void downloadSongs() throws MalformedURLException, FileNotFoundException, IOException {
		if (station == null) {
			return;
		}
		int songsToRead = songs;
		while (songsToRead > 0) {
			Song[] songs = station.getPlaylist("mp3-hifi");
			for (int i = 0; i < songs.length; i++) {
				Song song = songs[i];
				String albumFile = song.getArtist() + "_" + song.getAlbum();
				String songFile = song.getTitle() + "_" + albumFile;
				
				downloadFile(song.getAlbumCoverUrl(), "output/" + albumFile + ".png");
				downloadFile(song.getAudioUrl(), "output/" + songFile + ".mp3");
			}
			songsToRead -= songs.length;
		}
	}
	
	private void downloadFile(String url, String output) throws IOException {
		System.out.println("Downloading: " + url + "...");
		ReadableByteChannel channel = Channels.newChannel(new URL(url).openStream());
		FileOutputStream outputStream = new FileOutputStream(output);
		outputStream.getChannel().transferFrom(channel, 0, Long.MAX_VALUE);
		outputStream.close();
		System.out.println("Saved to: " + output);
	}

}
