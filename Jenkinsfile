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
                sh "rm -rf ${WWW_DIR}/*"
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
