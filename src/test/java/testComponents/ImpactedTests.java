package testComponents;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.io.File;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ImpactedTests {
    public static void main(String[] args) {
        try {
            // Paths
            String modifiedFilesPath = "D:\\MyProject\\src\\test\\java\\Data\\ModifiedFiles.txt";
            String codeToTestJsonPath = "D:\\MyProject\\src\\test\\java\\Data\\CodeToTest.json";
            String testNgXmlPath = "D:\\MyProject\\testng1.xml";

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

            // Find impacted test classes
            Set<String> impactedTestClasses = findImpactedTestClasses(normalizedModifiedFiles, normalizedCodeToTestMap);

            // Output impacted test classes
            if (impactedTestClasses.isEmpty()) {
                System.out.println("No impacted test classes found.");
            } else {
                System.out.println("Impacted Test Classes: " + impactedTestClasses);

                // Dynamically generate or update the TestNG XML
                generateTestNGXml(impactedTestClasses, testNgXmlPath);
                System.out.println("TestNG XML file updated at: " + testNgXmlPath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String normalizePath(String path) {
        return path.trim().replace("\\", "/").toLowerCase();
    }

    private static Map<String, String> normalizeCodeToTestMap(Map<String, String> codeToTestMap) {
        Map<String, String> normalizedMap = new HashMap<>();
        String unwantedPrefix = "d:/myproject/"; // Define the prefix to be removed.

        for (Map.Entry<String, String> entry : codeToTestMap.entrySet()) {
            String testClass = entry.getKey(); // Extract the test class name.
            String filePath = entry.getValue(); // Extract the full file path.

            // Normalize the file path
            String normalizedPath = filePath.trim().replace("\\", "/").toLowerCase();

            // Remove unwanted prefix, if present
            if (normalizedPath.startsWith(unwantedPrefix)) {
                normalizedPath = normalizedPath.substring(unwantedPrefix.length());
            }

            // Map the test class to the normalized file path
            normalizedMap.put(testClass, normalizedPath);
        }

        return normalizedMap;
    }

    private static Set<String> findImpactedTestClasses(List<String> modifiedFiles, Map<String, String> normalizedCodeToTestMap) {
        Set<String> impactedTestClasses = new HashSet<>();
        for (Map.Entry<String, String> entry : normalizedCodeToTestMap.entrySet()) {
            String testClass = entry.getKey();
            String sourceFilePath = entry.getValue();
            if (modifiedFiles.contains(sourceFilePath)) {
                impactedTestClasses.add(testClass); // Add the test class name if its source file is modified.
            }
        }
        return impactedTestClasses;
    }

    private static void generateTestNGXml(Set<String> impactedTestClasses, String outputPath) throws Exception {
        if (impactedTestClasses.isEmpty()) {
            System.out.println("No test classes to include in TestNG XML.");
            return;
        }

        StringBuilder xmlBuilder = new StringBuilder();

        xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xmlBuilder.append("<!DOCTYPE suite SYSTEM \"https://testng.org/testng-1.0.dtd\">\n");
        xmlBuilder.append("<suite name=\"ImpactedTests\">\n");
        xmlBuilder.append("  <test name=\"Test\">\n");
        xmlBuilder.append("    <classes>\n");

        // Add impacted test classes to TestNG XML
        for (String testClass : impactedTestClasses) {
            xmlBuilder.append("      <class name=\"").append(testClass).append("\"/>\n");
        }

        xmlBuilder.append("    </classes>\n");
        xmlBuilder.append("  </test>\n");
        xmlBuilder.append("</suite>");

        // Write XML to the file
        try (FileWriter writer = new FileWriter(outputPath)) {
            writer.write(xmlBuilder.toString());
        }
    }
}
