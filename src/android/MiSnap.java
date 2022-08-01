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

import $appid.MainActivity;
import com.miteksystems.facialcapture.controller.api.AutoCaptureTrigger;
import com.miteksystems.facialcapture.controller.params.FacialCaptureControllerApi;
import com.miteksystems.facialcapture.controller.params.FacialCaptureControllerParamMgr;
import com.miteksystems.facialcapture.science.params.FacialCaptureScienceApi;
import com.miteksystems.facialcapture.science.params.FacialCaptureScienceParamMgr;
import com.miteksystems.facialcapture.workflow.FacialCaptureWorkflowActivity;
import com.miteksystems.facialcapture.workflow.api.FacialCaptureResult;
import com.miteksystems.facialcapture.workflow.api.ManualReviewBackButtonBehavior;
import com.miteksystems.facialcapture.workflow.params.FacialCaptureWorkflowApi;
import com.miteksystems.facialcapture.workflow.params.FacialCaptureWorkflowApiConstants;
import com.miteksystems.facialcapture.workflow.params.FacialCaptureWorkflowParamMgr;
import com.miteksystems.misnap.params.CameraApi;
import com.miteksystems.misnap.params.MiSnapApi;
import com.miteksystems.misnap.utils.Utils;

import android.util.Base64;

/**
 * This class echoes a string called from JavaScript.
 */
public class MiSnap extends CordovaPlugin {

    private static String TAG;
    private static final long PREVENT_DOUBLE_CLICK_TIME_MS = 1000;
    private long mTime = 0;
    private CallbackContext callback;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        TAG = cordova.getActivity().getClass().getSimpleName();
        super.initialize(cordova, webView);
    }

    public void callbackResult(ActivityResult result){
        Intent data = result.getData();
        if (result.getResultCode() == Activity.RESULT_OK) {
            // There are no request codes
            if (data != null) {
                Bundle extras = data.getExtras();
                if (extras != null) {
                    String mibi = extras.getString(MiSnapApi.RESULT_MIBI_DATA);
                    Log.i(TAG, "MIBI: " + mibi);
                    if (extras.getParcelable(FacialCaptureWorkflowApi.FACIAL_CAPTURE_RESULT) instanceof FacialCaptureResult.Success) {
                        FacialCaptureResult.Success captureResult = extras.getParcelable(FacialCaptureWorkflowApi.FACIAL_CAPTURE_RESULT);
                        JSONObject json = new JSONObject();
                        try {
                            json.put("image", Base64.encodeToString(captureResult.getImage(), Base64.DEFAULT));
                            json.put("warnings",captureResult.getWarnings());
                            json.put("mibi",mibi);
                            callback.sendPluginResult(new PluginResult(PluginResult.Status.OK,json));
                        } catch (JSONException e) {
                            e.printStackTrace();
                            callback.error(e.getLocalizedMessage());
                        }
                    }
                }
            }
        }else{
            if (data != null) {
                FacialCaptureResult.Failure failureResult = data.getParcelableExtra(FacialCaptureWorkflowApi.FACIAL_CAPTURE_RESULT);
                callback.error("FacialCapture exit code: " + failureResult.getResultCode());
            }
        }
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        callback = callbackContext;
        if (action.equals("startFacialCapture")) {
            this.startFacialCaptureWorkflow(args);
            return true;
        }else if (action.equals("checkPermission")) {
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

    private void startFacialCaptureWorkflow(JSONArray args) throws JSONException {
        // Prevent multiple MiSnap instances by preventing multiple button presses
        if (System.currentTimeMillis() - mTime < PREVENT_DOUBLE_CLICK_TIME_MS) {
            // Double-press detected
            callback.error("Double press detected!");
            return;
        }
        mTime = System.currentTimeMillis();

        JSONObject arg = new JSONObject(args.getString(0));

        // Add in parameter info for MiSnap
        JSONObject jjs = new JSONObject();

        FacialCaptureControllerParamMgr controllerParamMgr = new FacialCaptureControllerParamMgr(new JSONObject());
        FacialCaptureScienceParamMgr scienceParamMgr = new FacialCaptureScienceParamMgr(new JSONObject());
        FacialCaptureWorkflowParamMgr workflowParamMgr = new FacialCaptureWorkflowParamMgr(new JSONObject());

        try {
            // MiSnap-specific parameters
            jjs.put(CameraApi.MiSnapAllowScreenshots, arg.optInt("AllowScreenshots",1));

            //CameraApi.PARAMETER_CAPTURE_MODE_MANUAL  1
            //CameraApi.PARAMETER_CAPTURE_MODE_AUTO  0 default
            jjs.put(CameraApi.MiSnapCaptureMode, spinnerIndexToCaptureMode(arg.optInt("CaptureMode",0)));

            jjs.put(FacialCaptureScienceApi.FacialCaptureMinHorizontalFill, arg.optInt("MinHorizontalFill",scienceParamMgr.getMinHorizontalFill()));

            jjs.put(FacialCaptureScienceApi.FacialCaptureMinPadding, arg.optInt("MinPadding",scienceParamMgr.getMinPadding()));

            jjs.put(FacialCaptureScienceApi.FacialCaptureMaxTilt, arg.optInt("widgetMaxTilt",scienceParamMgr.getMaxTilt()));

            jjs.put(FacialCaptureScienceApi.FacialCaptureMinSmileConfidence, arg.optInt("MinSmileConfidence",scienceParamMgr.getMinSmileConfidence()));

            jjs.put(FacialCaptureWorkflowApi.FacialCaptureWorkflowShowManualReviewScreen, arg.optBoolean("ShowManualReview",workflowParamMgr.shouldShowManualReviewScreen()));

            jjs.put(FacialCaptureWorkflowApi.FacialCaptureWorkflowShowCountdownTimer, arg.optBoolean("ShowCountdownTimer",workflowParamMgr.shouldShowCountdownTimer()));

            // ManualReviewBackButtonBehavior.CANCEL_SESSION  1
            // ManualReviewBackButtonBehavior.RETRY_SESSION  0 default
            jjs.put(FacialCaptureWorkflowApi.FacialCaptureManualReviewBackButtonBehavior, spinnerIndexToManualReviewBackButtonBehavior(arg.optInt("ManualReviewBackButtonBehaviour",manualReviewBackButtonBehaviorToSpinnerIndex(workflowParamMgr.getManualReviewBackButtonBehavior()))));

            // AutoCaptureTrigger.SMILE  1
            // AutoCaptureTrigger.IQA_ONLY  0 default
            jjs.put(FacialCaptureControllerApi.FacialCaptureAutoCaptureTrigger, spinnerIndexToAutoCaptureTrigger(arg.optInt("AutoCaptureTrigger",autoCaptureTriggerToSpinnerIndex(controllerParamMgr.getAutoCaptureTrigger()))));

            jjs.put(FacialCaptureControllerApi.FacialCaptureTriggerDelay, arg.optInt("TriggerDelay",controllerParamMgr.getTriggerDelay()));

            // FacialCaptureWorkflowApiConstants.COUNTDOWN_TIMER_STYLE_PULSE  2
            // FacialCaptureWorkflowApiConstants.COUNTDOWN_TIMER_STYLE_INFINITY  1
            // FacialCaptureWorkflowApiConstants.COUNTDOWN_TIMER_STYLE_NONE  0 default
            jjs.put(FacialCaptureWorkflowApi.FacialCaptureCountdownTimerStyle, spinnerIndexToCountdownTimerStyle(arg.optInt("CountdowTimerStyle",countdownTimerStyleToSpinnerIndex(workflowParamMgr.getCountdownTimerStyle()))));

            // An example of how to set workflow parameters
            //jjs.put(FacialCaptureWorkflowApi.FacialCaptureWorkflowMessageDelay, 500);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Intent intentFacialCapture = new Intent(cordova.getActivity(), FacialCaptureWorkflowActivity.class);
        intentFacialCapture.putExtra(MiSnapApi.JOB_SETTINGS, jjs.toString());

        ((MainActivity)cordova.getActivity()).misnapActivityResultLauncher.launch(intentFacialCapture);
    }

    private int spinnerIndexToCaptureMode(int index) {
        switch (index) {
            case 1:
                return CameraApi.PARAMETER_CAPTURE_MODE_MANUAL;
            case 0:
            default:
                return CameraApi.PARAMETER_CAPTURE_MODE_AUTO;
        }
    }

    private AutoCaptureTrigger spinnerIndexToAutoCaptureTrigger(int index) {
        switch (index) {
            case 1:
                return AutoCaptureTrigger.SMILE;
            case 0:
            default:
                return AutoCaptureTrigger.IQA_ONLY;
        }
    }

    private int autoCaptureTriggerToSpinnerIndex(AutoCaptureTrigger autoCaptureTrigger) {
        switch (autoCaptureTrigger) {
            case SMILE:
                return 1;
            case IQA_ONLY:
            default:
                return 0;
        }
    }

    private ManualReviewBackButtonBehavior spinnerIndexToManualReviewBackButtonBehavior(int index) {
        switch (index) {
            case 1:
                return ManualReviewBackButtonBehavior.CANCEL_SESSION;
            case 0:
            default:
                return ManualReviewBackButtonBehavior.RETRY_SESSION;
        }
    }

    private int manualReviewBackButtonBehaviorToSpinnerIndex(ManualReviewBackButtonBehavior manualReviewBackButtonBehavior) {
        switch (manualReviewBackButtonBehavior) {
            case CANCEL_SESSION:
                return 1;
            case RETRY_SESSION:
            default:
                return 0;
        }
    }

    private int spinnerIndexToCountdownTimerStyle(int index) {
        switch (index) {
            case 1:
                return FacialCaptureWorkflowApiConstants.COUNTDOWN_TIMER_STYLE_INFINITY;
            case 2:
                return FacialCaptureWorkflowApiConstants.COUNTDOWN_TIMER_STYLE_PULSE;
            case 0:
            default:
                return FacialCaptureWorkflowApiConstants.COUNTDOWN_TIMER_STYLE_NONE;
        }
    }

    private int countdownTimerStyleToSpinnerIndex(int countdownTimerStyle) {
        switch (countdownTimerStyle) {
            case FacialCaptureWorkflowApiConstants.COUNTDOWN_TIMER_STYLE_INFINITY:
                return 1;
            case FacialCaptureWorkflowApiConstants.COUNTDOWN_TIMER_STYLE_PULSE:
                return 2;
            case FacialCaptureWorkflowApiConstants.COUNTDOWN_TIMER_STYLE_NONE:
            default:
                return 0;
        }
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
