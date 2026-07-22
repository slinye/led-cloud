// ============================================
// LED Cloud Jenkins Pipeline (声明式)
// 阶段: Checkout → Build → Docker → Push → Deploy
// ============================================
pipeline {
    agent any

    environment {
        DOCKER_REGISTRY = credentials('docker-registry-url')      // Docker 仓库地址
        DOCKER_CREDS    = credentials('docker-registry-creds')     // username:password
        PROJECT_NAME    = 'led-cloud'
        JAVA_HOME       = tool name: 'JDK8', type: 'jdk'
        MAVEN_HOME      = tool name: 'Maven3', type: 'maven'
        NODE_HOME       = tool name: 'Node20', type: 'nodejs'
    }

    parameters {
        booleanParam(name: 'SKIP_TESTS', defaultValue: true, description: '跳过测试')
        booleanParam(name: 'DEPLOY', defaultValue: false, description: '是否自动部署到服务器')
        choice(name: 'DEPLOY_ENV', choices: ['dev', 'staging', 'prod'], description: '部署环境')
    }

    stages {
        // ==================== 1. 检出代码 ====================
        stage('Checkout') {
            steps {
                script {
                    GIT_COMMIT = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    GIT_BRANCH = env.BRANCH_NAME ?: sh(script: 'git rev-parse --abbrev-ref HEAD', returnStdout: true).trim()
                    IMAGE_TAG = "${GIT_COMMIT}"
                    currentBuild.displayName = "#${BUILD_NUMBER} - ${GIT_BRANCH} (${IMAGE_TAG})"
                }
                echo "�� 分支: ${GIT_BRANCH}  提交: ${GIT_COMMIT}"
            }
        }

        // ==================== 2. 后端编译 (Maven) ====================
        stage('Build Backend') {
            steps {
                sh '''
                    ${MAVEN_HOME}/bin/mvn clean package \
                        -DskipTests=${params.SKIP_TESTS} \
                        -pl led-common,led-gateway,led-auth,led-device,led-content,led-schedule \
                        -am --batch-mode --no-transfer-progress
                '''
            }
            post {
                success {
                    echo '✅ Maven 编译成功'
                    // 存档产物
                    archiveArtifacts artifacts: 'led-gateway/target/*.jar, led-auth/target/*.jar, led-device/target/*.jar, led-content/target/*.jar, led-schedule/target/*.jar', fingerprint: true
                }
                failure {
                    echo '❌ Maven 编译失败'
                }
            }
        }

        // ==================== 3. 前端编译 ====================
        stage('Build Frontend') {
            steps {
                dir('frontend') {
                    sh '''
                        npm ci
                        npm run build
                    '''
                }
            }
            post {
                success {
                    echo '✅ 前端编译成功'
                }
            }
        }

        // ==================== 4. Docker 构建 ====================
        stage('Docker Build') {
            steps {
                script {
                    def images = [
                        [ context: 'led-gateway',     name: 'led-gateway' ],
                        [ context: 'led-auth',        name: 'led-auth' ],
                        [ context: 'led-device',      name: 'led-device' ],
                        [ context: 'led-content',     name: 'led-content' ],
                        [ context: 'led-schedule',    name: 'led-schedule' ],
                        [ context: 'docker/sentinel', name: 'led-sentinel' ],
                        [ context: 'frontend',        name: 'led-frontend' ]
                    ]

                    // 先构建基础镜像
                    sh """
                        docker build -t led-cloud-base:latest ./docker/java-base
                    """

                    // 并行构建所有服务镜像
                    def buildTasks = [:]
                    images.each { img ->
                        buildTasks[img.name] = {
                            sh """
                                docker build -t ${DOCKER_REGISTRY}/${img.name}:${IMAGE_TAG} \
                                             -t ${DOCKER_REGISTRY}/${img.name}:latest \
                                             ${img.context}
                            """
                        }
                    }
                    parallel buildTasks
                }
            }
        }

        // ==================== 5. 推送到 Registry ====================
        stage('Docker Push') {
            when {
                anyOf {
                    branch 'main'
                    branch 'master'
                    branch 'develop'
                    buildingTag()
                }
            }
            steps {
                script {
                    withCredentials([usernamePassword(
                        credentialsId: 'docker-registry-creds',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )]) {
                        sh "docker login -u ${DOCKER_USER} -p ${DOCKER_PASS} ${DOCKER_REGISTRY}"
                    }

                    def services = ['led-cloud-base', 'led-gateway', 'led-auth',
                                    'led-device', 'led-content', 'led-schedule',
                                    'led-sentinel', 'led-frontend']

                    def pushTasks = [:]
                    services.each { svc ->
                        pushTasks[svc] = {
                            sh """
                                docker push ${DOCKER_REGISTRY}/${svc}:${IMAGE_TAG}
                                docker push ${DOCKER_REGISTRY}/${svc}:latest
                            """
                        }
                    }
                    parallel pushTasks

                    echo "�� 镜像已推送: ${DOCKER_REGISTRY}  Tags: ${IMAGE_TAG}, latest"
                }
            }
        }

        // ==================== 6. 自动部署 ====================
        stage('Deploy') {
            when {
                expression { params.DEPLOY }
            }
            steps {
                script {
                    def deployHost  = params.DEPLOY_ENV == 'prod' ? 'prod-server' : 'dev-server'
                    def deployPath  = params.DEPLOY_ENV == 'prod' ? '/opt/led-cloud' : '/opt/led-cloud-dev'

                    sshagent(credentials: ['deploy-ssh-key']) {
                        sh """
                            ssh -o StrictHostKeyChecking=no ${deployHost} '
                                cd ${deployPath} &&
                                docker compose -f docker-compose.${params.DEPLOY_ENV}.yml pull &&
                                docker compose -f docker-compose.${params.DEPLOY_ENV}.yml up -d --remove-orphans &&
                                docker image prune -f
                            '
                        """
                    }

                    echo "✅ 部署完成 → ${params.DEPLOY_ENV} 环境"
                }
            }
        }
    }

    // ==================== 构建后处理 ====================
    post {
        always {
            cleanWs(
                deleteDirs: true,
                patterns: [
                    [pattern: 'led-*/target/', type: 'INCLUDE'],
                    [pattern: 'frontend/dist/', type: 'INCLUDE'],
                    [pattern: 'frontend/node_modules/', type: 'INCLUDE']
                ]
            )
        }
        success {
            emailext(
                to: '${DEFAULT_RECIPIENTS}',
                subject: "[Jenkins] ${PROJECT_NAME} #${BUILD_NUMBER} 构建成功",
                body: """
                    <h3>构建成功 ✅</h3>
                    <table>
                    <tr><td>项目:</td><td>${PROJECT_NAME}</td></tr>
                    <tr><td>分支:</td><td>${GIT_BRANCH}</td></tr>
                    <tr><td>版本:</td><td>${IMAGE_TAG}</td></tr>
                    <tr><td>镜像:</td><td>${DOCKER_REGISTRY}</td></tr>
                    <tr><td>构建日志:</td><td><a href="${BUILD_URL}">${BUILD_URL}</a></td></tr>
                    </table>
                """
            )
        }
        failure {
            emailext(
                to: '${DEFAULT_RECIPIENTS}',
                subject: "[Jenkins] ${PROJECT_NAME} #${BUILD_NUMBER} 构建失败",
                body: """
                    <h3>构建失败 ❌</h3>
                    <p>项目: ${PROJECT_NAME} | 分支: ${GIT_BRANCH}</p>
                    <p>构建日志: <a href="${BUILD_URL}">${BUILD_URL}</a></p>
                """
            )
        }
    }
}
