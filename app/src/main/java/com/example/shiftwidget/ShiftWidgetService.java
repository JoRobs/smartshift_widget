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

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.util.Random;


/**
 * This is the service that provides the factory to be bound to the collection service.
 */
public class ShiftWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new StackRemoteViewsFactory(this.getApplicationContext(), intent);
    }


    /**
     * This is the factory that will provide data to the collection widget.
     */
    class StackRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
        private Context mContext;
        private Cursor mCursor;
        private int mAppWidgetId;

        public StackRemoteViewsFactory(Context context, Intent intent) {
            mContext = context;
            mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        public void onCreate() {
            // Since we reload the cursor in onDataSetChanged() which gets called immediately after
            // onCreate(), we do nothing here.
        }

        public void onDestroy() {
            if (mCursor != null) {
                mCursor.close();
            }
        }

        public int getCount() {
            return mCursor.getCount();
        }

        public RemoteViews getViewAt(int position) {
            // Get the data for this position from the content provider
            String day = "Unknown Date";
            String startTime = "Unknown Time";
            String endTime = "Unknown Time";
            Boolean accepted = false;

            if (mCursor.moveToPosition(position)) {
                final int dateColIndex = mCursor.getColumnIndex(ShiftDataProvider.Columns.DATE);
                final int startTimeColIndex = mCursor.getColumnIndex(
                        ShiftDataProvider.Columns.START_TIME);
                final int endTimeColIndex = mCursor.getColumnIndex(
                        ShiftDataProvider.Columns.END_TIME);
                final int acceptedColIndex = mCursor.getColumnIndex(ShiftDataProvider.Columns.ACCEPTED);

                day = mCursor.getString(dateColIndex);
                startTime = mCursor.getString(startTimeColIndex);
                endTime = mCursor.getString(endTimeColIndex);
                accepted = mCursor.getString(acceptedColIndex).equals("Yes");
            }

            // Sets text reflecting shift times
            final String dateFormatStr = mContext.getResources().getString(R.string.date_format_string);
            final String startTimeFormatStr = mContext.getResources().getString(R.string.start_time_format_string);
            final String endTimeformatStr = mContext.getResources().getString(R.string.end_time_format_string);



            final int itemId = R.layout.widget_item;
            RemoteViews rv = new RemoteViews(mContext.getPackageName(), itemId);

            rv.setTextViewText(R.id.widget_item_date, String.format(dateFormatStr, day));
            rv.setTextViewText(R.id.widget_item_start_time, String.format(startTimeFormatStr, startTime));
            rv.setTextViewText(R.id.widget_item_end_time, String.format(endTimeformatStr, endTime));
            rv.setViewVisibility(R.id.green, accepted? View.VISIBLE : View.INVISIBLE);
            rv.setViewVisibility(R.id.yellow, accepted? View.INVISIBLE : View.VISIBLE);

            // Set the click intent so that we can handle it and show a toast message
            final Intent fillInIntent = new Intent();
            rv.setOnClickFillInIntent(R.id.widget_item, fillInIntent);

            return rv;
        }

        public RemoteViews getLoadingView() {
            return null;
        }

        public int getViewTypeCount() {
            // Technically, we have two types of views (the dark and light background views)
            return 1;
        }

        public long getItemId(int position) {
            return position;
        }

        public boolean hasStableIds() {
            return true;
        }

        public void onDataSetChanged() {
            // Refresh the cursor
            if (mCursor != null) {
                mCursor.close();
            }
            mCursor = mContext.getContentResolver().query(ShiftDataProvider.CONTENT_URI, null, null,
                    null, null);
        }
    }
}
