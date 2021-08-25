package ru.abondarenko.cloudstorage.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

public class ClientHandler extends ChannelInboundHandlerAdapter {

    public enum State {
        IDLE,
        COMMAND_LENGTH, COMMAND, COMMAND_HANDLE,
        STRING_LENGTH, STRING,
        NAME_LENGTH, NAME, FILE_LENGTH, FILE
    }

    private State currentState = State.IDLE;
    private Controller controller;
    private int nextLength;
    private long fileLength;
    private long receivedFileLength;
    private String receivedString;
    private String receivedCommand;
    private BufferedOutputStream out;

    public ClientHandler(Controller controller) {
        this.controller = controller;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = ((ByteBuf) msg);
        while (buf.readableBytes() > 0) {
            if (currentState == State.IDLE) {
                byte readed = buf.readByte();
                if (readed == (byte) 25) {
                    currentState = State.NAME_LENGTH;
                    receivedFileLength = 0L;
                    System.out.println("STATE: Start file receiving");
                } else if (readed == (byte) 26) {
                    currentState = State.COMMAND_LENGTH;
                    System.out.println("STATE: Waiting for command");
                } else if (readed == (byte) 27) {
                    currentState = State.STRING_LENGTH;
                    System.out.println("STATE: Waiting for string");
                } else {
                    System.out.println("ERROR: Invalid first byte - " + readed);
                }
            }

            if (currentState == State.COMMAND_LENGTH) {
                if (buf.readableBytes() >= 4) {
                    System.out.println("STATE: Get command length");
                    nextLength = buf.readInt();
                    currentState = State.COMMAND;
                }
            }

            if (currentState == State.COMMAND) {
                if (buf.readableBytes() >= nextLength) {
                    byte[] commandBytes = new byte[nextLength];
                    buf.readBytes(commandBytes);
                    receivedCommand = new String(commandBytes, StandardCharsets.UTF_8);
                    System.out.println("STATE: Command received - " + receivedCommand);
                    currentState = State.COMMAND_HANDLE;
                }
            }

            if (currentState == State.COMMAND_HANDLE) {
                if (receivedCommand.equals("/update")) {
                    controller.updateCloudStorageFileList();
                }
                receivedCommand = null;
                currentState = State.IDLE;
            }

            if (currentState == State.STRING_LENGTH) {
                if (buf.readableBytes() >= 4) {
                    System.out.println("STATE: Get string length");
                    nextLength = buf.readInt();
                    currentState = State.STRING;
                }
            }

            if (currentState == State.STRING) {
                if (buf.readableBytes() >= nextLength) {
                    byte[] stringBytes = new byte[nextLength];
                    buf.readBytes(stringBytes);
                    receivedString = new String(stringBytes, StandardCharsets.UTF_8);
                    System.out.println("STATE: String received - " + receivedString);
                    controller.setCloudStorageFileList(receivedString);
                    currentState = State.IDLE;
                }
            }

            if (currentState == State.NAME_LENGTH) {
                if (buf.readableBytes() >= 4) {
                    System.out.println("STATE: Get filename length");
                    nextLength = buf.readInt();
                    currentState = State.NAME;
                }
            }

            if (currentState == State.NAME) {
                if (buf.readableBytes() >= nextLength) {
                    byte[] fileName = new byte[nextLength];
                    buf.readBytes(fileName);
                    System.out.println("STATE: Filename received - _" + new String(fileName, StandardCharsets.UTF_8));
                    out = new BufferedOutputStream(new FileOutputStream(ClientMain.CLIENT_FILES_LOCATION + "/" + new String(fileName)));
                    currentState = State.FILE_LENGTH;
                }
            }

            if (currentState == State.FILE_LENGTH) {
                if (buf.readableBytes() >= 8) {
                    fileLength = buf.readLong();
                    System.out.println("STATE: File length received - " + fileLength);
                    currentState = State.FILE;
                }
            }

            if (currentState == State.FILE) {
                while (buf.readableBytes() > 0) {
                    out.write(buf.readByte());
                    receivedFileLength++;
                    if (fileLength == receivedFileLength) {
                        currentState = State.IDLE;
                        System.out.println("File received");
                        controller.updateClientStorageFileList();
                        out.close();
                        break;
                    }
                }
            }
        }
        if (buf.readableBytes() == 0) {
            buf.release();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
