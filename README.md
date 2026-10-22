# MaiaSupport

Aplicativo Android independente que oferece inferência do engine Maia-3 a outros apps Android por meio de um serviço vinculado AIDL. Não é um app de xadrez e não é afiliado ao University of Toronto CSSLab, ao projeto Maia Chess, ao Mobile Maia ou ao MoveAPiece.

## Como funciona

Na primeira execução, a pessoa escolhe entre duas versões e confirma o tamanho antes de iniciar o download. O padrão recomendado é Maia-3 5M (**21 MB**): arquivo menor, menos armazenamento e memória. A opção Maia-3 79M (**316 MB; aprox. 302 MiB**) é muito maior e exige mais recursos, mas pode oferecer previsões mais precisas. Os tamanhos aparecem na tela e na caixa de confirmação. O app valida tamanho, SHA-256 e interface ONNX antes de instalar. O modelo fica no armazenamento privado do MaiaSupport. É possível substituí-lo depois ou importar manualmente outro ONNX compatível.

Depois que um modelo está instalado, outros apps de xadrez podem solicitar jogadas por AIDL. O app cliente fornece FEN, ratings Elo e sua lista de jogadas legais; MaiaSupport calcula a inferência local e retorna a melhor jogada legal. O cliente não precisa baixar uma segunda cópia dos pesos. Veja [docs/CLIENT_INTEGRATION.md](docs/CLIENT_INTEGRATION.md).

## Estado e compatibilidade

Este projeto contém o app Android, os contratos AIDL, os perfis ONNX para Maia-3 5M e 79M, importação local, downloads opcionais verificáveis, codificação FEN e testes unitários. A origem e os hashes dos dois modelos estão em [MODEL_PROVENANCE.md](MODEL_PROVENANCE.md). Os pesos não estão embutidos no APK nem versionados neste repositório. Um botão explícito e uma confirmação precedem qualquer download de modelo.

O perfil 5M espera entradas `tokens` `[1,64,97]`, `self_elo` e `oppo_elo` em `float32`, e saídas `policy` e `wdl`. O perfil 79M espera `tokens` `[1,64,97]`, `self_elo` e `opponent_elo` em `int64`, e saída `move_logits`. Ambos usam histórico 8 sem features temporais e são pontuados contra as jogadas legais que o cliente envia. O cliente é responsável por gerar e validar as jogadas legais, regras especiais, histórico e FEN.

## Construir

Requisitos: JDK 17, Android SDK 35 e Gradle compatível com Android Gradle Plugin 8.7.3.

```sh
gradle :app:testDebugUnitTest
gradle :app:assembleDebug
```

Depois de instalar o APK, abra MaiaSupport e escolha 5M ou 79M. O modelo selecionado é instalado uma vez; então apps clientes podem chamar o serviço local sem buscar o arquivo por conta própria.

## Licenças e proveniência

O app e suas adaptações são distribuídos sob GNU AGPL v3.0 somente. Maia-3 e os pesos/modelos citados também estão sob AGPL-3.0. ONNX Runtime Android é publicado sob MIT e obtido do Maven Central. Os avisos completos estão em `THIRD_PARTY_NOTICES.md` e `LICENSE`.

## F-Droid

O projeto evita Google Play Services, telemetria e publicidade. A permissão de rede serve aos downloads de modelo iniciados e confirmados explicitamente pela pessoa usuária. Antes da submissão, publique o código em um repositório Git público, configure os metadados de build do F-Droid e valide uma compilação reproduzível com `fdroidserver`. A inclusão é decidida pelos mantenedores do F-Droid e não é garantida.
