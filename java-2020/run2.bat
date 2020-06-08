#cd %wd%

#@ECHO off

#SET wd=D:\workspace\java-2020
#SET run=%wd%

#SET req=%run%;%wd%\lib;%wd%\artifacts

#@ECHO on

#java --module-path %req% --add-modules ru.ifmo.rain.levashov.implementor -m info.kgeorgiy.java.advanced.implementor %1 ru.ifmo.rain.levashov.implementor.Implementor %2

#java -cp . -p . --module-path D:\workspace\java-2020;D:\workspace\java-2020\lib;D:\workspace\java-2020\artifacts -m info.kgeorgiy.java.advanced.implementor jar-class ru.ifmo.rain.levashov.implementor.Implementor