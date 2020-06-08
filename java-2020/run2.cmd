SET home=D:\workspace\java-2020
SET package=ru.ifmo.rain.levashov.implementor
SET out=out\production\java-2020

cd %home%
javac -d %out% -p lib;artifacts; --module-source-path myModules --module %package%

cd %out%\%package%
jar -c -f %home%\artifacts\%package%.jar -e %package%.Implementor .

cd %home%