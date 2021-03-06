package sk.henrichg.phoneprofilesplus;

import android.content.Intent;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;

public class PhoneProfilesDashClockExtension extends DashClockExtension {

    private DataWrapper dataWrapper;
    private static PhoneProfilesDashClockExtension instance;

    public PhoneProfilesDashClockExtension()
    {
        instance = this;
    }

    public static PhoneProfilesDashClockExtension getInstance()
    {
        return instance;
    }

    @Override
    protected void onInitialize(boolean isReconnect) {
        super.onInitialize(isReconnect);

        GlobalGUIRoutines.setLanguage(this);

        if (dataWrapper == null)
            dataWrapper = new DataWrapper(this, false, 0);

        setUpdateWhenScreenOn(true);
    }

    @Override
    public void onDestroy()
    {
        /*if (dataWrapper != null)
            dataWrapper.invalidateDataWrapper();
        dataWrapper = null;*/
    }


    private int maxLength;
    private String addIntoIndicator(String indicator, String preference)
    {
        String ind = indicator;
        if (ind.length() > maxLength)
        {
            ind = ind + '\n';
            maxLength += 25;
        }
        else
            if (!ind.isEmpty()) ind = ind + "-";
        ind = ind + preference;
        return ind;
    }

    @Override
    protected void onUpdateData(int reason) {
        Profile profile;

        if (dataWrapper == null)
            return;

        profile = Profile.getMappedProfile(
                                    dataWrapper.getActivatedProfile(true, false), this);

        boolean isIconResourceID;
        String iconIdentifier;
        String profileName;
        if (profile != null)
        {
            isIconResourceID = profile.getIsIconResourceID();
            iconIdentifier = profile.getIconIdentifier();
            profileName = DataWrapper.getProfileNameWithManualIndicator(profile, true, false, false, dataWrapper, false);
        }
        else
        {
            isIconResourceID = true;
            iconIdentifier = Profile.PROFILE_ICON_DEFAULT;
            profileName = getResources().getString(R.string.profiles_header_profile_name_no_activated);
        }
        int iconResource;
        if (isIconResourceID)
            //iconResource = getResources().getIdentifier(iconIdentifier, "drawable", getPackageName());
            iconResource = Profile.getIconResource(iconIdentifier);
        else
            //iconResource = getResources().getIdentifier(Profile.PROFILE_ICON_DEFAULT, "drawable", getPackageName());
            iconResource = Profile.getIconResource(Profile.PROFILE_ICON_DEFAULT);

        // profile preferences indicator
        String indicator1 = "";
        if (profile != null)
        {
            maxLength = 25;
            if (profile._volumeRingerMode != 0) {
                if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_VOLUME_RINGER_MODE, null, this).allowed == PreferenceAllowed.PREFERENCE_ALLOWED) {
                    if (profile._volumeRingerMode == 5) {
                        // zen mode
                        if (profile._volumeZenMode == 1)
                            indicator1 = addIntoIndicator(indicator1, "ina");
                        if (profile._volumeZenMode == 2)
                            indicator1 = addIntoIndicator(indicator1, "inp");
                        if (profile._volumeZenMode == 3)
                            indicator1 = addIntoIndicator(indicator1, "inn");
                        if (profile._volumeZenMode == 4) {
                            indicator1 = addIntoIndicator(indicator1, "ina");
                            indicator1 = addIntoIndicator(indicator1, "vib");
                        }
                        if (profile._volumeZenMode == 5) {
                            indicator1 = addIntoIndicator(indicator1, "inp");
                            indicator1 = addIntoIndicator(indicator1, "vib");
                        }
                        if (profile._volumeZenMode == 6)
                            indicator1 = addIntoIndicator(indicator1, "inl");
                    } else {
                        // volume on
                        if ((profile._volumeRingerMode == 1) || (profile._volumeRingerMode == 2))
                            indicator1 = addIntoIndicator(indicator1, "rng");
                        // vibration
                        if ((profile._volumeRingerMode == 2) || (profile._volumeRingerMode == 3))
                            indicator1 = addIntoIndicator(indicator1, "vib");
                        // volume off
                        if (profile._volumeRingerMode == 4)
                            indicator1 = addIntoIndicator(indicator1, "sil");
                    }
                }
            }
            /*// vibrate when ringing
            if (profile._vibrateWhenRinging != 0) {
                if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_VIBRATE_WHEN_RINGING, this) == PreferenceAllowed.PREFERENCE_ALLOWED) {
                    if ((profile._vibrateWhenRinging == 1) || (profile._vibrateWhenRinging == 3))
                        indicator1 = addIntoIndicator(indicator1, "wr1");
                    if (profile._vibrateWhenRinging == 2)
                        indicator1 = addIntoIndicator(indicator1, "wr0");
                }
            }*/
            // volume level
            if (profile.getVolumeAlarmChange() ||
                profile.getVolumeMediaChange() ||
                profile.getVolumeNotificationChange() ||
                profile.getVolumeRingtoneChange() ||
                profile.getVolumeSystemChange() ||
                profile.getVolumeVoiceChange()) {
                if ((Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_VOLUME_ALARM, null, this).allowed == PreferenceAllowed.PREFERENCE_ALLOWED) ||
                    (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_VOLUME_MEDIA, null, this).allowed == PreferenceAllowed.PREFERENCE_ALLOWED) ||
                    (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_VOLUME_NOTIFICATION, null, this).allowed == PreferenceAllowed.PREFERENCE_ALLOWED) ||
                    (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_VOLUME_RINGTONE, null, this).allowed == PreferenceAllowed.PREFERENCE_ALLOWED) ||
                    (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_VOLUME_SYSTEM, null, this).allowed == PreferenceAllowed.PREFERENCE_ALLOWED) ||
                    (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_VOLUME_VOICE, null, this).allowed == PreferenceAllowed.PREFERENCE_ALLOWED))
                    indicator1 = addIntoIndicator(indicator1, "vol");
            }
            // speaker phone
            if (profile._volumeSpeakerPhone != 0) {
                if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_VOLUME_SPEAKER_PHONE, null, this).allowed == PreferenceAllowed.PREFERENCE_ALLOWED) {
                    if (profile._volumeSpeakerPhone == 1)
                        indicator1 = addIntoIndicator(indicator1, "sp1");
                    if (profile._volumeSpeakerPhone == 2)
                        indicator1 = addIntoIndicator(indicator1, "sp0");
                }
            }
            // sound
            if ((profile._soundRingtoneChange == 1) ||
                (profile._soundNotificationChange == 1) ||
                (profile._soundAlarmChange == 1)) {
                if ((Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_SOUND_RINGTONE_CHANGE, null, this).allowed == PreferenceAllowed.PREFERENCE_ALLOWED) ||
                    (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_SOUND_NOTIFICATION_CHANGE, null, this).allowed == PreferenceAllowed.PREFERENCE_ALLOWED) ||
                    (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_SOUND_ALARM_CHANGE, null, this).allowed == PreferenceAllowed.PREFERENCE_ALLOWED))
                    indicator1 = addIntoIndicator(indicator1, "snd");
            }
            // sound on touch
            if (profile._soundOnTouch != 0) {
                if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_SOUND_ON_TOUCH, null, this).allowed == PreferenceAllowed.PREFERENCE_ALLOWED) {
                    if ((profile._soundOnTouch == 1) || (profile._soundOnTouch == 3))
                        indicator1 = addIntoIndicator(indicator1, "st1");
                    if (profile._soundOnTouch == 2)
                        indicator1 = addIntoIndicator(indicator1, "st0");
                }
            }
            // vibration on touch
            if (profile._vibrationOnTouch != 0) {
                if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_VIBRATION_ON_TOUCH, null, this).allowed == PreferenceAllowed.PREFERENCE_ALLOWED) {
                    if ((profile._vibrationOnTouch == 1) || (profile._vibrationOnTouch == 3))
                        indicator1 = addIntoIndicator(indicator1, "vt1");
                    if (profile._vibrationOnTouch == 2)
                        indicator1 = addIntoIndicator(indicator1, "vt0");
                }
            }
            // dtmf tone when dialing
            if (profile._dtmfToneWhenDialing != 0) {
                if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_DTMF_TONE_WHEN_DIALING, null, this).allowed == PreferenceAllowed.PREFERENCE_ALLOWED) {
                    if ((profile._dtmfToneWhenDialing == 1) || (profile._dtmfToneWhenDialing == 3))
                        indicator1 = addIntoIndicator(indicator1, "dd1");
                    if (profile._dtmfToneWhenDialing == 2)
                        indicator1 = addIntoIndicator(indicator1, "dd0");
                }
            }
            // airplane mode
            if (profile._deviceAirplaneMode != 0) {
                if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_DEVICE_AIRPLANE_MODE, null, this).allowed == PreferenceAllowed.PREFERENCE_ALLOWED) {
                    if ((profile._deviceAirplaneMode == 1) || (profile._deviceAirplaneMode == 3))
                        indicator1 = addIntoIndicator(indicator1, "am1");
                    if (profile._deviceAirplaneMode == 2)
                        indicator1 = addIntoIndicator(indicator1, "am0");
                }
            }
            // auto-sync
            if (profile._deviceAutoSync != 0) {
                if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_DEVICE_AUTOSYNC, null, this).allowed == PreferenceAllowed.PREFERENCE_ALLOWED) {
                    if ((profile._deviceAutoSync == 1) || (profile._deviceAutoSync == 3))
                        indicator1 = addIntoIndicator(indicator1, "as1");
                    if (profile._deviceAutoSync == 2)
                        indicator1 = addIntoIndicator(indicator1, "as0");
                }
            }
            // Network type
            if (profile._deviceNetworkType != 0) {
                if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_DEVICE_NETWORK_TYPE, null, this).allowed == PreferenceAllowed.PREFERENCE_ALLOWED)
                    indicator1 = addIntoIndicator(indicator1, "ntt");
            }
            // Network type prefs
            if (profile._deviceNetworkTypePrefs != 0) {
                if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_DEVICE_NETWORK_TYPE_PREFS, null, this).allowed == PreferenceAllowed.PREFERENCE_ALLOWED)
                    indicator1 = addIntoIndicator(indicator1, "ntp");
            }
            // mobile data
            if (profile._deviceMobileData != 0) {
                if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_DEVICE_MOBILE_DATA, null, this).allowed == PreferenceAllowed.PREFERENCE_ALLOWED) {
                    if ((profile._deviceMobileData == 1) || (profile._deviceMobileData == 3))
                        indicator1 = addIntoIndicator(indicator1, "md1");
                    if (profile._deviceMobileData == 2)
                        indicator1 = addIntoIndicator(indicator1, "md0");
                }
            }
            // mobile data preferences
            if (profile._deviceMobileDataPrefs == 1) {
                if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_DEVICE_MOBILE_DATA_PREFS, null, this).allowed == PreferenceAllowed.PREFERENCE_ALLOWED)
                    indicator1 = addIntoIndicator(indicator1, "mdP");
            }
            // wifi
            if (profile._deviceWiFi != 0) {
                if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_DEVICE_WIFI, null, this).allowed == PreferenceAllowed.PREFERENCE_ALLOWED) {
                    if ((profile._deviceWiFi == 1) || (profile._deviceWiFi == 3) || (profile._deviceWiFi == 4) || (profile._deviceWiFi == 5))
                        indicator1 = addIntoIndicator(indicator1, "wf1");
                    if (profile._deviceWiFi == 2)
                        indicator1 = addIntoIndicator(indicator1, "wf0");
                }
            }
            // wifi AP
            if (profile._deviceWiFiAP != 0) {
                if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_DEVICE_WIFI_AP, null, this).allowed == PreferenceAllowed.PREFERENCE_ALLOWED) {
                    if ((profile._deviceWiFiAP == 1) || (profile._deviceWiFiAP == 3))
                        indicator1 = addIntoIndicator(indicator1, "wp1");
                    if (profile._deviceWiFiAP == 2)
                        indicator1 = addIntoIndicator(indicator1, "wp0");
                }
            }
            // wifi AP preferences
            if (profile._deviceWiFiAPPrefs == 1) {
                if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_DEVICE_WIFI_AP_PREFS, null, this).allowed == PreferenceAllowed.PREFERENCE_ALLOWED)
                    indicator1 = addIntoIndicator(indicator1, "wpP");
            }
            // bluetooth
            if (profile._deviceBluetooth != 0) {
                if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_DEVICE_BLUETOOTH, null, this).allowed == PreferenceAllowed.PREFERENCE_ALLOWED) {
                    if ((profile._deviceBluetooth == 1) || (profile._deviceBluetooth == 3))
                        indicator1 = addIntoIndicator(indicator1, "bt1");
                    if (profile._deviceBluetooth == 2)
                        indicator1 = addIntoIndicator(indicator1, "bt0");
                }
            }
            // gps
            if (profile._deviceGPS != 0) {
                if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_DEVICE_GPS, null, this).allowed == PreferenceAllowed.PREFERENCE_ALLOWED) {
                    if ((profile._deviceGPS == 1) || (profile._deviceGPS == 3))
                        indicator1 = addIntoIndicator(indicator1, "gp1");
                    if (profile._deviceGPS == 2)
                        indicator1 = addIntoIndicator(indicator1, "gp0");
                }
            }
            // location settings preferences
            if (profile._deviceLocationServicePrefs == 1) {
                if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_DEVICE_LOCATION_SERVICE_PREFS, null, this).allowed == PreferenceAllowed.PREFERENCE_ALLOWED)
                    indicator1 = addIntoIndicator(indicator1, "loP");
            }
            // nfc
            if (profile._deviceNFC != 0) {
                if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_DEVICE_NFC, null, this).allowed == PreferenceAllowed.PREFERENCE_ALLOWED) {
                    if ((profile._deviceNFC == 1) || (profile._deviceNFC == 3))
                        indicator1 = addIntoIndicator(indicator1, "nf1");
                    if (profile._deviceNFC == 2)
                        indicator1 = addIntoIndicator(indicator1, "nf0");
                }
            }
            // screen timeout
            if (profile._deviceScreenTimeout != 0) {
                if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_DEVICE_SCREEN_TIMEOUT, null, this).allowed == PreferenceAllowed.PREFERENCE_ALLOWED)
                    indicator1 = addIntoIndicator(indicator1, "stm");
            }
            // lock screen
            if (profile._deviceKeyguard != 0) {
                if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_DEVICE_KEYGUARD, null, this).allowed == PreferenceAllowed.PREFERENCE_ALLOWED) {
                    if ((profile._deviceKeyguard == 1) || (profile._deviceKeyguard == 3))
                        indicator1 = addIntoIndicator(indicator1, "kg1");
                    if (profile._deviceKeyguard == 2)
                        indicator1 = addIntoIndicator(indicator1, "kg0");
                }
            }
            // brightness/auto-brightness
            if (profile.getDeviceBrightnessChange())
            {
                if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_DEVICE_BRIGHTNESS, null, this).allowed == PreferenceAllowed.PREFERENCE_ALLOWED) {
                    if (profile.getDeviceBrightnessAutomatic())
                        indicator1 = addIntoIndicator(indicator1, "brA");
                    else
                        indicator1 = addIntoIndicator(indicator1, "brt");
                }
            }
            // auto-rotation
            if (profile._deviceAutoRotate != 0) {
                if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_DEVICE_AUTOROTATE, null, this).allowed == PreferenceAllowed.PREFERENCE_ALLOWED)
                    indicator1 = addIntoIndicator(indicator1, "rot");
            }
            // notification led
            if (profile._notificationLed != 0) {
                if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_NOTIFICATION_LED, null, this).allowed == PreferenceAllowed.PREFERENCE_ALLOWED) {
                    if ((profile._notificationLed == 1) || (profile._notificationLed == 3))
                        indicator1 = addIntoIndicator(indicator1, "nl1");
                    if (profile._notificationLed == 2)
                        indicator1 = addIntoIndicator(indicator1, "nl0");
                }
            }
            // heads-up notifications
            if (profile._headsUpNotifications != 0) {
                if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_HEADS_UP_NOTIFICATIONS, null, this).allowed == PreferenceAllowed.PREFERENCE_ALLOWED) {
                    if ((profile._headsUpNotifications == 1) || (profile._headsUpNotifications == 3))
                        indicator1 = addIntoIndicator(indicator1, "pn1");
                    if (profile._headsUpNotifications == 2)
                        indicator1 = addIntoIndicator(indicator1, "pn0");
                }
            }
            /*
            // screen night mode
            if (profile._screenNightMode != 0) {
                if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_SCREEN_NIGHT_MODE, null, this) == PreferenceAllowed.PREFERENCE_ALLOWED) {
                    if ((profile._screenNightMode == 1) || (profile._screenNightMode == 3))
                        indicator1 = addIntoIndicator(indicator1, "sn1");
                    if (profile._screenNightMode == 2)
                        indicator1 = addIntoIndicator(indicator1, "sn0");
                }
            }
            */
            // power save mode
            if (profile._devicePowerSaveMode != 0) {
                if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_DEVICE_POWER_SAVE_MODE, null, this).allowed == PreferenceAllowed.PREFERENCE_ALLOWED) {
                    if ((profile._devicePowerSaveMode == 1) || (profile._devicePowerSaveMode == 3))
                        indicator1 = addIntoIndicator(indicator1, "ps1");
                    if (profile._devicePowerSaveMode == 2)
                        indicator1 = addIntoIndicator(indicator1, "ps0");
                }
            }
            // run application
            if (profile._deviceRunApplicationChange == 1) {
                if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_DEVICE_RUN_APPLICATION_CHANGE, null, this).allowed == PreferenceAllowed.PREFERENCE_ALLOWED)
                    indicator1 = addIntoIndicator(indicator1, "rap");
            }
            // close all applications
            if (profile._deviceCloseAllApplications == 1) {
                if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_DEVICE_CLOSE_ALL_APPLICATIONS, null, this).allowed == PreferenceAllowed.PREFERENCE_ALLOWED)
                    indicator1 = addIntoIndicator(indicator1, "cap");
            }
            // force stop application
            if (profile._deviceForceStopApplicationChange == 1) {
                if ((Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_DEVICE_FORCE_STOP_APPLICATION_CHANGE, null, this).allowed == PreferenceAllowed.PREFERENCE_ALLOWED) &&
                        AccessibilityServiceBroadcastReceiver.isEnabled(this, PPApplication.VERSION_CODE_EXTENDER_2_0))
                    indicator1 = addIntoIndicator(indicator1, "sap");
            }
            // wallpaper
            if (profile._deviceWallpaperChange == 1) {
                if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_DEVICE_WALLPAPER_CHANGE, null, this).allowed == PreferenceAllowed.PREFERENCE_ALLOWED)
                    indicator1 = addIntoIndicator(indicator1, "wlp");
            }
            // lock device
            if (profile._lockDevice != 0) {
                if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_LOCK_DEVICE, null, this).allowed == PreferenceAllowed.PREFERENCE_ALLOWED)
                    indicator1 = addIntoIndicator(indicator1, "lck");
            }
            // disable wifi scanning
            if (profile._applicationDisableWifiScanning != 0) {
                if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_APPLICATION_DISABLE_WIFI_SCANNING, null, this).allowed == PreferenceAllowed.PREFERENCE_ALLOWED) {
                    if ((profile._applicationDisableWifiScanning == 1) || (profile._applicationDisableWifiScanning == 3))
                        indicator1 = addIntoIndicator(indicator1, "ws1");
                    if (profile._applicationDisableWifiScanning == 2)
                        indicator1 = addIntoIndicator(indicator1, "ws0");
                }
            }
            // disable bluetooth scanning
            if (profile._applicationDisableBluetoothScanning != 0) {
                if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_APPLICATION_DISABLE_BLUETOOTH_SCANNING, null, this).allowed == PreferenceAllowed.PREFERENCE_ALLOWED) {
                    if ((profile._applicationDisableBluetoothScanning == 1) || (profile._applicationDisableBluetoothScanning == 3))
                        indicator1 = addIntoIndicator(indicator1, "bs1");
                    if (profile._applicationDisableBluetoothScanning == 2)
                        indicator1 = addIntoIndicator(indicator1, "bs0");
                }
            }
            // disable location scanning
            if (profile._applicationDisableLocationScanning != 0) {
                if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_APPLICATION_DISABLE_LOCATION_SCANNING, null, this).allowed == PreferenceAllowed.PREFERENCE_ALLOWED) {
                    if ((profile._applicationDisableLocationScanning == 1) || (profile._applicationDisableLocationScanning == 3))
                        indicator1 = addIntoIndicator(indicator1, "ls1");
                    if (profile._applicationDisableLocationScanning == 2)
                        indicator1 = addIntoIndicator(indicator1, "ls0");
                }
            }
            // disable mobile cell scanning
            if (profile._applicationDisableMobileCellScanning != 0) {
                if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_APPLICATION_DISABLE_MOBILE_CELL_SCANNING, null, this).allowed == PreferenceAllowed.PREFERENCE_ALLOWED) {
                    if ((profile._applicationDisableMobileCellScanning == 1) || (profile._applicationDisableMobileCellScanning == 3))
                        indicator1 = addIntoIndicator(indicator1, "ms1");
                    if (profile._applicationDisableMobileCellScanning == 2)
                        indicator1 = addIntoIndicator(indicator1, "ms0");
                }
            }
            // disable orientation scanning
            if (profile._applicationDisableOrientationScanning != 0) {
                if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_APPLICATION_DISABLE_ORIENTATION_SCANNING, null, this).allowed == PreferenceAllowed.PREFERENCE_ALLOWED) {
                    if ((profile._applicationDisableOrientationScanning == 1) || (profile._applicationDisableOrientationScanning == 3))
                        indicator1 = addIntoIndicator(indicator1, "os1");
                    if (profile._applicationDisableOrientationScanning == 2)
                        indicator1 = addIntoIndicator(indicator1, "os0");
                }
            }
        }
        /////////////////////////////////////////////////////////////

        // intent
        Intent intent = new Intent(this, LauncherActivity.class);
        // clear all opened activities
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(PPApplication.EXTRA_STARTUP_SOURCE, PPApplication.STARTUP_SOURCE_WIDGET);

        String status = "";
        if (Event.getEventsBlocked(getApplicationContext()))
            if (Event.getForceRunEventRunning(getApplicationContext()))
            {
                /*if (android.os.Build.VERSION.SDK_INT >= 16)
                    status = "\u23E9";
                else*/
                    status = "[\u00BB]";
            }
            else
            {
                /*if (android.os.Build.VERSION.SDK_INT >= 16)
                    status = "\uD83D\uDC46";
                else */
                    status = "[M]";
            }

        // Publish the extension data update.
        publishUpdate(new ExtensionData()
                .visible(true)
                .icon(iconResource)
                .status(status)
                .expandedTitle(profileName)
                .expandedBody(indicator1)
                .contentDescription("PhoneProfilesPlus - "+profileName)
                .clickIntent(intent));		
    }

    public void updateExtension()
    {
        onUpdateData(UPDATE_REASON_CONTENT_CHANGED);
    }

}
