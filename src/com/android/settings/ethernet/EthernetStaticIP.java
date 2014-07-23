package com.android.settings.ethernet;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ContentResolver;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;

import android.provider.Settings.System;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

import android.text.TextUtils;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.Formatter;

import android.net.NetworkInfo.DetailedState;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.Context;
import android.net.NetworkInfo;

import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.app.AlertDialog;
import com.android.settings.R;

import android.net.ethernet.EthernetManager;
import android.net.ethernet.EthernetDataTracker;

public class EthernetStaticIP  extends PreferenceActivity
    implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "EthernetStaticIP";
    public static final boolean DEBUG = false;

    private static void LOG(String msg) {
        if ( DEBUG ) {
            Log.d(TAG, msg);
        }
    }

    /*-------------------------------------------------------*/

    private static final String KEY_USE_STATIC_IP = "use_static_ip";

    private static final String KEY_IP_ADDRESS = "ip_address";
    private static final String KEY_GATEWAY = "gateway";
    private static final String KEY_NETMASK = "netmask";
    private static final String KEY_DNS1 = "dns1";
    private static final String KEY_DNS2 = "dns2";

    private static final int MENU_ITEM_SAVE = Menu.FIRST;
    private static final int MENU_ITEM_CANCEL = Menu.FIRST + 1;

    private EthernetManager mEthManager;

    private String[] mSettingNames = {
        System.ETHERNET_STATIC_IP,
        System.ETHERNET_STATIC_GATEWAY,
        System.ETHERNET_STATIC_NETMASK,
        System.ETHERNET_STATIC_DNS1,
        System.ETHERNET_STATIC_DNS2
    };

    private String[] mPreferenceKeys = {
        KEY_IP_ADDRESS,
        KEY_GATEWAY,
        KEY_NETMASK,
        KEY_DNS1,
        KEY_DNS2,
    };

    /*-------------------------------------------------------*/

    private CheckBoxPreference mUseStaticIpCheckBox;

    // private Preference mIpNetworkStatusPref;

    // private EthernetEnabler mEthernetEnabler;

    /*-------------------------------------------------------*/

    //private int mEthState;
    private boolean chageState = false;
    //============================
    // Activity lifecycle
    //============================

    public EthernetStaticIP() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.ethernet_static_ip);

        mUseStaticIpCheckBox = (CheckBoxPreference)findPreference(KEY_USE_STATIC_IP);

        for ( int i = 0; i < mPreferenceKeys.length; i++ ) {
            Preference preference = findPreference(mPreferenceKeys[i] );
            preference.setOnPreferenceChangeListener(this);
        }

        mEthManager = (EthernetManager) getSystemService(Context.ETHERNET_SERVICE);
        if (mEthManager == null) {
            Log.e(TAG, "get ethernet manager failed");
            return;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        //mEthState = mEthMgr.getEthernetState();
        updateIpSettingsInfo();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if(!chageState) {
                finish();
                return true;
            }

            new AlertDialog.Builder(EthernetStaticIP.this)
                .setTitle(R.string.str_about)
                .setMessage(R.string.str_mesg)
                .setPositiveButton(R.string.str_ok,
                                   new DialogInterface.OnClickListener() {
                                       public void onClick(DialogInterface dialoginterfacd,int i) {
                                           saveIpSettingsInfo();
                                           int preState = mEthManager.getEthernetIfaceState();
                                           mEthManager.setEthernetEnabled(false);
                                           if (preState == EthernetDataTracker.ETHER_IFACE_STATE_UP) {
                                               mEthManager.setEthernetEnabled(true);
                                           }
                                           finish();
                                       }
                                   }
                                   )
                .setNegativeButton(R.string.str_cancel,
                                   new DialogInterface.OnClickListener() {
                                       public void onClick(DialogInterface dialoginterfacd,int i) {
                                           finish();
                                       }
                                   }
                                   )
                .show();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void updateIpSettingsInfo() {
    	LOG("Static IP status updateIpSettingsInfo");
        ContentResolver contentResolver = getContentResolver();

        mUseStaticIpCheckBox.setChecked(System.getInt(contentResolver, System.ETHERNET_USE_STATIC_IP, 0) != 0);

        for (int i = 0; i < mSettingNames.length; i++) {
            EditTextPreference preference = (EditTextPreference) findPreference(mPreferenceKeys[i]);
            String settingValue = System.getString(contentResolver, mSettingNames[i]);
            preference.setText(settingValue);
            preference.setSummary(settingValue);
        }
    }

    private void saveIpSettingsInfo() {
        ContentResolver contentResolver = getContentResolver();

        if (!chageState)
            return;

        if (!isIpDataInUiComplete()) {
            Toast.makeText(this, R.string.save_failed, Toast.LENGTH_LONG).show();
            return;
        }

        for (int i = 0; i < mSettingNames.length; i++) {
            EditTextPreference preference = (EditTextPreference) findPreference(mPreferenceKeys[i]);
            String text = preference.getText();
            if ( null == text || TextUtils.isEmpty(text) ) {
                System.putString(contentResolver, mSettingNames[i], null);
            } else {
                System.putString(contentResolver, mSettingNames[i], text);
            }
        }

        System.putInt(contentResolver, System.ETHERNET_USE_STATIC_IP, mUseStaticIpCheckBox.isChecked() ? 1 : 0);
    }

    //============================
    // Preference callbacks
    //============================

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean result = true;
        LOG("onPreferenceTreeClick()  chageState = " + chageState);
        chageState = true;

        return result;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean result = true;
        String key = preference.getKey();
        LOG("onPreferenceChange() : key = " + key);

        if ( null == key ) {
            return true;

        } else if ( key.equals(KEY_IP_ADDRESS)
                    || key.equals(KEY_GATEWAY)
                    || key.equals(KEY_NETMASK)
                    || key.equals(KEY_DNS1)
                    || key.equals(KEY_DNS2) ) {

            String value = (String) newValue;

            LOG("onPreferenceChange() : value = " + value);

            if ( TextUtils.isEmpty(value) ) {
                ( (EditTextPreference)preference).setText(value);
                preference.setSummary(value);
                result = true;
            }
            else  if ( !isValidIpAddress(value) ) {
                LOG("onPreferenceChange() : IP address user inputed is INVALID." );
                Toast.makeText(this, R.string.ethernet_ip_settings_invalid_ip, Toast.LENGTH_LONG).show();
                return false;
            }
            else {
                ( (EditTextPreference)preference).setText(value);
                preference.setSummary(value);
                result = true;
            }

            configEnableNewIpSettingsCheckBox();
        }

        return result;
    }

    private boolean isValidIpAddress(String value) {
        int start = 0;
        int end = value.indexOf('.');
        int numBlocks = 0;

        while (start < value.length()) {
            if ( -1 == end ) {
                end = value.length();
            }

            try {
                int block = Integer.parseInt(value.substring(start, end));
                if ((block > 255) || (block < 0)) {
                    Log.w(TAG, "isValidIpAddress() : invalid 'block', block = " + block);
                    return false;
                }
            } catch (NumberFormatException e) {
                Log.w(TAG, "isValidIpAddress() : e = " + e);
                return false;
            }

            numBlocks++;

            start = end + 1;
            end = value.indexOf('.', start);
        }

        return numBlocks == 4;
    }

    private boolean isIpDataInUiComplete() {
        ContentResolver contentResolver = getContentResolver();

        for (int i = 0; i < (mPreferenceKeys.length - 1); i++) {
            EditTextPreference preference = (EditTextPreference) findPreference(mPreferenceKeys[i]);
            String text = preference.getText();
            LOG("isIpDataInUiComplete() : text = " + text);

            if ( null == text || TextUtils.isEmpty(text) ) {
                return false;
            }
        }

        return true;
    }

    private void configEnableNewIpSettingsCheckBox() {
        if (!isIpDataInUiComplete()) {
            Toast.makeText(this, R.string.eth_ip_settings_please_complete_settings, Toast.LENGTH_LONG).show();
        }
    }
}
