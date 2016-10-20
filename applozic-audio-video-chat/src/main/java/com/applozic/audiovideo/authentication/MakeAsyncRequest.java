package com.applozic.audiovideo.authentication;


import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

/**
 * Created by anuranjit on 16/7/16.
 */
public class MakeAsyncRequest extends AsyncTask<String, Integer, String> {
    private static final String TAG = MakeAsyncRequest.class.getName();
    public TokenGeneratorCallback callback;
    private Context context;

    public MakeAsyncRequest(TokenGeneratorCallback callback, Context context) {
        this.callback = callback;
        this.context = context;

    }

    @Override
    protected void onPreExecute() {
    }


    @Override
    protected String doInBackground(String... args) {
        Log.i(TAG, "Generating token.");

        TokenGenClientService service = new TokenGenClientService(context);
        return service.getGeneratedToken();
    }

    @Override
    protected void onPostExecute(String result) {
        Log.i(TAG, "Generating token: " + result);
        callback.onNetworkComplete(result);
    }

}