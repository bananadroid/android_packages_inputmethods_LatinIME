/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.inputmethod.latin;

import static com.android.inputmethod.latin.Constants.Subtype.ExtraValue.KEYBOARD_LAYOUT_SET;
import static com.android.inputmethod.latin.Constants.Subtype.ExtraValue.UNTRANSLATABLE_STRING_IN_SUBTYPE_NAME;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;
import android.view.inputmethod.InputMethodSubtype;

import com.android.inputmethod.latin.utils.CollectionUtils;
import com.android.inputmethod.latin.utils.DebugLogUtils;
import com.android.inputmethod.latin.utils.LocaleUtils;
import com.android.inputmethod.latin.utils.LocaleUtils.RunInLocale;
import com.android.inputmethod.latin.utils.StringUtils;

import java.util.HashMap;
import java.util.Locale;

public final class SubtypeLocale {
    static final String TAG = SubtypeLocale.class.getSimpleName();
    // This class must be located in the same package as LatinIME.java.
    private static final String RESOURCE_PACKAGE_NAME =
            DictionaryFactory.class.getPackage().getName();

    // Special language code to represent "no language".
    public static final String NO_LANGUAGE = "zz";
    public static final String QWERTY = "qwerty";
    public static final int UNKNOWN_KEYBOARD_LAYOUT = R.string.subtype_generic;

    private static boolean sInitialized = false;
    private static Resources sResources;
    private static String[] sPredefinedKeyboardLayoutSet;
    // Keyboard layout to its display name map.
    private static final HashMap<String, String> sKeyboardLayoutToDisplayNameMap =
            CollectionUtils.newHashMap();
    // Keyboard layout to subtype name resource id map.
    private static final HashMap<String, Integer> sKeyboardLayoutToNameIdsMap =
            CollectionUtils.newHashMap();
    // Exceptional locale to subtype name resource id map.
    private static final HashMap<String, Integer> sExceptionalLocaleToNameIdsMap =
            CollectionUtils.newHashMap();
    // Exceptional locale to subtype name with layout resource id map.
    private static final HashMap<String, Integer> sExceptionalLocaleToWithLayoutNameIdsMap =
            CollectionUtils.newHashMap();
    private static final String SUBTYPE_NAME_RESOURCE_PREFIX =
            "string/subtype_";
    private static final String SUBTYPE_NAME_RESOURCE_GENERIC_PREFIX =
            "string/subtype_generic_";
    private static final String SUBTYPE_NAME_RESOURCE_WITH_LAYOUT_PREFIX =
            "string/subtype_with_layout_";
    private static final String SUBTYPE_NAME_RESOURCE_NO_LANGUAGE_PREFIX =
            "string/subtype_no_language_";
    // Keyboard layout set name for the subtypes that don't have a keyboardLayoutSet extra value.
    // This is for compatibility to keep the same subtype ids as pre-JellyBean.
    private static final HashMap<String, String> sLocaleAndExtraValueToKeyboardLayoutSetMap =
            CollectionUtils.newHashMap();

    private SubtypeLocale() {
        // Intentional empty constructor for utility class.
    }

    // Note that this initialization method can be called multiple times.
    public static synchronized void init(final Context context) {
        if (sInitialized) return;

        final Resources res = context.getResources();
        sResources = res;

        final String[] predefinedLayoutSet = res.getStringArray(R.array.predefined_layouts);
        sPredefinedKeyboardLayoutSet = predefinedLayoutSet;
        final String[] layoutDisplayNames = res.getStringArray(
                R.array.predefined_layout_display_names);
        for (int i = 0; i < predefinedLayoutSet.length; i++) {
            final String layoutName = predefinedLayoutSet[i];
            sKeyboardLayoutToDisplayNameMap.put(layoutName, layoutDisplayNames[i]);
            final String resourceName = SUBTYPE_NAME_RESOURCE_GENERIC_PREFIX + layoutName;
            final int resId = res.getIdentifier(resourceName, null, RESOURCE_PACKAGE_NAME);
            sKeyboardLayoutToNameIdsMap.put(layoutName, resId);
            // Register subtype name resource id of "No language" with key "zz_<layout>"
            final String noLanguageResName = SUBTYPE_NAME_RESOURCE_NO_LANGUAGE_PREFIX + layoutName;
            final int noLanguageResId = res.getIdentifier(
                    noLanguageResName, null, RESOURCE_PACKAGE_NAME);
            final String key = getNoLanguageLayoutKey(layoutName);
            sKeyboardLayoutToNameIdsMap.put(key, noLanguageResId);
        }

        final String[] exceptionalLocales = res.getStringArray(
                R.array.subtype_locale_exception_keys);
        for (int i = 0; i < exceptionalLocales.length; i++) {
            final String localeString = exceptionalLocales[i];
            final String resourceName = SUBTYPE_NAME_RESOURCE_PREFIX + localeString;
            final int resId = res.getIdentifier(resourceName, null, RESOURCE_PACKAGE_NAME);
            sExceptionalLocaleToNameIdsMap.put(localeString, resId);
            final String resourceNameWithLayout =
                    SUBTYPE_NAME_RESOURCE_WITH_LAYOUT_PREFIX + localeString;
            final int resIdWithLayout = res.getIdentifier(
                    resourceNameWithLayout, null, RESOURCE_PACKAGE_NAME);
            sExceptionalLocaleToWithLayoutNameIdsMap.put(localeString, resIdWithLayout);
        }

        final String[] keyboardLayoutSetMap = res.getStringArray(
                R.array.locale_and_extra_value_to_keyboard_layout_set_map);
        for (int i = 0; i + 1 < keyboardLayoutSetMap.length; i += 2) {
            final String key = keyboardLayoutSetMap[i];
            final String keyboardLayoutSet = keyboardLayoutSetMap[i + 1];
            sLocaleAndExtraValueToKeyboardLayoutSetMap.put(key, keyboardLayoutSet);
        }

        sInitialized = true;
    }

    public static String[] getPredefinedKeyboardLayoutSet() {
        return sPredefinedKeyboardLayoutSet;
    }

    public static boolean isExceptionalLocale(final String localeString) {
        return sExceptionalLocaleToNameIdsMap.containsKey(localeString);
    }

    private static final String getNoLanguageLayoutKey(final String keyboardLayoutName) {
        return NO_LANGUAGE + "_" + keyboardLayoutName;
    }

    public static int getSubtypeNameId(final String localeString, final String keyboardLayoutName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                && isExceptionalLocale(localeString)) {
            return sExceptionalLocaleToWithLayoutNameIdsMap.get(localeString);
        }
        final String key = NO_LANGUAGE.equals(localeString)
                ? getNoLanguageLayoutKey(keyboardLayoutName)
                : keyboardLayoutName;
        final Integer nameId = sKeyboardLayoutToNameIdsMap.get(key);
        return nameId == null ? UNKNOWN_KEYBOARD_LAYOUT : nameId;
    }

    private static Locale getDisplayLocaleOfSubtypeLocale(final String localeString) {
        if (NO_LANGUAGE.equals(localeString)) {
            return sResources.getConfiguration().locale;
        }
        return LocaleUtils.constructLocaleFromString(localeString);
    }

    public static String getSubtypeLocaleDisplayNameInSystemLocale(final String localeString) {
        final Locale displayLocale = sResources.getConfiguration().locale;
        return getSubtypeLocaleDisplayNameInternal(localeString, displayLocale);
    }

    public static String getSubtypeLocaleDisplayName(final String localeString) {
        final Locale displayLocale = getDisplayLocaleOfSubtypeLocale(localeString);
        return getSubtypeLocaleDisplayNameInternal(localeString, displayLocale);
    }

    private static String getSubtypeLocaleDisplayNameInternal(final String localeString,
            final Locale displayLocale) {
        final Integer exceptionalNameResId = sExceptionalLocaleToNameIdsMap.get(localeString);
        final String displayName;
        if (exceptionalNameResId != null) {
            final RunInLocale<String> getExceptionalName = new RunInLocale<String>() {
                @Override
                protected String job(final Resources res) {
                    return res.getString(exceptionalNameResId);
                }
            };
            displayName = getExceptionalName.runInLocale(sResources, displayLocale);
        } else if (NO_LANGUAGE.equals(localeString)) {
            // No language subtype should be displayed in system locale.
            return sResources.getString(R.string.subtype_no_language);
        } else {
            final Locale locale = LocaleUtils.constructLocaleFromString(localeString);
            displayName = locale.getDisplayName(displayLocale);
        }
        return StringUtils.capitalizeFirstCodePoint(displayName, displayLocale);
    }

    // InputMethodSubtype's display name in its locale.
    //        isAdditionalSubtype (T=true, F=false)
    // locale layout  |  display name
    // ------ ------- - ----------------------
    //  en_US qwerty  F  English (US)            exception
    //  en_GB qwerty  F  English (UK)            exception
    //  es_US spanish F  Español (EE.UU.)        exception
    //  fr    azerty  F  Français
    //  fr_CA qwerty  F  Français (Canada)
    //  de    qwertz  F  Deutsch
    //  zz    qwerty  F  No language (QWERTY)    in system locale
    //  fr    qwertz  T  Français (QWERTZ)
    //  de    qwerty  T  Deutsch (QWERTY)
    //  en_US azerty  T  English (US) (AZERTY)   exception
    //  zz    azerty  T  No language (AZERTY)    in system locale

    private static String getReplacementString(final InputMethodSubtype subtype,
            final Locale displayLocale) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                && subtype.containsExtraValueKey(UNTRANSLATABLE_STRING_IN_SUBTYPE_NAME)) {
            return subtype.getExtraValueOf(UNTRANSLATABLE_STRING_IN_SUBTYPE_NAME);
        } else {
            return getSubtypeLocaleDisplayNameInternal(subtype.getLocale(), displayLocale);
        }
    }

    public static String getSubtypeDisplayNameInSystemLocale(final InputMethodSubtype subtype) {
        final Locale displayLocale = sResources.getConfiguration().locale;
        return getSubtypeDisplayNameInternal(subtype, displayLocale);
    }

    public static String getSubtypeDisplayName(final InputMethodSubtype subtype) {
        final Locale displayLocale = getDisplayLocaleOfSubtypeLocale(subtype.getLocale());
        return getSubtypeDisplayNameInternal(subtype, displayLocale);
    }

    private static String getSubtypeDisplayNameInternal(final InputMethodSubtype subtype,
            final Locale displayLocale) {
        final String replacementString = getReplacementString(subtype, displayLocale);
        final int nameResId = subtype.getNameResId();
        final RunInLocale<String> getSubtypeName = new RunInLocale<String>() {
            @Override
            protected String job(final Resources res) {
                try {
                    return res.getString(nameResId, replacementString);
                } catch (Resources.NotFoundException e) {
                    // TODO: Remove this catch when InputMethodManager.getCurrentInputMethodSubtype
                    // is fixed.
                    Log.w(TAG, "Unknown subtype: mode=" + subtype.getMode()
                            + " nameResId=" + subtype.getNameResId()
                            + " locale=" + subtype.getLocale()
                            + " extra=" + subtype.getExtraValue()
                            + "\n" + DebugLogUtils.getStackTrace());
                    return "";
                }
            }
        };
        return StringUtils.capitalizeFirstCodePoint(
                getSubtypeName.runInLocale(sResources, displayLocale), displayLocale);
    }

    public static boolean isNoLanguage(final InputMethodSubtype subtype) {
        final String localeString = subtype.getLocale();
        return NO_LANGUAGE.equals(localeString);
    }

    public static Locale getSubtypeLocale(final InputMethodSubtype subtype) {
        final String localeString = subtype.getLocale();
        return LocaleUtils.constructLocaleFromString(localeString);
    }

    public static String getKeyboardLayoutSetDisplayName(final InputMethodSubtype subtype) {
        final String layoutName = getKeyboardLayoutSetName(subtype);
        return getKeyboardLayoutSetDisplayName(layoutName);
    }

    public static String getKeyboardLayoutSetDisplayName(final String layoutName) {
        return sKeyboardLayoutToDisplayNameMap.get(layoutName);
    }

    public static String getKeyboardLayoutSetName(final InputMethodSubtype subtype) {
        String keyboardLayoutSet = subtype.getExtraValueOf(KEYBOARD_LAYOUT_SET);
        if (keyboardLayoutSet == null) {
            // This subtype doesn't have a keyboardLayoutSet extra value, so lookup its keyboard
            // layout set in sLocaleAndExtraValueToKeyboardLayoutSetMap to keep it compatible with
            // pre-JellyBean.
            final String key = subtype.getLocale() + ":" + subtype.getExtraValue();
            keyboardLayoutSet = sLocaleAndExtraValueToKeyboardLayoutSetMap.get(key);
        }
        // TODO: Remove this null check when InputMethodManager.getCurrentInputMethodSubtype is
        // fixed.
        if (keyboardLayoutSet == null) {
            android.util.Log.w(TAG, "KeyboardLayoutSet not found, use QWERTY: " +
                    "locale=" + subtype.getLocale() + " extraValue=" + subtype.getExtraValue());
            return QWERTY;
        }
        return keyboardLayoutSet;
    }
}
