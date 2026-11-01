package com.maonaroda.mao_na_roda;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.google.android.material.card.MaterialCardView;

/** Applies locally selected presentation preferences to screens that support them. */
final class AccessibilityUi {
    private AccessibilityUi() { }

    static void apply(View root, SharedPreferences preferences) {
        boolean largerText = preferences.getBoolean(AccessibilityProfileStore.LARGE_TEXT_KEY, false);
        boolean highContrast = preferences.getBoolean(AccessibilityProfileStore.HIGH_CONTRAST_KEY, false);
        boolean largerButtons = preferences.getBoolean(AccessibilityProfileStore.LARGE_BUTTONS_KEY, false);
        applyToTree(root, preferences, largerText, highContrast, largerButtons);
    }

    private static void applyToTree(View view, SharedPreferences preferences, boolean largerText,
            boolean highContrast, boolean largerButtons) {
        float density = view.getResources().getDisplayMetrics().density;
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            float originalSp;
            Object savedSize = text.getTag(R.id.accessibility_original_text_size);
            if (savedSize instanceof Float) originalSp = (Float) savedSize;
            else {
                originalSp = text.getTextSize() / view.getResources().getDisplayMetrics().scaledDensity;
                text.setTag(R.id.accessibility_original_text_size, originalSp);
            }
            text.setTextSize(TypedValue.COMPLEX_UNIT_SP, originalSp + (largerText ? 2 : 0));
            Object savedColor = text.getTag(R.id.accessibility_original_text_color);
            if (!(savedColor instanceof ColorStateList)) {
                savedColor = text.getTextColors();
                text.setTag(R.id.accessibility_original_text_color, savedColor);
            }
            if (highContrast) text.setTextColor(ContextCompat.getColor(view.getContext(), R.color.ink));
            else text.setTextColor((ColorStateList) savedColor);
        }
        if (view instanceof Button) {
            Button button = (Button) view;
            Object savedHeight = button.getTag(R.id.accessibility_original_min_height);
            if (!(savedHeight instanceof Integer)) {
                button.setTag(R.id.accessibility_original_min_height, button.getMinimumHeight());
                savedHeight = button.getMinimumHeight();
            }
            int baseHeight = (Integer) savedHeight;
            button.setMinimumHeight(largerButtons ? Math.max(baseHeight, (int) (64 * density)) : baseHeight);
            if (highContrast && button instanceof com.google.android.material.button.MaterialButton) {
                com.google.android.material.button.MaterialButton materialButton =
                        (com.google.android.material.button.MaterialButton) button;
                if (materialButton.getTag(R.id.accessibility_original_button_stroke_width) == null) {
                    materialButton.setTag(R.id.accessibility_original_button_stroke_width,
                            materialButton.getStrokeWidth());
                    materialButton.setTag(R.id.accessibility_original_button_stroke_color,
                            materialButton.getStrokeColor());
                }
                materialButton.setStrokeColor(ContextCompat.getColorStateList(view.getContext(), R.color.ink));
                materialButton.setStrokeWidth((int) (2 * density));
            } else if (button instanceof com.google.android.material.button.MaterialButton) {
                com.google.android.material.button.MaterialButton materialButton =
                        (com.google.android.material.button.MaterialButton) button;
                Object width = materialButton.getTag(R.id.accessibility_original_button_stroke_width);
                Object color = materialButton.getTag(R.id.accessibility_original_button_stroke_color);
                if (width instanceof Integer) materialButton.setStrokeWidth((Integer) width);
                if (color instanceof ColorStateList) materialButton.setStrokeColor((ColorStateList) color);
            }
        }
        if (view instanceof MaterialCardView) {
            MaterialCardView card = (MaterialCardView) view;
            card.setStrokeColor(ContextCompat.getColorStateList(view.getContext(), R.color.ink));
            card.setStrokeWidth(highContrast ? (int) (3 * density) : 0);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++)
                applyToTree(group.getChildAt(i), preferences, largerText, highContrast, largerButtons);
        }
    }
}
