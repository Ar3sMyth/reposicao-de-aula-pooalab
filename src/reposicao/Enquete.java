package reposicao;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class Enquete {
    final String aula;
    final List<LocalDateTime> horarios;
    final Map<String, Set<LocalDateTime>> respostas;
    final boolean encerrada;

    Enquete(String aula, List<LocalDateTime> horarios,
            Map<String, Set<LocalDateTime>> respostas, boolean encerrada) {
        this.aula = aula.trim();
        if (this.aula.isEmpty() || this.aula.length() > 80) {
            throw new IllegalArgumentException("Informe o nome da aula com até 80 caracteres.");
        }
        List<LocalDateTime> opcoes = new ArrayList<>(horarios);
        if (opcoes.size() < 2 || opcoes.size() > 4) {
            throw new IllegalArgumentException("Informe de dois a quatro horários.");
        }
        if (new LinkedHashSet<>(opcoes).size() != opcoes.size()) {
            throw new IllegalArgumentException("Cada opção precisa ter um horário diferente.");
        }
        for (LocalDateTime horario : opcoes) {
            if (horario.getYear() < 2000 || horario.getYear() > 2099
                    || horario.getSecond() != 0 || horario.getNano() != 0) {
                throw new IllegalArgumentException("Informe datas de 2000 a 2099, com precisão de minutos.");
            }
        }
        Collections.sort(opcoes);
        this.horarios = Collections.unmodifiableList(opcoes);
        Map<String, Set<LocalDateTime>> copia = new LinkedHashMap<>();
        for (Map.Entry<String, Set<LocalDateTime>> resposta : respostas.entrySet()) {
            String codigo = normalizarCodigo(resposta.getKey());
            if (!opcoes.containsAll(resposta.getValue())) {
                throw new IllegalArgumentException("Selecione apenas horários desta enquete.");
            }
            copia.put(codigo, Collections.unmodifiableSet(new LinkedHashSet<>(resposta.getValue())));
        }
        this.respostas = Collections.unmodifiableMap(copia);
        this.encerrada = encerrada;
    }

    Enquete comResposta(String codigo, Set<LocalDateTime> disponibilidade, boolean nenhum) {
        if (encerrada) {
            throw new IllegalArgumentException("A votação foi encerrada.");
        }
        if (nenhum && !disponibilidade.isEmpty()) {
            throw new IllegalArgumentException("Desmarque Nenhum horário antes de indicar um horário disponível.");
        }
        if (!nenhum && disponibilidade.isEmpty()) {
            throw new IllegalArgumentException("Marque os horários disponíveis ou a opção Nenhum horário.");
        }
        Map<String, Set<LocalDateTime>> atualizadas = new LinkedHashMap<>(respostas);
        atualizadas.put(normalizarCodigo(codigo), disponibilidade);
        return new Enquete(aula, horarios, atualizadas, false);
    }

    private static String normalizarCodigo(String entrada) {
        String codigo = entrada.trim();
        if (!codigo.matches("[A-Za-z0-9]{3,12}")) {
            throw new IllegalArgumentException("Use um código de 3 a 12 letras sem acento ou números.");
        }
        return codigo.toUpperCase(Locale.ROOT);
    }

    List<Placar> resultado() {
        List<Placar> placar = new ArrayList<>();
        for (LocalDateTime horario : horarios) {
            long total = respostas.values().stream()
                    .filter(disponiveis -> disponiveis.contains(horario))
                    .count();
            placar.add(new Placar(horario, total));
        }
        placar.sort(Comparator.comparingLong((Placar item) -> item.total)
                .reversed().thenComparing(item -> item.horario));
        return placar;
    }

    static final class Placar {
        final LocalDateTime horario;
        final long total;

        Placar(LocalDateTime horario, long total) {
            this.horario = horario;
            this.total = total;
        }
    }
}
