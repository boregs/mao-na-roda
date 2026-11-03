package com.maonaroda.mao_na_roda;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.button.MaterialButton;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

public class ReportHistoryFragment extends Fragment {
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup parent, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_report_history, parent, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        AccessibilityUi.apply(view, AccessibilityProfileStore.preferences(requireContext()));
        view.findViewById(R.id.history_back).setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        LinearLayout list = view.findViewById(R.id.history_list);
        TextView empty = view.findViewById(R.id.history_empty);
        try {
            var reports = BarrierReportStore.from(requireContext()).all();
            empty.setVisibility(reports.isEmpty() ? View.VISIBLE : View.GONE);
            for (BarrierReport report : reports) {
                MaterialButton item = new MaterialButton(requireContext(), null,
                        com.google.android.material.R.attr.materialButtonOutlinedStyle);
                item.setText(report.category + "\n" + DateFormat.getDateTimeInstance().format(new Date(report.createdAt))
                        + "\n" + (report.description.length() > 90 ? report.description.substring(0, 90) + "…" : report.description));
                item.setContentDescription(report.category + ", relato local não verificado. " + report.description);
                item.setMinHeight((int) (88 * getResources().getDisplayMetrics().density));
                item.setGravity(android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);
                item.setOnClickListener(v -> {
                    Bundle args = new Bundle(); args.putString("reportId", report.id);
                    NavHostFragment.findNavController(this).navigate(R.id.action_history_to_report_detail, args);
                });
                list.addView(item, new LinearLayout.LayoutParams(-1, -2));
            }
        } catch (IOException error) {
            Toast.makeText(requireContext(), "Não foi possível carregar o histórico.", Toast.LENGTH_LONG).show();
        }
    }
}
