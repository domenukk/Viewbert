package co.dmnk.viewbert;

import android.content.Intent;
import android.support.annotation.NonNull;

/**
 * Result after an activity succeeded (or failed)
 */
public class ActivityResult {

    private final int resultCode;
    private final @NonNull Intent data;

    public ActivityResult(int resultCode, @NonNull Intent data) {
        this.resultCode = resultCode;
        this.data = data;
    }

    /**
     * The data intent is returned by onActivityResult and may contain extras.
     * @return The returned data intent
     */
    @NonNull
    public Intent getData() {
        return data;
    }

    /**
     * The Resultcode returned by the acitivyt
     * @return something like Activity.RESULT_OK etc.
     */
    public int getResultCode() {
        return resultCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ActivityResult result = (ActivityResult) o;

        if (resultCode != result.resultCode) return false;
        return data != null ? data.equals(result.data) : result.data == null;

    }

    @Override
    public String toString() {
        return "ActivityResult{" +
                "resultCode=" + resultCode +
                ", data=" + data +
                '}';
    }

    @Override
    public int hashCode() {
        int result = resultCode;
        result = 31 * result + (data != null ? data.hashCode() : 0);
        return result;
    }
}
