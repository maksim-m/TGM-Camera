package me.maxdev.tgmcamera.media;

import android.content.Context;
import android.content.Intent;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

/**
 * Created by Max on 06.05.2016.
 */
public class VideoFileObserver extends FileObserver {

    public static final String BROADCAST_WRITE_FINISHED = "WriteFinished";
    public static final String EXTRA_FILE_NAME_KEY = "VideoFileName";
    private static final String LOG_TAG = "VideoFileObserver";

    private Context context;
    private String path;

    public VideoFileObserver(Context context, String path, int mask) {
        super(path, mask);
        this.path = path;
        this.context = context;
    }

    @Override
    public void onEvent(final int event, final String path) {
        Log.d(LOG_TAG, "onEvent(" + event + ", " + path + ");");
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(LOG_TAG, "run()");
                if (event == CLOSE_WRITE && path.endsWith(".mp4")) {
                    VideoFileObserver.this.path = path;
                    sendWriteFinishedBroadcast();
                }
            }
        });

    }

    private void sendWriteFinishedBroadcast() {
        Intent intent = new Intent(BROADCAST_WRITE_FINISHED);
        intent.putExtra(EXTRA_FILE_NAME_KEY, path);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
