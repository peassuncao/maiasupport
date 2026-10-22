package com.maiasupport.engine

import android.content.Context
import android.net.Uri
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.FloatBuffer
import java.nio.LongBuffer
import java.security.MessageDigest

/** Maia-3 ONNX runtime supporting the compact 5M and larger 79M model exports. */
internal object MaiaModelRuntime {
    private val environment = OrtEnvironment.getEnvironment()
    private val lock = Any()
    private var session: OrtSession? = null
    private var activeProfile: ModelProfile? = null

    private data class ModelSpec(val label: String, val bytes: Long, val sha256: String, val url: String)
    private data class ModelProfile(val selfEloName: String, val opponentEloName: String, val policyOutputName: String, val eloIsFloat: Boolean, val label: String)

    private val smallModel = ModelSpec(
        label = "Maia-3 5M",
        bytes = 20_990_437L,
        sha256 = "f72550295eaaf66d8d8c94c5554cf5c3742dfd07f153d758b0d7baf0206ec589",
        url = "https://raw.githubusercontent.com/egdels/moveapiece/5dd3521bc95197e8f778d515e83ba7c66f09d3f3/app/src/main/assets/maia/maia3-5m.onnx",
    )
    private val largeModel = ModelSpec(
        label = "Maia-3 79M",
        bytes = 316_034_244L,
        sha256 = "3454b03ae78baa64a87b345fdb1a457265d912caec531039b074f07eda0d8010",
        url = "https://media.githubusercontent.com/media/Dash1971/maia-chess-android/cd2551f70f9dde903d4bf543c7ea309beecee58f/assets/models/maia3-79m.onnx",
    )

    fun modelFile(context: Context) = File(context.noBackupFilesDir, "maia3.onnx")

    fun importModel(context: Context, uri: Uri) = synchronized(lock) {
        val temporary = File(modelFile(context).parentFile, "maia3.onnx.importing")
        try {
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "Não foi possível abrir o arquivo selecionado." }
                FileOutputStream(temporary).use { output -> input.copyTo(output) }
            }
            require(temporary.length() > 1024 * 1024) { "O arquivo não parece ser um modelo ONNX válido." }
            installValidatedCandidate(temporary, modelFile(context))
        } finally {
            temporary.delete()
        }
    }

    /** Called only after the user taps a size-specific download button and confirms. */
    fun downloadModel(context: Context, useLargeModel: Boolean, onProgress: (Int) -> Unit) = synchronized(lock) {
        val spec = if (useLargeModel) largeModel else smallModel
        val target = modelFile(context)
        if (isReady(context) && target.length() == spec.bytes) {
            onProgress(100)
            return@synchronized
        }
        val temporary = File(target.parentFile, "maia3.onnx.download")
        var connection: HttpURLConnection? = null
        try {
            temporary.delete()
            connection = (URL(spec.url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 90_000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "MaiaSupport/0.1 Android")
            }
            connection.connect()
            check(connection.responseCode == HttpURLConnection.HTTP_OK) {
                "Download indisponível (HTTP ${connection.responseCode}). Tente novamente ou importe um ONNX manualmente."
            }
            val advertisedLength = connection.getHeaderField("Content-Length")?.toLongOrNull() ?: -1L
            require(advertisedLength == spec.bytes) { "Tamanho inesperado do arquivo; download interrompido por segurança." }
            val digest = MessageDigest.getInstance("SHA-256")
            var received = 0L
            connection.inputStream.use { input ->
                FileOutputStream(temporary).use { output ->
                    val buffer = ByteArray(1024 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        digest.update(buffer, 0, count)
                        received += count
                        onProgress(((received * 100) / spec.bytes).toInt().coerceIn(0, 99))
                    }
                    output.fd.sync()
                }
            }
            require(received == spec.bytes) { "Download incompleto: tamanho do modelo não corresponde ao esperado." }
            val actualHash = digest.digest().joinToString("") { "%02x".format(it) }
            require(actualHash == spec.sha256) { "SHA-256 do modelo não corresponde; arquivo rejeitado." }
            installValidatedCandidate(temporary, target)
            onProgress(100)
        } finally {
            connection?.disconnect()
            temporary.delete()
        }
    }

    private fun installValidatedCandidate(candidateFile: File, target: File) {
        val (candidate, profile) = createSession(candidateFile)
        candidate.close()
        session?.close()
        session = null
        activeProfile = profile
        check(target.delete() || !target.exists()) { "Não foi possível substituir o modelo existente." }
        check(candidateFile.renameTo(target)) { "Não foi possível salvar o modelo validado." }
    }

    fun isReady(context: Context): Boolean = modelFile(context).isFile && modelFile(context).length() > 1024 * 1024
    fun modelLabel(context: Context): String = when (modelFile(context).length()) {
        smallModel.bytes -> smallModel.label
        largeModel.bytes -> largeModel.label
        else -> "Maia-3 ONNX"
    }

    fun bestMove(context: Context, fen: String, selfElo: Int, opponentElo: Int, legalMoves: Array<out String>): String = synchronized(lock) {
        require(selfElo in 0..5000 && opponentElo in 0..5000) { "Elo deve estar entre 0 e 5000." }
        require(legalMoves.isNotEmpty()) { "A posição não tem jogadas legais disponíveis." }
        require(legalMoves.size <= 256) { "Lista de jogadas legais excede o limite." }
        val side = fen.trim().split(Regex("\\s+")).getOrNull(1)
        require(side == "w" || side == "b") { "FEN inválido." }
        val tokens = MaiaEncoding.encodeFen(fen)
        if (session == null) {
            val (loaded, profile) = createSession(modelFile(context))
            session = loaded
            activeProfile = profile
        }
        val active = requireNotNull(session)
        val profile = requireNotNull(activeProfile)
        OnnxTensor.createTensor(environment, FloatBuffer.wrap(tokens), longArrayOf(1, 64, 97)).use { tokenTensor ->
            val selfTensor = if (profile.eloIsFloat) {
                OnnxTensor.createTensor(environment, FloatBuffer.wrap(floatArrayOf(selfElo.toFloat())), longArrayOf(1))
            } else {
                OnnxTensor.createTensor(environment, LongBuffer.wrap(longArrayOf(selfElo.toLong())), longArrayOf(1))
            }
            selfTensor.use {
                val opponentTensor = if (profile.eloIsFloat) {
                    OnnxTensor.createTensor(environment, FloatBuffer.wrap(floatArrayOf(opponentElo.toFloat())), longArrayOf(1))
                } else {
                    OnnxTensor.createTensor(environment, LongBuffer.wrap(longArrayOf(opponentElo.toLong())), longArrayOf(1))
                }
                opponentTensor.use {
                    active.run(mapOf("tokens" to tokenTensor, profile.selfEloName to selfTensor, profile.opponentEloName to opponentTensor)).use { output ->
                        @Suppress("UNCHECKED_CAST")
                        val logits = output.get(profile.policyOutputName).get().value as Array<FloatArray>
                        val scores = logits.firstOrNull() ?: error("Saída de política vazia.")
                        legalMoves.maxByOrNull { move ->
                            val index = MaiaEncoding.moveIndex(move, side == "b")
                            require(index in scores.indices) { "Modelo incompatível: índice de jogada fora da saída." }
                            scores[index]
                        } ?: error("Nenhuma jogada candidata.")
                    }
                }
            }
        }
    }

    private fun createSession(file: File): Pair<OrtSession, ModelProfile> {
        require(file.isFile) { "Modelo Maia-3 não instalado. Baixe ou importe um arquivo Maia-3 ONNX no MaiaSupport." }
        val session = OrtSession.SessionOptions().use { options ->
            options.setIntraOpNumThreads(2)
            options.setInterOpNumThreads(1)
            options.setExecutionMode(OrtSession.SessionOptions.ExecutionMode.SEQUENTIAL)
            options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT)
            options.setMemoryPatternOptimization(false)
            environment.createSession(file.absolutePath, options)
        }
        return try {
            val inputs = session.inputNames
            val outputs = session.outputNames
            val profile = when {
                inputs.containsAll(setOf("tokens", "self_elo", "oppo_elo")) && "policy" in outputs ->
                    ModelProfile("self_elo", "oppo_elo", "policy", true, "Maia-3 5M")
                inputs.containsAll(setOf("tokens", "self_elo", "opponent_elo")) && "move_logits" in outputs ->
                    ModelProfile("self_elo", "opponent_elo", "move_logits", false, "Maia-3 79M")
                else -> throw IllegalArgumentException("ONNX Maia-3 incompatível: esperadas entradas/saídas de Maia-3 5M ou 79M.")
            }
            Pair(session, profile)
        } catch (error: Throwable) {
            session.close()
            throw error
        }
    }
}
