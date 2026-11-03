package com.maonaroda.api.integracao.rotas;

import java.time.Duration;
import java.util.List;
import com.maonaroda.api.model.rota.PlanejarRotaRequest;

public interface ProvedorRotas {
    List<CandidataRota> consultar(PlanejarRotaRequest request, Duration timeout);
    default List<String> avisos() { return List.of(); }
}
