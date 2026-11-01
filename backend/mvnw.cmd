@echo off
setlocal

set "WRAPPER_JAR=%~dp0.mvn\wrapper\maven-wrapper.jar"
for %%I in ("%~dp0.") do set "MAVEN_PROJECT_DIR=%%~fI"

if not exist "%WRAPPER_JAR%" (
    echo Maven Wrapper JAR is missing.
    exit /b 1
)

java "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECT_DIR%" -classpath "%WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*
