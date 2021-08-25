package ru.abondarenko.cloudstorage.library;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class LocalUtils {
    public static List<String> getFileListFromDirectory(Path path) throws IOException {
        return Files.list(path).
                filter(p -> !Files.isDirectory(p)).
                map(p -> p.getFileName().toString())
                .collect(Collectors.toList());
    }
}
