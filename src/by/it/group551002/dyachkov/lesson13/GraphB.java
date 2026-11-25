package by.it.group551002.dyachkov.lesson13;

import java.util.*;

public class GraphB {

    private static final int UNVISITED = 0;
    private static final int VISITING = 1;
    private static final int VISITED = 2;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        if (!scanner.hasNextLine()) {
            System.out.println("no");
            return;
        }

        String input = scanner.nextLine().trim();
        if (input.isEmpty()) {
            System.out.println("no");
            return;
        }

        Map<String, List<String>> graph = new HashMap<>();
        Set<String> allVertices = new HashSet<>();

        String[] edges = input.split(",");
        for (String edge : edges) {
            String[] parts = edge.split("->");
            if (parts.length == 2) {
                String from = parts[0].trim();
                String to = parts[1].trim();

                graph.computeIfAbsent(from, k -> new ArrayList<>()).add(to);
                allVertices.add(from);
                allVertices.add(to);
            }
        }

        if (hasCycle(graph, allVertices)) {
            System.out.println("yes");
        } else {
            System.out.println("no");
        }
    }

    private static boolean hasCycle(Map<String, List<String>> graph, Set<String> allVertices) {
        Map<String, Integer> state = new HashMap<>();
        for (String vertex : allVertices) {
            state.put(vertex, UNVISITED);
        }

        for (String vertex : allVertices) {
            if (state.get(vertex) == UNVISITED) {
                if (dfs(vertex, graph, state)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean dfs(String current, Map<String, List<String>> graph, Map<String, Integer> state) {
        state.put(current, VISITING);

        List<String> neighbors = graph.get(current);
        if (neighbors != null) {
            for (String neighbor : neighbors) {
                int neighborState = state.get(neighbor);

                if (neighborState == VISITING) {
                    return true;
                }
                if (neighborState == UNVISITED) {
                    if (dfs(neighbor, graph, state)) {
                        return true;
                    }
                }
            }
        }

        state.put(current, VISITED);
        return false;
    }
}
