# K6 Load Testing Guide

## 📦 Installation

### macOS
```bash
brew install k6
```

### Linux
```bash
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6
```

### Windows
```bash
choco install k6
```

---

## 🚀 Running Tests

### Local Testing
```bash
cd tools/k6

# Basic test
k6 run load-test.js

# With custom VUs and duration
k6 run --vus 100 --duration 5m load-test.js
```

### Production Testing
```bash
# Set production URLs
export AWS_SERVICE_URL=http://your-aws-ec2-ip:8080
export ALIYUN_SERVICE_URL=http://your-aliyun-ecs-ip:8081

# Run full load test
k6 run load-test.js

# Run with custom stages
k6 run --stage 1m:50,3m:100,1m:0 load-test.js
```

---

## 📊 Test Scenarios

### Scenario 1: Baseline (100 users)
```bash
k6 run --vus 100 --duration 5m load-test.js
```

### Scenario 2: Peak Load (500 users)
```bash
k6 run --vus 500 --duration 10m load-test.js
```

### Scenario 3: Stress Test (1000+ users)
```bash
k6 run --vus 1000 --duration 15m load-test.js
```

### Scenario 4: Spike Test
```bash
k6 run --stage 0s:0,10s:1000,1m:1000,10s:0 load-test.js
```

---

## 📈 Metrics to Monitor

### Key Performance Indicators (KPIs)
- **http_req_duration**: Request duration (target: p95 < 2s)
- **http_req_failed**: Failed requests (target: < 1%)
- **http_reqs**: Requests per second
- **errors**: Custom error rate (target: < 10%)

### Cloud Metrics to Watch
- **AWS**: CloudWatch for EC2 CPU, DynamoDB throttling
- **Aliyun**: ECS monitoring, TableStore QPS
- **Response Times**: ETA calculation latency

---

## 🎯 Expected Results

### Good Performance
```
http_req_duration..........: avg=500ms  p95=1500ms
http_req_failed............: 0.5%
http_reqs..................: 500/s
errors.....................: 2%
```

### Issues to Investigate
- p95 > 2s: Backend optimization needed
- Error rate > 10%: Check database capacity
- Failed requests > 5%: Scale infrastructure

---

## 🔧 Troubleshooting

### High Latency
- Check database connection pooling
- Verify network latency between services
- Scale up EC2/ECS instances

### Errors
- Check backend logs: `journalctl -u smartqueue-*`
- Verify DynamoDB/TableStore capacity
- Check memory and CPU usage

### Throttling
- Increase DynamoDB WCU/RCU
- Upgrade TableStore instance
- Add caching layer (Redis)

---

## 📝 Generating Reports

### HTML Report
```bash
k6 run --out json=results.json load-test.js
k6 report results.json --export results.html
```

### CSV Export
```bash
k6 run --out csv=results.csv load-test.js
```

### Cloud Upload (k6 Cloud)
```bash
k6 cloud load-test.js
```
