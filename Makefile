all:
	$(MAKE) aliyun
	$(MAKE) aws

aliyun:
	cd infra/aliyun && terraform apply 

aws:
	cd infra/aws && terraform apply

plan:
	$(MAKE) plan-aliyun
	$(MAKE) plan-aws

plan-aliyun:
	cd infra/aliyun && terraform plan

plan-aws:
	cd infra/aws && terraform plan


destroy:
	$(MAKE) destroy-aliyun
	$(MAKE) destroy-aws

destroy-aliyun:
	cd infra/aliyun && terraform destroy

destroy-aws:
	cd infra/aws && terraform destroy

deploy-aws:
	cd service-queue-aws && mvn clean package -DskipTests
	scp -i aws_key.pem service-queue-aws/target/service-queue-aws-1.0.0.jar ec2-user@52.221.245.143:/opt/smartqueue-aws/app.jar
	ssh -i aws_key.pem ec2-user@52.221.245.143 'cd /opt/smartqueue-aws && ./restart.sh'

deploy-aliyun:
	cd service-eta-aliyun && mvn clean package -DskipTests
	scp -i aliyun_key.pem service-eta-aliyun/target/service-eta-aliyun-1.0.0.jar root@47.237.162.140:/opt/smartqueue-aliyun/app.jar
	ssh -i aliyun_key.pem root@47.237.162.140 'cd /opt/smartqueue-aliyun && ./restart.sh'
