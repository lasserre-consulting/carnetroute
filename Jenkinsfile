pipeline {
    agent any

    environment {
        DOCKER_REGISTRY = 'your-registry.example.com'
        IMAGE_BACKEND  = "${DOCKER_REGISTRY}/carnetroute-backend"
        IMAGE_FRONTEND = "${DOCKER_REGISTRY}/carnetroute-frontend"
        KUBE_NAMESPACE = 'carnetroute'
        HELM_RELEASE   = 'carnetroute'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Backend - Build & Test') {
            steps {
                dir('backend') {
                    sh './gradlew clean build --no-daemon'
                    sh './gradlew test --no-daemon'
                }
            }
            post {
                always {
                    junit 'backend/build/test-results/test/*.xml'
                }
            }
        }

        stage('Frontend - Install & Build') {
            steps {
                dir('frontend') {
                    sh 'npm ci'
                    sh 'npx ng build --configuration production'
                }
            }
        }

        stage('Docker - Build Images') {
            parallel {
                stage('Backend Image') {
                    steps {
                        dir('backend') {
                            sh "docker build -t ${IMAGE_BACKEND}:${BUILD_NUMBER} -t ${IMAGE_BACKEND}:latest ."
                        }
                    }
                }
                stage('Frontend Image') {
                    steps {
                        dir('frontend') {
                            sh "docker build -t ${IMAGE_FRONTEND}:${BUILD_NUMBER} -t ${IMAGE_FRONTEND}:latest ."
                        }
                    }
                }
            }
        }

        stage('Docker - Push Images') {
            when {
                branch 'main'
            }
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'docker-registry-creds',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh "echo ${DOCKER_PASS} | docker login ${DOCKER_REGISTRY} -u ${DOCKER_USER} --password-stdin"
                    sh "docker push ${IMAGE_BACKEND}:${BUILD_NUMBER}"
                    sh "docker push ${IMAGE_BACKEND}:latest"
                    sh "docker push ${IMAGE_FRONTEND}:${BUILD_NUMBER}"
                    sh "docker push ${IMAGE_FRONTEND}:latest"
                }
            }
        }

        stage('Deploy - Helm Upgrade') {
            when {
                branch 'main'
            }
            steps {
                withKubeConfig(credentialsId: 'kubeconfig-creds') {
                    sh """
                        helm upgrade --install ${HELM_RELEASE} ./helm/carnetroute \
                            --namespace ${KUBE_NAMESPACE} \
                            --create-namespace \
                            --set image.backend.repository=${IMAGE_BACKEND} \
                            --set image.backend.tag=${BUILD_NUMBER} \
                            --set image.frontend.repository=${IMAGE_FRONTEND} \
                            --set image.frontend.tag=${BUILD_NUMBER} \
                            --wait --timeout 300s
                    """
                }
            }
        }

        stage('Verify Deployment') {
            when {
                branch 'main'
            }
            steps {
                withKubeConfig(credentialsId: 'kubeconfig-creds') {
                    sh "kubectl rollout status deployment/carnetroute-backend -n ${KUBE_NAMESPACE} --timeout=120s"
                    sh "kubectl rollout status deployment/carnetroute-frontend -n ${KUBE_NAMESPACE} --timeout=120s"
                    sh "kubectl get pods -n ${KUBE_NAMESPACE}"
                }
            }
        }
    }

    post {
        success {
            echo '✅ Pipeline completed successfully!'
        }
        failure {
            echo '❌ Pipeline failed.'
        }
        always {
            sh 'docker system prune -f || true'
        }
    }
}
