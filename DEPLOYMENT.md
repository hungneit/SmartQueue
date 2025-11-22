# SmartQueue Deployment Guide

## 📋 Prerequisites

### AWS Setup
1. Create AWS Account (Free Tier): https://aws.amazon.com/free/
2. Create IAM User with permissions:
   - AmazonDynamoDBFullAccess
   - AmazonEC2FullAccess
3. Create DynamoDB Tables:
   ```bash
   # Table 1: smartqueue-tickets
   aws dynamodb create-table \
     --table-name smartqueue-tickets \
     --attribute-definitions AttributeName=ticketId,AttributeType=S AttributeName=userId,AttributeType=S \
     --key-schema AttributeName=ticketId,KeyType=HASH \
     --global-secondary-indexes IndexName=userId-index,KeySchema=[{AttributeName=userId,KeyType=HASH}],Projection={ProjectionType=ALL},ProvisionedThroughput={ReadCapacityUnits=5,WriteCapacityUnits=5} \
     --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5

   # Table 2: smartqueue-queues
   aws dynamodb create-table \
     --table-name smartqueue-queues \
     --attribute-definitions AttributeName=queueId,AttributeType=S \
     --key-schema AttributeName=queueId,KeyType=HASH \
     --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5
   ```

### Aliyun Setup
1. Create Aliyun Account: https://www.alibabacloud.com/campaign/free-trial
2. Create TableStore instance: `smartqueue-ots`
3. Create DirectMail domain verification
4. Get Access Keys (AK/SK)

---

## 🚀 Deployment Steps

### 1. Deploy AWS Service (service-queue-aws)

#### Option A: EC2 Deployment
```bash
# SSH to EC2
ssh -i your-key.pem ec2-user@your-ec2-ip

# Install Java 18
sudo yum install java-18-amazon-corretto -y

# Upload JAR
scp -i your-key.pem target/service-queue-aws-*.jar ec2-user@your-ec2-ip:~/

# Run with environment variables
export AWS_REGION=ap-southeast-1
export DDB_TABLE_TICKETS=smartqueue-tickets
export DDB_TABLE_QUEUES=smartqueue-queues
export SERVICE_B_BASEURL=http://aliyun-service-ip:8081

java -jar service-queue-aws-*.jar
```

#### Option B: Docker on EC2
```bash
# Build Docker image locally
cd service-queue-aws
docker build -t smartqueue-aws .

# Push to ECR or Docker Hub
docker tag smartqueue-aws:latest your-registry/smartqueue-aws:latest
docker push your-registry/smartqueue-aws:latest

# Run on EC2
docker run -d -p 8080:8080 \
  -e AWS_REGION=ap-southeast-1 \
  -e DDB_TABLE_TICKETS=smartqueue-tickets \
  -e DDB_TABLE_QUEUES=smartqueue-queues \
  your-registry/smartqueue-aws:latest
```

---

### 2. Deploy Aliyun Service (service-eta-aliyun)

```bash
# SSH to Aliyun ECS
ssh root@your-aliyun-ip

# Install Java 18
yum install java-18-openjdk -y

# Upload JAR
scp target/service-eta-aliyun-*.jar root@your-aliyun-ip:~/

# Run with production config
export ALIYUN_AK=your-access-key
export ALIYUN_SK=your-secret-key
export ALI_REGION=ap-southeast-1
export OTS_ENDPOINT=https://smartqueue-ots.ap-southeast-1.ots.aliyuncs.com
export OTS_INSTANCE=smartqueue-ots
export TABLESTORE_ENABLED=true
export DIRECTMAIL_ENABLED=true

java -jar service-eta-aliyun-*.jar
```

---

### 3. Deploy Frontend

#### Option A: Vercel (Recommended)
```bash
# Install Vercel CLI
npm install -g vercel

# Build frontend
cd frontend
npm run build

# Deploy
vercel --prod

# Set environment variables in Vercel dashboard:
# VITE_AWS_SERVICE_URL=http://your-aws-ec2-ip:8080
# VITE_ALIYUN_SERVICE_URL=http://your-aliyun-ecs-ip:8081
```

#### Option B: Netlify
```bash
# Install Netlify CLI
npm install -g netlify-cli

# Build and deploy
cd frontend
npm run build
netlify deploy --prod --dir=dist
```

---

## 🔒 Security Checklist

- [ ] Enable HTTPS for all services
- [ ] Configure CORS properly
- [ ] Set strong passwords for users
- [ ] Use environment variables for secrets
- [ ] Enable AWS/Aliyun security groups
- [ ] Set up monitoring and alerts

---

## 📊 Monitoring URLs

- AWS Service Health: `http://your-aws-ip:8080/actuator/health`
- Aliyun Service Health: `http://your-aliyun-ip:8081/actuator/health`
- Frontend: `https://your-vercel-domain.vercel.app`

---

## 🧪 Performance Testing (Next Phase)

After deployment, use k6 for load testing:
```bash
cd tools/k6
k6 run load-test.js --vus 100 --duration 5m
```
