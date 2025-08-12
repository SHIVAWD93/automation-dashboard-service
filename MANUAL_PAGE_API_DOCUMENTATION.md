# Manual Page Feature - API Documentation

## Overview

The Manual Page feature extends the QA Automation Coverage Dashboard with Jira integration capabilities. It allows teams to:

- Fetch Jira issues from specific sprints
- Link QTest test cases to Jira issues
- Mark test cases as automatable or non-automatable
- Automatically integrate with the Test Case Service for automation readiness
- Search for keywords in Jira issue comments

## Configuration

### Prerequisites

1. **Jira Server/Cloud Access**: You need access to a Jira instance with:
   - Valid username and API token/password
   - Board ID and Project Key
   - Proper permissions to read issues and comments

2. **Jira Configuration**: Update your `application.properties` file:

```properties
# Jira Configuration
jira.url=https://your-jira-instance.atlassian.net
jira.username=your-email@company.com
jira.token=your-jira-api-token
jira.project.key=PROJECT_KEY
jira.board.id=123
```

### Getting Jira API Token

1. Go to https://id.atlassian.com/manage-profile/security/api-tokens
2. Create an API token
3. Copy the token and use it in the configuration

### Finding Board ID

1. Go to your Jira board
2. Look at the URL: `https://your-jira.atlassian.net/jira/software/projects/PROJECT/boards/123`
3. The number `123` is your board ID

## API Endpoints

### Base URL: `/api/manual-page`

### 1. Test Connection

Test if the Jira configuration is working.

**GET** `/test-connection`

**Response:**
```json
{
  "connected": true,
  "message": "Successfully connected to Jira"
}
```

### 2. Get Available Sprints

Fetch all sprints from the configured Jira board.

**GET** `/sprints`

**Response:**
```json
[
  {
    "id": "123",
    "name": "Sprint 1",
    "state": "ACTIVE",
    "startDate": "2024-01-01T00:00:00.000Z",
    "endDate": "2024-01-14T00:00:00.000Z"
  },
  {
    "id": "124",
    "name": "Sprint 2",
    "state": "FUTURE",
    "startDate": "2024-01-15T00:00:00.000Z",
    "endDate": "2024-01-28T00:00:00.000Z"
  }
]
```

### 3. Sync Sprint Issues

Fetch issues from Jira and sync them with the local database.

**POST** `/sprints/{sprintId}/sync`

**Response:**
```json
[
  {
    "id": 1,
    "jiraKey": "PROJECT-123",
    "summary": "Implement user login feature",
    "description": "As a user, I want to log in...",
    "assignee": "john.doe",
    "assigneeDisplayName": "John Doe",
    "sprintId": "123",
    "sprintName": "Sprint 1",
    "issueType": "Story",
    "status": "In Progress",
    "priority": "High",
    "keywordCount": 0,
    "linkedTestCases": [
      {
        "id": 1,
        "qtestTitle": "Test login with valid credentials",
        "canBeAutomated": false,
        "cannotBeAutomated": false,
        "automationStatus": "PENDING"
      }
    ]
  }
]
```

### 4. Get Sprint Issues

Get previously synced issues for a sprint from the local database.

**GET** `/sprints/{sprintId}/issues`

**Response:** Same as sync endpoint

### 5. Update Test Case Automation Flags

Mark test cases as automatable or non-automatable.

**PUT** `/test-cases/{testCaseId}/automation-flags`

**Request Body:**
```json
{
  "canBeAutomated": true,
  "cannotBeAutomated": false
}
```

**Response:**
```json
{
  "id": 1,
  "qtestTitle": "Test login with valid credentials",
  "canBeAutomated": true,
  "cannotBeAutomated": false,
  "automationStatus": "Ready to Automate",
  "projectId": 1,
  "projectName": "Web Application",
  "assignedTesterId": 1,
  "assignedTesterName": "Jane Smith"
}
```

### 6. Map Test Case to Project and Tester

Assign test cases to projects and testers.

**PUT** `/test-cases/{testCaseId}/mapping`

**Request Body:**
```json
{
  "projectId": 1,
  "testerId": 2
}
```

**Response:**
```json
{
  "id": 1,
  "qtestTitle": "Test login with valid credentials",
  "projectId": 1,
  "projectName": "Web Application",
  "assignedTesterId": 2,
  "assignedTesterName": "Jane Smith",
  "domainMapped": "Authentication"
}
```

### 7. Search Keywords in Comments

Search for specific keywords in Jira issue comments.

**POST** `/issues/{jiraKey}/keyword-search`

**Request Body:**
```json
{
  "keyword": "observation"
}
```

**Response:**
```json
{
  "id": 1,
  "jiraKey": "PROJECT-123",
  "summary": "Implement user login feature",
  "keywordCount": 3,
  "searchKeyword": "observation",
  "linkedTestCases": [...]
}
```

### 8. Get Sprint Automation Statistics

Get automation readiness statistics for a sprint.

**GET** `/sprints/{sprintId}/statistics`

**Response:**
```json
{
  "totalTestCases": 25,
  "readyToAutomate": 10,
  "notAutomatable": 5,
  "pending": 10,
  "projectBreakdown": {
    "Web Application": {
      "Ready to Automate": 6,
      "NOT_AUTOMATABLE": 2,
      "PENDING": 4
    },
    "Mobile App": {
      "Ready to Automate": 4,
      "NOT_AUTOMATABLE": 3,
      "PENDING": 6
    }
  }
}
```

### 9. Get Projects and Testers

Get available projects and testers for mapping.

**GET** `/projects`
```json
[
  {
    "id": 1,
    "name": "Web Application",
    "description": "Main web application",
    "domain": {
      "id": 1,
      "name": "Authentication"
    }
  }
]
```

**GET** `/testers`
```json
[
  {
    "id": 1,
    "name": "Jane Smith",
    "email": "jane.smith@company.com",
    "role": "Senior QA Engineer"
  }
]
```

## Global Keyword Search

**Endpoint:** `POST /api/manual-page/global-keyword-search`

**Description:** Search for a keyword across all issues in a project with optional sprint filtering.

**Request Body:**
```json
{
    "keyword": "string",           // Required: The keyword to search for
    "jiraProjectKey": "string",    // Optional: JIRA project key (defaults to configured project)
    "sprintId": "string"           // Optional: Sprint ID for sprint-specific search
}
```

**Response:**
```json
{
    "keyword": "string",
    "totalCount": 0,
    "totalOccurrences": 0,
    "matchingIssues": [
        {
            "key": "string",
            "summary": "string",
            "issueType": "string",
            "status": "string",
            "priority": "string",
            "occurrences": 0
        }
    ],
    "searchDate": "2024-01-01T00:00:00.000Z"
}
```

**Enhanced Features:**
- **Sprint Filtering**: When `sprintId` is provided, search is limited to issues within that specific sprint
- **Cross-reference Comments**: Searches through issue comments for keyword occurrences
- **QTest Integration**: Automatically fetches linked test cases from QTest instead of extracting from JIRA text patterns

## Automation Readiness Flow

When a test case is marked as "Can be Automated" (and "Cannot be Automated" is false), the system automatically:

1. **Validates Mapping**: Checks if the test case has both project and tester assignments
2. **Creates TestCase**: Creates or updates a corresponding `TestCase` entity in the automation system
3. **Sets Status**: Marks the test case as "READY_TO_AUTOMATE"
4. **Assigns Resources**: Links it to the specified project and tester

## Database Schema

### JiraIssue Table
```sql
CREATE TABLE jira_issues (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    jira_key VARCHAR(50) UNIQUE NOT NULL,
    summary VARCHAR(500) NOT NULL,
    description TEXT,
    assignee VARCHAR(100),
    assignee_display_name VARCHAR(100),
    sprint_id VARCHAR(50),
    sprint_name VARCHAR(100),
    issue_type VARCHAR(50),
    status VARCHAR(50),
    priority VARCHAR(50),
    keyword_count INT DEFAULT 0,
    search_keyword VARCHAR(100),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

### JiraTestCase Table
```sql
CREATE TABLE jira_test_cases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    qtest_title VARCHAR(500) NOT NULL,
    qtest_id VARCHAR(100),
    can_be_automated BOOLEAN DEFAULT FALSE,
    cannot_be_automated BOOLEAN DEFAULT FALSE,
    automation_status VARCHAR(50),
    assigned_tester_id BIGINT,
    jira_issue_id BIGINT NOT NULL,
    project_id BIGINT,
    tester_id BIGINT,
    domain_mapped VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (jira_issue_id) REFERENCES jira_issues(id),
    FOREIGN KEY (project_id) REFERENCES projects(id),
    FOREIGN KEY (tester_id) REFERENCES testers(id)
);
```

## Error Handling

All endpoints return appropriate HTTP status codes:

- **200 OK**: Successful operation
- **400 Bad Request**: Invalid request data
- **500 Internal Server Error**: Server-side errors

Error responses include descriptive messages:
```json
{
  "error": "Test case not found with id: 123",
  "timestamp": "2024-01-01T12:00:00Z"
}
```

## QTest Integration Notes

The system is designed to work with QTest test case links in Jira issues. It looks for patterns like:

- "QTest: Test Case Name"
- "Test Case: Test Case Name"
- Bulleted lists with test case names
- Numbered lists with test case names

The extraction is flexible and can be customized by modifying the regex patterns in `JiraIntegrationService.java`.

## Security Considerations

1. **API Tokens**: Store Jira credentials securely
2. **HTTPS**: Use HTTPS for all Jira API calls
3. **Rate Limiting**: Be mindful of Jira API rate limits
4. **Permissions**: Ensure proper Jira permissions for the integration user

## Troubleshooting

### Common Issues

1. **Connection Failed**: Check Jira URL, username, and token
2. **No Sprints Found**: Verify board ID and project key
3. **No Issues Found**: Check sprint ID and JQL permissions
4. **Comments Not Accessible**: Verify comment read permissions

### Logging

Enable debug logging for detailed troubleshooting:
```properties
logging.level.com.qa.automation.service.JiraIntegrationService=DEBUG
logging.level.com.qa.automation.service.ManualPageService=DEBUG
```