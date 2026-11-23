package reposicao;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringJoiner;

final class EnqueteArquivo implements RepositorioEnquete {
    private final Path arquivo;

    EnqueteArquivo(Path arquivo) {
        this.arquivo = arquivo.toAbsolutePath();
    }

    @Override
    public Sessao carregar() throws IOException {
        Properties dados = new Properties();
        try (Reader leitor = Files.newBufferedReader(arquivo, StandardCharsets.UTF_8)) {
            dados.load(leitor);
        } catch (NoSuchFileException e) {
            return new Sessao(null, Collections.emptyList());
        } catch (IllegalArgumentException e) {
            throw new IOException("O arquivo de dados tem formato inválido.", e);
        }
        try {
            Enquete atual = lerEnquete(dados, "");
            int total = Integer.parseInt(dados.getProperty("historico.total", "0"));
            if (total < 0 || total > dados.size()) {
                throw new IllegalArgumentException("Histórico inválido.");
            }
            List<Enquete> historico = new ArrayList<>();
            for (int i = 1; i <= total; i++) {
                Enquete anterior = lerEnquete(dados, prefixoHistorico(i));
                if (!anterior.encerrada) {
                    throw new IllegalArgumentException("Há uma enquete aberta no histórico.");
                }
                historico.add(anterior);
            }
            return new Sessao(atual, historico);
        } catch (IllegalArgumentException | java.time.format.DateTimeParseException e) {
            throw new IOException("Não foi possível ler a sessão. Preserve o arquivo de dados.", e);
        }
    }

    @Override
    public void salvar(Sessao sessao) throws IOException {
        Properties dados = new Properties();
        escreverEnquete(dados, "", sessao.atual);
        dados.setProperty("historico.total", Integer.toString(sessao.historico.size()));
        for (int i = 0; i < sessao.historico.size(); i++) {
            escreverEnquete(dados, prefixoHistorico(i + 1), sessao.historico.get(i));
        }
        Files.createDirectories(arquivo.getParent());
        Path temporario = Files.createTempFile(arquivo.getParent(), "enquete-", ".tmp");
        // Terminamos de escrever a nova enquete antes de substituir a anterior.
        try {
            try (Writer escritor = Files.newBufferedWriter(temporario, StandardCharsets.UTF_8)) {
                dados.store(escritor, "Reposição de Aula");
            }
            try {
                Files.move(temporario, arquivo, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporario, arquivo, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporario);
        }
    }

    private static String prefixoHistorico(int numero) {
        return "historico." + numero + ".";
    }

    private static Enquete lerEnquete(Properties dados, String prefixo) {
        List<LocalDateTime> horarios = lerHorarios(dados.getProperty(prefixo + "horarios"));
        Map<String, Set<LocalDateTime>> respostas = new LinkedHashMap<>();
        String prefixoVoto = prefixo + "voto.";
        for (String chave : dados.stringPropertyNames()) {
            if (chave.startsWith(prefixoVoto)) {
                List<LocalDateTime> escolha = lerHorarios(dados.getProperty(chave));
                respostas.put(chave.substring(prefixoVoto.length()), new LinkedHashSet<>(escolha));
            }
        }
        String encerrada = dados.getProperty(prefixo + "encerrada");
        if (!"true".equals(encerrada) && !"false".equals(encerrada)) {
            throw new IllegalArgumentException("Estado inválido no arquivo.");
        }
        return new Enquete(dados.getProperty(prefixo + "aula", ""), horarios,
                respostas, Boolean.parseBoolean(encerrada));
    }

    private static void escreverEnquete(Properties dados, String prefixo, Enquete enquete) {
        dados.setProperty(prefixo + "aula", enquete.aula);
        dados.setProperty(prefixo + "horarios", juntar(enquete.horarios));
        dados.setProperty(prefixo + "encerrada", Boolean.toString(enquete.encerrada));
        for (Map.Entry<String, Set<LocalDateTime>> resposta : enquete.respostas.entrySet()) {
            dados.setProperty(prefixo + "voto." + resposta.getKey(), juntar(resposta.getValue()));
        }
    }

    private static String juntar(Collection<LocalDateTime> horarios) {
        List<LocalDateTime> ordenados = new ArrayList<>(horarios);
        Collections.sort(ordenados);
        StringJoiner texto = new StringJoiner(",");
        for (LocalDateTime horario : ordenados) {
            texto.add(horario.toString());
        }
        return texto.toString();
    }

    private static List<LocalDateTime> lerHorarios(String dados) {
        if (dados == null) {
            throw new IllegalArgumentException("Campo de horários ausente.");
        }
        List<LocalDateTime> horarios = new ArrayList<>();
        if (!dados.isEmpty()) {
            for (String horario : dados.split(",", -1)) {
                horarios.add(LocalDateTime.parse(horario));
            }
        }
        return horarios;
    }
}
