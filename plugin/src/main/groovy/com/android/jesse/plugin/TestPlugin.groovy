package com.android.jesse.plugin

import com.android.jesse.PluginConfig
import org.gradle.api.Plugin
import org.gradle.api.Project

class TestPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        def config = project.extensions.create('config', PluginConfig)
        project.task('testTask') {
            doLast {
                println('hello testTask ' + config.debug)
            }
        }
    }
}