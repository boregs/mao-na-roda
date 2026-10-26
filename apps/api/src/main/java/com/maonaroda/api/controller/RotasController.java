package com.maonaroda.api.controller;

import com.maonaroda.api.rotas.ConsultaRota;
import com.maonaroda.api.service.GoogleRoutesService;
import com.maonaroda.api.rotas.RotaBase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rotas")
public class RotasController {
    private final GoogleRoutesService service;

    public RotasController(GoogleRoutesService service) {
        this.service = service;
    }

    @PostMapping("/base")
    @Operation(summary = "Consulta uma rota-base a pé, sem avaliar acessibilidade",
            description = "Informe endereço ou par de coordenadas por ponto. Não persiste a consulta.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(schema = @Schema(implementation = ConsultaRota.class), examples = @ExampleObject(
                            name = "Metrô Carrão até UNICID Tatuapé",
                            value = """
                                    {"origem":{"endereco":"Estação de Metrô Carrão, São Paulo, SP, Brasil"},
                                     "destino":{"endereco":"UNICID, Rua Cesário Galeno, 448, Tatuapé, São Paulo, SP, Brasil"}}
                                    """))))
    public RotaBase consultar(@RequestBody ConsultaRota consulta) {
        return service.consultar(consulta);
    }
}
