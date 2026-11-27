# Freya

Aplicativo Android de finanças pessoais, escrito em Kotlin com Jetpack Compose e Room.
O Freya usa o Open Finance do Pluggy / Meu Pluggy para consultar contas, cartões e
investimentos diretamente no aparelho.

> **Privacidade:** não existe servidor próprio neste projeto. As credenciais, dados
> locais e preferências ficam no dispositivo, em armazenamento criptografado. O app
> conversa diretamente com a API do Pluggy e, no simulador de investimentos, com a API
> pública do Banco Central (CDI e IPCA), que não usa chave nem envia dados seus.

## Recursos

- Visão geral de saldo, receitas, despesas, categorias e evolução mensal.
- Extrato com busca, filtros, recategorização, observações e lançamentos manuais.
- Categorias personalizadas e regras aprendidas a partir das alterações do usuário.
- Planejamento com orçamentos, modo envelope, metas, compromissos, financiamentos,
  receitas previstas e agenda de 45 dias.
- Frescor dos bancos: em Mais, o app mostra quando o Meu Pluggy leu os bancos pela última vez e tem um atalho
  para abrir o Meu Pluggy; ao voltar, o Freya sincroniza sem esperar o intervalo mínimo.
- Contas e cartões podem ser renomeados dentro do app (Mais > Contas ou Planejar > Cartões); o nome do banco
  não muda e o apelido sobrevive às sincronizações e entra no backup.
- Recebimentos futuros podem ser recorrentes ou avulsos, para entradas únicas como freelances e bônus.
- Aba Patrimônio com investimentos do Pluggy, bens, dívidas e evolução do patrimônio.
- Simulador de investimentos na aba Patrimônio: juros compostos com aportes, rentabilidade em % a.a., % a.m.,
  % do CDI ou IPCA + taxa (CDI e IPCA digitados ou buscados na API pública do Banco Central), IR regressivo,
  meta reversa e comparação de cenários.
- Alertas locais de orçamento, contas a pagar, faturas e saldo previsto.
- Agenda do celular: em Mais > Agenda do celular, o Freya cria lembretes com o vencimento das faturas (e,
  opcionalmente, das contas a pagar) num calendário local "Freya", que não sobe para a conta Google. Pede as
  permissões de leitura e escrita da agenda; se o aparelho não deixar criar o calendário local, usa um calendário
  editável existente e mostra qual na tela. O valor só entra no evento se você ligar a opção. Desligar remove os
  lembretes futuros.
- Personalização: cor de destaque (paletas ou cor livre, com ajuste automático de contraste), tamanho do texto
  (80% a 140%), densidade da interface, aba inicial e ordem das abas, atalhos do ícone do app (até 4) e widget de
  saldo configurável por widget (conteúdo, estilo e destino do toque). As escolhas ficam só neste aparelho.
- Fatura por ciclo: em Planejar > Cartões, informe o dia de fechamento de cada cartão (ou use a data que o Pluggy
  envia em `balanceCloseDate`) para ver o ciclo atual e os anteriores, com total, categorias e lançamentos. O total
  vem dos lançamentos do extrato (sem os pagamentos de fatura) e pode diferir do valor oficial; compras no dia do
  fechamento contam no ciclo seguinte. O dia de fechamento informado fica só neste aparelho e não entra no backup.
- Início personalizável: o botão Personalizar início, no fim da tela Início, liga, desliga e reordena os blocos.
- Tutorial interativo: aparece no primeiro acesso, depois de conectar o Pluggy, e pode ser revisto em Mais > Ajuda.
- Calendário de gastos na Visão geral: cada dia fica vermelho (saiu mais) ou verde-azulado (entrou mais) conforme
  o saldo do dia; tocar no dia mostra entradas, saídas e lançamentos.
- Widget de saldo, tema claro/escuro/sistema e modo para ocultar valores.
- Exportação CSV, resumo mensal compartilhável e backup/restauração em JSON, com senha opcional (AES-256).
- Dinheiro físico: em Mais > Dinheiro físico, crie contas de dinheiro vivo; o saldo acompanha os lançamentos
  feitos nelas (Extrato > + > "Onde foi"), pode ser ajustado e entra no total e no patrimônio.
- Transferências entre as suas próprias contas (Pix/TED de mesmo valor em contas diferentes, até 1 dia de
  diferença) são reconhecidas e não contam como receita nem despesa; uma categoria escolhida à mão vence.
- Bloqueio por biometria e PIN próprio de 4 dígitos, com espera após tentativas erradas.
- Atualização pelo próprio app quando existe uma Release publicada no GitHub.

## Instalação para desenvolvimento

Requisitos:

- Android Studio Koala ou mais recente.
- JDK 17.
- Android SDK 34.
- Dispositivo ou emulador com Android 8.0 (API 26) ou mais recente.

Abra a raiz do repositório no Android Studio, aguarde a sincronização do Gradle e
execute o app em um dispositivo ou emulador.

Para gerar o APK de debug pela linha de comando:

```bash
gradle assembleDebug --no-daemon
```

O arquivo será gerado em:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Configuração do Pluggy

O app exibe um tutorial na primeira abertura. O fluxo resumido é:

1. Crie uma conta e conecte seus bancos em [Meu Pluggy](https://meu.pluggy.ai).
2. No [Dashboard do Pluggy](https://dashboard.pluggy.ai), crie uma Application.
3. Na Demo, use **Connect Account** e escolha o conector **MeuPluggy**.
4. Copie o **Client ID**, o **Client Secret** e o **Item ID** de cada conexão.
5. Cole os dados no Freya e toque em salvar e sincronizar.

O app autentica no Pluggy e valida cada Item ID antes de salvar as credenciais.
O plano pessoal aceita até cinco conexões. O Meu Pluggy normalmente atualiza os
bancos cerca de uma vez por dia; o Freya consulta o que já estiver disponível.
Ao abrir ou retornar ao app, uma nova sincronização só é iniciada se tiver passado
o intervalo mínimo configurado.

### Erros comuns

- **401/403:** confira o Client ID e o Client Secret.
- **404:** confira o Item ID da conexão; ele não é o Client ID.
- **Sandbox ou contratação:** volte ao fluxo e escolha o conector MeuPluggy, não o
  banco diretamente.
- **Sem dados:** aguarde a conexão aparecer como atualizada no Meu Pluggy e tente
  sincronizar novamente.

## Segurança e dados locais

Em **Mais > Segurança e avisos**, é possível:

- ativar o bloqueio por biometria;
- configurar, alterar ou remover um PIN próprio de quatro dígitos;
- ativar o modo para ocultar valores;
- controlar a verificação automática de atualizações.

O PIN não é salvo em texto puro: o app guarda apenas um hash PBKDF2 com salt no
`EncryptedSharedPreferences`, protegido pelo Android Keystore. Depois de 5 erros seguidos o PIN
trava por 30 s, e o tempo dobra a cada erro (até 15 min). PINs da versão anterior (SHA-256) são
migrados no primeiro desbloqueio. Com valores ocultos ou bloqueio ligado, o app usa `FLAG_SECURE`
(sem capturas de tela e sem miniatura nos apps recentes).

O backup em JSON inclui dados locais que não vêm diretamente do banco: orçamentos, metas,
contas a pagar, regras, notas, lançamentos manuais, contas de dinheiro, histórico dos gráficos,
categorias próprias e apelidos de contas. Ele pode ser protegido com senha (AES-256-GCM, chave
derivada por PBKDF2); sem a senha não há como recuperá-lo. Ao restaurar, **Mesclar** só adiciona o
que não existe e **Substituir** troca o que o backup contém; a restauração roda numa transação (se
falhar, nada muda). O app lembra de fazer backup a cada 30 dias.

## Sincronização e atualizações

- A sincronização é iniciada ao abrir ou retornar ao app, respeitando o intervalo
  mínimo entre consultas.
- Cada banco é sincronizado de forma independente e gravado numa transação: se um falhar, os
  outros continuam e o aviso informa qual foi. Falhas temporárias do Pluggy (429/5xx) têm novas
  tentativas automáticas.
- Não há serviço próprio nem servidor intermediário.
- O app pode consultar a Release mais recente do repositório
  [Japaneixxx/FreyaVault](https://github.com/Japaneixxx/FreyaVault).
- Se houver um APK novo, o Android solicitará confirmação para instalar. O APK é conferido pelo
  SHA-256 publicado na Release (`app-release.apk.sha256`).
- A opção fica em **Mais > Segurança e avisos** e vem ativada por padrão.
- Versões **beta** (o último número da versão tem 2 dígitos, como `1.8.41` e `1.8.51`;
  com 1 dígito, como `1.8.5` e `1.9.0`, é estável) são publicadas como pré-lançamento no GitHub e só aparecem quando você
  toca em **Verificar atualizações**; a verificação automática ao abrir oferece só as estáveis.

O histórico de versões fica em
`app/src/main/java/com/japaneixxx/freya/ui/Changelog.kt`. Cada nova versão deve entrar no
topo de `releaseNotes`, com a mesma versão definida em `app/build.gradle.kts`.
As versões `1.x` atuais usam o codinome **Fimbulwinter**.

## Testes automatizados

Testes unitários (JUnit, sem emulador) em `app/src/test`: categorização, Sanitizer, transferências
entre contas próprias, backup (exportar, restaurar, mesclar/substituir, versão futura), criptografia do
backup, PIN, regras de versão/beta, totais em moeda e as regras de Insights (envelopes, contas a pagar,
parcelas, receitas). Rode com `gradle testDebugUnitTest`; o workflow executa a mesma suíte antes de
gerar a Release.

## Build e publicação

O workflow `.github/workflows/build.yml` gera e publica o APK assinado quando há
push em `main` ou `master`, ou quando é executado manualmente.

Antes de publicar:

1. Incremente `versionCode`.
2. Atualize `versionName` no formato `Major.Minor.Bug`.
3. Adicione a entrada correspondente no topo de `releaseNotes`.
4. Execute o build de debug e corrija erros de compilação.

O workflow valida o formato da versão, executa `assembleRelease` e publica a tag
`v<versionName>`, marcando como pré-lançamento as versões beta.

O repositório precisa dos seguintes GitHub Secrets:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Nunca versionar:

- keystores;
- senhas ou credenciais do Pluggy;
- `local.properties`;
- APKs gerados em `build/`.

Perder o keystore de release impede atualizações sobre instalações existentes.

## Estrutura principal

```text
app/src/main/java/com/japaneixxx/freya/
├── data/   persistência, Room, Pluggy, configurações e backup
└── ui/     telas Compose, ViewModels, tema e changelog
```

O namespace e o application ID são `com.japaneixxx.freya`. Esta mudança inaugura um novo identificador
do aplicativo Android; instalações da versão anterior precisam ser migradas por backup e restauração.
