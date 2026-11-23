# Reposição de Aula

Trabalho de Programação Orientada a Objetos Avançada da UCSal, POOALAB 2026.2.
Turma ADS-NOT-PIT-4S-T1, professor Mario Jorge Pereira.

Andrey Soares dos Santos, Sophia Desiderio e Luis Fernando Alves.

Quando uma aula precisa ser reposta, a turma indica em quais horários pode
comparecer. O sistema reúne as respostas, soma as indicações de cada horário
e mostra quando há empate. A decisão final fica com a turma e o professor.

## Como rodar

No Windows, extraia o ZIP e dê dois cliques em **Iniciar Reposicao de Aula.cmd**.
O iniciador abre o navegador e usa o Java instalado. Se faltar Java, baixa
o Eclipse Temurin para a pasta do projeto. Esse primeiro download precisa de
internet e Windows x64 ou ARM64. Use `Ctrl+C` na janela do iniciador para parar.

Com Java 8 ou superior, também dá para executar pelo terminal:

```sh
java -jar reposicao-de-aula.jar
```

Abra [http://127.0.0.1:8080](http://127.0.0.1:8080). Se a porta estiver ocupada,
passe `--porta=8081` e abra o endereço com a porta 8081.

## Senha do professor

Na primeira abertura, o iniciador mostra uma senha de seis dígitos. Guarde-a
com o professor e use-a para entrar na aba **Professor**. Ela continua válida
ao reiniciar a mesma sessão. Os alunos votam apenas com o código fictício.
Use **Sair** antes de passar o computador para outra pessoa. Mantenha a janela
do iniciador com o professor, pois ela exibe a senha na primeira abertura.

Se perder a senha, feche o sistema e execute na pasta extraída:

```powershell
& '.\Iniciar Reposicao de Aula.cmd' --nova-senha-professor
```

Se estiver usando uma demonstração com `--dados`, repita esse argumento para
gerar a senha da sessão certa. A troca mantém as enquetes e o histórico.

## Como demonstrar

1. Entre na aba **Professor** com a senha e clique em **Criar enquete**. Cadastre horários em 02/12/2026 às 18h, 03/12/2026 às 19h e
   04/12/2026 às 20h.
2. Na aba **Aluno**, use `A01` nas opções 1 e 2, `B02` na opção 1 e `C03` na opção 2.
   O placar mostra três respostas e as indicações 2, 2 e 0.
3. Use `a01` de novo, marcando só a opção 2. Ela fica na liderança com duas
   indicações. O total de respostas continua três.
4. Na aba **Professor**, confira **Respostas por código** e encerre a votação.
5. Clique em **Nova enquete**. A anterior passa para **Histórico**. Abra o
   histórico para conferir o placar e os horários informados por cada código.

Para começar com o empate pronto, siga o [roteiro da apresentação](<docs/Reposição de Aula - Roteiro de Apresentação.pdf>).
Ele usa uma cópia de `demo/exemplo.properties`.

## Onde está o código

- [Enquete.java](src/reposicao/Enquete.java) valida horários e respostas.
  Cada código é uma chave no mapa. O placar percorre os horários e usa um
  Stream para contar as respostas que marcam cada um.
- [EnqueteService.java](src/reposicao/EnqueteService.java) coordena as ações.
  Ele usa a interface [RepositorioEnquete](src/reposicao/RepositorioEnquete.java).
  [EnqueteArquivo.java](src/reposicao/EnqueteArquivo.java) grava a enquete atual e o
  histórico em um arquivo `.properties`.
- [Aplicacao.java](src/reposicao/Aplicacao.java) recebe os formulários.
  [Tela.java](src/reposicao/Tela.java) monta as páginas.
  [AcessoProfessor.java](src/reposicao/AcessoProfessor.java) verifica a senha e a sessão.
  [scripts/iniciar.ps1](scripts/iniciar.ps1) prepara o Java no Windows.

O sistema é local. A senha protege a área Professor no navegador. Use códigos
fictícios e uma instância por arquivo de dados. A hipótese
sobre como a turma organiza reposições ainda precisa de validação com colegas.

## Como compilar e testar

O JDK fornece `javac` e `jar`. Os comandos abaixo executam `EnqueteTest` e
recriam o JAR.

- Windows: `powershell -NoProfile -ExecutionPolicy Bypass -File ./compilar.ps1`
- Linux ou macOS: `sh ./compilar.sh`

## Material da entrega

- Relatório acadêmico em [HTML](<docs/Reposição de Aula - Relatório Acadêmico.html>),
  [PDF](<docs/Reposição de Aula - Relatório Acadêmico.pdf>) e
  [Word](<docs/Reposição de Aula - Relatório Acadêmico.docx>).
- [Slides da apresentação](<docs/Reposição de Aula - Apresentação POOALAB.pdf>),
  [roteiro da apresentação](<docs/Reposição de Aula - Roteiro de Apresentação.pdf>)
  e [demonstração de contingência](demo/roteiro-visual.pdf).
- O relatório inclui o roteiro de validação e a declaração de uso de IA e
  componentes de terceiros. A validação com colegas e a ciência do grupo estão
  pendentes.

A versão de entrega inclui acesso por senha na aba Professor e mantém o
histórico de enquetes no arquivo de dados. O ZIP do Drive acompanha o código
da branch `main`. Para atualizar a entrega, recompile o JAR e monte o ZIP
sem os arquivos gerados em `dados/`, `.java/` ou `build/`.
