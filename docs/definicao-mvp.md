# Definição do MVP

- **Público:** turma que precisa informar disponibilidade e quem organiza uma reposição.
- **Resultado:** uma contagem por opção de data e hora, com indicação de liderança e empate.
- **Fluxo:** cadastrar duas a quatro opções, receber respostas por código fictício,
  visualizar o placar, conferir as respostas por código, encerrar a votação
  e criar outra mantendo a anterior no histórico.
- **Pronto quando:** horários repetidos são rejeitados, cada código tem uma resposta,
  selecionar mais de um horário conta uma indicação em cada, empate fica visível e
  votos, encerramento e histórico sobrevivem a um reinício.
- **Fora do escopo:** autenticação, votação remota, gestão de turmas, notificações e
  confirmação institucional da reposição.
- **Risco principal:** a consulta pode não resolver o ponto mais difícil do processo
  real, que talvez seja confirmar professor, sala ou carga horária.
- **Teste principal:** uma pessoa recebe a tarefa de registrar dois horários, atualizar
  sua resposta e dizer, olhando para o placar, se há empate ou liderança.
- **Limitações:** uma enquete aberta por vez e códigos fictícios editáveis em uma estação
  sem login. A decisão final continua com a turma e o docente.
