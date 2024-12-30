package testComponents;

import java.io.*;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.*;
import com.google.gson.Gson;

public class CodeToTestMapper {

    public static void main(String[] args) throws Exception {
        // Step 1: Parse JaCoCo XML to extract covered classes and methods
        Map<String, List<String>> classToMethods = extractClassesFromJaCoCo("D:\\MyProject\\target\\site\\jacoco\\jacoco.xml");

        // Step 2: Dynamically extract test class names and methods from all test scripts in the package
        Map<String, List<String>> testClassesWithMethods = extractTestMethodsFromPackage("D:\\MyProject\\src\\test\\java\\Tests");

        // Step 3: Map test classes to classes from JaCoCo
        Map<String, String> mappedTests = mapTestClassesToClasses(testClassesWithMethods, classToMethods);

        // Step 4: Generate codeToTest.json
        generateCodeToTestJson(mappedTests);

        // Output the results
        System.out.println("Classes and Methods from JaCoCo XML: " + classToMethods);
        System.out.println("Test Classes and Methods from Package: " + testClassesWithMethods);
        System.out.println("Mapped Test Classes to Classes: " + mappedTests);
    }

    private static Map<String, List<String>> extractClassesFromJaCoCo(String jacocoFilePath) throws Exception {
        Map<String, List<String>> classToMethods = new HashMap<>();
        File jacocoFile = new File(jacocoFilePath);

        // Disable DTD validation to avoid FileNotFoundException
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setFeature("http://xml.org/sax/features/validation", false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(jacocoFile);

        NodeList classNodes = doc.getElementsByTagName("class");
        for (int i = 0; i < classNodes.getLength(); i++) {
            Element classElement = (Element) classNodes.item(i);
            String className = classElement.getAttribute("name").replace("/", ".");
            NodeList methodNodes = classElement.getElementsByTagName("method");

            List<String> methods = new ArrayList<>();
            for (int j = 0; j < methodNodes.getLength(); j++) {
                Element methodElement = (Element) methodNodes.item(j);
                String methodName = methodElement.getAttribute("name");
                methods.add(methodName);
            }
            classToMethods.put(className, methods);
        }

        return classToMethods;
    }

    private static Map<String, List<String>> extractTestMethodsFromPackage(String packagePath) {
        Map<String, List<String>> testClassesWithMethods = new HashMap<>();
        File packageDir = new File(packagePath);

        if (!packageDir.exists() || !packageDir.isDirectory()) {
            System.out.println("The package path does not exist or is not a directory: " + packagePath);
            return testClassesWithMethods;
        }

        // Iterate over all files in the package directory
        File[] files = packageDir.listFiles((dir, name) -> name.endsWith(".java"));
        if (files == null || files.length == 0) {
            System.out.println("No Java files found in the package: " + packagePath);
            return testClassesWithMethods;
        }

        for (File file : files) {
            Map.Entry<String, List<String>> result = extractTestMethodsFromFile(file.getAbsolutePath());
            if (result != null) {
                testClassesWithMethods.put(result.getKey(), result.getValue());
            }
        }

        return testClassesWithMethods;
    }

    private static Map.Entry<String, List<String>> extractTestMethodsFromFile(String filePath) {
        File file = new File(filePath);
        List<String> methodNames = new ArrayList<>();
        String className = null;

        if (!file.exists()) {
            System.out.println("The file does not exist: " + filePath);
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim(); // Trim any extra whitespace

                // Extract class name (adjusting for potential variations)
                if (line.startsWith("public class") || line.startsWith("class")) {
                    String[] tokens = line.split("\\s+");
                    className = tokens[2].replace("{", "").trim(); // Extract the class name
                }

                // Check for the @Test annotation
                if (line.startsWith("@Test")) {
                    // Read the next line to find the method signature
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();

                        // Match method signature line
                        if (line.matches("(public|protected|private|default).*\\s+\\w+\\s*\\(.*\\).*")) {
                            // Extract method name from the method declaration line
                            String[] tokens = line.split("\\(");
                            String methodSignature = tokens[0].trim();
                            String[] parts = methodSignature.split("\\s+");
                            if (parts.length >= 2) {
                                String methodName = parts[parts.length - 1]; // The last part is the method name
                                methodNames.add(methodName);
                            }
                            break;
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return (className != null && !methodNames.isEmpty())
                ? new AbstractMap.SimpleEntry<>(className, methodNames)
                : null;
    }


    private static Map<String, String> mapTestClassesToClasses(
            Map<String, List<String>> testClassesWithMethods,
            Map<String, List<String>> classToMethods) {

        Map<String, String> testToClassMap = new HashMap<>();

        for (Map.Entry<String, List<String>> testClassEntry : testClassesWithMethods.entrySet()) {
            String testClass = testClassEntry.getKey(); // e.g., "ProductPage"
            String mappedClassPath = "No Coverage"; // Default value

            // Split the test class name into words
            List<String> testClassWords = getWordsFromClassName(testClass);

            for (String className : classToMethods.keySet()) {
                // Extract simple class name from the fully qualified class name
                String simpleClassName = className.substring(className.lastIndexOf(".") + 1); // e.g., "ProductSourceCode"

                // Split the class name into words
                List<String> classNameWords = getWordsFromClassName(simpleClassName);

                // Check for word overlap between test class and JaCoCo class
                if (hasCommonWords(testClassWords, classNameWords)) {
                    // Build the full class file path
                    mappedClassPath = "D:\\MyProject\\src\\main\\java\\" + className.replace(".", "\\") + ".java";
                    break; // Stop searching after a match is found
                }
            }

            // Map the test class to the resolved class path or "No Coverage"
            testToClassMap.put(testClass, mappedClassPath);
        }

        return testToClassMap;
    }

    // Helper method to split class names into words
    private static List<String> getWordsFromClassName(String className) {
        // Assuming words are separated by uppercase letters (CamelCase style)
        List<String> words = new ArrayList<>();
        for (String word : className.split("(?=[A-Z])")) {
            words.add(word.toLowerCase()); // Convert to lowercase for case-insensitive comparison
        }
        return words;
    }

    // Helper method to check if two lists of words have any common words
    private static boolean hasCommonWords(List<String> list1, List<String> list2) {
        for (String word1 : list1) {
            for (String word2 : list2) {
                if (word1.equals(word2)) {
                    return true; // Common word found
                }
            }
        }
        return false; // No common words
    }


    private static void generateCodeToTestJson(Map<String, String> mappedTests) {
        Gson gson = new Gson();
        String jsonOutput = gson.toJson(mappedTests);

        try (FileWriter writer = new FileWriter("D:\\MyProject\\src\\test\\java\\Data\\CodeToTest.json")) {
            writer.write(jsonOutput);
            System.out.println("codeToTest.json generated successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
