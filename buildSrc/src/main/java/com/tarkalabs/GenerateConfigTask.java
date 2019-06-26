package com.tarkalabs;

import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.Modifier;
import org.gradle.api.DefaultTask;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.tasks.TaskAction;

public class GenerateConfigTask extends DefaultTask {

  @TaskAction
  public void generateConfig() throws IOException {
    Configuration configuration = getProject().getExtensions().getByType(Configuration.class);
    if (configuration.getConfigurationFilePath() == null || configuration.getConfigurationFilePath()
        .trim()
        .isEmpty()) {
      throw new InvalidUserDataException(
          "configurationFilePath is not defined. Please set file path for configuration file in \ngeneratorConfiguration {\n\tconfigurationFilePath {$filePath} \n}");
    }

    if (configuration.getPackageName() == null || configuration.getPackageName()
        .trim()
        .isEmpty()) {
      throw new InvalidUserDataException(
          "packageName is not defined. Please provide package name in under which java configuration file need to be generated, in \ngeneratorConfiguration {\n\tpackageName {$packageName} \n}");
    }

    if (configuration.getConfigurationClassName() == null
        || configuration.getConfigurationClassName()
        .trim()
        .isEmpty()) {
      throw new InvalidUserDataException(
          "configurationInterfaceName is not defined. Please provide name of class under which you want to generate configuration, in \ngeneratorConfiguration {\n\tconfigurationClassName {$className} \n}");
    }

    System.out.println(configuration);
    File configurationJsonFile = getProject().file(configuration.getConfigurationFilePath());
    try {
      JsonElement jsonElement = new JsonParser().parse(new FileReader(configurationJsonFile));
      convertJsonToJava(configuration, jsonElement);
    } catch (JsonIOException e) {
      throw new InvalidUserDataException(
          "Could not read configuration file at " + configuration.getConfigurationFilePath(), e);
    } catch (JsonSyntaxException e) {
      throw new InvalidUserDataException(
          "configuration file does not contain valid json.", e);
    }
  }

  private void convertJsonToJava(Configuration configuration, JsonElement jsonElement)
      throws IOException {
    if (jsonElement.isJsonObject()) {
      TypeSpec interfaceSpec = convertJsonObjectToType(configuration.getConfigurationClassName(),
          jsonElement.getAsJsonObject());
      JavaFile javaFile =
          JavaFile.builder(configuration.getPackageName(), interfaceSpec)
              .build();
      javaFile.writeTo(this.getProject().getBuildDir());
    } else if (jsonElement.isJsonArray()) {
      throw new InvalidUserDataException(
          "Parent can not be Array. If you need help, Please raise issue on https://github.com/tarkalabs/config-generator-plugin/issues");
    } else {
      throw new InvalidUserDataException(
          "Plugin does not understand provided json. Please raise issue on https://github.com/tarkalabs/config-generator-plugin/issues");
    }
  }

  private FieldSpec convertJsonNullToField(String key, JsonElement value) {
    return null;
  }

  private FieldSpec convertJsonPrimitiveToField(String key, JsonElement value) {
    JsonPrimitive jsonPrimitive = value.getAsJsonPrimitive();
    if (jsonPrimitive.isBoolean()) {
      FieldSpec booleanFieldSpec =
          FieldSpec.builder(TypeName.BOOLEAN, key, Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
              .initializer("$L", value.getAsBoolean()).build();
      return booleanFieldSpec;
    } else if (jsonPrimitive.isNumber()) {
      BigDecimal bigDecimal = jsonPrimitive.getAsBigDecimal();
      FieldSpec numberFieldSpec;
      try {
        long l = bigDecimal.longValueExact();
        numberFieldSpec =
            FieldSpec.builder(TypeName.LONG, key, Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
                .initializer("$L", l).build();
      } catch (ArithmeticException e) {
        numberFieldSpec =
            FieldSpec.builder(TypeName.DOUBLE, key, Modifier.PUBLIC, Modifier.FINAL,
                Modifier.STATIC)
                .initializer("$L", bigDecimal.doubleValue()).build();
      }
      return numberFieldSpec;
    } else {
      FieldSpec stringFieldSpec =
          FieldSpec.builder(String.class, key, Modifier.PUBLIC, Modifier.FINAL,
              Modifier.STATIC)
              .initializer("$S", value.getAsString())
              .build();
      return stringFieldSpec;
    }
  }

  private FieldSpec convertJsonArrayToField(String key, JsonElement value) {
    return null;
  }

  private TypeSpec convertJsonObjectToType(String key, JsonElement value) {
    TypeSpec.Builder interfaceSpecBuilder =
        TypeSpec.interfaceBuilder(key)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC);
    JsonObject valueJson = value.getAsJsonObject();
    Set<Map.Entry<String, JsonElement>> entries = valueJson.entrySet();
    for (Map.Entry<String, JsonElement> entry : entries) {
      JsonElement valueElement = entry.getValue();
      if (valueElement.isJsonObject()) {
        interfaceSpecBuilder.addType(convertJsonObjectToType(entry.getKey(), valueElement));
      } else if (valueElement.isJsonArray()) {
        interfaceSpecBuilder.addField(convertJsonArrayToField(entry.getKey(), valueElement));
      } else if (valueElement.isJsonPrimitive()) {
        interfaceSpecBuilder.addField(convertJsonPrimitiveToField(entry.getKey(), valueElement));
      } else if (valueElement.isJsonNull()) {
        interfaceSpecBuilder.addField(convertJsonNullToField(entry.getKey(), valueElement));
      }
    }
    return interfaceSpecBuilder.build();
  }
}
