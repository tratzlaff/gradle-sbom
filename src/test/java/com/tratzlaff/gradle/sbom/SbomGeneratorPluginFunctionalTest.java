package com.tratzlaff.gradle.sbom;

import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import java.io.File;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SbomGeneratorPluginFunctionalTest {

    @Test
    public void testSbomGeneration() {
        File projectDir = new File("src/test/resources/sampleProject");  // replace with the path to a sample Gradle project

        GradleRunner runner = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("generateSpdxSbom")
            .withPluginClasspath()
            .forwardOutput();

        var result = runner.build();

        assertEquals(SUCCESS, result.task(":generateSpdxSbom").getOutcome());

        //TODO: Additional assertions to check SPDX file, logs, or other outputs
    }
}
