@echo off

SET root=%cd%\..\..\..\..\..\..\..\..
SET req=%root%\java-2020\lib
SET wd=%cd%

cd ..

javac --module-path %req% --add-modules junit -d compiled *.java tests/*.java

cd %wd%