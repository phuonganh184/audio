package com.example.utils

import android.content.Context
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

// Bộ tạo tệp âm thanh WAV mẫu offline thực tế để kiểm thử ngay trình phát ExoPlayer
object SampleAudioGenerator {

    data class GeneratedSampleTrack(
        val filePath: String,
        val durationMs: Long,
        val fileSize: Long
    )

    // Tạo file WAV 16-bit PCM giai điệu ambient êm dịu nếu chưa tồn tại trên bộ nhớ ứng dụng
    fun ensureSampleWav(
        context: Context,
        fileName: String,
        durationSeconds: Int,
        baseFrequencies: DoubleArray
    ): GeneratedSampleTrack {
        val dir = File(context.filesDir, "sample_audio")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val outFile = File(dir, fileName)
        val durationMs = durationSeconds * 1000L

        if (outFile.exists() && outFile.length() > 44L) {
            return GeneratedSampleTrack(
                filePath = outFile.absolutePath,
                durationMs = durationMs,
                fileSize = outFile.length()
            )
        }

        val sampleRate = 16000
        val totalSamples = sampleRate * durationSeconds
        val dataSize = totalSamples * 2 // 16-bit mono = 2 bytes/sample
        val totalFileSize = 36 + dataSize

        BufferedOutputStream(FileOutputStream(outFile)).use { bos ->
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray(Charsets.US_ASCII))
            header.putInt(totalFileSize)
            header.put("WAVE".toByteArray(Charsets.US_ASCII))
            header.put("fmt ".toByteArray(Charsets.US_ASCII))
            header.putInt(16) // PCM chunk size
            header.putShort(1) // AudioFormat = 1 (PCM)
            header.putShort(1) // Mono = 1 channel
            header.putInt(sampleRate)
            header.putInt(sampleRate * 2) // ByteRate
            header.putShort(2) // BlockAlign
            header.putShort(16) // BitsPerSample
            header.put("data".toByteArray(Charsets.US_ASCII))
            header.putInt(dataSize)
            bos.write(header.array())

            // Ghi từng khối âm thanh hợp âm chuông thiền nhẹ nhàng giúp dễ nghe khi kiểm thử tua & tốc độ
            val chunkSamples = 4096
            val chunkBuffer = ByteBuffer.allocate(chunkSamples * 2).order(ByteOrder.LITTLE_ENDIAN)
            var sampleIdx = 0

            while (sampleIdx < totalSamples) {
                chunkBuffer.clear()
                val limit = minOf(chunkSamples, totalSamples - sampleIdx)
                for (i in 0 until limit) {
                    val globalSample = sampleIdx + i
                    val timeSec = globalSample.toDouble() / sampleRate.toDouble()

                    // Chuyển nốt mỗi 3 giây tạo giai điệu du dương thư giãn
                    val noteIndex = ((timeSec / 3.0).toInt()) % baseFrequencies.size
                    val noteLocalTime = timeSec % 3.0
                    val freq = baseFrequencies[noteIndex]

                    val envelope = exp(-noteLocalTime * 1.15) * (1.0 - exp(-noteLocalTime * 35.0))
                    val pad = 0.18 * sin(2.0 * PI * (freq * 0.5) * timeSec)
                    val fundamental = 0.52 * sin(2.0 * PI * freq * timeSec) * envelope
                    val harmonic = 0.22 * sin(2.0 * PI * (freq * 1.5) * timeSec) * envelope

                    // Fade-in đầu bài và fade-out cuối bài mượt mà
                    val edgeFade = when {
                        timeSec < 0.8 -> timeSec / 0.8
                        timeSec > durationSeconds - 1.2 -> ((durationSeconds - timeSec) / 1.2).coerceAtLeast(0.0)
                        else -> 1.0
                    }

                    val mixed = ((pad + fundamental + harmonic) * 0.42 * edgeFade).coerceIn(-0.95, 0.95)
                    val pcmVal = (mixed * Short.MAX_VALUE).toInt().toShort()
                    chunkBuffer.putShort(pcmVal)
                }
                bos.write(chunkBuffer.array(), 0, limit * 2)
                sampleIdx += limit
            }
            bos.flush()
        }

        return GeneratedSampleTrack(
            filePath = outFile.absolutePath,
            durationMs = durationMs,
            fileSize = outFile.length()
        )
    }
}
