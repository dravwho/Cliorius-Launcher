# Contributing to Cliorius Launcher

Thanks for taking the time to contribute. This document explains how to report
problems, propose features, and submit code.

## Reporting bugs

Open an issue and include:

1. **Device information** — manufacturer, model, Android version, and CPU
   architecture (arm64-v8a, armeabi-v7a, x86, x86_64).
2. **Launcher version** — visible in *Settings → About*.
3. **Minecraft version and mod loader** you were launching.
4. **Renderer** selected in *Settings → Video and renderer*.
5. **Steps to reproduce** — what you tapped, in order.
6. **Logs** — use *Settings → Share log file* and attach the result. If the app
   crashed, `latestcrash.txt` in the game directory contains the stack trace.

Please search existing issues before opening a new one.

## Proposing features

Open an issue with the `enhancement` label. Describe the problem you are trying
to solve rather than only the solution you have in mind — it makes it much easier
to find the right approach.

## Submitting code

1. Fork the repository and create a branch from `main`.
2. Keep commits focused; one logical change per commit.
3. Match the existing code style:
   - Kotlin for new UI and version-management code.
   - Java where you are extending existing Pojav engine classes.
   - Four-space indentation, no tabs.
   - Comments in English.
4. Build before pushing:

   ```bash
   echo "sdk.dir=/path/to/android-sdk" > local.properties
   JAVA_HOME=/path/to/jdk17 ./gradlew -Darch=arm64 assembleDebug
   ```

5. Open a pull request describing what changed and why. Reference the issue it
   closes, if any.

### What we look for in a pull request

- The change actually works — include a screenshot or log excerpt when relevant.
- No new emoji in the user interface; use vector drawables.
- No hard-coded strings; add them to `res/values/strings.xml`.
- No new permissions unless they are strictly required.
- Performance settings must reach the JVM or renderer for real. Placeholder
  switches that do nothing are rejected.

## Translation

UI strings live in `app/src/main/res/values/strings.xml`. To add a language,
create `values-<locale>/strings.xml` with the translated keys and open a pull
request. Partial translations are welcome.

## License

By contributing you agree that your work is licensed under the GNU General
Public License v3.0, the same license as the rest of the project.
