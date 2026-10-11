package com.estudo.votacao.urna;

import java.security.KeyPair;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.estudo.votacao.boletim.BoletimRepository;
import com.estudo.votacao.boletim.RegistrarBoletimRequest;
import com.estudo.votacao.boletim.RegistrarBoletimRequest.VotacaoCargo;
import com.estudo.votacao.boletim.RegistrarBoletimRequest.Voto;
import com.estudo.votacao.candidato.Candidato;
import com.estudo.votacao.candidato.CandidatoRepository;
import com.estudo.votacao.comum.ConflitoException;
import com.estudo.votacao.comum.PaginaResponse;
import com.estudo.votacao.comum.RecursoNaoEncontradoException;
import com.estudo.votacao.comum.RegraNegocioException;
import com.estudo.votacao.eleicao.Cargo;
import com.estudo.votacao.eleicao.Eleicao;
import com.estudo.votacao.eleicao.EleicaoRepository;
import com.estudo.votacao.secao.Secao;
import com.estudo.votacao.secao.SecaoRepository;
import com.estudo.votacao.urna.RdvRepository.Contagem;
import com.estudo.votacao.urna.VotarRequest.Escolha;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Ciclo de vida da urna de uma seção: abertura (zerésima), habilitação do eleitor pelo mesário,
 * votação, encerramento (BU gerado a partir do RDV e assinado) e consultas de auditoria.
 */
@Service
public class UrnaService {

    private record VotoApurado(Cargo cargo, TipoVoto tipo, String numero) {
    }

    private final UrnaRepository urnaRepository;
    private final EleicaoRepository eleicaoRepository;
    private final SecaoRepository secaoRepository;
    private final EleitorRepository eleitorRepository;
    private final ComparecimentoRepository comparecimentoRepository;
    private final CandidatoRepository candidatoRepository;
    private final BoletimRepository boletimRepository;
    private final RdvRepository rdv;
    private final CofreDeChaves cofre;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public UrnaService(UrnaRepository urnaRepository, EleicaoRepository eleicaoRepository,
                       SecaoRepository secaoRepository, EleitorRepository eleitorRepository,
                       ComparecimentoRepository comparecimentoRepository, CandidatoRepository candidatoRepository,
                       BoletimRepository boletimRepository, RdvRepository rdv, CofreDeChaves cofre,
                       ObjectMapper objectMapper, Clock clock) {
        this.urnaRepository = urnaRepository;
        this.eleicaoRepository = eleicaoRepository;
        this.secaoRepository = secaoRepository;
        this.eleitorRepository = eleitorRepository;
        this.comparecimentoRepository = comparecimentoRepository;
        this.candidatoRepository = candidatoRepository;
        this.boletimRepository = boletimRepository;
        this.rdv = rdv;
        this.cofre = cofre;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public ZeresimaResponse abrir(Long eleicaoId, int zona, int numeroSecao) {
        Eleicao eleicao = eleicaoRepository.findById(eleicaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Eleição " + eleicaoId + " não encontrada."));
        Secao secao = secaoRepository.findByZonaAndNumero(zona, numeroSecao)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Seção " + numeroSecao + " da zona " + zona + " não encontrada."));
        if (urnaRepository.existsByEleicaoIdAndSecaoId(eleicaoId, secao.getId())) {
            throw new ConflitoException("A urna desta seção já foi aberta.");
        }
        if (boletimRepository.existsByEleicaoIdAndSecaoId(eleicaoId, secao.getId())) {
            throw new ConflitoException("Esta seção já tem boletim de urna recebido.");
        }

        KeyPair chaves = AssinaturaDigital.gerarPar();
        Urna urna = urnaRepository.save(
                new Urna(eleicao, secao, AssinaturaDigital.codificar(chaves.getPublic()), clock.instant()));
        cofre.guardar(urna.getId(), chaves.getPrivate());

        return zeresima(urna);
    }

    @Transactional(readOnly = true)
    public UrnaResponse consultar(Long eleicaoId, int zona, int secao) {
        Urna urna = carregar(eleicaoId, zona, secao);
        return new UrnaResponse(eleicaoId, zona, secao, urna.getEstado(), urna.getEleitorHabilitado() != null,
                comparecimentoRepository.countByUrnaId(urna.getId()), urna.getSecao().getEleitoresAptos(),
                urna.getAbertaEm(), urna.getEncerradaEm());
    }

    @Transactional(readOnly = true)
    public CedulaResponse cedula(Long eleicaoId) {
        Map<Cargo, List<Candidato>> porCargo = candidatosPorCargo(eleicaoId);
        List<CedulaResponse.CargoNaUrna> cargos = porCargo.entrySet().stream()
                .map(entrada -> new CedulaResponse.CargoNaUrna(entrada.getKey(), entrada.getKey().digitos(),
                        entrada.getValue().stream()
                                .map(c -> new CedulaResponse.CandidatoNaUrna(c.getNumero(), c.getNome(), c.getPartido()))
                                .toList()))
                .toList();
        return new CedulaResponse(cargos);
    }

    @Transactional(readOnly = true)
    public PaginaResponse<EleitorCadernoResponse> caderno(Long eleicaoId, int zona, int numeroSecao,
                                                         boolean somentePendentes, Pageable pageable) {
        Secao secao = secaoRepository.findByZonaAndNumero(zona, numeroSecao)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Seção " + numeroSecao + " da zona " + zona + " não encontrada."));
        Long urnaId = urnaRepository.findByEleicaoIdAndSecaoZonaAndSecaoNumero(eleicaoId, zona, numeroSecao)
                .map(Urna::getId)
                .orElse(-1L);
        return PaginaResponse.de(eleitorRepository.caderno(secao.getId(), urnaId, somentePendentes, pageable), e -> e);
    }

    @Transactional
    public HabilitacaoResponse habilitar(Long eleicaoId, int zona, int secao, String titulo) {
        Urna urna = carregar(eleicaoId, zona, secao);
        urna.exigirAberta();
        Eleitor eleitor = eleitorRepository.findByTitulo(titulo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Título " + titulo + " não encontrado."));
        Secao secaoDoEleitor = eleitor.getSecao();
        if (!secaoDoEleitor.getId().equals(urna.getSecao().getId())) {
            throw new RegraNegocioException("Eleitor não pertence a esta seção. Vota na zona "
                    + secaoDoEleitor.getZona() + ", seção " + secaoDoEleitor.getNumero() + ".");
        }
        if (comparecimentoRepository.existsByUrnaIdAndEleitorId(urna.getId(), eleitor.getId())) {
            throw new ConflitoException("Eleitor já votou.");
        }
        urna.habilitar(eleitor);
        return new HabilitacaoResponse(eleitor.getTitulo(), eleitor.getNome());
    }

    @Transactional
    public void cancelarHabilitacao(Long eleicaoId, int zona, int secao) {
        carregar(eleicaoId, zona, secao).cancelarHabilitacao();
    }

    /**
     * Grava o voto completo de uma vez: ou todos os cargos entram no RDV, ou nenhum.
     * Tudo é validado antes da primeira escrita.
     */
    @Transactional
    public void votar(Long eleicaoId, int zona, int secao, VotarRequest request) {
        Urna urna = carregar(eleicaoId, zona, secao);
        urna.exigirAberta();
        Eleitor eleitor = urna.getEleitorHabilitado();
        if (eleitor == null) {
            throw new RegraNegocioException("Nenhum eleitor habilitado. O mesário precisa liberar a urna.");
        }

        Map<Cargo, List<Candidato>> candidatos = candidatosPorCargo(eleicaoId);
        Map<Cargo, Escolha> escolhas = new EnumMap<>(Cargo.class);
        for (Escolha escolha : request.escolhas()) {
            if (escolhas.put(escolha.cargo(), escolha) != null) {
                throw new RegraNegocioException("Cargo " + escolha.cargo() + " informado mais de uma vez.");
            }
        }
        if (!escolhas.keySet().equals(candidatos.keySet())) {
            throw new RegraNegocioException("O voto deve conter exatamente os cargos " + candidatos.keySet() + ".");
        }

        List<VotoApurado> votos = new ArrayList<>();
        for (Cargo cargo : candidatos.keySet()) {
            Set<String> numeros = candidatos.get(cargo).stream().map(Candidato::getNumero).collect(Collectors.toSet());
            votos.add(apurar(cargo, escolhas.get(cargo), numeros));
        }

        int capacidade = urna.getSecao().getEleitoresAptos();
        votos.forEach(voto -> rdv.registrar(urna.getId(), capacidade, voto.cargo(), voto.tipo(), voto.numero()));
        comparecimentoRepository.save(new Comparecimento(urna, eleitor));
        urna.concluirVoto();
    }

    @Transactional
    public BoletimAssinadoResponse encerrar(Long eleicaoId, int zona, int secao) {
        Urna urna = carregar(eleicaoId, zona, secao);
        urna.exigirPodeEncerrar();

        RegistrarBoletimRequest boletim = montarBoletim(urna, eleicaoId, zona, secao);
        String conteudo = serializar(boletim);
        String assinatura = cofre.assinar(urna.getId(), conteudo);
        urna.encerrar(clock.instant(), conteudo, assinatura);

        return new BoletimAssinadoResponse(eleicaoId, zona, secao, conteudo, assinatura, AssinaturaDigital.sha256(conteudo));
    }

    @Transactional(readOnly = true)
    public BoletimAssinadoResponse boletim(Long eleicaoId, int zona, int secao) {
        Urna urna = carregar(eleicaoId, zona, secao);
        urna.exigirEncerrada();
        return new BoletimAssinadoResponse(eleicaoId, zona, secao, urna.getBuConteudo(), urna.getBuAssinatura(),
                AssinaturaDigital.sha256(urna.getBuConteudo()));
    }

    /** O RDV só é divulgado depois do encerramento, como no processo real. */
    @Transactional(readOnly = true)
    public List<RdvRepository.Registro> rdv(Long eleicaoId, int zona, int secao) {
        Urna urna = carregar(eleicaoId, zona, secao);
        urna.exigirEncerrada();
        return rdv.listar(urna.getId());
    }

    private static VotoApurado apurar(Cargo cargo, Escolha escolha, Set<String> numerosValidos) {
        if (escolha.branco()) {
            if (escolha.numero() != null) {
                throw new RegraNegocioException("Voto em branco para " + cargo + " não pode ter número.");
            }
            return new VotoApurado(cargo, TipoVoto.BRANCO, null);
        }
        if (escolha.numero() == null) {
            throw new RegraNegocioException("Informe um número ou voto em branco para " + cargo + ".");
        }
        if (!cargo.aceitaNumero(escolha.numero())) {
            throw new RegraNegocioException("Número para " + cargo + " deve ter " + cargo.digitos() + " dígitos.");
        }
        // Como na urna: número completo que não corresponde a candidato é voto nulo.
        return numerosValidos.contains(escolha.numero())
                ? new VotoApurado(cargo, TipoVoto.NOMINAL, escolha.numero())
                : new VotoApurado(cargo, TipoVoto.NULO, null);
    }

    private RegistrarBoletimRequest montarBoletim(Urna urna, Long eleicaoId, int zona, int secao) {
        List<Contagem> contagens = rdv.contar(urna.getId());
        List<VotacaoCargo> cargos = new ArrayList<>();
        for (Cargo cargo : candidatosPorCargo(eleicaoId).keySet()) {
            List<Contagem> doCargo = contagens.stream().filter(c -> c.cargo() == cargo).toList();
            List<Voto> nominais = doCargo.stream()
                    .filter(c -> c.tipo() == TipoVoto.NOMINAL)
                    .sorted(Comparator.comparing(Contagem::numero))
                    .map(c -> new Voto(c.numero(), Math.toIntExact(c.quantidade())))
                    .toList();
            cargos.add(new VotacaoCargo(cargo, total(doCargo, TipoVoto.BRANCO), total(doCargo, TipoVoto.NULO), nominais));
        }
        int comparecimento = Math.toIntExact(comparecimentoRepository.countByUrnaId(urna.getId()));
        return new RegistrarBoletimRequest(eleicaoId, zona, secao, comparecimento, cargos);
    }

    private static int total(List<Contagem> contagens, TipoVoto tipo) {
        return Math.toIntExact(contagens.stream().filter(c -> c.tipo() == tipo).mapToLong(Contagem::quantidade).sum());
    }

    private ZeresimaResponse zeresima(Urna urna) {
        List<Contagem> contagens = rdv.contar(urna.getId());
        List<ZeresimaResponse.CargoZerado> cargos = candidatosPorCargo(urna.getEleicao().getId()).entrySet().stream()
                .map(entrada -> {
                    Cargo cargo = entrada.getKey();
                    List<Contagem> doCargo = contagens.stream().filter(c -> c.cargo() == cargo).toList();
                    List<ZeresimaResponse.CandidatoZerado> candidatos = entrada.getValue().stream()
                            .map(c -> new ZeresimaResponse.CandidatoZerado(c.getNumero(), c.getNome(),
                                    doCargo.stream()
                                            .filter(v -> v.tipo() == TipoVoto.NOMINAL && c.getNumero().equals(v.numero()))
                                            .mapToLong(Contagem::quantidade).sum()))
                            .toList();
                    return new ZeresimaResponse.CargoZerado(cargo, total(doCargo, TipoVoto.BRANCO),
                            total(doCargo, TipoVoto.NULO), candidatos);
                })
                .toList();
        long registros = contagens.stream().mapToLong(Contagem::quantidade).sum();
        Secao secao = urna.getSecao();
        return new ZeresimaResponse(urna.getEleicao().getId(), secao.getZona(), secao.getNumero(), clock.instant(),
                registros, urna.getChavePublica(), cargos);
    }

    /** Cargos com candidatos nesta eleição, na ordem em que a urna os apresenta. */
    private Map<Cargo, List<Candidato>> candidatosPorCargo(Long eleicaoId) {
        Map<Cargo, List<Candidato>> porCargo = new TreeMap<>(Comparator.comparingInt(Cargo::ordemNaUrna));
        for (Candidato candidato : candidatoRepository.findByEleicaoId(eleicaoId)) {
            porCargo.computeIfAbsent(candidato.getCargo(), c -> new ArrayList<>()).add(candidato);
        }
        porCargo.values().forEach(lista -> lista.sort(Comparator.comparing(Candidato::getNumero)));
        return porCargo;
    }

    private Urna carregar(Long eleicaoId, int zona, int secao) {
        return urnaRepository.findByEleicaoIdAndSecaoZonaAndSecaoNumero(eleicaoId, zona, secao)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Urna da zona " + zona + ", seção " + secao + " ainda não foi aberta."));
    }

    private String serializar(RegistrarBoletimRequest boletim) {
        try {
            return objectMapper.writeValueAsString(boletim);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar o boletim", e);
        }
    }
}
