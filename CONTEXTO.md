# Contexto do projeto

## Estado em 23 de novembro de 2026

Projeto acadêmico da disciplina POOALAB 2026.2. A base veio do ZIP original,
que não continha o histórico Git. O código-fonte é a referência para as regras
da enquete. O JAR é o executável recompilado para a entrega. O Word é a fonte do
relatório acadêmico. O PDF foi exportado a partir dele e o HTML reúne esse conteúdo e instruções de demonstração.

- Interface simplificada com marca oficial da UCSal servida pelo JAR. Veja
  [DESIGN.md](DESIGN.md) para as decisões visuais.
- O iniciador do Windows reutiliza Java 8+ ou baixa Temurin 21 com SHA-256
  (x64 ou ARM64). Usa a porta local livre escolhida pelo sistema e abre o
  navegador. O modo de terminal via JAR continua disponível.
- Revisão em dois ciclos no Chrome, em desktop e celular emulado. Conferidos
  criação, estado vazio, foco, erro de formulário, logo, confirmação, empate,
  mudança de resposta, encerramento e persistência após reinício.
- Testes automatizados do domínio e da persistência rodaram com Temurin 21.
  Classe principal compilada para Java 8. Primeira execução do iniciador,
  reabertura e Java existente verificadas em Windows x64. ARM64 não foi
  executado neste computador.
- O guia e a contingência acompanham a interface revisada. A versão HTML do
  relatório foi conferida no Chrome offline em desktop e celular, com
  navegação por teclado e impressão em A4. A declaração de IA
  e de recursos de terceiros está em [docs](docs/declaracao-ia-terceiros.md).
- Código e entregáveis de referência têm como destino o repositório
  [Ar3sMyth/reposicao-de-aula-pooalab](https://github.com/Ar3sMyth/reposicao-de-aula-pooalab),
  criado com visibilidade privada. A pasta de entrega é
  [POOALAB 2026.2](https://drive.google.com/drive/folders/19F2ZUCKcCNuO0Bf4oNsTSc96_NxrdBMp).

- Nomes padronizados no Drive e no ZIP. O pacote se chama
  `Reposição de Aula - POOALAB 2026.2.zip`. Os documentos se identificam
  como Roteiro de Apresentação ou Relatório Acadêmico.

## Retomada

Abra o guia do grupo e ensaie com uma cópia de `demo/exemplo.properties`.
Para encaminhar o projeto ao professor, use o ZIP extraível do Drive. Se ele
precisar revisar o repositório privado, atribua acesso pela conta do GitHub.
A equipe ainda precisa revisar a declaração, ensaiar na estação da apresentação
e registrar evidências reais de validação, se forem coletadas.

## Revisão para a apresentação

O README agora concentra as instruções de execução, os quatro passos da
demonstração e a função de cada arquivo Java. A contagem em `Enquete.resultado()`
usa um laço para percorrer os horários e um Stream para contar as respostas.
A ordenação ocorre em seguida. A leitura e a gravação de horários usam laços,
com o mesmo formato dos arquivos de dados anteriores. O roteiro apresenta o
trecho de código por nome de método. O iniciador do Windows continua na raiz,
com o PowerShell de instalação em `scripts/`.

## Área Professor e histórico

A área Professor permite criar e encerrar enquetes e ver os horários de cada
código. A área Aluno recebe as respostas e Resultado exibe só os totais.
As áreas são locais e sem login. Na demonstração, use códigos fictícios.

Há uma enquete aberta por vez. Quando outra é criada, a anterior é arquivada
com os horários, as respostas e o estado encerrado. A enquete atual e o
histórico são gravados juntos no mesmo arquivo, com o mesmo formato anterior
para a enquete atual. Sessões antigas abrem sem histórico. Formulários de
outra enquete são recusados para evitar voto ou encerramento no lugar errado.

Os testes Java verificaram erro de gravação, criação de outra enquete, mais
de uma entrada no histórico e recuperação após reiniciar. O fluxo no Chrome
foi conferido em desktop e celular emulado. README, relatório e roteiro de
apresentação foram ajustados para a nova navegação.
