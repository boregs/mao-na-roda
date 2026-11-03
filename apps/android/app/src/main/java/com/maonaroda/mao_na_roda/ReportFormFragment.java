package com.maonaroda.mao_na_roda;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.textfield.TextInputEditText;
import java.io.IOException;

public class ReportFormFragment extends Fragment {
    private static final String[] CATEGORIES = {
            "Selecione uma categoria", "Calçada bloqueada ou danificada", "Ausência de rampa",
            "Rampa inadequada", "Elevador quebrado", "Escadas sem acessibilidade", "Outro"
    };
    private Uri photoUri;
    private final ActivityResultLauncher<String[]> imagePicker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null || !isAdded()) return;
                photoUri = uri;
                try { requireContext().getContentResolver().takePersistableUriPermission(
                        uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION); }
                catch (SecurityException ignored) { }
                ImageView preview = requireView().findViewById(R.id.report_photo_preview);
                preview.setImageURI(uri);
                preview.setVisibility(View.VISIBLE);
                requireView().findViewById(R.id.report_add_photo).setContentDescription("Alterar foto anexada");
            });

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_report_form, parent, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        AccessibilityUi.apply(view, AccessibilityProfileStore.preferences(requireContext()));
        Spinner category = view.findViewById(R.id.report_category);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, CATEGORIES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        category.setAdapter(adapter);
        view.findViewById(R.id.report_back).setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        view.findViewById(R.id.report_add_photo).setOnClickListener(v -> imagePicker.launch(new String[]{"image/*"}));
        view.findViewById(R.id.report_save).setOnClickListener(v -> save(category,
                view.findViewById(R.id.report_description)));
    }

    private void save(Spinner category, TextInputEditText descriptionInput) {
        int categoryPosition = category.getSelectedItemPosition();
        String description = descriptionInput.getText() == null ? "" : descriptionInput.getText().toString().trim();
        if (categoryPosition == 0) {
            Toast.makeText(requireContext(), "Selecione o tipo de barreira.", Toast.LENGTH_LONG).show();
            category.requestFocus();
            return;
        }
        if (description.isEmpty()) {
            descriptionInput.setError("Descreva a barreira encontrada.");
            descriptionInput.requestFocus();
            return;
        }
        try {
            BarrierReportStore.from(requireContext()).add(CATEGORIES[categoryPosition], description,
                    photoUri == null ? "" : photoUri.toString());
            Toast.makeText(requireContext(), "Relato salvo neste dispositivo.", Toast.LENGTH_LONG).show();
            NavHostFragment.findNavController(this).navigate(R.id.action_form_to_history);
        } catch (IOException error) {
            Toast.makeText(requireContext(), "Não foi possível salvar. Tente novamente.", Toast.LENGTH_LONG).show();
        }
    }
}
