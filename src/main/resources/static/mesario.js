'use strict';

const INTERVALO_STATUS_MS = 2000;
const TAMANHO_PAGINA = 10;

let paginaCaderno = 0;
let ultimoComparecimento; // undefined força a primeira carga do caderno

function atualizarLinkUrna() {
    $('link-urna').href = `/urna${consultaSecao()}`;
}

// ---------------------------------------------------------------- Estado da urna

async function atualizarStatus() {
    let urna = null;
    try {
        urna = (await api('GET', baseUrna())).dados;
        const habilitado = urna.eleitorHabilitado ? 'eleitor votando' : 'nenhum eleitor habilitado';
        $('status').textContent =
            `Urna ${urna.estado} · ${urna.comparecimento} de ${urna.eleitoresAptos} votaram · ${habilitado}`;
    } catch (erro) {
        $('status').textContent = erro.status === 404 ? 'Urna ainda não aberta nesta seção.' : `Erro: ${erro.message}`;
    }
    atualizarBotoes(urna);

    // O caderno só é recarregado quando alguém termina de votar.
    const comparecimento = urna ? urna.comparecimento : null;
    if (comparecimento !== ultimoComparecimento) {
        ultimoComparecimento = comparecimento;
        await carregarCaderno();
    }
}

function atualizarBotoes(urna) {
    const aberta = urna && urna.estado === 'ABERTA';
    $('btn-abrir').disabled = urna !== null;
    $('btn-habilitar').disabled = !aberta || urna.eleitorHabilitado;
    $('btn-proximo').disabled = !aberta;
    $('btn-cancelar').disabled = !aberta || !urna.eleitorHabilitado;
    $('btn-encerrar').disabled = !aberta || urna.eleitorHabilitado;
    $('btn-rdv').disabled = !urna || urna.estado !== 'ENCERRADA';
}

// ---------------------------------------------------------------- Caderno

async function carregarCaderno() {
    const pendentes = $('somente-pendentes').checked;
    try {
        const { dados } = await api('GET',
            `${baseUrna()}/caderno?pendentes=${pendentes}&page=${paginaCaderno}&size=${TAMANHO_PAGINA}`);
        $('caderno').replaceChildren(tabela(['Título', 'Nome', 'Situação'],
            dados.conteudo.map((e) => [e.titulo, e.nome, e.votou ? 'votou' : '—'])));
        const total = Math.max(dados.totalPaginas, 1);
        $('pagina-info').textContent = `Página ${dados.pagina + 1} de ${total} · ${dados.totalElementos} eleitores`;
        $('pagina-anterior').disabled = dados.pagina === 0;
        $('pagina-seguinte').disabled = dados.pagina + 1 >= total;
    } catch (erro) {
        $('caderno').replaceChildren(elemento('p', {}, `Não foi possível carregar o caderno: ${erro.message}`));
        $('pagina-info').textContent = '';
    }
}

// ---------------------------------------------------------------- Ações

async function abrir() {
    const { dados } = await api('POST', `${baseUrna()}/abertura`);
    const linhas = dados.cargos.flatMap((cargo) => [
        ...cargo.candidatos.map((c) => [NOMES_CARGO[cargo.cargo], `${c.numero} ${c.nome}`, c.votos]),
        [NOMES_CARGO[cargo.cargo], 'Brancos', cargo.brancos],
        [NOMES_CARGO[cargo.cargo], 'Nulos', cargo.nulos],
    ]);
    registrar(`Zerésima emitida. Registros no RDV: ${dados.registrosNoRdv}`, 'ok',
        tabela(['Cargo', 'Candidato', 'Votos'], linhas, [2]),
        elemento('pre', {}, `Chave pública da urna (Ed25519):\n${dados.chavePublica}`));
}

async function proximoDaFila() {
    const { dados } = await api('GET', `${baseUrna()}/caderno?pendentes=true&size=1`);
    if (dados.conteudo.length === 0) {
        registrar('Todos os eleitores da seção já votaram.');
        return;
    }
    $('titulo').value = dados.conteudo[0].titulo;
}

async function habilitar() {
    const { dados } = await api('POST', `${baseUrna()}/habilitacao`, { titulo: $('titulo').value.trim() });
    registrar(`Eleitor habilitado: ${dados.nome} (${dados.titulo}). A urna está liberada.`, 'ok');
    $('titulo').value = '';
}

async function cancelar() {
    await api('DELETE', `${baseUrna()}/habilitacao`);
    registrar('Habilitação cancelada. Nenhum voto foi gravado.');
}

async function encerrar() {
    const { dados } = await api('POST', `${baseUrna()}/encerramento`);
    registrar('Votação encerrada. BU gerado a partir do RDV e assinado pela urna.', 'ok',
        elemento('pre', {}, `SHA-256: ${dados.hash}\n\n${JSON.stringify(JSON.parse(dados.conteudo), null, 2)}`),
        elemento('p', {}, 'Próximo passo: ', elemento('a', { href: `/transmissao${consultaSecao()}` }, 'transmitir o BU')));
}

async function verRdv() {
    const { dados } = await api('GET', `${baseUrna()}/rdv`);
    registrar(`RDV: ${dados.length} registros. Posições sorteadas, sem eleitor e sem horário.`, 'info',
        tabela(['Cargo', 'Posição', 'Tipo', 'Número'],
            dados.slice(0, 40).map((r) => [NOMES_CARGO[r.cargo], r.posicao, r.tipo, r.numero]), [1]),
        dados.length > 40 ? elemento('p', {}, `… e mais ${dados.length - 40}.`) : null);
}

function comAtualizacao(funcao) {
    return async () => {
        await funcao();
        await atualizarStatus();
    };
}

// ---------------------------------------------------------------- Eventos

$('btn-abrir').addEventListener('click', () => acao('Abrir urna', comAtualizacao(abrir)));
$('btn-proximo').addEventListener('click', () => acao('Próximo da fila', proximoDaFila));
$('btn-habilitar').addEventListener('click', () => acao('Habilitar', comAtualizacao(habilitar)));
$('btn-cancelar').addEventListener('click', () => acao('Cancelar habilitação', comAtualizacao(cancelar)));
$('btn-encerrar').addEventListener('click', () => acao('Encerrar', comAtualizacao(encerrar)));
$('btn-rdv').addEventListener('click', () => acao('Ver RDV', verRdv));
$('titulo').addEventListener('keydown', (evento) => {
    if (evento.key === 'Enter') {
        acao('Habilitar', comAtualizacao(habilitar));
    }
});

$('somente-pendentes').addEventListener('change', () => {
    paginaCaderno = 0;
    carregarCaderno();
});
$('pagina-anterior').addEventListener('click', () => {
    paginaCaderno = Math.max(0, paginaCaderno - 1);
    carregarCaderno();
});
$('pagina-seguinte').addEventListener('click', () => {
    paginaCaderno += 1;
    carregarCaderno();
});

ligarSeletorSecao(() => {
    paginaCaderno = 0;
    ultimoComparecimento = undefined;
    atualizarLinkUrna();
    atualizarStatus();
});

atualizarLinkUrna();
atualizarStatus();
setInterval(atualizarStatus, INTERVALO_STATUS_MS);
