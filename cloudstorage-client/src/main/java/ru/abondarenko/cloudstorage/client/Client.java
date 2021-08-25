package ru.abondarenko.cloudstorage.client;

import ru.abondarenko.cloudstorage.library.NetworkUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

public class Client {

    CountDownLatch networkStarter;

    public Client() {
        networkStarter = new CountDownLatch(1);
    }

    public void connect(Controller controller, String address, int port) throws Exception {
        System.out.println("Trying to connect to " + address + ":" + port);
        new Thread(() -> Network.getInstance().start(controller, networkStarter, address, port)).start();
        networkStarter.await();
    }

    public void disconnect() {
        Network.getInstance().stop();
    }

    public void sendFileToServer(Path path) throws IOException {
        NetworkUtils.sendFile(path, Network.getInstance().getCurrentChannel(), future -> {
            if (!future.isSuccess()) {
                future.cause().printStackTrace();
            }
            if (future.isSuccess()) {
                System.out.println("Файл успешно передан");
            }
        });
    }

    public void copyFileToServer(String filename) throws Exception {
        sendFileToServer(Paths.get(ClientMain.CLIENT_FILES_LOCATION, filename));
    }

    public void deleteFile(String filename) throws Exception {
        Files.delete(Paths.get(ClientMain.CLIENT_FILES_LOCATION, filename));
    }

    public void requestFile(String filename) {
        NetworkUtils.requestFile(filename, Network.getInstance().getCurrentChannel());
    }
}
