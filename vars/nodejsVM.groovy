def call(Map configMap) {
    pipeline {
        agent any

        environment {
            packageVersion = ''
            nexusURL = '35.175.173.5:8081'
            branchName = ''
        }

        options {
            timeout(time: 1, unit: 'HOURS')
            disableConcurrentBuilds()
        }

        parameters {
            booleanParam(name: 'Deploy', defaultValue: false, description: 'Toggle this value')
        }

        stages {
            stage('Set Branch Info') {
                steps {
                    script {
                        // Get branch name cleanly
                        branchName = env.BRANCH_NAME ?: env.GIT_BRANCH?.replaceAll('origin/', '') ?: 'unknown'
                        echo "Triggered by branch: ${branchName}"
                    }
                }
            }

            stage('Get the version') {
                steps {
                    script {
                        def packageJson = readJSON file: 'package.json'
                        packageVersion = packageJson.version
                        echo "Application version: $packageVersion"
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
                    sh 'echo "Unit tests will run here"'
                }
            }

            stage('Sonar Scan') {
                steps {
                    sh 'sonar-scanner'
                }
            }

            stage('Build') {
                steps {
                    sh """
                        echo "Zipping artifact for branch: ${branchName}"
                        zip -q -r ${configMap.component}-${branchName}.zip ./* -x ".git" -x "*.zip"
                    """
                }
            }

            stage('Publish Artifact') {
                steps {
                    nexusArtifactUploader(
                        nexusVersion: 'nexus3',
                        protocol: 'http',
                        nexusUrl: pipelineGlobals.nexusURL(),
                        groupId: "com.roboshop.${branchName}",
                        version: "${packageVersion}",
                        repository: "${configMap.component}",
                        credentialsId: 'nexus-auth',
                        artifacts: [[
                            artifactId: "${configMap.component}",
                            classifier: '',
                            file: "${configMap.component}-${branchName}.zip",
                            type: 'zip'
                        ]]
                    )
                }
            }
        }
    } 
}
