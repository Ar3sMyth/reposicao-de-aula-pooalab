package reposicao;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import reposicao.RepositorioEnquete.Sessao;

final class Tela {
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", new Locale("pt", "BR"));
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private Tela() {}

    static String criar(Sessao sessao, String aviso, Map<String, List<String>> dados) {
        String titulo = sessao.atual == null ? "Criar enquete" : "Nova enquete";
        StringBuilder form = new StringBuilder("<h1>" + titulo + "</h1>"
                + "<p>Informe o nome da aula e escolha de dois a quatro horários.</p>"
                + erro(aviso) + "<form method=\"post\" action=\"/criar\">" + campoNumero(sessao)
                + "<label for=\"aula\">Nome da aula</label>"
                + "<input id=\"aula\" name=\"aula\" maxlength=\"80\" required value=\""
                + escapar(Aplicacao.primeiro(dados, "aula")) + "\"><div class=\"grade\">");
        for (int i = 1; i <= 4; i++) {
            String id = "horario" + i;
            form.append("<div><label for=\"").append(id).append("\">Opção ").append(i)
                    .append(i > 2 ? " <span class=\"opcional\">(opcional)</span>" : "")
                    .append("</label><input type=\"datetime-local\" id=\"").append(id)
                    .append("\" name=\"").append(id).append("\" value=\"")
                    .append(escapar(Aplicacao.primeiro(dados, id))).append("\"")
                    .append(i <= 2 ? " required" : "").append("></div>");
        }
        form.append("</div><button type=\"submit\">Criar enquete</button></form>");
        if (sessao.atual != null) {
            form.append("<p class=\"acoes\"><a href=\"/professor\">Voltar à área do professor</a></p>");
        }
        return pagina(titulo, form.toString(), "/professor");
    }

    static String inicio(Sessao sessao, String aviso, Map<String, List<String>> dados) {
        Enquete enquete = sessao.atual;
        if (enquete == null) {
            return pagina("Aluno", "<h1>Enquete da turma</h1>" + erro(aviso)
                    + "<p>Quando o professor criar uma enquete, os horários aparecerão aqui.</p>", "/");
        }
        String intro = "<h1>" + escapar(enquete.aula) + "</h1>";
        if (enquete.encerrada) {
            return pagina("Disponibilidade", intro + erro(aviso)
                    + "<p class=\"mensagem\">Votação encerrada.</p>"
                    + "<a class=\"botao\" href=\"/resultado\">Ver resultado final</a>", "/");
        }
        List<String> marcados = dados.getOrDefault("opcao", Collections.emptyList());
        StringBuilder form = new StringBuilder(intro + "<p>Marque todos os horários em que você pode participar.</p>"
                + erro(aviso) + "<form method=\"post\" action=\"/responder\">" + campoNumero(sessao)
                + "<label for=\"codigo\">Código de participante</label>"
                + "<input id=\"codigo\" name=\"codigo\" required maxlength=\"12\" minlength=\"3\""
                + " pattern=\"[A-Za-z0-9]{3,12}\" aria-describedby=\"dica-codigo\" autocomplete=\"off\" value=\""
                + escapar(Aplicacao.primeiro(dados, "codigo")) + "\">"
                + "<p id=\"dica-codigo\" class=\"dica\">Use um código fictício, como A07. "
                + "Para alterar sua resposta, use o mesmo código.</p>"
                + "<fieldset><legend>Horários disponíveis</legend>");
        for (LocalDateTime horario : enquete.horarios) {
            form.append("<label class=\"opcao\"><input type=\"checkbox\" name=\"opcao\" value=\"")
                    .append(horario).append("\"").append(marcados.contains(horario.toString()) ? " checked" : "")
                    .append(">").append(dataHora(horario)).append("</label>");
        }
        form.append("<label class=\"opcao nenhum\"><input type=\"checkbox\" name=\"nenhum\" value=\"sim\"")
                .append(dados.containsKey("nenhum") ? " checked" : "")
                .append(">Nenhum horário</label></fieldset>")
                .append("<button type=\"submit\">Registrar disponibilidade</button></form>");
        return pagina("Disponibilidade", form.toString(), "/");
    }

    static String resultado(Enquete enquete, boolean salvou) {
        String corpo = "<h1>Resultado</h1>";
        if (enquete == null) {
            corpo += "<p>O resultado aparece assim que houver uma enquete.</p>";
        } else {
            corpo += "<p>" + escapar(enquete.aula) + "</p>"
                    + (salvou ? "<p class=\"mensagem\" role=\"status\">Disponibilidade registrada.</p>" : "")
                    + placar(enquete);
        }
        return pagina("Resultado", corpo, "/resultado");
    }

    static String entrar(String aviso) {
        return pagina("Entrar como professor", "<h1>Professor</h1>"
                + "<p>Use a senha de seis dígitos mostrada na janela do iniciador na primeira abertura.</p>" + erro(aviso)
                + "<form method=\"post\" action=\"/professor/entrar\">"
                + "<label for=\"senha\">Senha do professor</label>"
                + "<input id=\"senha\" name=\"senha\" type=\"password\" inputmode=\"numeric\""
                + " autocomplete=\"current-password\" required maxlength=\"6\" pattern=\"[0-9]{6}\">"
                + "<button type=\"submit\">Entrar</button></form>", "/professor");
    }

    static String professor(Sessao sessao, String aviso, String mensagem) {
        Enquete enquete = sessao.atual;
        StringBuilder corpo = new StringBuilder("<div class=\"cabecalho-professor\"><h1>Professor</h1>"
                + "<form class=\"sair-professor\" method=\"post\" action=\"/professor/sair\">"
                + "<button type=\"submit\" aria-label=\"Sair da área do professor\">Sair</button></form></div>"
                + erro(aviso)
                + "<p class=\"dica\">Demonstração local. Use códigos fictícios.</p>"
                + (mensagem.isEmpty() ? "" : "<p class=\"mensagem\" role=\"status\">" + mensagem + "</p>"));
        if (enquete == null) {
            corpo.append("<p>A turma ainda não tem uma enquete.</p>"
                    + "<a class=\"botao\" href=\"/professor/nova\">Criar enquete</a>");
        } else {
            corpo.append("<h2>").append(escapar(enquete.aula)).append("</h2>");
            if (!enquete.respostas.isEmpty()) {
                corpo.append("<p><a class=\"atalho-respostas\" href=\"#respostas\">Ver respostas por código</a></p>");
            }
            corpo.append(placar(enquete));
            if (enquete.encerrada) {
                corpo.append("<p class=\"acoes\"><a class=\"botao\" href=\"/professor/nova\">Nova enquete</a></p>");
            } else {
                corpo.append("<details><summary>Encerrar votação</summary>"
                        + "<p>Depois de encerrar, os alunos não poderão enviar ou alterar respostas.</p>"
                        + "<form method=\"post\" action=\"/encerrar\">" + campoNumero(sessao)
                        + "<button type=\"submit\">Confirmar encerramento</button></form></details>");
            }
            corpo.append(respostas(enquete));
        }
        if (!sessao.historico.isEmpty()) {
            corpo.append("<section><h2>Histórico</h2><ul>");
            for (int i = sessao.historico.size() - 1; i >= 0; i--) {
                corpo.append("<li><a href=\"/professor/historico?").append(i + 1).append("\">")
                        .append(escapar(sessao.historico.get(i).aula)).append(" (enquete ")
                        .append(i + 1).append(")</a></li>");
            }
            corpo.append("</ul></section>");
        }
        return pagina("Professor", corpo.toString(), "/professor");
    }

    static String historico(Enquete enquete) {
        return pagina("Histórico", "<h1>" + escapar(enquete.aula) + "</h1>"
                + "<p><a href=\"/professor\">Voltar à área do professor</a></p>"
                + placar(enquete) + respostas(enquete), "/professor");
    }

    private static String respostas(Enquete enquete) {
        StringBuilder corpo = new StringBuilder("<section id=\"respostas\"><h2>Respostas por código</h2>");
        if (enquete.respostas.isEmpty()) {
            return corpo.append("<p>Nenhum código respondeu ainda.</p></section>").toString();
        }
        corpo.append("<table class=\"respostas\"><thead><tr><th scope=\"col\">Código</th>"
                + "<th scope=\"col\">Horários informados</th></tr></thead><tbody>");
        for (Map.Entry<String, Set<LocalDateTime>> resposta : new TreeMap<>(enquete.respostas).entrySet()) {
            corpo.append("<tr><th scope=\"row\">").append(escapar(resposta.getKey())).append("</th><td>");
            if (resposta.getValue().isEmpty()) {
                corpo.append("Nenhum horário");
            } else {
                for (LocalDateTime horario : enquete.horarios) {
                    if (resposta.getValue().contains(horario)) {
                        corpo.append(dataHora(horario));
                    }
                }
            }
            corpo.append("</td></tr>");
        }
        return corpo.append("</tbody></table></section>").toString();
    }

    private static String campoNumero(Sessao sessao) {
        return "<input type=\"hidden\" name=\"enquete\" value=\"" + sessao.numeroAtual() + "\">";
    }

    private static String placar(Enquete enquete) {
        List<Enquete.Placar> placar = enquete.resultado();
        long maior = placar.get(0).total;
        long empatados = placar.stream().filter(item -> item.total == maior).count();
        int totalRespostas = enquete.respostas.size();
        String resumo;
        if (totalRespostas == 0) {
            resumo = "Ainda não há respostas.";
        } else if (maior == 0) {
            resumo = "Nenhum horário recebeu indicação de disponibilidade.";
        } else if (empatados > 1) {
            resumo = "Empate entre " + empatados + " horários com " + plural(maior, "indicação", "indicações") + ".";
        } else {
            resumo = "O horário com mais indicações recebeu " + plural(maior, "indicação", "indicações") + ".";
        }
        StringBuilder corpo = new StringBuilder(
                "<div class=\"resumo\"><strong>" + resumo + "</strong><span>"
                + plural(totalRespostas, "resposta", "respostas") + " · Votação "
                + (enquete.encerrada ? "encerrada" : "aberta") + "</span></div>"
                + "<table><caption>Disponibilidade por horário</caption>"
                + "<thead><tr><th scope=\"col\">Horário</th><th scope=\"col\">Indicações</th></tr></thead><tbody>");
        for (Enquete.Placar item : placar) {
            boolean lider = maior > 0 && item.total == maior;
            corpo.append("<tr><th scope=\"row\">").append(dataHora(item.horario))
                    .append(lider ? "<small>" + (empatados > 1 ? "Empatado na liderança" : "Maior disponibilidade") + "</small>" : "")
                    .append("</th><td>").append(item.total).append("</td></tr>");
        }
        return corpo.append("</tbody></table>").toString();
    }

    static String recado(String mensagem) {
        return pagina("Aviso", "<h1>Aviso</h1>" + erro(mensagem) + "<a href=\"/\">Voltar ao início</a>", "/");
    }

    private static String pagina(String titulo, String corpo, String paginaAtual) {
        return "<!doctype html><html lang=\"pt-BR\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<title>" + titulo + " | Reposição de Aula</title><link rel=\"stylesheet\" href=\"/estilos.css\"></head>"
                + "<body><a class=\"pular\" href=\"#conteudo\">Ir para o conteúdo</a>"
                + "<header><div class=\"topo\"><a class=\"nome\" href=\"/\">"
                + "<img src=\"/ucsal.svg\" alt=\"UCSal\" width=\"112\" height=\"49\">"
                + "<span>Reposição de Aula</span></a>"
                + "<nav aria-label=\"Páginas\">" + nav("/", "Aluno", paginaAtual)
                + nav("/resultado", "Resultado", paginaAtual) + nav("/professor", "Professor", paginaAtual)
                + "</nav></div></header><main id=\"conteudo\">" + corpo + "</main></body></html>";
    }

    private static String nav(String rota, String nome, String paginaAtual) {
        return "<a href=\"" + rota + "\"" + (rota.equals(paginaAtual) ? " aria-current=\"page\"" : "")
                + ">" + nome + "</a>";
    }

    private static String dataHora(LocalDateTime horario) {
        return "<time datetime=\"" + horario + "\"><strong>" + horario.format(HORA) + "</strong> "
                + "<span>" + horario.format(DIA) + "</span></time>";
    }

    private static String plural(long total, String singular, String plural) {
        return total + " " + (total == 1 ? singular : plural);
    }

    private static String erro(String aviso) {
        return aviso.isEmpty() ? "" : "<p class=\"erro\" role=\"alert\">" + escapar(aviso) + "</p>";
    }

    private static String escapar(String valor) {
        return valor.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
