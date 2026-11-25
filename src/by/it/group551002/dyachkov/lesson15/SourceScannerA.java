package by.it.group551002.dyachkov.lesson15;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class SourceScannerA {
    static class Item {
        String path;
        long size;
        public Item(String path, long size) { this.path = path; this.size = size; }
    }

    public static void main(String[] args) throws IOException {
        String src = System.getProperty("user.dir") + File.separator + "src" + File.separator;
        Path root = Paths.get(src);
        if (!Files.isDirectory(root)) return;

        List<Path> files;
        try (Stream<Path> walk = Files.walk(root)) {
            files = walk.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .toList();
        }

        List<Item> items = new ArrayList<>();
        for (Path p : files) {
            String text = new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
            if (text.contains("@Test") || text.contains("org.junit.Test")) continue;

            StringBuilder sb = new StringBuilder();
            for (String line : text.split("\n", -1)) {
                String trimmed = line.strip();
                if (trimmed.isEmpty() || trimmed.startsWith("package ") || trimmed.startsWith("import ")) continue;
                sb.append(line).append("\n");
            }
            String cleaned = sb.toString().strip();
            long size = cleaned.getBytes(StandardCharsets.UTF_8).length;
            items.add(new Item(root.relativize(p).toString(), size));
        }

        items.sort((a, b) -> {
            int c = Long.compare(a.size, b.size);
            return c != 0 ? c : a.path.compareTo(b.path);
        });

        for (Item it : items) {
            System.out.println(it.size + " " + it.path);
        }
    }
}