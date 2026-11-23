package reposicao;

import java.io.IOException;
import java.util.Optional;

interface RepositorioEnquete {
    Optional<Enquete> carregar() throws IOException;
    void salvar(Enquete enquete) throws IOException;
}
