# Freya

App Android (Kotlin + Jetpack Compose + Room) de finanças pessoais via Open Finance (Pluggy / Meu Pluggy), desenvolvido por Japaneixxx.
100% local: sem servidor próprio. O app chama api.pluggy.ai direto do aparelho.

## Rodar
1. Abra a pasta no Android Studio (Koala ou mais novo, JDK 17) e aguarde o Gradle sync.
2. Run no aparelho/emulador (minSdk 26).

## Credenciais Pluggy (plano pessoal gratuito)
O app abre com um tutorial passo a passo (botões que abrem os sites, campos com "Colar" e teste da conexão antes de salvar). Resumo:
1. Crie conta e conecte seus bancos em https://meu.pluggy.ai
2. No https://dashboard.pluggy.ai crie uma Application, abra a tela Demo, toque em Connect Account e escolha o conector MeuPluggy.
3. Copie Client ID, Client Secret (mostrado uma vez) e o Item ID de cada banco (máx. 5).
4. Cole no app. O app autentica no Pluggy e valida cada Item ID antes de salvar; as credenciais ficam criptografadas (EncryptedSharedPreferences).

## Como funciona a sincronização
- Dispara em MainActivity.onStart (sempre que o app abre/volta), com intervalo mínimo de 15 min.
- Sem WorkManager/serviço: nada roda em segundo plano.
- Meu Pluggy atualiza os bancos ~1x/24h; o app só lê o que já está lá.

## Assinatura e publicação

As Releases são geradas pelo GitHub Actions com uma chave Release permanente. Nunca
adicione o keystore, senhas ou `local.properties` ao Git.

Para criar um keystore novo localmente, use o alias escolhido:

```bash
keytool -genkeypair -v -keystore meufin-release.keystore -alias freya-release \
  -keyalg RSA -keysize 2048 -validity 10000
```

Converta o arquivo para Base64 e cadastre estes GitHub Secrets no repositório:

- `ANDROID_KEYSTORE_BASE64`: conteúdo Base64 de `meufin-release.keystore`.
- `ANDROID_KEYSTORE_PASSWORD`: senha do keystore.
- `ANDROID_KEY_ALIAS`: `freya-release`.
- `ANDROID_KEY_PASSWORD`: senha da chave.

No PowerShell, o Base64 pode ser gerado com:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes(".\meufin-release.keystore"))
```

O workflow publica `app-release.apk` assinado sempre que houver push em `main` ou
`master`. Preserve o keystore e as senhas: perder essa chave impede atualizações
sobre instalações existentes.

## Categorias
Pluggy (se vier) -> palavras-chave locais (Categorizer.kt) -> regra aprendida ao tocar numa transação.
"Pagamento de fatura" e "Transferência própria" não entram em receitas/despesas.

## Funções (v1.1)
- Visual nórdico (claro/escuro): cartões sem sombra, paleta fiorde/sálvia/areia.
- Início: saldo, receitas/despesas, insights, rosca por categoria, ritmo de gastos vs mês anterior, orçamentos, últimos 6 meses, maiores gastos.
- Extrato: busca, filtros (período, tipo, categoria), detalhe com observação, "ignorar nos totais", lançamentos manuais (dinheiro).
- Planejar: orçamentos por categoria (com sugestão automática), metas de poupança, cobranças recorrentes detectadas.
- Mais: contas, modo privacidade (ocultar valores), exportar CSV, credenciais.
- Atualizar da v1.0 limpa o banco local uma vez (tudo é re-sincronizado); credenciais são mantidas.

## v1.2
- Bloqueio por biometria/PIN (também esconde o app nos recentes e o widget).
- Alertas locais: orçamento a 80%/100%, contas a pagar e fatura de cartão a vencer (checagem após cada sincronização + 1x/dia às 9h via WorkManager, sem rede).
- Widget de saldo na tela inicial.
- Contas a pagar, cartões (limite/fatura/vencimento), parcelas detectadas, evolução do saldo, previsão de fim de mês.
- Filtro por conta no extrato, tema claro/escuro/sistema, regras de categoria gerenciáveis.
- Backup/restauração em JSON, compartilhar resumo do mês, ícone do app.
- Banco migra de v2 para v3 sem perder dados.

## v1.3.0
- Redesign nórdico completo: Início com topo em gradiente e montanhas, avatares de categoria, carrossel de insights e calendário de gastos.
- Extrato novo: cartões por dia com total do dia, chips de mês/filtros, ordenação (recentes, maiores gastos, maiores entradas), resumo do período, seleção em lote (segure um lançamento) para recategorizar ou ignorar, e detalhe em painel inferior com estatísticas do estabelecimento.
- Categorias personalizadas (Mais) disponíveis em filtros, orçamentos e lançamentos.
- Planejar: "quanto posso gastar por dia" até o fim do mês e avatares nas recorrências.
- Insight do dia da semana com maior gasto.

## Atualizações
<<<<<<< HEAD
Ao abrir o app, ele consulta a Release mais recente do repositório **Japaneixxx/FreyaVault** (`Updater.kt`). Se o repositório ainda não tiver Release, nada é exibido. Quando houver
=======
Ao abrir o app, ele consulta a Release mais recente deste repositório. Quando houver
>>>>>>> ca3862f5ab875d29627bcebfbf222fb3e17f0ba8
um APK novo, o app oferece o download e abre o instalador oficial do Android para
confirmar a atualização. A instalação silenciosa não é permitida para apps comuns;
use a mesma chave de assinatura das Releases anteriores. Essa verificação fica
habilitada por padrão e pode ser desligada em **Mais > Segurança e avisos**.

<<<<<<< HEAD
## v1.5.0
- **Nomes limpos no extrato:** o app remove o texto repetido do banco ("Compra no débito", "Transferência enviada pelo Pix", CPF mascarado, agência/conta) e mostra só o estabelecimento ou a pessoa, com sinalizações (💳 Débito, ⚡ Pix enviado, 🧾 Boleto, 🧩 parcela 3/10, 🔁 Débito automático...). A descrição original continua no detalhe e na busca (`data/Sanitizer.kt`). As regras de categoria passam a usar o nome limpo (regras antigas continuam valendo).
- **Cobranças para revisar:** detecta cobranças duplicadas (mesmo nome e valor em até 3 dias) e assinaturas/contas fixas que ficaram mais caras. Aparece no Início, em Planejar > Cobranças e em notificação (se os alertas estiverem ligados).
- **Patrimônio:** nova aba com patrimônio líquido (contas + investimentos + bens − dívidas − faturas), evolução diária, composição, carteira de investimentos vinda do Pluggy (`GET /investments`) e cadastro manual de bens e dívidas.
- **Envelopes:** em Planejar > Orçamentos, o "modo envelope" acumula a sobra de cada mês para o seguinte (estouros não acumulam).
- **Seletor de categorias em grade:** busca, "mais usadas" e criação de categoria na hora, em todo o app.
- **Tutorial de conexão** refeito: passos com links, ajuda para erros comuns e teste da conexão antes de salvar.
- Banco migra de v3 para v4 sem perder dados. Backup agora inclui bens, dívidas e envelopes.

=======
>>>>>>> ca3862f5ab875d29627bcebfbf222fb3e17f0ba8
