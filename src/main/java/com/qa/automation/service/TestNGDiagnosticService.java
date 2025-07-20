package com.qa.automation.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

@Service
public class TestNGDiagnosticService {

    @Value("${jenkins.url:}")
    private String jenkinsUrl;

    @Value("${jenkins.username:}")
    private String jenkinsUsername;

    @Value("${jenkins.token:}")
    private String jenkinsToken;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> analyzeTestNGStructure(String jobName, String buildNumber) {
        Map<String, Object> analysis = new HashMap<>();

        try {
            // Fetch raw TestNG JSON
            String testngUrl = jenkinsUrl + "/job/" + jobName + "/" + buildNumber + "/testngreports/api/json";

            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    testngUrl, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                String rawJson = response.getBody();
                analysis.put("rawJsonLength", rawJson.length());
                analysis.put("rawJsonPreview", rawJson.substring(0, Math.min(1000, rawJson.length())));

                // Parse JSON and analyze structure
                JsonNode root = objectMapper.readTree(rawJson);
                analysis.put("rootKeys", getKeys(root));

                // Analyze key fields
                if (root.has("total")) {
                    analysis.put("total", root.get("total").asInt());
                }
                if (root.has("failCount")) {
                    analysis.put("failCount", root.get("failCount").asInt());
                }
                if (root.has("skipCount")) {
                    analysis.put("skipCount", root.get("skipCount").asInt());
                }

                // Analyze packageResults structure
                if (root.has("packageResults")) {
                    JsonNode packageResults = root.get("packageResults");
                    analysis.put("packageResultsType", packageResults.getNodeType().toString());
                    analysis.put("packageResultsSize", packageResults.size());

                    if (packageResults.isArray() && packageResults.size() > 0) {
                        JsonNode firstPackage = packageResults.get(0);
                        analysis.put("firstPackageKeys", getKeys(firstPackage));

                        if (firstPackage.has("classResults")) {
                            JsonNode classResults = firstPackage.get("classResults");
                            analysis.put("classResultsType", classResults.getNodeType().toString());
                            analysis.put("classResultsSize", classResults.size());

                            if (classResults.isArray() && classResults.size() > 0) {
                                JsonNode firstClass = classResults.get(0);
                                analysis.put("firstClassKeys", getKeys(firstClass));

                                if (firstClass.has("testMethods")) {
                                    JsonNode testMethods = firstClass.get("testMethods");
                                    analysis.put("testMethodsType", testMethods.getNodeType().toString());
                                    analysis.put("testMethodsSize", testMethods.size());

                                    if (testMethods.isArray() && testMethods.size() > 0) {
                                        JsonNode firstMethod = testMethods.get(0);
                                        analysis.put("firstMethodKeys", getKeys(firstMethod));
                                        analysis.put("firstMethodSample", firstMethod.toString());
                                    }
                                }
                            }
                        }
                    }
                } else {
                    analysis.put("packageResultsExists", false);
                }

                // Look for alternative structures
                if (root.has("testResults")) {
                    analysis.put("alternativeStructure", "testResults");
                    analysis.put("testResultsKeys", getKeys(root.get("testResults")));
                }

                if (root.has("suites")) {
                    analysis.put("alternativeStructure", "suites");
                    analysis.put("suitesKeys", getKeys(root.get("suites")));
                }

                // Check for test case details in different locations
                analysis.put("allPossibleTestCasePaths", findTestCasePaths(root, ""));

            } else {
                analysis.put("error", "HTTP " + response.getStatusCodeValue());
            }

        } catch (Exception e) {
            analysis.put("error", "Exception: " + e.getMessage());
            e.printStackTrace();
        }

        return analysis;
    }

    private List<String> getKeys(JsonNode node) {
        List<String> keys = new ArrayList<>();
        if (node != null && node.isObject()) {
            node.fieldNames().forEachRemaining(keys::add);
        }
        return keys;
    }

    private List<String> findTestCasePaths(JsonNode node, String path) {
        List<String> paths = new ArrayList<>();

        if (node.isObject()) {
            node.fieldNames().forEachRemaining(fieldName -> {
                String currentPath = path.isEmpty() ? fieldName : path + "." + fieldName;
                JsonNode child = node.get(fieldName);

                // Look for arrays that might contain test cases
                if (child.isArray() && child.size() > 0) {
                    JsonNode firstElement = child.get(0);
                    if (firstElement.isObject()) {
                        List<String> elementKeys = getKeys(firstElement);
                        if (elementKeys.contains("name") || elementKeys.contains("testName") ||
                                elementKeys.contains("methodName") || elementKeys.contains("status")) {
                            paths.add(currentPath + " (potential test cases: " + child.size() + ")");
                        }
                    }
                }

                // Recurse into objects
                if (child.isObject()) {
                    paths.addAll(findTestCasePaths(child, currentPath));
                }

                // Recurse into first element of arrays
                if (child.isArray() && child.size() > 0 && child.get(0).isObject()) {
                    paths.addAll(findTestCasePaths(child.get(0), currentPath + "[0]"));
                }
            });
        }

        return paths;
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