package com.maonaroda.mao_na_roda;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.SharedPreferences;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class AccessibilityProfileStoreTest {
    @Test
    public void removesLegacyMobilityWithoutRemovingVisualPreferences() {
        FakePreferences preferences = new FakePreferences();
        preferences.edit().putString("mobility_profile", "crutches")
                .putStringSet("accessibility_profiles", Collections.singleton("wheelchair"))
                .putBoolean(AccessibilityProfileStore.LARGE_TEXT_KEY, true).apply();
        AccessibilityProfileStore.removerPerfisLegados(preferences);
        assertTrue(!preferences.contains("mobility_profile"));
        assertTrue(!preferences.contains("accessibility_profiles"));
        assertTrue(preferences.getBoolean(AccessibilityProfileStore.LARGE_TEXT_KEY, false));
    }

    /** Também usada por SessaoStoreTest. */
    static final class FakePreferences implements SharedPreferences {
        private final Map<String, Object> values = new HashMap<>();

        @Override public Map<String, ?> getAll() { return new HashMap<>(values); }
        @Override public String getString(String key, String fallback) {
            Object value = values.get(key); return value instanceof String ? (String) value : fallback;
        }
        @SuppressWarnings("unchecked")
        @Override public Set<String> getStringSet(String key, Set<String> fallback) {
            Object value = values.get(key); return value instanceof Set ? new HashSet<>((Set<String>) value) : fallback;
        }
        @Override public int getInt(String key, int fallback) { Object v = values.get(key); return v instanceof Integer ? (Integer) v : fallback; }
        @Override public long getLong(String key, long fallback) { Object v = values.get(key); return v instanceof Long ? (Long) v : fallback; }
        @Override public float getFloat(String key, float fallback) { Object v = values.get(key); return v instanceof Float ? (Float) v : fallback; }
        @Override public boolean getBoolean(String key, boolean fallback) { Object v = values.get(key); return v instanceof Boolean ? (Boolean) v : fallback; }
        @Override public boolean contains(String key) { return values.containsKey(key); }
        @Override public Editor edit() { return new FakeEditor(); }
        @Override public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) { }
        @Override public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) { }

        private final class FakeEditor implements Editor {
            private final Map<String, Object> changes = new HashMap<>();
            @Override public Editor putString(String key, String value) { changes.put(key, value); return this; }
            @Override public Editor putStringSet(String key, Set<String> value) { changes.put(key, new HashSet<>(value)); return this; }
            @Override public Editor putInt(String key, int value) { changes.put(key, value); return this; }
            @Override public Editor putLong(String key, long value) { changes.put(key, value); return this; }
            @Override public Editor putFloat(String key, float value) { changes.put(key, value); return this; }
            @Override public Editor putBoolean(String key, boolean value) { changes.put(key, value); return this; }
            @Override public Editor remove(String key) { changes.put(key, null); return this; }
            @Override public Editor clear() { values.clear(); return this; }
            @Override public boolean commit() { apply(); return true; }
            @Override public void apply() {
                for (Map.Entry<String, Object> entry : changes.entrySet()) {
                    if (entry.getValue() == null) values.remove(entry.getKey());
                    else values.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }
}
