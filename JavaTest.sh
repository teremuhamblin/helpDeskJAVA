# 1. Compiler le code principal dans bin/
javac -d bin src/main/java/model/*.java src/main/java/service/*.java src/main/java/*.java

# 2. Compiler les tests dans bin-test/ en s'appuyant sur bin/
javac -cp bin -d bin-test src/test/java/HelpdeskTest.java

# 3. Lancer les tests
java -cp "bin:bin-test" HelpdeskTest
