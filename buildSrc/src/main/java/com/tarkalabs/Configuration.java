package com.tarkalabs;

public class Configuration {
  private String configurationFilePath;
  private String packageName;
  private String configurationClassName;

  public String getConfigurationFilePath() {
    return configurationFilePath;
  }

  public void setConfigurationFilePath(String configurationFilePath) {
    this.configurationFilePath = configurationFilePath;
  }

  public String getPackageName() {
    return packageName;
  }

  public void setPackageName(String packageName) {
    this.packageName = packageName;
  }

  public String getConfigurationClassName() {
    return configurationClassName;
  }

  public void setConfigurationClassName(String configurationClassName) {
    this.configurationClassName = configurationClassName;
  }

  @Override public String toString() {
    return "Configuration{" +
        "configurationFilePath='" + configurationFilePath + '\'' +
        ", packageName='" + packageName + '\'' +
        ", configurationClassName='" + configurationClassName + '\'' +
        '}';
  }
}
