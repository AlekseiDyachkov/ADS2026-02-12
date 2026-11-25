package by.it.group551002.dyachkov.lesson15;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class SourceScannerB {
    static class Item {
        String path;
        long size;
        public Item(String path, long size) { this.path = path; this.size = size; }
    }

    static final Charset[] CHARSETS = {
            StandardCharsets.UTF_8,
            Charset.forName("windows-1251"),
            StandardCharsets.ISO_8859_1
    };

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
            byte[] bytes = Files.readAllBytes(p);
            String text = decode(bytes);
            if (text.contains("@Test") || text.contains("org.junit.Test")) continue;

            text = stripComments(text);
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

    static String decode(byte[] bytes) {
        for (Charset cs : CHARSETS) {
            try {
                return cs.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(bytes)).toString();
            } catch (CharacterCodingException ignored) {}
        }
        return new String(bytes, StandardCharsets.ISO_8859_1);
    }

    static String stripComments(String t) {
        StringBuilder sb = new StringBuilder();
        boolean inStr = false, inChar = false, inLineComment = false, inBlockComment = false;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (inLineComment) {
                if (c == '\n') { inLineComment = false; sb.append(c); }
            } else if (inBlockComment) {
                if (c == '*' && i + 1 < t.length() && t.charAt(i + 1) == '/') { inBlockComment = false; i++; }
                else if (c == '\n') sb.append(c);
            } else if (inStr) {
                sb.append(c);
                if (c == '\\' && i + 1 < t.length()) { sb.append(t.charAt(i + 1)); i++; }
                else if (c == '"') inStr = false;
            } else if (inChar) {
                sb.append(c);
                if (c == '\\' && i + 1 < t.length()) { sb.append(t.charAt(i + 1)); i++; }
                else if (c == '\'') inChar = false;
            } else {
                if (c == '/' && i + 1 < t.length() && t.charAt(i + 1) == '/') { inLineComment = true; i++; }
                else if (c == '/' && i + 1 < t.length() && t.charAt(i + 1) == '*') { inBlockComment = true; i++; }
                else if (c == '"') { inStr = true; sb.append(c); }
                else if (c == '\'') { inChar = true; sb.append(c); }
                else sb.append(c);
            }
        }
        return sb.toString();
    }
}