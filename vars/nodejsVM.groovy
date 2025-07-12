def call(Map configMap) {
    pipeline {
        agent any

        environment {
            packageVersion = ''
            nexusURL = 'http://localhost:8081/'
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
                    bat 'npm install'
                }
            }

            stage('Unit tests') {
                steps {
                    bat 'echo Unit tests will run here'
                }
            }

            stage('Sonar Scan') {
                steps {
                    bat 'sonar-scanner'
                }
            }

            stage('Build') {
                steps {
                    bat """
                        echo Zipping artifact for branch: ${branchName}
                        powershell Compress-Archive -Path * -DestinationPath ${configMap.component}-${branchName}.zip -Force
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
