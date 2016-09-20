.PHONY: edge-app

target/aws.zip:	Dockerfile target/edge-0.1.0-SNAPSHOT-standalone.jar
	zip $@ $^

edge-app:
	aws elasticbeanstalk create-application --application-name dominic
