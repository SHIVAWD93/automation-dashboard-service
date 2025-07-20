package com.qa.automation.service;

import com.qa.automation.model.JenkinsResult;
import com.qa.automation.model.JenkinsTestCase;
import com.qa.automation.repository.JenkinsTestCaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.*;

@Service
public class TestNGXMLParserService {

    @Autowired
    private JenkinsTestCaseRepository jenkinsTestCaseRepository;

    @Value("${jenkins.url:}")
    private String jenkinsUrl;

    @Value("${jenkins.username:}")
    private String jenkinsUsername;

    @Value("${jenkins.token:}")
    private String jenkinsToken;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Extract test cases from TestNG XML files in Jenkins artifacts
     */
    public List<JenkinsTestCase> extractTestCasesFromXMLFiles(JenkinsResult jenkinsResult) {
        List<JenkinsTestCase> testCases = new ArrayList<>();

        try {
            System.out.println("Attempting to extract test cases from TestNG XML files for job: " + jenkinsResult.getJobName());

            // First, get the list of artifacts
            List<String> testngXmlFiles = findTestNGXMLFiles(jenkinsResult.getJobName(), jenkinsResult.getBuildNumber());

            if (testngXmlFiles.isEmpty()) {
                System.out.println("No TestNG XML files found in artifacts");
                return testCases;
            }

            // Parse each TestNG XML file
            for (String xmlFile : testngXmlFiles) {
                try {
                    String xmlContent = downloadArtifact(jenkinsResult.getJobName(), jenkinsResult.getBuildNumber(), xmlFile);
                    if (xmlContent != null && !xmlContent.isEmpty()) {
                        List<JenkinsTestCase> fileCases = parseTestNGXML(jenkinsResult, xmlContent, xmlFile);
                        testCases.addAll(fileCases);
                        System.out.println("Extracted " + fileCases.size() + " test cases from " + xmlFile);
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing XML file " + xmlFile + ": " + e.getMessage());
                }
            }

            System.out.println("Total test cases extracted from XML files: " + testCases.size());

        } catch (Exception e) {
            System.err.println("Error extracting test cases from XML files: " + e.getMessage());
            e.printStackTrace();
        }

        return testCases;
    }

    private List<String> findTestNGXMLFiles(String jobName, String buildNumber) {
        List<String> xmlFiles = new ArrayList<>();

        try {
            // Get artifacts list
            String artifactsUrl = jenkinsUrl + "/job/" + jobName + "/" + buildNumber + "/artifact/*zip*/archive.zip";

            // Try to get artifact tree first
            String treeUrl = jenkinsUrl + "/job/" + jobName + "/" + buildNumber + "/api/json?tree=artifacts[*]";

            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(treeUrl, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode artifactsJson = objectMapper.readTree(response.getBody());
                JsonNode artifacts = artifactsJson.get("artifacts");

                if (artifacts != null && artifacts.isArray()) {
                    for (JsonNode artifact : artifacts) {
                        String fileName = artifact.get("fileName").asText();
                        String relativePath = artifact.get("relativePath").asText();

                        // Look for TestNG result files
                        if (fileName.contains("testng") && fileName.endsWith(".xml")) {
                            xmlFiles.add(relativePath);
                            System.out.println("Found TestNG XML file: " + relativePath);
                        }
                        // Also look for surefire reports
                        else if (relativePath.contains("surefire-reports") && fileName.endsWith(".xml")) {
                            xmlFiles.add(relativePath);
                            System.out.println("Found Surefire XML file: " + relativePath);
                        }
                        // Look for any XML in test-output directory
                        else if (relativePath.contains("test-output") && fileName.endsWith(".xml")) {
                            xmlFiles.add(relativePath);
                            System.out.println("Found test-output XML file: " + relativePath);
                        }
                    }
                }
            }

            // If no artifacts found, try common paths
            if (xmlFiles.isEmpty()) {
                String[] commonPaths = {
                        "target/surefire-reports/testng-results.xml",
                        "test-output/testng-results.xml",
                        "testng-results.xml",
                        "target/surefire-reports/TEST-TestSuite.xml"
                };

                for (String path : commonPaths) {
                    if (artifactExists(jobName, buildNumber, path)) {
                        xmlFiles.add(path);
                        System.out.println("Found TestNG XML at common path: " + path);
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error finding TestNG XML files: " + e.getMessage());
        }

        return xmlFiles;
    }

    private boolean artifactExists(String jobName, String buildNumber, String path) {
        try {
            String url = jenkinsUrl + "/job/" + jobName + "/" + buildNumber + "/artifact/" + path;
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.HEAD, entity, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    private String downloadArtifact(String jobName, String buildNumber, String artifactPath) {
        try {
            String url = jenkinsUrl + "/job/" + jobName + "/" + buildNumber + "/artifact/" + artifactPath;

            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }
        } catch (Exception e) {
            System.err.println("Error downloading artifact " + artifactPath + ": " + e.getMessage());
        }

        return null;
    }

    private List<JenkinsTestCase> parseTestNGXML(JenkinsResult jenkinsResult, String xmlContent, String fileName) {
        List<JenkinsTestCase> testCases = new ArrayList<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(xmlContent.getBytes()));

            // Handle TestNG results XML format
            if (xmlContent.contains("<testng-results") || xmlContent.contains("<suite")) {
                testCases.addAll(parseTestNGResultsXML(jenkinsResult, document));
            }
            // Handle Surefire XML format
            else if (xmlContent.contains("<testsuite")) {
                testCases.addAll(parseSurefireXML(jenkinsResult, document));
            }

        } catch (Exception e) {
            System.err.println("Error parsing XML content from " + fileName + ": " + e.getMessage());
        }

        return testCases;
    }

    private List<JenkinsTestCase> parseTestNGResultsXML(JenkinsResult jenkinsResult, Document document) {
        List<JenkinsTestCase> testCases = new ArrayList<>();

        try {
            // TestNG XML structure: <testng-results> -> <suite> -> <test> -> <class> -> <test-method>
            NodeList suites = document.getElementsByTagName("suite");

            for (int s = 0; s < suites.getLength(); s++) {
                Element suite = (Element) suites.item(s);

                NodeList tests = suite.getElementsByTagName("test");
                for (int t = 0; t < tests.getLength(); t++) {
                    Element test = (Element) tests.item(t);

                    NodeList classes = test.getElementsByTagName("class");
                    for (int c = 0; c < classes.getLength(); c++) {
                        Element clazz = (Element) classes.item(c);
                        String className = clazz.getAttribute("name");

                        NodeList testMethods = clazz.getElementsByTagName("test-method");
                        for (int m = 0; m < testMethods.getLength(); m++) {
                            Element method = (Element) testMethods.item(m);

                            // Skip configuration methods, only get test methods
                            if ("true".equals(method.getAttribute("is-config"))) {
                                continue;
                            }

                            JenkinsTestCase testCase = new JenkinsTestCase();
                            testCase.setJenkinsResult(jenkinsResult);
                            testCase.setClassName(className);
                            testCase.setTestName(method.getAttribute("name"));

                            // Determine status
                            String status = method.getAttribute("status");
                            if ("PASS".equals(status)) {
                                testCase.setStatus("PASSED");
                            } else if ("FAIL".equals(status)) {
                                testCase.setStatus("FAILED");
                            } else if ("SKIP".equals(status)) {
                                testCase.setStatus("SKIPPED");
                            } else {
                                testCase.setStatus("UNKNOWN");
                            }

                            // Get duration if available
                            String durationMs = method.getAttribute("duration-ms");
                            if (durationMs != null && !durationMs.isEmpty()) {
                                try {
                                    double duration = Double.parseDouble(durationMs) / 1000.0; // Convert to seconds
                                    testCase.setDuration(duration);
                                } catch (NumberFormatException e) {
                                    // Ignore duration parsing errors
                                }
                            }

                            // Get exception information if failed
                            if ("FAILED".equals(testCase.getStatus())) {
                                NodeList exceptions = method.getElementsByTagName("exception");
                                if (exceptions.getLength() > 0) {
                                    Element exception = (Element) exceptions.item(0);

                                    String message = exception.getAttribute("message");
                                    if (message != null && !message.isEmpty()) {
                                        testCase.setErrorMessage(message.length() > 2000 ?
                                                message.substring(0, 2000) + "..." : message);
                                    }

                                    NodeList fullStackTrace = exception.getElementsByTagName("full-stacktrace");
                                    if (fullStackTrace.getLength() > 0) {
                                        String stackTrace = fullStackTrace.item(0).getTextContent();
                                        testCase.setStackTrace(stackTrace.length() > 5000 ?
                                                stackTrace.substring(0, 5000) + "..." : stackTrace);
                                    }
                                }
                            }

                            testCases.add(testCase);
                            System.out.println("Parsed TestNG test case: " + className + "." + testCase.getTestName() + " - " + testCase.getStatus());
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error parsing TestNG results XML: " + e.getMessage());
        }

        return testCases;
    }

    private List<JenkinsTestCase> parseSurefireXML(JenkinsResult jenkinsResult, Document document) {
        List<JenkinsTestCase> testCases = new ArrayList<>();

        try {
            // Surefire XML structure: <testsuite> -> <testcase>
            NodeList testSuites = document.getElementsByTagName("testsuite");

            for (int s = 0; s < testSuites.getLength(); s++) {
                Element testSuite = (Element) testSuites.item(s);

                NodeList testCaseNodes = testSuite.getElementsByTagName("testcase");
                for (int t = 0; t < testCaseNodes.getLength(); t++) {
                    Element testCaseNode = (Element) testCaseNodes.item(t);

                    JenkinsTestCase testCase = new JenkinsTestCase();
                    testCase.setJenkinsResult(jenkinsResult);
                    testCase.setClassName(testCaseNode.getAttribute("classname"));
                    testCase.setTestName(testCaseNode.getAttribute("name"));

                    // Get duration
                    String time = testCaseNode.getAttribute("time");
                    if (time != null && !time.isEmpty()) {
                        try {
                            testCase.setDuration(Double.parseDouble(time));
                        } catch (NumberFormatException e) {
                            // Ignore duration parsing errors
                        }
                    }

                    // Determine status based on child elements
                    NodeList failures = testCaseNode.getElementsByTagName("failure");
                    NodeList errors = testCaseNode.getElementsByTagName("error");
                    NodeList skipped = testCaseNode.getElementsByTagName("skipped");

                    if (failures.getLength() > 0) {
                        testCase.setStatus("FAILED");
                        Element failure = (Element) failures.item(0);
                        testCase.setErrorMessage(failure.getAttribute("message"));
                        testCase.setStackTrace(failure.getTextContent());
                    } else if (errors.getLength() > 0) {
                        testCase.setStatus("FAILED");
                        Element error = (Element) errors.item(0);
                        testCase.setErrorMessage(error.getAttribute("message"));
                        testCase.setStackTrace(error.getTextContent());
                    } else if (skipped.getLength() > 0) {
                        testCase.setStatus("SKIPPED");
                    } else {
                        testCase.setStatus("PASSED");
                    }

                    testCases.add(testCase);
                    System.out.println("Parsed Surefire test case: " + testCase.getClassName() + "." + testCase.getTestName() + " - " + testCase.getStatus());
                }
            }

        } catch (Exception e) {
            System.err.println("Error parsing Surefire XML: " + e.getMessage());
        }

        return testCases;
    }

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        if (jenkinsUsername != null && !jenkinsUsername.isEmpty() &&
                jenkinsToken != null && !jenkinsToken.isEmpty()) {
            String auth = jenkinsUsername + ":" + jenkinsToken;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            headers.set("Authorization", "Basic " + encodedAuth);
        }
        return headers;
    }
}