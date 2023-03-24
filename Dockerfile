FROM gradle:8.0.2-jdk11
ADD ./ ./
EXPOSE 8080
RUN gradle shadow
CMD ["java","-jar","./build/libs/NewBingGoGo-MagicURL-java-1.0-SNAPSHOT-all.jar"]