package sk.henrichg.phoneprofilesplus;

import android.app.backup.BackupAgentHelper;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;

public class PhoneProfilesBackupAgent extends BackupAgentHelper {

    @Override
    public void onCreate() {
        PPApplication.logE("PhoneProfilesBackupAgent","onCreate");
    }

    @Override
    public void onRestoreFinished() {
        PPApplication.logE("PhoneProfilesBackupAgent","onRestoreFinished");

        // Do NOT CLOSE APPLICATION AFTER RESTORE.

        final Context appContext = getApplicationContext();

        DataWrapper dataWrapper = new DataWrapper(appContext, false, 0);

        PPApplication.exitApp(appContext, dataWrapper, null, false, false);

        Intent intent = new Intent("FinishActivatorBroadcastReceiver");
        LocalBroadcastManager.getInstance(appContext).sendBroadcast(intent);
        intent = new Intent("FinishEditorBroadcastReceiver");
        LocalBroadcastManager.getInstance(appContext).sendBroadcast(intent);
        /*
        ActivateProfileActivity activateProfileActivity = ActivateProfileActivity.getInstance();
        if (activateProfileActivity != null)
        {
            PPApplication.logE("PhoneProfilesBackupAgent","close ActivateProfileActivity");
            activateProfileActivity.finish();
        }

        EditorProfilesActivity editorProfilesActivity = EditorProfilesActivity.getInstance();
        if (editorProfilesActivity != null)
        {
            PPApplication.logE("PhoneProfilesBackupAgent","close EditorProfilesActivity");
            editorProfilesActivity.finish();
        }
        */

        PPApplication.startHandlerThread("PhoneProfilesBackupAgent.onRestoreFinished");
        final Handler handler = new Handler(PPApplication.handlerThread.getLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                PPApplication.logE("PhoneProfilesBackupAgent.onRestoreFinished", "in handler");

                PowerManager powerManager = (PowerManager) appContext.getSystemService(POWER_SERVICE);
                PowerManager.WakeLock wakeLock = null;
                if (powerManager != null) {
                    wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PhoneProfilesBackupAgent.onRestoreFinished");
                    wakeLock.acquire(10 * 60 * 1000);
                }

                PPApplication.setSavedVersionCode(appContext, 0);

                Permissions.setAllShowRequestPermissions(appContext, true);

                WifiBluetoothScanner.setShowEnableLocationNotification(appContext, true);
                //ActivateProfileHelper.setScreenUnlocked(appContext, true);
                ActivateProfileHelper.setMergedRingNotificationVolumes(appContext, true);

                if ((wakeLock != null) && wakeLock.isHeld()) {
                    try {
                        wakeLock.release();
                    } catch (Exception ignored) {}
                }
            }
        });
    }

}
