@echo off
REM Script to run the unique identifier assignment property test
REM Requires Maven to be installed and in PATH

echo Running Unique Identifier Assignment Property Test...
echo.

mvn test -Dtest=UniqueIdentifierPropertyTest

if %ERRORLEVEL% EQU 0 (
    echo.
    echo Test completed successfully!
) else (
    echo.
    echo Test failed or Maven is not installed.
    echo Please install Maven from https://maven.apache.org/download.cgi
)

pause
