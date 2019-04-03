pipeline {
    agent {
        kubernetes {
            label 'program-service-executor'
            yaml """
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: docker
    image: docker:18
    tty: true
    volumeMounts:
    - mountPath: "/var/run/docker.sock"
      name: "docker-sock"
  volumes:
  - name: "docker-sock"
    hostPath: "/var/run/docker.sock"
"""
        }
    }
    stages {
        stage('Build image') {
            steps {
                container('docker') {
                    sh 'docker build .'
                }
            }
        }
    }
}
