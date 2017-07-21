package com.digiturtle.pandora;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;

import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v1Tag;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.NotSupportedException;
import com.mpatric.mp3agic.UnsupportedTagException;

public class PandoraCache {
	
	public static void main(String[] args) throws Exception {
		System.setOut(new PrintStream(new FileOutputStream("log.txt")));
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
	
	public void downloadSongs() throws MalformedURLException, FileNotFoundException, IOException, UnsupportedTagException, InvalidDataException, NotSupportedException {
		if (station == null) {
			return;
		}
		int songCount = 0;
		int songsToRead = songs;
		while (songsToRead > 0) {
			Song[] songs = station.getPlaylist("mp3-hifi");
			for (int i = 0; i < songs.length; i++) {
				Song song = songs[i];
				String albumFilename = song.getArtist() + "_" + song.getAlbum();
				String songFilename = song.getTitle() + "_" + albumFilename;
				// Download the song and album image
				File albumFile = new File("output/" + albumFilename + ".png");
				Path songFile = Files.createTempFile(cleanPath(new File(songFilename)).getPath(), ".tmp");
				downloadFile(song.getAlbumCoverUrl(), albumFile);
				downloadFile(song.getAudioUrl(), songFile.toFile());
				// Add the metadata
				Mp3File mp3 = new Mp3File(songFile.toFile());
				mp3.setId3v1Tag(new ID3v1Tag());
				ID3v1 tag = mp3.getId3v1Tag();
				tag.setTitle(song.getTitle());
				tag.setArtist(song.getArtist());
				tag.setAlbum(song.getAlbum());
				mp3.save("output/" + song.getTitle() + ".mp3");
				songCount++;
				System.out.println(songCount + "\t" + tag.getTitle() + "\t" + tag.getArtist() + "\t" + tag.getAlbum());
			}
			songsToRead -= songs.length;
		}
	}
	
	private void downloadFile(String url, File output) throws IOException {
		output = cleanPath(output);
		System.out.println("Downloading: " + url + "...");
		ReadableByteChannel channel = Channels.newChannel(new URL(url).openStream());
		FileOutputStream outputStream = new FileOutputStream(output);
		outputStream.getChannel().transferFrom(channel, 0, Long.MAX_VALUE);
		outputStream.close();
	}
	
	private File cleanPath(File path) {
		String pathText = path.getPath();
		pathText = pathText.replaceAll("&", "and").replaceAll(": ", "  ");
		return new File(pathText);
	}

}
