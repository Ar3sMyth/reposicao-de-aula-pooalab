package reposicao;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class Tela {
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", new Locale("pt", "BR"));
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private Tela() {}

    static String inicio(Enquete enquete, String aviso, Map<String, List<String>> dados) {
        if (enquete == null) {
            StringBuilder form = new StringBuilder("<h1>Criar enquete</h1>"
                    + "<p>Informe o nome da aula e escolha de dois a quatro horários.</p>"
                    + erro(aviso) + "<form method=\"post\" action=\"/criar\">"
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
            return pagina("Criar enquete", form.toString(), false);
        }
        String intro = "<h1>" + escapar(enquete.aula) + "</h1>";
        if (enquete.encerrada) {
            return pagina("Disponibilidade", intro + erro(aviso)
                    + "<p class=\"mensagem\">Votação encerrada.</p>"
                    + "<a class=\"botao\" href=\"/resultado\">Ver resultado final</a>", false);
        }
        List<String> marcados = dados.getOrDefault("opcao", Collections.emptyList());
        StringBuilder form = new StringBuilder(intro + "<p>Marque todos os horários em que você pode participar.</p>"
                + erro(aviso) + "<form method=\"post\" action=\"/responder\">"
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
        return pagina("Disponibilidade", form.toString(), false);
    }

    static String resultado(Enquete enquete, String aviso, boolean salvou) {
        if (enquete == null) {
            return pagina("Resultado", "<h1>Resultado</h1>" + erro(aviso)
                    + "<p>Crie uma enquete para começar a receber respostas.</p>"
                    + "<a class=\"botao\" href=\"/\">Criar enquete</a>", true);
        }
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
        StringBuilder corpo = new StringBuilder("<h1>Resultado</h1><p>" + escapar(enquete.aula) + "</p>"
                + erro(aviso) + (salvou ? "<p class=\"mensagem\" role=\"status\">Disponibilidade registrada.</p>" : "")
                + "<div class=\"resumo\"><strong>" + resumo + "</strong><span>"
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
        corpo.append("</tbody></table>");
        if (!enquete.encerrada) {
            corpo.append("<div class=\"acoes\"><a href=\"/\">Voltar à enquete</a>"
                    + "<details><summary>Encerrar votação</summary>"
                    + "<p>Depois de encerrar, os participantes não poderão enviar ou alterar respostas.</p>"
                    + "<form method=\"post\" action=\"/encerrar\"><button type=\"submit\">Confirmar encerramento</button></form>"
                    + "</details></div>");
        }
        return pagina("Resultado", corpo.toString(), true);
    }

    static String recado(String mensagem) {
        return pagina("Aviso", "<h1>Aviso</h1>" + erro(mensagem) + "<a href=\"/\">Voltar ao início</a>", false);
    }

    private static String pagina(String titulo, String corpo, boolean resultado) {
        return "<!doctype html><html lang=\"pt-BR\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<title>" + titulo + " | Reposição de Aula</title><link rel=\"stylesheet\" href=\"/estilos.css\"></head>"
                + "<body><a class=\"pular\" href=\"#conteudo\">Ir para o conteúdo</a>"
                + "<header><div class=\"topo\"><a class=\"nome\" href=\"/\">"
                + "<img src=\"/ucsal.svg\" alt=\"UCSal\" width=\"112\" height=\"49\">"
                + "<span>Reposição de Aula</span></a>"
                + "<nav aria-label=\"Páginas\"><a href=\"/\"" + (resultado ? "" : " aria-current=\"page\"")
                + ">Enquete</a><a href=\"/resultado\"" + (resultado ? " aria-current=\"page\"" : "")
                + ">Resultado</a></nav></div></header><main id=\"conteudo\">" + corpo + "</main></body></html>";
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
