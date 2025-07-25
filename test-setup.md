# Database Setup Verification Guide

## 🚀 Quick Start Testing

### 1. Clean Start
```bash
# Remove any existing database files
rm -rf ./data/

# Start the application
mvn spring-boot:run
```

### 2. Check Database Initialization
The application should start without errors and you should see logs like:
```
INFO  - Starting data initialization...
INFO  - Initializing permissions...
INFO  - Created permission: read
INFO  - Created permission: write
INFO  - Created permission: admin
INFO  - Created permission: delete
INFO  - Initializing domains...
INFO  - Created domain: Authentication
...
INFO  - Data initialization completed successfully
```

### 3. Test H2 Console
1. Open browser: http://localhost:8000/h2-console
2. Use these settings:
   - JDBC URL: `jdbc:h2:file:./data/qa_automation`
   - User Name: `sa`
   - Password: (leave empty)
3. Click "Connect"

### 4. Verify Tables Created
In H2 Console, run these queries:
```sql
-- Check permissions
SELECT * FROM PERMISSIONS;

-- Check domains
SELECT * FROM DOMAINS;

-- Check projects
SELECT * FROM PROJECTS;

-- Check testers
SELECT * FROM TESTERS;

-- Check all tables
SHOW TABLES;
```

### 5. Test User Creation
```bash
curl -X POST http://localhost:8000/api/user \
  -H "Content-Type: application/json" \
  -d '{
    "userName": "testuser",
    "password": "password123",
    "role": "QA Engineer"
  }'
```

Expected response: User created successfully with default "read" permission.

### 6. Test Manual Page Endpoints
```bash
# Test Jira connection (will fail without credentials - that's expected)
curl http://localhost:8000/api/manual-page/test-connection

# Get projects
curl http://localhost:8000/api/manual-page/projects

# Get testers
curl http://localhost:8000/api/manual-page/testers
```

## 🔧 Troubleshooting

### Common Issues:

1. **Tables not created**
   - Check that `spring.jpa.hibernate.ddl-auto=update` in application.properties
   - Delete `./data/` folder and restart

2. **Permission errors**
   - Verify DataInitializationService runs on startup
   - Check logs for initialization messages

3. **H2 Console not accessible**
   - Ensure `spring.h2.console.enabled=true`
   - Try: http://localhost:8000/h2-console

4. **Database file location**
   - Database files are stored in `./data/` folder
   - This persists data between restarts

## 📊 Database Schema Verification

After startup, you should have these tables:
- PERMISSIONS (with 4 default permissions)
- DOMAINS (with 7 default domains)  
- PROJECTS (with 4 default projects)
- TESTERS (with 4 default testers)
- USERS
- TEST_CASES
- JIRA_ISSUES (new)
- JIRA_TEST_CASES (new)

## 🎯 Next Steps

Once the basic setup is working:
1. Add your Jira credentials to application.properties
2. Test Jira integration endpoints
3. Build the frontend to interact with the Manual Page APIs