package com.maonaroda.mao_na_roda;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.materialswitch.MaterialSwitch;

public class AccessibilityProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent,
            @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_accessibility_profile, parent, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        var preferences = AccessibilityProfileStore.preferences(requireContext());
        AccessibilityUi.apply(view, preferences);
        view.findViewById(R.id.profile_back).setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp());
        bindSwitch(view, R.id.preference_large_text, AccessibilityProfileStore.LARGE_TEXT_KEY, preferences);
        bindSwitch(view, R.id.preference_high_contrast, AccessibilityProfileStore.HIGH_CONTRAST_KEY, preferences);
        bindSwitch(view, R.id.preference_large_buttons, AccessibilityProfileStore.LARGE_BUTTONS_KEY, preferences);
        bindSwitch(view, R.id.preference_reduce_motion, AccessibilityProfileStore.REDUCE_MOTION_KEY, preferences);
        view.findViewById(R.id.profile_saved_status).setVisibility(View.VISIBLE);
    }

    private void bindSwitch(View root, int viewId, String key,
            android.content.SharedPreferences preferences) {
        MaterialSwitch toggle = root.findViewById(viewId);
        toggle.setChecked(preferences.getBoolean(key, false));
        toggle.setOnCheckedChangeListener((button, checked) -> {
            preferences.edit().putBoolean(key, checked).apply();
            AccessibilityUi.apply(root, preferences);
            ((TextView) root.findViewById(R.id.profile_saved_status)).setText(R.string.profile_saved);
        });
    }
}
