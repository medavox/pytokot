# pytokot

A semi-automatic[^1] Python-to-Kotlin converter.

* [online](https://kotlinguistics.github.io/pytokot)
* commandline app
* local webpage


<details>
  <summary><b>As a local webpage</b></summary>

To build it:

```shell script
./gradlew jsBrowserWebpack
./update-site.sh # or manually copy the files specified in that script
```

then open `./docs/index.html` in your browser.

</details>

<details>
  <summary> <b>As a Java desktop app</b></summary>

to build it:

```shell script
./gradlew shadowJar
```

to run it:

```shell script
java -jar build/libs/IPA-transcribers-0.3-all.jar
```

</details>

<details>
  <summary><b>As a library in a Gradle/Maven project</b></summary>


First, add the jitpack repository to your repositories if you haven't already:

`gradle`
``` gradle
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

`maven`
``` xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

Then add this library to your project:

`gradle`
``` gradle
dependencies {
    implementation 'com.github.medavox:IPA-Transcribers:v0.3'
}
```

`maven`
``` xml
<dependency>
    <groupId>com.github.medavox</groupId>
    <artifactId>IPA-Transcribers</artifactId>
    <version>v0.3</version>
</dependency>
```
</details>

[^1]: pytokot does as much as it for you, vastly reducing the work necessary to manually convert a .py file.
But there will always need to be some editing by hand; custom libraries cannot be predicted and matched to Kotlin equivalents.
