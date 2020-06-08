@echo off
SET root=%cd%\..\..\..\..\..\..\..\..
SET req=%root%\java-2020\lib

cd ..

javac --module-path %req% --add-modules junit -d compiled *.java tests/*.java

java --module-path %req% --add-modules junit -cp compiled org.junit.runner.JUnitCore ru.ifmo.rain.levashov.bank.tests.Tests

echo Status code: %errorlevel%

cd scripts
