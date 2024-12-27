package testComponents;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImpactedTestsFinder {
    public static void main(String[] args) {
        try {
            // Paths
            String modifiedFilesPath = "D:\\MyProject\\src\\test\\java\\Data\\ModifiedFiles.txt";
            String codeToTestJsonPath = "D:\\MyProject\\src\\test\\java\\Data\\CodeToTest.json";

            // Read modified files
            List<String> modifiedFiles = Files.readAllLines(Paths.get(modifiedFilesPath));

            // Normalize modified file paths
            List<String> normalizedModifiedFiles = new ArrayList<>();
            for (String filePath : modifiedFiles) {
                normalizedModifiedFiles.add(normalizePath(filePath));
            }

            // Load codeToTest JSON
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> codeToTestMap = mapper.readValue(new File(codeToTestJsonPath), Map.class);

            // Normalize paths in codeToTest.json
            Map<String, String> normalizedCodeToTestMap = normalizeCodeToTestMap(codeToTestMap);

            // Debugging: Print normalized paths and mappings
            System.out.println("Normalized Modified Files:");
            normalizedModifiedFiles.forEach(System.out::println);
            System.out.println("\nNormalized Code to Test Mapping:");
            normalizedCodeToTestMap.forEach((key, value) -> System.out.println(key + ": " + value));

            // Find impacted tests
            List<String> impactedTests = findImpactedTests(normalizedModifiedFiles, normalizedCodeToTestMap);

            // Output impacted tests
            if (impactedTests.isEmpty()) {
                System.out.println("No impacted tests found.");
            } else {
                System.out.println("Impacted Tests: " + impactedTests);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String normalizePath(String path) {
        // Convert backslashes to forward slashes and trim unnecessary spaces
        return path.trim().replace("\\", "/").toLowerCase();
    }

    private static Map<String, String> normalizeCodeToTestMap(Map<String, String> codeToTestMap) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> normalizedMap = new HashMap<>();
        codeToTestMap.forEach((key, value) -> {
            String normalizedPath = normalizePath(value);
            normalizedMap.put(key, normalizedPath);
        });
        return normalizedMap;
    }

    private static List<String> findImpactedTests(List<String> modifiedFiles, Map<String, String> normalizedCodeToTestMap) {
        List<String> impactedTests = new ArrayList<>();

        for (Map.Entry<String, String> entry : normalizedCodeToTestMap.entrySet()) {
            String testMethod = entry.getKey();
            String sourceFilePath = entry.getValue(); // Assume already normalized

            if (modifiedFiles.contains(sourceFilePath)) {
                impactedTests.add(testMethod);
            }
        }

        return impactedTests;
    }
}

