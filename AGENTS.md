# Diretrizes do projeto

## Contexto

- Projeto Android em Kotlin com Jetpack Compose, Room e KSP.
- O namespace e o application ID são `app.meufin`.
- O app é local e consulta diretamente a API do Pluggy; não crie um servidor próprio sem solicitação explícita.

## Estrutura

- `app/src/main/java/app/meufin/data/`: persistência, configurações, categorização, integração Pluggy e backup.
- `app/src/main/java/app/meufin/ui/`: telas Compose, ViewModels, tema e formatação.
- `app/src/main/res/`: recursos Android.
- `.github/workflows/build.yml`: build e publicação automática do APK.

## Regras de alteração

- Preserve Kotlin oficial, tipagem explícita quando necessário e coroutines nas APIs suspensas.
- Reutilize DAOs, repositórios, ViewModels e componentes Compose existentes antes de criar duplicações.
- Não inclua credenciais Pluggy, `local.properties` ou outros segredos no controle de versão.
- Não edite arquivos gerados em `build/`; altere a fonte correspondente.
- Ao alterar funcionalidades de dados, preserve o armazenamento local e trate falhas de sincronização de forma explícita.
- Atualize o `README.md` quando mudar o fluxo de configuração, sincronização ou uso do app.

## Versão e releases

- Sempre que alterar o app ou preparar uma nova entrega, lembre explicitamente o agente de atualizar a versão antes de finalizar.
- Antes de concluir qualquer mudança que será publicada, incremente `versionCode` e atualize `versionName` no formato `Major.Minor.Bug` (por exemplo, `1.2.0`).
- Atualize `versionCode` e `versionName` em `app/build.gradle.kts` juntos; o `versionCode` deve sempre aumentar.
- O workflow valida `versionName` e publica a Release com a tag `v<Major>.<Minor>.<Bug>`.
- Use `push` para `main` ou `master` somente quando quiser disparar a publicação automática do APK.
- Releases devem usar o keystore permanente configurado pelos secrets do GitHub; nunca crie ou versione chaves de assinatura no repositório.
- Não troque `assembleRelease` por `assembleDebug` no workflow de publicação, pois isso pode gerar APKs incompatíveis para atualização.

## Build e validação

- Compile o APK debug com `gradle assembleDebug --no-daemon` a partir da raiz, quando o Gradle estiver disponível.
- O APK é gerado em `app/build/outputs/apk/debug/app-debug.apk`.
- Para mudanças Kotlin, execute o build antes de concluir e corrija erros de compilação; avisos de APIs deprecated devem ser avaliados sem mascarar erros.
- Não altere `local.properties`; ele é específico da máquina e ignorado pelo Git.
