# super-star
Personal daily life management

## Spring profiles

- `application.yml`: shared configuration; `dev` is the default profile.
- `application-dev.yml`: local MySQL and localhost service URLs.
- `application-prod.yml`: production values loaded from environment variables.

Local startup:

```powershell
java -jar user/target/user-1.0-SNAPSHOT.jar
java -jar oauth/target/oauth-1.0-SNAPSHOT.jar
```

Production startup:

```powershell
java -jar user/target/user-1.0-SNAPSHOT.jar --spring.profiles.active=prod
java -jar oauth/target/oauth-1.0-SNAPSHOT.jar --spring.profiles.active=prod
```
