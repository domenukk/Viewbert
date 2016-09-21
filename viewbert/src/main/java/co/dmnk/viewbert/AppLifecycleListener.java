package co.dmnk.viewbert;

import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;


/**
 * A service that is supposed to listen to app lifecycle events.
 * Created by dcmai on 24.08.2016.
 */
public class AppLifecycleListener extends Service {

    private static final String TAG = "AppLifecycleListener";
    private boolean started = false;
    //LruCache<Class<? extends ViewComponent>, String> cacheNames; // Using an indirection as LRU Cache here because we need more than one instance of a view sometimes.
    //HashMap<String, ViewComponent> viewCache = new HashMap<>();

    public AppLifecycleListener() {
        // TODO: Get cache size and all that stuff here, according to freemem (?).
        //cacheNames = new LruCache<>(300);
    }

    /**
     * This does not seem to get called.
     */
    @Override
    public void onDestroy() {
        Log.d(TAG, "App finishing");
        super.onDestroy();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        /*
       When your app is running:
TRIM_MEMORY_RUNNING_MODERATE
The device is beginning to run low on memory. Your app is running and not killable.
TRIM_MEMORY_RUNNING_LOW
The device is running much lower on memory. Your app is running and not killable, but please release unused resources to improve system performance (which directly impacts your app's performance).
TRIM_MEMORY_RUNNING_CRITICAL
The device is running extremely low on memory. Your app is not yet considered a killable process, but the system will begin killing background processes if apps do not release resources, so you should release non-critical resources now to prevent performance degradation.
When your app's visibility changes:
TRIM_MEMORY_UI_HIDDEN
Your app's UI is no longer visible, so this is a good time to release large resources that are used only by your UI.
When your app's process resides in the background LRU list:
TRIM_MEMORY_BACKGROUND
The system is running low on memory and your process is near the beginning of the LRU list. Although your app process is not at a high risk of being killed, the system may already be killing processes in the LRU list, so you should release resources that are easy to recover so your process will remain in the list and resume quickly when the user returns to your app.
TRIM_MEMORY_MODERATE
The system is running low on memory and your process is near the middle of the LRU list. If the system becomes further constrained for memory, there's a chance your process will be killed.
TRIM_MEMORY_COMPLETE
The system is running low on memory and your process is one of the first to be killed if the system does not recover memory now. You should release absolutely everything that's not critical to resuming your app state.
To support API levels lower than 14, you can use the onLowMemory() method as a fallback that's roughly equivalent to the TRIM_MEMORY_COMPLETE level.
          */

        switch (level) {
            // In the foreground
            case TRIM_MEMORY_RUNNING_MODERATE:
                //1. The device is beginning to run low on memory. Your app is running and not killable.
                Log.d(TAG, "Memory moderate");
                break;
            case TRIM_MEMORY_RUNNING_LOW:
                //2. The device is running much lower on memory. Your app is running and not killable, but please release unused resources to improve system performance (which directly impacts your app's performance).
                Log.d(TAG, "Memory low");
                break;
            case TRIM_MEMORY_RUNNING_CRITICAL:
                //3. The device is running extremely low on memory. Your app is not yet considered a killable process, but the system will begin killing background processes if apps do not release resources, so you should release non-critical resources now to prevent performance degradation.
                Log.d(TAG, "Memory critical");
                break;
            // When visibility changes
            case TRIM_MEMORY_UI_HIDDEN:
                // Your app's UI is no longer visible, so this is a good time to release large resources that are used only by your UI.
                Log.d(TAG, "Ui Hidden");
                break;
            // App in background
            case TRIM_MEMORY_BACKGROUND:
                // 1. The system is running low on memory and your process is near the beginning of the LRU list. Although your app process is not at a high risk of being killed, the system may already be killing processes in the LRU list, so you should release resources that are easy to recover so your process will remain in the list and resume quickly when the user returns to your app.
                Log.d(TAG, "Backgroud");
                break;
            case TRIM_MEMORY_MODERATE:
                // 2. The system is running low on memory and your process is near the middle of the LRU list. If the system becomes further constrained for memory, there's a chance your process will be killed.
                Log.d(TAG, "Moderate bg");
                break;
            case TRIM_MEMORY_COMPLETE:
                // 3. The system is running low on memory and your process is one of the first to be killed if the system does not recover memory now. You should release absolutely everything that's not critical to resuming your app state.
                // To support API levels lower than 14, you can use the onLowMemory() method as a fallback that's roughly equivalent to the TRIM_MEMORY_COMPLETE level.
                Log.d(TAG, "Very badly in need of ram.");
                break;
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.d(TAG, "OnLowMem");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "New Config: " + newConfig);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (!started) {
            started = true;
            Log.d(TAG, "App started");
        }


        //TODO: Not doing anything at the moment. No need to run.
        stopSelf();

        return START_NOT_STICKY;
    }
}
