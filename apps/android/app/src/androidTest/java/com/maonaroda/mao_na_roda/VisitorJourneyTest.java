package com.maonaroda.mao_na_roda;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import android.widget.EditText;
import android.widget.TextView;
import com.maonaroda.mao_na_roda.data.*;
import com.maonaroda.mao_na_roda.data.DadosRotas.*;
import com.maonaroda.mao_na_roda.mapa.HomeViewModel;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class VisitorJourneyTest {
    @Test public void consultaManualMostraIncertezaEMantemEstadoAoGirar() {
        var context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.getSharedPreferences("prefs", 0).edit().putBoolean("onboarding_completed", false).commit();
        try (var scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                var repo = new RotasRepository() {
                    @Override public Cancelavel perfis(Retorno<List<Perfil>> r) { r.sucesso(List.of(new Perfil("cadeirante", "Cadeirante"))); return () -> { }; }
                    @Override public Cancelavel planejar(LocalRota a, LocalRota b, String perfil, Retorno<Consulta> r) {
                        r.sucesso(new Consulta("PARCIAL", List.of(new Rota("fixture", List.of(a.coordenada(), b.coordenada()), 150, 120,
                                "DESCONHECIDA", 0, 0, 0, "Dados desconhecidos")), "Dados desconhecidos não garantem acessibilidade."));
                        return () -> { };
                    }
                    @Override public void close() { }
                };
                var model = new ViewModelProvider(activity, new ViewModelProvider.Factory() {
                    @Override @SuppressWarnings("unchecked") public <T extends ViewModel> T create(Class<T> type) { return (T) new HomeViewModel(repo); }
                }).get(HomeViewModel.class);
                model.carregarPerfis(); model.perfil("cadeirante");
                model.editar(true, "-23.5, -46.6"); model.editar(false, "-23.6, -46.7");
                var host = (NavHostFragment) activity.getSupportFragmentManager().findFragmentById(R.id.nav_host);
                host.getNavController().navigate(R.id.homeFragment);
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> new ViewModelProvider(activity).get(HomeViewModel.class).consultar());
            scenario.recreate();
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> {
                assertEquals("-23.5, -46.6", ((EditText) activity.findViewById(R.id.origin_input)).getText().toString());
                assertTrue(((TextView) activity.findViewById(R.id.planning_status)).getText().toString().contains("não garantem"));
                var model = new ViewModelProvider(activity).get(HomeViewModel.class);
                assertEquals("DESCONHECIDA", model.estado().getValue().rotas().get(0).qualidade());
                assertFalse(context.getSharedPreferences("prefs", 0).contains("mobility_profile"));
                assertFalse(context.getSharedPreferences("prefs", 0).contains("accessibility_profiles"));
            });
        }
    }
}
