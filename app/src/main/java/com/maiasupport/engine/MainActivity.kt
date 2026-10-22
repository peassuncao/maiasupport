package com.maiasupport.engine

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var smallModelButton: Button
    private lateinit var largeModelButton: Button
    private lateinit var importButton: Button
    private lateinit var progress: ProgressBar
    private val io = Executors.newSingleThreadExecutor()
    private val chooseModel = 41

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (24 * resources.displayMetrics.density).toInt()
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        layout.addView(TextView(this).apply {
            text = "MaiaSupport"
            textSize = 28f
            setTextColor(0xFF202124.toInt())
        }, fullWidth())
        layout.addView(TextView(this).apply {
            text = "Disponibilize o engine Maia-3 localmente para outros apps de xadrez Android."
            textSize = 16f
            setPadding(0, pad / 2, 0, pad)
        }, fullWidth())
        status = TextView(this).apply { textSize = 15f }
        layout.addView(status, fullWidth())
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            visibility = ProgressBar.GONE
        }
        layout.addView(progress, fullWidth())

        layout.addView(TextView(this).apply {
            text = "Escolha uma versão"
            textSize = 18f
            setPadding(0, pad, 0, 0)
        }, fullWidth())
        layout.addView(TextView(this).apply {
            text = "5M — cerca de 21 MB. Menor download e menor uso de armazenamento e memória; recomendado para começar."
            textSize = 14f
            setPadding(0, pad / 3, 0, 0)
        }, fullWidth())
        smallModelButton = Button(this).apply {
            text = "Baixar Maia-3 5M (21 MB)"
            setOnClickListener { confirmDownload(useLargeModel = false) }
        }
        layout.addView(smallModelButton, fullWidth())
        layout.addView(TextView(this).apply {
            text = "79M — 316 MB (aprox. 302 MiB). Modelo maior, com mais exigência de armazenamento e memória; pode oferecer previsões mais precisas."
            textSize = 14f
            setPadding(0, pad / 3, 0, 0)
        }, fullWidth())
        largeModelButton = Button(this).apply {
            text = "Baixar Maia-3 79M (316 MB)"
            setOnClickListener { confirmDownload(useLargeModel = true) }
        }
        layout.addView(largeModelButton, fullWidth())
        importButton = Button(this).apply {
            text = "Importar outro modelo ONNX"
            setOnClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/onnx", "application/octet-stream"))
                }
                startActivityForResult(intent, chooseModel)
            }
        }
        layout.addView(importButton, fullWidth())
        layout.addView(TextView(this).apply {
            text = "O download só começa após sua confirmação. O MaiaSupport verifica o tamanho, o SHA-256 e o formato ONNX. Depois da instalação, os apps clientes usam o serviço AIDL local; não precisam baixar uma segunda cópia do modelo. Você pode substituir a versão mais tarde."
            textSize = 13f
            setPadding(0, pad / 2, 0, 0)
        }, fullWidth())
        setContentView(layout)
        refreshStatus()
    }

    private fun confirmDownload(useLargeModel: Boolean) {
        val version = if (useLargeModel) "Maia-3 79M" else "Maia-3 5M"
        val size = if (useLargeModel) "316 MB (aprox. 302 MiB)" else "21 MB"
        val explanation = if (useLargeModel) {
            "É a opção de maior porte, com mais uso de armazenamento e memória; pode oferecer previsões mais precisas."
        } else {
            "É a opção compacta, usa menos armazenamento e memória e é recomendada para começar."
        }
        AlertDialog.Builder(this)
            .setTitle("Baixar $version?")
            .setMessage("Tamanho do download: $size. $explanation O download usa sua conexão de Internet. O arquivo será verificado por tamanho e SHA-256 antes de ser instalado. Deseja continuar?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Baixar") { _, _ -> downloadModel(useLargeModel) }
            .show()
    }

    private fun downloadModel(useLargeModel: Boolean) {
        setBusy(true)
        progress.progress = 0
        progress.visibility = ProgressBar.VISIBLE
        val version = if (useLargeModel) "Maia-3 79M (316 MB)" else "Maia-3 5M (21 MB)"
        status.text = "Conectando para baixar $version…"
        io.execute {
            val message = try {
                MaiaModelRuntime.downloadModel(applicationContext, useLargeModel) { percent ->
                    runOnUiThread {
                        progress.progress = percent
                        status.text = if (percent < 100) "Baixando e verificando $version: $percent%" else "Validando modelo ONNX…"
                    }
                }
                "${MaiaModelRuntime.modelLabel(applicationContext)} instalado e pronto para apps clientes."
            } catch (error: Throwable) {
                "Não foi possível instalar o modelo: ${error.message ?: error.javaClass.simpleName}"
            }
            runOnUiThread {
                progress.visibility = ProgressBar.GONE
                status.text = message
                setBusy(false)
            }
        }
    }

    @Deprecated("Deprecated by Android API; retained for API 23 compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != chooseModel || resultCode != RESULT_OK) return
        val uri: Uri = data?.data ?: return
        setBusy(true)
        status.text = "Importando e validando modelo…"
        io.execute {
            val message = try {
                MaiaModelRuntime.importModel(applicationContext, uri)
                "${MaiaModelRuntime.modelLabel(applicationContext)} importado e pronto para apps clientes."
            } catch (error: Throwable) {
                "Falha na importação: ${error.message ?: error.javaClass.simpleName}"
            }
            runOnUiThread {
                status.text = message
                setBusy(false)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        if (!::status.isInitialized) return
        status.text = if (MaiaModelRuntime.isReady(this)) {
            "${MaiaModelRuntime.modelLabel(this)} instalado localmente. O serviço AIDL está pronto."
        } else {
            "Nenhum modelo instalado. Escolha 5M ou 79M para disponibilizar o engine."
        }
    }

    private fun setBusy(busy: Boolean) {
        smallModelButton.isEnabled = !busy
        largeModelButton.isEnabled = !busy
        importButton.isEnabled = !busy
    }

    private fun fullWidth() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

    override fun onDestroy() {
        io.shutdownNow()
        super.onDestroy()
    }
}
