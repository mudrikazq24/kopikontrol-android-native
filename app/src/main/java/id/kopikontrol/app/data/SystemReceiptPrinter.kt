package id.kopikontrol.app.data

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import java.io.FileOutputStream

class SystemReceiptPrinter(private val context: Context) {
    fun print(jobId: String, receipt: String, columns: Int = 33) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val media = PrintAttributes.MediaSize("KOPIKONTROL_72MM", "Nota Kasir 72 mm", 2_835, 11_000)
        val attributes = PrintAttributes.Builder()
            .setMediaSize(media)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
            .build()
        printManager.print("KopiKontrol-$jobId", ReceiptAdapter(context, receipt, jobId, columns), attributes)
    }
}

private class ReceiptAdapter(
    private val context: Context,
    private val receipt: String,
    private val jobId: String,
    private val columns: Int,
) : PrintDocumentAdapter() {
    private var attributes: PrintAttributes? = null

    override fun onLayout(
        oldAttributes: PrintAttributes?, newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal, callback: LayoutResultCallback, extras: Bundle?,
    ) {
        if (cancellationSignal.isCanceled) { callback.onLayoutCancelled(); return }
        attributes = newAttributes
        callback.onLayoutFinished(
            PrintDocumentInfo.Builder("$jobId.pdf").setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(1).build(),
            oldAttributes != newAttributes,
        )
    }

    override fun onWrite(
        pages: Array<out PageRange>, destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal, callback: WriteResultCallback,
    ) {
        val printAttributes = attributes ?: run { callback.onWriteFailed("Pengaturan kertas tidak tersedia."); return }
        if (cancellationSignal.isCanceled) { callback.onWriteCancelled(); return }
        runCatching {
    val document = PrintedPdfDocument(context, printAttributes)

    try {
        val page = document.startPage(0)
        val canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            textSize = 10f
            typeface = Typeface.MONOSPACE
        }

        val left = page.info.contentRect.left + 12f
        var y = page.info.contentRect.top + 20f

        receipt.lineSequence().forEach { line ->
            canvas.drawText(
                line.take(columns.coerceIn(24, 48)),
                left,
                y,
                paint
            )
            y += 13f
        }

        document.finishPage(page)

        FileOutputStream(destination.fileDescriptor).use { output ->
            document.writeTo(output)
        }
    } finally {
        document.close()
    }
}.onSuccess {
    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
}.onFailure { error ->
    callback.onWriteFailed(
        error.message ?: "Nota gagal dibuat."
    )
}
    }
}
