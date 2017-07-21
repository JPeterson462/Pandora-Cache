package com.digiturtle.pandora.patch;

import java.io.File;
import java.io.IOException;

import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v1Tag;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.NotSupportedException;
import com.mpatric.mp3agic.UnsupportedTagException;

// Using https://github.com/mpatric/mp3agic

public class PandoraCacheFilenamePatch {
	
	private static final String prefix = "output/songs2";
	
	private static final String prefixUpdated = "output/";
	
	public static void main(String[] args) throws UnsupportedTagException, InvalidDataException, IOException, NotSupportedException {
		File directory = new File(prefix);
		for (File song : directory.listFiles((file) -> file.getName().endsWith(".mp3"))) {
			Mp3File updatedSong = new Mp3File(song);
			String name = song.getName();
			name = name.substring(0, name.lastIndexOf('.'));
			String[] data = name.split("_");
			String songName = data[0];
			String songArtist = data[1];
			String songAlbum = data[2];
			if (updatedSong.getId3v1Tag() == null) {
				updatedSong.setId3v1Tag(new ID3v1Tag());
			}
			ID3v1 tag = updatedSong.getId3v1Tag();
			System.out.println(tag);
			tag.setTitle(songName);
			tag.setArtist(songArtist);
			tag.setAlbum(songAlbum);
			updatedSong.save(prefixUpdated + songName + ".mp3");
			System.out.println(songName + "\t" + songArtist + "\t" + songAlbum);
		}
	}

}
