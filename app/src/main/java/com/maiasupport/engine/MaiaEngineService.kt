package com.maiasupport.engine

import android.app.Service
import android.content.Intent
import android.os.IBinder
import java.util.concurrent.Executors

/** Public, bound AIDL service. The client supplies FEN and its legal-move list. */
class MaiaEngineService : Service() {
    private val worker = Executors.newSingleThreadExecutor { task ->
        Thread(task, "MaiaSupport-inference").apply { isDaemon = true }
    }

    private val binder = object : IMaiaEngine.Stub() {
        override fun getApiVersion(): Int = API_VERSION
        override fun isModelReady(): Boolean = MaiaModelRuntime.isReady(applicationContext)
        override fun getEngineName(): String = if (MaiaModelRuntime.isReady(applicationContext)) {
            "${MaiaModelRuntime.modelLabel(applicationContext)} (MaiaSupport)"
        } else {
            "Maia-3 (model not installed)"
        }

        override fun requestBestMove(
            requestId: String?,
            fen: String?,
            selfElo: Int,
            opponentElo: Int,
            legalMoves: Array<out String>?,
            callback: IMaiaResultCallback?,
        ) {
            val id = requestId?.take(128) ?: ""
            val result = callback ?: return
            val position = fen?.take(256) ?: ""
            val moves = legalMoves?.take(256)?.toTypedArray() ?: emptyArray()
            worker.execute {
                try {
                    require(id.isNotBlank()) { "requestId não pode ser vazio." }
                    require(position.isNotBlank()) { "FEN não pode ser vazio." }
                    val best = MaiaModelRuntime.bestMove(applicationContext, position, selfElo, opponentElo, moves)
                    result.onResult(id, best, null)
                } catch (error: Throwable) {
                    try {
                        result.onResult(id, null, error.message ?: "Falha ao executar o engine Maia-3.")
                    } catch (_: android.os.RemoteException) {
                        // The requesting app may have exited; no local action is needed.
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        worker.shutdownNow()
        super.onDestroy()
    }

    companion object {
        const val API_VERSION = 1
        const val ACTION_BIND = "com.maiasupport.engine.BIND"
    }
}
