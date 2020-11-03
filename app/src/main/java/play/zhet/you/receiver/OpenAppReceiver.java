package play.zhet.you.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import play.zhet.you.activity.MainActivity;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class OpenAppReceiver extends BroadcastReceiver {
    private static final String TAG = OpenAppReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "onReceive");
        intent = new Intent(context, MainActivity.class);
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
