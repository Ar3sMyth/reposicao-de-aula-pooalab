# Reposição de Aula

Aplicação local para uma turma comparar horários de reposição. Cada aluno
marca todas as opções em que pode comparecer. O professor acompanha o
placar e consulta as respostas por código.

POOALAB 2026.2 · ADS-NOT-PIT-4S-T1 · UCSal
Professor Mario Jorge Pereira

[Repositório](https://github.com/Ar3sMyth/reposicao-de-aula-pooalab)

Andrey Soares dos Santos, Sophia Desiderio e Luis Fernando Alves.

## Como executar

No Windows, extraia o ZIP e dê dois cliques em **Iniciar Reposicao de Aula.cmd**.
O iniciador usa Java 8 ou superior. Se faltar Java, baixa o Temurin 21 para a
pasta do projeto em Windows x64 ou ARM64. Só esse primeiro download requer
internet. Mantenha a janela do iniciador aberta e use `Ctrl+C` para parar.

Com Java 8 ou superior, o JAR também pode ser aberto pelo terminal:

```sh
java -jar reposicao-de-aula.jar
```

Abra [127.0.0.1:8080](http://127.0.0.1:8080). Se necessário, troque a porta
com `--porta=8081`. O JAR vem no ZIP. Quem baixar o código pelo GitHub pode
gerá-lo com os comandos da seção de testes.

Na primeira abertura, o iniciador mostra a senha de seis dígitos da aba
**Professor**. Guarde-a com o professor e use **Sair** antes de passar o computador.
Para gerar outra senha, feche a aplicação
e execute o comando abaixo. Se usa `--dados`, repita o caminho da sessão.

```powershell
& '.\Iniciar Reposicao de Aula.cmd' --nova-senha-professor
```

## Testes e demonstração

Com um JDK 8 ou superior no PATH, os comandos abaixo compilam, executam
`EnqueteTest` e geram o JAR. A versão da entrega foi conferida com Temurin 21
no Windows e no Chrome em desktop e celular emulado.

- Windows: `powershell -NoProfile -ExecutionPolicy Bypass -File ./compilar.ps1`
- Linux ou macOS: `sh ./compilar.sh`

1. Entre em **Professor** e crie uma enquete com 02/12/2026 às 18h, 03/12/2026
   às 19h e 04/12/2026 às 20h.
2. Na aba **Aluno**, registre `A01` nas opções 1 e 2, `B02` na opção 1 e
   `C03` na opção 2. O resultado fica em 2, 2 e 0.
3. Reenvie `a01` marcando só a opção 2. Ela fica em primeiro lugar com duas
   indicações e o total continua em três respostas.
4. Na aba **Professor**, confira as escolhas por código e encerre. Crie outra
   enquete e abra a primeira no **Histórico**.

Para iniciar direto pelo empate, use o exemplo fictício no PowerShell:

```powershell
New-Item -ItemType Directory -Force dados | Out-Null
$sessao = "dados/ensaio-$(Get-Date -Format yyyyMMdd-HHmmss).properties"
Copy-Item demo/exemplo.properties $sessao
& '.\Iniciar Reposicao de Aula.cmd' "--dados=$sessao"
```

## Anexo A · Ficha do problema

- **Público e situação:** estudantes e docentes da UCSal quando precisam
  combinar a reposição de uma aula.
- **Problema e impacto esperado:** reunir disponibilidades e contar quantas
  pessoas podem em cada horário pode dar trabalho se as respostas se espalham.
- **Alternativa a investigar:** combinar pelo grupo da turma e contar
  respostas manualmente. A equipe ainda precisa confirmar como isso acontece.
- **Hipótese crítica:** a contagem em um único lugar facilita comparar
  horários. Para verificar, observar como uma turma já marcou uma reposição
  e testar se os colegas conseguem responder e interpretar o placar.
- **Recorte:** reunir as escolhas e mostrar a contagem. A decisão final sobre
  data e sala continua com a turma e o professor.

## Anexo B · Registro de validação

| Data e perfil | Hipótese ou fluxo verificado | Evidência observada | Decisão |
| --- | --- | --- | --- |
| 23/11/2026 · teste técnico com dados fictícios | Reenviar o código substitui a resposta anterior e o histórico continua disponível ao reiniciar. | A edição de A01 desempatou o placar sem aumentar o total de respostas. O histórico foi recuperado após reiniciar. | Manter a edição por código e o histórico no mesmo arquivo local. |
| 23/11/2026 · teste técnico do ZIP no Windows x64 | A aplicação inicia em uma pasta recém-extraída e separa as permissões de aluno e professor. | O iniciador baixou o Java na primeira abertura e o reutilizou depois. Os testes conferiram voto por código, bloqueio de ações do professor sem senha, saída e senha preservada após reiniciar. | Manter o ZIP com o iniciador, os dados de exemplo e a senha local do professor. |
| Relato confirmado em 23/11/2026 · P1 e P2, perfil não informado | Experimentar a enquete e conhecer a área Professor. | Segundo a equipe, os dois participantes testaram a enquete e consultaram a área Professor. O relato disponível identifica as etapas, sem observações individuais. | Registrar os dois testes iniciais. Nenhuma mudança foi atribuída a esse relato. |

**Próximo teste com o público:** observar como uma turma compara horários em
uma reposição real. Registrar o que ajuda ou dificulta a escolha, com perfil
sem identificação. O teste inicial confirma as etapas percorridas. A hipótese
sobre a rotina da turma ainda está em aberto.

## Anexo C · Definição do MVP

- **Usuários e resultado:** professor abre e encerra a enquete. Alunos
  informam horários. A turma compara as contagens por opção.
- **Fluxo:** cadastrar de duas a quatro opções, responder por código, conferir
  o placar, encerrar e recuperar as enquetes anteriores.
- **Critérios de pronto:** código repetido substitui a resposta, empate fica
  visível, ações do professor exigem senha e o histórico sobrevive ao reinício.
- **Tecnologia:** Java com `HttpServer`, HTML e CSS para rodar no navegador
  sem dependências externas. Um mapa agrupa as respostas, um Stream conta as
  escolhas e o Repository separa a regra da gravação em `.properties`.
- **Limites e próximo teste:** uma enquete aberta por vez, na mesma estação,
  sem identificação de alunos ou votação remota. A senha protege a área do
  professor no navegador. O próximo passo é observar o uso com colegas.

## Anexo D · Roteiro do pitch

Apresentar a dificuldade de comparar horários e relatar que dois participantes
testaram a enquete e consultaram a área Professor. Em seguida, abrir o exemplo, alterar A01, entrar como professor, encerrar e
mostrar o histórico. Fechar explicando o recorte local e o teste com colegas
que ainda precisa ser feito.

- [Pitch em PDF](docs/pitch.pdf)
- [Roteiro visual de contingência](demo/roteiro-visual.pdf)

## Anexo E · Declaração de uso de IA e de terceiros

O Codex apoiou a implementação, os testes e a documentação. O
[Anexo E](docs/declaracao-ia-terceiros.pdf) reúne as fontes, as licenças e o registro de ciência da equipe.

## Material complementar

[Relatório acadêmico em PDF](<docs/Reposição de Aula - Relatório Acadêmico.pdf>).
