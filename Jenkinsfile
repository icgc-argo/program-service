def dockerRegistry = "ghcr.io"
def githubRepo = "icgc-argo/program-service"
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
  - name: docker
    image: docker:18-git
    tty: true
    env:
      - name: DOCKER_HOST
        value: tcp://localhost:2375
      - name: HOME
        value: /home/jenkins/agent
  - name: java
    image: openjdk:11-jdk-slim
    command:
    - cat
    tty: true
    env:
      - name: DOCKER_HOST
        value: tcp://localhost:2375
      - name: HOME
        value: /home/jenkins/agent
  - name: postgres
    image: postgres:11.2-alpine
    securityContext:
      runAsUser: 70
    env:
      - name: POSTGRES_DB
        value: program_db
      - name: HOME
        value: /home/jenkins/agent
  - name: dind-daemon
    image: docker:18.06-dind
    securityContext:
      privileged: true
      runAsUser: 0
    volumeMounts:
      - name: docker-graph-storage
        mountPath: /var/lib/docker
  securityContext:
    runAsUser: 1000
  volumes:
  - name: docker-graph-storage
    emptyDir: {}
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
                container('java') {
                    sh "./fly.sh migrate"
                    sh "./mvnw clean verify"
                }
            }
        }

        stage('Build & Publish Develop') {
            when { branch 'develop' }
            steps {
               container('docker') {
                    withCredentials([usernamePassword(credentialsId:'argoContainers', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                        sh "docker login ${dockerRegistry} -u $USERNAME -p $PASSWORD"
                    }
                    sh "docker build --network=host -f Dockerfile . -t ${dockerRegistry}/${githubRepo}:edge -t ${dockerRegistry}/${githubRepo}:${commit}"
                    sh "docker push ${dockerRegistry}/${githubRepo}:edge"
                    sh "docker push ${dockerRegistry}/${githubRepo}:${commit}"
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
                        sh "git push https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/${githubRepo} --tags"
                    }
                    withCredentials([usernamePassword(credentialsId:'argoContainers', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                        sh "docker login ${dockerRegistry} -u $USERNAME -p $PASSWORD"
                    }
                    sh "docker build --network=host -f Dockerfile . -t ${dockerRegistry}/${githubRepo}:latest -t ${dockerRegistry}/${githubRepo}:${version}"
                    sh "docker push ${dockerRegistry}/${githubRepo}:${version}"
                    sh "docker push ${dockerRegistry}/${githubRepo}:latest"
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
