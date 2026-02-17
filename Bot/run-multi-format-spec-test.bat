@echo off
REM Script to run the multi-format specification upload property test
REM Requires Maven to be installed and in PATH

echo Running Multi-Format Specification Upload Property Test...
echo.

mvn test -Dtest=MultiFormatSpecificationUploadPropertyTest

if %ERRORLEVEL% EQU 0 (
    echo.
    echo Test completed successfully!
) else (
    echo.
    echo Test failed or Maven is not installed.
    echo Please install Maven from https://maven.apache.org/download.cgi
)

pause
