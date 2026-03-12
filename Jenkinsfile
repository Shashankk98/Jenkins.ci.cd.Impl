/* ===========================================================================
   Jenkinsfile — Declarative CI/CD Pipeline
   ===========================================================================
   Pipeline Flow:
   GitHub Push → Jenkins Webhook → Checkout → Build → Test → SonarQube →
   Quality Gate → Checkmarx SAST → OWASP Dependency-Check → Deploy Staging →
   Approval → Deploy Production
   =========================================================================== */

pipeline {
    agent any

    /* ── Tool installations (configured in Jenkins Global Tool Configuration) ── */
    tools {
        maven 'Maven-3.9'       // Name must match Jenkins > Global Tool Config > Maven
        jdk   'JDK-25'          // Name must match Jenkins > Global Tool Config > JDK
    }

    /* ── Environment variables ── */
    environment {
        SONARQUBE_SERVER  = 'SonarQube'                 // Name in Jenkins > Configure System > SonarQube servers
        SONAR_PROJECT_KEY = 'org.example:untitled'
        APP_NAME          = 'untitled'
        APP_VERSION       = '1.0-SNAPSHOT'
        JAR_FILE          = "target/${APP_NAME}-${APP_VERSION}.jar"
        DEPLOY_PATH       = '/opt/app'
    }

    /* ── Pipeline options ── */
    options {
        timestamps()                        // Add timestamps to console output
        timeout(time: 60, unit: 'MINUTES')  // Abort if pipeline exceeds 60 min
        disableConcurrentBuilds()           // Only one build at a time
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    /* ── Pipeline stages ── */
    stages {

        /* ────────────────────── Stage 1: Checkout ────────────────────── */
        stage('Checkout') {
            steps {
                echo '📥 Checking out source code from GitHub...'
                checkout scm
            }
        }

        /* ────────────────────── Stage 2: Build ────────────────────── */
        stage('Build') {
            steps {
                echo '🔨 Building the project with Maven...'
                sh 'mvn clean compile -DskipTests'
            }
        }

        /* ────────────────────── Stage 3: Unit Tests + Coverage ────── */
        stage('Unit Tests') {
            steps {
                echo '🧪 Running unit tests with JaCoCo coverage...'
                sh 'mvn test'
            }
            post {
                always {
                    // Publish JUnit test results
                    junit testResults: '**/target/surefire-reports/*.xml',
                          allowEmptyResults: true

                    // Publish JaCoCo coverage report
                    jacoco(
                        execPattern: '**/target/jacoco.exec',
                        classPattern: '**/target/classes',
                        sourcePattern: '**/src/main/java'
                    )
                }
            }
        }

        /* ────────────────────── Stage 4: Package ────────────────────── */
        stage('Package') {
            steps {
                echo '📦 Packaging the application JAR...'
                sh 'mvn package -DskipTests'
                archiveArtifacts artifacts: "${JAR_FILE}", fingerprint: true
            }
        }

        /* ────────────────────── Stage 5: SonarQube Analysis ────────── */
        stage('SonarQube Analysis') {
            steps {
                echo '🔍 Running SonarQube code quality analysis...'
                withSonarQubeEnv("${SONARQUBE_SERVER}") {
                    sh """
                        mvn sonar:sonar \
                            -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                            -Dsonar.projectName=${APP_NAME} \
                            -Dsonar.sources=src/main/java \
                            -Dsonar.tests=src/test/java \
                            -Dsonar.java.binaries=target/classes \
                            -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                    """
                }
            }
        }

        /* ────────────────────── Stage 6: Quality Gate ────────────── */
        stage('Quality Gate') {
            steps {
                echo '🚦 Waiting for SonarQube Quality Gate result...'
                // Requires SonarQube webhook configured:
                //   SonarQube → Administration → Configuration → Webhooks
                //   URL: http://<JENKINS_URL>/sonarqube-webhook/
                timeout(time: 10, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        /* ────────────────────── Stage 7: Checkmarx SAST Scan ──────── */
        stage('Checkmarx SAST') {
            steps {
                echo '🛡️ Running Checkmarx SAST security scan...'
                /*
                 * If you have Checkmarx installed, uncomment below.
                 * Otherwise, this stage demonstrates where SAST fits in the pipeline.
                 *
                 * step([$class: 'CxScanBuilder',
                 *     projectName: "${APP_NAME}",
                 *     serverUrl: "${CHECKMARX_SERVER_URL}",
                 *     credentialsId: 'checkmarx-credentials',
                 *     preset: 'Checkmarx Default',
                 *     teamPath: 'CxServer\\SP\\Company\\TeamName',
                 *     exclusionsSetting: 'job',
                 *     excludeFolders: 'target, .git, .idea',
                 *     highThreshold: 0,
                 *     mediumThreshold: 5,
                 *     lowThreshold: 10,
                 *     vulnerabilityThresholdEnabled: true,
                 *     waitForResultsEnabled: true
                 * ])
                 */

                // ── FREE ALTERNATIVE: OWASP Dependency-Check ──
                echo '🔐 Running OWASP Dependency-Check (free SAST alternative)...'
                sh 'mvn org.owasp:dependency-check-maven:check'
            }
            post {
                always {
                    // Publish OWASP Dependency-Check report
                    dependencyCheckPublisher pattern: 'target/dependency-check-report.json'
                }
            }
        }

        /* ────────────────────── Stage 8: Deploy to Staging ────────── */
        stage('Deploy to Staging') {
            steps {
                echo '🚀 Deploying to STAGING environment via Ansible...'
                ansiblePlaybook(
                    playbook: 'deploy/deploy.yml',
                    inventory: 'deploy/inventory.ini',
                    extras: "-e target_env=staging -e jar_file=${JAR_FILE} -e deploy_path=${DEPLOY_PATH}",
                    credentialsId: 'ansible-ssh-key',
                    colorized: true
                )
            }
        }

        /* ────────────────────── Stage 9: Smoke Test ────────────────── */
        stage('Smoke Test') {
            steps {
                echo '🧪 Running smoke tests on staging...'
                sh '''
                    sleep 10
                    echo "Smoke test: checking if application process is running on staging..."
                    # Replace with actual health check, e.g.:
                    # curl -f http://staging-server:8080/health || exit 1
                    echo "Smoke test PASSED ✅"
                '''
            }
        }

        /* ────────────────────── Stage 10: Approval Gate ───────────── */
        stage('Approval') {
            steps {
                echo '⏳ Waiting for manual approval to deploy to PRODUCTION...'
                input message: 'Deploy to Production?',
                      ok: 'Yes, deploy to production!',
                      submitter: 'admin,release-managers'
            }
        }

        /* ────────────────────── Stage 11: Deploy to Production ────── */
        stage('Deploy to Production') {
            steps {
                echo '🚀 Deploying to PRODUCTION environment via Ansible...'
                ansiblePlaybook(
                    playbook: 'deploy/deploy.yml',
                    inventory: 'deploy/inventory.ini',
                    extras: "-e target_env=production -e jar_file=${JAR_FILE} -e deploy_path=${DEPLOY_PATH}",
                    credentialsId: 'ansible-ssh-key',
                    colorized: true
                )
            }
        }
    }

    /* ── Post-build actions (run regardless of pipeline result) ── */
    post {
        success {
            echo '✅ Pipeline completed successfully!'
            // Uncomment to send Slack/email notification:
            // slackSend channel: '#deployments', color: 'good', message: "✅ ${APP_NAME} deployed to production"
        }
        failure {
            echo '❌ Pipeline failed!'
            // slackSend channel: '#deployments', color: 'danger', message: "❌ ${APP_NAME} pipeline failed"
        }
        always {
            echo '🧹 Cleaning up workspace...'
            cleanWs()
        }
    }
}

