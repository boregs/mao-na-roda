package com.maonaroda.mao_na_roda;

import android.os.Bundle;
import android.util.Patterns;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;

public class AuthFragment extends Fragment {
    boolean signIn;
    EditText name, surname, email, password;
    TextView error;

    public View onCreateView(LayoutInflater i, ViewGroup p, Bundle b) {
        return i.inflate(R.layout.fragment_auth, p, false);
    }

    public void onViewCreated(@NonNull View v, Bundle b) {
        signIn = getArguments() != null && getArguments().getBoolean("signIn");
        name = v.findViewById(R.id.name);
        surname = v.findViewById(R.id.surname);
        email = v.findViewById(R.id.email);
        password = v.findViewById(R.id.password);
        error = v.findViewById(R.id.error);
        com.google.android.material.appbar.MaterialToolbar toolbar = v.findViewById(R.id.toolbar);
        toolbar.setTitle(signIn ? R.string.sign_in : R.string.create_account);
        toolbar.setNavigationOnClickListener(
                x -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        v.<TextView>findViewById(R.id.title).setText(
                signIn ? R.string.auth_signin_title : R.string.auth_create_title);
        v.<TextView>findViewById(R.id.body).setText(
                signIn ? R.string.auth_signin_body : R.string.auth_create_body);
        name.setVisibility(signIn ? View.GONE : View.VISIBLE);
        surname.setVisibility(signIn ? View.GONE : View.VISIBLE);
        v.<Button>findViewById(R.id.submit).setText(
                signIn ? R.string.sign_in : R.string.create_account);
        v.<TextView>findViewById(R.id.switchMode).setText(
                signIn ? R.string.create_account : R.string.sign_in_link);
        v.findViewById(R.id.submit).setOnClickListener(x -> submit());
        v.findViewById(R.id.guest).setOnClickListener(x -> {
            requireContext().getSharedPreferences("prefs", 0)
                    .edit()
                    .putBoolean("onboarding_completed", true)
                    .apply();
            androidx.navigation.NavController nav =
                    androidx.navigation.fragment.NavHostFragment.findNavController(this);
            nav.popBackStack(R.id.onboardingFragment, false);
            nav.navigate(R.id.action_onboarding_to_home);
        });
        v.findViewById(R.id.switchMode).setOnClickListener(x -> {
            Bundle a = new Bundle();
            a.putBoolean("signIn", !signIn);
            androidx.navigation.fragment.NavHostFragment.findNavController(this)
                    .navigate(R.id.authFragment, a);
        });
    }

    void submit() {
        error.setText("");
        error.setVisibility(View.GONE);
        if (!signIn && name.getText().toString().trim().isEmpty()) {
            error.setText("Informe seu nome.");
            error.setVisibility(View.VISIBLE);
            return;
        }
        if (!signIn && surname.getText().toString().trim().isEmpty()) {
            error.setText("Informe seu sobrenome.");
            error.setVisibility(View.VISIBLE);
            return;
        }
        if (email.getText().toString().trim().isEmpty()) {
            error.setText("Informe seu e-mail.");
            error.setVisibility(View.VISIBLE);
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email.getText()).matches()) {
            error.setText("Informe um e-mail válido.");
            error.setVisibility(View.VISIBLE);
            return;
        }
        if (password.getText().length() < 8) {
            error.setText("A senha deve ter pelo menos 8 caracteres.");
            error.setVisibility(View.VISIBLE);
            return;
        }
        error.setText(R.string.auth_unavailable);
        error.setVisibility(View.VISIBLE);
    }
}
