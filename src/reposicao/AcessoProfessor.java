package reposicao;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

final class AcessoProfessor {
    static final int DURACAO_SESSAO_SEGUNDOS = 7200;
    private static final int MAXIMO_TENTATIVAS = 5;
    private static final SecureRandom SORTEIO = new SecureRandom();
    private final byte[] sal;
    private final byte[] senhaProtegida;
    private int falhas;
    private long inicioBloqueio;
    private String sessao;
    private long inicioSessao;

    private AcessoProfessor(byte[] sal, byte[] senhaProtegida) {
        this.sal = sal;
        this.senhaProtegida = senhaProtegida;
    }

    static AcessoProfessor abrir(Path dados, boolean gerarOutra) throws IOException {
        Path arquivo = dados.toAbsolutePath().resolveSibling(dados.getFileName() + ".senha-professor");
        if (!gerarOutra && Files.exists(arquivo)) {
            Properties registro = new Properties();
            try (InputStream entrada = Files.newInputStream(arquivo)) {
                registro.load(entrada);
                byte[] sal = Base64.getDecoder().decode(registro.getProperty("sal", ""));
                byte[] senhaProtegida = Base64.getDecoder().decode(registro.getProperty("senha", ""));
                if (sal.length != 16 || senhaProtegida.length != 32) {
                    throw new IllegalArgumentException("Formato inválido.");
                }
                return new AcessoProfessor(sal, senhaProtegida);
            } catch (IllegalArgumentException e) {
                throw new IOException("Arquivo da senha inválido. Use --nova-senha-professor para gerar outra senha.", e);
            }
        }

        String senha = String.format(Locale.ROOT, "%06d", SORTEIO.nextInt(1000000));
        byte[] sal = bytesAleatorios(16);
        AcessoProfessor acesso = new AcessoProfessor(sal, proteger(senha, sal));
        Properties registro = new Properties();
        registro.setProperty("sal", Base64.getEncoder().encodeToString(sal));
        registro.setProperty("senha", Base64.getEncoder().encodeToString(acesso.senhaProtegida));
        Files.createDirectories(arquivo.getParent());
        Path temporario = Files.createTempFile(arquivo.getParent(), "acesso-professor-", ".tmp");
        try {
            try (OutputStream saida = Files.newOutputStream(temporario)) {
                registro.store(saida, "Acesso do professor");
            }
            try {
                Files.move(temporario, arquivo, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporario, arquivo, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporario);
        }
        System.out.println("Senha do professor (guarde com o professor): " + senha);
        return acesso;
    }

    synchronized String entrar(String senha) {
        if (falhas >= MAXIMO_TENTATIVAS) {
            if (System.nanoTime() - inicioBloqueio < TimeUnit.SECONDS.toNanos(30)) {
                return null;
            }
            falhas = 0;
        }
        if (!senha.matches("[0-9]{6}") || !MessageDigest.isEqual(senhaProtegida, proteger(senha, sal))) {
            falhas++;
            if (falhas == MAXIMO_TENTATIVAS) {
                inicioBloqueio = System.nanoTime();
            }
            return "";
        }
        falhas = 0;
        sessao = Base64.getUrlEncoder().withoutPadding().encodeToString(bytesAleatorios(32));
        inicioSessao = System.nanoTime();
        return sessao;
    }

    synchronized boolean autorizado(String chave) {
        return sessao != null && sessao.equals(chave)
                && System.nanoTime() - inicioSessao < TimeUnit.SECONDS.toNanos(DURACAO_SESSAO_SEGUNDOS);
    }

    synchronized void sair(String chave) {
        if (autorizado(chave)) {
            sessao = null;
        }
    }

    private static byte[] proteger(String senha, byte[] sal) {
        PBEKeySpec dados = new PBEKeySpec(senha.toCharArray(), sal, 150000, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(dados).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("O Java não oferece suporte à proteção da senha.", e);
        } finally {
            dados.clearPassword();
        }
    }

    private static byte[] bytesAleatorios(int tamanho) {
        byte[] bytes = new byte[tamanho];
        SORTEIO.nextBytes(bytes);
        return bytes;
    }
}
