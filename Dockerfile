FROM java:8
MAINTAINER JUXT <info@juxt.pro>

ADD target/edge-*-standalone.jar /srv/edge-app.jar

EXPOSE 3080

CMD ["java", "-jar", "/srv/edge-app.jar"]
