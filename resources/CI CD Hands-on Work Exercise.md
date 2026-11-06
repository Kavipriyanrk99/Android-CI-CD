# Android + Jetpack Compose: CI/CD Hands-on Work Exercise



Maximum duration: 5 hours

Beginner to intermediate



Project: Build an Android CI/CD pipeline from scratch using Gradle, GitHub Actions, secure release signing, and Firebase App Distribution.

This exercise is designed around the Android CI/CD study guide you shared. Instead of learning each topic in isolation, you'll implement one realistic workflow that brings the modules together.

By the end, you should be able to honestly say in an interview:

> I created an Android Compose application, configured Gradle build variants, implemented release signing using environment variables, automated testing and builds using GitHub Actions, and distributed a release APK to testers using Firebase App Distribution.

You will build this on a throwaway repository. You do not need to build a production-quality application. The objective is to understand the complete CI/CD lifecycle.

## 1. What you are going to build

## TaskFlow – Android Compose App

[Snaplist - Full UI Collection for a Task Management App by Mohammad Mahdi Jafari on Dribbble](https://images.openai.com/static-rsc-4/0TE4xI7oHPgSjqBmYDoQMw_CWlNXwtsfgw3AHTpk-_AwshDhafydLPXSFs4B36VvLL8Uyh7BG2md9zR8-6lfCLLKEr-I9E-8eQJoCfQ5CUm3t0XFobGfpqCbePKt999kWjzL1ccmdKfYvmNLgIxUWy28IVvX9b14yTfwH0Rv_go?purpose=inline)

[dribbble.com](https://dribbble.com/shots/24373822-Snaplist-Full-UI-Collection-for-a-Task-Management-App?utm_source=chatgpt.com)

A simple task management application

The application should have:

- A home screen displaying a list of tasks.
- An Add Task button.
- A way to mark tasks as completed.
- A simple task counter.
- Different app configurations for development and production.

The final CI/CD architecture

Developer pushes code / opens PR

GitHub Actions – CI

Checkout → JDK → Gradle cache

Unit Tests → Lint → Release Build

Upload APK and Reports

CI quality gate

Developer pushes v1.0.0 tag

GitHub Actions – Release

Decode keystore → Sign → Build APK + AAB

Firebase App Distribution

QA receives installable APK

## 2. Five-hour execution plan

| Time        | Task                                               | Duration |
| ----------- | -------------------------------------------------- | -------- |
| 00:00–00:30 | Create Compose app and understand Gradle           | 30 min   |
| 00:30–01:10 | Configure build types, flavors and variants        | 40 min   |
| 01:10–01:50 | Configure release signing and secrets              | 40 min   |
| 01:50–03:10 | Implement GitHub Actions CI pipeline               | 80 min   |
| 03:10–04:20 | Implement release pipeline and Firebase            | 70 min   |
| 04:20–05:00 | Troubleshooting, verification and interview review | 40 min   |
| Total       |                                                    | 300 min  |

The time limit assumes you have Android Studio, a GitHub account, and basic familiarity with creating a Compose project. Firebase project creation and permissions should ideally be ready before starting.

# 3. Detailed work assignments

## Task 1 – Create the Android application and explore Gradle

30 minutes

Objective: Understand what Gradle does and identify the important files in an Android project.

### Your work

1. Create a new Empty Activity project using Jetpack Compose.
2. Name it `TaskFlow`.
3. Set the package name to `com.example.taskflow`.
4. Push the initial project to a new GitHub repository.
5. Open the terminal and execute the following commands.

```
./gradlew tasks
./gradlew tasks --all

./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug

./gradlew assembleDebug --dry-run
```

### Answer these questions in your notes

- What is the difference between Gradle and AGP?
- Why do we use `./gradlew` instead of `gradle`?
- What is a Gradle task?
- What is the difference between `assemble`, `check` and `build`?
- Where is the generated debug APK?
- Where are unit test results stored?
- What is the difference between unit tests and instrumented tests?

### Expected output

Gradle exploration completed

- Identified `settings.gradle.kts`.
- Identified root `build.gradle.kts`.
- Identified `app/build.gradle.kts`.
- Identified `gradle.properties`.
- Identified `gradle/libs.versions.toml`.
- Successfully generated a debug APK.
- Located test and lint reports.

## Task 2 – Configure build types, flavors and variants

40 minutes

Objective: Learn how to produce different Android builds from the same codebase.

You must configure:

Build types

- `debug`
- `release`

Product flavors

- `dev`
- `prod`

Use one flavor dimension called `environment`.

### Your implementation

Modify `app/build.gradle.kts` to configure:

| Configuration  | Dev                        | Prod                   |
| -------------- | -------------------------- | ---------------------- |
| Application ID | `com.example.taskflow.dev` | `com.example.taskflow` |
| Base URL       | Dev API URL                | Production API URL     |
| Build types    | Debug + Release            | Debug + Release        |

Use these sample URLs:

```
Dev:  https://dev-api.example.com
Prod: https://api.example.com
```

Configure the flavors using `buildConfigField`.

Remember that AGP 8+ requires:

```
buildFeatures {
    buildConfig = true
}
```

Enable release minification and resource shrinking. Configure the default optimized R8 rules file and `proguard-rules.pro`.

### Your verification

Run:

```
./gradlew tasks --all

./gradlew assembleDevDebug
./gradlew assembleProdDebug

./gradlew assembleDevRelease
./gradlew assembleProdRelease
```

Inspect the generated APKs.

Questions to answer:

1. What is a build type?
2. What is a product flavor?
3. What is a build variant?
4. What is the difference between APK and AAB?
5. What does R8 do?
6. Why can a release build fail even when debug works?

Acceptance criteria

- Four variants are generated.
- Dev and prod have different application IDs.
- Dev and prod have different `BASE_URL` values.
- Release builds have R8 minification enabled.
- `bundleProdRelease` successfully generates an AAB.

## Task 3 – Release signing and secrets management

40 minutes

Objective: Understand how to sign an Android release build and protect credentials.

### Part A: Generate a release keystore

Execute:

```
keytool -genkeypair -v \
  -keystore release.jks \
  -storetype PKCS12 \
  -alias upload \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

Use a password that you store securely.

Inspect the keystore:

```
keytool -list -v -keystore release.jks
```

### Part B: Configure Gradle signing

In `app/build.gradle.kts`, create a release signing configuration.

Your configuration must read these environment variables:

```
KEYSTORE_PATH
KEYSTORE_PASSWORD
KEY_ALIAS
KEY_PASSWORD
```

Important: Never hardcode passwords in Gradle files.

Configure release signing conditionally so that a local unsigned release build still works without a keystore.

### Part C: Test signing locally

Set the environment variables in your terminal and run:

```
./gradlew assembleProdRelease
```

Verify the generated APK:

```
apksigner verify --print-certs \
  app/build/outputs/apk/prod/release/app-prod-release.apk
```

Adjust the output path to the actual path Gradle generates for your variant.

### Part D: Protect the keystore

Add these entries to `.gitignore`:

```
*.jks
*.keystore
keystore.properties
```

Base64 encode your keystore.

```
base64 -w 0 release.jks > release.jks.b64
```

On macOS, use:

```
base64 -i release.jks -o release.jks.b64
```

Never commit the `.jks` file or `.b64` file.

### Questions to answer

- What is a keystore?
- What is a key alias?
- What is the difference between store password and key password?
- Why can't you use the debug keystore for production?
- Why do we base64 encode the keystore for GitHub Secrets?
- What is Play App Signing?
- What is the difference between an upload key and an app signing key?

Acceptance criteria

- Generated a release keystore.
- Successfully signed a release APK.
- Verified the APK signature.
- No passwords are hardcoded.
- Keystore files are excluded from Git.
- Local builds work without release secrets.

## Task 4 – Create the GitHub Actions CI pipeline

80 minutes

Objective: Automate testing, linting and APK generation whenever code is pushed or a pull request is created.

This is the most important section of the exercise.

### Part A: Create your workflow

Create this directory structure:

```
TaskFlow/
 ├── .github/
 │    └── workflows/
 │         ├── ci.yml
 │         └── release.yml
 ├── app/
 ├── gradle/
 ├── build.gradle.kts
 └── settings.gradle.kts
```

Create `.github/workflows/ci.yml`.

### Part B: Implement the CI workflow

Your workflow must:

1. Trigger on pull requests.
2. Trigger on pushes to `main`.
3. Use an Ubuntu runner.
4. Checkout the repository.
5. Set up JDK 17.
6. Configure Gradle caching.
7. Make `gradlew` executable.
8. Run unit tests.
9. Run Android Lint.
10. Assemble an unsigned release APK.
11. Upload test and lint reports, even if a previous step fails.
12. Upload the generated APK.

Use these as your starting commands:

```
./gradlew testDebugUnitTest --stacktrace

./gradlew lintDebug --stacktrace

./gradlew assembleProdRelease --stacktrace
```

For your workflow, use the corresponding artifact path for your actual variant.

### Part C: Add the following GitHub Actions features

Your CI workflow must demonstrate these concepts:

| Feature           | Your assignment                                 |
| ----------------- | ----------------------------------------------- |
| `on`              | Configure `push` and `pull_request`             |
| `runs-on`         | Use `ubuntu-latest`                             |
| `steps`           | Separate setup, verification and artifact steps |
| `uses`            | Use checkout, setup-java and setup-gradle       |
| `run`             | Execute Gradle commands                         |
| `permissions`     | Set `contents: read`                            |
| `timeout-minutes` | Set a 30-minute limit                           |
| `concurrency`     | Cancel outdated PR runs                         |
| `if: always()`    | Upload reports after failures                   |
| `upload-artifact` | Save APK and reports                            |

Use the action versions from your supplied guide as the initial reference, and verify their current compatibility before implementation.

### Part D: Test the workflow

Perform the following activities:

Test 1 – Successful build

- Push your initial implementation.
- Open GitHub → Actions.
- Confirm the workflow runs successfully.
- Download the APK artifact.
- Install it on an Android device or emulator.

Test 2 – Failed unit test

Intentionally create a failing unit test.

For example:

```
@Test
fun failingTest() {
    assertEquals(10, 5)
}
```

Push the change.

Observe:

- Does the workflow fail?
- Does the test step show the error?
- Are the reports uploaded despite the failure?

Fix the test and push again.

Test 3 – Pull request

- Create a feature branch.
- Make a small Compose UI change.
- Push the branch.
- Open a pull request against `main`.
- Verify that CI runs on the PR.

Test 4 – Branch protection

Configure the `verify` job as a required status check on `main`, where your repository's branch protection settings support it.

### Part E: Understand workflow architecture

Answer these questions:

1. What is the difference between a workflow, job, runner and step?
2. Why are jobs isolated?
3. What is the difference between `run` and `uses`?
4. Why do we use `needs`?
5. What are GitHub Actions artifacts?
6. Why should reports use `if: always()`?
7. Why should a CI workflow not have access to release secrets?
8. What is the difference between a cache and an artifact?
9. What is a matrix build?
10. What happens when a PR comes from a fork?

Acceptance criteria

- CI runs on push.
- CI runs on pull requests.
- Unit tests execute successfully.
- Lint executes successfully.
- Release variant builds successfully without release secrets.
- APK artifact is downloadable.
- Test and lint reports upload on failure.
- A deliberately failing test makes CI red.
- Fixed code makes CI green.
- Required status check is configured where supported.

## Task 5 – Release pipeline and Firebase App Distribution

70 minutes

Objective: Automate the process of producing a signed Android release and delivering it to testers.

This is where CI becomes a practical delivery pipeline.

### Part A: Prepare Firebase

Create or use a Firebase project.

Complete the following:

1. Open Firebase Console.
2. Register your Android app.
3. Use the exact production application ID: `com.example.taskflow`.
4. Open App Distribution.
5. Create a tester group named `qa`.
6. Add your own email as a tester.
7. Find your Firebase App ID in Project Settings.

Remember: the Firebase App ID is not the same as the Android application ID.

You do not need to add Firebase SDK dependencies or `google-services.json` just to distribute an APK.

### Part B: Create a service account

Follow the service-account authentication approach from your guide.

1. Open Firebase Project Settings.
2. Navigate to Service Accounts.
3. Follow the Google Cloud link.
4. Create a service account.
5. Assign the Firebase App Distribution Admin role.
6. Generate a JSON key.
7. Store the JSON contents securely.

Do not commit the JSON key to Git.

### Part C: Configure GitHub Secrets

Go to:

`GitHub Repository → Settings → Secrets and variables → Actions`

Create these six secrets:

| Secret name                | Value                         |
| -------------------------- | ----------------------------- |
| `KEYSTORE_BASE64`          | Base64-encoded keystore       |
| `KEYSTORE_PASSWORD`        | Keystore password             |
| `KEY_ALIAS`                | `upload`                      |
| `KEY_PASSWORD`             | Key password                  |
| `FIREBASE_APP_ID`          | Firebase App ID               |
| `FIREBASE_SERVICE_ACCOUNT` | Complete service-account JSON |

Do not print secrets in workflow logs. Do not upload decoded keystores as artifacts.

### Part D: Implement release.yml

Your release workflow should:

- Trigger when a version tag such as `v1.0.0` is pushed.
- Also support manual execution through `workflow_dispatch`.
- Use Ubuntu and JDK 17.
- Restore the Gradle cache.
- Decode the keystore into `$RUNNER_TEMP`.
- Pass signing credentials as environment variables.
- Build a signed APK.
- Build an AAB.
- Distribute the APK to Firebase group `qa`.
- Upload the AAB and R8 mapping file as artifacts.

Use this workflow outline:

```
name: Android Release

on:
  push:
    tags:
      - 'v*'
  workflow_dispatch:

permissions:
  contents: read

jobs:
  release:
    runs-on: ubuntu-latest
    timeout-minutes: 45

    steps:
      - uses: actions/checkout@v5

      - uses: actions/setup-java@v5
        with:
          distribution: temurin
          java-version: '17'

      - uses: gradle/actions/setup-gradle@v4

      - name: Decode keystore
        env:
          KEYSTORE_B64: ${{ secrets.KEYSTORE_BASE64 }}
        run: |
          echo "$KEYSTORE_B64" | base64 --decode \
            > "$RUNNER_TEMP/release.jks"

      - name: Build signed release
        env:
          KEYSTORE_PATH: ${{ runner.temp }}/release.jks
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
          CI_BUILD_NUMBER: ${{ github.run_number }}
        run: |
          ./gradlew assembleProdRelease bundleProdRelease --stacktrace
```

Complete the remaining steps yourself:

- Firebase distribution.
- Release notes.
- APK upload if needed.
- AAB and mapping upload.

Use the Firebase GitHub Action approach from the guide, with the service-account JSON passed through `serviceCredentialsFileContent`.

### Part E: Test the complete release process

Create a version tag:

```
git tag v1.0.0
git push origin v1.0.0
```

Now observe the release workflow.

Verify these five things:

1. GitHub Actions successfully decodes the keystore.
2. Gradle creates a signed release APK.
3. Gradle creates an AAB.
4. Firebase App Distribution receives the APK.
5. Your email receives the tester invitation or build notification, as applicable.

Install the APK and open TaskFlow.

### Part F: Understand release delivery

Answer:

- Why do we use separate CI and release workflows?
- Why should release signing secrets not be available to fork PRs?
- Why do we use a service account instead of the older Firebase token approach?
- Why does Firebase need the App ID?
- Why does Firebase App Distribution not require the Firebase SDK?
- Why do we retain `mapping.txt`?
- What is the difference between Firebase App Distribution and Play internal testing?
- Why is an AAB not directly installable?

Acceptance criteria

- Firebase project configured.
- Tester group created.
- Service account configured.
- Six GitHub Secrets configured.
- Release workflow triggered by a version tag.
- Signed APK generated.
- AAB generated.
- APK successfully uploaded to Firebase.
- Tester can access and install the APK.
- AAB and mapping file preserved as artifacts.

## Task 6 – Troubleshooting and final verification

40 minutes

Objective: Learn to investigate failures instead of randomly changing Gradle or YAML configurations.

### Part A: Complete these five failure scenarios

| Scenario                                 | What you should investigate                         |
| ---------------------------------------- | --------------------------------------------------- |
| 1. `gradlew` permission denied           | Executable bit and `chmod +x`                       |
| 2. Release signing fails                 | Keystore path, passwords, alias and Base64 decoding |
| 3. Debug works but release crashes       | R8, minification and missing keep rules             |
| 4. Dependencies download on every CI run | Gradle cache configuration                          |
| 5. Firebase upload fails                 | App ID, service account permissions and APK path    |

For each scenario:

1. Reproduce the problem if possible.
2. Identify the failed workflow step.
3. Read the actual error.
4. Add `--stacktrace` where appropriate.
5. Identify the root cause.
6. Fix it.
7. Rerun the pipeline.
8. Document what happened.

### Part B: Learn these troubleshooting commands

```
# Full Gradle error trace
./gradlew assembleProdRelease --stacktrace

# Detailed logging
./gradlew assembleProdRelease --info

# Display task execution graph
./gradlew assembleProdRelease --dry-run

# Force tasks to execute again
./gradlew assembleProdRelease --rerun-tasks

# Run without downloading dependencies
./gradlew assembleProdRelease --offline

# Verify the wrapper
./gradlew --version

# Inspect APK signature
apksigner verify --print-certs app.apk
```

### Part C: Final end-to-end test

Perform this exact sequence:

1. Make a small change to the Compose UI.
2. Push a feature branch.
3. Open a PR.
4. Confirm CI runs tests, lint and release assembly.
5. Merge the PR after successful checks.
6. Create and push the `v1.0.0` tag.
7. Confirm the release workflow runs.
8. Download the signed APK.
9. Verify its signature.
10. Install it on your device.
11. Check Firebase App Distribution for the uploaded release.
12. Verify the AAB and R8 mapping file artifacts.

## Final project completion checklist

Your progress

# 0/21

Compose TaskFlow app created

Gradle commands understood and executed

Debug and release build types configured

Dev and prod product flavors configured

Four build variants generated

APK and AAB generated

R8 configured for release

Release keystore generated

APK signature verified

Secrets kept outside Git

GitHub CI workflow implemented

Push and PR triggers verified

Unit tests and lint integrated

Build artifacts uploaded

Reports uploaded even after failures

Branch protection configured

Release workflow implemented

Firebase service account configured

Signed APK distributed to QA

R8 mapping and AAB preserved

End-to-end release tested

Reset checklist

# 4. What you should be able to explain in an interview

After completing the practical work, you should be able to explain these topics without looking at your notes.

| Topic               | Expected understanding                                     |
| ------------------- | ---------------------------------------------------------- |
| Gradle              | Tasks, task graph, wrapper, caching                        |
| AGP                 | Its relationship with Gradle                               |
| Build configuration | Build types, flavors and variants                          |
| Packaging           | APK versus AAB                                             |
| R8                  | Shrinking, optimization and obfuscation                    |
| Versioning          | `versionCode` versus `versionName`                         |
| Signing             | Keystore, alias, passwords and signatures                  |
| Security            | GitHub Secrets, environment variables and service accounts |
| GitHub Actions      | Workflow, events, jobs, runners and steps                  |
| CI                  | PR checks, unit tests, lint and build verification         |
| Artifacts           | APKs, reports, AABs and mapping files                      |
| Performance         | Gradle caching and concurrency                             |
| Distribution        | Firebase App Distribution versus Play internal testing     |
| Troubleshooting     | Stacktrace, logs and runner differences                    |

## 5. Additional interview challenges

Use the remaining time to answer these five scenarios verbally.

Challenge 1:

Your debug build succeeds, but your release build crashes immediately after opening the application. What would you investigate?

Challenge 2:

Your CI pipeline takes 20 minutes, but your local build takes 4 minutes. What would you investigate to improve performance?

Challenge 3:

A developer accidentally commits a keystore and its passwords to GitHub. What should the team do?

Challenge 4:

Your pull-request workflow requires a Firebase service-account secret. A contributor opens a PR from a fork, and the workflow fails. Why?

Challenge 5:

Your release APK is successfully uploaded to Firebase, but the tester cannot install it. How would you investigate?

Try answering each in 2–3 minutes.

# 6. How to evaluate yourself

Use this scoring system after finishing.

| Area                             | Points |
| -------------------------------- | ------ |
| Gradle and build configuration   | 15     |
| Build variants and R8            | 15     |
| Signing and secrets              | 15     |
| GitHub Actions CI                | 25     |
| Release pipeline and Firebase    | 20     |
| Troubleshooting and explanations | 10     |
| Total                            | 100    |

# Your target score

# 80 / 100

Target for practical interview readiness

- 90–100: Strong practical understanding.
- 80–89: Good interview readiness.
- 65–79: Understand the concepts; more practice needed.
- Below 65: Repeat the failed sections.

# 7. What NOT to do during these five hours

To keep this exercise within the time limit, avoid:

- Building a sophisticated Compose application.
- Implementing a database or authentication system.
- Creating a complex multi-module architecture.
- Setting up an emulator matrix.
- Configuring Play Store production publishing.
- Setting up remote Gradle build caching.
- Learning Bitrise or Codemagic alongside GitHub Actions.

These are additional learning topics, not prerequisites for completing this exercise.

One important time-saving recommendation: Create your Firebase project and service account before the five-hour session. If external account permissions or Firebase setup become blockers, finish the complete signing and release workflow first, and treat the actual Firebase upload as the final integration checkpoint.

# 8. Your final deliverables

At the end, your GitHub repository should look approximately like this:

```
TaskFlow/
│
├── .github/
│   └── workflows/
│       ├── ci.yml
│       └── release.yml
│
├── app/
│   ├── src/
│   │   ├── main/
│   │   ├── test/
│   │   ├── androidTest/
│   │   ├── dev/
│   │   └── prod/
│   │
│   ├── build.gradle.kts
│   └── proguard-rules.pro
│
├── gradle/
│   └── libs.versions.toml
│
├── gradle.properties
├── settings.gradle.kts
├── build.gradle.kts
├── gradlew
└── README.md
```

Your README should contain:

1. Project overview.
2. Technologies used.
3. Build variants available.
4. Gradle commands.
5. CI pipeline explanation.
6. Release pipeline explanation.
7. Secrets required (names only, never values).
8. Screenshots of successful GitHub Actions runs.
9. Screenshot of Firebase App Distribution.
10. Troubleshooting notes.

Keep the actual APK, AAB and mapping file as GitHub artifacts rather than committing them to your repository.

## My recommendation

Follow the supplied guide's learning sequence: Gradle → Build configuration → Signing and secrets → GitHub Actions → Firebase App Distribution. This exercise follows that order deliberately, rather than introducing everything simultaneously.&#x20;

Pasted markdown.md



The most important outcome is not completing all 21 checklist items. It is being able to explain what happens when you push code, how the APK is generated, how signing works, how CI catches failures, and how QA receives the build.

If you finish this exercise successfully, you will have a concrete Android CI/CD project to discuss in a junior Android developer interview, rather than just theoretical knowledge.