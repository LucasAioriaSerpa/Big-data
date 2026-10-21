cd temperatura-media/
mvn clean package
mvn package
rm -rf src/main/resources/output
hadoop jar target/temperatura-media-1.0-SNAPSHOT.jar \
    org.forestFire.Main \
    src/main/resources/input/forestfireinput.csv \
    src/main/resources/output
cat src/main/resources/output/part-r-00000
cd ..