.PHONY: create-application create-environment

aws-region := eu-west-1
aws-account-id := 247806367507
aws-solution-stack := "64bit Amazon Linux 2016.03 v2.1.6 running Docker 1.11.2"
s3-bucket := elasticbeanstalk-$(aws-region)-$(aws-account-id)

application-name := edge
environment-name := $(application-name)-prod

version := $(shell git describe --dirty --long --tags --match '[0-9]*')
#version := 0.1.0-SNAPSHOT
uberjar := target/$(application-name)-$(version)-standalone.jar
zipfile := edge-aws-ebs-upload-$(version).zip

show-version:
	@echo $(version)

target/$(zipfile):	Dockerfile $(uberjar)
	zip $@ $^

zip:	target/$(zipfile)

create-application:
	aws elasticbeanstalk create-application --application-name $(application-name)

create-environment:
	aws elasticbeanstalk create-environment --application-name $(application-name) --environment-name $(environment-name) --description "Edge production" --cname-prefix $(environment-name) --solution-stack-name $(aws-solution-stack)

upload:	target/$(zipfile)
	aws s3 cp $< s3://elasticbeanstalk-$(aws-region)-$(aws-account-id)/$(zipfile)

create-application-version:
	aws elasticbeanstalk create-application-version --application-name $(application-name) --version-label $(version) --source-bundle S3Bucket=$(s3-bucket),S3Key=$(zipfile)

update-environment:
	aws elasticbeanstalk update-environment --application-name $(application-name) --environment-name $(environment-name) --version-label $(version)
