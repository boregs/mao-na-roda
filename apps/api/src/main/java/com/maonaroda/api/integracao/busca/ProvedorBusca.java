package com.maonaroda.api.integracao.busca;

import java.util.List;
import com.maonaroda.api.model.rota.Coordenada;

public interface ProvedorBusca {
    record Destino(String id, String nome, Coordenada coordenada) { }
    List<Destino> buscar(String texto);
}
