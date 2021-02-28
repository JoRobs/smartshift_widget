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

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import java.util.ArrayList;
import java.util.Random;


public class ShiftDataProvider extends ContentProvider {
    public static final Uri CONTENT_URI =
        Uri.parse("content://com.example.android.shiftwidget.provider");
    public static class Columns {
        public static final String ID = "_id";
        public static final String DATE = "date";
        public static final String START_TIME = "startTime";
        public static final String END_TIME = "endTime";
        public static final String ACCEPTED = "accepted";
    }

    private static final ArrayList<ShiftData> sData = new ArrayList<ShiftData>();

    @Override
    public boolean onCreate() {

        return true;
    }

    @Override
    public synchronized Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        assert(uri.getPathSegments().isEmpty());

        final MatrixCursor c = new MatrixCursor(
                new String[]{ Columns.ID, Columns.DATE, Columns.START_TIME, Columns.END_TIME, Columns.ACCEPTED});
        for (int i = 0; i < sData.size(); ++i) {
            final ShiftData data = sData.get(i);
            c.addRow(new Object[]{ new Integer(i), data.date, data.startTime, data.endTime, data.accepted });
        }
        return c;
    }

    @Override
    public String getType(Uri uri) {
        return "vnd.android.cursor.dir/vnd.shiftwidget.shift";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        final ShiftData data = new ShiftData(values.getAsString("date"), values.getAsString("startTime"), values.getAsString("endTime"), values.getAsString("accepted"));
        sData.add(data);

        getContext().getContentResolver().notifyChange(uri, null);
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Only supports full clear
        assert(uri.getPathSegments().isEmpty());

        sData.clear();
        return 0;
    }

    @Override
    public synchronized int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        assert(uri.getPathSegments().size() == 1);

        // In this sample, we only update the content provider individually for each row with new
        // temperature values.
        final int index = Integer.parseInt(uri.getPathSegments().get(0));
        final MatrixCursor c = new MatrixCursor(
                new String[]{ Columns.ID, Columns.DATE, Columns.START_TIME, Columns.END_TIME, Columns.ACCEPTED});
        assert(0 <= index && index < sData.size());
        final ShiftData data = sData.get(index);
        data.startTime = values.getAsString(Columns.START_TIME);

        // Notify any listeners that the data backing the content provider has changed, and return
        // the number of rows affected.
        getContext().getContentResolver().notifyChange(uri, null);
        return 1;
    }

}
