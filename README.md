# QA Automation Coverage Dashboard - Role-Based Authentication System

A Spring Boot backend application with role-based authentication for QA automation dashboard management.

## Features

### 🔐 Role-Based Authentication
- **Admin**: Full access with read and write permissions
- **Maintainer**: Limited access with read-only permissions
- JWT token-based authentication
- Secure password encryption using BCrypt

### 👥 User Roles

#### Admin Role
- Full CRUD operations on all resources
- Create, update, and delete testers
- Manage system configurations
- Access to all API endpoints

#### Maintainer Role
- Read-only access to data
- View all testers and statistics
- Search and filter capabilities
- Generate reports and monitor metrics

## Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.6 or higher

### Installation

1. **Clone the repository**
```bash
git clone <repository-url>
cd automation-dashboard-service
```

2. **Build the application**
```bash
mvn clean install
```

3. **Run the application**
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8000`

### Default User Accounts

The system creates default users on startup:

| Username | Password | Role | Email |
|----------|----------|------|--------|
| admin | admin123 | ADMIN | admin@qa.automation.com |
| maintainer | maintainer123 | MAINTAINER | maintainer@qa.automation.com |

## API Endpoints

### Authentication Endpoints

#### Login
```http
POST /api/auth/signin
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "id": 1,
  "username": "admin",
  "name": "System Administrator",
  "email": "admin@qa.automation.com",
  "role": "ADMIN"
}
```

#### Register
```http
POST /api/auth/signup
Content-Type: application/json

{
  "name": "John Doe",
  "username": "johndoe",
  "email": "john@example.com",
  "password": "password123",
  "role": "MAINTAINER",
  "gender": "Male",
  "experience": 3
}
```

### Protected Endpoints

All API endpoints require authentication except `/api/auth/**`. Include the JWT token in the Authorization header:

```http
Authorization: Bearer <jwt-token>
```

#### User Information
```http
GET /api/user/me
Authorization: Bearer <jwt-token>
```

#### Testers Management

| Endpoint | Method | Admin | Maintainer | Description |
|----------|--------|-------|------------|-------------|
| `/api/testers` | GET | ✅ | ✅ | Get all testers |
| `/api/testers` | POST | ✅ | ❌ | Create new tester |
| `/api/testers/{id}` | GET | ✅ | ✅ | Get tester by ID |
| `/api/testers/{id}` | PUT | ✅ | ❌ | Update tester |
| `/api/testers/{id}` | DELETE | ✅ | ❌ | Delete tester |
| `/api/testers/search` | GET | ✅ | ✅ | Search testers |

## Security Configuration

### JWT Configuration
- **Secret Key**: Configurable via `app.jwtSecret` property
- **Token Expiration**: 24 hours (configurable via `app.jwtExpirationMs`)
- **Algorithm**: HS256

### Password Security
- Passwords are encrypted using BCrypt
- Minimum password length: 6 characters
- Maximum password length: 40 characters

### CORS Configuration
- Configured for Angular frontend (`http://localhost:4200`)
- Supports credentials and authorization headers
- Allows all standard HTTP methods

## Database Configuration

### H2 In-Memory Database (Development)
- **URL**: `jdbc:h2:mem:qa_automation`
- **Console**: Available at `http://localhost:8000/h2-console`
- **Username**: `sa`
- **Password**: (empty)

### Schema
The `Tester` entity includes authentication fields:
- `username` (unique)
- `password` (encrypted)
- `email` (unique)
- `enabled` (account status)

## Frontend Integration

### Basic Web Interface
Access the basic web interface at `http://localhost:8000` to test the authentication system.

### Angular Frontend Integration
For your Angular frontend, implement the following:

1. **Authentication Service**
```typescript
// auth.service.ts
@Injectable()
export class AuthService {
  private tokenKey = 'authToken';
  
  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/api/auth/signin', credentials);
  }
  
  logout(): void {
    localStorage.removeItem(this.tokenKey);
  }
  
  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }
  
  isAuthenticated(): boolean {
    return !!this.getToken();
  }
  
  getUserRole(): string | null {
    // Decode JWT token to get user role
    const token = this.getToken();
    if (token) {
      // Implement JWT decoding logic
    }
    return null;
  }
}
```

2. **HTTP Interceptor**
```typescript
// auth.interceptor.ts
@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private authService: AuthService) {}
  
  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const token = this.authService.getToken();
    
    if (token) {
      req = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    }
    
    return next.handle(req);
  }
}
```

3. **Route Guards**
```typescript
// admin.guard.ts
@Injectable()
export class AdminGuard implements CanActivate {
  constructor(private authService: AuthService, private router: Router) {}
  
  canActivate(): boolean {
    if (this.authService.isAuthenticated() && this.authService.getUserRole() === 'ADMIN') {
      return true;
    }
    this.router.navigate(['/login']);
    return false;
  }
}
```

4. **Role-Based UI Components**
```typescript
// role-based.directive.ts
@Directive({
  selector: '[appHasRole]'
})
export class HasRoleDirective implements OnInit {
  @Input() appHasRole: string[] = [];
  
  constructor(
    private templateRef: TemplateRef<any>,
    private viewContainer: ViewContainerRef,
    private authService: AuthService
  ) {}
  
  ngOnInit(): void {
    const userRole = this.authService.getUserRole();
    
    if (userRole && this.appHasRole.includes(userRole)) {
      this.viewContainer.createEmbeddedView(this.templateRef);
    } else {
      this.viewContainer.clear();
    }
  }
}
```

**Usage in templates:**
```html
<!-- Admin only content -->
<div *appHasRole="['ADMIN']">
  <button (click)="deleteUser()">Delete User</button>
</div>

<!-- Both Admin and Maintainer -->
<div *appHasRole="['ADMIN', 'MAINTAINER']">
  <app-user-list></app-user-list>
</div>
```

## Configuration

### Application Properties
```properties
# JWT Configuration
app.jwtSecret=mySecretKey
app.jwtExpirationMs=86400000

# CORS Configuration
spring.web.cors.allowed-origins=http://localhost:4200
spring.web.cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
spring.web.cors.allowed-headers=*
spring.web.cors.allow-credentials=true
```

### Environment Variables
For production, set these environment variables:
- `JWT_SECRET`: Your JWT secret key
- `JWT_EXPIRATION_MS`: Token expiration time in milliseconds
- `DATABASE_URL`: Database connection URL
- `DATABASE_USERNAME`: Database username
- `DATABASE_PASSWORD`: Database password

## Testing the Authentication System

1. **Start the application**
```bash
mvn spring-boot:run
```

2. **Open the web interface**
Navigate to `http://localhost:8000`

3. **Test login with different roles**
- Login as admin: `admin` / `admin123`
- Login as maintainer: `maintainer` / `maintainer123`

4. **Verify role-based access**
- Admin can see both admin and maintainer sections
- Maintainer can only see maintainer section
- Test API endpoints to verify permissions

## Development

### Adding New Roles
To add new roles:

1. Update the `RegisterRequest` validation pattern
2. Add role constants
3. Update security configuration
4. Add role-specific endpoints

### Customizing Permissions
Modify the `@PreAuthorize` annotations in controllers to customize access permissions:

```java
@PreAuthorize("hasRole('ADMIN') or (hasRole('MAINTAINER') and #id == authentication.principal.id)")
public ResponseEntity<?> updateOwnProfile(@PathVariable Long id, @RequestBody UpdateProfileRequest request) {
    // Allow maintainers to update their own profile
}
```

## Troubleshooting

### Common Issues

1. **CORS Errors**
   - Ensure your frontend URL is in the allowed origins
   - Check that credentials are allowed

2. **JWT Token Issues**
   - Verify the secret key is properly configured
   - Check token expiration settings

3. **Authentication Failures**
   - Ensure passwords meet minimum requirements
   - Check that usernames/emails are unique

### Logs
Enable debug logging for security:
```properties
logging.level.org.springframework.security=DEBUG
logging.level.com.qa.automation=DEBUG
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Implement your changes
4. Add tests
5. Submit a pull request

## License

This project is licensed under the MIT License.
