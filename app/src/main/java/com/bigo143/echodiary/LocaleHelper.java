package com.bigo143.echodiary;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import java.util.Locale;

public class LocaleHelper {

    public static Context setLocale(Context context, String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString("app_language", languageCode).apply();

        Configuration config = context.getResources().getConfiguration();
        config.setLocale(locale);

        return context.createConfigurationContext(config);
    }


    public static ContextWrapper wrap(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String language = prefs.getString("app_language", "en");
        Locale newLocale = new Locale(language);
        Locale.setDefault(newLocale);

        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(newLocale);
        Context newContext = context.createConfigurationContext(configuration);
        return new ContextWrapper(newContext);
    }
}
