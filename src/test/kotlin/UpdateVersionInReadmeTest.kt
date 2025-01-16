/*
 * Copyright 2025 Kazimierz Pogoda / Xemantic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xemantic.gradle

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.io.TempDir
import io.mockk.mockk
import io.mockk.verify
import org.gradle.api.GradleException
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UpdateVersionInReadmeTest {

    @TempDir
    lateinit var testProjectDir: File

    private lateinit var project: Project
    private lateinit var task: UpdateVersionInReadme
    private lateinit var log: (String) -> Unit

    @BeforeTest
    fun setup() {
        project = ProjectBuilder.builder()
            .withProjectDir(testProjectDir)
            .withName("my-project")
            .build()
        project.group = "com.example"
        project.version = "1.0.1"
        log = mockk(relaxed = true)
        task = project.tasks.register(
            "updateVersionInReadme",
            UpdateVersionInReadme::class.java,
            log
        ).get()
    }

    @Test
    fun `should update version in README when match found`() {
        // given
        val readme = File(testProjectDir, "README.md")
        readme.writeText("""
            # My Project
            
            ```kotlin
            dependencies {
                implementation("com.example:my-project:1.0.0")
            }
            ```
        """.trimIndent())

        // when
        task.action()

        // then
        val updatedContent = readme.readText()
        assertEquals("""
            # My Project
            
            ```kotlin
            dependencies {
                implementation("com.example:my-project:1.0.1")
            }
            ```
        """.trimIndent(), updatedContent)

        verify {
            log(
                "Successfully updated version in README.md to 1.0.1"
            )
        }


    }

    @Test
    fun `should not update version in README when already correct`() {
        val readme = File(testProjectDir, "README.md")
        readme.writeText("""
            # My Project
            
            ```kotlin
            dependencies {
                implementation("com.example:my-project:1.0.1")
            }
            ```
        """.trimIndent())

        task.action()

        val updatedContent = readme.readText()
        assertEquals("""
            # My Project
            
            ```kotlin
            dependencies {
                implementation("com.example:my-project:1.0.1")
            }
            ```
        """.trimIndent(), updatedContent)

        verify {
            log(
                "No update needed. Version in README.md is already 1.0.1"
            )
        }
    }

    @Test
    fun `should log warning when no match found in README`() {
        val readme = File(testProjectDir, "README.md")
        readme.writeText("""
            # My Project
            
            This is a sample project.
        """.trimIndent())

        task.action()

        verify {
            log("No matching dependency reference found in README.md. " +
                "Expected format: com.example:my-project:x.y.z")
        }
    }

    @Test
    fun `should fail when README file not found`() {
        val exception = assertFailsWith<GradleException> {
            task.action()
        }
        assert(exception.message == "README.md file not found in the project root directory.")
    }

}