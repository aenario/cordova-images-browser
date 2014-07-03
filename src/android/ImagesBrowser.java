package io.cozy.imagesbrowser;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import android.annotation.SuppressLint;
import android.content.CursorLoader;
import android.database.Cursor;
import android.provider.MediaStore;

public class ImagesBrowser extends CordovaPlugin {

    /**
     * Constructor.
     */
    public ImagesBrowser() {
    }

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action            The action to execute.
     * @param args              JSONArray of arguments for the plugin.
     * @param callbackContext   The callback id used when calling back into JavaScript.
     * @return                  True if the action was valid, false if not.
     */
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if (action.equals("getImageList")) {
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    callbackContext.success(makeImageList(cordova));
                }
            });
        }
        else {
            return false;
        }
        return true;
    }


    @SuppressLint("NewApi")
    public JSONArray makeImageList(CordovaInterface cordova){
        // get images ID & path
        String[] projection = {MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA};
        // Create the cursor pointing to the SDCard
        CursorLoader loader = new CursorLoader(cordova.getActivity(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection, // Which columns to return
                null,       // Return all rows
                null,
                MediaStore.Images.Media.DATE_TAKEN);
        // Get the column index of the Thumbnails Image ID
        Cursor cursor = loader.loadInBackground();
        int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        JSONArray results = new JSONArray();
        while(cursor.moveToNext()){
            results.put(cursor.getString(columnIndex));
        }
        return results;
    }

}
