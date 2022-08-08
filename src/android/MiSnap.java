package com.outsystems.misnap;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.Manifest;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Base64;

/**
 * This class echoes a string called from JavaScript.
 */
public class MiSnap extends CordovaPlugin {

    private CallbackContext callback;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        callback = callbackContext;
        if (action.equals("checkPermission")) {
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    callback.sendPluginResult(new PluginResult(PluginResult.Status.OK,checkPermissionAction()));
                }
            });
            return true;
        } else if (action.equals("requestPermission")) {
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    try {
                        requestPermissionAction();
                    } catch (Exception e) {
                        e.printStackTrace();
                        callbackContext.error("Request permission has been denied.");
                        callback = null;
                    }
                }
            });
            return true;
        }
        callbackContext.error("Action not mapped!");
        callback = null;
        return false;
    }
    
    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        if (callback == null) {
            return;
        }

        JSONObject returnObj = new JSONObject();
        if (permissions != null && permissions.length > 0) {
            //Call checkPermission again to verify
            boolean hasPermission = checkPermissionAction();
            callback.sendPluginResult(new PluginResult(PluginResult.Status.OK,hasPermission));
        } else {
            callback.error("Unknown error.");
        }
        callback = null;
    }

    private boolean checkPermissionAction() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        } else {
            return cordova.hasPermission(Manifest.permission.CAMERA);
        }
    }

    private void requestPermissionAction() throws Exception {
        if (checkPermissionAction()) {
            callback.sendPluginResult(new PluginResult(PluginResult.Status.OK,true));
        } else {
            cordova.requestPermissions(this, 10001, new String[]{Manifest.permission.CAMERA});
        }
    }
}
