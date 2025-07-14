def call(Map configMap) {
    pipeline {
        agent any

        environment {
            packageVersion = ''
            nexusURL = 'http://localhost:8081/'
            branchName = ''
            SONAR_HOST_URL = 'http://localhost:9000/' // required by sonar-scanner
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

stage('Sonarqube Analysis') {
    steps {
        withCredentials([string(credentialsId: 'sonarqube', variable: 'sonarqube')]) {
            bat 'echo Token value: %sonarqube%'  // debug line (will be masked)
            bat '''
                sonar-scanner ^
                -Dsonar.projectKey=catalogue ^
                -Dsonar.sources=. ^
                -Dsonar.host.url=http://localhost:9000 ^
                -Dsonar.token=%sonarqube%
            '''
        }
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
