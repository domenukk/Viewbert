package co.dmnk.viewbert.example;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.OnClick;
import co.dmnk.viewbert.ActivityStarter;
import co.dmnk.viewbert.ViewComponent;

/**
 * Example ViewComponent that only shows some text.
 * Created by dcmai on 20.09.2016.
 */

public class ExampleViewComponent extends ViewComponent {

    private static final String TAG = "ExampleViewComponent";

    @BindView(R.id.text_top)
    TextView textTop;

    @BindView(R.id.text_bottom)
    TextView textBottom;

    /**
     * Create a new view component passing a context
     *
     * @param context The context.
     */
    public ExampleViewComponent(@NonNull Context context) {
        super(context);
    }

    /**
     * This constructor will usually be used for layout inflation, passing attributes.
     *
     * @param context
     * @param attrs
     */
    public ExampleViewComponent(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ExampleViewComponent(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //@BindView(R

    @Override
    public int getLayoutId() {
        return R.layout.vc_example;
    }

    @OnClick(R.id.button_start_browser)
    public void onButtonClick(Button button) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/domenukk/Viewbert")));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Log.d(TAG, "Attached to window. Can set all the cool stuff here.");
        if (!inActivity()) {
            Log.d(TAG, "Not in activity (this can happen when I run in a service/direclty in a window)");
        } else {
            Log.d(TAG, "Yoyoyo activity: " + getActivity());
        }
    }

    @Override
    public void onShow() {
        Log.d(TAG, "This view is being shown. I am visible: " + isVisible());
        textTop.setText("Hello");
        showSoftKeyboard();
    }

    @Override
    public void onHide() {
        Log.d(TAG, "View is being hidden");
        hideSoftKeyboard();
    }
}
