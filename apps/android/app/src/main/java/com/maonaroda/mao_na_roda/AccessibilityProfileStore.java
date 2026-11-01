package com.maonaroda.mao_na_roda;

import android.content.Context;
import android.content.SharedPreferences;

/** Somente preferências visuais; o perfil de rota fica no ViewModel da sessão. */
final class AccessibilityProfileStore {
    static final String PREFS_NAME = "prefs";
    static final String LARGE_TEXT_KEY = "accessibility_large_text";
    static final String HIGH_CONTRAST_KEY = "accessibility_high_contrast";
    static final String LARGE_BUTTONS_KEY = "accessibility_large_buttons";
    static final String REDUCE_MOTION_KEY = "accessibility_reduce_motion";
    private AccessibilityProfileStore() { }
    static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    static void removerPerfisLegados(SharedPreferences preferences) {
        preferences.edit().remove("mobility_profile").remove("accessibility_profiles").apply();
    }
}
