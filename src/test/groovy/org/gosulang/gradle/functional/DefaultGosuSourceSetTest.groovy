package org.gosulang.gradle.functional

import org.gosulang.gradle.tasks.DefaultGosuSourceSet
import org.gradle.api.internal.file.DefaultSourceDirectorySet
import org.gradle.api.internal.file.FileResolver

import static org.hamcrest.Matchers.*
import static spock.util.matcher.HamcrestSupport.expect

class DefaultGosuSourceSetTest extends AbstractGosuPluginSpecification {

    private DefaultGosuSourceSet sourceSet

    def 'verify the default values'() {
        when:
        sourceSet = new DefaultGosuSourceSet("<set-display-name>", [resolve: { it as File }] as FileResolver)
        
        then:
        sourceSet.gosu instanceof DefaultSourceDirectorySet
        expect sourceSet.gosu, emptyIterable()
        sourceSet.gosu.displayName == '<set-display-name> Gosu source'
        expect sourceSet.gosu.filter.includes,  equalTo(['**/*.gs', '**/*.gsx', '**/*.gst'] as Set)
        expect sourceSet.gosu.filter.excludes, empty()
    }
    
    def 'can configure Gosu source'() {
        given:
        sourceSet = new DefaultGosuSourceSet("<set-display-name>", [resolve: { it as File }] as FileResolver)

        when:
        sourceSet.gosu {
            srcDir 'src/somepathtogosu'
        }
        
        then:
        expect sourceSet.gosu.srcDirs, equalTo([new File('src/somepathtogosu').canonicalFile] as Set)
    }

}
