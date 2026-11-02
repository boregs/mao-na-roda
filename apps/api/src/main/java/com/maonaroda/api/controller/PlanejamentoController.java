package com.maonaroda.api.controller;

import java.util.List;
import org.springframework.web.bind.annotation.*;
import com.maonaroda.api.model.rota.*;
import com.maonaroda.api.repository.rota.PerfilMobilidadeRepository;
import com.maonaroda.api.service.rota.PlanejadorRotasService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

@RestController
@RequestMapping("/api/v1")
public class PlanejamentoController {
    private final PerfilMobilidadeRepository perfis;
    private final PlanejadorRotasService planejador;
    public PlanejamentoController(PerfilMobilidadeRepository perfis, PlanejadorRotasService planejador) {
        this.perfis = perfis; this.planejador = planejador;
    }
    @GetMapping("/perfis-mobilidade")
    public List<PerfilMobilidadeRepository.Perfil> perfis() { return perfis.listar(); }
    @PostMapping("/rotas")
    public ConsultaRotasResponse planejar(@RequestBody PlanejarRotaRequest request) { return planejador.planejar(request); }
    public record BuscaRequest(String texto) { }
    @Deprecated
    @PostMapping("/destinos/busca")
    @ResponseStatus(HttpStatus.GONE)
    public ProblemDetail buscar(@RequestBody BuscaRequest request) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.GONE,
                "Informe os endereços diretamente em POST /api/v1/rotas. A busca separada foi descontinuada.");
    }
}
