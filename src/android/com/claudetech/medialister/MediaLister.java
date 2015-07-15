package com.claudetech.medialister;

import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MediaLister extends CordovaPlugin {
    private static final String TAG = "MEDIA_LISTER_PLUGIN";

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("readLibrary")) {
            JSONObject options = args.getJSONObject(0);
            this.readLibrary(options, callbackContext);
            return true;
        }
        return false;
    }

    private void readLibrary(JSONObject options, CallbackContext callbackContext) {
        try {
            JSONArray results = readMediaLibrary();
            callbackContext.success(results);
        } catch (JSONException e) {
            callbackContext.error(e.getMessage());
            e.printStackTrace();
        }
    }

    private JSONArray readMediaLibrary() throws JSONException {
        JSONArray result = new JSONArray();
        Cursor cursor = makeMediaQueryCursor();

        for (boolean hasNext = cursor.moveToFirst(); hasNext; hasNext = cursor.moveToNext()) {
            JSONObject media = new JSONObject();
            int mediaType = cursor.getInt(cursor.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE));
            media.put("id", cursor.getInt(cursor.getColumnIndex(MediaStore.Files.FileColumns._ID)));
            media.put("path", cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)));
            media.put("size", cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)));
            media.put("mimeType", cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)));
            media.put("mediaType", mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE ? "image" : "video");
            media.put("title", cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.TITLE)));
            media.put("dateAdded", cursor.getInt(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_ADDED)));
            media.put("dateModified", cursor.getInt(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED)));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                media.put("width", cursor.getInt(cursor.getColumnIndex(MediaStore.Files.FileColumns.WIDTH)));
                media.put("height", cursor.getInt(cursor.getColumnIndex(MediaStore.Files.FileColumns.HEIGHT)));
            }
            result.put(media);
        }
        return result;
    }

    private Cursor makeMediaQueryCursor() {
        List<String> projectionList = new ArrayList<String>(Arrays.asList(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.MEDIA_TYPE,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.TITLE,
                MediaStore.Files.FileColumns.DATE_ADDED,
                MediaStore.Files.FileColumns.DATE_MODIFIED
        ));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            projectionList.add(MediaStore.Files.FileColumns.WIDTH);
            projectionList.add(MediaStore.Files.FileColumns.HEIGHT);
        }

        String[] projection = new String[projectionList.size()];
        projection = projectionList.toArray(projection);

        String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                + " OR "
                + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

        Uri queryUri = MediaStore.Files.getContentUri("external");

        CursorLoader cursorLoader = new CursorLoader(
                cordova.getActivity(),
                queryUri,
                projection,
                selection,
                null,
                MediaStore.Files.FileColumns.DATE_ADDED + " DESC"
        );
        return cursorLoader.loadInBackground();
    }
}

