package ru.abondarenko.cloudstorage.library;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class NetworkUtils {

    private static ByteBuf buf = null;

    public static void sendFile(Path path, Channel channel, ChannelFutureListener finishListener) throws IOException {
        FileRegion region = new DefaultFileRegion(path.toFile(), 0, Files.size(path));

        // служебный байт (25 - отправка файла)
        sendCommandByte(channel, 25);
        // длина имени файла
        sendInt(channel, path.getFileName().toString().length());
        // имя файла
        sendString(channel, path.getFileName().toString());
        // размер файла
        sendLong(channel, Files.size(path));

        // отправка данных файла
        ChannelFuture transferOperationFuture = channel.writeAndFlush(region);
        if (finishListener != null) {
            transferOperationFuture.addListener(finishListener);
        }
    }

    public static void requestFile(String filename, Channel channel) {
        // запрос на скачивание файла (команда)
        sendCommand("/file " + filename, channel);
    }

    public static void sendCommand(String command, Channel channel) {
        // служебный байт (26 - команда)
        sendCommandByte(channel, 26);
        // длина команды
        sendInt(channel, command.length());
        // текст команды
        sendString(channel, command);
    }

    public static void sendDataString(String string, Channel channel) {
        // служебный байт (27 - текст)
        sendCommandByte(channel, 27);
        // длина команды
        sendInt(channel, string.length());
        // текст команды
        sendString(channel, string);
    }

    private static void sendCommandByte(Channel channel, int value) {
        if (buf != null) buf = null;
        buf = ByteBufAllocator.DEFAULT.directBuffer(1);
        buf.writeByte((byte) value);
        channel.writeAndFlush(buf);
    }

    private static void sendInt(Channel channel, int value) {
        if (buf != null) buf = null;
        buf = ByteBufAllocator.DEFAULT.directBuffer(4);
        buf.writeInt(value);
        channel.writeAndFlush(buf);
    }

    private static void sendString(Channel channel, String string) {
        if (buf != null) buf = null;
        byte[] stringBytes = string.getBytes(StandardCharsets.UTF_8);
        buf = ByteBufAllocator.DEFAULT.directBuffer(stringBytes.length);
        buf.writeBytes(stringBytes);
        channel.writeAndFlush(buf);
    }

    private static void sendLong(Channel channel, long value) {
        if (buf != null) buf = null;
        buf = ByteBufAllocator.DEFAULT.directBuffer(8);
        buf.writeLong(value);
        channel.writeAndFlush(buf);
    }
}
