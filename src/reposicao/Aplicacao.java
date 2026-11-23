package reposicao;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.awt.Desktop;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import reposicao.RepositorioEnquete.Sessao;

public final class Aplicacao {
    private static final int LIMITE_FORMULARIO = 4096;
    private final EnqueteService servico;
    private final String host;
    private final byte[] css;
    private final byte[] logo;

    private Aplicacao(EnqueteService servico, int porta) throws IOException {
        this.servico = servico;
        this.host = "127.0.0.1:" + porta;
        this.css = lerRecurso("/estilos.css");
        this.logo = lerRecurso("/ucsal.svg");
    }

    private static byte[] lerRecurso(String caminho) throws IOException {
        try (InputStream recurso = Aplicacao.class.getResourceAsStream(caminho)) {
            if (recurso == null) {
                throw new IOException(caminho + " não encontrado no pacote.");
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] bloco = new byte[2048];
            int lidos;
            while ((lidos = recurso.read(bloco)) != -1) {
                bytes.write(bloco, 0, lidos);
            }
            return bytes.toByteArray();
        }
    }

    public static void main(String[] args) {
        try {
            int porta = 8080;
            boolean abrir = false;
            Path dados = Paths.get("dados", "enquete.properties");
            for (String arg : args) {
                if (arg.startsWith("--porta=")) {
                    porta = Integer.parseInt(arg.substring(8));
                } else if (arg.startsWith("--dados=")) {
                    dados = Paths.get(arg.substring(8));
                } else if ("--abrir".equals(arg)) {
                    abrir = true;
                } else {
                    throw new IllegalArgumentException("Argumento desconhecido: " + arg);
                }
            }
            if (porta != 0 && (porta < 1024 || porta > 65535)) {
                throw new IllegalArgumentException("Escolha uma porta entre 1024 e 65535 ou use 0 para escolha automática.");
            }
            EnqueteService servico = new EnqueteService(new EnqueteArquivo(dados));
            HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", porta), 0);
            Aplicacao app = new Aplicacao(servico, servidor.getAddress().getPort());
            servidor.createContext("/", app::atender);
            servidor.start();
            String endereco = "http://" + app.host;
            System.out.println("Abra " + endereco + " no navegador. Para encerrar, use Ctrl+C.");
            if (abrir) {
                abrirNavegador(endereco + (servico.sessao().atual == null ? "/professor" : ""));
            }
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Não foi possível iniciar. " + e.getMessage());
            System.exit(1);
        }
    }

    private static void abrirNavegador(String endereco) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(endereco));
                return;
            }
        } catch (IOException | RuntimeException e) {
            // O servidor continua disponível mesmo se o navegador não abrir.
        }
        System.out.println("Abra o endereço acima manualmente.");
    }

    private void atender(HttpExchange conexao) throws IOException {
        try {
            if (!host.equals(conexao.getRequestHeaders().getFirst("Host"))) {
                html(conexao, 403, Tela.recado("Use o endereço local exibido no terminal."));
                return;
            }
            String rota = conexao.getRequestURI().getPath();
            String metodo = conexao.getRequestMethod();
            if ("GET".equals(metodo)) {
                mostrar(conexao, rota);
            } else if ("POST".equals(metodo) && ("/criar".equals(rota)
                    || "/responder".equals(rota) || "/encerrar".equals(rota))) {
                alterar(conexao, rota);
            } else {
                html(conexao, 404, Tela.recado("Página não encontrada."));
            }
        } finally {
            conexao.close();
        }
    }

    private void mostrar(HttpExchange conexao, String rota) throws IOException {
        if ("/estilos.css".equals(rota)) {
            enviar(conexao, 200, "text/css; charset=utf-8", css);
            return;
        }
        if ("/ucsal.svg".equals(rota)) {
            enviar(conexao, 200, "image/svg+xml", logo);
            return;
        }
        Sessao sessao = servico.sessao();
        String consulta = conexao.getRequestURI().getQuery();
        switch (rota) {
            case "/":
                html(conexao, 200, Tela.inicio(sessao, "", Collections.emptyMap()));
                break;
            case "/resultado":
                html(conexao, 200, Tela.resultado(sessao.atual, "sim".equals(consulta)));
                break;
            case "/professor":
                String mensagem = "";
                if ("criada".equals(consulta)) {
                    mensagem = "Enquete aberta para a turma.";
                } else if ("encerrada".equals(consulta)) {
                    mensagem = "Votação encerrada.";
                }
                html(conexao, 200, Tela.professor(sessao, "", mensagem));
                break;
            case "/professor/nova":
                if (sessao.atual == null || sessao.atual.encerrada) {
                    html(conexao, 200, Tela.criar(sessao, "", Collections.emptyMap()));
                } else {
                    redirecionar(conexao, "/professor");
                }
                break;
            case "/professor/historico":
                mostrarHistorico(conexao, sessao, consulta);
                break;
            default:
                html(conexao, 404, Tela.recado("Página não encontrada."));
        }
    }

    private void mostrarHistorico(HttpExchange conexao, Sessao sessao, String consulta) throws IOException {
        int numero;
        try {
            numero = Integer.parseInt(consulta);
        } catch (NumberFormatException e) {
            numero = 0;
        }
        if (numero < 1 || numero > sessao.historico.size()) {
            html(conexao, 404, Tela.recado("Enquete do histórico não encontrada."));
        } else {
            html(conexao, 200, Tela.historico(sessao.historico.get(numero - 1)));
        }
    }

    private void alterar(HttpExchange conexao, String rota) throws IOException {
        String origem = conexao.getRequestHeaders().getFirst("Origin");
        String tipo = conexao.getRequestHeaders().getFirst("Content-Type");
        if ((origem != null && !("http://" + host).equals(origem))
                || "cross-site".equals(conexao.getRequestHeaders().getFirst("Sec-Fetch-Site"))) {
            html(conexao, 403, Tela.recado("Abra a aplicação local para enviar o formulário."));
            return;
        }
        if (tipo == null || !tipo.split(";", 2)[0].trim().equalsIgnoreCase("application/x-www-form-urlencoded")) {
            html(conexao, 415, Tela.recado("Envie os dados pelo formulário da aplicação."));
            return;
        }
        Map<String, List<String>> dados = Collections.emptyMap();
        try {
            dados = lerFormulario(conexao.getRequestBody());
            int numero = numeroEnquete(dados);
            switch (rota) {
                case "/criar":
                    List<LocalDateTime> horarios = new ArrayList<>();
                    for (int i = 1; i <= 4; i++) {
                        String opcao = primeiro(dados, "horario" + i);
                        if (!opcao.isEmpty()) {
                            horarios.add(LocalDateTime.parse(opcao));
                        }
                    }
                    servico.criar(numero, primeiro(dados, "aula"), horarios);
                    redirecionar(conexao, "/professor?criada");
                    break;
                case "/responder":
                    Set<LocalDateTime> escolhidos = new LinkedHashSet<>();
                    for (String valor : dados.getOrDefault("opcao", Collections.emptyList())) {
                        escolhidos.add(LocalDateTime.parse(valor));
                    }
                    servico.responder(numero, primeiro(dados, "codigo"), escolhidos, dados.containsKey("nenhum"));
                    redirecionar(conexao, "/resultado?sim");
                    break;
                default:
                    servico.encerrar(numero);
                    redirecionar(conexao, "/professor?encerrada");
            }
        } catch (DateTimeParseException e) {
            erroFormulario(conexao, 400, rota, "Confira as datas e os horários informados.", dados);
        } catch (IllegalArgumentException e) {
            erroFormulario(conexao, 400, rota, e.getMessage(), dados);
        } catch (IOException e) {
            System.err.println("Falha ao gravar a solicitação: " + e.getMessage());
            erroFormulario(conexao, 500, rota, "Não foi possível salvar. Verifique o arquivo de dados e tente novamente.", dados);
        }
    }

    private void erroFormulario(HttpExchange conexao, int status, String rota, String aviso,
            Map<String, List<String>> dados) throws IOException {
        Sessao sessao = servico.sessao();
        Map<String, List<String>> rascunho = Integer.toString(sessao.numeroAtual()).equals(primeiro(dados, "enquete"))
                ? dados : Collections.emptyMap();
        if ("/responder".equals(rota)) {
            html(conexao, status, Tela.inicio(sessao, aviso, rascunho));
        } else if ("/criar".equals(rota) && (sessao.atual == null || sessao.atual.encerrada)) {
            html(conexao, status, Tela.criar(sessao, aviso, rascunho));
        } else {
            html(conexao, status, Tela.professor(sessao, aviso, ""));
        }
    }

    private static int numeroEnquete(Map<String, List<String>> dados) {
        try {
            return Integer.parseInt(primeiro(dados, "enquete"));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Abra a enquete novamente antes de enviar o formulário.");
        }
    }

    static String primeiro(Map<String, List<String>> dados, String chave) {
        List<String> valores = dados.get(chave);
        return valores == null ? "" : valores.get(0);
    }

    private static Map<String, List<String>> lerFormulario(InputStream entrada) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] bloco = new byte[512];
        int lidos;
        while ((lidos = entrada.read(bloco)) != -1) {
            if (bytes.size() + lidos > LIMITE_FORMULARIO) {
                throw new IllegalArgumentException("O formulário ultrapassou o tamanho aceito.");
            }
            bytes.write(bloco, 0, lidos);
        }
        Map<String, List<String>> campos = new LinkedHashMap<>();
        for (String par : bytes.toString("UTF-8").split("&")) {
            String[] partes = par.split("=", 2);
            if (partes.length == 2) {
                try {
                    String chave = URLDecoder.decode(partes[0], "UTF-8");
                    String valor = URLDecoder.decode(partes[1], "UTF-8");
                    campos.computeIfAbsent(chave, k -> new ArrayList<>()).add(valor);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("O formulário contém caracteres inválidos.");
                }
            }
        }
        return campos;
    }

    private static void redirecionar(HttpExchange conexao, String caminho) throws IOException {
        conexao.getResponseHeaders().set("Location", caminho);
        conexao.getResponseHeaders().set("Cache-Control", "no-store");
        conexao.sendResponseHeaders(303, -1);
    }

    private static void html(HttpExchange conexao, int status, String pagina) throws IOException {
        enviar(conexao, status, "text/html; charset=utf-8", pagina.getBytes(StandardCharsets.UTF_8));
    }

    private static void enviar(HttpExchange conexao, int status, String tipo, byte[] conteudo) throws IOException {
        conexao.getResponseHeaders().set("Content-Type", tipo);
        conexao.getResponseHeaders().set("Cache-Control", "no-store");
        conexao.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        conexao.getResponseHeaders().set("Content-Security-Policy",
                "default-src 'none'; style-src 'self'; img-src 'self'; form-action 'self'; base-uri 'none'; frame-ancestors 'none'");
        conexao.sendResponseHeaders(status, conteudo.length);
        conexao.getResponseBody().write(conteudo);
    }
}
