package com.claudetech.medialister;

import android.content.CursorLoader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MediaLister extends CordovaPlugin {
    private static final String TAG = "MEDIA_LISTER_PLUGIN";

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if (action.equals("readLibrary")) {
            final JSONObject options = args.getJSONObject(0);
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    readLibrary(options, callbackContext);
                }
            });
            return true;
        }
        return false;
    }

    private void readLibrary(JSONObject options, CallbackContext callbackContext) {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        try {
            JSONArray results = readMediaLibrary(options);
            callbackContext.success(results);
        } catch (JSONException e) {
            callbackContext.error(e.getMessage());
            e.printStackTrace();
        } finally {
            Looper.myLooper().quit();
        }
    }

    private JSONArray readMediaLibrary(JSONObject options) throws JSONException {
        JSONArray result = new JSONArray();
        Cursor cursor = makeMediaQueryCursor(options);


        boolean writeThumbnail = options.optBoolean("thumbnail", false);
        int thumbnailWidth = 400;
        int thumbnailHeight = 400;

        if (writeThumbnail) {
            thumbnailWidth = options.optJSONObject("thumbnailSize").optInt("width", 400);
            thumbnailHeight = options.optJSONObject("thumbnailSize").optInt("height", 400);
        }

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
            if (media.getString("mediaType").equals("image") && writeThumbnail) {
                addThumbnail(media, thumbnailWidth, thumbnailHeight);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                media.put("width", cursor.getInt(cursor.getColumnIndex(MediaStore.Files.FileColumns.WIDTH)));
                media.put("height", cursor.getInt(cursor.getColumnIndex(MediaStore.Files.FileColumns.HEIGHT)));
            }
            result.put(media);
        }
        return result;
    }

    private void addThumbnail(JSONObject image, int width, int height) throws JSONException {
        int id = image.getInt("id");
        String outputPath = getThumbnailPath(id, width, height);
        boolean hasThumbnail = new File(outputPath).isFile();
        if (!hasThumbnail) {
            try {
                createThumbnail(image, width, height);
                hasThumbnail = true;
            } catch (IOException e) {
                Log.w(TAG, e.getMessage());
            }
        }
        image.put("thumbnailPath", hasThumbnail ? outputPath : null);
    }

    private void createThumbnail(JSONObject image, int width, int height) throws JSONException, IOException {
        File file = new File(image.getString("path"));
        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();

        bitmapOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), bitmapOptions);

        float widthScale = (float)bitmapOptions.outWidth / width;
        float heightScale = (float)bitmapOptions.outHeight / height;
        float scale = Math.min(widthScale, heightScale);

        int sampleSize = 1;
        while (sampleSize < scale) {
            sampleSize *= 2;
        }
        bitmapOptions.inSampleSize = sampleSize;
        bitmapOptions.inJustDecodeBounds = false;

        Bitmap thumbnail = BitmapFactory.decodeFile(file.getAbsolutePath(), bitmapOptions);

        File thumbnailFile = new File(getThumbnailPath(image.getInt("id"), width, height));
        FileOutputStream fos = new FileOutputStream(thumbnailFile);
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, fos);
        fos.flush();
        fos.close();

        thumbnail.recycle();
    }

    private Cursor makeMediaQueryCursor(JSONObject options) {
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

        String selection = getSelection(options);

        Uri queryUri = MediaStore.Files.getContentUri("external");

        int limit = options.optInt("limit", 20);

        CursorLoader cursorLoader = new CursorLoader(
                cordova.getActivity(),
                queryUri,
                projection,
                selection,
                null,
                MediaStore.Files.FileColumns.DATE_ADDED + " DESC LIMIT " + limit
        );
        return cursorLoader.loadInBackground();
    }

    private String getSelection(JSONObject options) {
        List<String> conditions = new ArrayList<String>();
        String mediaSelection = getMediaSelection(options);
        if (mediaSelection.length() > 0) {
            conditions.add(mediaSelection);
        }
        int addedBefore = options.optInt("addedBefore", -1);
        if (addedBefore != -1) {
            conditions.add(MediaStore.Files.FileColumns.DATE_ADDED + "<" + addedBefore);
        }
        String[] formattedConditions = new String[conditions.size()];
        for (int i = 0; i < conditions.size(); i++) {
            formattedConditions[i] = "(" + conditions.get(i) + ")";
        }
        return TextUtils.join(" AND ", formattedConditions);
    }

    private String getMediaSelection(JSONObject options) {
        List<String> conditions = new ArrayList<String>();
        JSONArray types = options.optJSONArray("mediaTypes");
        for (int i = 0; types != null && i < types.length(); i++) {
            String type = types.optString(i, null);
            if (type == null) {
                continue;
            }
            int typeValue = -1;
            if (type.equals("image")) {
                typeValue = MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
            } else if (type.equals("video")) {
                typeValue += MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;
            } else if (type.equals("audio")) {
                typeValue += MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;
            }
            if (typeValue != -1) {
                conditions.add(MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + typeValue);
            }
        }
        return TextUtils.join(" OR ", conditions);
    }

    private String getThumbnailPath(int id, int width, int height) {
        File outputDir = cordova.getActivity().getCacheDir();
        return outputDir.getAbsolutePath() + "/" + id + "-" + width + "x" + height + ".jpg";
    }
}

