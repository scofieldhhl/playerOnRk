package org.app.enjoy.music.data;

import android.text.TextUtils;

import java.io.Serializable;

/**
 * Created by victor on 2016/5/4.
 */
public class MusicData implements Serializable{
    public static final String UNIT_SAMPLE_RATE = "HZ";
    public static final String UNIT_BIT = "bits";
    public static final String UNIT_BITS_RATE = "Kbps";
    public String title;
    public long duration;
    public String artist;
    public String artistId;
    public int id;
    public String displayName;
    public String data;
    public String path;//路径
    public String albumId;
    public String album;
    public String size;

    public String indexBegin;//开始时间
    public String indexEnd;//结束时间

    public String format;//格式信息
    public long seekPostion;

    public String sampleRate;//采样频率 HZ
    public String bit;// bits
    public String bitRate;//码率Kbps

    public String categoryType;//列表所属类别

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setData(String data) {
        this.data = data;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setAlbumId(String albumId) {
        this.albumId = albumId;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getTitle() {
        return title;
    }

    public long getDuration() {
        return duration;
    }

    public String getArtist() {
        return artist;
    }

    public int getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getData() {
        return data;
    }

    public String getPath() {
        return path;
    }

    public String getAlbumId() {
        return albumId;
    }

    public String getAlbum() {
        return album;
    }

    public String getSize() {
        return size;
    }

    public String getIndexBegin() {
        return indexBegin;
    }

    public String getIndexEnd() {
        return indexEnd;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public void setIndexBegin(String indexBegin) {
        this.indexBegin = indexBegin;
    }

    public void setIndexEnd(String indexEnd) {
        this.indexEnd = indexEnd;
    }

    public long getSeekPostion() {
        return seekPostion;
    }

    public void setSeekPostion(long seekPostion) {
        this.seekPostion = seekPostion;
    }

    public String getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(String sampleRate) {
        this.sampleRate = sampleRate;
    }

    public String getBit() {
        if(bit == null || TextUtils.isEmpty(bit)){
            bit = "16 bits";
        }
        return bit;
    }

    public void setBit(String bit) {
        this.bit = bit;
    }

    public String getBitRate() {
        return bitRate;
    }

    public void setBitRate(String bitRate) {
        this.bitRate = bitRate;
    }
}
