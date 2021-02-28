package com.example.shiftwidget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;

import androidx.annotation.RequiresApi;

import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequests;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.cookie.BasicClientCookie;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.io.CloseMode;

import java.net.URLEncoder;
import java.security.Provider;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmartshiftResourceAccess extends Observable {
    BasicCookieStore cookieStore;
    Context context;
    String resource;
    String loginCredentials;
    ArrayList<ShiftData> result;

    final String loginUri = "https://app.smartshift.com.au/SmartShift/login/index.asp";
    final String dataUri = "https://app.smartshift.com.au/SmartShift/staff/staff_access/viewShifts.asp";
    final String pingUri = "https://app.smartshift.com.au/SmartShift/";

    ArrayList<Observer> observers;

    String dataPattern = "<td [^>]*>([^<]*)<\\/td>";
    String loginPattern = "message-failed";

    public SmartshiftResourceAccess(Context context) {
        cookieStore = new BasicCookieStore();
        this.context = context;

        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);

        String email = prefs.getString("user_email", "");
        String password = prefs.getString("user_password", "");

        String encodedEmail = URLEncoder.encode(email);
        String encodedPassword = URLEncoder.encode(password);

        this.loginCredentials = "email=" + encodedEmail + "&password=" + encodedPassword + "&submit=Log+in";

        observers = new ArrayList<>();
    }

    public void subscribe(Observer observer){
        observers.add(observer);
    }

    public void notifySubscribers(){
        for(Observer o : observers){
            o.update(this, null);
        }
    }

    public void execute() throws Exception{

        try{
            FetchResource();
        } catch (Exception e){
            throw e;
        }


        result = processResource(resource);

        //provider.populateData(result);
        ContentResolver r = context.getContentResolver();
        r.delete(ShiftDataProvider.CONTENT_URI, null, null);

        for(int i=0;i<result.size();i++){
            ShiftData data = result.get(i);
            ContentValues values = new ContentValues();
            values.put(ShiftDataProvider.Columns.DATE, data.date);
            values.put(ShiftDataProvider.Columns.START_TIME, data.startTime);
            values.put(ShiftDataProvider.Columns.END_TIME, data.endTime);
            values.put(ShiftDataProvider.Columns.ACCEPTED, data.accepted);

            r.insert(Uri.withAppendedPath(ShiftDataProvider.CONTENT_URI, String.valueOf(i)) , values);
        }

        notifySubscribers();
    }

    private void FetchResource() throws Exception{
        if(cookieStore.getCookies().isEmpty()){
            SetCookies();
        }

        //Fetching
        CloseableHttpAsyncClient httpClient = HttpAsyncClients.createDefault();

        //Login
        SimpleHttpRequest loginRequest = SimpleHttpRequests.post(loginUri);
        loginRequest.setBody(loginCredentials, ContentType.APPLICATION_FORM_URLENCODED);

        //Data
        SimpleHttpRequest dataRequest = SimpleHttpRequests.get(dataUri);

        try{
            httpClient.start();
            Future<SimpleHttpResponse> loginFuture = httpClient.execute(loginRequest, null);
            SimpleHttpResponse loginResponse = loginFuture.get();

            if(!checkLogin(loginResponse.getBodyText())){
                throw new Exception("Invalid Smartshift credentials");
            }

            Future<SimpleHttpResponse> dataFuture = httpClient.execute(dataRequest, null);
            SimpleHttpResponse dataResponse = dataFuture.get();

            this.resource = dataResponse.getBodyText();

        } catch(Exception e){
            throw e;
        } finally {
            httpClient.close(CloseMode.GRACEFUL);
        }

    }

    private void SetCookies(){
        String cookieString="";
        CloseableHttpAsyncClient httpClient = HttpAsyncClients.createDefault();

        SimpleHttpRequest request = SimpleHttpRequests.get(pingUri);

        try{
            httpClient.start();
            Future<SimpleHttpResponse> future = httpClient.execute(request, null);
            SimpleHttpResponse response = future.get();
            Header cookieHeader = response.getHeader("Set-Cookie");
            if(cookieHeader != null){
                cookieString = cookieHeader.getValue();
            }

            httpClient.close(CloseMode.GRACEFUL);
        } catch(Exception e){
            System.err.println(e);
        }

        if(cookieString.contains("=")){
            String[] cookieParts = cookieString.split("=");

            BasicClientCookie cookie = new BasicClientCookie(cookieParts[0], cookieParts[1]);
            cookie.setPath("/");

            cookieStore.addCookie(cookie);
        }

    }

    private boolean checkLogin(String resource){
        Pattern pattern = Pattern.compile(loginPattern, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(resource);

        if (matcher.find()){
            return false;
        }

        return true;
    }

    private ArrayList<ShiftData> processResource(String resource){
        ArrayList<ShiftData> result = new ArrayList<>();
        String date = "";
        String start = "";
        String end = "";
        String code = "";
        String accepted = "";
        Pattern pattern = Pattern.compile(dataPattern, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(resource);

        int counter = 0;
        while(matcher.find()){

            switch (counter){
                case 0:
                    date = matcher.group(1);
                    break;
                case 4:
                    start = matcher.group(1);
                    break;
                case 5:
                    end = matcher.group(1);
                    break;
                case 8:
                    code = matcher.group(1);
                    break;
                case 9:
                    accepted = matcher.group(1);
            }

            if(counter == 9){
                result.add(new ShiftData(date, start, end, accepted));
            }

            counter = (counter + 1) % 10;
        }

        return result;
    }

}




