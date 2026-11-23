package reposicao;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

interface RepositorioEnquete {
    Sessao carregar() throws IOException;
    void salvar(Sessao sessao) throws IOException;

    final class Sessao {
        final Enquete atual;
        final List<Enquete> historico;

        Sessao(Enquete atual, List<Enquete> historico) {
            this.atual = atual;
            this.historico = Collections.unmodifiableList(new ArrayList<>(historico));
        }

        int numeroAtual() {
            return historico.size() + 1;
        }
    }
}
