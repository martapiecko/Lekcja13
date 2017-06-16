package com.example.x.lekcja13;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

/**
 * ZADANIE:
 * Przykład prezentuje uzycie Asynctask do pobierania pliku z servera.
 * Utworz odpowiedni Layout zawierajacy komponenty uzyte w tej klasie(SeekBar,Button).
 * UŻYJ ASYNCTASK  do wyswietlania postepu odtwarzania pliku dzwiekowego w widoku seekBar.
 * Pamietaj o dodaniu pozwolen i aktywnosci do pliku manifestu.
 *
 */
public class MainActivity extends Activity {

    String[] permissions = new String[]{
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };
    private boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            result = ContextCompat.checkSelfPermission(this, p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), 100);
            return false;
        }
        return true;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == 100) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // do something
            }
            return;
        }
    }
    // Button to download and play Music
    private Button btnPlayMusic;
    // Media Player Object
    private MediaPlayer mPlayer;
    private SeekBar seekBar;
    // Progress Dialog Object
    private ProgressDialog prgDialog;
    // Progress Dialog type (0 - for Horizontal progress bar)
    public static final int progress_bar_type = 0;
    // Music resource URL
    private static String file_url = "http://kolokwium.ugu.pl/pan-lodowego-ogroduSample.mp3";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Show Download Music Button
        btnPlayMusic = (Button) findViewById(R.id.btnProgressBar);
        // Download Music Button click listener
        btnPlayMusic.setOnClickListener(new View.OnClickListener() {
            // When Download Music Button is clicked
            public void onClick(View v) {
                // Disable the button to avoid playing of song multiple times
                btnPlayMusic.setEnabled(false);
                // Downloaded Music File path in SD Card
                File file = new File(Environment.getExternalStorageDirectory().getPath()+"/pan-lodowego-ogroduSample.mp3");
                // Check if the Music file already exists
                if (file.exists()) {
                    Toast.makeText(getApplicationContext(), "File already exist under SD card, playing Music", Toast.LENGTH_LONG).show();
                    // Play Music
                    playMusic();
                    // If the Music File doesn't exist in SD card (Not yet downloaded)
                } else {
                    Toast.makeText(getApplicationContext(), "File doesn't exist under SD Card, downloading Mp3 from Internet", Toast.LENGTH_LONG).show();
                    // Trigger Async Task (onPreExecute method)
                    new DownloadMusicfromInternet().execute(file_url);
                }
            }
        });
        checkPermissions();
    }

    // Show Dialog Box with Progress bar
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case progress_bar_type:
                prgDialog = new ProgressDialog(this);
                prgDialog.setMessage("Downloading Mp3 file. Please wait...");
                prgDialog.setIndeterminate(false);
                prgDialog.setMax(100);
                prgDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                prgDialog.setCancelable(false);
                prgDialog.show();
                return prgDialog;
            default:
                return null;
        }
    }

    // Async Task Class
    class DownloadMusicfromInternet extends AsyncTask<String, String, String> {

        // Show Progress bar before downloading Music
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Shows Progress Bar Dialog and then call doInBackground method
            showDialog(progress_bar_type);
        }

        // Download Music File from Internet
        @Override
        protected String doInBackground(String... f_url) {
            int count;
            try {
                URL url = new URL(f_url[0]);
                URLConnection conection = url.openConnection();
                conection.connect();
                // Get Music file length
                int lenghtOfFile = conection.getContentLength();
                // input stream to read file - with 8k buffer
                InputStream input = new BufferedInputStream(url.openStream(),10*1024);
                // Output stream to write file in SD card
                OutputStream output = new FileOutputStream(Environment.getExternalStorageDirectory().getPath()+"/pan-lodowego-ogroduSample.mp3");
                byte data[] = new byte[1024];
                long total = 0;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    // Publish the progress which triggers onProgressUpdate method
                    publishProgress("" + (int) ((total * 100) / lenghtOfFile));

                    // Write data to file
                    output.write(data, 0, count);
                }
                // Flush output
                output.flush();
                // Close streams
                output.close();
                input.close();
            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
            }
            return null;
        }

        // While Downloading Music File
        protected void onProgressUpdate(String... progress) {
            // Set progress percentage
            prgDialog.setProgress(Integer.parseInt(progress[0]));
        }

        // Once Music File is downloaded
        @Override
        protected void onPostExecute(String file_url) {
            // Dismiss the dialog after the Music file was downloaded
            dismissDialog(progress_bar_type);
            Toast.makeText(getApplicationContext(), "Download complete, playing Music", Toast.LENGTH_LONG).show();
            // Play the music
            playMusic();
        }
    }

    int currentPosition=0;
    private Handler mHandler = new Handler();

    // Play Music
    protected void playMusic(){
        // Read Mp3 file present under SD card
        Uri myUri1 = Uri.parse(Environment.getExternalStorageDirectory().getPath()+"/pan-lodowego-ogroduSample.mp3");
        seekBar = (SeekBar) findViewById(R.id.seekBar);

        mPlayer  = new MediaPlayer();
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mPlayer.setDataSource(getApplicationContext(), myUri1);
            mPlayer.prepare();
            // Start playing the Music file
            mPlayer.start();
            seekBar.setMax(mPlayer.getDuration()/1000);
            final int total=mPlayer.getDuration();

            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(final Void... params) {
                    while (mPlayer!=null && currentPosition < total) {
                        try {
                            Thread.sleep(1000);
                            currentPosition=mPlayer.getCurrentPosition();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        mHandler.post(new Runnable() {
                            public void run() {
                                long pos=1000L * currentPosition/total;
                                Log.d("thread pos", pos+"");
                                Log.e("thread pos", total+"");
                                seekBar.setProgress((int)(pos));
                            }
                        });
                    }
                    return null;
                }

            }.execute();
            //ZADANIE TUTAJ UŻYJ ASYNCTASK  do wyświetlania postępu odtwarzania pliku dzwiękowego w widoku seekBar!!!
            
        } catch (IllegalArgumentException e) {
            Toast.makeText(getApplicationContext(), "You might not set the URI correctly!",	Toast.LENGTH_LONG).show();
        } catch (SecurityException e) {
            Toast.makeText(getApplicationContext(),	"URI cannot be accessed, permissed needed",	Toast.LENGTH_LONG).show();
        } catch (IllegalStateException e) {
            Toast.makeText(getApplicationContext(),	"Media Player is not in correct state",	Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(),	"IO Error occured",	Toast.LENGTH_LONG).show();
        }
    }
}