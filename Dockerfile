FROM frolvlad/alpine-java:jdk8-slim
VOLUME /tmp
ADD target/website-1.0.jar website.jar
EXPOSE 8001
ENTRYPOINT ["java","-jar","/website.jar"]
