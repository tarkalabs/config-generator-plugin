package com.tarkalabs;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class GeneratorPlugin implements Plugin<Project> {
  public void apply(Project project) {
    project.getExtensions().create("generatorConfiguration", Configuration.class);
    project.getTasks().create("generateConfig", GenerateConfigTask.class);
  }
}
