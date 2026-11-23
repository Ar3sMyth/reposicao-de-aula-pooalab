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
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.StringJoiner;

final class EnqueteArquivo implements RepositorioEnquete {
    private final Path arquivo;

    EnqueteArquivo(Path arquivo) {
        this.arquivo = arquivo.toAbsolutePath();
    }

    @Override
    public Optional<Enquete> carregar() throws IOException {
        Properties dados = new Properties();
        try (Reader leitor = Files.newBufferedReader(arquivo, StandardCharsets.UTF_8)) {
            dados.load(leitor);
        } catch (NoSuchFileException e) {
            return Optional.empty();
        } catch (IllegalArgumentException e) {
            throw new IOException("O arquivo de dados tem formato inválido.", e);
        }
        try {
            List<LocalDateTime> horarios = lerHorarios(dados.getProperty("horarios"));
            Map<String, Set<LocalDateTime>> respostas = new LinkedHashMap<>();
            for (String chave : dados.stringPropertyNames()) {
                if (chave.startsWith("voto.")) {
                    List<LocalDateTime> horariosDaResposta = lerHorarios(dados.getProperty(chave));
                    respostas.put(chave.substring(5), new LinkedHashSet<>(horariosDaResposta));
                }
            }
            String encerrada = dados.getProperty("encerrada");
            if (!"true".equals(encerrada) && !"false".equals(encerrada)) {
                throw new IllegalArgumentException("Estado inválido no arquivo.");
            }
            return Optional.of(new Enquete(dados.getProperty("aula", ""), horarios,
                    respostas, Boolean.parseBoolean(encerrada)));
        } catch (RuntimeException e) {
            throw new IOException("Não foi possível ler a enquete existente. Preserve o arquivo de dados.", e);
        }
    }

    @Override
    public void salvar(Enquete enquete) throws IOException {
        Properties dados = new Properties();
        dados.setProperty("aula", enquete.aula);
        dados.setProperty("horarios", juntar(enquete.horarios));
        dados.setProperty("encerrada", Boolean.toString(enquete.encerrada));
        for (Map.Entry<String, Set<LocalDateTime>> resposta : enquete.respostas.entrySet()) {
            dados.setProperty("voto." + resposta.getKey(), juntar(resposta.getValue()));
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
