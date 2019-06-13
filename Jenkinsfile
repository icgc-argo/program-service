def commit = "UNKNOWN"

pipeline {
    agent {
        kubernetes {
            label 'program-service-executor'
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
  - name: docker
    image: docker:18-git
    tty: true
    env:
      - name: DOCKER_HOST
        value: tcp://localhost:2375
  - name: java
    image: openjdk:11-jdk-slim
    command:
    - cat
    tty: true
    env:
      - name: DOCKER_HOST
        value: tcp://localhost:2375
  - name: postgres
    image: postgres:11.2-alpine
    env:
    - name: POSTGRES_DB
      value: program_db
  - name: curl
    image: pstauffer/curl
    tty: true

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
"""
        }
    }
    stages {

    /*
        stage('Test') {
            // TODO: integration test
            steps {
                container('java') {
                    sh "./fly.sh migrate"
                    //sh "./mvnw verify"
                }
            }
        }

        stage('Build') {
            steps {
                container('docker') {
                    withCredentials([usernamePassword(credentialsId:'8d0aaceb-2a19-4f92-ae37-5b61e4c0feb8', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                        sh 'docker login -u $USERNAME -p $PASSWORD'
                    }
                    script {
                        commit = sh(returnStdout: true, script: 'git describe --always').trim()
                    }

                    // DNS error if --network is default
                    sh "docker build --network=host . -t overture/program-service:${commit}"

                    sh "docker push overture/program-service:${commit}"
                }
            }
        }

        stage('Deploy qa - deprecated') {
            when { branch 'master' }
            steps {
                container('helm') {
                    withCredentials([file(credentialsId:'4ed1e45c-b552-466b-8f86-729402993e3b', variable: 'KUBECONFIG')]) {
                        sh 'helm init --client-only'
                        sh 'helm ls'
                        sh 'helm repo add argo  https://icgc-argo.github.io/charts/'
                        sh "helm upgrade program-service-qa argo/program-service --reuse-values --set-string image.tag=${commit}"
                    }
                }
            }
        }
    */
        stage('Deploy to argo QA') {
            when { branch 'feature/update-pipeline' }
            steps {
                container('curl') {
                    sh "env"
                    withCredentials([string(credentialsId:'REMOTE_BUILD_TOKEN', variable: 'REMOTE_BUILD_TOKEN')]) {
                            sh "curl -v https://jenkins.qa.cancercollaboratory.org/job/ARGO/job/provision/job/program-service/buildWithParameters?token=${REMOTE_BUILD_TOKEN}&AP_ARGO_ENV=qa&AP_ARGS_LINE=--set%20image.tag%3D${commit}"
                    }
                }
            }
        }
    }

    /*
    post {
        always {
            junit "**/TEST-*.xml"
        }
    }*/
}
