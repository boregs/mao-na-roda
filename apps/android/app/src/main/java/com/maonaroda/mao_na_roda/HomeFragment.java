package com.maonaroda.mao_na_roda;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

public class HomeFragment extends Fragment {
    private static final String[] PROFILES = {"Não informado", "Cadeira de rodas", "Muletas", "Outra necessidade de mobilidade"};
    private static final String[] PROFILE_KEYS = {"unspecified", "wheelchair", "crutches", "other"};
    private SharedPreferences preferences;
    private TextView profileSummary;

    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle state) {
        return inflater.inflate(R.layout.fragment_home, parent, false);
    }
    @Override public void onViewCreated(@NonNull View view, Bundle state) {
        super.onViewCreated(view, state);
        preferences = requireContext().getSharedPreferences("prefs", 0);
        profileSummary = view.findViewById(R.id.profile_summary);
        updateProfile();
        BottomSheetBehavior<View> panel = BottomSheetBehavior.from(view.findViewById(R.id.map_panel));
        View toggle = view.findViewById(R.id.panel_toggle);
        toggle.setOnClickListener(v -> panel.setState(panel.getState() == BottomSheetBehavior.STATE_EXPANDED
                ? BottomSheetBehavior.STATE_COLLAPSED : BottomSheetBehavior.STATE_EXPANDED));
        panel.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override public void onStateChanged(@NonNull View sheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) toggle.setContentDescription("Recolher painel");
                else if (newState == BottomSheetBehavior.STATE_COLLAPSED) toggle.setContentDescription("Expandir painel");
            }
            @Override public void onSlide(@NonNull View sheet, float offset) { }
        });
        view.findViewById(R.id.account).setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Conta").setMessage("A autenticação ainda não está disponível.")
                .setPositiveButton("Entendi", null).show());
        view.findViewById(R.id.profile).setOnClickListener(v -> chooseProfile());
        view.findViewById(R.id.search).setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Buscar destino")
                .setMessage("A busca de lugares ainda não está disponível.")
                .setPositiveButton("Entendi", null).show());
        view.findViewById(R.id.about).setOnClickListener(v -> HowItWorksSheet.show(getParentFragmentManager()));
    }
    private int selectedProfile() {
        String current = preferences.getString("mobility_profile", PROFILE_KEYS[0]);
        for (int i = 0; i < PROFILE_KEYS.length; i++) if (PROFILE_KEYS[i].equals(current)) return i;
        return 0;
    }
    private void updateProfile() { profileSummary.setText(PROFILES[selectedProfile()]); }
    private void chooseProfile() {
        new MaterialAlertDialogBuilder(requireContext()).setTitle("Sua mobilidade")
                .setSingleChoiceItems(PROFILES, selectedProfile(), (dialog, which) -> {
                    preferences.edit().putString("mobility_profile", PROFILE_KEYS[which]).apply();
                    updateProfile();
                    dialog.dismiss();
                }).setNegativeButton("Cancelar", null).show();
    }
}
