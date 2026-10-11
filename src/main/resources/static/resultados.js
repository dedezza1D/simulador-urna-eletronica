'use strict';

const CARGOS = ['PRESIDENTE', 'GOVERNADOR', 'SENADOR'];
const INTERVALO_MS = 5000;
const SITUACOES = { ELEITO: 'Eleito', SEGUNDO_TURNO: '2º turno', NAO_ELEITO: 'Não eleito', INDEFINIDA: '—' };

function formatar(numero) {
    return Number(numero).toLocaleString('pt-BR');
}

function percentual(valor) {
    return `${Number(valor).toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}%`;
}

function cartaoDoCargo(resposta) {
    const r = resposta.resultado;
    const linhas = r.candidatos.map((c) => {
        const barra = elemento('div', { className: 'barra' }, elemento('span'));
        barra.firstChild.style.width = `${c.percentualValidos}%`;
        return [
            `${c.numero} ${c.nome} (${c.partido})`,
            formatar(c.votos),
            percentual(c.percentualValidos),
            barra,
            elemento('span', { className: `selo ${c.situacao}` }, SITUACOES[c.situacao] || c.situacao),
        ];
    });
    return elemento('section', { className: 'painel' },
        elemento('h2', {}, NOMES_CARGO[r.cargo]),
        tabela(['Candidato', 'Votos', '%', '', 'Situação'], linhas, [1, 2]),
        elemento('p', { style: 'margin:8px 0 0;font-size:.85rem;color:var(--suave)' },
            `Válidos ${formatar(r.votosValidos)} · Brancos ${formatar(r.brancos)} · Nulos ${formatar(r.nulos)}`));
}

async function atualizar() {
    const eleicao = secaoAtual().eleicao;
    try {
        const respostas = await Promise.all(CARGOS.map((cargo) =>
            api('GET', `/api/eleicoes/${encodeURIComponent(eleicao)}/resultado?cargo=${cargo}`).then((r) => r.dados)));
        const geral = respostas[0];
        $('barra-secoes').style.width = `${geral.percentualSecoesTotalizadas}%`;
        $('secoes').textContent = `${geral.secoesTotalizadas} de ${geral.secoesTotal} seções totalizadas `
            + `(${percentual(geral.percentualSecoesTotalizadas)})${geral.parcial ? ' · resultado parcial' : ' · apuração concluída'}`;
        $('cargos').replaceChildren(...respostas.map(cartaoDoCargo));
        $('atualizado').textContent = `Atualizado às ${new Date().toLocaleTimeString('pt-BR')}`;
        $('log').replaceChildren();
    } catch (erro) {
        $('log').replaceChildren();
        registrarErro('Resultado', erro);
    }
}

let temporizador = null;

function programar() {
    clearInterval(temporizador);
    temporizador = $('automatico').checked ? setInterval(atualizar, INTERVALO_MS) : null;
}

$('automatico').addEventListener('change', programar);
$('btn-atualizar').addEventListener('click', atualizar);

atualizar();
programar();
