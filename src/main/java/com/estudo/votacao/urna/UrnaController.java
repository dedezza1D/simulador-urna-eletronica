package com.estudo.votacao.urna;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.estudo.votacao.comum.PaginaResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/eleicoes/{eleicaoId}/zonas/{zona}/secoes/{secao}/urna")
public class UrnaController {

    private final UrnaService service;

    public UrnaController(UrnaService service) {
        this.service = service;
    }

    @PostMapping("/abertura")
    @ResponseStatus(HttpStatus.CREATED)
    public ZeresimaResponse abrir(@PathVariable Long eleicaoId, @PathVariable int zona, @PathVariable int secao) {
        return service.abrir(eleicaoId, zona, secao);
    }

    @GetMapping
    public UrnaResponse consultar(@PathVariable Long eleicaoId, @PathVariable int zona, @PathVariable int secao) {
        return service.consultar(eleicaoId, zona, secao);
    }

    @GetMapping("/cedula")
    public CedulaResponse cedula(@PathVariable Long eleicaoId) {
        return service.cedula(eleicaoId);
    }

    @GetMapping("/caderno")
    public PaginaResponse<EleitorCadernoResponse> caderno(
            @PathVariable Long eleicaoId, @PathVariable int zona, @PathVariable int secao,
            @RequestParam(defaultValue = "false") boolean pendentes,
            @PageableDefault(size = 20, sort = "titulo", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.caderno(eleicaoId, zona, secao, pendentes, pageable);
    }

    @PostMapping("/habilitacao")
    public HabilitacaoResponse habilitar(@PathVariable Long eleicaoId, @PathVariable int zona, @PathVariable int secao,
                                         @Valid @RequestBody HabilitarRequest request) {
        return service.habilitar(eleicaoId, zona, secao, request.titulo());
    }

    @DeleteMapping("/habilitacao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelarHabilitacao(@PathVariable Long eleicaoId, @PathVariable int zona, @PathVariable int secao) {
        service.cancelarHabilitacao(eleicaoId, zona, secao);
    }

    /** Não devolve nada sobre o voto: a urna não emite comprovante. */
    @PostMapping("/votos")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void votar(@PathVariable Long eleicaoId, @PathVariable int zona, @PathVariable int secao,
                      @Valid @RequestBody VotarRequest request) {
        service.votar(eleicaoId, zona, secao, request);
    }

    @PostMapping("/encerramento")
    public BoletimAssinadoResponse encerrar(@PathVariable Long eleicaoId, @PathVariable int zona, @PathVariable int secao) {
        return service.encerrar(eleicaoId, zona, secao);
    }

    @GetMapping("/boletim")
    public BoletimAssinadoResponse boletim(@PathVariable Long eleicaoId, @PathVariable int zona, @PathVariable int secao) {
        return service.boletim(eleicaoId, zona, secao);
    }

    @GetMapping("/rdv")
    public List<RdvRepository.Registro> rdv(@PathVariable Long eleicaoId, @PathVariable int zona, @PathVariable int secao) {
        return service.rdv(eleicaoId, zona, secao);
    }
}
