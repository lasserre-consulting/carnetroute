pipeline {
  agent any

  environment {
    PROJECT_DIR = '/home/ubuntu/projects/carnetroute'
  }

  stages {

    stage('Checkout') {
      steps {
        dir(PROJECT_DIR) {
          sh 'git fetch origin master'
          sh 'git reset --hard origin/master'
        }
      }
    }

    stage('Build') {
      steps {
        dir(PROJECT_DIR) {
          sh 'docker compose build backend frontend'
        }
      }
    }

    stage('Deploy') {
      steps {
        dir(PROJECT_DIR) {
          sh 'docker compose up -d'
        }
      }
    }

    stage('Healthcheck') {
      steps {
        sh 'sleep 15'
        sh 'curl -sf http://127.0.0.1:8080/api/health || (echo "Backend healthcheck failed" && exit 1)'
        sh 'curl -sf http://127.0.0.1:4202/ || (echo "Frontend healthcheck failed" && exit 1)'
      }
    }

  }

  post {
    failure {
      echo 'Deploy failed — vérifier les logs avec: docker compose logs'
    }
  }
}
