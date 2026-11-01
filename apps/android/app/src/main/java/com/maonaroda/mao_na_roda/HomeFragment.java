package com.maonaroda.mao_na_roda;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

public class HomeFragment extends Fragment {
    private TextView profileSummary;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle state) {
        return inflater.inflate(R.layout.fragment_home, parent, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle state) {
        super.onViewCreated(view, state);
        AccessibilityUi.apply(view, AccessibilityProfileStore.preferences(requireContext()));
        profileSummary = view.findViewById(R.id.profile_summary);
        updateProfile();

        BottomSheetBehavior<View> panel = BottomSheetBehavior.from(view.findViewById(R.id.map_panel));
        View toggle = view.findViewById(R.id.panel_toggle);
        toggle.setOnClickListener(v -> panel.setState(panel.getState() == BottomSheetBehavior.STATE_EXPANDED
                ? BottomSheetBehavior.STATE_COLLAPSED : BottomSheetBehavior.STATE_EXPANDED));
        panel.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View sheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED)
                    toggle.setContentDescription("Recolher painel de ações");
                else if (newState == BottomSheetBehavior.STATE_COLLAPSED)
                    toggle.setContentDescription("Expandir painel de ações");
            }

            @Override
            public void onSlide(@NonNull View sheet, float offset) { }
        });

        view.findViewById(R.id.account).setOnClickListener(v -> showUnavailable(
                "Conta", "A autenticação ainda não está conectada. O envio de relatos exigirá uma conta."));
        view.findViewById(R.id.profile).setOnClickListener(v -> {
            boolean reduceMotion = AccessibilityProfileStore.preferences(requireContext())
                    .getBoolean(AccessibilityProfileStore.REDUCE_MOTION_KEY, false);
            int fade = reduceMotion ? 0 : R.anim.profile_fade;
            androidx.navigation.NavOptions options = new androidx.navigation.NavOptions.Builder()
                    .setEnterAnim(fade).setExitAnim(fade).setPopEnterAnim(fade).setPopExitAnim(fade)
                    .build();
            androidx.navigation.fragment.NavHostFragment.findNavController(this)
                    .navigate(R.id.action_home_to_accessibility_profile, null, options);
        });
        view.findViewById(R.id.search).setOnClickListener(v -> showSearch());
        view.findViewById(R.id.report_barrier).setOnClickListener(v -> showUnavailable(
                "Relatar uma barreira", "O envio de relatos ainda não está disponível. Os novos relatos serão identificados como não validados."));
        view.findViewById(R.id.about).setOnClickListener(v -> HowItWorksSheet.show(getParentFragmentManager()));
    }

    private void updateProfile() {
        profileSummary.setText(AccessibilityProfileStore.summary(
                AccessibilityProfileStore.loadProfiles(
                        AccessibilityProfileStore.preferences(requireContext()))));
    }

    private void showSearch() {
        int side = (int) (24 * getResources().getDisplayMetrics().density);
        LinearLayout fields = new LinearLayout(requireContext());
        fields.setOrientation(LinearLayout.VERTICAL);
        fields.setPadding(side, side / 2, side, 0);

        TextInputLayout originLayout = createInput("Origem", "Ponto de partida");
        TextInputLayout destinationLayout = createInput("Destino", "Destino da rota");
        fields.addView(originLayout);
        LinearLayout.LayoutParams destinationParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        destinationParams.topMargin = side / 2;
        fields.addView(destinationLayout, destinationParams);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Planejar caminho")
                .setMessage("Informe origem e destino. A busca e o cálculo de rotas ainda não estão conectados.")
                .setView(fields)
                .setNegativeButton("Fechar", null)
                .setPositiveButton("Buscar rota", (dialog, which) -> Toast.makeText(requireContext(),
                        "A busca de lugares ainda não está disponível.", Toast.LENGTH_LONG).show())
                .show();
    }

    private TextInputLayout createInput(String hint, String label) {
        TextInputLayout inputLayout = new TextInputLayout(requireContext());
        inputLayout.setHint(hint);
        EditText input = new EditText(requireContext());
        input.setSingleLine(true);
        input.setTextSize(20);
        input.setMinHeight((int) (56 * getResources().getDisplayMetrics().density));
        input.setContentDescription(label);
        inputLayout.addView(input);
        return inputLayout;
    }

    private void showUnavailable(String title, String message) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Entendi", null)
                .show();
    }
}
