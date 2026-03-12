# 🚀 Complete CI/CD Pipeline Guide
## From Zero to Production — Jenkins, SonarQube, Checkmarx & Ansible

> **For Beginners** | Practical, Hands-On, Interview-Ready

---

## 📋 Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Environment Prerequisites](#2-environment-prerequisites)
3. [Step 1: Push Project to GitHub](#3-step-1-push-project-to-github)
4. [Step 2: Install Jenkins](#4-step-2-install-jenkins)
5. [Step 3: Install SonarQube](#5-step-3-install-sonarqube)
6. [Step 4: Set Up Checkmarx / OWASP Dependency-Check](#6-step-4-set-up-security-scanning)
7. [Step 5: Install Ansible](#7-step-5-install-ansible)
8. [Step 6: Integrate All Tools in Jenkins](#8-step-6-integrate-all-tools-in-jenkins)
9. [Step 7: Configure the Jenkins Pipeline](#9-step-7-configure-the-jenkins-pipeline)
10. [Step 8: Run & Verify the Pipeline](#10-step-8-run--verify-the-pipeline)
11. [Step 9: Deployment with Ansible](#11-step-9-deployment-with-ansible)
12. [Understanding the Jenkinsfile](#12-understanding-the-jenkinsfile)
13. [Understanding the Ansible Playbook](#13-understanding-the-ansible-playbook)
14. [Production Hardening Tips](#14-production-hardening-tips)
15. [Troubleshooting Common Issues](#15-troubleshooting-common-issues)
16. [Interview Questions & Answers](#16-interview-questions--answers)

---

## 1. Architecture Overview

```
┌─────────────┐     webhook      ┌──────────────┐
│   GitHub     │────────────────▶│   Jenkins    │
│  (SCM)       │                 │  (CI/CD)     │
└─────────────┘                  └──────┬───────┘
                                        │
                    ┌───────────────────┼───────────────────┐
                    │                   │                   │
                    ▼                   ▼                   ▼
            ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
            │  SonarQube   │  │  Checkmarx   │  │   Ansible    │
            │ (Code Quality│  │  (Security   │  │ (Deployment) │
            │  + Coverage) │  │   SAST Scan) │  │              │
            └──────────────┘  └──────────────┘  └──────┬───────┘
                                                       │
                                        ┌──────────────┼──────────────┐
                                        ▼                             ▼
                                ┌──────────────┐             ┌──────────────┐
                                │   Staging    │             │  Production  │
                                │   Server     │             │   Server     │
                                └──────────────┘             └──────────────┘
```

### How the Pipeline Flows (Real-World Scenario)

Imagine you're a developer on a team. Here's what happens when you push code:

1. **You push code** to GitHub (e.g., `git push origin main`)
2. **GitHub fires a webhook** to Jenkins saying "new code arrived!"
3. **Jenkins wakes up** and starts the pipeline defined in `Jenkinsfile`
4. **Maven builds** your Java project and runs **unit tests** with **JaCoCo coverage**
5. **SonarQube analyzes** code quality (bugs, code smells, coverage %)
6. **Quality Gate check** — if quality is below threshold, pipeline **fails** (no bad code reaches production!)
7. **Checkmarx/OWASP** scans for **security vulnerabilities** in code & dependencies
8. **Ansible deploys** the JAR to the **staging** server
9. **Smoke tests** verify the staging deployment works
10. **Manual approval gate** — a team lead reviews and clicks "Deploy to Production"
11. **Ansible deploys** to the **production** server
12. **Notifications** go out via Slack/email

### What Each Tool Does

| Tool | Role | Why It's Needed |
|------|------|----------------|
| **GitHub** | Source Code Management | Stores code, tracks changes, triggers CI/CD via webhooks |
| **Jenkins** | CI/CD Orchestrator | Automates build → test → scan → deploy pipeline |
| **Maven** | Build Tool | Compiles Java code, runs tests, creates JAR |
| **JaCoCo** | Code Coverage | Measures how much of your code is tested |
| **SonarQube** | Code Quality | Finds bugs, code smells, security hotspots, enforces quality gates |
| **Checkmarx** | SAST Security Scanner | Finds security vulnerabilities in source code |
| **OWASP Dep-Check** | Dependency Scanner | Finds known CVEs in third-party libraries (free alternative) |
| **Ansible** | Deployment Automation | Deploys application to servers via SSH (agentless) |

---

## 2. Environment Prerequisites

### Option A: Local Setup with Docker (Recommended for Learning)

You only need your **Mac** with Docker installed. Everything runs in containers.

**Requirements:**
- macOS with **8 GB+ RAM** (16 GB recommended)
- **Docker Desktop** installed ([download](https://docker.com/products/docker-desktop))
- **Git** (comes with macOS or `brew install git`)
- **Java 25** (`brew install openjdk@25` or use SDKMAN)
- **Maven** (`brew install maven`)

### Option B: Multi-Server Setup (Production-Like)

| Server | Purpose | Specs | Port |
|--------|---------|-------|------|
| Server 1 | Jenkins Controller | 4 GB RAM, 2 CPU | 8080 |
| Server 2 | SonarQube + PostgreSQL | 4 GB RAM, 2 CPU | 9000 |
| Server 3 | Checkmarx (or SaaS) | 8 GB RAM, 4 CPU | 8080 |
| Server 4 | Staging App Server | 2 GB RAM, 1 CPU | 8080 |
| Server 5 | Production App Server | 2 GB RAM, 1 CPU | 8080 |

> 💡 **Tip for interviews**: "We used a controller-agent architecture with Jenkins. The controller manages the pipeline, and agents (workers) execute the builds, so the controller isn't overloaded."

---

## 3. Step 1: Push Project to GitHub

### 3.1 Create a GitHub Repository

1. Go to [github.com/new](https://github.com/new)
2. Name: `cicd-pipeline-demo`
3. Keep it **Public** (or Private)
4. Do NOT initialize with README (we'll push our existing project)

### 3.2 Initialize Git & Push

```bash
# Navigate to your project directory
cd /path/to/untitled

# Initialize git
git init

# Add all files
git add .

# First commit
git commit -m "Initial commit: Java project with CI/CD pipeline configuration"

# Add your GitHub remote (replace with YOUR repo URL)
git remote add origin https://github.com/YOUR_USERNAME/cicd-pipeline-demo.git

# Push to GitHub
git branch -M main
git push -u origin main
```

### 3.3 Create a GitHub Personal Access Token

1. Go to **GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)**
2. Click **"Generate new token (classic)"**
3. Name: `jenkins-integration`
4. Select scopes: `repo`, `admin:repo_hook`
5. Click **Generate token** → **COPY AND SAVE the token** (you won't see it again!)

> 🔑 **Save this token** — Jenkins will use it to access your GitHub repo.

---

## 4. Step 2: Install Jenkins

### Option A: Docker (Recommended)

The `docker-compose.yml` in this project handles everything. Just run:

```bash
# Start Jenkins + SonarQube + PostgreSQL
docker-compose up -d

# Check containers are running
docker-compose ps

# Get Jenkins initial admin password
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

### Option B: Native Install on macOS

```bash
# Install Jenkins via Homebrew
brew install jenkins-lts

# Start Jenkins
brew services start jenkins-lts

# Jenkins runs at http://localhost:8080
# Find initial password at:
cat /Users/$(whoami)/.jenkins/secrets/initialAdminPassword
```

### 4.1 Complete Jenkins Setup Wizard

1. Open **http://localhost:8080** in your browser
2. Paste the **initial admin password**
3. Click **"Install suggested plugins"** (wait for installation)
4. Create your **admin user** (e.g., admin/admin123)
5. Set Jenkins URL to `http://localhost:8080/`
6. Click **"Start using Jenkins"**

### 4.2 Install Required Jenkins Plugins

Go to **Manage Jenkins → Plugins → Available plugins** and install:

| Plugin | Purpose |
|--------|---------|
| **Pipeline** | Enables Jenkinsfile pipelines (usually pre-installed) |
| **Git** | Git SCM integration (usually pre-installed) |
| **GitHub Integration** | Webhooks & GitHub status checks |
| **SonarQube Scanner** | Integrates SonarQube analysis |
| **JaCoCo** | Publishes code coverage reports |
| **Checkmarx** | Checkmarx SAST integration (optional if using OWASP) |
| **OWASP Dependency-Check** | Publishes OWASP reports |
| **Ansible** | Run Ansible playbooks from Jenkins |
| **Credentials Binding** | Securely use secrets in pipelines |
| **Slack Notification** | Send pipeline notifications to Slack (optional) |

Click **"Install without restart"** → check **"Restart Jenkins when no jobs are running"**

### 4.3 Configure Global Tools

Go to **Manage Jenkins → Tools**:

**JDK Installation:**
- Name: `JDK-25`
- JAVA_HOME: `/opt/java/openjdk` (Docker) or `/usr/local/opt/openjdk@25` (Homebrew)

**Maven Installation:**
- Name: `Maven-3.9`
- Check "Install automatically" → Version `3.9.9`

**Ansible Installation (if plugin installed):**
- Name: `Ansible`
- Path: `/usr/bin/` (or wherever `ansible-playbook` is installed)

---

## 5. Step 3: Install SonarQube

### Using Docker (already in docker-compose.yml)

If you ran `docker-compose up -d`, SonarQube is already running!

```bash
# Verify SonarQube is running
curl http://localhost:9000/api/system/status
# Should return: {"status":"UP"}
```

### 5.1 Configure SonarQube

1. Open **http://localhost:9000**
2. Login with default credentials: **admin / admin**
3. You'll be prompted to **change the password** — change it to something memorable (e.g., `sonar123`)

### 5.2 Generate SonarQube Token (for Jenkins)

1. Go to **My Account** (top-right icon) → **Security**
2. Token name: `jenkins-sonar-token`
3. Type: **Global Analysis Token**
4. Click **Generate** → **COPY the token**

### 5.3 Create a SonarQube Project

1. Go to **Projects → Create Project → Manually**
2. Project key: `org.example:untitled`
3. Display name: `untitled`
4. Main branch: `main`

### 5.4 Configure Quality Gate

Go to **Quality Gates** in SonarQube:

The default "Sonar way" quality gate includes:
- ✅ Coverage on new code > 80%
- ✅ Duplicated lines on new code < 3%
- ✅ Maintainability rating is A
- ✅ Reliability rating is A
- ✅ Security rating is A

> 💡 **Interview tip**: "A Quality Gate is a set of conditions that code must meet. If any condition fails, the pipeline breaks. This prevents bad code from reaching production — it's the gatekeeper of code quality."

---

## 6. Step 4: Set Up Security Scanning

### Option A: Checkmarx (Enterprise — Paid License Required)

Checkmarx is an industry-standard **SAST (Static Application Security Testing)** tool used in most large enterprises.

**If your company provides Checkmarx access:**

1. Get from your security team:
   - Server URL (e.g., `https://checkmarx.company.com`)
   - Team path (e.g., `CxServer\SP\Company\YourTeam`)
   - Service account credentials
2. In Jenkins → **Manage Jenkins → Configure System → Checkmarx**:
   - Server URL: `https://checkmarx.company.com`
   - Credentials: Add username/password in Jenkins Credentials
   - Default Preset: `Checkmarx Default`

**What Checkmarx does:**
- Scans your **source code** (not compiled binaries) for security flaws
- Finds: SQL injection, XSS, path traversal, hardcoded secrets, etc.
- Generates a detailed report with **severity levels** (High, Medium, Low)
- Can **break the build** if High severity issues are found

### Option B: OWASP Dependency-Check (Free — Already Configured!)

Your `pom.xml` already has the OWASP Dependency-Check plugin. It scans your **dependencies** (third-party libraries) for **known CVEs**.

```bash
# Run manually to test:
mvn org.owasp:dependency-check-maven:check

# Report is generated at:
# target/dependency-check-report.html
```

> 💡 **Interview tip**: "Checkmarx is SAST — it analyzes source code. OWASP Dependency-Check is SCA (Software Composition Analysis) — it checks third-party libraries for known vulnerabilities. In our pipeline, we used both for defense-in-depth."

---

## 7. Step 5: Install Ansible

### On macOS:

```bash
# Install Ansible via pip
pip3 install ansible

# Verify installation
ansible --version
```

### 7.1 Set Up SSH Key for Deployment

```bash
# Generate SSH key pair (if you don't have one)
ssh-keygen -t ed25519 -C "jenkins-deploy-key" -f ~/.ssh/jenkins_deploy_key -N ""

# Copy public key to target servers
ssh-copy-id -i ~/.ssh/jenkins_deploy_key.pub deploy@staging-server-ip
ssh-copy-id -i ~/.ssh/jenkins_deploy_key.pub deploy@production-server-ip

# Test connection
ansible -i deploy/inventory.ini staging -m ping
```

### 7.2 Store SSH Key in Jenkins

1. Go to **Manage Jenkins → Credentials → System → Global credentials**
2. Click **"Add Credentials"**
3. Kind: **SSH Username with private key**
4. ID: `ansible-ssh-key`
5. Username: `deploy`
6. Private Key: **Enter directly** → paste contents of `~/.ssh/jenkins_deploy_key`

### 7.3 Understanding the Ansible Files

```
deploy/
├── inventory.ini          # Lists target servers (staging + production)
├── deploy.yml             # Main deployment playbook
├── rollback.yml           # Rollback playbook (restore previous version)
└── templates/
    └── app.service.j2     # Systemd service template for the Java app
```

> 💡 **Interview tip**: "Ansible is agentless — it connects via SSH and executes tasks. Unlike Chef or Puppet, there's nothing to install on target servers. Playbooks are written in YAML, making them easy to read and version-control."

---

## 8. Step 6: Integrate All Tools in Jenkins

### 8.1 Add GitHub Credentials in Jenkins

1. **Manage Jenkins → Credentials → System → Global credentials → Add Credentials**
2. Kind: **Secret text**
3. Secret: Paste your **GitHub Personal Access Token**
4. ID: `github-token`
5. Description: `GitHub PAT for repo access`

### 8.2 Configure SonarQube Server in Jenkins

1. **Manage Jenkins → Configure System** → scroll to **"SonarQube servers"**
2. Check **"Environment variables"**
3. Click **"Add SonarQube"**:
   - Name: `SonarQube` (must match the Jenkinsfile!)
   - Server URL: `http://sonarqube:9000` (Docker) or `http://localhost:9000`
   - Server authentication token: Add → **Secret text** → paste SonarQube token
4. Click **Save**

### 8.3 Set Up SonarQube Webhook (for Quality Gate)

In **SonarQube** (http://localhost:9000):

1. Go to **Administration → Configuration → Webhooks**
2. Click **Create**
3. Name: `Jenkins`
4. URL: `http://jenkins:8080/sonarqube-webhook/` (Docker) or `http://localhost:8080/sonarqube-webhook/`
5. Click **Create**

> This webhook tells Jenkins when SonarQube has finished analyzing, so the `waitForQualityGate` step works.

### 8.4 Set Up GitHub Webhook (for auto-triggering)

In **GitHub** (your repo):

1. Go to **Settings → Webhooks → Add webhook**
2. Payload URL: `http://YOUR_JENKINS_URL:8080/github-webhook/`
3. Content type: `application/json`
4. Which events: **"Just the push event"**
5. Click **Add webhook**

> ⚠️ **Note**: For local testing, Jenkins must be accessible from the internet. Use **ngrok** (`ngrok http 8080`) to create a public URL for your local Jenkins.

---

## 9. Step 7: Configure the Jenkins Pipeline

### 9.1 Create a New Pipeline Job

1. Go to **Jenkins Dashboard → New Item**
2. Name: `cicd-pipeline-demo`
3. Select **"Pipeline"** → Click **OK**

### 9.2 Configure the Pipeline

In the job configuration:

**General:**
- ☑️ GitHub project: `https://github.com/YOUR_USERNAME/cicd-pipeline-demo/`

**Build Triggers:**
- ☑️ GitHub hook trigger for GITScm polling

**Pipeline:**
- Definition: **Pipeline script from SCM**
- SCM: **Git**
- Repository URL: `https://github.com/YOUR_USERNAME/cicd-pipeline-demo.git`
- Credentials: Select your GitHub credentials
- Branch Specifier: `*/main`
- Script Path: `Jenkinsfile`

Click **Save**.

---

## 10. Step 8: Run & Verify the Pipeline

### 10.1 Trigger Your First Build

**Option A: Push a commit**
```bash
git add .
git commit -m "Add CI/CD pipeline configuration"
git push origin main
```

**Option B: Manual trigger**
- In Jenkins, click **"Build Now"** on your pipeline job

### 10.2 Watch the Pipeline Execute

1. Click on the **build number** (e.g., `#1`)
2. Click **"Console Output"** to see real-time logs
3. Or use **"Pipeline Steps"** / **"Blue Ocean"** for a visual view

### 10.3 Expected Pipeline Stages

```
✅ Checkout ............. Clone code from GitHub
✅ Build ................ mvn clean compile
✅ Unit Tests ........... mvn test (10 tests pass, JaCoCo coverage report)
✅ Package .............. mvn package (creates JAR)
✅ SonarQube Analysis ... Sends code to SonarQube for analysis
✅ Quality Gate ......... Waits for SonarQube pass/fail verdict
✅ Checkmarx/OWASP ..... Security vulnerability scan
✅ Deploy to Staging .... Ansible deploys JAR to staging server
✅ Smoke Test ........... Verifies staging deployment
⏳ Approval ............ Waits for human to click "Deploy to Production"
✅ Deploy to Production . Ansible deploys JAR to production server
```

### 10.4 Check SonarQube Results

1. Open **http://localhost:9000**
2. Click on your project **"untitled"**
3. You'll see:
   - **Bugs**: 0
   - **Code Smells**: any style issues
   - **Coverage**: % of code covered by tests
   - **Duplications**: % of duplicated code
   - **Quality Gate**: PASSED ✅ or FAILED ❌

---

## 11. Step 9: Deployment with Ansible

### How Ansible Deployment Works

```
Jenkins                          Target Server
  │                                    │
  │  1. SSH connect                    │
  ├───────────────────────────────────▶│
  │  2. Stop old app                   │
  ├───────────────────────────────────▶│
  │  3. Backup current JAR             │
  ├───────────────────────────────────▶│
  │  4. Copy new JAR                   │
  ├───────────────────────────────────▶│
  │  5. Start new app via systemd      │
  ├───────────────────────────────────▶│
  │  6. Health check                   │
  ├───────────────────────────────────▶│
  │  7. Report success ✅              │
  │◀───────────────────────────────────┤
```

### Testing Ansible Locally

```bash
# Test connectivity to your servers
ansible -i deploy/inventory.ini all -m ping

# Dry-run the deployment (check mode)
ansible-playbook deploy/deploy.yml -i deploy/inventory.ini \
    -e "target_env=staging" --check

# Actual deployment
ansible-playbook deploy/deploy.yml -i deploy/inventory.ini \
    -e "target_env=staging"

# Rollback if something goes wrong
ansible-playbook deploy/rollback.yml -i deploy/inventory.ini \
    -e "target_env=staging"
```

---

## 12. Understanding the Jenkinsfile

Let's break down the key parts of the `Jenkinsfile`:

```groovy
pipeline {
    agent any                     // Run on any available Jenkins agent

    tools {
        maven 'Maven-3.9'        // Use Maven 3.9 (configured in Jenkins)
        jdk   'JDK-25'           // Use JDK 25
    }

    stages {
        stage('SonarQube Analysis') {
            steps {
                // withSonarQubeEnv injects SonarQube server URL and token
                // as environment variables automatically
                withSonarQubeEnv('SonarQube') {
                    sh 'mvn sonar:sonar'
                }
            }
        }

        stage('Quality Gate') {
            steps {
                // This BLOCKS until SonarQube sends a webhook back to Jenkins
                // If the quality gate fails, the pipeline is ABORTED
                waitForQualityGate abortPipeline: true
            }
        }

        stage('Approval') {
            steps {
                // Pipeline PAUSES here until a human clicks "Approve"
                // Only 'admin' or 'release-managers' can approve
                input message: 'Deploy to Production?',
                      submitter: 'admin,release-managers'
            }
        }

        stage('Deploy') {
            steps {
                // Runs the Ansible playbook from Jenkins
                ansiblePlaybook(
                    playbook: 'deploy/deploy.yml',
                    inventory: 'deploy/inventory.ini',
                    credentialsId: 'ansible-ssh-key'
                )
            }
        }
    }
}
```

---

## 13. Understanding the Ansible Playbook

Key concepts in `deploy/deploy.yml`:

```yaml
- name: Deploy Java Application
  hosts: "{{ target_env }}"       # Dynamic: staging or production
  become: yes                      # Run as root (sudo)

  tasks:
    - name: Stop existing application
      # Finds and kills the running Java process gracefully
      shell: "kill $(pgrep -f 'untitled.*\\.jar')"

    - name: Backup current JAR
      # Copies current JAR to backups/ with timestamp for rollback
      copy:
        src: /opt/app/untitled.jar
        dest: /opt/app/backups/untitled-20260312.jar
        remote_src: yes

    - name: Copy new JAR
      # Transfers the new JAR from Jenkins workspace to the server
      copy:
        src: "target/untitled-1.0-SNAPSHOT.jar"
        dest: "/opt/app/untitled.jar"

    - name: Start application via systemd
      # Uses systemd to manage the app as a service
      systemd:
        name: untitled
        state: restarted

    - name: Health check
      # Waits for the app to respond on port 8080
      wait_for:
        port: 8080
        timeout: 60
```

---

## 14. Production Hardening Tips

### Security
- ✅ Store all secrets in **Jenkins Credentials** (never in code)
- ✅ Use **Ansible Vault** for encrypting sensitive variables
- ✅ Enable **RBAC** in Jenkins (Role-Based Access Control plugin)
- ✅ Run Jenkins behind a **reverse proxy** (Nginx/Apache) with HTTPS
- ✅ Restrict who can approve production deployments

### Reliability
- ✅ Set up **Jenkins agents** (don't run builds on the controller)
- ✅ Use **shared libraries** for reusable pipeline code
- ✅ Configure **email/Slack notifications** for pipeline failures
- ✅ Keep **backups** of Jenkins configuration (`/var/jenkins_home`)
- ✅ Use **Blue/Green deployment** or **rolling updates** for zero-downtime

### Monitoring
- ✅ Track **build success rate** and **deployment frequency** (DORA metrics)
- ✅ Monitor SonarQube **technical debt** trend over time
- ✅ Set up **alerting** for failed deployments
- ✅ Use **Prometheus + Grafana** to monitor Jenkins performance

---

## 15. Troubleshooting Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| Jenkins can't connect to GitHub | Wrong credentials or firewall | Verify GitHub PAT, check network/firewall |
| SonarQube Quality Gate timeout | Webhook not configured | Add webhook in SonarQube → Administration → Webhooks |
| `waitForQualityGate` hangs forever | SonarQube webhook can't reach Jenkins | Ensure SonarQube can reach Jenkins URL (check Docker networking) |
| Maven build fails | Wrong JDK version | Verify JDK 25 is installed and configured in Jenkins tools |
| Ansible "Permission denied" | SSH key not authorized | Run `ssh-copy-id` to copy public key to target server |
| OWASP check takes too long | First run downloads NVD database | First run takes ~10min. Add `--noupdate` flag for subsequent runs |
| Docker: SonarQube won't start | Insufficient memory for Elasticsearch | Run `sysctl -w vm.max_map_count=262144` on the Docker host |

---

## 16. Interview Questions & Answers

### Q1: "Explain your CI/CD pipeline end-to-end."

> **Answer**: "When a developer pushes code to GitHub, a webhook notifies Jenkins. Jenkins checks out the code and executes the Jenkinsfile pipeline. First, Maven compiles the code and runs unit tests with JaCoCo coverage. Then, SonarQube analyzes code quality — if the quality gate fails (e.g., coverage below 80% or critical bugs found), the pipeline breaks and the developer is notified. Next, Checkmarx performs a SAST scan for security vulnerabilities. If all checks pass, the JAR artifact is archived and Ansible deploys it to the staging server. After smoke tests pass, a release manager approves the production deployment via an input gate in Jenkins, and Ansible deploys to production. The entire process takes about 10-15 minutes."

---

### Q2: "What is a Quality Gate in SonarQube? Why is it important?"

> **Answer**: "A Quality Gate is a set of threshold conditions — like minimum code coverage, maximum bugs allowed, or acceptable security hotspots. When SonarQube finishes analysis, it evaluates the code against these conditions. If any condition fails, the quality gate status is 'FAILED'. In our Jenkins pipeline, we use `waitForQualityGate abortPipeline: true`, which means a failed quality gate stops the pipeline. This is crucial because it prevents code with poor quality from reaching production — it acts as an automated code reviewer."

---

### Q3: "What is the difference between SAST and DAST?"

> **Answer**: "SAST (Static Application Security Testing) — like Checkmarx — analyzes the **source code** without running the application. It finds vulnerabilities like SQL injection, XSS, and hardcoded credentials during the build phase. DAST (Dynamic Application Security Testing) — like OWASP ZAP — tests the **running application** by sending malicious requests and analyzing responses. SAST is 'white-box' testing (you can see the code), DAST is 'black-box' testing (you test from outside). In a mature pipeline, you use both — SAST in the CI phase and DAST against the staging environment."

---

### Q4: "How does Ansible differ from Chef and Puppet?"

> **Answer**: "The key difference is that Ansible is **agentless** — it connects to servers via SSH and executes tasks. Chef and Puppet require agents installed on every managed node. Ansible uses **YAML playbooks** (human-readable), while Chef uses Ruby DSL and Puppet uses its own declarative language. Ansible is **push-based** (you push changes to servers), while Puppet is pull-based (agents pull configuration from a server). Ansible is also easier to learn, making it great for teams that don't have dedicated config management engineers."

---

### Q5: "How do you handle secrets in your CI/CD pipeline?"

> **Answer**: "We never hardcode secrets in code or Jenkinsfiles. We use three layers: (1) **Jenkins Credentials** store for API tokens, passwords, and SSH keys — referenced in the pipeline by credential ID. (2) **Ansible Vault** for encrypting sensitive variables in playbooks like database passwords. (3) **Environment-level secrets** — each environment (staging, production) has its own secrets, and they're injected at deploy time, not build time. The `withCredentials` block in Jenkins ensures secrets are masked in console output."

---

### Q6: "What is Declarative vs Scripted pipeline in Jenkins?"

> **Answer**: "**Declarative Pipeline** uses a structured `pipeline { }` block with predefined sections like `stages`, `steps`, and `post`. It's easier to read, validates syntax before running, and is the recommended approach. **Scripted Pipeline** uses raw Groovy code in a `node { }` block — it's more flexible but harder to maintain. In our project, we use Declarative Pipeline because it enforces a consistent structure and is easier for the whole team to understand. You can still use `script { }` blocks within Declarative for complex logic."

---

### Q7: "How do you implement a rollback strategy?"

> **Answer**: "We keep the last 5 versioned JARs as backups on each server. When we deploy, Ansible first backs up the current JAR with a timestamp, then deploys the new one. If something goes wrong, we run the `rollback.yml` playbook, which finds the most recent backup and restores it. The rollback takes under 60 seconds. We also use systemd to manage the application lifecycle, so starting/stopping is reliable. For zero-downtime deployments, we'd use blue-green deployment with a load balancer."

---

### Q8: "How do you scale Jenkins for a large team?"

> **Answer**: "Jenkins uses a **controller-agent architecture**. The controller manages the UI, scheduling, and configuration, while **agents (workers)** execute the actual builds. You can add agents as separate VMs, Docker containers, or even Kubernetes pods. With the **Kubernetes plugin**, Jenkins dynamically spins up a pod for each build and destroys it after — so you only use resources during builds. We also use **shared libraries** so all teams reuse the same pipeline code, and we set up **folder-based organization** with RBAC so each team only sees their own jobs."

---

### Q9: "What happens if the SonarQube server goes down during a pipeline run?"

> **Answer**: "The `waitForQualityGate` step has a timeout of 5 minutes. If SonarQube is down, the webhook never fires, Jenkins times out, and the pipeline fails. This is by design — we'd rather fail the pipeline than skip quality checks. In production, we run SonarQube with high availability (PostgreSQL replication, multiple app nodes behind a load balancer). We also have monitoring on SonarQube, so the team gets alerted before it impacts pipelines."

---

### Q10: "What are DORA metrics and how does your pipeline support them?"

> **Answer**: "DORA metrics measure DevOps performance: (1) **Deployment Frequency** — how often you deploy (our pipeline enables multiple deployments per day). (2) **Lead Time for Changes** — time from commit to production (our pipeline takes ~15 minutes). (3) **Change Failure Rate** — percentage of deployments causing failures (reduced by our quality gates and security scans). (4) **Mean Time to Recovery** — how fast you recover from failures (our Ansible rollback takes under 60 seconds). Jenkins tracks build history, so we can measure all four metrics."

---

## 🎯 Quick Reference: File Structure

```
untitled/
├── pom.xml                      # Maven build configuration
├── Jenkinsfile                  # CI/CD pipeline definition
├── docker-compose.yml           # Local infrastructure (Jenkins + SonarQube)
├── sonar-project.properties     # SonarQube scanner configuration
├── .gitignore                   # Git ignore rules
├── CICD_PIPELINE_GUIDE.md       # This guide
├── src/
│   ├── main/java/org/example/
│   │   └── Main.java            # Application code
│   └── test/java/org/example/
│       └── MainTest.java        # Unit tests (10 tests)
└── deploy/
    ├── inventory.ini            # Ansible: target server list
    ├── deploy.yml               # Ansible: deployment playbook
    ├── rollback.yml             # Ansible: rollback playbook
    └── templates/
        └── app.service.j2      # Systemd service template
```

---

## 🚀 Getting Started (TL;DR)

```bash
# 1. Start local infrastructure
docker-compose up -d

# 2. Configure Jenkins (http://localhost:8080)
#    - Install plugins, add credentials, configure SonarQube

# 3. Configure SonarQube (http://localhost:9000)
#    - Create project, generate token, add webhook

# 4. Push to GitHub
git add . && git commit -m "CI/CD pipeline" && git push origin main

# 5. Create Jenkins Pipeline job pointing to your GitHub repo

# 6. Watch the magic happen! 🎉
```

---

*Built with ❤️ for learning CI/CD and DevOps*

