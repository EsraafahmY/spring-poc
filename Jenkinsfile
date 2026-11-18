// CI pipeline: build -> test -> docker images -> push -> bump Helm tag in Git.
// CD is done by ArgoCD, which notices the Git change and deploys it (GitOps).
pipeline {
    agent any

    tools {
        maven 'maven-3.9'   // configured in Manage Jenkins > Tools
        jdk 'jdk-21'
    }

    environment {
        REGISTRY      = 'docker.io/your-dockerhub-user'
        IMAGE_TAG     = "${env.BUILD_NUMBER}-${env.GIT_COMMIT.take(7)}"
        MODULES       = 'config-server discovery-server api-gateway product-service order-service'
        VALUES_FILE   = 'helm/spring-poc/values.yaml'
    }

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    stages {
        stage('Build & Unit Test') {
            steps {
                sh 'mvn -B clean verify'
            }
            post {
                always { junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml' }
            }
        }

        stage('Docker Build') {
            steps {
                script {
                    def builds = [:]
                    env.MODULES.split(' ').each { m ->
                        builds[m] = {
                            sh "docker build --build-arg MODULE=${m} -t ${REGISTRY}/${m}:${IMAGE_TAG} ."
                        }
                    }
                    parallel builds
                }
            }
        }

        stage('Docker Push') {
            when { branch 'main' }
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker-registry-creds',
                                                  usernameVariable: 'REG_USER', passwordVariable: 'REG_PASS')]) {
                    sh 'echo "$REG_PASS" | docker login -u "$REG_USER" --password-stdin'
                    script {
                        env.MODULES.split(' ').each { m -> sh "docker push ${REGISTRY}/${m}:${IMAGE_TAG}" }
                    }
                }
            }
        }

        stage('Update Helm values (GitOps)') {
            when { branch 'main' }
            steps {
                withCredentials([usernamePassword(credentialsId: 'git-creds',
                                                  usernameVariable: 'GIT_USER', passwordVariable: 'GIT_TOKEN')]) {
                    sh '''
                        sed -i "s|^  imageTag: .*|  imageTag: \\"${IMAGE_TAG}\\"|" ${VALUES_FILE}
                        git config user.email "jenkins@ci.local"
                        git config user.name  "jenkins"
                        git add ${VALUES_FILE}
                        git commit -m "ci: deploy ${IMAGE_TAG} [skip ci]" || echo "nothing to commit"
                        git push https://${GIT_USER}:${GIT_TOKEN}@github.com/your-org/spring-poc.git HEAD:main
                    '''
                }
            }
        }
    }

    post {
        success { echo "Images ${IMAGE_TAG} pushed. ArgoCD will sync the new tag to Kubernetes." }
        always  { sh 'docker image prune -f || true' }
    }
}
