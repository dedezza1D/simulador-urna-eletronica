package com.estudo.votacao.apuracao;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.estudo.votacao.eleicao.Cargo;

@RestController
public class ResultadoController {

    private final ResultadoService service;

    public ResultadoController(ResultadoService service) {
        this.service = service;
    }

    @GetMapping("/api/eleicoes/{eleicaoId}/resultado")
    public ResultadoResponse resultado(@PathVariable Long eleicaoId, @RequestParam Cargo cargo) {
        return service.apurar(eleicaoId, cargo);
    }
}
