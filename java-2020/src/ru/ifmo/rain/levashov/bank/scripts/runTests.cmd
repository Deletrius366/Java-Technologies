@echo off
SET root=%cd%\..\..\..\..\..\..\..\..
SET req=%root%\java-2020\lib

cd ..

javac --module-path %req% --add-modules junit -d compiled *.java tests/*.java

java --module-path %req% --add-modules junit -cp compiled ru/ifmo/rain/levashov/bank/tests/BankTests

echo Status code: %errorlevel%

cd scripts