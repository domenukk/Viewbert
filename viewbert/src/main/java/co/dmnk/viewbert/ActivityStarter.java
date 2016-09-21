package co.dmnk.viewbert;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;
import android.view.View;
import android.view.Window;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class that calls an activity and returns the result.
 */
public class ActivityStarter extends Activity {

    public static final String EXTRA_START_ACITIVY_INTENT = "EXTRA_START_ACTIVITY_INTENT";
    public static final String EXTRA_REQUEST_ID = "EXTRA_REQUEST_ID";

    static final AtomicInteger requestIds = new AtomicInteger(0);

    private static final SparseArray<TaskCompletionSource<ActivityResult>> tcsById = new SparseArray<TaskCompletionSource<ActivityResult>>();
    private static final String EXTRA_OPTIONS_BUNDLE = "EXTRA_OPTIONS_BUNDLE";

    private static final Drawable TRANSPARENT_DRAWABLE = new ColorDrawable(Color.TRANSPARENT);

    private static volatile boolean initialized = false;
    private static final Object initLock = new Object();

    /**private static RestoredState {
     * //TODO: Figure out a way for Actiity return to work for destroyed apps
        @State int currentId;
    }**/

    /**
     * Restores from disk
     * @param context the context
     */
    private static void restore(Context context) {
        // TODO: PocketKnife.restoreInstanceState();
    }

    /**
     * Restores saved state if needed
     */
    private static void initIfNeeded(Context context) {
        if (!initialized) {
            synchronized (initLock) {
                if (!initialized) {
                    initialized = true;
                }
            }
        }

    }

    @NonNull
    private static Task<ActivityResult> startActivity(@NonNull Context context, @NonNull Intent intent,
                                                      @Nullable Bundle options, boolean flagNewTask) {
        initIfNeeded(context);

        int requestId = requestIds.getAndIncrement();

        Intent startTrampolin = new Intent(context, ActivityStarter.class);
        startTrampolin.putExtra(EXTRA_START_ACITIVY_INTENT, intent);
        startTrampolin.putExtra(EXTRA_REQUEST_ID, requestId);
        startTrampolin.putExtra(EXTRA_OPTIONS_BUNDLE, options);

        if (flagNewTask) {
            startTrampolin.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        TaskCompletionSource<ActivityResult> tcs = new TaskCompletionSource<>();
        tcsById.put(requestId, tcs);

        context.startActivity(startTrampolin);
        return tcs.getTask();
    }

    Set<Integer> externalIds = new HashSet<>();

    /**
     * This returns the id for a task, in case the calling Activity is about to be destroyed.
     * Implement this method together with {@link #popTaskForId(int) popTaskForId} popTaskForId(int) to get t
     *
     * @return the id for the task received.
     */
    public static int getIdForTask(Task<ActivityResult> task) {
        // TODO:
        // externalIds.add(id);
        return -1;
    }

    public static Task<ActivityResult> popIdForTask(int taskId) {
        // Pops the id for a given task.
        throw new UnsupportedOperationException("This part of ActivityStarter has not been implemented yet");
    }

    /**
     * Starts a new Activity using a trampolin activity to gather results.
     * For example using retrolambda, a call to pick contacts could look like this:
     * <pre>
     *     <code>
     * Intent pickContactIntent = new Intent(Intent.ACTION_PICK, Uri.parse("content://contacts"));
     * pickContactIntent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE); // Show user only contacts w/ phone numbers
     * ActivityStarter.startActivity(this, pickContactIntent).continueWithTask(task -> {
     *      ActivityResult result = task.getResult();
     *      Log.d(TAG, "Contact activity returned result: " + result);
     *          return null;
     * });
     *     </code>
     * </pre>
     *
     * @return a Task containing the result of the called activity.
     */
    @NonNull
    public static Task<ActivityResult> startActivity(@NonNull Activity activity, @NonNull Intent intent) {
        return startActivity(activity, intent, null, false);
    }

    /***
     * Calling this on a version below Jelly bean will ignore the `options`-bundle.
     *
     * @param activity The activity
     * @param intent   the intent
     * @param options  options how to start the activity. See Android docs.
     * @return A task that will include the results later.
     */
    @NonNull
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static Task<ActivityResult> startActivity(@NonNull Activity activity, @NonNull Intent intent, @Nullable Bundle options) {
        return startActivity(activity, intent, options, false);
    }

    @NonNull
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static Task<ActivityResult> startActivity(@NonNull Context context, @NonNull Intent intent, @Nullable Bundle options) {
        return startActivity(context, intent, options, true);
    }

    @NonNull
    public static Task<ActivityResult> startActivity(Context context, Intent intent) {
        return startActivity(context, intent, null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setBackgroundDrawable(TRANSPARENT_DRAWABLE);
        View transparentView = new View(this);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            transparentView.setBackground(TRANSPARENT_DRAWABLE);
        } else {
            //noinspection deprecation
            transparentView.setBackgroundDrawable(TRANSPARENT_DRAWABLE);
        }

        setContentView(transparentView);
        Intent myIntent = getIntent();
        Intent startActivityIntent = myIntent.getParcelableExtra(EXTRA_START_ACITIVY_INTENT);
        int requestId = myIntent.getIntExtra(EXTRA_REQUEST_ID, -1);
        Bundle options = myIntent.getBundleExtra(EXTRA_OPTIONS_BUNDLE);
        if (requestId < 0) {
            // Alternatively: We fired too many intents and the int overran. Which also shouldn't happen but leaving this comment here.
            throw new IllegalStateException("This Activity should never be started directly. Used the static 'startActivity' instead.");
        }
        if (options != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            startActivityForResult(startActivityIntent, requestId, options);
        } else {
            startActivityForResult(startActivityIntent, requestId);
        }
    }

/*
 * TODO: Do something like this instead? Fragments do it and it might be a cleaner solution than the trampolin.
 Instrumentation.ActivityResult ar = mInstrumentation.execStartActivity(
 3958                this, mMainThread.getApplicationThread(), mToken, fragment,
 3959                intent, requestCode, options);
 3960        if (ar != null) {
 3961            mMainThread.sendActivityResult(
 3962                mToken, fragment.mWho, requestCode,
 3963                ar.getResultCode(), ar.getResultData());
 **/

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        TaskCompletionSource<ActivityResult> taskCompletionSource = tcsById.get(requestCode);
        if (taskCompletionSource == null) {
            // Our App has been destroyed in the meantime.
            taskCompletionSource = new TaskCompletionSource<>();
            tcsById.put(requestCode, taskCompletionSource);
        } else {
            // Everything is fine. :)
            tcsById.remove(requestCode);
        }
        taskCompletionSource.setResult(new ActivityResult(resultCode, data));
        finish();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }
}
