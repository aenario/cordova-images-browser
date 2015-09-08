package io.cozy.imagesbrowser;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.CursorLoader;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Looper;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
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
        if (action.equals("getImagesList")) {
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    if (Looper.myLooper() == null) {
                        Looper.prepare();
                    }
                    callbackContext.success(makeImageList(cordova));
                }
            });
        } else if(action.equals("getContactsList")){
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (Looper.myLooper() == null) {
                            Looper.prepare();
                        }
                        callbackContext.success(getContacts(cordova));
                    } catch (JSONException e) {
                        e.printStackTrace();
                        callbackContext.error(e.getMessage());
                    }
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
                MediaStore.Images.Media.DATE_TAKEN + " DESC");
        // Get the column index of the Thumbnails Image ID
        Cursor cursor = loader.loadInBackground();
        JSONArray results = new JSONArray();
        // Cursor may be null when sdcard isnt mounted.
        if (cursor != null) {
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            while(cursor.moveToNext()){
                results.put(cursor.getString(columnIndex));
            }
        }
        return results;
    }


    @SuppressLint("NewApi")
    public JSONArray getContacts(CordovaInterface cordova) throws JSONException{
        JSONArray result = new JSONArray();

        Cursor datapoints = new CursorLoader(cordova.getActivity(),
            ContactsContract.Data.CONTENT_URI,
            null,
            null,
            null,
            ContactsContract.Data.CONTACT_ID
        ).loadInBackground();

        int contact_id_idx = datapoints.getColumnIndex(ContactsContract.Data.CONTACT_ID);
        int mime_type_idx = datapoints.getColumnIndex(ContactsContract.Data.MIMETYPE);


        String currentId = "";
        String versionHash = "";
        JSONObject currentContact = new JSONObject();
        JSONArray items = new JSONArray();

        while(datapoints.moveToNext()){
            String id = datapoints.getString(contact_id_idx);
            if(!id.equals(currentId)){
                // new ID, store contact, create next
                if(items.length() > 0 && !currentId.equals("")){
                    currentContact.put("localVersion", versionHash);
                    currentContact.put("datapoints", items);
                    result.put(currentContact);
                }
                currentContact = new JSONObject();
                currentContact.put("localId", id);
                items = new JSONArray();
                currentId = id;
                versionHash = "";
            }

            String type = datapoints.getString(mime_type_idx);
            if(type.equals(Phone.CONTENT_ITEM_TYPE)){
                items.put(handleBaseColumns(datapoints, false));
                versionHash += "-" + datapoints.getInt(datapoints.getColumnIndex(Data.DATA_VERSION));
            }else if(type.equals(Email.CONTENT_ITEM_TYPE)){
                items.put(handleBaseColumns(datapoints, true));
                versionHash += "-" +datapoints.getInt(datapoints.getColumnIndex(Data.DATA_VERSION));
            }else if(type.equals(StructuredName.CONTENT_ITEM_TYPE)){
                handleName(currentContact, datapoints);
                versionHash += "-" +datapoints.getInt(datapoints.getColumnIndex(Data.DATA_VERSION));
            }else if(type.equals(Photo.CONTENT_ITEM_TYPE)){
                // byte[] blob = datapoints.getBlob(datapoints.getColumnIndex(Photo.PHOTO));
                // currentContact.put("photo", Base64.encode(blob));
            }
        }
        if(!currentId.equals("")){
            currentContact.put("localVersion", versionHash);
            currentContact.put("datapoints", items);
            result.put(currentContact);
        }


        return result;
    }

    public JSONObject handleBaseColumns(Cursor item, boolean isEmail) throws JSONException{
        JSONObject result = new JSONObject();
        result.put("value", item.getString(item.getColumnIndex(Email.DATA)));
        result.put("type", getLabel(isEmail, item));
        if(isEmail){
            result.put("name", "email");
        }else{
            result.put("name", "tel");
        }
        return result;
    }

    public String getLabel(boolean isEmail, Cursor item){
        Resources res = cordova.getActivity().getResources();
        int type = Integer.valueOf(item.getString(item.getColumnIndex(Email.TYPE)));
        if(Email.TYPE_CUSTOM == type){
            return item.getString(item.getColumnIndex(Email.LABEL));
        }
        if(isEmail){
            return Email.getTypeLabel(res, type, "other").toString();
        }else{
            return Phone.getTypeLabel(res, type, "other").toString();
        }

    }

    public void handleName(JSONObject contact, Cursor name) throws JSONException{
        contact.put("fn", name.getString(name.getColumnIndex(StructuredName.DISPLAY_NAME)));
        String sep = ";";
        contact.put("n",
                getString(name, StructuredName.FAMILY_NAME, "") + sep +
                getString(name, StructuredName.GIVEN_NAME, "") + sep +
                getString(name, StructuredName.MIDDLE_NAME, "") + sep +
                getString(name, StructuredName.PREFIX, "") + sep +
                getString(name, StructuredName.SUFFIX, ""));
    }

    public String getString(Cursor cur, String col, String def){
        String result = cur.getString(cur.getColumnIndex(col));
        if(result != null) return result;
        return def;
    }


}
