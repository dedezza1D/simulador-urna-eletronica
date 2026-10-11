package com.estudo.votacao.candidato;

import java.util.Locale;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.estudo.votacao.apuracao.ApuracaoRepository;
import com.estudo.votacao.comum.ConflitoException;
import com.estudo.votacao.comum.PaginaResponse;
import com.estudo.votacao.comum.ParametroInvalidoException;
import com.estudo.votacao.comum.RecursoNaoEncontradoException;
import com.estudo.votacao.comum.RegraNegocioException;
import com.estudo.votacao.eleicao.Cargo;
import com.estudo.votacao.eleicao.Eleicao;
import com.estudo.votacao.eleicao.EleicaoRepository;

@Service
public class CandidatoService {

    private static final Set<String> CAMPOS_ORDENAVEIS = Set.of("cargo", "numero", "nome", "partido");

    private final CandidatoRepository candidatoRepository;
    private final EleicaoRepository eleicaoRepository;
    private final ApuracaoRepository apuracaoRepository;

    public CandidatoService(CandidatoRepository candidatoRepository,
                            EleicaoRepository eleicaoRepository,
                            ApuracaoRepository apuracaoRepository) {
        this.candidatoRepository = candidatoRepository;
        this.eleicaoRepository = eleicaoRepository;
        this.apuracaoRepository = apuracaoRepository;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<CandidatoResponse> listar(Long eleicaoId, Cargo cargo, Pageable pageable) {
        // Lista branca: o cliente não escolhe livremente a coluna que vai para o ORDER BY.
        pageable.getSort().stream()
                .map(Sort.Order::getProperty)
                .filter(campo -> !CAMPOS_ORDENAVEIS.contains(campo))
                .findFirst()
                .ifPresent(campo -> {
                    throw new ParametroInvalidoException(
                            "Ordenação por '" + campo + "' não permitida. Use: " + CAMPOS_ORDENAVEIS + ".");
                });
        return PaginaResponse.de(candidatoRepository.buscar(eleicaoId, cargo, pageable), CandidatoResponse::de);
    }

    @Transactional(readOnly = true)
    public CandidatoResponse buscar(Long id) {
        return CandidatoResponse.de(carregar(id));
    }

    @Transactional
    public CandidatoResponse criar(CandidatoRequest request) {
        Eleicao eleicao = eleicaoRepository.findById(request.eleicaoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Eleição " + request.eleicaoId() + " não encontrada."));
        validarNumero(request.cargo(), request.numero());
        if (candidatoRepository.existsByEleicaoIdAndCargoAndNumero(eleicao.getId(), request.cargo(), request.numero())) {
            throw numeroEmUso(request);
        }

        Candidato candidato = new Candidato(eleicao, request.cargo(), request.numero(),
                request.nome().strip(), normalizarPartido(request.partido()));
        return CandidatoResponse.de(candidatoRepository.save(candidato));
    }

    @Transactional
    public CandidatoResponse atualizar(Long id, CandidatoRequest request) {
        Candidato candidato = carregar(id);
        Long eleicaoId = candidato.getEleicao().getId();
        if (!eleicaoId.equals(request.eleicaoId())) {
            throw new RegraNegocioException("Não é permitido mover o candidato para outra eleição.");
        }
        validarNumero(request.cargo(), request.numero());

        boolean mudaIdentificacao = candidato.getCargo() != request.cargo()
                || !candidato.getNumero().equals(request.numero());
        if (mudaIdentificacao && apuracaoRepository.existsByCandidatoId(id)) {
            throw new ConflitoException("Candidato com votos apurados não pode mudar de cargo ou número.");
        }
        if (candidatoRepository.existsByEleicaoIdAndCargoAndNumeroAndIdNot(eleicaoId, request.cargo(), request.numero(), id)) {
            throw numeroEmUso(request);
        }

        // Sem save explícito: a entidade está gerenciada e o dirty checking gera o UPDATE no commit.
        candidato.atualizar(request.cargo(), request.numero(), request.nome().strip(), normalizarPartido(request.partido()));
        return CandidatoResponse.de(candidato);
    }

    @Transactional
    public void remover(Long id) {
        Candidato candidato = carregar(id);
        if (apuracaoRepository.existsByCandidatoId(id)) {
            throw new ConflitoException("Candidato com votos apurados não pode ser removido.");
        }
        candidatoRepository.delete(candidato);
    }

    private Candidato carregar(Long id) {
        return candidatoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Candidato " + id + " não encontrado."));
    }

    private static void validarNumero(Cargo cargo, String numero) {
        if (!cargo.aceitaNumero(numero)) {
            throw new RegraNegocioException(
                    "Número de candidato a " + cargo + " deve ter " + cargo.digitos() + " dígitos.");
        }
    }

    private static String normalizarPartido(String partido) {
        return partido.strip().toUpperCase(Locale.ROOT);
    }

    private static ConflitoException numeroEmUso(CandidatoRequest request) {
        return new ConflitoException("Número " + request.numero() + " já está em uso para " + request.cargo() + " nesta eleição.");
    }
}
