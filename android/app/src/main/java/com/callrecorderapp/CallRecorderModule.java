package com.callrecorderapp;

import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import androidx.annotation.NonNull;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import java.io.File;
import java.io.IOException;

public class CallRecorderModule extends ReactContextBaseJavaModule {
    private static final String TAG = "CallRecorderModule";
    private MediaRecorder recorder;
    private boolean isRecording = false;
    private String outputFilePath;
    private final ReactApplicationContext reactContext;
    private final PhoneStateListener phoneStateListener;

    public CallRecorderModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        this.phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String phoneNumber) {
                switch (state) {
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        startRecording();
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        stopRecording();
                        break;
                }
            }
        };
    }

    @Override
    public void initialize() {
        super.initialize();
        new Handler(Looper.getMainLooper()).post(() -> {
            TelephonyManager telephonyManager = (TelephonyManager) reactContext.getSystemService(ReactApplicationContext.TELEPHONY_SERVICE);
            if (telephonyManager != null) {
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
            }
        });
    }

    @Override
    public String getName() {
        return "CallRecorderModule";
    }

    @ReactMethod
    public void requestPermissions(Promise promise) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (reactContext.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED ||
                reactContext.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                promise.reject("PERMISSION_DENIED", "Audio and Phone permissions are required.");
                return;
            }
        }
        promise.resolve("Permissions Granted");
    }

    @ReactMethod
    public void startRecording() {
        if (isRecording) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            outputFilePath = reactContext.getExternalFilesDir(null).getAbsolutePath() + "/call_record.mp3";
        } else {
            outputFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/call_record.mp3";
        }

        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC_ELD);
        recorder.setOutputFile(outputFilePath);

        try {
            recorder.prepare();
            recorder.start();
            isRecording = true;
        } catch (IOException e) {
            Log.e(TAG, "Recording failed", e);
        }
    }

    @ReactMethod
    public void stopRecording() {
        if (!isRecording) return;

        recorder.stop();
        recorder.release();
        recorder = null;
        isRecording = false;

        new Handler(Looper.getMainLooper()).post(() -> {
            reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit("onCallRecorded", outputFilePath);
        });
    }
}
