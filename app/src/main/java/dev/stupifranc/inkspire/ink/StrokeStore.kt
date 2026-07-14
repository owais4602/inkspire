package dev.stupifranc.inkspire.ink

import androidx.ink.brush.Brush
import androidx.ink.storage.decode
import androidx.ink.storage.encode
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInputBatch
import dev.stupifranc.inkspire.model.BrushFamilyChoice
import dev.stupifranc.inkspire.model.StrokeEntry
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

/**
 * Encodes/decodes [StrokeEntry] lists to/from a single opaque binary blob for [dev.stupifranc.inkspire.data.DrawingRepository].
 * `Stroke`/`Brush`/`StrokeInputBatch` are native-backed (trap #1: no Robolectric workaround), so this is
 * Android instrumented/on-device glue, not JVM-unit-testable — same category as [CanvasExporter].
 *
 * Per-entry layout (all via DataOutputStream/DataInputStream):
 * id (UTF) | groupId (UTF) | brushFamily (UTF enum name) | colorIntArgb (Int) | size (Float) | epsilon (Float) |
 * strokeInputBatch bytes (length-prefixed Int, via ink-storage's own StrokeInputBatch codec).
 *
 * `BrushFamily` itself is not serialized via ink-storage's generic codec — that API is still
 * `@ExperimentalInkCustomBrushApi` (meant for custom brush textures this app doesn't use) — instead the
 * family is persisted as our own [BrushFamilyChoice] enum and rebuilt via [dev.stupifranc.inkspire.ink.toBrushFamily].
 */
object StrokeStore {

    fun encode(entries: List<StrokeEntry>): ByteArray {
        val out = ByteArrayOutputStream()
        DataOutputStream(out).use { data ->
            data.writeInt(entries.size)
            for (entry in entries) {
                val brush = entry.stroke.brush
                data.writeUTF(entry.id)
                data.writeUTF(entry.groupId)
                data.writeUTF(brush.family.toBrushFamilyChoice().name)
                data.writeInt(brush.colorIntArgb)
                data.writeFloat(brush.size)
                data.writeFloat(brush.epsilon)
                writeLengthPrefixed(data) { entry.stroke.inputs.encode(it) }
            }
        }
        return out.toByteArray()
    }

    fun decode(bytes: ByteArray): List<StrokeEntry> {
        val data = DataInputStream(ByteArrayInputStream(bytes))
        val count = data.readInt()
        return (0 until count).map {
            val id = data.readUTF()
            val groupId = data.readUTF()
            val family = BrushFamilyChoice.valueOf(data.readUTF()).toBrushFamily()
            val colorIntArgb = data.readInt()
            val size = data.readFloat()
            val epsilon = data.readFloat()
            val inputs = StrokeInputBatch.decode(ByteArrayInputStream(readLengthPrefixed(data)))
            val brush = Brush.createWithColorIntArgb(family, colorIntArgb, size, epsilon)
            StrokeEntry(id = id, groupId = groupId, stroke = Stroke(brush, inputs))
        }
    }

    private inline fun writeLengthPrefixed(data: DataOutputStream, write: (ByteArrayOutputStream) -> Unit) {
        val bytes = ByteArrayOutputStream().also(write).toByteArray()
        data.writeInt(bytes.size)
        data.write(bytes)
    }

    private fun readLengthPrefixed(data: DataInputStream): ByteArray =
        ByteArray(data.readInt()).also { data.readFully(it) }
}
