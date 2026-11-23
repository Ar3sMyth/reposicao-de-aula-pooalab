package reposicao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public final class EnqueteTest {
    public static void main(String[] args) throws Exception {
        Path pasta = Files.createTempDirectory("reposicao-teste-");
        Path arquivo = pasta.resolve("enquete.properties");
        try {
            LocalDateTime a = LocalDateTime.parse("2026-12-02T18:00");
            LocalDateTime b = LocalDateTime.parse("2026-12-03T19:00");
            LocalDateTime c = LocalDateTime.parse("2026-12-04T20:00");
            RepositorioEnquete repositorio = new EnqueteArquivo(arquivo);
            EnqueteService servico = new EnqueteService(repositorio);
            verificar(!servico.consultar().isPresent(), "Inicialização sem dados");
            rejeitar(() -> servico.criar("POOA", Arrays.asList(a, a)), "Horários repetidos");
            rejeitar(() -> servico.criar("POOA", Arrays.asList(a)), "Uma opção apenas");
            servico.criar("Reposição de POOA", Arrays.asList(b, a, c));
            rejeitar(() -> servico.criar("Outra", Arrays.asList(a, b)), "Substituição sem autorização");
            rejeitar(() -> servico.responder("!?1", opcoes(a), false), "Código inválido");
            rejeitar(() -> servico.responder("A01", opcoes(a.plusDays(10)), false), "Horário externo");
            rejeitar(() -> servico.responder("A01", opcoes(a), true), "Resposta contraditória");
            rejeitar(() -> servico.responder("A01", Collections.emptySet(), false), "Resposta sem seleção");
            servico.responder("X01", Collections.emptySet(), true);
            verificar(servico.consultar().get().resultado().get(0).total == 0, "Indisponível em todas as opções");
            servico.responder("a01", opcoes(a, b), false);
            servico.responder("B02", opcoes(a), false);
            servico.responder("C03", opcoes(b), false);
            Enquete empate = servico.consultar().get();
            verificar(empate.resultado().get(0).horario.equals(a) && empate.resultado().get(0).total == 2
                    && empate.resultado().get(1).total == 2, "Empate ordenado por data e hora");
            servico.responder("A01", opcoes(b), false);
            EnqueteService reiniciado = new EnqueteService(repositorio);
            Enquete salva = reiniciado.consultar().get();
            verificar(salva.respostas.size() == 4, "Um código mantém uma única resposta");
            verificar(salva.resultado().get(0).horario.equals(b)
                    && salva.resultado().get(1).total == 1, "Edição persistida e placar atualizado");
            verificar(salva.aula.equals("Reposição de POOA"), "Acentos preservados no arquivo");
            reiniciado.encerrar();
            EnqueteService encerrado = new EnqueteService(repositorio);
            verificar(encerrado.consultar().get().encerrada, "Encerramento persistido");
            rejeitar(() -> encerrado.responder("D04", opcoes(a), false), "Voto após encerramento");

            EnqueteService semGravacao = new EnqueteService(new RepositorioEnquete() {
                public Optional<Enquete> carregar() { return Optional.of(salva); }
                public void salvar(Enquete enquete) throws IOException { throw new IOException("Falha simulada"); }
            });
            try {
                semGravacao.responder("D04", opcoes(c), false);
                throw new AssertionError("A falha de gravação deveria chegar ao chamador.");
            } catch (IOException esperado) {
                verificar(semGravacao.consultar().get() == salva, "Falha de gravação preserva estado anterior");
            }
            System.out.println("OK: criação, validações, empate, edição, encerramento e persistência.");
        } finally {
            Files.deleteIfExists(arquivo);
            Files.deleteIfExists(pasta);
        }
    }

    private static Set<LocalDateTime> opcoes(LocalDateTime... datas) {
        return new LinkedHashSet<>(Arrays.asList(datas));
    }

    private static void verificar(boolean condicao, String caso) {
        if (!condicao) throw new AssertionError(caso);
    }

    private static void rejeitar(Acao acao, String caso) throws IOException {
        try {
            acao.executar();
        } catch (IllegalArgumentException esperado) {
            return;
        }
        throw new AssertionError(caso);
    }

    private interface Acao { void executar() throws IOException; }
}
