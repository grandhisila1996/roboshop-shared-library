def call(Map configMap) {
    pipeline {
        agent any

        environment {
            packageVersion = ''
            nexusURL = '35.175.173.5:8081'
        }

        options {
            timeout(time: 1, unit: 'HOURS')
            disableConcurrentBuilds()
        }

        parameters {
            booleanParam(name: 'Deploy', defaultValue: false, description: 'Toggle this value')
        }

        stages {
            stage('Get the version') {
                steps {
                    script {
                        def packageJson = readJSON file: 'package.json'
                        packageVersion = packageJson.version
                        echo "application version: $packageVersion"
                    }
                }
            }

            stage('Install dependencies') {
                steps {
                    sh """
                        npm install
                    """
                }
            }

            stage('Unit tests') {
                steps {
                    sh """
                        echo "unit tests will run here"
                    """
                }
            }

            stage('Sonar Scan') {
                steps {
                    sh """
                        sonar-scanner
                    """
                }
            }

            stage('Build') {
                steps {
                    sh """
                        ls -la
                        zip -q -r ${configMap.component}.zip ./* -x ".git" -x "*.zip"
                        ls -ltr
                    """
                }
            }

            stage('Publish Artifact') {
                steps {
                    nexusArtifactUploader(
                        nexusVersion: 'nexus3',
                        protocol: 'http',
                        nexusUrl: pipelineGlobals.nexusURL(),
                        groupId: 'com.roboshop',
                        version: "${packageVersion}",
                        repository: "${configMap.component}",
                        credentialsId: 'nexus-auth',
                        artifacts: [
                            [
                                artifactId: "${configMap.component}",
                                classifier: '',
                                file: "${configMap.component}.zip",
                                type: 'zip'
                            ]
                        ]
                    )
                }
            }
        }
    } 
} 
