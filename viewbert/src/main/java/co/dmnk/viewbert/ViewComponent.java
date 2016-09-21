package co.dmnk.viewbert;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * A baseview to use for custom views with layout.
 * <p>
 * This view forwards everything to its child view and framelayout is used for simplicity.
 * Try not to rely on framelayout specific methods or behaviours since this may change in the future
 * (since nested view trees are negatively affecting performance)
 */
public abstract class ViewComponent extends FrameLayout {

    private static final String TAG = "ViewComponent";
    private static final WeakHashMap<Integer, ViewComponent> componentsById = new WeakHashMap<>();
    private static final Map<Resources.Theme, Integer> themeFromId = new HashMap<>();
    private static AtomicInteger currentInstanceId = new AtomicInteger(0);
    // Cache all parents. This way we don't have to traverse pointers. #speed
    ArrayList<View> parents = null;
    private boolean isVisible = false;
    private Unbinder unbinder = null;
    // the cached activity instance
    private WeakReference<Activity> activity = null;
    private boolean inOverlay = false; // Returns true if the view is shown in an overlay
    private final BroadcastReceiver systemButtonReciever = new BroadcastReceiver() {
        static final String EXTRA_SYSTEM_DIALOG_REASON = "reason";
        @SuppressWarnings("unused")
        static final String SYSTEM_DIALOG_REASON_GLOBAL_ACTIONS = "globalactions";
        @SuppressWarnings("unused")
        static final String SYSTEM_DIALOG_REASON_RECENT_APPS = "recentapps";
        @SuppressWarnings("unused")
        static final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                String reason = intent.getStringExtra(EXTRA_SYSTEM_DIALOG_REASON);
                if (reason != null) {
                    Log.i(TAG, "receive action: " + action + " - reason: " + reason);
                    hide();
                }
            }
        }
    };
    private int instanceId = currentInstanceId.getAndIncrement();
    // Viewtree observer to see if we are hidden or shown.
    private ViewTreeObserver.OnGlobalLayoutListener visibilityListener = () -> {
        boolean willBeVisible = getRealVisibility();
        if (isVisible != willBeVisible) {
            isVisible = willBeVisible;
            if (isVisible) {
                onShow();
            } else {
                onHide();
            }
        }
        if (isVisible && inOverlay) {
            handleOverlayMethods();
        }
    };
    private Task<Object> task = null;
    private AttributeSet attrs = null;

    /**
     * Create a new view component passing a context
     * @param context The context.
     */
    public ViewComponent(@NonNull Context context) {
        super(getNonActivityContext(context));
        _init(context);
    }

    /**
     * This constructor will usually be used for layout inflation, passing attributes.
     * @param context
     * @param attrs
     */
    public ViewComponent(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(getNonActivityContext(context), attrs);
        this.attrs = attrs;
        _init(context);
    }

    public ViewComponent(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(getNonActivityContext(context), attrs, defStyleAttr);
        this.attrs = attrs;
        _init(context);
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ViewComponent(@NonNull Context context, @Nullable AttributeSet attrs,
                         int defStyleAttr, @StyleRes int defStyleRes) {
        super(context.getApplicationContext(), attrs, defStyleAttr, defStyleRes);
        this.attrs = attrs;
        _init(context);
    }

    /**
     * The attributes passed into the constructor
     * @return Attrs passed into the constructor
     */
    @Nullable
    public AttributeSet getAttrs() {
        return attrs;
    }

    /**
     * Gets components by their unique instance id. This is not the layout id.
     *
     * @param instanceId the id of an component instance.
     * @return the component or null if id doesn't exist or ViewComponents was already collected
     */
    @SuppressWarnings("unused")
    protected static ViewComponent getComponentById(int instanceId) {
        return componentsById.get(instanceId);
    }

    /**
     * Try to get current theme to set it on the new context if applicable. Hackish workaround using reflection.
     * This is cached for subsequent requests.
     *
     * @param context the context
     * @return the theme Id or 0, if none was found.
     */
    @StyleRes
    private static int getThemeIdFromContext(@NonNull Context context) {
        Resources.Theme theme = context.getTheme();
        Integer themeResId = themeFromId.get(theme);
        if (themeResId != null) {
            return themeResId;
        }
        themeResId = 0;
        try {
            Class<?> clazz = Context.class;
            Method method = clazz.getMethod("getThemeResId");
            method.setAccessible(true);
            themeResId = (Integer) method.invoke(context);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get theme resource ID", e);
        }
        themeFromId.put(theme, themeResId);
        return themeResId;
    }

    /**
     * Ensures the context is not leaking the activity.
     *
     * @param context the context to test
     * @return The context if it is not instance of activity, the base context otherwise.
     */
    @NonNull
    public static Context getNonActivityContext(@NonNull Context context) {
        if (getActivityFromContext(context) == null) {
            // Not an activity context. We can savely use this one
            return context;
        }
        Context appContext = context.getApplicationContext();
        @StyleRes int themeId = getThemeIdFromContext(context);
        if (themeId != 0) {
            appContext.setTheme(themeId);
        } else {
            Log.i(TAG, "ThemeId was null for context.");
        }
        // We'll have to use the base context here.
        return context.getApplicationContext();
    }

    /**
     * Returns activity or null
     *
     * @param context the context to check
     * @return The Activity inside the context or null if the context does not contain an activity.
     */
    @Nullable
    private static Activity getActivityFromContext(@Nullable Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    /**
     * Convenience method to get the Activity to a view that's a child of a view Component
     *
     * @param viewComponentChild the child view
     * @return the activity, extracted from the parent viewComponent, or null if either there is no activity or no parent viewComponent
     */
    @SuppressWarnings("unused")
    @Nullable
    public static Activity getActivityFromViewComponentChild(View viewComponentChild) {
        ViewParent parent = viewComponentChild.getParent();
        while (parent instanceof View) {
            if (parent instanceof ViewComponent) {
                return ((ViewComponent) parent).getActivity();
            }
            parent = parent.getParent();
        }
        return null;
    }

    protected int getInstanceId() {
        return instanceId;
    }

    /**
     * Hide this view.
     *
     * @return A task, in case we want to run animations
     */
    @NonNull
    public Task<ViewComponent> hide() {
        if (inOverlay) {
            return hideAsOverlay();
        } else {
            setVisibility(GONE);
        }
        return Tasks.forResult(this);
    }

    private void handleOverlayMethods() {
        // TODO: Check for keyboard, call
        onSoftKeyboardShow();
        onSoftKeyboardHide();
    }

    /**
     * Gets called when the softkeybaord is being shown and this view is shown as overlay.
     */
    private void onSoftKeyboardHide() {
        //TODO
    }

    /**
     * Returns the task returned from initWithTask
     *
     * @return the task returned from initWithTask
     */
    @SuppressWarnings("unused")
    @NonNull
    public Task<Object> getInitTask() {
        return task;
    }

    @Override
    public void onScreenStateChanged(int screenState) {
        //Log.d(TAG, "ScreenState " + screenState);
        super.onScreenStateChanged(screenState);
    }

    @Override
    @CallSuper
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        // TODO: Is this really correct? try out Android N Multiwindow.
        if (!hasFocus()) {
            isVisible = false;
            onHide();
        }
        //Log.d(TAG, "IsShown: " + isShown());
    }

    /**
     * Traverse all parents to see if they are visible.
     *
     * @return true if visible, false otherwise
     */
    private boolean getRealVisibility() {
        if (parents == null || getVisibility() != VISIBLE) {
            return false;
        }
        for (View parent : parents) {
            if (parent.getVisibility() != VISIBLE) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if we are inside an activity
     *
     * @return if this is in an activity. Fase for overlay.
     */
    public boolean inActivity() {
        return getActivity() != null;
    }

    /**
     * The layout to inflate
     *
     * @return the layout id to inflate or -1 if not set.
     */
    @LayoutRes
    public int getLayoutId() {
        return -1;
    }

    /**
     * the real init. :)
     *
     * @param context the original context passed into this function. We'll try to extract the activity from it.
     */
    private void _init(Context context) {
        activity = new WeakReference<>(getActivityFromContext(context));

        Intent cacheService = new Intent(context, AppLifecycleListener.class);
        context.startService(cacheService);

        if (getLayoutId() != -1) {
            // Using getContext() here because that we we never keep the reference to an Activity.
            inflate(getContext(), getLayoutId(), this);
        }

        //TODO: We don't want to bind this early, if we reuse the view somewhere else later.
        unbinder = ButterKnife.bind(this);

        componentsById.put(getInstanceId(), this);

        init();
        task = initWithTask();
    }

    /**
     * Overwrite this method if you want to change the layout params when showing this view as overlay..
     *
     * @return the WindowManager LayoutParams used in shoAsOverlay()
     */
    @NonNull
    public WindowManager.LayoutParams getParamsForOverlay() {
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.CENTER;
        params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN;
        return params;
    }

    /**
     * Shows this as an overlay.
     * This can be used for Chatheads and many more.
     * This will throw all kinds of exceptions if the view is already attached to an activity.
     */
    @SuppressWarnings("unused")
    public void showAsOverlay() {
        WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        windowManager.addView(this, getParamsForOverlay());
        inOverlay = true;
        IntentFilter braodcastFilter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        getContext().registerReceiver(systemButtonReciever, braodcastFilter);
    }

    /**
     * This gets called when the view is in an overlay and a system button is clicked.
     */
    @SuppressWarnings("unused")
    public void onSystemButtonClicked() {
        if (isAttached() && inOverlay) {
            hide();
        }
    }

    @NonNull
    protected Task<ViewComponent> hideAsOverlay() {
        WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        windowManager.removeView(this);
        inOverlay = false;
        getContext().unregisterReceiver(systemButtonReciever);
        return Tasks.forResult(this);
    }

    @SuppressWarnings("unused")
    public void removeAsOverlay() {
        //TODO
    }

    /**
     * will be called if this view is shown as an Overlay and the keyboard is shown.
     * It uses simple heuristics so it may break. Please issue pull requests if you come up with better solutions.
     */
    private void onSoftKeyboardShow() {
        //TODO
    }

    /**
     * Hides the soft keyboard
     */
    @SuppressWarnings("unused")
    public void hideSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getWindowToken(), 0);
    }

    /**
     * Shows the soft keyboard
     */
    @SuppressWarnings("unused")
    public void showSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT);
    }

    /**
     * This will indicate if the ViewComponent ViewCache should be enabled for this view.
     * You might want to disable this option to start with a new view instance everytime.
     *
     * @return true if cache is enabled (default), else false
     */
    @SuppressWarnings("unused")
    public boolean cacheEnabled() {
        return true;
    }

    /**
     * This method will get called once, even before the layout is bound
     * For everything else, use onAttach and onDetach instead.
     */
    public void init() {
    }

    /**
     * This will be called after init and can return a Task. The task can then be used in onShowTaskCompleted
     *
     * @return A Task
     */
    @NonNull
    public Task<Object> initWithTask() {
        return Tasks.forResult(null);
    }

    /**
     * Called when the view is hidden
     */
    public void onHide() {
        //Log.d(TAG, "OnHide");
    }

    /**
     * Called when the view is being shown
     */
    public void onShow() {
        // Log.d(TAG, "OnShow");
    }

    /**
     * This method is being called when the view is first attached to the window.
     * Your views will be bound at this point. Do all initialization here.
     * Call super.onAttachedToWindow in the first statement.
     */
    @Override
    @CallSuper
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        parents = new ArrayList<>();
        ViewParent parent = getParent();
        while (parent != null && parent instanceof View) {
            View group = (View) parent;
            parents.add(group);

            parent = group.getParent();
        }

        if (unbinder == null) {
            unbinder = ButterKnife.bind(this);
        }

        getViewTreeObserver().addOnGlobalLayoutListener(visibilityListener);
    }

    /**
     * @return if this view is currently attached, VISIBLE and all the parents are visible.
     * It does not handle overlapping views or the position on the screen.
     * For example in a listview, this may well show wrong results.
     */
    public boolean isVisible() {
        // isVisible gets set in the global layout listener.
        return parents != null && isVisible;
    }

    /**
     * This method is being called whenever this view is detached from a window.
     * Use this to save everything etc.
     */
    @CallSuper
    @Override
    protected void onDetachedFromWindow() {
        //Log.d(TAG, "onDetachedFromWindow");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            getViewTreeObserver().removeOnGlobalLayoutListener(visibilityListener);
        } else {
            //noinspection deprecation
            getViewTreeObserver().removeGlobalOnLayoutListener(visibilityListener);
        }
        parents = null;
        if (isVisible) {
            isVisible = false;
            onHide();
        }
        if (unbinder != null) {
            unbinder.unbind();
            unbinder = null;
        }
        super.onDetachedFromWindow();
    }

    /**
     * Returns true if this view is currently attached somewhere.
     *
     * @return true if attached, false otherwise.
     */
    public boolean isAttached() {
        return parents != null;
    }

    /**
     * Starts an activity and returns a result task.
     * The task can be ignored if the result is not needed..
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @NonNull
    public Task<ActivityResult> startActivity(Intent intent, Bundle options) {
        Activity activity = getActivity();
        if (activity != null) {
            return ActivityStarter.startActivity(activity, intent, options);
        }
        return ActivityStarter.startActivity(getContext(), intent, options);
    }

    /**
     * Starts an activity and returns a result task.
     * The task can be ignored if the result is not needed..
     */
    @NonNull
    public Task<ActivityResult> startActivity(Intent intent) {
        return startActivity(intent, null);
    }

    /**
     * Gets the current activity, either set via setActivity, passed in the construcor as context or found from the parent views.
     * The activity is only stored as weak reference and this method will return null if  the activity is not avaliable.
     *
     * @return the current Acitivity or null, if it was not set or destroyed.
     */
    @Nullable
    public Activity getActivity() {
        Activity ret = activity.get();
        if (ret == null) {
            for (View parent : parents) {
                if (parent instanceof ViewComponent) {
                    ret = getActivity();
                    break;
                }
                ret = getActivityFromContext(parent.getContext());
                if (ret != null) {
                    break;
                }
            }
            activity = new WeakReference<>(ret);
        }
        return ret;
    }

    /**
     * Sets an activity. This should for example be called in onCreate.
     * It's best practice to use this method instead of passing the activity in the constructor.
     *
     * @param activity the Activity to set.
     */
    public void setActivity(Activity activity) {
        this.activity = new WeakReference<>(activity);
    }

    /**
     * Convenience method to get a string from resources
     * @param resId the resource id to get
     * @return the String
     */
    public String getString(@StringRes int resId) {
        return getContext().getString(resId);
    }
}
