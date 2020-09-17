package com.android.jesse.plugin

import com.android.build.gradle.AppExtension
import com.android.jesse.Log
import com.android.jesse.extension.ReplaceExtension
import com.android.jesse.transforms.ReplaceTransform
import org.gradle.api.Plugin
import org.gradle.api.Project

class ReplacePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        Log.i("zyf", "-----ReplacePlugin apply-----")
        ReplaceExtension replaceExtension = project.extensions.create('replace', ReplaceExtension)

        //注册
        AppExtension appExtension = project.extensions.findByType(AppExtension.class)
        appExtension.registerTransform(new ReplaceTransform(replaceExtension))
    }
}