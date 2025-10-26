# LocalNews

LocalNews is a Spring Boot web application for local news handling (media/video upload, user authentication, and basic interactions). This repository contains the source code and configuration to build and run the application.

## Requirements
- Java 21 (as declared in `pom.xml`) - please install a compatible JDK
- Maven 3.6+ (for build and test)
- MySQL (or configure a compatible datasource)
- Optional: AWS credentials if you plan to use S3/RDS/SecretsManager features

## Quick start (Windows PowerShell)

1. Build the project:

```powershell
mvn clean package -DskipTests
```

2. Run with Spring Boot plugin (development):

```powershell
mvn spring-boot:run
```

3. Or run the produced jar:

```powershell
java -jar target/local-news.jar
```

4. Run tests:

```powershell
mvn test
```

## Configuration

- Application properties are in `src/main/resources/application.properties` and `application-production.properties`.
- You will need to configure DB connection properties (URL, username, password) and any AWS/S3 credentials if used.
- The app includes JWT-based auth (see `JwtUtil` and related config). Provide a secure JWT secret through configuration or environment.
- The project references AWS SDK (S3, RDS, Secrets Manager). If you enable those features, set AWS credentials via environment variables, the AWS credentials file, or an IAM role when deployed on AWS.

## Notes
- I added a `.gitignore` and removed compiled classes / `target/` from the Git history to keep the repo clean.
- The project uses Lombok; to avoid IDE warnings, enable Lombok support in your IDE.

## Contributing
- Create branches from `main` and open pull requests.

## Contact / Support
If you want me to add a README section specific to deployment (Docker, AWS Elastic Beanstalk, or ECS) or to add CI (GitHub Actions), tell me which option you prefer and I can add it.
# localnewss
news 
