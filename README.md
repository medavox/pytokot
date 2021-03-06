# pytokot

A semi-automatic<sup>[1](#semiauto)</sup> Python-to-Kotlin converter.

As-yet unfinished: still under heavy development.

See [these comments](https://github.com/medavox/pytokot/blob/f6a1d2097bb47e8cfc983552fd1c709e606ef57e/src/commonMain/kotlin/com/github/medavox/pytokot/Pytokot.kt#L53) for a list of planned-but-unimplemented features, as of 3 June 2020.

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

<a name="semiauto">1</a>: pytokot does as much as it can for you, vastly reducing the work necessary to manually convert a Python file Kotlin.

But there will always need to be some editing by hand; custom libraries cannot be predicted and matched to Kotlin equivalents.
