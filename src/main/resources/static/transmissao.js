'use strict';

let original = null;

async function sha256(texto) {
    const resumo = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(texto));
    return Array.from(new Uint8Array(resumo), (b) => b.toString(16).padStart(2, '0')).join('');
}

// Mostra se o conteúdo atual ainda é o que a urna assinou.
async function compararComOriginal() {
    if (!original) {
        $('comparacao').textContent = 'Nenhum BU carregado.';
        return;
    }
    const atual = $('conteudo').value;
    if (atual === original.conteudo && $('assinatura').value === original.assinatura) {
        $('comparacao').textContent = `Conteúdo idêntico ao assinado pela urna. SHA-256 ${original.hash.slice(0, 16)}…`;
        return;
    }
    const hash = crypto.subtle ? await sha256(atual) : '(indisponível)';
    $('comparacao').textContent =
        `ALTERADO em relação ao original. SHA-256 atual ${hash.slice(0, 16)}… (original ${original.hash.slice(0, 16)}…)`;
}

async function carregar() {
    const { dados } = await api('GET', `${baseUrna()}/boletim`);
    original = dados;
    restaurar();
    registrar(`BU da zona ${dados.zona}, seção ${dados.secao} lido da urna.`, 'info');
}

function restaurar() {
    if (!original) {
        return;
    }
    $('conteudo').value = original.conteudo;
    $('assinatura').value = original.assinatura;
    compararComOriginal();
}

function adulterar() {
    const bu = JSON.parse($('conteudo').value);
    const cargo = bu.cargos.find((c) => c.votos.length > 0) || bu.cargos[0];
    if (cargo.votos.length > 0) {
        cargo.votos[0].quantidade += 1;
    } else {
        cargo.brancos += 1;
    }
    $('conteudo').value = JSON.stringify(bu);
    compararComOriginal();
}

async function transmitir() {
    const secao = secaoAtual();
    const envelope = {
        eleicaoId: Number(secao.eleicao),
        zona: Number(secao.zona),
        secao: Number(secao.secao),
        conteudo: $('conteudo').value,
        assinatura: $('assinatura').value,
    };
    const { status, dados } = await api('POST', '/api/boletins/assinados', envelope);
    if (status === 201) {
        registrar(`Aceito (HTTP 201): assinatura válida, BU ${dados.id} totalizado.`, 'ok',
            elemento('p', {}, elemento('a', { href: `/resultados${consultaSecao()}` }, 'Ver resultados')));
    } else {
        registrar('Aceito (HTTP 200): BU idêntico já recebido antes. Retransmissão sem duplicar votos.', 'ok');
    }
}

$('btn-carregar').addEventListener('click', () => acao('Ler BU', carregar));
$('btn-transmitir').addEventListener('click', () => acao('Transmitir', transmitir));
$('btn-adulterar').addEventListener('click', () => acao('Adulterar', async () => adulterar()));
$('btn-restaurar').addEventListener('click', restaurar);
$('conteudo').addEventListener('input', compararComOriginal);
$('assinatura').addEventListener('input', compararComOriginal);

ligarSeletorSecao(() => {
    original = null;
    $('conteudo').value = '';
    $('assinatura').value = '';
    compararComOriginal();
});

// Tenta ler o BU ao abrir a página; se a urna ainda não foi encerrada, só avisa.
carregar().catch((erro) => {
    $('comparacao').textContent = `BU indisponível: ${erro.message}`;
});
