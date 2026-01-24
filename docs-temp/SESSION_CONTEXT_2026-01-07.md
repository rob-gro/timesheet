# Session Context - 2026-01-07

## IMPORTANT: Read this entire file before continuing work

This file contains the complete context of the work session that was interrupted. Use this to restore full context.

---

## Current Branch & Status

**Branch:** `feature/password-reset-flow`
**Status:** IN PROGRESS - Task #7 partially implemented

```bash
git branch
# Should show: * feature/password-reset-flow
```

---

## User Preferences & Workflow

### Commit Guidelines
- NO Claude footer in commits
- Concise, professional commit messages
- User provides exact text if message is "prostacki" (too simple)
- Step-by-step approval required ("krok po kroku musze akceptować")
- Follow patterns from `.claude/commands/01_instructions.md` and `.claude/commands/think.md`

### Development Workflow
1. Create feature branch for larger features
2. Modify entity classes first (Hibernate auto-creates columns on dev)
3. Test on dev environment
4. Before commit: create Flyway migration V*.sql + ROLLBACK script for prod
5. Everything in one batch commit per feature

### Git Workflow
- **Dev**: Flyway disabled, Hibernate `ddl-auto=update` (auto schema changes)
- **Prod**: Flyway enabled, Hibernate `ddl-auto=validate` (requires migrations)
- Push to GitHub master triggers Heroku deployment
- Use `git pull --rebase` for linear history

---

## Project Overview

**Tech Stack:**
- Spring Boot 3.2.0, Java 17+
- MariaDB (dev: robgro_test_invoices, prod: robgro_aga_invoices)
- HikariCP 5.0.1
- Spring Security with JWT
- Thymeleaf templates
- Flyway migrations (prod only)

**Databases:**
- Dev: `robgro_test_invoices` @ mysql-robgro.alwaysdata.net:3306
- Prod: `robgro_aga_invoices` @ mysql-robgro.alwaysdata.net:3306

---

## Completed Tasks

### Task #2: Hikari Connection Pool Configuration ✅
**Commit:** Already pushed to master
**Files Modified:**
- `src/main/resources/application.properties` - baseline config (max 5, min 2)
- `src/main/resources/application-dev.properties` - dev overrides (max 3, min 1)
- `src/main/resources/application-prod.properties` - prod overrides (max 15, min 5)

### Task #9: Access Control Broadening ✅
**Commit:** Already pushed to master
**Message:** "Broadened access control from ADMIN-only to ADMIN and USER roles for client management operations."
**Files Modified:**
- `ClientViewController.java` - Added `@PreAuthorize("hasAnyRole('ADMIN','USER')")`

### Task #8: Client Activate/Deactivate UI ✅
**Commit:** Already pushed to master
**Message:** "feat: implement client activate/deactivate UI"
**Files Modified:**
- `ClientRepository.java` - Added `findAllOrderByActiveAndName()`
- `ClientService.java` - Added `setActiveStatus()` and `getAllClients(boolean)`
- `ClientServiceImpl.java` - Implemented both methods
- `ClientViewController.java` - Added activate/deactivate endpoints, role-based filtering
- `clients/list.html` - Status column, badges, toggle buttons, row styling
- `clients.js` - Replaced with `toggleClientActive()` function

**Functionality:**
- Admin sees all clients (active + inactive)
- User sees only active clients
- Inactive clients grayed out (opacity 0.6)
- Deactivate button for active, Activate button for inactive (Admin only)

---

## CURRENT WORK: Task #7 - Password Reset Flow

### Status: PARTIALLY IMPLEMENTED (NOT COMMITTED)

**Goal:** Force password change after admin resets password

**What's Done:**

#### 1. Entity Modified: `User.java`
**Location:** `src/main/java/dev/robgro/timesheet/user/User.java`

Added import:
```java
import java.time.LocalDateTime;
```

Added fields (after `defaultSeller`, before `@ManyToMany`):
```java
@Column(name = "requires_password_change", nullable = false)
private boolean requiresPasswordChange = false;

@Column(name = "temp_password_expires_at")
private LocalDateTime tempPasswordExpiresAt;

@Column(name = "last_password_changed_at")
private LocalDateTime lastPasswordChangedAt;
```

#### 2. Service Modified: `UserServiceImpl.java`
**Location:** `src/main/java/dev/robgro/timesheet/user/UserServiceImpl.java`

Added import:
```java
import java.time.LocalDateTime;
```

Modified `resetPassword()` method (lines 141-155):
```java
@Transactional
@Override
public String resetPassword(Long id) {
    User user = userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("User ", id));

    String tempPassword = generateRandomPassword();
    user.setPassword(passwordEncoder.encode(tempPassword));
    user.setRequiresPasswordChange(true);
    user.setTempPasswordExpiresAt(LocalDateTime.now().plusHours(24));

    userRepository.save(user);
    log.info("Password reset for user {}, temp password expires in 24h", id);
    return tempPassword;
}
```

#### 3. Compilation Status
✅ **BUILD SUCCESS** (verified with `mvn clean compile -DskipTests`)

Hibernate will auto-create the 3 new columns when app starts on dev.

---

## NEXT STEPS for Task #7

### Still TODO:

1. **Test on dev environment**
   - Start app, verify Hibernate creates columns
   - Check logs for schema updates
   - Test password reset function

2. **Add password change endpoint** (UserController.java)
   - New endpoint: `POST /api/v1/users/{id}/change-password-required`
   - Verify current temp password
   - Check expiry (24h)
   - Set new password
   - Clear `requiresPasswordChange` flag
   - Set `lastPasswordChangedAt`

3. **Create DTO** (PasswordChangeDto.java)
   ```java
   public record PasswordChangeDto(
       String currentPassword,
       String newPassword,
       String confirmPassword
   ) {}
   ```

4. **Modify AuthServiceImpl.java**
   - Add `requiresPasswordChange` to JWT response
   - Frontend will check this flag after login

5. **Create Frontend Templates**
   - `change-password-required.html` - form for password change
   - Update login flow to redirect if `requiresPasswordChange == true`

6. **Before Commit: Create Flyway Migration**
   - Create folder: `src/main/resources/db/migration/password-reset/`
   - File: `V15__add_password_reset_fields.sql`
   ```sql
   -- Add password reset fields to users table
   ALTER TABLE users
   ADD COLUMN requires_password_change BOOLEAN DEFAULT false NOT NULL AFTER email,
   ADD COLUMN temp_password_expires_at DATETIME NULL AFTER requires_password_change,
   ADD COLUMN last_password_changed_at DATETIME NULL AFTER temp_password_expires_at;

   -- Set existing users to not require change
   UPDATE users SET requires_password_change = false WHERE requires_password_change IS NULL;
   ```

7. **Create Rollback Script**
   - File: `ROLLBACK_password_reset_PRODUCTION.sql`
   - Pattern: Follow `ROLLBACK_seller_migrations_PRODUCTION.sql`
   - Steps:
     1. Drop columns
     2. Verify schema
     3. Delete Flyway record for V15
     4. Transaction with manual COMMIT/ROLLBACK

---

## Task #6: Email Temp Password (AFTER #7)

Depends on Task #7 completion (needs `requiresPasswordChange` field).

**Plan:**
1. Create email template: `templates/email/password-reset.html`
2. Add method to `EmailMessageService.java`: `sendPasswordResetEmail()`
3. Modify `UserController.resetPassword()` to send email
4. Update `users.js` to show "Email sent to..." confirmation

---

## Task #4: Invoice Numbering (INDEPENDENT)

Can be done in parallel after #6+#7.

**Goal:** Change format from `nnn-MM-yyyy` to `INV-yyyy-MM-nnn`

**Changes:**
- `InvoiceNumberGeneratorImpl.java` - new format logic
- `InvoiceRepository.java` - add `findByInvoiceNumberStartingWith()`
- `V16__invoice_number_format_change.sql` - document format change
- Tests update

---

## Important Files & Locations

### Configuration
- `application.properties` - common config
- `application-dev.properties` - dev (Flyway OFF, Hibernate update)
- `application-prod.properties` - prod (Flyway ON, Hibernate validate)

### Migrations
- `src/main/resources/db/migration/seller-entity/` - seller migrations (V9-V14)
- `src/main/resources/db/migration/email-tracking/` - tracking migrations (V1-V3)
- **Next:** `src/main/resources/db/migration/password-reset/` - V15

### Rollback Scripts
- `ROLLBACK_seller_migrations_PRODUCTION.sql` - example pattern
- `ROLLBACK_seller_migrations_TEST.sql` - test db version

### User Management
- `User.java` - entity (MODIFIED - not committed)
- `UserServiceImpl.java` - service (MODIFIED - not committed)
- `UserController.java` - REST controller (NEXT to modify)
- `UserViewController.java` - Thymeleaf controller

---

## Git Status

```bash
# Current branch
* feature/password-reset-flow

# Modified files (NOT STAGED, NOT COMMITTED):
modified:   src/main/java/dev/robgro/timesheet/user/User.java
modified:   src/main/java/dev/robgro/timesheet/user/UserServiceImpl.java

# Untracked files (ignore these):
CHECK_PROD_tracking_schema.sql
EXECUTE_NOW_production_database.sql
EXECUTE_NOW_test_database.sql
ROLLBACK_seller_migrations_PRODUCTION.sql
ROLLBACK_seller_migrations_TEST.sql
SELLER_ENTITY_CHANGES_REVIEW.md
SELLER_ENTITY_CLASSES_LIST.md
TEST_TRACKING_LOCALLY.md
nul
src/main/resources/application.properties.bak
test-coverage-report.html
test-output.log
```

---

## User's Original Requirements (Tickets)

### MUST HAVE (Priority Order)
1. ~~#2 Hikari - Connection pool config~~ ✅ DONE
2. ~~#8 Client Deactivation - Activate/deactivate UI~~ ✅ DONE
3. **#7 Password Change - Force change on temp password** 🔄 IN PROGRESS
4. **#6 Email Temp Password - Send reset email** 📋 PENDING
5. **#4 Invoice Numbering - INV-yyyy-MM-nnn format** 📋 PENDING

### SHOULD HAVE
- #1, #3, ~~#9~~, #11, #12

### NICE TO HAVE
- #5, #14, #13

### DONE
- ~~#10~~ (seller default for CRON - already complete)
- ~~#9~~ (access control - already complete)

---

## Migration Pattern Reference

### V15 Migration Structure (to create):
```sql
-- Header comment explaining what and why
-- Add columns with proper types and constraints
-- AFTER clause for column ordering (optional but nice)
-- UPDATE existing rows if needed
-- Add indexes if needed (optional for these fields)
```

### Rollback Structure (to create):
```sql
START TRANSACTION;

-- Safety check
SELECT DATABASE() as current_database;

-- Step 1: Drop columns
ALTER TABLE users DROP COLUMN IF EXISTS requires_password_change;
ALTER TABLE users DROP COLUMN IF EXISTS temp_password_expires_at;
ALTER TABLE users DROP COLUMN IF EXISTS last_password_changed_at;

-- Step 2: Verify
SELECT * FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'robgro_aga_invoices'
  AND TABLE_NAME = 'users'
  AND COLUMN_NAME LIKE '%password%';

-- Step 3: Flyway cleanup
DELETE FROM flyway_schema_history WHERE version = '15';

-- Final message
SELECT 'Review output above, then: COMMIT; or ROLLBACK;' AS NEXT_STEPS;

-- DO NOT AUTO-COMMIT!
```

---

## Context Restore Commands

When Claude Code restarts, execute these commands to understand state:

```bash
# Check current branch
git branch

# Check what's modified
git status

# See the changes
git diff src/main/java/dev/robgro/timesheet/user/User.java
git diff src/main/java/dev/robgro/timesheet/user/UserServiceImpl.java

# Verify compilation
mvn clean compile -DskipTests

# Check properties files
cat src/main/resources/application-dev.properties | grep flyway
cat src/main/resources/application-prod.properties | grep flyway
```

---

## User Interaction Notes

- User prefers Polish communication but understands English technical terms
- Expects step-by-step approval for each change
- Will reject edits if they don't align with vision
- Appreciates reference to previous patterns (seller implementation, etc.)
- Will provide exact commit message text if not satisfied
- Uses "poczekaj" (wait) when needs to think/review

---

## Key Technical Decisions

1. **Password expiry:** 24 hours from reset
2. **Field names:** Snake case in DB (`requires_password_change`), camelCase in Java
3. **Nullable:** `requiresPasswordChange` NOT NULL (default false), others nullable
4. **Logging:** Info level for password resets with user ID
5. **Pattern:** Similar to seller entity implementation (3 fields, transaction, logging)

---

## Resume Instructions for New Claude Session

1. Read this entire file
2. Check git branch: `git branch` (should be on `feature/password-reset-flow`)
3. Check git status: `git status` (should see User.java and UserServiceImpl.java modified)
4. Ask user: "Kontynuujemy Task #7 od miejsca gdzie skończyliśmy? Co chcesz zrobić dalej?"
5. Reference this file when user asks about context or previous decisions

---

## Last Known State Before Interruption

**Time:** 2026-01-07 ~21:06 UTC
**Action:** User needed to fix Claude Code auto-update issue
**Reason:** File `libvips-42.dll` locked, required Claude Code restart
**Next Step After Restart:** Continue with Task #7 implementation (step 1: test on dev)

---

END OF SESSION CONTEXT FILE
