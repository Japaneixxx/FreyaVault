# Freya

Aplicativo Android de finanças pessoais, escrito em Kotlin com Jetpack Compose e Room.
O Freya usa o Open Finance do Pluggy / Meu Pluggy para consultar contas, cartões e
investimentos diretamente no aparelho.

> **Privacidade:** não existe servidor próprio neste projeto. As credenciais, dados
> locais e preferências ficam no dispositivo, em armazenamento criptografado. O app
> conversa diretamente com a API do Pluggy.

## Recursos

- Visão geral de saldo, receitas, despesas, categorias e evolução mensal.
- Extrato com busca, filtros, recategorização, observações e lançamentos manuais.
- Categorias personalizadas e regras aprendidas a partir das alterações do usuário.
- Planejamento com orçamentos, modo envelope, metas, compromissos, financiamentos,
  receitas previstas e agenda de 45 dias.
- Aba Patrimônio com investimentos do Pluggy, bens, dívidas e evolução do patrimônio.
- Alertas locais de orçamento, contas a pagar, faturas e saldo previsto.
- Widget de saldo, tema claro/escuro/sistema e modo para ocultar valores.
- Exportação CSV, resumo mensal compartilhável e backup/restauração em JSON.
- Bloqueio por biometria e PIN próprio de 4 dígitos.
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

O PIN não é salvo em texto puro: o app guarda apenas um hash com salt no
`EncryptedSharedPreferences`, protegido pelo Android Keystore.

O backup em JSON inclui dados locais que não vêm diretamente do banco, como
orçamentos, metas, contas a pagar, regras, notas e lançamentos manuais. Guarde o
arquivo de backup em local seguro.

## Sincronização e atualizações

- A sincronização é iniciada ao abrir ou retornar ao app, respeitando o intervalo
  mínimo entre consultas.
- Não há serviço próprio nem servidor intermediário.
- O app pode consultar a Release mais recente do repositório
  [Japaneixxx/FreyaVault](https://github.com/Japaneixxx/FreyaVault).
- Se houver um APK novo, o Android solicitará confirmação para instalar.
- A opção fica em **Mais > Segurança e avisos** e vem ativada por padrão.

O histórico de versões fica em
`app/src/main/java/app/meufin/ui/Changelog.kt`. Cada nova versão deve entrar no
topo de `releaseNotes`, com a mesma versão definida em `app/build.gradle.kts`.
As versões `1.x` atuais usam o codinome **Fimbulwinter**.

## Build e publicação

O workflow `.github/workflows/build.yml` gera e publica o APK assinado quando há
push em `main` ou `master`, ou quando é executado manualmente.

Antes de publicar:

1. Incremente `versionCode`.
2. Atualize `versionName` no formato `Major.Minor.Bug`.
3. Adicione a entrada correspondente no topo de `releaseNotes`.
4. Execute o build de debug e corrija erros de compilação.

O workflow valida o formato da versão, executa `assembleRelease` e publica a tag
`v<versionName>`.

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
app/src/main/java/app/meufin/
├── data/   persistência, Room, Pluggy, configurações e backup
└── ui/     telas Compose, ViewModels, tema e changelog
```

O namespace e o application ID são `app.meufin`.
