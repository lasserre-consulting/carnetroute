pipeline {
    agent any

    environment {
        PROJECT_DIR = '/home/ubuntu/projects/carnetroute'
        WWW_DIR     = '/var/www/carnetroute'
    }

    stages {

        stage('Checkout') {
            steps {
                dir("${PROJECT_DIR}") {
                    sh 'git pull origin master'
                }
            }
        }

        stage('Frontend - Build') {
            steps {
                dir("${PROJECT_DIR}/frontend") {
                    sh 'npm ci'
                    sh 'npx ng build --configuration production'
                }
            }
        }

        stage('Frontend - Deploy') {
            steps {
                sh '''
                    if [ -z "${WWW_DIR}" ] || [ ! -d "${WWW_DIR}" ]; then
                        echo "WWW_DIR invalide ou absent : ${WWW_DIR}"
                        exit 1
                    fi
                    rm -rf "${WWW_DIR:?}/"*
                '''
                sh "cp -r ${PROJECT_DIR}/frontend/dist/carnetroute/browser/* ${WWW_DIR}/"
            }
        }

        stage('Backend - Build & Deploy') {
            steps {
                dir("${PROJECT_DIR}") {
                    sh 'docker compose up -d --build backend'
                }
            }
        }

    }

    post {
        success {
            echo 'carnetroute déployé avec succès.'
        }
        failure {
            echo 'Echec du pipeline carnetroute.'
        }
    }
}
