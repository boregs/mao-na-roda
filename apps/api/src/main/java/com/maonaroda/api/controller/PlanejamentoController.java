package com.maonaroda.api.controller;

import java.util.List;
import org.springframework.web.bind.annotation.*;
import com.maonaroda.api.model.rota.*;
import com.maonaroda.api.repository.rota.PerfilMobilidadeRepository;
import com.maonaroda.api.service.rota.PlanejadorRotasService;
import com.maonaroda.api.integracao.busca.ProvedorBusca;

@RestController
@RequestMapping("/api/v1")
public class PlanejamentoController {
    private final PerfilMobilidadeRepository perfis;
    private final PlanejadorRotasService planejador;
    private final ProvedorBusca busca;
    public PlanejamentoController(PerfilMobilidadeRepository perfis, PlanejadorRotasService planejador, ProvedorBusca busca) {
        this.perfis = perfis; this.planejador = planejador; this.busca = busca;
    }
    @GetMapping("/perfis-mobilidade")
    public List<PerfilMobilidadeRepository.Perfil> perfis() { return perfis.listar(); }
    @PostMapping("/rotas")
    public ConsultaRotasResponse planejar(@RequestBody PlanejarRotaRequest request) { return planejador.planejar(request); }
    public record BuscaRequest(String texto) { }
    @PostMapping("/destinos/busca")
    public List<ProvedorBusca.Destino> buscar(@RequestBody BuscaRequest request) { return busca.buscar(request.texto()); }
}
