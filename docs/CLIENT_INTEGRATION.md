# Integração do MaiaSupport por AIDL

MaiaSupport implementa um serviço vinculado exportado, com AIDL, no pacote `com.maiasupport.engine`. O processo cliente precisa copiar `IMaiaEngine.aidl` e `IMaiaResultCallback.aidl` para o mesmo pacote AIDL e compilar com AIDL habilitado. Não é necessário incluir dependências ONNX Runtime no app cliente.

## Descoberta e conexão

No manifesto do app cliente (Android 11+), declare visibilidade:

```xml
<queries>
    <package android:name="com.maiasupport.engine" />
</queries>
```

Exemplo Kotlin:

```kotlin
private var maia: IMaiaEngine? = null
private val connection = object : android.content.ServiceConnection {
    override fun onServiceConnected(name: android.content.ComponentName, binder: android.os.IBinder) {
        maia = IMaiaEngine.Stub.asInterface(binder)
        // Consulte maia?.isModelReady() antes de enviar uma posição.
    }
    override fun onServiceDisconnected(name: android.content.ComponentName) { maia = null }
}

val intent = android.content.Intent("com.maiasupport.engine.BIND")
    .setPackage("com.maiasupport.engine")
val bound = bindService(intent, connection, android.content.Context.BIND_AUTO_CREATE)
```

Desconecte com `unbindService(connection)` ao finalizar a sessão/Activity.

## Solicitar uma jogada

```kotlin
val requestId = java.util.UUID.randomUUID().toString()
maia?.requestBestMove(
    requestId,
    currentFen,
    selfElo,
    opponentElo,
    legalMovesUci.toTypedArray(),
    object : IMaiaResultCallback.Stub() {
        override fun onResult(id: String?, bestMoveUci: String?, errorMessage: String?) {
            if (id != requestId) return // associe concorrência pelo seu próprio requestId
            if (errorMessage != null) showEngineError(errorMessage)
            else if (bestMoveUci != null) applyMove(bestMoveUci)
        }
    }
)
```

O pedido é executado em fila de worker do MaiaSupport e o retorno é assíncrono. `bestMoveUci` é uma jogada UCI (ex.: `e2e4`, `e7e8q`). O cliente deve cancelar/ignorar resultados obsoletos por requestId e validar novamente a jogada contra a posição atual antes de aplicá-la.

## Parâmetros e limites

- `fen`: FEN da posição corrente. O serviço usa a posição atual e não reconstrói a história de lances.
- `selfElo`, `opponentElo`: rating de ambos os lados na escala de Elo usada pelo modelo, valores inteiros de 0 a 5000.
- `legalMoves`: lista completa e correta de jogadas legais para esse FEN, codificadas em UCI, incluindo promoção como `q`, `r`, `b` ou `n`. Máximo 256.
- `getApiVersion()`: atualmente `1`.
- `getEngineName()`: nome legível, não é um identificador de licença/afiliação.

O serviço escolhe a jogada legal com maior pontuação Maia (argmax; sem amostragem/temperatura nesta versão). Lista vazia, FEN ou formato de jogada inválidos, modelo ausente/incompatível e erros de inferência retornam `errorMessage`. Solicitações são serializadas para limitar uso de memória e CPU.

## Segurança e privacidade

O serviço precisa ser exportado para permitir chamadas entre apps. Ele não solicita permissões de rede nem publica dados, mas apps clientes locais podem invocá-lo. Não envie dados pessoais em FEN ou requestId. Passe apenas FEN, ratings, lista de jogadas legais e identificador aleatório de correlação. O modelo selecionado é salvo no diretório privado do MaiaSupport e não é compartilhado com o cliente.
