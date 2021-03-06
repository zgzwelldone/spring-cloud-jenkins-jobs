job('spring-cloud-meta-seed') {
    triggers {
        githubPush()
    }
    scm {
        git {
            remote {
                github('spring-cloud/spring-cloud-jenkins-jobs')
            }
            branch('master')
        }
    }
    steps {
        gradle("clean build")
        dsl {
            external('projects/*.groovy')
            removeAction('DISABLE')
            removeViewAction('DELETE')
            ignoreExisting(false)
            additionalClasspath([
                    'src/main/groovy', 'src/main/resources', 'build/lib/*.jar'
            ].join("\n"))
        }
    }
}
