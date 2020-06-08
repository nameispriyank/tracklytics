package tracklytics.weaving.plugin

import org.aspectj.bridge.MessageHandler
import org.aspectj.tools.ajc.Main
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile

class TracklyticsPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        println("----------------------------------------------")
        println("--------------Tracklytics Weave---------------")

        project.dependencies {
            println("-----------add dependencies------------------------")

            implementation 'org.aspectj:aspectjrt:1.8.10'
            implementation 'com.orhanobut.tracklytics:tracklytics-runtime:2.2.1'
            compileOnly "org.aspectj:aspectjrt:1.8.10"
        }

        if (!project.hasProperty("android")) {
            throw RuntimeException("\'apply plugin: \'com.orhanobut.tracklytics\'\' should be added after " +
                    "\'apply plugin: \'com.android.application' or \'apply plugin: \'com.android.library\'\'")
        }

        if (project.android.hasProperty("libraryVariants")) {
            /*if the plugin is added to android library module*/
            println("-----------libraryVariants------------------------")
            project.android.libraryVariants.all { variant ->
                JavaCompile javaCompile
                if (variant.hasProperty('javaCompileProvider')) {
                    // Android 3.3.0+
                    javaCompile = variant.javaCompileProvider.get()
                } else {
                    javaCompile = variant.javaCompile
                }

                javaCompile.doLast {
                    def dirName = variant.name
                    runWithArgs(javaCompile, project, dirName, "libraryVariants javaCompile")
                }
            }
        } else if (project.android.hasProperty("applicationVariants")) {
            /*if the plugin is added to android application module*/
            println("-----------applicationVariants------------------------")

            if (project.android.productFlavors.size() == 0) {
                /*if android application hasn't flavors*/
                println("-----------hasn't flavors------------------------")
                project.android.applicationVariants.all { variant ->
                    JavaCompile javaCompile
                    if (variant.hasProperty('javaCompileProvider')) {
                        // Android 3.3.0+
                        javaCompile = variant.javaCompileProvider.get()
                    } else {
                        javaCompile = variant.javaCompile
                    }

                    // Gets the variant name and capitalize the first character
                    def taskPartName = variant.name[0].toUpperCase() + variant.name[1..-1].toLowerCase()

                    // Weave the binary for the actual code
                    // CompileSources task is invoked after java and kotlin compilers and copy kotlin classes
                    // That's the moment we have the finalized byte code and we can weave the aspects
                    project.tasks.findByName('compile' + taskPartName + 'Sources')?.doLast {
                        def dirName = variant.name
                        runWithArgs(javaCompile, project, dirName, "applicationVariants " + 'compile' + taskPartName + 'Sources')
                    }

                    // Weave the binary for unit tests
                    // compile unit tests task is invoked after the byte code is finalized
                    // This is the time that we can weave the aspects onto byte code
                    project.tasks.findByName('compile' + taskPartName + 'UnitTestSources')?.doLast {
                        def dirName = variant.name
                        runWithArgs(javaCompile, project, dirName, "applicationVariants " + 'compile' + taskPartName + 'UnitTestSources')
                    }
                }
            } else {
                /*if android application has flavors*/
                println("-----------has flavors ${project.android.productFlavors.size()}------------------------")
                project.android.applicationVariants.all { variant ->
                    JavaCompile javaCompile
                    if (variant.hasProperty('javaCompileProvider')) {
                        // Android 3.3.0+
                        javaCompile = variant.javaCompileProvider.get()
                    } else {
                        javaCompile = variant.javaCompile
                    }

                    def variantName = variant.name
                    println("variantName: ${variantName}")

                    project.android.productFlavors.all { flavor ->
                        def flavorName = flavor.name
                        if (variantName.toLowerCase().contains(flavorName.toLowerCase())) {
                            println("flavor: ${flavorName}")
                            project.android.buildTypes.all { buildType ->
                                def buildTypeName = buildType.name
                                if (variantName.toLowerCase().contains(buildTypeName.toLowerCase())) {
                                    println("buildType: ${buildTypeName}")
                                    def taskPartName = flavorName[0].toUpperCase() + flavorName[1..-1].toLowerCase() + buildTypeName[0].toUpperCase() + buildTypeName[1..-1].toLowerCase()
                                    project.tasks.findByName('compile' + taskPartName + 'Sources')?.doLast {
                                        def dirName = flavorName + buildTypeName[0].toUpperCase() + buildTypeName[1..-1].toLowerCase()
                                        runWithArgs(javaCompile, project, dirName, "applicationVariants " + 'compile' + taskPartName + 'Sources')
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            throw RuntimeException("\'apply plugin: \'com.orhanobut.tracklytics\'\' should be added after " +
                    "\'apply plugin: \'com.android.application' or \'apply plugin: \'com.android.library\'\'")
        }

    }

    private static void runWithArgs(JavaCompile javaCompile, Project project, dirName, type) {
        def destinationDir = javaCompile.destinationDir.toString()
        def bootClassPath = project.android.bootClasspath.join(File.pathSeparator)
        def classPath = javaCompile.classpath.asPath
        def aspectPath = javaCompile.classpath.asFileTree.filter {
            it.canonicalPath.contains("tracklytics-runtime")
        }.asPath
        String[] javaArgs = [
                "-showWeaveInfo",
                "-1.7",
                "-inpath", destinationDir,
                "-aspectpath", aspectPath,
                "-d", destinationDir,
                "-classpath", classPath,
                "-bootclasspath", bootClassPath
        ]
        String[] kotlinArgs = [
                "-showWeaveInfo",
                "-1.8",
                "-inpath", project.buildDir.path + "/tmp/kotlin-classes/" + dirName,
                "-aspectpath", aspectPath,
                "-d", project.buildDir.path + "/tmp/kotlin-classes/" + dirName,
                "-classpath", classPath,
                "-bootclasspath", bootClassPath
        ]

        MessageHandler handler = new MessageHandler(true)
        new Main().run(javaArgs, handler)
        new Main().run(kotlinArgs, handler)

        println("----------------------------------------------")
        println("--------------Tracklytics Weave ($type)---------------")
        println("----------------------------------------------")
        println("dirName: $dirName")
        println("destinationDir: $destinationDir")
        println("classPath: $classPath")
        println "aspectpath: $aspectPath"
        println("bootClassPath: $bootClassPath")
        println("javaArgs: $javaArgs")
        println("kotlinArgs: $kotlinArgs")
        println("----------------------------------------------")
    }

}