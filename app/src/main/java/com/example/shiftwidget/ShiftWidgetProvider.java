/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.shiftwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.util.Observable;
import java.util.Observer;

/**
 * Our data observer just notifies an update for all weather widgets when it detects a change.
 */
class ShiftDataProviderObserver extends ContentObserver {
    private AppWidgetManager mAppWidgetManager;
    private ComponentName mComponentName;

    ShiftDataProviderObserver(AppWidgetManager mgr, ComponentName cn, Handler h) {
        super(h);
        mAppWidgetManager = mgr;
        mComponentName = cn;
    }

    @Override
    public void onChange(boolean selfChange) {
        // The data has changed, so notify the widget that the collection view needs to be updated.
        // In response, the factory's onDataSetChanged() will be called which will requery the
        // cursor for the new data.
        mAppWidgetManager.notifyAppWidgetViewDataChanged(
                mAppWidgetManager.getAppWidgetIds(mComponentName), R.id.shift_list);
    }
}

/**
 * The weather widget's AppWidgetProvider.
 */
public class ShiftWidgetProvider extends AppWidgetProvider implements Observer {
    public static String CLICK_ACTION = "com.example.android.shiftwidget.CLICK";
    public static String REFRESH_ACTION = "com.example.android.shiftwidget.REFRESH";
    public static String SETTINGS_ACTION = "com.example.android.shiftwidget.REFRESH";

    private static HandlerThread sWorkerThread;
    private static Handler sWorkerQueue;
    private static ShiftDataProviderObserver sDataObserver;

    private boolean mIsLargeLayout = true;
    private int mHeaderWeatherState = 0;

    public ShiftWidgetProvider() {
        // Start the worker thread
        sWorkerThread = new HandlerThread("ShiftWidgetProvider-worker");
        sWorkerThread.start();
        sWorkerQueue = new Handler(sWorkerThread.getLooper());
    }

    @Override
    public void onEnabled(Context context) {
        // Register for external updates to the data to trigger an update of the widget.  When using
        // content providers, the data is often updated via a background service, or in response to
        // user interaction in the main app.  To ensure that the widget always reflects the current
        // state of the data, we must listen for changes and update ourselves accordingly.
        final ContentResolver r = context.getContentResolver();

        if (sDataObserver == null) {
            final AppWidgetManager mgr = AppWidgetManager.getInstance(context);
            final ComponentName cn = new ComponentName(context, ShiftWidgetProvider.class);
            sDataObserver = new ShiftDataProviderObserver(mgr, cn, sWorkerQueue);
            r.registerContentObserver(ShiftDataProvider.CONTENT_URI, true, sDataObserver);
        }

        final Context cxt = context;

        sWorkerQueue.post(new FetchDataTask(cxt, this));
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        final String action = intent.getAction();

        if (action.equals(REFRESH_ACTION)) {
            // BroadcastReceivers have a limited amount of time to do work, so for this sample, we
            // are triggering an update of the data on another thread.  In practice, this update
            // can be triggered from a background service, or perhaps as a result of user actions
            // inside the main application.
            final Context context = ctx;

            sWorkerQueue.removeMessages(0);
            sWorkerQueue.post(new FetchDataTask(context, this));
        }

        super.onReceive(ctx, intent);
    }

    private RemoteViews buildLayout(Context context, int appWidgetId) {
        RemoteViews rv;

        // Specify the service to provide data for the collection widget.  Note that we need to
        // embed the appWidgetId via the data otherwise it will be ignored.
        final Intent intent = new Intent(context, ShiftWidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        rv = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
        rv.setRemoteAdapter(appWidgetId, R.id.shift_list, intent);


        // Set the empty view to be displayed if the collection is empty.  It must be a sibling
        // view of the collection view.
        rv.setEmptyView(R.id.shift_list, R.id.empty_view);

        // Bind a click listener template for the contents of the list.  Note that we
        // need to update the intent's data if we set an extra, since the extras will be
        // ignored otherwise.
        final Intent onClickIntent = new Intent(context, ShiftWidgetProvider.class);

        onClickIntent.setAction(ShiftWidgetProvider.CLICK_ACTION);
        onClickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        onClickIntent.setData(Uri.parse(onClickIntent.toUri(Intent.URI_INTENT_SCHEME)));

        final PendingIntent onClickPendingIntent = PendingIntent.getBroadcast(context, 0,
                onClickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        rv.setPendingIntentTemplate(R.id.shift_list, onClickPendingIntent);

        // Bind the click intent for the refresh button on the widget
        final Intent refreshIntent = new Intent(context, ShiftWidgetProvider.class);
        refreshIntent.setAction(ShiftWidgetProvider.REFRESH_ACTION);
        final PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(context, 0,
                refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        rv.setOnClickPendingIntent(R.id.refresh, refreshPendingIntent);

        // Bind the click intent for the settings/login button on the widget
        Intent settingsIntent = new Intent(Intent.ACTION_MAIN, null);
        settingsIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        ComponentName cn = new ComponentName("com.example.shiftwidget", "com.example.shiftwidget.MainActivity");
        settingsIntent.setComponent(cn);
        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        final PendingIntent settingsPendingIntent = PendingIntent.getActivity(context, 0, settingsIntent, 0);

        rv.setOnClickPendingIntent(R.id.settings, settingsPendingIntent);

        // Restore the minimal header
        rv.setTextViewText(R.id.title_text, context.getString(R.string.widget_title));

        return rv;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Update each of the widgets with the remote adapter
        for (int i = 0; i < appWidgetIds.length; ++i) {
            RemoteViews layout = buildLayout(context, appWidgetIds[i]);
            appWidgetManager.updateAppWidget(appWidgetIds[i], layout);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
            int appWidgetId, Bundle newOptions) {

        RemoteViews layout;

        layout = buildLayout(context, appWidgetId);
        appWidgetManager.updateAppWidget(appWidgetId, layout);
    }

    class FetchDataTask implements Runnable{
        Observer o;
        Context ctx;

        public FetchDataTask(Context ctx, Observer o){
            this.o = o;
            this.ctx = ctx;
        }

        public void run() {
            SmartshiftResourceAccess accessor = new SmartshiftResourceAccess(ctx);
            accessor.subscribe(o);

            final AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
            final ComponentName cn = new ComponentName(ctx, ShiftWidgetProvider.class);

            try{
                accessor.execute();
            } catch (Exception e){
                System.out.println(e);
                Toast.makeText(ctx, e.getMessage(), Toast.LENGTH_LONG).show();
            }

            mgr.notifyAppWidgetViewDataChanged(mgr.getAppWidgetIds(cn), R.id.shift_list);
        }
    }

    @Override
    public void update(Observable observable, Object o) {
    }
}