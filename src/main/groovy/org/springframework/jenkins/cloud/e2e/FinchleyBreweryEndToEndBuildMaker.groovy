package org.springframework.jenkins.cloud.e2e

import javaposse.jobdsl.dsl.DslFactory

import org.springframework.jenkins.cloud.common.AllCloudConstants

/**
 * @author Marcin Grzejszczak
 */
class FinchleyBreweryEndToEndBuildMaker extends EndToEndBuildMaker {
	private final String repoName = 'brewery'
	private static final String RELEASE_TRAIN_NAME = "finchley"

	FinchleyBreweryEndToEndBuildMaker(DslFactory dsl) {
		super(dsl, 'spring-cloud-samples')
	}

	void build() {
		buildWithSwitches(RELEASE_TRAIN_NAME, defaultSwitches())
	}

	private void buildWithSwitches(String prefix, String defaultSwitches) {
		super.build("$prefix-zookeeper", repoName, "runAcceptanceTests.sh -t ZOOKEEPER $defaultSwitches", everyThreeHours())
		super.build("$prefix-sleuth", repoName, "runAcceptanceTests.sh -t SLEUTH $defaultSwitches", everyThreeHours())
		// TODO: Wait for Brewery to be fixed to use Stream server in version 1.5.x
		//super.build("$prefix-sleuth-stream", repoName, "runAcceptanceTests.sh -t SLEUTH_STREAM $defaultSwitches", everyThreeHours())
		//super.build("$prefix-sleuth-stream-kafka", repoName, "runAcceptanceTests.sh -t SLEUTH_STREAM -k $defaultSwitches", everyThreeHours())
		super.build("$prefix-eureka", repoName, "runAcceptanceTests.sh -t EUREKA $defaultSwitches", everyThreeHours())
		super.build("$prefix-consul", repoName, "runAcceptanceTests.sh -t CONSUL $defaultSwitches", everyThreeHours())
	}


	private String defaultSwitches() {
		String releaseTrain = RELEASE_TRAIN_NAME.capitalize()
		return "--killattheend -v ${releaseTrain}.BUILD-SNAPSHOT -b 2.0.0.M7 -r"
	}
}
