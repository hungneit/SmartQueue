import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');

// Test configuration
export const options = {
  stages: [
    { duration: '2m', target: 100 },   // Ramp up to 100 users
    { duration: '5m', target: 100 },   // Stay at 100 users
    { duration: '2m', target: 500 },   // Ramp up to 500 users
    { duration: '5m', target: 500 },   // Stay at 500 users
    { duration: '2m', target: 1000 },  // Ramp up to 1000 users
    { duration: '5m', target: 1000 },  // Stay at 1000 users
    { duration: '2m', target: 0 },     // Ramp down to 0
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'], // 95% requests < 2s
    errors: ['rate<0.1'],               // Error rate < 10%
  },
};

// Configuration - Update these URLs after deployment
const AWS_SERVICE_URL = __ENV.AWS_SERVICE_URL || 'http://localhost:8080';
const ALIYUN_SERVICE_URL = __ENV.ALIYUN_SERVICE_URL || 'http://localhost:8081';

// Test data
const testUsers = [];
for (let i = 0; i < 1000; i++) {
  testUsers.push({
    name: `User${i}`,
    email: `user${i}@loadtest.com`,
    phone: `+1234567${String(i).padStart(4, '0')}`,
    password: 'LoadTest123!',
  });
}

const testQueues = [
  { name: 'Express Queue', description: 'Fast service queue' },
  { name: 'Standard Queue', description: 'Normal service queue' },
  { name: 'Premium Queue', description: 'VIP service queue' },
];

// Setup function - runs once per VU
export function setup() {
  console.log('🚀 Starting load test...');
  console.log(`📊 AWS Service: ${AWS_SERVICE_URL}`);
  console.log(`📊 Aliyun Service: ${ALIYUN_SERVICE_URL}`);

  // Create test queues
  const createdQueues = [];
  for (const queue of testQueues) {
    const res = http.post(
      `${AWS_SERVICE_URL}/api/v1/queues`,
      JSON.stringify(queue),
      { headers: { 'Content-Type': 'application/json' } }
    );
    
    if (res.status === 201) {
      const queueData = JSON.parse(res.body);
      createdQueues.push(queueData.queueId);
      console.log(`✅ Created queue: ${queue.name}`);
    }
  }

  return { queues: createdQueues };
}

// Main test scenario
export default function (data) {
  const queueId = data.queues[Math.floor(Math.random() * data.queues.length)];
  const user = testUsers[Math.floor(Math.random() * testUsers.length)];

  // 1. Register user (if not exists)
  const registerRes = http.post(
    `${AWS_SERVICE_URL}/api/v1/users/register`,
    JSON.stringify(user),
    { headers: { 'Content-Type': 'application/json' } }
  );

  const registerSuccess = check(registerRes, {
    'register: status is 201 or 409': (r) => r.status === 201 || r.status === 409,
  });
  errorRate.add(!registerSuccess);

  sleep(1);

  // 2. Login
  const loginRes = http.post(
    `${AWS_SERVICE_URL}/api/v1/users/login`,
    JSON.stringify({
      email: user.email,
      password: user.password,
    }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  const loginSuccess = check(loginRes, {
    'login: status is 200': (r) => r.status === 200,
    'login: has token': (r) => JSON.parse(r.body).token !== undefined,
  });
  errorRate.add(!loginSuccess);

  if (!loginSuccess) {
    return;
  }

  const userData = JSON.parse(loginRes.body);
  const userId = userData.userId;

  sleep(1);

  // 3. Join queue
  const joinRes = http.post(
    `${AWS_SERVICE_URL}/api/v1/queues/${queueId}/join`,
    JSON.stringify({ userId }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  const joinSuccess = check(joinRes, {
    'join queue: status is 201': (r) => r.status === 201,
    'join queue: has ticketId': (r) => JSON.parse(r.body).ticketId !== undefined,
  });
  errorRate.add(!joinSuccess);

  if (!joinSuccess) {
    return;
  }

  const ticketData = JSON.parse(joinRes.body);
  const ticketId = ticketData.ticketId;

  sleep(2);

  // 4. Get ETA (from Aliyun service)
  const etaRes = http.get(
    `${ALIYUN_SERVICE_URL}/api/v1/eta/calculate?queueId=${queueId}&ticketId=${ticketId}&position=${ticketData.position}`
  );

  const etaSuccess = check(etaRes, {
    'get ETA: status is 200': (r) => r.status === 200,
    'get ETA: has estimatedWaitMinutes': (r) => JSON.parse(r.body).estimatedWaitMinutes !== undefined,
  });
  errorRate.add(!etaSuccess);

  sleep(1);

  // 5. Check queue status
  const statusRes = http.get(`${AWS_SERVICE_URL}/api/v1/queues/${queueId}`);

  const statusSuccess = check(statusRes, {
    'queue status: status is 200': (r) => r.status === 200,
  });
  errorRate.add(!statusSuccess);

  sleep(2);

  // 6. Get my tickets
  const myTicketsRes = http.get(`${AWS_SERVICE_URL}/api/v1/queues/my-tickets?userId=${userId}`);

  const myTicketsSuccess = check(myTicketsRes, {
    'my tickets: status is 200': (r) => r.status === 200,
  });
  errorRate.add(!myTicketsSuccess);

  sleep(1);
}

// Teardown function
export function teardown(data) {
  console.log('🏁 Load test completed!');
  console.log(`📊 Total queues tested: ${data.queues.length}`);
}
