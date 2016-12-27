package org.app.enjoy.music.tool;

import android.text.TextUtils;
import android.util.Log;

import org.app.enjoy.music.data.MusicData;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * parse cue file tool 
 * eg： 
 * File cueFile = new File("/sdcard/test.cue"); 
 * CueFileBean bean = CueUtil.parseCueFile(cueFile); 
 *
 PERFORMER "test" 
 TITLE "10.Moonlight+ShadowWinner" 
 FILE "10.Moonlight ShadowWinner.ape" WAVE 
 TRACK 01 AUDIO     
 TITLE "La lettre"      
 PERFORMER "Lara Fabian"    
 INDEX 01 00:00:00     
 TRACK 02 AUDIO      
 TITLE "Un ave maria"     
 PERFORMER "Lara Fabian"     
 INDEX 00 03:52:57     
 INDEX 01 03:52:99    
 TRACK 03 AUDIO 
 TITLE "Si tu n'as pas d'amour" 
 PERFORMER "Lara Fabian" 
 INDEX 00 08:50:49 
 INDEX 01 08:50:65 
 TRACK 04 AUDIO 
 TITLE "Il ne manquait que toi" 
 PERFORMER "Lara Fabian" 
 INDEX 00 12:36:17 
 INDEX 01 12:40:19 
 * @author xuweilin
 *
 */
public class CueUtil {
    private static String TAG = "CueUtil";
    /**
     * parse cue file 
     * @param cueFile file 
     * @return CueFileBean
     */
    public static CueFileBean parseCueFile(File cueFile, String musicPlayPath){
        String albumName = "";
        Long seekPosition = 0L;
        LineNumberReader reader = null;
        CueFileBean  cueFileBean = new CueUtil().new CueFileBean();
        ArrayList<MusicData> cueMusicList = new ArrayList<MusicData>();
        MusicData cueMusic = new MusicData();
        boolean parseSong = false;
        int songIndex = 0;
        try {
            reader = new LineNumberReader( new InputStreamReader(new FileInputStream(cueFile),Contsant.CHAR_ENCODING));
            while (true) {
                String s = new String();
                s = reader.readLine();
                if (s != null)
                {
                    if(!parseSong && s.trim().toUpperCase().startsWith("PERFORMER")){
                        cueFileBean.setPerformer(s.substring(s.indexOf("\"") + 1, s.lastIndexOf("\"")));
                    }
                    if(!parseSong && s.trim().toUpperCase().startsWith("TITLE")){
                        cueFileBean.setAlbumName(s.substring(s.indexOf("\"")+1, s.lastIndexOf("\"")));
                        albumName = cueFileBean.getAlbumName();
                    }
                    if(s.trim().toUpperCase().startsWith("FILE")){
                        cueFileBean.setFileName(s.substring(s.indexOf("\"")+1, s.lastIndexOf("\"")));
                    }
                    if(s.trim().toUpperCase().startsWith("TRACK")){
                        parseSong = true;
                        songIndex ++;
                    }
                    if(parseSong && s.trim().toUpperCase().startsWith("TITLE")){
                        cueMusic.setTitle(s.substring(s.indexOf("\"")+1, s.lastIndexOf("\"")));
                    }
                    if(parseSong && s.trim().toUpperCase().startsWith("PERFORMER")){
                        cueMusic.setArtist(s.substring(s.indexOf("\"")+1, s.lastIndexOf("\"")));
                    }
                    if(songIndex == 1 && s.trim().toUpperCase().startsWith("INDEX")){
                        String[] arrBegin =s.trim().split(" 01 ");
                       if(arrBegin != null && arrBegin.length > 1){
                           cueMusic.setIndexBegin(arrBegin[1].trim());
                       }
                    }
                    if(songIndex > 1 && s.trim().toUpperCase().startsWith("INDEX")){

                        if(s.trim().contains(" 00 ")){
                            cueMusicList.get(songIndex - 2).setIndexEnd(s.trim().split(" 00 ")[1].trim());
                        }
                        if(s.trim().contains(" 01 ")){
                            String[] arrBegin =s.trim().split(" 01 ");
                            if(arrBegin != null && arrBegin.length > 1){
                                cueMusic.setIndexBegin(arrBegin[1].trim());
                            }
//                            cueMusic.setIndexBegin(s.trim().split(" 01 ")[1].trim());
                            int size = cueMusicList.size();
                            if(size > 0){
                                cueMusicList.get(size - 1).setDuration((int)(getLongFromTimeByMin(cueMusic.getIndexBegin()) - seekPosition));
                            }
                            seekPosition = getLongFromTimeByMin(cueMusic.getIndexBegin()) + 1500;
                            cueMusic.setSeekPostion(seekPosition);
                        }
                    }
                    if(songIndex >= 1 && s.trim().toUpperCase().startsWith("INDEX") && s.trim().contains(" 01 ")){
                        cueMusic.setPath(musicPlayPath);
                        cueMusic.setAlbum(albumName);
                        cueMusicList.add(cueMusic);
                        cueMusic = new MusicData();
                    }
                }else{
                    cueFileBean.setSongs(cueMusicList);
                    break;
                }
            }

        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "UnsupportedEncodingException:"+e.getMessage());
        } catch (FileNotFoundException e) {
            Log.e(TAG, "FileNotFoundException:"+e.getMessage());
        }catch (IOException e) {
            Log.e(TAG, "IOException:"+e.getMessage());
        }finally{
            try{
                if(reader!=null ){
                    reader.close();
                }
            }
            catch(Exception e){
                Log.e(TAG, "Exception:"+e.getMessage());
            }
        }
        return cueFileBean;
    }


    /**
     * @param cueIso
     * @param musicPlayPath
     * @return
     *
    REM File created by SACD Extract, version: 0.38
    REM DATE 2004-02-03
    PERFORMER "Tsai Chin"
    TITLE "SACD : Tsai Chin"
    CATALOG 57708920
    FILE "SACD:///storage/emulated/legacy/Download/music/蔡琴 民歌+精选 SACD.iso" WAVE
    TRACK 01 AUDIO
    TITLE "Bei Wei Wang De Shi Guang"
    PERFORMER "Tsai Chin"
    ISRC TWA239600001
    INDEX 00 00:00:00
    INDEX 01 00:01:10
    TRACK 02 AUDIO
    TITLE "Gen Wo Shuo Ai Wo"
    PERFORMER "Tsai Chin"
    ISRC TWA239600002
    INDEX 00 02:42:30
    INDEX 01 02:46:42
    TRACK 03 AUDIO
    TITLE "Ni De Yan Shen"
    PERFORMER "Tsai Chin"
    ISRC TWA239600003
    INDEX 00 06:27:25
    INDEX 01 06:31:63
    TRACK 04 AUDIO
    TITLE "Xiang Ni De Shi Hou"
    PERFORMER "Tsai Chin"
    ISRC TWA239600004
    INDEX 00 10:39:27
    INDEX 01 10:43:58
    TRACK 05 AUDIO
    TITLE "Jue Zhai"
    PERFORMER "Tsai Chin"
    ISRC TWA239600005
    INDEX 00 14:52:28
    INDEX 01 14:56:51
    TRACK 06 AUDIO
    TITLE "Wu De Si Nian"
    PERFORMER "Tsai Chin"
    ISRC TWA239600006
    INDEX 00 19:35:16
    INDEX 01 19:39:20
    TRACK 07 AUDIO
    TITLE "Chu Sai Qu"
    PERFORMER "Tsai Chin"
    ISRC TWA239600007
    INDEX 00 23:27:28
    INDEX 01 23:32:16
    TRACK 08 AUDIO
    TITLE "Du Kou"
    PERFORMER "Tsai Chin"
    ISRC TWA239600008
    INDEX 00 27:20:19
    INDEX 01 27:24:01
    TRACK 09 AUDIO
    TITLE "Zen Mo Neng"
    PERFORMER "Tsai Chin"
    ISRC TWA239600009
    INDEX 00 31:07:14
    INDEX 01 31:10:69
    TRACK 10 AUDIO
    TITLE "Ne Xie Shi Ne Xie Ren"
    PERFORMER "Tsai Chin"
    ISRC TWA239700047
    INDEX 00 34:35:33
    INDEX 01 34:39:73
    TRACK 11 AUDIO
    TITLE "Ai Duan Qing Shang"
    PERFORMER "Tsai Chin"
    ISRC TWA239700043
    INDEX 00 39:00:24
    INDEX 01 39:04:70
    TRACK 12 AUDIO
    TITLE "Xin Tai Ji"
    PERFORMER "Tsai Chin"
    ISRC TWA239700042
    INDEX 00 43:49:22
    INDEX 01 43:53:37
    TRACK 13 AUDIO
    TITLE "Dian Liang Ni Hong Deng"
    PERFORMER "Tsai Chin"
    ISRC TWA239500002
    INDEX 00 48:28:28
    INDEX 01 48:33:08
    TRACK 14 AUDIO
    TITLE "Qia Shi Ni De Wen"
    PERFORMER "Tsai Chin"
    ISRC TWA239600010
    INDEX 00 52:57:12
    INDEX 01 53:00:60
    TRACK 15 AUDIO
    TITLE "Zai Ai Wo Yi Zi"
    PERFORMER "Tsai Chin"
    ISRC TWA239600011
    INDEX 00 57:13:29
    INDEX 01 57:17:43
    TRACK 16 AUDIO
    TITLE "Xie Mu Qu"
    PERFORMER "Tsai Chin"
    ISRC TWA239600012
    INDEX 00 60:29:03
    INDEX 01 60:32:56
     */
    public static CueFileBean parseCueFile(String cueIso, String musicPlayPath){
        String albumName = "";
        long seekPosition = 0L;
        long duration = 0;
        String strIndexTrack = "";
        LineNumberReader reader = null;
        CueFileBean  cueFileBean = new CueUtil().new CueFileBean();
        ArrayList<MusicData> cueMusicList = new ArrayList<MusicData>();
        MusicData cueMusic = new MusicData();
        boolean parseSong = false;
        int songIndex = 0;
        try {
            reader = new LineNumberReader( new InputStreamReader(new ByteArrayInputStream(cueIso.getBytes()),Contsant.CHAR_ENCODING));
            while (true) {
                String s = new String();
                s = reader.readLine();
                if (s != null)
                {
                    if(!parseSong && s.trim().toUpperCase().startsWith("PERFORMER")){
                        cueFileBean.setPerformer(s.substring(s.indexOf("\"") + 1, s.lastIndexOf("\"")));
                    }
                    if(!parseSong && s.trim().toUpperCase().startsWith("TITLE")){
                        cueFileBean.setAlbumName(s.substring(s.indexOf("\"")+1, s.lastIndexOf("\"")));
                        albumName = cueFileBean.getAlbumName();
                    }
                    if(s.trim().toUpperCase().startsWith("FILE")){
                        cueFileBean.setFileName(s.substring(s.indexOf("\"")+1, s.lastIndexOf("\"")));
                    }
                    if(s.trim().toUpperCase().startsWith("TRACK")){
                        strIndexTrack = s.substring("TRACK ".length() + 2, "TRACK ".length() + 5);
                        parseSong = true;
                        songIndex ++;
                    }
                    if(parseSong && s.trim().toUpperCase().startsWith("TITLE")){
                        cueMusic.setTitle(s.substring(s.indexOf("\"") + 1, s.lastIndexOf("\"")));
                    }
                    if(parseSong && s.trim().toUpperCase().startsWith("PERFORMER")){
                        cueMusic.setArtist(s.substring(s.indexOf("\"")+1, s.lastIndexOf("\"")));
                    }

                    if(songIndex > 0 && s.trim().toUpperCase().startsWith("INDEX")){

                        if(s.trim().contains(" 00 ")){
                            String[] arrBegin = s.trim().split(" 00 ");
                            if(arrBegin != null && arrBegin.length > 1){
                                if(songIndex > 1){
                                    cueMusicList.get(songIndex - 2).setIndexEnd(arrBegin[1].trim());
                                }
                                cueMusic.setIndexBegin(arrBegin[1].trim());
//                                LogTool.d("setIndexBegin:" + arrBegin[1].trim());
                            }
                        }else if(s.trim().contains(" 01 ")){
                            String[] arrEnd =s.trim().split(" 01 ");
                            if(arrEnd != null && arrEnd.length > 1){
                                cueMusic.setIndexEnd(arrEnd[1].trim());
//                                LogTool.d("setIndexEnd:" + arrEnd[1].trim());
                            }
                            if(musicPlayPath != null && musicPlayPath.endsWith(Contsant.DSD_ISO)){
                                String trackPath = Contsant.DSD_ISO_HEADER + musicPlayPath + ":"+strIndexTrack;
//                                LogTool.d(Contsant.DSD_ISO+"trackPath:"+ trackPath);
                                cueMusic.setPath(trackPath);
                                duration = getLongFromTime(cueMusic.getIndexEnd()) - getLongFromTime(cueMusic.getIndexBegin());
                                cueMusic.setDuration(duration);
                            }else{
                                cueMusic.setPath(musicPlayPath);
                                int size = cueMusicList.size();
                                if(size > 0){
                                    duration = getLongFromTime(cueMusic.getIndexBegin()) - seekPosition;
                                    cueMusicList.get(size - 1).setDuration(duration);
                                }
                                seekPosition = getLongFromTime(cueMusic.getIndexBegin()) + Contsant.DELAY_SEEK_HEADER;
                                cueMusic.setSeekPostion(seekPosition);
                            }
                        }
                    }
                    if(songIndex >= 1 && s.trim().toUpperCase().startsWith("INDEX") && s.trim().contains(" 01 ")){
                        /*if(musicPlayPath != null && musicPlayPath.endsWith(Contsant.DSD_ISO)){
                            String trackPath = Contsant.DSD_ISO_HEADER + musicPlayPath + ":"+strIndexTrack;
                            LogTool.d("trackPath:"+ trackPath);
                            cueMusic.setPath(trackPath);
                        }else{
                            cueMusic.setPath(musicPlayPath);
                        }*/

                        cueMusic.setAlbum(albumName);
                        cueMusicList.add(cueMusic);
                        cueMusic = new MusicData();
                        seekPosition = 0;
                        duration = 0;
                        strIndexTrack = "";
                    }
                }else{
                    cueFileBean.setSongs(cueMusicList);
                    break;
                }
            }

        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "UnsupportedEncodingException:"+e.getMessage());
        } catch (FileNotFoundException e) {
            Log.e(TAG, "FileNotFoundException:"+e.getMessage());
        }catch (IOException e) {
            Log.e(TAG, "IOException:"+e.getMessage());
        }finally{
            try{
                if(reader!=null ){
                    reader.close();
                }
            }
            catch(Exception e){
                Log.e(TAG, "Exception:"+e.getMessage());
            }
        }
        return cueFileBean;
    }
    /**
     * cue bean 
     */
    public class CueFileBean{
        private String musicPath; //播放路径
        private String performer; //performer  歌手
        private String albumName; //albumName  专辑名称
        private String fileName;  //fileName   文件名
        private ArrayList<MusicData> songs = null; //songs list
        public String getPerformer() {
            return performer;
        }
        public void setPerformer(String performer) {
            this.performer = performer;
        }
        public String getAlbumName() {
            return albumName;
        }
        public void setAlbumName(String albumName) {
            this.albumName = albumName;
        }
        public String getFileName() {
            return fileName;
        }
        public void setFileName(String fileName) {
            this.fileName = fileName;
        }
        public ArrayList<MusicData> getSongs() {
            return songs;
        }
        public void setSongs(ArrayList<MusicData> songs) {
            this.songs = songs;
        }

        public String getMusicPath() {
            return musicPath;
        }

        public void setMusicPath(String musicPath) {
            this.musicPath = musicPath;
        }
    }

    /**
     * */
    public static List<MusicData> getMusicListFromApe(MusicData music) {
        ArrayList<MusicData> musicDataList = null;
        if (music != null && music.path != null && music.path.endsWith(Contsant.DSD_APE)) {
            String cuePath = music.path.replace(Contsant.DSD_APE, Contsant.DSD_CUE);
            Log.i(TAG, "cuePath" + cuePath);
            File cueFile = new File(cuePath);
            if (cueFile.exists()) {
                CueFileBean cueFileBean = parseCueFile(cueFile, music.path);
                musicDataList =  cueFileBean.getSongs();
                int size = musicDataList.size();
                if(size > 1){
                    long preSeekPoistion = musicDataList.get(size - 2).getSeekPostion();
                    long duration = musicDataList.get(size - 2).getDuration();
                    musicDataList.get(size - 1).setDuration(music.getDuration() - preSeekPoistion -duration);
                }else if(size == 1){
                    musicDataList.get(size - 1).setDuration(music.getDuration());
                }
            }
        }
        return musicDataList;
    }

    /**
     *
     * */
    public static List<MusicData> getMusicsFromIso(String cueIso, String path) {
        ArrayList<MusicData> musicDataList;
            CueFileBean cueFileBean = parseCueFile(cueIso, path);
            musicDataList =  cueFileBean.getSongs();
        return musicDataList;
    }

    public static long getLongFromTimeByMin(String time){
        String[] my =time.split(":");
        int hour =Integer.parseInt(my[0]);
        int min =Integer.parseInt(my[1]);
        int sec = Integer.parseInt(my[2]);
        long totalSec = hour * 60*1000 + min * 1000 + sec;
        return totalSec;
    }

    public static long getLongFromTime(String time){
        if(time == null || TextUtils.isEmpty(time)){
            return 0;
        }
        String[] my =time.split(":");
        int hour =Integer.parseInt(my[0]);
        int min =Integer.parseInt(my[1]);
        int sec = Integer.parseInt(my[2]);
        long totalSec = hour * 60*60*1000 + min * 60*1000 + sec *1000;
        return totalSec;
    }

    //
    public static int toInt(byte[] b) {
        return ((b[3] << 24) + (b[2] << 16) + (b[1] << 8) + (b[0] << 0));
    }

    public static short toShort(byte[] b) {
        return (short)((b[1] << 8) + (b[0] << 0));
    }


    public static byte[] read(RandomAccessFile rdf, int pos, int length) throws IOException {
        rdf.seek(pos);
        byte result[] = new byte[length];
        for (int i = 0; i < length; i++) {
            result[i] = rdf.readByte();
        }
        return result;
    }

    public static String getFormatRateInfo(String path){
        String info = "";
        if(path != null && !TextUtils.isEmpty(path)){
            try {
                File f = new File(path);
                RandomAccessFile rdf  = new RandomAccessFile(f,"r");
                int musicHz = toInt(read(rdf, 24, 4)) / 1000;
                short musicBits = toShort(read(rdf, 34, 2));
                int musicRate = toInt(read(rdf, 28, 4)) / 1000;
                if(musicHz <= 0){
                    musicHz = 44;
                }
                if(musicBits <= 0){
                    musicBits = 16;
                }
                if(musicRate <= 0){
                    musicRate = 294;
                }
                info = musicHz + "KHZ/" + musicBits +"bits/" + musicRate+"Kbps";
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return info;
    }
    /*public static void main(String[] args) throws IOException {

        File f = new File("c:\\bong.wav");
        RandomAccessFile rdf = null;
        rdf = new RandomAccessFile(f,"r");

        System.out.println("audio size: " + toInt(read(rdf, 4, 4))); // 声音尺寸

        System.out.println("audio format: " + toShort(read(rdf, 20, 2))); // 音频格式 1 = PCM

        System.out.println("num channels: " + toShort(read(rdf, 22, 2))); // 1 单声道 2 双声道

        System.out.println("sample rate: " + toInt(read(rdf, 24, 4)));  // 采样率、音频采样级别 8000 = 8KHz

        System.out.println("byte rate: " + toInt(read(rdf, 28, 4)));  // 每秒波形的数据量

        System.out.println("block align: " + toShort(read(rdf, 32, 2)));  // 采样帧的大小

        System.out.println("bits per sample: " + toShort(read(rdf, 34, 2)));  // 采样位数

        rdf.close();

    }*/
}  