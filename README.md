#  Mass++ ver. 4 desktop

##  How to develop

This repository requires Maven to build.

First of all, please prepare your GitHub access token.
Go to <https://github.com/settings/tokens> and click `Generate new token` -> `Generate new token (classic)`.
This token should have at least the `read:packages` scope assigned.
(In my case, granting the `write:packages` scope provided sufficient permissions.)

Next, create a Maven settings file (`~/.m2/settings.xml`) in your local environment as follows:

```bash
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <servers>
    <server>
      <id>github</id>
      <username>YOUR_GITHUB_ACCOUNT_NAME</username>
      <password>YOUR_ACCESS_TOKEN</password>
    </server>
  </servers>
</settings>
```

You are now ready to build.
You can build the source code as follows.

```bash
# Install `mspp4-core` first
$ cd mspp4-core
$ mvn install

$ cd ../

$ cd mspp4-desktop
$ mvn package
```

