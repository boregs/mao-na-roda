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
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.materialswitch.MaterialSwitch;
import java.util.HashSet;
import java.util.Set;

public class AccessibilityProfileFragment extends Fragment {
    private final Set<String> selectedProfiles = new HashSet<>();
    private boolean updatingControls;

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
        selectedProfiles.clear();
        selectedProfiles.addAll(AccessibilityProfileStore.loadProfiles(preferences));

        view.findViewById(R.id.profile_back).setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp());
        bindProfile(view, R.id.profile_wheelchair, AccessibilityProfileStore.WHEELCHAIR, preferences);
        bindProfile(view, R.id.profile_reduced_mobility, AccessibilityProfileStore.REDUCED_MOBILITY, preferences);
        bindProfile(view, R.id.profile_visual, AccessibilityProfileStore.VISUAL, preferences);
        bindProfile(view, R.id.profile_hearing, AccessibilityProfileStore.HEARING, preferences);
        bindProfile(view, R.id.profile_other, AccessibilityProfileStore.OTHER, preferences);
        bindProfile(view, R.id.profile_none, AccessibilityProfileStore.NONE, preferences);
        bindSwitch(view, R.id.preference_large_text, AccessibilityProfileStore.LARGE_TEXT_KEY, preferences);
        bindSwitch(view, R.id.preference_high_contrast, AccessibilityProfileStore.HIGH_CONTRAST_KEY, preferences);
        bindSwitch(view, R.id.preference_large_buttons, AccessibilityProfileStore.LARGE_BUTTONS_KEY, preferences);
        bindSwitch(view, R.id.preference_reduce_motion, AccessibilityProfileStore.REDUCE_MOTION_KEY, preferences);
        view.findViewById(R.id.profile_saved_status).setVisibility(View.VISIBLE);
    }

    private void bindProfile(View root, int viewId, String profile,
            android.content.SharedPreferences preferences) {
        MaterialCheckBox checkBox = root.findViewById(viewId);
        checkBox.setChecked(selectedProfiles.contains(profile));
        checkBox.setOnCheckedChangeListener((button, checked) -> {
            if (updatingControls) return;
            if (AccessibilityProfileStore.NONE.equals(profile) && checked) {
                selectedProfiles.clear();
                selectedProfiles.add(AccessibilityProfileStore.NONE);
                updateProfileChecks(root);
            } else {
                if (checked) {
                    selectedProfiles.remove(AccessibilityProfileStore.NONE);
                    selectedProfiles.add(profile);
                } else {
                    selectedProfiles.remove(profile);
                }
                updateProfileChecks(root);
            }
            AccessibilityProfileStore.saveProfiles(preferences, selectedProfiles);
            ((TextView) root.findViewById(R.id.profile_saved_status)).setText(R.string.profile_saved);
        });
    }

    private void updateProfileChecks(View root) {
        updatingControls = true;
        ((MaterialCheckBox) root.findViewById(R.id.profile_wheelchair))
                .setChecked(selectedProfiles.contains(AccessibilityProfileStore.WHEELCHAIR));
        ((MaterialCheckBox) root.findViewById(R.id.profile_reduced_mobility))
                .setChecked(selectedProfiles.contains(AccessibilityProfileStore.REDUCED_MOBILITY));
        ((MaterialCheckBox) root.findViewById(R.id.profile_visual))
                .setChecked(selectedProfiles.contains(AccessibilityProfileStore.VISUAL));
        ((MaterialCheckBox) root.findViewById(R.id.profile_hearing))
                .setChecked(selectedProfiles.contains(AccessibilityProfileStore.HEARING));
        ((MaterialCheckBox) root.findViewById(R.id.profile_other))
                .setChecked(selectedProfiles.contains(AccessibilityProfileStore.OTHER));
        ((MaterialCheckBox) root.findViewById(R.id.profile_none))
                .setChecked(selectedProfiles.contains(AccessibilityProfileStore.NONE));
        updatingControls = false;
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
