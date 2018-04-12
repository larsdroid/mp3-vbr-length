MP3 VBR Length
==
*MP3 VBR Length* is a Java library that can be used to
calculate the length, in seconds, of an MP3 file stored
with a **variable bit-rate**.

The code based in part on [MP3Info version 0.8.5a by Cedric
Tefft](http://ibiblio.org/mp3info/).

[ ![Download](https://api.bintray.com/packages/larsdroid/mp3-vbr-length/mp3-vbr-length/images/download.svg) ](https://bintray.com/larsdroid/mp3-vbr-length/mp3-vbr-length/_latestVersion)

Setup
--
Add the repository to your `build.gradle`:
```groovy
repositories {
    maven {
        url "https://dl.bintray.com/larsdroid/mp3-vbr-length"
    }
}
```
Add the depencency to your `build.gradle`:
```groovy
compile 'org.willemsens:mp3-vbr-length:1.0'
```

Usage
--
```java
try {
    // Using NIO:
    final Path songPath = Paths.get("song.mp3");
    Mp3Info songInfo = Mp3Info.of(songPath);
    int songLengthSeconds = songInfo.getSeconds();
    
    // Using Java classic IO:
    final File songFile = new File("song.mp3");
    songInfo = Mp3Info.of(songFile);
    songLengthSeconds = songInfo.getSeconds();
} catch (IOException e) {
    // Handle exception
}
```

Please note that calling the `Mp3Info::of` method is a lengthy
operation since the entire file is scanned.

Author
--
Lars Willemsens ([https://github.com/larsdroid](https://github.com/larsdroid))

Original authors of MP3Info:  
[http://ibiblio.org/mp3info/](http://ibiblio.org/mp3info/)

License
--
*MP3 VBR Length* is licensed under the [GNU General Public License, version 2](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html).
