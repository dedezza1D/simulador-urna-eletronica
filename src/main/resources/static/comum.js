'use strict';

// Funções compartilhadas pelas páginas: navegação, seção atual, chamadas à API e registro.

const $ = (id) => document.getElementById(id);

const NOMES_CARGO = { SENADOR: 'Senador', GOVERNADOR: 'Governador', PRESIDENTE: 'Presidente' };

const ROTAS = [
    ['/', 'Início'],
    ['/mesario', 'Mesário'],
    ['/urna', 'Urna'],
    ['/transmissao', 'Transmissão'],
    ['/resultados', 'Resultados'],
    ['/candidatos', 'Candidatos'],
];

// ---------------------------------------------------------------- Seção atual (na query string)

function secaoAtual() {
    const parametros = new URLSearchParams(location.search);
    return {
        eleicao: parametros.get('eleicao') || '1',
        zona: parametros.get('zona') || '3',
        secao: parametros.get('secao') || '2',
    };
}

function consultaSecao(secao = secaoAtual()) {
    return `?${new URLSearchParams(secao)}`;
}

function baseUrna(secao = secaoAtual()) {
    return `/api/eleicoes/${encodeURIComponent(secao.eleicao)}/zonas/${encodeURIComponent(secao.zona)}`
        + `/secoes/${encodeURIComponent(secao.secao)}/urna`;
}

/** Liga os campos #eleicao, #zona e #secao à query string; chama aoMudar quando o usuário troca. */
function ligarSeletorSecao(aoMudar) {
    const secao = secaoAtual();
    ['eleicao', 'zona', 'secao'].forEach((campo) => {
        $(campo).value = secao[campo];
        $(campo).addEventListener('change', () => {
            const nova = { eleicao: $('eleicao').value, zona: $('zona').value, secao: $('secao').value };
            history.replaceState(null, '', consultaSecao(nova));
            montarNavegacao();
            aoMudar();
        });
    });
}

// ---------------------------------------------------------------- Navegação

function montarNavegacao() {
    const nav = $('nav');
    if (!nav) {
        return;
    }
    const consulta = consultaSecao();
    nav.replaceChildren(...ROTAS.map(([caminho, nome]) => {
        const link = elemento('a', { href: caminho + consulta }, nome);
        if (location.pathname === caminho || location.pathname === `${caminho}.html`
                || (caminho === '/' && location.pathname === '/index.html')) {
            link.setAttribute('aria-current', 'page');
        }
        return link;
    }));
}

// ---------------------------------------------------------------- HTTP

async function api(metodo, url, corpo) {
    const resposta = await fetch(url, {
        method: metodo,
        headers: corpo ? { 'Content-Type': 'application/json' } : {},
        body: corpo ? JSON.stringify(corpo) : undefined,
    });
    const texto = await resposta.text();
    const dados = texto ? JSON.parse(texto) : null;
    if (!resposta.ok) {
        const erro = new Error((dados && (dados.detail || dados.title)) || resposta.statusText);
        erro.status = resposta.status;
        erro.dados = dados;
        throw erro;
    }
    return { status: resposta.status, dados };
}

// ---------------------------------------------------------------- DOM e registro

function elemento(tag, props = {}, ...filhos) {
    const el = document.createElement(tag);
    Object.assign(el, props);
    filhos.flat().forEach((f) => {
        if (f !== null && f !== undefined && f !== '') {
            el.append(f instanceof Node ? f : document.createTextNode(String(f)));
        }
    });
    return el;
}

function registrar(titulo, tipo = 'info', ...conteudo) {
    const quando = new Date().toLocaleTimeString('pt-BR');
    $('log').prepend(elemento('div', { className: `entrada ${tipo}` },
        elemento('span', { className: 'quando' }, quando),
        elemento('strong', {}, titulo),
        ...conteudo));
}

function registrarErro(acao, erro) {
    const status = erro.status ? ` (HTTP ${erro.status})` : '';
    const detalhes = erro.dados && erro.dados.erros
        ? elemento('pre', {}, erro.dados.erros.map((e) => `${e.campo}: ${e.mensagem}`).join('\n'))
        : null;
    registrar(`${acao}: ${erro.message}${status}`, 'erro', detalhes);
}

function tabela(cabecalho, linhas, numericas = []) {
    return elemento('table', {},
        elemento('thead', {}, elemento('tr', {},
            cabecalho.map((c, i) => elemento('th', { className: numericas.includes(i) ? 'num' : '' }, c)))),
        elemento('tbody', {}, linhas.map((linha) => elemento('tr', {},
            linha.map((valor, i) => {
                const celula = elemento('td', { className: numericas.includes(i) ? 'num' : '' });
                celula.append(valor instanceof Node ? valor : document.createTextNode(valor ?? '—'));
                return celula;
            })))));
}

/** Executa uma ação de botão registrando erro, sem derrubar a página. */
async function acao(nome, funcao) {
    try {
        await funcao();
    } catch (erro) {
        registrarErro(nome, erro);
    }
}

montarNavegacao();
