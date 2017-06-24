package com.digiturtle.pandora;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.Key;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class JSONPandoraRadio implements PandoraRadio {
	private static final String ANDROID_DECRYPTION_KEY = "R=U!LH$O2B#";
	private static final String ANDROID_ENCRYPTION_KEY = "6#26FRL$ZWD";
	private static final String BLOWFISH_ECB_PKCS5_PADDING = "Blowfish/ECB/PKCS5Padding";
	private static final String BASE_URL = "https://tuner.pandora.com/services/json/?";
	private static final String BASE_NON_TLS_URL = "http://tuner.pandora.com/services/json/?";
	private static final String ANDROID_PARTNER_PASSWORD = "AC7IBG09A3DTSYM4R41UJWL07VLN8JI7";
	private Long syncTime;
	private Long clientStartTime;
	private Integer partnerId;
	private String partnerAuthToken;
	private String userAuthToken;
	private Long userId;
	private ArrayList<Station> stations;
	public boolean incompat = false;
	public static final String DEFAULT_AUDIO_FORMAT = "aacplus";
	public static final long PLAYLIST_VALIDITY_TIME = 10800L;

	public String pandoraEncrypt(String s) {
		try {
			Cipher cipher = Cipher.getInstance(BLOWFISH_ECB_PKCS5_PADDING);
			Key key = new SecretKeySpec(ANDROID_ENCRYPTION_KEY.getBytes(), "Blowfish");
			cipher.init(1, key);
			byte[] enc_bytes = cipher.doFinal(s.getBytes());
			return convertToHexString(enc_bytes);
		} catch (Exception e) {
			System.err.println("Encryption failed");
		}
		return null;
	}

	private String convertToHexString(byte[] input) {
		StringBuffer sb = new StringBuffer(input.length);
		for (int i = 0; i < input.length; i++) {
			sb.append(String.format("%02x", new Object[] { Byte.valueOf(input[i]) }));
		}
		return sb.toString();
	}

	private byte[] convertHexStringToBytes(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[(i / 2)] = 
					((byte)((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16)));
		}
		return data;
	}

	public String pandoraDecrypt(String hex) {
		try {
			Cipher dec_cipher = Cipher.getInstance(BLOWFISH_ECB_PKCS5_PADDING);
			Key dec_key = new SecretKeySpec(ANDROID_DECRYPTION_KEY.getBytes(), "Blowfish");
			dec_cipher.init(2, dec_key);
			byte[] enc_bytes = convertHexStringToBytes(hex);
			byte[] dec_result = dec_cipher.doFinal(enc_bytes);

			byte[] cut = new byte[dec_result.length - 4];
			System.arraycopy(dec_result, 4, cut, 0, cut.length);
			return new String(cut);
		} catch (Exception e) {
			System.err.println("Decryption failed");
		}
		return null;
	}

	public void connect(String user, String password) {
		this.clientStartTime = Long.valueOf(System.currentTimeMillis() / 1000L);
		partnerLogin();
		login(user, password);
	}

	private void partnerLogin() {
		JsonElement partnerLoginData = doPartnerLogin();
		JsonObject asJsonObject = partnerLoginData.getAsJsonObject();
		checkForError(asJsonObject, "Failed at Partner Login");
		JsonObject result = asJsonObject.getAsJsonObject("result");
		String encryptedSyncTime = result.get("syncTime").getAsString();
		this.partnerAuthToken = result.get("partnerAuthToken").getAsString();
		this.syncTime = Long.valueOf(pandoraDecrypt(encryptedSyncTime));
		this.partnerId = Integer.valueOf(result.get("partnerId").getAsInt());
	}

	private void checkForError(JsonObject songResult, String errorMessage) {
		String stat = songResult.get("stat").getAsString();
		if (!"ok".equals(stat)) {
			throw new Error(errorMessage);
		}
	}

	private boolean hasError(JsonObject songResult) {
		String stat = songResult.get("stat").getAsString();
		if (!"ok".equals(stat)) {
			return true;
		}
		return false;
	}

	private JsonElement doPartnerLogin() {
		String partnerLoginUrl = BASE_URL + "method=auth.partnerLogin";
		Map<String, Object> data = new HashMap<>();
		data.put("username", "android");
		data.put("password", ANDROID_PARTNER_PASSWORD);
		data.put("deviceModel", "android-generic");
		data.put("version", "5");
		data.put("includeUrls", Boolean.valueOf(true));
		String stringData = new Gson().toJson(data);

		return doPost(partnerLoginUrl, stringData);
	}

	private JsonElement doPost(String urlInput, String stringData) {
		try {
			URL url = new URL(urlInput);
			HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
			urlConnection.setRequestMethod("POST");
			urlConnection.setDoOutput(true);
			urlConnection.setDoInput(true);

			setRequestHeaders(urlConnection);

			urlConnection.setRequestProperty("Content-length", String.valueOf(stringData.length()));
			urlConnection.connect();
			DataOutputStream out = new DataOutputStream(urlConnection.getOutputStream());

			out.writeBytes(stringData);
			out.flush();
			out.close();
			BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
			String line;
			if ((line = reader.readLine()) != null)
			{
				System.out.println("response = " + line);
				JsonParser parser = new JsonParser();
				return parser.parse(line);
			}
		} catch (IOException e) {
			throw new Error("Failed to send POST data to Pandora");
		}
		return null;
	}

	private void setRequestHeaders(HttpURLConnection conn) {
		conn.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0)");
		conn.setRequestProperty("Content-Type", "text/plain");
		conn.setRequestProperty("Accept", "*/*");
	}

	private long getPandoraTime() {
		long diff = System.currentTimeMillis() / 1000L - this.clientStartTime.longValue();
		return this.syncTime.longValue() + diff;
	}

	private boolean login(String user, String password) {
		Map<String, Object> userLoginInputs = new HashMap<>();
		userLoginInputs.put("loginType", "user");
		userLoginInputs.put("username", user);
		userLoginInputs.put("password", password);
		userLoginInputs.put("partnerAuthToken", this.partnerAuthToken);
		userLoginInputs.put("syncTime", Long.valueOf(getPandoraTime()));
		String userLoginData = new Gson().toJson(userLoginInputs);
		String encryptedUserLoginData = pandoraEncrypt(userLoginData);
		String urlEncodedPartnerAuthToken = urlEncode(this.partnerAuthToken);

		String userLoginUrl = String.format(BASE_URL + "method=auth.userLogin&auth_token=%s&partner_id=%d", new Object[] { urlEncodedPartnerAuthToken, this.partnerId });
		JsonObject jsonElement = doPost(userLoginUrl, encryptedUserLoginData).getAsJsonObject();
		String loginStatus = jsonElement.get("stat").getAsString();
		if ("ok".equals(loginStatus)) {
			JsonObject userLoginResult = jsonElement.get("result").getAsJsonObject();
			this.userAuthToken = userLoginResult.get("userAuthToken").getAsString();
			this.userId = Long.valueOf(userLoginResult.get("userId").getAsLong());
			return true;
		}
		return false;
	}

	private String urlEncode(String s) {
		String encoding = "ISO-8859-1";
		try {
			return URLEncoder.encode(s, encoding);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(encoding + " is NOT a supported encoding", e);
		}
	}

	public void sync() {

	}

	public void disconnect() {
		this.syncTime = null;
		this.clientStartTime = null;
		this.partnerId = null;
		this.partnerAuthToken = null;
		this.userAuthToken = null;
		this.stations = null;
	}

	public ArrayList<Station> getStations() {
		JsonObject result = doStandardCall("user.getStationList", new HashMap<>(), false);
		checkForError(result, "Failed to get Stations");
		JsonArray stationArray = result.get("result").getAsJsonObject().getAsJsonArray("stations");
		this.stations = new ArrayList<>();
		for (JsonElement jsonStationElement : stationArray)
		{
			JsonObject jsonStation = jsonStationElement.getAsJsonObject();

			String stationId = jsonStation.get("stationId").getAsString();
			String stationIdToken = jsonStation.get("stationToken").getAsString();
			boolean isQuickMix = jsonStation.getAsJsonPrimitive("isQuickMix").getAsBoolean();
			String stationName = jsonStation.get("stationName").getAsString();

			HashMap<String, Object> hm = new HashMap<>(10);
			hm.put("stationId", stationId);
			hm.put("stationIdToken", stationIdToken);
			hm.put("isQuickMix", Boolean.valueOf(isQuickMix));
			hm.put("stationName", stationName);

			this.stations.add(new Station(hm, this));
		}
		Collections.sort(this.stations);

		return this.stations;
	}

	private JsonObject doStandardCall(String method, Map<String, Object> postData, boolean useSsl) {
		String url = String.format((useSsl ? BASE_URL : BASE_NON_TLS_URL) + "method=%s&auth_token=%s&partner_id=%d&user_id=%s", new Object[] { method, urlEncode(this.userAuthToken), this.partnerId, this.userId });
		System.out.println("url = " + url);
		postData.put("userAuthToken", this.userAuthToken);
		postData.put("syncTime", Long.valueOf(getPandoraTime()));
		String jsonData = new Gson().toJson(postData);
		System.out.println("jsonData = " + jsonData);
		return doPost(url, pandoraEncrypt(jsonData)).getAsJsonObject();
	}

	public Station getStationById(long sid) {
		if (this.stations == null) {
			getStations();
		}
		for (Station station : this.stations) {
			if (sid == station.getId()) {
				return station;
			}
		}
		return null;
	}

	public boolean rate(Station station, Song song, boolean rating) {
		String method = "station.addFeedback";
		Map<String, Object> data = new HashMap<>();
		data.put("trackToken", song.getTrackToken());
		data.put("isPositive", Boolean.valueOf(rating));
		JsonObject ratingResult = doStandardCall(method, data, false);
		checkForError(ratingResult, "failed to rate song");
		return true;
	}

	public Song[] getPlaylist(Station station) {
		Map<String, Object> data = new HashMap<>();
		data.put("stationToken", station.getStationIdToken());
		data.put("additionalAudioUrl", "HTTP_192_MP3,HTTP_128_MP3");
		JsonObject songResult = doStandardCall("station.getPlaylist", data, true);
		if (hasError(songResult)) {
			String err = "An error occured while getting playlist on station " + station.getName();
			System.err.println("An error occured while getting playlist on station " + station.getName());
			throw new RuntimeException(err);
		}
		JsonArray songsArray = songResult.get("result").getAsJsonObject().get("items").getAsJsonArray();
		List<Song> results = new ArrayList<>();
		try {
			for (JsonElement songElement : songsArray) {
				JsonObject songData = songElement.getAsJsonObject();
				if (songData.get("adToken") == null) {
					String album = songData.get("albumName").getAsString();
					String artist = songData.get("artistName").getAsString();
					String audioUrl = songData.get("audioUrlMap").getAsJsonObject()
							.get("highQuality").getAsJsonObject()
							.get("audioUrl").getAsString();
					String additional_audioUrl = songData.get("additionalAudioUrl").getAsString();
					if (additional_audioUrl != null) {
						audioUrl = additional_audioUrl;
					}
					String title = songData.get("songName").getAsString();
					String albumDetailUrl = songData.get("albumDetailUrl").getAsString();
					String artRadio = songData.get("albumArtUrl").getAsString();
					String trackToken = songData.get("trackToken").getAsString();
					Integer rating = Integer.valueOf(songData.get("songRating").getAsInt());

					String stationId = station.getStationId();
					results.add(new Song(album, artist, audioUrl, title, albumDetailUrl, artRadio, trackToken, rating, stationId));
				}
			}
			return (Song[]) results.toArray(new Song[results.size()]);
		}
		catch (Exception e) {
			String err = "An error occured while loading station " + station.getName() + ". Stack-trace :\n" + e.getMessage();
			throw new RuntimeException(err);
		}
	}

	public boolean bookmarkSong(Station station, Song song) {
		return false;
	}

	public boolean isAlive() {
		return this.userAuthToken != null;
	}

	public boolean bookmarkArtist(Station station, Song song) {
		return false;
	}

	public boolean tired(Station station, Song song) {
		return false;
	}

}

