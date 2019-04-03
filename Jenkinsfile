pipeline {
    agent {
        kubernetes {
            label 'program-service-executor'
            defaultContainer 'jnlp'
            yaml """
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: java
    image: openjdk:11-jdk
    tty: true
"""
        }
    }
    stages {
        stage('Run maven') {
            steps {
                container('java') {
                    sh './mvnw clean package'
                }
            }
        }
    }
}
