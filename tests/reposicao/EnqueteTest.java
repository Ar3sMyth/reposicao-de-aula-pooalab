package reposicao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import reposicao.RepositorioEnquete.Sessao;

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
            verificar(servico.sessao().atual == null, "Inicialização sem dados");
            rejeitar(() -> servico.criar(1, "POOA", Arrays.asList(a, a)), "Horários repetidos");
            rejeitar(() -> servico.criar(1, "POOA", Arrays.asList(a)), "Uma opção apenas");
            servico.criar(1, "Reposição de POOA", Arrays.asList(b, a, c));
            rejeitar(() -> servico.criar(1, "Outra", Arrays.asList(a, b)), "Substituição sem autorização");
            rejeitar(() -> servico.responder(1, "!?1", opcoes(a), false), "Código inválido");
            rejeitar(() -> servico.responder(1, "A01", opcoes(a.plusDays(10)), false), "Horário externo");
            rejeitar(() -> servico.responder(1, "A01", opcoes(a), true), "Resposta contraditória");
            rejeitar(() -> servico.responder(1, "A01", Collections.emptySet(), false), "Resposta sem seleção");
            servico.responder(1, "X01", Collections.emptySet(), true);
            verificar(servico.sessao().atual.resultado().get(0).total == 0, "Indisponível em todas as opções");
            servico.responder(1, "a01", opcoes(a, b), false);
            servico.responder(1, "B02", opcoes(a), false);
            servico.responder(1, "C03", opcoes(b), false);
            Enquete empate = servico.sessao().atual;
            verificar(empate.resultado().get(0).horario.equals(a) && empate.resultado().get(0).total == 2
                    && empate.resultado().get(1).total == 2, "Empate ordenado por data e hora");
            servico.responder(1, "A01", opcoes(b), false);
            EnqueteService reiniciado = new EnqueteService(repositorio);
            Enquete salva = reiniciado.sessao().atual;
            verificar(salva.respostas.size() == 4, "Um código mantém uma única resposta");
            verificar(salva.resultado().get(0).horario.equals(b)
                    && salva.resultado().get(1).total == 1, "Edição persistida e placar atualizado");
            verificar(salva.aula.equals("Reposição de POOA"), "Acentos preservados no arquivo");
            reiniciado.encerrar(1);
            EnqueteService encerrado = new EnqueteService(repositorio);
            verificar(encerrado.sessao().atual.encerrada, "Encerramento persistido");
            rejeitar(() -> encerrado.responder(1, "D04", opcoes(a), false), "Voto após encerramento");

            rejeitar(() -> encerrado.criar(1, "Outra", Arrays.asList(a, a)), "Nova enquete inválida");
            verificar(encerrado.sessao().historico.isEmpty(), "Erro na criação mantém o histórico anterior");
            encerrado.criar(1, "Segunda reposição", Arrays.asList(a, b));
            rejeitar(() -> encerrado.responder(1, "A01", opcoes(a), false), "Formulário de enquete antiga");
            rejeitar(() -> encerrado.encerrar(1), "Encerramento de enquete antiga");
            EnqueteService segunda = new EnqueteService(repositorio);
            Sessao sessao = segunda.sessao();
            verificar(sessao.numeroAtual() == 2 && !sessao.atual.encerrada && sessao.atual.respostas.isEmpty(),
                    "Nova enquete inicia sem respostas");
            verificar(sessao.historico.size() == 1 && sessao.historico.get(0).encerrada
                    && sessao.historico.get(0).respostas.get("A01").equals(opcoes(b))
                    && sessao.historico.get(0).respostas.get("X01").isEmpty(),
                    "Histórico preserva respostas e encerramento após reiniciar");
            segunda.responder(2, "D04", opcoes(a), false);
            segunda.encerrar(2);
            segunda.criar(2, "Terceira reposição", Arrays.asList(b, c));
            verificar(new EnqueteService(repositorio).sessao().historico.size() == 2,
                    "Mais de uma enquete pode entrar no histórico");

            EnqueteService semGravacao = new EnqueteService(new RepositorioEnquete() {
                public Sessao carregar() { return new Sessao(salva, Collections.emptyList()); }
                public void salvar(Sessao sessao) throws IOException { throw new IOException("Falha simulada"); }
            });
            try {
                semGravacao.responder(1, "D04", opcoes(c), false);
                throw new AssertionError("A falha de gravação deveria chegar ao chamador.");
            } catch (IOException esperado) {
                verificar(semGravacao.sessao().atual == salva, "Falha de gravação preserva estado anterior");
            }
            Enquete anterior = sessao.historico.get(0);
            EnqueteService semHistorico = new EnqueteService(new RepositorioEnquete() {
                public Sessao carregar() { return new Sessao(anterior, Collections.emptyList()); }
                public void salvar(Sessao sessao) throws IOException { throw new IOException("Falha simulada"); }
            });
            try {
                semHistorico.criar(1, "Outra reposição", Arrays.asList(a, b));
                throw new AssertionError("A falha de gravação deveria chegar ao chamador.");
            } catch (IOException esperado) {
                verificar(semHistorico.sessao().atual == anterior && semHistorico.sessao().historico.isEmpty(),
                        "Falha na criação mantém a enquete anterior intacta");
            }
            System.out.println("OK: criação, validações, empate, edição, encerramento, nova enquete e histórico.");
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
