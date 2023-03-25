FROM gradle:8.0.2-jdk11
ADD ./ ./
RUN gradle shadow
EXPOSE 80/tcp
EXPOSE 80/udp
CMD ["java","-jar","./build/libs/NewBingGoGo-MagicURL-java-1.0-SNAPSHOT-all.jar","80"]