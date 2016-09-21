FROM java:8
MAINTAINER JUXT <info@juxt.pro>

ADD target/edge-app.jar /srv/edge-app.jar

EXPOSE 3080

CMD ["java", "-jar", "/srv/edge-app.jar"]
