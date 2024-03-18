FROM openjdk:11

COPY target/scala-2.13/cqdg-ferload-drs-import.jar .

ENTRYPOINT ["java", "-jar", "cqdg-ferload-drs-import.jar"]
