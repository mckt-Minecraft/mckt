package io.github.gaming32.mckt.nbt

import io.github.gaming32.mckt.NonCloseableOutputStream
import net.kyori.adventure.nbt.BinaryTagTypes
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

fun writeNbt(nbt: NbtCompound, out: OutputStream) {
    val dos = if (out is DataOutputStream) out else DataOutputStream(out)
    dos.writeByte(BinaryTagTypes.COMPOUND.id().toInt())
    dos.writeUTF("")
    nbt.encode(dos)
}

fun writeNbtCompressed(nbt: NbtCompound, out: OutputStream) =
    GZIPOutputStream(NonCloseableOutputStream(out)).use { writeNbt(nbt, it) }

fun readNbt(inp: InputStream): NbtCompound {
    val dis = if (inp is DataInputStream) inp else DataInputStream(inp)
    val type = dis.readByte()
    if (type != BinaryTagTypes.COMPOUND.id()) throw IllegalArgumentException("Stream data is not NbtCompound")
    dis.skipBytes(dis.readUnsignedShort())
    return getNbtDecoder(type)(dis) as NbtCompound
}

fun readCompressedNbt(inp: InputStream) = readNbt(GZIPInputStream(inp))
