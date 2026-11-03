package com.maonaroda.api.service.rota;

import java.util.List;
import com.maonaroda.api.integracao.rotas.CandidataRota;
import com.maonaroda.api.repository.rota.EvidenciaRepository.Evidencia;
import org.springframework.stereotype.Component;

@Component
public class AssociadorEspacial {
    public List<Evidencia> associar(CandidataRota rota, List<Evidencia> evidencias) {
        // Implementação piloto: a revisão humana resolve lado/nível/entrada antes de publicar.
        // Geometria diferente (inclusive outra direção) permanece desconhecida.
        String assinatura = rota.assinatura();
        return evidencias.stream().filter(e -> assinatura.equals(e.assinatura())).toList();
    }
}
