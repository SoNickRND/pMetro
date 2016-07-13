package com.utyf.pmetro.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.utyf.pmetro.R;
import com.utyf.pmetro.util.LanguageUpdater;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Utyf on 12.04.2015.
 *
 */

public class SettingsActivity  extends PreferenceActivity {

    static boolean exit;
    static ArrayList<SettingsActivity> listAct = new ArrayList<>();

    static void addAct(SettingsActivity act) {
        if( listAct.indexOf(act)==-1 ) listAct.add(act);
    }

    static boolean delAct(SettingsActivity act) {
        listAct.remove(act);
        return listAct.isEmpty();
    }

    private LanguageUpdater languageUpdater;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // LanguageUpdater should be created before onCreate. Otherwise, language
        // is not updated in GeneralFragment
        languageUpdater = new LanguageUpdater(this, SET.lang);
        // For some reason language of the title is not updated if the title is set in
        // AndroidManifest.xml, so it is set manually
        setTitle(R.string.action_settings);
        super.onCreate(savedInstanceState);
        addAct(this);
    }

    @Override
    public void onDestroy() {
        if( delAct(this) ) exit = false;
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        if( exit ) finish();
        super.onResume();

        if (languageUpdater.isUpdateNeeded(SET.lang)) {
            // For some reason recreate() doesn't work here. If used, headers of SettingsActivity
            // do not respond to clicks.
            Intent intent = getIntent();
            finish();
            startActivity(intent);
        }
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preference_headers, target);
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        //return StockPreferenceFragment.class.getName().equals(fragmentName);
        return true; // MyPreferenceFragmentA.class.getName().equals(fragmentName)
                     // || MyPreferenceFragmentB.class.getName().equals(fragmentName)
    }

    public static class GeneralFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Make sure default values are applied.  In a real app, you would
            // want this in a shared function that is used to retrieve the
            // SharedPreferences wherever they are needed.
            //PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences_catalog2, false);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences_general);

            Preference connectionPref;
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());

            PreferenceScreen screen = getPreferenceScreen();
            ListPreference langPreference = (ListPreference)screen.findPreference(SET.KEY_LANGUAGE);

            langPreference.setKey(SET.KEY_LANGUAGE); //Refer to get the pref value
            int index = langPreference.findIndexOfValue(SET.lang);
            if (index != -1) {
                langPreference.setSummary(langPreference.getEntries()[index]);
            }
            else {
                Log.e("GeneralFragment", "Cannot find current language in preferences");
            }

            connectionPref = findPreference(SET.KEY_ROUTE_DIFFERENCE);
            connectionPref.setSummary(sp.getString(SET.KEY_ROUTE_DIFFERENCE, ""));
            connectionPref = findPreference(SET.KEY_ROUTE_MAX_TRANSFERS);
            connectionPref.setSummary(sp.getString(SET.KEY_ROUTE_MAX_TRANSFERS, ""));
            //connectionPref = findPreference(SET.KEY_HW_ACCELERATION);
            //connectionPref.setSummary(sp.getBoolean(KEY_HW_ACCELERATION, true) ? "true" : "false");
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

            Preference preference;
            switch (key) {
                case SET.KEY_LANGUAGE:
                    String newLang = sharedPreferences.getString(key, "");
                    if (!SET.lang.equals(newLang)) {
                        SET.lang = newLang;
                        getActivity().recreate();
                    }
                    break;
                case SET.KEY_ROUTE_DIFFERENCE:
                    preference = findPreference(key);
                    preference.setSummary(sharedPreferences.getString(key, ""));
                    SET.rDif = sharedPreferences.getInt(key, 3);
                    break;
                case SET.KEY_ROUTE_MAX_TRANSFERS:
                    preference = findPreference(key);
                    preference.setSummary(sharedPreferences.getString(key, ""));
                    SET.maxTransfer = sharedPreferences.getInt(key, 5);
                    break;
                case SET.KEY_HW_ACCELERATION:
                    SET.hw_acceleration = sharedPreferences.getBoolean(key, true);
                    // TODO change mode for current view
                    break;
            }
        }
    }

    public static class CatalogFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences_catalog);

            Preference connectionPref;
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
            connectionPref = findPreference(SET.KEY_CATALOG_STORAGE);
            connectionPref.setSummary(sp.getString(SET.KEY_CATALOG_STORAGE, ""));
            connectionPref = findPreference(SET.KEY_CATALOG_SITE);
            connectionPref.setSummary(sp.getString(SET.KEY_CATALOG_SITE, ""));
            connectionPref = findPreference(SET.KEY_SITE_MAP_PATH);
            connectionPref.setSummary(sp.getString(SET.KEY_SITE_MAP_PATH, ""));
            connectionPref = findPreference(SET.KEY_CATALOG_LIST);
            connectionPref.setSummary(sp.getString(SET.KEY_CATALOG_LIST, ""));
            connectionPref = findPreference(SET.KEY_CATALOG_UPDATE);
            connectionPref.setSummary(sp.getString(SET.KEY_CATALOG_UPDATE, ""));
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

            Preference connectionPref;
            switch (key) {
                case SET.KEY_CATALOG_STORAGE:
                    connectionPref = findPreference(key);
                    connectionPref.setSummary(sharedPreferences.getString(key, ""));
                    SET.storage = sharedPreferences.getString(key, SET.storage);
                    break;
                case SET.KEY_CATALOG_SITE:
                    connectionPref = findPreference(key);
                    connectionPref.setSummary(sharedPreferences.getString(key, ""));
                    SET.site = sharedPreferences.getString(key, SET.site);
                    break;
                case SET.KEY_SITE_MAP_PATH:
                    connectionPref = findPreference(key);
                    connectionPref.setSummary(sharedPreferences.getString(key, ""));
                    SET.site = sharedPreferences.getString(key, SET.site);
                    break;
                case SET.KEY_CATALOG_LIST:
                    connectionPref = findPreference(key);
                    connectionPref.setSummary(sharedPreferences.getString(key, ""));
                    SET.site = sharedPreferences.getString(key, SET.site);
                    break;
                case SET.KEY_CATALOG_UPDATE:
                    connectionPref = findPreference(key);
                    connectionPref.setSummary(sharedPreferences.getString(key, ""));
                    SET.cat_upd = sharedPreferences.getString(key, SET.cat_upd);
                    SET.checkUpdateScheduler();
                    break;
            }
        }
    }
}
