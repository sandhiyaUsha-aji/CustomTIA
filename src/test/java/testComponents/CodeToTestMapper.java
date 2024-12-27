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

        // Step 2: Dynamically extract test method names from test scripts
        List<String> testMethods = extractTestMethodsFromFile("D:\\MyProject\\src\\test\\java\\Tests\\LoginPage.java");

        // Step 3: Map test methods to classes
        Map<String, String> mappedTests = mapTestMethodsToClasses(testMethods, classToMethods);

        // Step 4: Generate codeToTest.json
        generateCodeToTestJson(mappedTests);

        // Output the results
        System.out.println("Classes and Methods from JaCoCo XML: " + classToMethods);
        System.out.println("Test Methods from File: " + testMethods);
        System.out.println("Mapped Test Methods to Classes: " + mappedTests);
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

    private static List<String> extractTestMethodsFromFile(String filePath) {
        List<String> methodNames = new ArrayList<>();
        File file = new File(filePath);

        if (!file.exists()) {
            System.out.println("The file does not exist: " + filePath);
            return methodNames;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean insideTestMethod = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim(); // Trim any extra whitespace

                // Check for the @Test annotation
                if (line.startsWith("@Test")) {
                    insideTestMethod = true;
                    continue; // Skip the @Test annotation line
                }

                // If inside a @Test method, look for the method name
                if (insideTestMethod) {
                    if (line.startsWith("public") || line.startsWith("protected") || line.startsWith("private")) {
                        // Extract method name from the method declaration line
                        String[] tokens = line.split("\\(");
                        String methodSignature = tokens[0].trim();
                        String[] parts = methodSignature.split("\\s+");
                        if (parts.length >= 2) {
                            String methodName = parts[parts.length - 1]; // The last part is the method name
                            methodNames.add(methodName);
                        }
                        insideTestMethod = false; // Reset after capturing the method name
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return methodNames;
    }

    private static Map<String, String> mapTestMethodsToClasses(List<String> testMethods, Map<String, List<String>> classToMethods) {
        Map<String, String> testToClassMap = new HashMap<>();

        for (String testMethod : testMethods) {
            boolean isMapped = false;

            for (Map.Entry<String, List<String>> entry : classToMethods.entrySet()) {
                String className = entry.getKey();
                List<String> methods = entry.getValue();

                // Check if test method matches any method in this class (case-insensitive)
                for (String method : methods) {
                    if (method.equalsIgnoreCase(testMethod)) {
                        testToClassMap.put(testMethod, "D:\\MyProject\\src\\main\\java\\" + className.replace(".", "\\") + ".java");
                        isMapped = true;
                        break;
                    }
                }

                if (isMapped) break; // Stop searching if a match is found
            }

            // If no mapping is found, mark as "No Coverage"
            if (!isMapped) {
                testToClassMap.put(testMethod, "No Coverage");
            }
        }

        return testToClassMap;
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
