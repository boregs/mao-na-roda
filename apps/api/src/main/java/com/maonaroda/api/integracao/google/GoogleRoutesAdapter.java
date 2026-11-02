package com.maonaroda.api.integracao.google;

import com.maonaroda.api.integracao.rotas.*;
import com.maonaroda.api.model.rota.PlanejarRotaRequest;
import com.maonaroda.api.service.GoogleRoutesService;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.List;

@Component
public class GoogleRoutesAdapter implements ProvedorRotas {
    private final GoogleRoutesService service;
    public GoogleRoutesAdapter(GoogleRoutesService service) { this.service = service; }
    @Override public List<CandidataRota> consultar(PlanejarRotaRequest request, Duration timeout) {
        return service.consultarCandidatas(request, timeout);
    }
    @Override public List<String> avisos() { return List.of(GoogleRoutesService.AVISO_CAMINHADA); }
}
