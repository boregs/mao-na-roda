package com.maonaroda.mao_na_roda;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

public class ReportDetailFragment extends Fragment {
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup parent, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_report_detail, parent, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        AccessibilityUi.apply(view, AccessibilityProfileStore.preferences(requireContext()));
        view.findViewById(R.id.detail_back).setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        try {
            String id = requireArguments().getString("reportId");
            BarrierReport report = BarrierReportStore.from(requireContext()).find(id);
            if (report == null) { Toast.makeText(requireContext(), "Relato não encontrado.", Toast.LENGTH_LONG).show();
                NavHostFragment.findNavController(this).navigateUp(); return; }
            ((TextView) view.findViewById(R.id.detail_category)).setText(report.category + "\n" + getString(R.string.report_local_status));
            ((TextView) view.findViewById(R.id.detail_date)).setText(DateFormat.getDateTimeInstance().format(new Date(report.createdAt)));
            ((TextView) view.findViewById(R.id.detail_description)).setText(report.description);
            ImageView photo = view.findViewById(R.id.detail_photo);
            if (report.photoUri.isEmpty()) photo.setVisibility(View.GONE);
            else photo.setImageURI(Uri.parse(report.photoUri));
        } catch (IOException error) {
            Toast.makeText(requireContext(), "Não foi possível abrir o relato.", Toast.LENGTH_LONG).show();
        }
    }
}
