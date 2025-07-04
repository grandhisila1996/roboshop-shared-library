def call(Map configMap) {
    pipeline {
        agent any

        environment {
            packageVersion = ''
            nexusURL = '35.175.173.5:8081'
            BRANCH_NAME = "${env.BRANCH_NAME ?: env.GIT_BRANCH?.replaceAll('origin/', '') ?: 'unknown'}"
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
                        echo "Branch Name: ${env.BRANCH_NAME}"
                    }
                }
            }

            stage('Install dependencies') {
                steps {
                    sh 'npm install'
                }
            }

            stage('Unit tests') {
                steps {
                    sh 'echo "unit tests will run here"'
                }
            }

            stage('Sonar Scan') {
                steps {
                    sh 'sonar-scanner'
                }
            }

            stage('Build') {
                steps {
                    script {
                        def artifactName = "${configMap.component}-${packageVersion}-${BRANCH_NAME}.zip"
                        env.ARTIFACT_NAME = artifactName
                        sh """
                            ls -la
                            zip -q -r ${artifactName} ./* -x ".git" -x "*.zip"
                            ls -ltr
                        """
                    }
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
                                classifier: "${BRANCH_NAME}",
                                file: "${ARTIFACT_NAME}",
                                type: 'zip'
                            ]
                        ]
                    )
                }
            }
        }
    }
}
