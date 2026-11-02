package com.maonaroda.api.model.rota;

import com.maonaroda.api.rotas.PontoRota;

public record PlanejarRotaRequest(PontoRota origem, PontoRota destino, String perfil) {
    public PlanejarRotaRequest {
        if (origem == null || destino == null) throw new IllegalArgumentException("Informe origem e destino.");
        origem.waypoint();
        destino.waypoint();
        if (!java.util.Set.of("cadeirante", "muleta_ou_outro_auxilio", "mobilidade_reduzida").contains(perfil == null ? "" : perfil))
            throw new IllegalArgumentException("Selecione um dos três perfis de mobilidade.");
        if (origem.equals(destino)) throw new IllegalArgumentException("Origem e destino devem ser diferentes.");
    }
    public PlanejarRotaRequest(Coordenada origem, Coordenada destino, String perfil) {
        this(ponto(origem), ponto(destino), perfil);
    }
    private static PontoRota ponto(Coordenada coordenada) {
        return coordenada == null ? null : new PontoRota(coordenada.latitude(), coordenada.longitude(), null);
    }
}
