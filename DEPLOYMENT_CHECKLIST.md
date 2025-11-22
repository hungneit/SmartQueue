# 🚀 SmartQueue Production Deployment Checklist

## Phase 1: Cloud Account Setup ⏱️ 1-2 giờ

### AWS Setup
- [ ] Đăng ký AWS Free Tier: https://aws.amazon.com/free/
- [ ] Tạo IAM User với quyền: DynamoDB, EC2
- [ ] Download Access Key CSV
- [ ] Tạo DynamoDB Table: `smartqueue-tickets`
  - Primary Key: `ticketId` (String)
  - GSI: `userId-index` (userId as Hash Key)
- [ ] Tạo DynamoDB Table: `smartqueue-queues`
  - Primary Key: `queueId` (String)
- [ ] Launch EC2 instance: t2.micro (Amazon Linux 2)
- [ ] Configure Security Group: Allow port 8080, 22
- [ ] Tạo Key Pair và download `.pem` file

### Aliyun Setup
- [ ] Đăng ký Aliyun Free Trial: https://www.alibabacloud.com/campaign/free-trial
- [ ] Verify email và phone
- [ ] Tạo Access Key (AK/SK)
- [ ] Tạo TableStore instance: `smartqueue-ots`
- [ ] Tạo ECS instance (Singapore/Jakarta region)
- [ ] Configure Security Group: Allow port 8081, 22
- [ ] Setup DirectMail (optional cho email notifications)

### Frontend Hosting
- [ ] Đăng ký Vercel: https://vercel.com
- [ ] Connect GitHub repository (hoặc)
- [ ] Đăng ký Netlify: https://netlify.com

---

## Phase 2: Build & Package ⏱️ 30 phút

### Backend Services
```bash
# Build AWS Service
cd service-queue-aws
mvn clean package -DskipTests
# Output: target/service-queue-aws-1.0.0.jar

# Build Aliyun Service
cd ../service-eta-aliyun
mvn clean package -DskipTests
# Output: target/service-eta-aliyun-1.0.0.jar
```

- [ ] AWS Service JAR built successfully
- [ ] Aliyun Service JAR built successfully
- [ ] Test JARs locally before deploy

### Frontend
```bash
cd frontend
npm run build
# Output: dist/ folder
```

- [ ] Frontend build successful
- [ ] Update API URLs in environment variables

---

## Phase 3: Deploy Backend Services ⏱️ 1-2 giờ

### Deploy AWS Service
```bash
# Upload JAR to EC2
scp -i your-key.pem target/service-queue-aws-*.jar ec2-user@<EC2-IP>:~/

# Upload deploy script
scp -i your-key.pem infra/aws/deploy.sh ec2-user@<EC2-IP>:~/

# SSH and deploy
ssh -i your-key.pem ec2-user@<EC2-IP>
chmod +x deploy.sh

# Set environment variables
export AWS_REGION=ap-southeast-1
export DDB_TABLE_TICKETS=smartqueue-tickets
export DDB_TABLE_QUEUES=smartqueue-queues
export SERVICE_B_BASEURL=http://<ALIYUN-ECS-IP>:8081

./deploy.sh
```

- [ ] JAR uploaded to EC2
- [ ] Environment variables configured
- [ ] Service started successfully
- [ ] Health check: `curl http://<EC2-IP>:8080/actuator/health`
- [ ] Test API: Register user, Create queue

### Deploy Aliyun Service
```bash
# Upload JAR to ECS
scp target/service-eta-aliyun-*.jar root@<ECS-IP>:~/

# Upload deploy script
scp infra/aliyun/deploy.sh root@<ECS-IP>:~/

# SSH and deploy
ssh root@<ECS-IP>
chmod +x deploy.sh

# Set environment variables
export ALIYUN_AK=<your-access-key>
export ALIYUN_SK=<your-secret-key>
export OTS_ENDPOINT=https://smartqueue-ots.ap-southeast-1.ots.aliyuncs.com
export OTS_INSTANCE=smartqueue-ots
export TABLESTORE_ENABLED=true

./deploy.sh
```

- [ ] JAR uploaded to ECS
- [ ] Access Keys configured
- [ ] TableStore connected
- [ ] Service started successfully
- [ ] Health check: `curl http://<ECS-IP>:8081/actuator/health`
- [ ] Test ETA calculation API

---

## Phase 4: Deploy Frontend ⏱️ 30 phút

### Vercel Deployment
```bash
cd frontend

# Install Vercel CLI
npm install -g vercel

# Login
vercel login

# Deploy
vercel --prod
```

**Configure Environment Variables in Vercel:**
- [ ] `VITE_AWS_SERVICE_URL=http://<EC2-IP>:8080`
- [ ] `VITE_ALIYUN_SERVICE_URL=http://<ECS-IP>:8081`

### Verify Deployment
- [ ] Frontend accessible at Vercel URL
- [ ] Can register new user
- [ ] Can create queue
- [ ] Can join queue
- [ ] ETA calculation works
- [ ] No CORS errors in browser console

---

## Phase 5: Integration Testing ⏱️ 30 phút

### Manual Testing
- [ ] Register 3-5 test users
- [ ] Create 2-3 queues
- [ ] Join queues with different users
- [ ] Check ETA calculations
- [ ] Verify ticket status updates
- [ ] Test logout/login flow

### API Testing
```bash
# Test AWS Service
curl -X POST http://<EC2-IP>:8080/api/v1/users/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","email":"test@test.com","phone":"+1234567890","password":"Test123!"}'

# Test Aliyun Service
curl "http://<ECS-IP>:8081/api/v1/eta/calculate?queueId=<ID>&ticketId=<ID>&position=5"
```

- [ ] All API endpoints responding
- [ ] Data persisting in DynamoDB
- [ ] ETA calculations accurate
- [ ] No errors in logs

---

## Phase 6: Performance Testing ⏱️ 1-2 giờ

### Install k6
```bash
# macOS
brew install k6

# Linux
sudo apt-get install k6
```

### Configure Test
```bash
cd tools/k6

# Update URLs in load-test.js
export AWS_SERVICE_URL=http://<EC2-IP>:8080
export ALIYUN_SERVICE_URL=http://<ECS-IP>:8081
```

### Run Tests
```bash
# Baseline: 100 users
k6 run --vus 100 --duration 5m load-test.js

# Stress test: 500 users
k6 run --vus 500 --duration 10m load-test.js

# Peak load: 1000 users
k6 run load-test.js  # Uses default stages
```

### Monitor Results
- [ ] p95 response time < 2 seconds
- [ ] Error rate < 10%
- [ ] No database throttling
- [ ] CPU/Memory within limits
- [ ] All services stable under load

---

## Phase 7: Monitoring & Optimization ⏱️ Ongoing

### AWS Monitoring
- [ ] Enable CloudWatch for EC2
- [ ] Monitor DynamoDB metrics
- [ ] Set up billing alerts
- [ ] Configure auto-scaling (if needed)

### Aliyun Monitoring
- [ ] Enable ECS monitoring
- [ ] Check TableStore QPS
- [ ] Monitor DirectMail usage
- [ ] Review cost analytics

### Application Monitoring
- [ ] Check application logs
- [ ] Monitor API response times
- [ ] Track error rates
- [ ] Set up alerts

---

## 🎯 Success Criteria

- ✅ All services deployed and accessible
- ✅ Frontend working with backend APIs
- ✅ Data persisting correctly
- ✅ Performance meets targets (p95 < 2s)
- ✅ Error rate < 5%
- ✅ System stable under 1000 concurrent users

---

## 📊 Cost Estimation (Free Tier)

### AWS (12 months free)
- EC2 t2.micro: **$0** (750 hours/month)
- DynamoDB: **$0** (25GB + 25 RCU/WCU)
- Data Transfer: **$0** (first 100GB)

### Aliyun (Free trial)
- ECS Lightweight: **$0** (trial period)
- TableStore: **$0** (1GB free)
- DirectMail: **$0** (200 emails/day)

### Frontend
- Vercel/Netlify: **$0** (hobby plan)

**Total Monthly Cost: $0** (during free tier)

---

## 🆘 Troubleshooting

### Services not starting
```bash
# Check logs
journalctl -u smartqueue-aws -f
journalctl -u smartqueue-aliyun -f

# Check Java process
ps aux | grep java

# Check ports
netstat -tulpn | grep 808
```

### CORS errors
- Add frontend domain to CORS config
- Restart backend services
- Clear browser cache

### Database connection errors
- Verify Access Keys
- Check Security Groups
- Test network connectivity

---

## 📝 Next Steps After Deployment

1. **Documentation**: Update README with production URLs
2. **CI/CD**: Setup GitHub Actions for auto-deploy
3. **Monitoring**: Integrate APM tools (New Relic, DataDog)
4. **Caching**: Add Redis for performance
5. **CDN**: Use CloudFront/Aliyun CDN for frontend
6. **HTTPS**: Configure SSL certificates
7. **Domain**: Buy custom domain
8. **Backup**: Setup automated backups

---

## 🎓 Learning Outcomes

After completing this deployment, you will understand:
- ✅ Multi-cloud architecture deployment
- ✅ AWS and Aliyun cloud services
- ✅ Container orchestration
- ✅ Performance testing with k6
- ✅ Production monitoring
- ✅ DevOps best practices

**Good luck with your deployment! 🚀**
