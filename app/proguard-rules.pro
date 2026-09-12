# ProGuard / R8 rules for the release build.
#
# The app itself needs no keep rules: it uses no reflection, no serialisation
# framework and no JNI. Compose, lifecycle and DataStore all ship consumer rules.
#
# Keep rules below are added only when a real need appears, and each must state
# why it exists (see USER_STORIES.md PERF-2).

# Keep crash/stack-trace line numbers useful while diagnosing R8-only failures.
# (Mapping files are written to app/build/outputs/mapping/release/.)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
