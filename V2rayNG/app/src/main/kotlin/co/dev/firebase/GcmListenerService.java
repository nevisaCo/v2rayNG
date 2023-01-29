package co.dev.firebase;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import co.nevisa.commonlib.Config;
import co.nevisa.commonlib.firebase.CloudMessagingController;

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
public class GcmListenerService extends FirebaseMessagingService {
    private static final String TAG = Config.TAG + "gls";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        Log.i(Config.TAG, "GcmListenerService > onMessageReceived: ");

        final Map<String, String> data = message.getData();
        JSONObject jsonObject = new JSONObject(data);

        try {
            new CloudMessagingController().onReceive(getApplicationContext(), jsonObject);

        } catch (JSONException e) {
            Log.e(TAG, "onMessageReceived: ", e);
        }

    }
}
