# Facebook Data Image Exif Tool

[![Release](https://img.shields.io/github/release/addshore/facebook-data-image-exif.svg?style=flat-square)](https://github.com/addshore/facebook-data-image-exif/releases/latest)
![GitHub Workflow Status](https://img.shields.io/github/workflow/status/addshore/facebook-data-image-exif/Java%20CI%20with%20Maven)
[![Software License](https://img.shields.io/badge/license-MIT-brightgreen.svg?style=flat-square)](LICENSE.md)


A simple tool, written in Java, to add EXIF data back to images downloaded in a Facebook data export.

Instructions can be found [here](https://addshore.com/2020/04/add-exif-data-back-to-facebook-images-0-10/)

Downloads can be found on [the releases page!](https://github.com/addshore/facebook-data-image-exif/releases)

![](https://i.imgur.com/1pKZNPC.png)

## Development

You need [JDK 11](https://openjdk.java.net/projects/jdk/11/).

```sh
sudo apt-get install openjdk-11-jdk
```

You can build a JAR using Maven:

```sh
JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64/ mvn clean package
```

### Github Actions

Github actions are configured on this repository and will build JARs for you.

You will find these JARs in the build output of Pull Requests.
## Further Reading

- This tool (in a PHP form) was [originally created in 2016](https://addshore.com/2016/09/add-exif-data-back-to-facebook-images/).
- It was [converted to Java in 2018](https://addshore.com/2019/02/add-exif-data-back-to-facebook-images-0-1/)
- And then most recently updated [in 2020](https://addshore.com/2020/04/add-exif-data-back-to-facebook-images-0-10/)