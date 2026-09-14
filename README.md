#  Mass++ 4

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

##  How to run

Once `mspp4-core` is installed and dependencies are resolved, the desktop
application can be launched directly from Maven via the `javafx-maven-plugin`.

```bash
# Compile changed sources and run the application (typical dev cycle)
$ mvn compile javafx:run
```

Tips for faster iteration:

- Skip dependency resolution once everything is cached:

  ```bash
  $ mvn -o compile javafx:run
  ```

- A full clean build is rarely needed during development; use it only when
  switching branches or after dependency changes:

  ```bash
  $ mvn clean package -DskipTests
  ```

- To re-run without recompilation (e.g. after editing only resources):

  ```bash
  $ mvn javafx:run
  ```

###  Optional: Docker for vendor file conversion

Opening Thermo `.raw` or Sciex `.wiff` files (via `File > Open > MS Data...`
or `File > Convert > mzML...`) requires **Docker Desktop** to be running.
The application launches the
`chambm/pwiz-skyline-i-agree-to-the-vendor-licenses` image to invoke
ProteoWizard `msconvert` for the conversion. Reading `.mzML` files does not
require Docker.

