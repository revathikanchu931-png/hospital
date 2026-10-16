@echo off
setlocal

set "WRAPPER_JAR=%~dp0.mvn\wrapper\maven-wrapper.jar"

if not exist "%WRAPPER_JAR%" (
    echo Maven Wrapper JAR is missing.
    exit /b 1
)

java "-Dmaven.multiModuleProjectDirectory=%~dp0" -classpath "%WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*