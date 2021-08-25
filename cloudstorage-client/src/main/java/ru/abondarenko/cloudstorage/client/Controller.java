package ru.abondarenko.cloudstorage.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import ru.abondarenko.cloudstorage.library.LocalUtils;
import ru.abondarenko.cloudstorage.library.NetworkUtils;

import java.net.URL;
import java.nio.file.Paths;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    @FXML
    ListView<String> leftPanel;

    @FXML
    ListView<String> rightPanel;

    @FXML
    GridPane connectionSetupPanel;

    @FXML
    TextField serverAddress;

    @FXML
    TextField serverPort;

    @FXML
    TextField loginField;

    @FXML
    PasswordField passwordField;

    @FXML
    Label connectionFailedLabel;

    private Client client;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        client = new Client();
        updateClientStorageFileList();
    }

    public void connectAction(ActionEvent actionEvent) {

        String address = serverAddress.getText();
        int port = Integer.parseInt(serverPort.getText());

        try {
            client.connect(this, address, port);
            updateCloudStorageFileList();
            connectionSetupPanel.setVisible(false);
            rightPanel.setVisible(true);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Unable to connect to server", ButtonType.OK).showAndWait();
        }
    }

    public void btnExitAction(ActionEvent actionEvent) {
        client.disconnect();
        Platform.exit();
    }

    public void btnCopyFileAction(ActionEvent actionEvent) {
        // копирование файла на сервер
        if (leftPanel.isFocused()) {
            try {
                client.copyFileToServer(leftPanel.getSelectionModel().getSelectedItem());
                updateCloudStorageFileList();
            } catch (Exception e) {
                new Alert(Alert.AlertType.ERROR, "Unable to copy file to server", ButtonType.OK);
            }
        }

        // копирование файла с сервера
        if (rightPanel.isFocused()) {
            client.requestFile(rightPanel.getSelectionModel().getSelectedItem());
        }
    }

    // TODO move action
    public void btnMoveFileAction(ActionEvent actionEvent) {
        // перемещение файла с клиента на сервер
        if (leftPanel.isFocused()) {

        }

        // перемещение файла с сервера на клиент
        if (rightPanel.isFocused()) {

        }
    }

    public void btnDeleteFileAction(ActionEvent actionEvent) {
        // удаление файла на клиенте
        if (leftPanel.isFocused()) {
            try {
                client.deleteFile(leftPanel.getSelectionModel().getSelectedItem());
                updateClientStorageFileList();
            } catch (Exception e) {
                new Alert(Alert.AlertType.ERROR, "Unable to delete file", ButtonType.OK).showAndWait();
            };
        }

        // удаление файла на сервере
        if (rightPanel.isFocused()) {
            NetworkUtils.sendCommand("/delete " + rightPanel.getSelectionModel().getSelectedItem(), Network.getInstance().getCurrentChannel());
        }
    }

    // обновление списка файлов клиента
    public void updateClientStorageFileList() {
        Platform.runLater(() -> {
            try {
                leftPanel.getItems().clear();
                leftPanel.getItems().addAll(LocalUtils.getFileListFromDirectory(Paths.get(ClientMain.CLIENT_FILES_LOCATION)));
            } catch (Exception e) {
                new Alert(Alert.AlertType.WARNING, "Unable to update client file list", ButtonType.OK).showAndWait();
            }
        });
    }

    // запрос на обновление списка файлов на сервере
    public void updateCloudStorageFileList() {
        NetworkUtils.sendCommand("/list", Network.getInstance().getCurrentChannel());
    }

    // обновление списка файлов сервера
    public void setCloudStorageFileList(String files) {
        Platform.runLater(() -> {
            rightPanel.getItems().clear();
            rightPanel.getItems().addAll(files.split(";"));
        });
    }
}
