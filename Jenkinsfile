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
    image: docker:18-git
    tty: true
    volumeMounts:
    - mountPath: /var/run/docker.sock
      name: docker-sock
  volumes:
  - name: docker-sock
    hostPath:
      path: /var/run/docker.sock
      type: File
"""
        }
    }
    stages {
        stage('Build image') {
            steps {
                container('docker') {
                    withCredentials([usernamePassword(credentialsId:'8d0aaceb-2a19-4f92-ae37-5b61e4c0feb8', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                        sh 'docker login -u $USERNAME -p $PASSWORD'
                    }
                    // DNS error if --network is default
                    sh 'docker build --network=host . -t overture/program-service:$(git describe --always)'
                    sh 'docker push overture/program-service:$(git describe --always)'
                }
            }
        }
    }
}
