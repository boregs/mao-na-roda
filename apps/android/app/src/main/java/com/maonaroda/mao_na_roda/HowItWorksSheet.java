package com.maonaroda.mao_na_roda;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class HowItWorksSheet extends BottomSheetDialogFragment {
    public static void show(androidx.fragment.app.FragmentManager manager) {
        if (!manager.isStateSaved() && manager.findFragmentByTag("how_it_works") == null) {
            new HowItWorksSheet().show(manager, "how_it_works");
        }
    }
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle state) {
        return inflater.inflate(R.layout.sheet_how_it_works, parent, false);
    }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        view.findViewById(R.id.close).setOnClickListener(v -> dismiss());
    }
}
