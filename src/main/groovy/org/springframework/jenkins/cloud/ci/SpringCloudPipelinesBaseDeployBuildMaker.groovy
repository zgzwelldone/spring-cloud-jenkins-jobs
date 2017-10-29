package org.springframework.jenkins.cloud.ci

import javaposse.jobdsl.dsl.DslFactory

import org.springframework.jenkins.cloud.common.SpringCloudJobs
import org.springframework.jenkins.cloud.common.SpringCloudNotification
import org.springframework.jenkins.cloud.common.TapPublisher
import org.springframework.jenkins.common.job.Cron
import org.springframework.jenkins.common.job.JdkConfig
import org.springframework.jenkins.common.job.Maven
import org.springframework.jenkins.common.job.TestPublisher

/**
 * @author Marcin Grzejszczak
 */
class SpringCloudPipelinesBaseDeployBuildMaker implements JdkConfig, TestPublisher, Cron,
		SpringCloudJobs, Maven {
	private final DslFactory dsl
	final String organization
	final String project

	SpringCloudPipelinesBaseDeployBuildMaker(DslFactory dsl) {
		this.dsl = dsl
		this.organization = 'spring-cloud'
		this.project = "pipeline-base"
	}

	void deploy() {
		dsl.job("spring-cloud-${project}-${masterBranch()}-ci") {
			triggers {
				githubPush()
			}
			parameters {
				stringParam("tagName", "latest", 'Which tag to use')
			}
			jdk jdk8()
			scm {
				git {
					remote {
						url "https://github.com/${organization}/${project}"
						branch masterBranch()
					}
					extensions {
						wipeOutWorkspace()
						submoduleOptions {
							recursive()
						}
					}
				}
			}
			wrappers {
				timestamps()
				colorizeOutput()
				maskPasswords()
				credentialsBinding {
					usernamePassword(dockerhubUserNameEnvVar(),
							dockerhubPasswordEnvVar(),
							dockerhubCredentialId())
				}
				timeout {
					noActivity(300)
					failBuild()
					writeDescription('Build failed due to timeout after {0} minutes of inactivity')
				}
			}
			steps {
				shell("""#!/bin/bash
				docker build . -t springcloud/pipeline-base
				docker login -u \$${dockerhubUserNameEnvVar()} -p \$${dockerhubPasswordEnvVar()}
				docker push springcloud/pipeline-base:latest
				docker push springcloud/pipeline-base:\${tagName}
				""")
			}
			configure {
				SpringCloudNotification.cloudSlack(it as Node)
			}
			publishers {
				archiveJunit gradleJUnitResults()
			}
		}
	}

	private String buildWithDocs() {
		return """#!/bin/bash -x
					${setupGitCredentials()}
					${setOrigin()}
					${checkoutMaster()}
					${build()} || exit 1 
					${syncDocs()} || echo "Failed to sync docs"
					${cleanGitCredentials()}
					${dockerBuildAndPush()}
					"""
	}

	private String setOrigin() {
		return "git remote set-url --push origin `git config remote.origin.url | sed -e 's/^git:/https:/'`"
	}

	private String checkoutMaster() {
		return "git checkout master && git pull origin master"
	}

	private String build() {
		return "./gradlew clean build generateDocs"
	}

	private String syncDocs() {
		return """git commit -a -m "Sync docs" && git push origin ${masterBranch()}"""
	}

	private String buildNumber() {
		return '${BUILD_NUMBER}'
	}

	private String dockerBuildAndPush() {
		return """
			echo "Deploying image to DockerHub"
			docker login --username=\$${dockerhubUserNameEnvVar()} --password=\$${dockerhubPasswordEnvVar()}
			echo "Docker images"
			docker images
			echo "Performing Docker Build"
			docker build -t springcloud/spring-cloud-pipeline-jenkins ./jenkins
			echo "Docker images post build"
			docker images
			echo "Pushing LATEST image to DockerHub"
			docker push springcloud/spring-cloud-pipeline-jenkins:latest
			echo "Removing all local images"
			docker rmi -f springcloud/spring-cloud-pipeline-jenkins
		"""
	}
}
