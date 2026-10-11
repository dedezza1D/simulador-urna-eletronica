'use strict';

const TAMANHO_PAGINA = 8;
const CAMPOS = ['cargo', 'numero', 'nome', 'partido'];

let pagina = 0;

// ---------------------------------------------------------------- Lista

async function carregarLista() {
    const parametros = new URLSearchParams({
        eleicaoId: secaoAtual().eleicao,
        page: pagina,
        size: TAMANHO_PAGINA,
        sort: $('ordem').value,
    });
    if ($('filtro-cargo').value) {
        parametros.set('cargo', $('filtro-cargo').value);
    }
    const { dados } = await api('GET', `/api/candidatos?${parametros}`);

    const linhas = dados.conteudo.map((c) => {
        const editar = elemento('button', { type: 'button', className: 'secundario pequeno' }, 'Editar');
        editar.addEventListener('click', () => preencher(c));
        const remover = elemento('button', { type: 'button', className: 'perigo pequeno' }, 'Remover');
        remover.addEventListener('click', () => acao('Remover', () => removerCandidato(c)));
        return [NOMES_CARGO[c.cargo], c.numero, c.nome, c.partido, elemento('span', { className: 'linha' }, editar, remover)];
    });
    $('lista').replaceChildren(tabela(['Cargo', 'Número', 'Nome', 'Partido', ''], linhas));

    const total = Math.max(dados.totalPaginas, 1);
    $('pagina-info').textContent = `Página ${dados.pagina + 1} de ${total} · ${dados.totalElementos} candidatos`;
    $('pagina-anterior').disabled = dados.pagina === 0;
    $('pagina-seguinte').disabled = dados.pagina + 1 >= total;
}

async function removerCandidato(candidato) {
    if (!confirm(`Remover ${candidato.nome} (${candidato.numero})?`)) {
        return;
    }
    await api('DELETE', `/api/candidatos/${candidato.id}`);
    registrar(`${candidato.nome} removido.`, 'ok');
    await carregarLista();
}

// ---------------------------------------------------------------- Formulário

function limparErros() {
    CAMPOS.forEach((campo) => {
        $(campo).removeAttribute('aria-invalid');
        document.querySelector(`[data-erro="${campo}"]`).textContent = '';
    });
}

function mostrarErros(erros) {
    erros.forEach(({ campo, mensagem }) => {
        const alvo = document.querySelector(`[data-erro="${campo}"]`);
        if (alvo) {
            $(campo).setAttribute('aria-invalid', 'true');
            alvo.textContent = mensagem;
        }
    });
}

function preencher(candidato) {
    limparErros();
    $('id').value = candidato.id;
    CAMPOS.forEach((campo) => { $(campo).value = candidato[campo]; });
    $('titulo-form').textContent = `Editar candidato ${candidato.numero}`;
    $('btn-salvar').textContent = 'Salvar alterações';
}

function limparFormulario() {
    $('formulario').reset();
    $('id').value = '';
    limparErros();
    $('titulo-form').textContent = 'Novo candidato';
    $('btn-salvar').textContent = 'Cadastrar';
}

async function salvar(evento) {
    evento.preventDefault();
    limparErros();
    const corpo = { eleicaoId: Number(secaoAtual().eleicao) };
    CAMPOS.forEach((campo) => { corpo[campo] = $(campo).value; });
    const id = $('id').value;
    try {
        const { dados } = id
            ? await api('PUT', `/api/candidatos/${id}`, corpo)
            : await api('POST', '/api/candidatos', corpo);
        registrar(`${id ? 'Atualizado' : 'Cadastrado'}: ${dados.numero} ${dados.nome} (${dados.partido})`, 'ok');
        limparFormulario();
        await carregarLista();
    } catch (erro) {
        if (erro.dados && erro.dados.erros) {
            mostrarErros(erro.dados.erros);
        }
        registrarErro(id ? 'Atualizar' : 'Cadastrar', erro);
    }
}

// ---------------------------------------------------------------- Eventos

$('formulario').addEventListener('submit', salvar);
$('btn-limpar').addEventListener('click', limparFormulario);
$('filtro-cargo').addEventListener('change', () => { pagina = 0; acao('Listar', carregarLista); });
$('ordem').addEventListener('change', () => { pagina = 0; acao('Listar', carregarLista); });
$('pagina-anterior').addEventListener('click', () => { pagina = Math.max(0, pagina - 1); acao('Listar', carregarLista); });
$('pagina-seguinte').addEventListener('click', () => { pagina += 1; acao('Listar', carregarLista); });

acao('Listar', carregarLista);
