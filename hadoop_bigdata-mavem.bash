cd BigData-Mavem/
mvn clean package
mvn package
rm -rf output-wordcount
hadoop jar target/BigData-Mavem-1.0-SNAPSHOT.jar \
    org.countWords.Main \
    src/main/resources/input/bible.txt \
    src/main/resources/output
cat src/main/resources/output/part-r-00000
cd ..