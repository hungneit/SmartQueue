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

push-aws:
	scp -i infra/aws/generated_key.pem service-queue-aws/target/service-queue-aws-1.0.0.jar ec2-user@52.221.245.143:~/

