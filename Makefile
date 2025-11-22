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
