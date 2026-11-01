package com.maonaroda.api.model.rota;

public record PlanejarRotaRequest(Coordenada origem, Coordenada destino, String perfil) {
    public PlanejarRotaRequest {
        if (origem == null || destino == null) throw new IllegalArgumentException("Informe origem e destino.");
        if (!java.util.Set.of("cadeirante", "muleta_ou_outro_auxilio", "mobilidade_reduzida").contains(perfil == null ? "" : perfil))
            throw new IllegalArgumentException("Selecione um dos três perfis de mobilidade.");
        if (origem.equals(destino)) throw new IllegalArgumentException("Origem e destino devem ser diferentes.");
    }
}
