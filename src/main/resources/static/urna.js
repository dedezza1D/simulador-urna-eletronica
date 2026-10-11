'use strict';

// Terminal do eleitor. Não tem controle nenhum de mesário: consulta a urna no servidor e
// libera o voto quando o mesário habilita alguém, como o cabo entre os dois terminais reais.

const INTERVALO_SINCRONIA_MS = 1000;

const estado = {
    cedula: null,      // cargos na ordem da urna, com candidatos
    votacao: null,     // { indice, digitos, branco, escolhas }
    fim: false,
    enviando: false,
    aviso: 'CONECTANDO…',
    avisoAte: 0,       // mantém um aviso temporário na tela até este instante
    ultimoHtml: '',
};

const secao = secaoAtual();
$('identificacao').textContent = `Eleição ${secao.eleicao} · Zona ${secao.zona} · Seção ${secao.secao}`;

// ---------------------------------------------------------------- Sincronia com o mesário

async function sincronizar() {
    if (estado.enviando || estado.fim) {
        return;
    }
    try {
        const { dados } = await api('GET', baseUrna());
        if (dados.estado === 'ENCERRADA') {
            estado.votacao = null;
            definirAviso('VOTAÇÃO ENCERRADA');
        } else if (dados.eleitorHabilitado) {
            if (!estado.votacao) {
                if (!estado.cedula) {
                    estado.cedula = (await api('GET', `${baseUrna()}/cedula`)).dados.cargos;
                }
                estado.votacao = { indice: 0, digitos: '', branco: false, escolhas: [] };
            }
        } else if (estado.votacao) {
            estado.votacao = null;
            definirAviso('HABILITAÇÃO CANCELADA PELO MESÁRIO', 3000);
        } else {
            definirAviso('AGUARDANDO LIBERAÇÃO PELO MESÁRIO');
        }
    } catch (erro) {
        estado.votacao = null;
        definirAviso(erro.status === 404 ? 'URNA FECHADA' : 'SEM COMUNICAÇÃO COM O SERVIDOR');
    }
    renderizarTela();
}

function definirAviso(texto, duracaoMs = 0) {
    if (Date.now() < estado.avisoAte && duracaoMs === 0) {
        return;
    }
    estado.aviso = texto;
    estado.avisoAte = duracaoMs ? Date.now() + duracaoMs : 0;
}

// ---------------------------------------------------------------- Som

let audio;

function som(tipo) {
    try {
        audio = audio || new (window.AudioContext || window.webkitAudioContext)();
        const sequencias = {
            tecla: [[1400, 0.04]],
            confirma: [[1000, 0.16]],
            fim: [[880, 0.11], [0, 0.04], [880, 0.11], [0, 0.04], [880, 0.11], [0, 0.04], [880, 0.11], [0, 0.04], [880, 0.4]],
        };
        let instante = audio.currentTime;
        for (const [frequencia, duracao] of sequencias[tipo]) {
            if (frequencia) {
                const oscilador = audio.createOscillator();
                const ganho = audio.createGain();
                oscilador.type = 'square';
                oscilador.frequency.value = frequencia;
                ganho.gain.value = 0.04;
                oscilador.connect(ganho).connect(audio.destination);
                oscilador.start(instante);
                oscilador.stop(instante + duracao);
            }
            instante += duracao;
        }
    } catch {
        // Sem áudio disponível: a urna continua funcionando em silêncio.
    }
}

// ---------------------------------------------------------------- Teclas

function cargoAtual() {
    return estado.cedula[estado.votacao.indice];
}

function tecla(valor) {
    if (!estado.votacao || estado.enviando) {
        return;
    }
    const voto = estado.votacao;
    const cargo = cargoAtual();
    if (/^\d$/.test(valor)) {
        if (!voto.branco && voto.digitos.length < cargo.digitos) {
            voto.digitos += valor;
            som('tecla');
        }
    } else if (valor === 'CORRIGE') {
        voto.digitos = '';
        voto.branco = false;
        som('tecla');
    } else if (valor === 'BRANCO') {
        // Como na urna: BRANCO só vale com o campo vazio; com número digitado, é preciso CORRIGE antes.
        if (voto.digitos === '') {
            voto.branco = true;
            som('tecla');
        }
    } else if (valor === 'CONFIRMA') {
        confirmar();
        return;
    }
    renderizarTela();
}

async function confirmar() {
    const voto = estado.votacao;
    const cargo = cargoAtual();
    if (!voto.branco && voto.digitos.length < cargo.digitos) {
        return;
    }
    voto.escolhas.push(voto.branco
        ? { cargo: cargo.cargo, branco: true }
        : { cargo: cargo.cargo, numero: voto.digitos, branco: false });
    som('confirma');

    if (voto.indice + 1 < estado.cedula.length) {
        voto.indice += 1;
        voto.digitos = '';
        voto.branco = false;
        renderizarTela();
        return;
    }

    // Último cargo: o voto inteiro vai de uma vez, ou não vai.
    estado.enviando = true;
    try {
        await api('POST', `${baseUrna()}/votos`, { escolhas: voto.escolhas });
        estado.votacao = null;
        estado.fim = true;
        som('fim');
        renderizarTela();
        setTimeout(() => {
            estado.fim = false;
            sincronizar();
        }, 2500);
    } catch (erro) {
        estado.votacao = null;
        definirAviso(`VOTO NÃO REGISTRADO: ${erro.message}`, 5000);
        renderizarTela();
    } finally {
        estado.enviando = false;
    }
}

// ---------------------------------------------------------------- Tela

const SILHUETA = '<svg viewBox="0 0 90 110" aria-hidden="true"><circle cx="45" cy="34" r="22"/><path d="M5 110c0-26 18-44 40-44s40 18 40 44z"/></svg>';

function escapar(texto) {
    return String(texto).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]);
}

function rodape(passo) {
    return `
        <div class="rodape">
            <p>Aperte a tecla:</p>
            <p>&nbsp;&nbsp;<b>CONFIRMA</b> para CONFIRMAR este voto</p>
            <p>&nbsp;&nbsp;<b>CORRIGE</b> para REINICIAR este voto</p>
            <p class="passo">${passo}</p>
        </div>`;
}

function htmlDaTela() {
    if (estado.fim) {
        return '<div class="centro"><div class="fim">FIM</div></div>';
    }
    if (!estado.votacao) {
        return `<div class="centro"><div class="aviso">${escapar(estado.aviso)}</div></div>`;
    }

    const voto = estado.votacao;
    const cargo = cargoAtual();
    const nomeCargo = escapar(NOMES_CARGO[cargo.cargo] || cargo.cargo);
    const passo = `${voto.indice + 1} de ${estado.cedula.length}`;

    if (voto.branco) {
        return `
            <div class="voto">
                <div class="corpo">
                    <div><div class="topo">SEU VOTO PARA</div><div class="cargo">${nomeCargo}</div></div>
                    <div class="em-branco">VOTO EM BRANCO</div>
                </div>
                ${rodape(passo)}
            </div>`;
    }

    const caixas = Array.from({ length: cargo.digitos }, (_, i) => {
        const ativa = i === voto.digitos.length ? ' ativa' : '';
        return `<span class="caixa${ativa}">${escapar(voto.digitos[i] || '')}</span>`;
    }).join('');

    const completo = voto.digitos.length === cargo.digitos;
    const candidato = completo ? cargo.candidatos.find((c) => c.numero === voto.digitos) : null;

    let dados = '';
    let foto = '';
    if (candidato) {
        dados = `<div class="dados"><div>Nome: <b>${escapar(candidato.nome)}</b></div><div>Partido: <b>${escapar(candidato.partido)}</b></div></div>`;
        foto = `<div class="foto">${SILHUETA}</div>`;
    } else if (completo) {
        dados = '<div class="grande">NÚMERO ERRADO</div><div class="medio">VOTO NULO</div>';
    }

    return `
        <div class="voto">
            <div class="corpo">
                <div>
                    <div class="topo">SEU VOTO PARA</div>
                    <div class="cargo">${nomeCargo}</div>
                    <div class="numero">Número: <span class="caixas">${caixas}</span></div>
                    ${dados}
                </div>
                ${foto}
            </div>
            ${completo ? rodape(passo) : `<div class="rodape"><p class="passo">${passo}</p></div>`}
        </div>`;
}

// Só troca o HTML quando muda, para a sincronia periódica não reiniciar a animação do cursor.
function renderizarTela() {
    const html = htmlDaTela();
    if (html !== estado.ultimoHtml) {
        $('tela').innerHTML = html;
        estado.ultimoHtml = html;
    }
}

// ---------------------------------------------------------------- Eventos

document.querySelectorAll('[data-tecla]').forEach((botao) =>
    botao.addEventListener('click', () => tecla(botao.dataset.tecla)));

document.addEventListener('keydown', (evento) => {
    if (evento.target.closest('input, select, textarea')) {
        return;
    }
    if (/^\d$/.test(evento.key)) {
        tecla(evento.key);
    } else if (evento.key === 'Enter') {
        tecla('CONFIRMA');
    } else if (evento.key === 'Backspace') {
        tecla('CORRIGE');
    } else if (evento.key.toLowerCase() === 'b') {
        tecla('BRANCO');
    } else {
        return;
    }
    evento.preventDefault();
});

renderizarTela();
sincronizar();
setInterval(sincronizar, INTERVALO_SINCRONIA_MS);
