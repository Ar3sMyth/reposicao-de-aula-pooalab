package reposicao;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

final class EnqueteService {
    private final RepositorioEnquete repositorio;
    private Enquete atual;

    EnqueteService(RepositorioEnquete repositorio) throws IOException {
        this.repositorio = repositorio;
        this.atual = repositorio.carregar().orElse(null);
    }

    synchronized Optional<Enquete> consultar() {
        return Optional.ofNullable(atual);
    }

    synchronized void criar(String aula, List<LocalDateTime> horarios) throws IOException {
        if (atual != null) {
            throw new IllegalArgumentException("Já existe uma enquete nesta sessão.");
        }
        gravar(new Enquete(aula, horarios, Collections.emptyMap(), false));
    }

    synchronized void responder(String codigo, Set<LocalDateTime> horarios, boolean nenhum) throws IOException {
        gravar(exigirEnquete().comResposta(codigo, horarios, nenhum));
    }

    synchronized void encerrar() throws IOException {
        Enquete enquete = exigirEnquete();
        if (!enquete.encerrada) {
            gravar(new Enquete(enquete.aula, enquete.horarios, enquete.respostas, true));
        }
    }

    private Enquete exigirEnquete() {
        if (atual == null) {
            throw new IllegalArgumentException("Crie uma enquete primeiro.");
        }
        return atual;
    }

    private void gravar(Enquete atualizada) throws IOException {
        repositorio.salvar(atualizada);
        atual = atualizada;
    }
}
