# Reposição de Aula

MVP local para uma turma comparar opções de data e hora para repor uma aula.
Os participantes indicam todas as opções em que podem comparecer. A aplicação
conta as indicações, mostra os empates e preserva as respostas após reiniciar.

**POOALAB 2026.2 | Programação Orientada a Objetos Avançada | ADS-NOT-PIT-4S-T1.**
Universidade Católica do Salvador (UCSal). Professor Mario Jorge Pereira.

**Integrantes**

- Andrey Soares dos Santos
- Sophia Desiderio
- Luis Fernando Alves

## Problema e público

O público proposto são estudantes e docentes que precisam combinar uma reposição.
A hipótese é que reunir respostas soltas e descobrir quantas pessoas podem em
cada horário exige retrabalho. O projeto centraliza essa contagem. O grupo
deve verificar o cenário com participantes reais antes de apresentá-lo como
uma necessidade observada. Veja [problema e validação](docs/problema-validacao.md).

## Executar

1. Extraia o ZIP para uma pasta. Abra o terminal nessa pasta.
2. Rode `java -version`. Use Java 8 ou superior, com um navegador moderno.
   A versão testada foi Eclipse Temurin 8u492 no Windows.
3. Execute `java -jar reposicao-de-aula.jar`.
4. Abra [http://127.0.0.1:8080](http://127.0.0.1:8080). Use `Ctrl+C` para parar.

O JAR pronto roda sem Maven, banco de dados, internet ou instalação de
bibliotecas. Se a porta estiver ocupada, execute
`java -jar reposicao-de-aula.jar --porta=8081` e abra
`http://127.0.0.1:8081`.

## Compilar e testar

Para recompilar, instale um **JDK** e confira `javac -version` e `jar` no PATH.

- Windows (PowerShell): `powershell -NoProfile -ExecutionPolicy Bypass -File ./compilar.ps1`
- Linux ou macOS (terminal): `sh ./compilar.sh`

O script compila o código, executa `EnqueteTest` e cria o JAR. Resultado
esperado: linha iniciada por `OK: criação`. No Windows, confira o código de
saída se o terminal mostrar os acentos incorretamente. O script de Windows
foi testado neste ambiente. O script POSIX usa os mesmos comandos com o
separador de classpath correspondente.

## Demonstrar

Para começar do zero, execute em uma pasta recém-extraída. Crie “Reposição de
POOA” com as opções do quadro abaixo e abra o placar antes de responder.

| Opção | Data e hora de demonstração |
| --- | --- |
| 1 | 02/12/2026 às 18:00 |
| 2 | 03/12/2026 às 19:00 |
| 3 | 04/12/2026 às 20:00 |

1. Na aba **Enquete**, responda com `A01` nas opções 1 e 2, `B02` na opção 1
   e `C03` na opção 2. Confira o placar 2, 2, 0 e as três respostas.
2. Reenvie com `a01` marcando só a opção 2. Confira 2, 1, 0 na ordem do
   placar, com a opção 2 na liderança. O total de respostas continua três.
3. Encerre a votação no resultado. Reinicie o JAR e confira que o placar
   está disponível e que a enquete permanece encerrada.

Para abrir direto no empate, use uma **cópia** do exemplo em uma sessão
própria. No PowerShell:

```powershell
New-Item -ItemType Directory -Force dados | Out-Null
if (-not (Test-Path 'dados/demo.properties')) { Copy-Item 'demo/exemplo.properties' 'dados/demo.properties' }
java -jar reposicao-de-aula.jar --dados=dados/demo.properties
```

No Linux/macOS, crie `dados/` e copie `demo/exemplo.properties` para
`dados/demo.properties` antes de executar o último comando. Para abrir uma
nova enquete sem sobrescrever nenhuma, escolha outro arquivo ainda inexistente
com `--dados=dados/nova.properties`.

## Código, decisões e limitações

- `Enquete.java` define as regras e o placar. Um `Map` guarda uma resposta por
  código e um `Set` guarda os horários distintos daquela resposta.
- `EnqueteService.java` coordena criação, resposta e encerramento. A interface
  `RepositorioEnquete` permite trocar a gravação sem mudar essas regras.
- `EnqueteArquivo.java` persiste a sessão com `Properties` no diretório `dados/`.
- `Aplicacao.java` recebe os formulários pelo `HttpServer`. `Tela.java` monta
  as páginas HTML e `resources/estilos.css` cuida da apresentação.
- Uma enquete por arquivo, com duas a quatro opções. Uma única instância deve
  usar cada arquivo de dados. Os horários são locais, sem conversão de fuso.
- O MVP é local, com uso na mesma estação. Não há login, verificação de
  identidade, acesso por celular de outra rede ou integração com o Classroom.
  Qualquer pessoa usando a estação pode encerrar a enquete ou substituir um
  código conhecido. Não use dados pessoais nem exponha o servidor na rede.
- O placar informa disponibilidade. A turma e o docente ainda combinam o
  horário, sobretudo em empate, e confirmam a sala por fora do sistema.

## Material da entrega

- [Relatório acadêmico em PDF](docs/Relatorio-academico-Reposicao-de-Aula.pdf)
  e [versão editável em Word](docs/Relatorio-academico-Reposicao-de-Aula.docx).
  O relatório apresenta problema, escopo, decisões técnicas, testes e limites
  do MVP, em formato adaptado do
  [manual da Biblioteca da UCSal](https://ri.ucsal.br/handle/prefix/2148).
- [Definição do MVP](docs/definicao-mvp.md)
- [Problema e registro de validação](docs/problema-validacao.md)
- [Declaração de IA e terceiros](docs/declaracao-ia-terceiros.md)
- [Roteiro editável do pitch](docs/roteiro-pitch.md) e [pitch em PDF](docs/pitch-base.pdf)
- [Roteiro visual de contingência](demo/roteiro-visual.pdf)

IA usada para propor o recorte, gerar e revisar a base de código, testes e
documentos. A declaração detalha o uso e a verificação pendente pelo grupo.
O aplicativo usa APIs distribuídas com o JDK, sem bibliotecas adicionadas.
Referências: [HttpServer](https://docs.oracle.com/javase/8/docs/jre/api/net/httpserver/spec/com/sun/net/httpserver/HttpServer.html),
[Stream](https://docs.oracle.com/javase/8/docs/api/java/util/stream/Stream.html)
e [Properties](https://docs.oracle.com/javase/8/docs/api/java/util/Properties.html).
Para relacionar o código aos conteúdos da disciplina, consulte a [página de
POOA do professor Mario Jorge Pereira](https://pooadoc.github.io/) e o
[catálogo de Repository](https://martinfowler.com/eaaCatalog/repository.html).
