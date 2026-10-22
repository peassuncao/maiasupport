# Proveniência dos modelos Maia-3

O MaiaSupport permite que a pessoa escolha e baixe explicitamente um dos dois modelos. A tela exibe o tamanho antes do download; a transferência só começa após uma confirmação. O app verifica o tamanho, o SHA-256 e a estrutura de entradas/saídas ONNX antes de instalar. Só um modelo ocupa o arquivo ativo por vez; escolher outra versão substitui o modelo anterior após a validação.

## Maia-3 5M — compacto

O arquivo ONNX tem **20,990,437 bytes (aprox. 21 MB)**. É uma exportação documentada por MoveAPiece do checkpoint oficial Maia-3 5M de 20,968,049 bytes. A proveniência de MoveAPiece informa verificação de paridade em 131 posições em cinco ratings, com diferença máxima de logits `2.6e-05`.

- Checkpoint upstream: `UofTCSSLab/Maia3-5M`, revisão `b6559de2398d7140b985f28fd2c19fb5e47ddabe`.
- Conversão ONNX: repositório MoveAPiece, revisão fixada `5dd3521bc95197e8f778d515e83ba7c66f09d3f3`, arquivo `app/src/main/assets/maia/maia3-5m.onnx`.
- URL de download: https://raw.githubusercontent.com/egdels/moveapiece/5dd3521bc95197e8f778d515e83ba7c66f09d3f3/app/src/main/assets/maia/maia3-5m.onnx
- SHA-256 esperado: `f72550295eaaf66d8d8c94c5554cf5c3742dfd07f153d758b0d7baf0206ec589`.
- Interface: entradas `tokens [1,64,97]`, `self_elo [1]` e `oppo_elo [1]`, Elo float32; saídas `policy [1,4352]` e `wdl [1,3]`.
- Licença declarada na proveniência e upstream: AGPL-3.0.

## Maia-3 79M — modelo maior

O arquivo ONNX tem **316,034,244 bytes (316 MB, aprox. 302 MiB)**. É uma conversão do checkpoint oficial Maia-3 79M documentada pelo projeto Mobile Maia; o app não declara essa conversão como feita ou mantida pelo CSSLab.

- Checkpoint upstream: `UofTCSSLab/Maia3-79M`, revisão `a107d6ceb7b298cb04ae1da4edffe2939858b894`.
- Conversão ONNX: repositório Mobile Maia, revisão fixada `cd2551f70f9dde903d4bf543c7ea309beecee58f`, arquivo `assets/models/maia3-79m.onnx`.
- URL de download: https://media.githubusercontent.com/media/Dash1971/maia-chess-android/cd2551f70f9dde903d4bf543c7ea309beecee58f/assets/models/maia3-79m.onnx
- SHA-256 esperado: `3454b03ae78baa64a87b345fdb1a457265d912caec531039b074f07eda0d8010`.
- Interface: entradas `tokens [1,64,97]`, `self_elo [1]` e `opponent_elo [1]`, Elo int64; saída `move_logits`.
- Licença declarada na proveniência e upstream: AGPL-3.0.

## Referências e atribuição

- Código oficial Maia-3 e licença: https://github.com/CSSLab/maia3
- Checkpoint oficial 5M: https://huggingface.co/UofTCSSLab/Maia3-5M
- Checkpoint oficial 79M: https://huggingface.co/UofTCSSLab/Maia3-79M
- Proveniência técnica detalhada da conversão 5M: https://github.com/egdels/moveapiece/blob/5dd3521bc95197e8f778d515e83ba7c66f09d3f3/MAIA_PROVENANCE.md
- Proveniência da conversão 79M: https://github.com/Dash1971/maia-chess-android/blob/cd2551f70f9dde903d4bf543c7ea309beecee58f/MODEL_PROVENANCE.md

Os modelos não estão embutidos no APK nem versionados neste repositório. O 5M é a opção compacta recomendada para começar; o 79M ocupa significativamente mais espaço e memória, e pode fornecer previsões mais precisas. Ambos são modelos Maia-3; MaiaSupport é um projeto independente e não representa as conversões como endossadas pelo CSSLab.
