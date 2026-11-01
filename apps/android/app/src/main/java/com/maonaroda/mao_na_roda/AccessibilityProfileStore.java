package com.maonaroda.mao_na_roda;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Local, device-only storage for accessibility profile and display preferences. */
final class AccessibilityProfileStore {
    static final String PREFS_NAME = "prefs";
    static final String PROFILES_KEY = "accessibility_profiles";
    static final String LARGE_TEXT_KEY = "accessibility_large_text";
    static final String HIGH_CONTRAST_KEY = "accessibility_high_contrast";
    static final String LARGE_BUTTONS_KEY = "accessibility_large_buttons";
    static final String REDUCE_MOTION_KEY = "accessibility_reduce_motion";

    static final String WHEELCHAIR = "wheelchair";
    static final String REDUCED_MOBILITY = "reduced_mobility";
    static final String VISUAL = "visual";
    static final String HEARING = "hearing";
    static final String OTHER = "other";
    static final String NONE = "none";

    private static final String LEGACY_MOBILITY_KEY = "mobility_profile";
    private static final String[] VALID_PROFILES = {
            WHEELCHAIR, REDUCED_MOBILITY, VISUAL, HEARING, OTHER, NONE
    };

    private AccessibilityProfileStore() { }

    static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    static Set<String> loadProfiles(SharedPreferences preferences) {
        if (preferences.contains(PROFILES_KEY)) {
            Set<String> saved = preferences.getStringSet(PROFILES_KEY, Collections.emptySet());
            return normalize(saved);
        }

        // Preserve the mobility choice saved by the previous home-screen implementation.
        String legacy = preferences.getString(LEGACY_MOBILITY_KEY, "unspecified");
        Set<String> migrated = new HashSet<>();
        if ("wheelchair".equals(legacy)) migrated.add(WHEELCHAIR);
        else if ("crutches".equals(legacy)) migrated.add(REDUCED_MOBILITY);
        else if ("other".equals(legacy)) migrated.add(OTHER);
        return migrated;
    }

    static void saveProfiles(SharedPreferences preferences, Set<String> profiles) {
        preferences.edit().putStringSet(PROFILES_KEY, new HashSet<>(normalize(profiles))).apply();
    }

    static Set<String> normalize(Set<String> profiles) {
        Set<String> clean = new HashSet<>();
        if (profiles != null) {
            for (String profile : profiles) {
                for (String valid : VALID_PROFILES) {
                    if (valid.equals(profile)) clean.add(valid);
                }
            }
        }
        if (clean.contains(NONE)) return Collections.singleton(NONE);
        return clean;
    }

    static String summary(Set<String> profiles) {
        Set<String> selected = normalize(profiles);
        if (selected.isEmpty()) return "Nenhuma necessidade específica selecionada";
        if (selected.contains(NONE)) return "Nenhuma necessidade específica";

        StringBuilder result = new StringBuilder();
        String[] keys = {WHEELCHAIR, REDUCED_MOBILITY, VISUAL, HEARING, OTHER};
        String[] labels = {"Cadeira de rodas", "Mobilidade reduzida", "Deficiência visual",
                "Deficiência auditiva", "Outro perfil"};
        int shown = 0;
        for (int i = 0; i < keys.length; i++) {
            if (!selected.contains(keys[i])) continue;
            if (shown == 2) {
                result.append(" +").append(selected.size() - shown);
                break;
            }
            if (shown > 0) result.append(" · ");
            result.append(labels[i]);
            shown++;
        }
        return result.toString();
    }
}
