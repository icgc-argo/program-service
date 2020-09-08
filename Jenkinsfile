def version = "UNKNOWN"
def commit = "UNKNOWN"

pipeline {
    agent {
        kubernetes {
            label 'programservice-executor'
            yaml """
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: helm
    image: alpine/helm:2.12.3
    command:
    - cat
    tty: true
  - name: jdk
    tty: true
    image: openjdk:11-jdk
    env:
    - name: DOCKER_HOST
      value: tcp://localhost:2375
  - name: docker
    image: docker:18-git
    tty: true
    volumeMounts:
    - mountPath: /var/run/docker.sock
      name: docker-sock
  - name: dind-daemon
    image: docker:18.06-dind
    securityContext:
      privileged: true
    volumeMounts:
    - name: docker-graph-storage
      mountPath: /var/lib/docker
  volumes:
  - name: docker-graph-storage
    emptyDir: {}
  - name: docker-sock
    hostPath:
      path: /var/run/docker.sock
      type: File
"""
        }
    }
    stages {
        stage('Prepare') {
            steps {
                script {
                    commit = sh(returnStdout: true, script: 'git describe --always').trim()
                }
                script {
                    version = readMavenPom().getVersion()
                }
            }
        }
        stage('Test') {
            steps {
                container('jdk') {
                    sh "./fly.sh migrate"
                    sh "./mvnw clean verify"
                }
            }
        }

        stage('Build & Publish Develop') {
            when { branch 'develop' }
            steps {
               container('docker') {
                    withCredentials([usernamePassword(credentialsId:'argoDockerHub', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                        sh 'docker login -u $USERNAME -p $PASSWORD'
                    }
                    sh "docker build --network=host -f Dockerfile . -t icgcargo/program-service:edge -t icgcargo/program-service:${commit}"
                    sh "docker push icgcargo/program-service:edge"
                    sh "docker push icgcargo/program-service:${commit}"
               }
            }
        }

        stage('Deploy to argo-dev') {
            when { branch 'develop' }
            steps {
                build(job: "/ARGO/provision/program-service", parameters: [
                     [$class: 'StringParameterValue', name: 'AP_ARGO_ENV', value: 'dev' ],
                     [$class: 'StringParameterValue', name: 'AP_ARGS_LINE', value: "--set-string image.tag=${commit}" ]
                ])
            }
        }

        stage('Release & tag') {
            when { branch 'master' }
            steps {
               container('docker') {
                   withCredentials([usernamePassword(credentialsId: 'argoGithub', passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
                        sh "git tag ${version}"
                        sh "git push https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/icgc-argo/program-service --tags"
                    }
                    withCredentials([usernamePassword(credentialsId:'argoDockerHub', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                        sh 'docker login -u $USERNAME -p $PASSWORD'
                    }
                    sh "docker build --network=host -f Dockerfile . -t icgcargo/program-service:latest -t icgcargo/program-service:${version}"
                    sh "docker push icgcargo/program-service:${version}"
                    sh "docker push icgcargo/program-service:latest"
                }
            }
        }

        stage('Deploy to argo-qa') {
            when { branch 'master' }
            steps {
                build(job: "/ARGO/provision/program-service", parameters: [
                     [$class: 'StringParameterValue', name: 'AP_ARGO_ENV', value: 'qa' ],
                     [$class: 'StringParameterValue', name: 'AP_ARGS_LINE', value: "--set-string image.tag=${version}" ]
                ])
            }
        }

    }

    post {
        always {
            junit "**/TEST-*.xml"
       }
    }
 
}
