package com.applozic.mobicomkit.api;

import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.applozic.mobicomkit.api.account.user.MobiComUserPreference;
import com.applozic.mobicomkit.api.account.user.User;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;


/**
 * Created by devashish on 28/11/14.
 */
public class HttpRequestUtils {

    private Context context;

    private static final String TAG = "HttpRequestUtils";

    private static String SOURCE_HEADER = "Source";

    private static String SOURCE_HEADER_VALUE = "1";

    public static String APPLICATION_KEY_HEADER = "Application-Key";

    public static String USERID_HEADER = "UserId-Enabled";

    public static String USERID_HEADER_VALUE = "true";

    public static String DEVICE_KEY_HEADER = "Device-Key";

    public static String APP_MODULE_NAME_KEY_HEADER = "App-Module-Name";

    public static String ACCESS_TOKEN = "Access-Token";


    public HttpRequestUtils(Context context) {
        this.context = context;
    }

    private void log(String message) {
        Log.i(TAG, message);
    }

    public String postData(PasswordAuthentication credentials, String urlString, String contentType, String accept, String data) throws Exception {
        Log.i(TAG, "Calling url: " + urlString);
        HttpURLConnection connection;
        URL url;
        try {
            url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);

            if (!TextUtils.isEmpty(contentType)) {
                connection.setRequestProperty("Content-Type", contentType);
            }
            if (!TextUtils.isEmpty(accept)) {
                connection.setRequestProperty("Accept", accept);
            }
            addGlobalHeaders(connection);
            connection.connect();

            if (connection == null) {
                return null;
            }

            byte[] dataBytes = data.getBytes("UTF-8");
            DataOutputStream os = new DataOutputStream(connection.getOutputStream());
            os.write(dataBytes);
            os.flush();
            os.close();
            BufferedReader br = null;
            if(connection.getResponseCode() == 200){
                InputStream inputStream = connection.getInputStream();
                br = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            }
            StringBuilder sb = new StringBuilder();
            try {
                String line;if(br != null){
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.i(TAG, "Response: " + sb.toString());
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.e(TAG, "Http call failed");
        return null;
    }

    public String getStringFromUrl(String url) throws Exception {
     /*   BufferedReader br;
        br = new BufferedReader(new InputStreamReader(getInputStreamFromUrl(url), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        try {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();*/
        return null;
    }

    public String postJsonToServer(String StringUrl, String data) throws Exception {
        HttpURLConnection connection;
        URL url = new URL(StringUrl);
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.connect();

        byte[] dataBytes = data.getBytes("UTF-8");
        DataOutputStream os = new DataOutputStream(connection.getOutputStream());
        os.write(dataBytes);
        os.flush();
        os.close();
        BufferedReader br = null;
        if(connection.getResponseCode() == HttpURLConnection.HTTP_OK){
            InputStream inputStream = connection.getInputStream();
            br = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        }else {
            Log.i(TAG,"Response code for post json is :"+connection.getResponseCode());
        }
        StringBuilder sb = new StringBuilder();
        try {
            String line;
            if(br != null){
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i(TAG, "Response: " + sb.toString());
        return sb.toString();
    }

    public String getStringFromUrlWithPost(String url, String data) throws Exception {
       /* HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(url);
        httppost.addHeader("Content-Type", "application/xml");
        addGlobalHeaders(httppost);
        HttpEntity entity = new StringEntity(data, "UTF-8");
        httppost.setEntity(entity);
        HttpResponse httpResponse = httpclient.execute(httppost);
        String response = EntityUtils.toString(httpResponse.getEntity());
        log("response for post call is: " + response);
        return response;*/
        return  null;
    }

    public String getResponse(PasswordAuthentication credentials, String urlString, String contentType, String accept) {
        Log.i(TAG, "Calling url: " + urlString);

        HttpURLConnection connection = null;
        URL url;

        try {
            url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setRequestMethod("GET");
            connection.setUseCaches(false);
            connection.setDoInput(true);

            if (!TextUtils.isEmpty(contentType)) {
                connection.setRequestProperty("Content-Type", contentType);
            }
            if (!TextUtils.isEmpty(accept)) {
                connection.setRequestProperty("Accept", accept);
            }
            addGlobalHeaders(connection);
            connection.connect();

            if (connection == null) {
                return null;
            }
            BufferedReader br = null;
            if(connection.getResponseCode() == HttpURLConnection.HTTP_OK){
                InputStream inputStream = connection.getInputStream();
                br = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            }else {
                Log.i(TAG,"Response code for getResponse is  :"+connection.getResponseCode());
            }

            StringBuilder sb = new StringBuilder();
            try {
                String line;
                if (br != null){
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.d(TAG, "Response: " + sb.toString());
            return sb.toString();
        } catch (ConnectException e) {
            Log.i(TAG, "failed to connect Internet is not working");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public void addGlobalHeaders(HttpURLConnection connection) {
        try {
            connection.setRequestProperty(APPLICATION_KEY_HEADER, MobiComKitClientService.getApplicationKey(context));
            connection.setRequestProperty(SOURCE_HEADER, SOURCE_HEADER_VALUE);
            connection.setRequestProperty(USERID_HEADER, USERID_HEADER_VALUE);
            connection.setRequestProperty(DEVICE_KEY_HEADER, MobiComUserPreference.getInstance(context).getDeviceKeyString());
            Short authenticationType = Short.valueOf(MobiComUserPreference.getInstance(context).getAuthenticationType());
            if(User.AuthenticationType.APPLOZIC.getValue() == authenticationType ){
                connection.setRequestProperty(ACCESS_TOKEN, MobiComUserPreference.getInstance(context).getPassword());
            }

            if(MobiComKitClientService.getAppModuleName(context)!=null){
                connection.setRequestProperty(APP_MODULE_NAME_KEY_HEADER,MobiComKitClientService.getAppModuleName(context));
            }

            MobiComUserPreference userPreferences = MobiComUserPreference.getInstance(context);
            if (userPreferences.isRegistered()) {
                String userCredentials = getCredentials().getUserName() + ":" +String.valueOf(getCredentials().getPassword());
                String basicAuth = "Basic " + Base64.encodeToString(userCredentials.getBytes(), Base64.NO_WRAP);
                connection.setRequestProperty("Authorization", basicAuth);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public PasswordAuthentication getCredentials() {
        MobiComUserPreference userPreferences = MobiComUserPreference.getInstance(context);
        if (!userPreferences.isRegistered()) {
            return null;
        }
        return new PasswordAuthentication(userPreferences.getUserId(), userPreferences.getDeviceKeyString().toCharArray());
    }

}
