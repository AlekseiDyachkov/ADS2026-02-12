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
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class SourceScannerC {
    static class Doc {
        String path;
        char[] text;
        public Doc(String path, char[] text) { this.path = path; this.text = text; }
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

        List<Doc> docs = new ArrayList<>();
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
            String collapsed = sb.toString().replaceAll("\\s+", " ").strip();
            docs.add(new Doc(root.relativize(p).toString(), collapsed.toCharArray()));
        }

        Doc[] arr = docs.toArray(new Doc[0]);
        Arrays.sort(arr, Comparator.comparingInt(d -> d.text.length));
        int n = arr.length;

        List<int[]> pairs = IntStream.range(0, n).parallel().boxed()
                .flatMap(i -> {
                    List<int[]> local = new ArrayList<>();
                    char[] a = arr[i].text;
                    for (int j = i + 1; j < n; j++) {
                        char[] b = arr[j].text;
                        if (b.length - a.length > 9) break;
                        if (within(a, b, 9)) local.add(new int[]{i, j});
                    }
                    return local.stream();
                })
                .toList();

        Map<String, TreeSet<String>> copies = new TreeMap<>();
        for (int[] pr : pairs) {
            String p1 = arr[pr[0]].path;
            String p2 = arr[pr[1]].path;
            copies.computeIfAbsent(p1, k -> new TreeSet<>()).add(p2);
            copies.computeIfAbsent(p2, k -> new TreeSet<>()).add(p1);
        }

        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, TreeSet<String>> e : copies.entrySet()) {
            out.append(e.getKey()).append('\n');
            for (String c : e.getValue()) {
                out.append(c).append('\n');
            }
        }
        System.out.print(out);
    }

    static boolean within(char[] a, char[] b, int k) {
        int n = a.length, m = b.length;
        if (Math.abs(n - m) > k) return false;

        int p = 0, minLen = Math.min(n, m);
        while (p < minLen && a[p] == b[p]) p++;
        int s = 0;
        while (s < minLen - p && a[n - 1 - s] == b[m - 1 - s]) s++;

        int la = n - p - s, lb = m - p - s;
        if (la == 0 || lb == 0) return Math.max(la, lb) <= k;

        int inf = k + 1;
        int[] prev = new int[lb + 1];
        int[] cur = new int[lb + 1];
        for (int j = 0; j <= lb; j++) prev[j] = j <= k ? j : inf;

        for (int i = 1; i <= la; i++) {
            int lo = Math.max(1, i - k), hi = Math.min(lb, i + k);
            Arrays.fill(cur, inf);
            cur[lo - 1] = (lo == 1) ? Math.min(i, inf) : inf;
            if (hi < lb) cur[hi + 1] = inf;

            char ca = a[p + i - 1];
            int rowMin = inf;
            for (int j = lo; j <= hi; j++) {
                int v = prev[j - 1] + (ca == b[p + j - 1] ? 0 : 1);
                v = Math.min(v, Math.min(prev[j] + 1, cur[j - 1] + 1));
                if (v > inf) v = inf;
                cur[j] = v;
                if (v < rowMin) rowMin = v;
            }
            if (rowMin > k) return false;
            int[] t = prev; prev = cur; cur = t;
        }
        return prev[lb] <= k;
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