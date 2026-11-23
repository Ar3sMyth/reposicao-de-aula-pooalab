package reposicao;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
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

public final class Aplicacao {
    private static final int LIMITE_FORMULARIO = 4096;
    private final EnqueteService servico;
    private final String host;
    private final byte[] css;

    private Aplicacao(EnqueteService servico, int porta) throws IOException {
        this.servico = servico;
        this.host = "127.0.0.1:" + porta;
        try (InputStream recurso = Aplicacao.class.getResourceAsStream("/estilos.css")) {
            if (recurso == null) {
                throw new IOException("estilos.css não encontrado no pacote.");
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] bloco = new byte[2048];
            int lidos;
            while ((lidos = recurso.read(bloco)) != -1) {
                bytes.write(bloco, 0, lidos);
            }
            css = bytes.toByteArray();
        }
    }

    public static void main(String[] args) {
        try {
            int porta = 8080;
            Path dados = Paths.get("dados", "enquete.properties");
            for (String arg : args) {
                if (arg.startsWith("--porta=")) {
                    porta = Integer.parseInt(arg.substring(8));
                } else if (arg.startsWith("--dados=")) {
                    dados = Paths.get(arg.substring(8));
                } else {
                    throw new IllegalArgumentException("Argumento desconhecido: " + arg);
                }
            }
            if (porta < 1024 || porta > 65535) {
                throw new IllegalArgumentException("Escolha uma porta entre 1024 e 65535.");
            }
            Aplicacao app = new Aplicacao(new EnqueteService(new EnqueteArquivo(dados)), porta);
            HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", porta), 0);
            servidor.createContext("/", app::atender);
            servidor.start();
            System.out.println("Abra http://" + app.host + " no navegador. Para encerrar, use Ctrl+C.");
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Não foi possível iniciar. " + e.getMessage());
            System.exit(1);
        }
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
        Enquete enquete = servico.consultar().orElse(null);
        if ("/".equals(rota)) {
            html(conexao, 200, Tela.inicio(enquete, "", Collections.emptyMap()));
        } else if ("/resultado".equals(rota)) {
            html(conexao, 200, Tela.resultado(enquete, "", "sim".equals(conexao.getRequestURI().getQuery())));
        } else {
            html(conexao, 404, Tela.recado("Página não encontrada."));
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
            switch (rota) {
                case "/criar":
                    List<LocalDateTime> horarios = new ArrayList<>();
                    for (int i = 1; i <= 4; i++) {
                        String opcao = primeiro(dados, "horario" + i);
                        if (!opcao.isEmpty()) {
                            horarios.add(LocalDateTime.parse(opcao));
                        }
                    }
                    servico.criar(primeiro(dados, "aula"), horarios);
                    redirecionar(conexao, "/");
                    break;
                case "/responder":
                    Set<LocalDateTime> escolhidos = new LinkedHashSet<>();
                    for (String valor : dados.getOrDefault("opcao", Collections.emptyList())) {
                        escolhidos.add(LocalDateTime.parse(valor));
                    }
                    servico.responder(primeiro(dados, "codigo"), escolhidos, dados.containsKey("nenhum"));
                    redirecionar(conexao, "/resultado?sim");
                    break;
                default:
                    servico.encerrar();
                    redirecionar(conexao, "/resultado");
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
        Enquete enquete = servico.consultar().orElse(null);
        html(conexao, status, "/encerrar".equals(rota)
                ? Tela.resultado(enquete, aviso, false) : Tela.inicio(enquete, aviso, dados));
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
                "default-src 'none'; style-src 'self'; form-action 'self'; base-uri 'none'; frame-ancestors 'none'");
        conexao.sendResponseHeaders(status, conteudo.length);
        conexao.getResponseBody().write(conteudo);
    }
}
