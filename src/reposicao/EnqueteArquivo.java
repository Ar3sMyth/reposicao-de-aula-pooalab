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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

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
                    respostas.put(chave.substring(5),
                            lerHorarios(dados.getProperty(chave)).stream().collect(Collectors.toSet()));
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
        enquete.respostas.forEach((codigo, horarios) -> dados.setProperty("voto." + codigo, juntar(horarios)));
        Files.createDirectories(arquivo.getParent());
        Path temporario = Files.createTempFile(arquivo.getParent(), "enquete-", ".tmp");
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

    private static String juntar(java.util.Collection<LocalDateTime> horarios) {
        return horarios.stream().sorted().map(LocalDateTime::toString).collect(Collectors.joining(","));
    }

    private static List<LocalDateTime> lerHorarios(String dados) {
        if (dados == null) {
            throw new IllegalArgumentException("Campo de horários ausente.");
        }
        return dados.isEmpty() ? java.util.Collections.emptyList()
                : Arrays.stream(dados.split(",", -1)).map(LocalDateTime::parse).collect(Collectors.toList());
    }
}
