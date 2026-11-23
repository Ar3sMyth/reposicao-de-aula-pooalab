package reposicao;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import reposicao.RepositorioEnquete.Sessao;

final class EnqueteService {
    private final RepositorioEnquete repositorio;
    private Sessao sessao;

    EnqueteService(RepositorioEnquete repositorio) throws IOException {
        this.repositorio = repositorio;
        this.sessao = repositorio.carregar();
    }

    synchronized Sessao sessao() {
        return sessao;
    }

    synchronized void criar(int numero, String aula, List<LocalDateTime> horarios) throws IOException {
        conferirNumero(numero);
        if (sessao.atual != null && !sessao.atual.encerrada) {
            throw new IllegalArgumentException("Encerre a votação atual antes de criar outra enquete.");
        }
        Enquete nova = new Enquete(aula, horarios, Collections.emptyMap(), false);
        List<Enquete> historico = new ArrayList<>(sessao.historico);
        if (sessao.atual != null) {
            historico.add(sessao.atual);
        }
        gravar(new Sessao(nova, historico));
    }

    synchronized void responder(int numero, String codigo, Set<LocalDateTime> horarios, boolean nenhum) throws IOException {
        conferirNumero(numero);
        Enquete atualizada = exigirEnquete().comResposta(codigo, horarios, nenhum);
        gravar(new Sessao(atualizada, sessao.historico));
    }

    synchronized void encerrar(int numero) throws IOException {
        conferirNumero(numero);
        Enquete enquete = exigirEnquete();
        if (!enquete.encerrada) {
            Enquete encerrada = new Enquete(enquete.aula, enquete.horarios, enquete.respostas, true);
            gravar(new Sessao(encerrada, sessao.historico));
        }
    }

    private void conferirNumero(int numero) {
        if (numero != sessao.numeroAtual()) {
            throw new IllegalArgumentException("Esta tela estava em outra enquete. Confira a enquete atual e tente novamente.");
        }
    }

    private Enquete exigirEnquete() {
        if (sessao.atual == null) {
            throw new IllegalArgumentException("Crie uma enquete primeiro.");
        }
        return sessao.atual;
    }

    private void gravar(Sessao atualizada) throws IOException {
        repositorio.salvar(atualizada);
        sessao = atualizada;
    }
}
