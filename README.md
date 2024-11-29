This is the PalorderSMP Mod its open source and be free to contact me if you want me to add features but not intense one's By the way if your a skid are someone who wants the jar file for this mod well you need to trade so let gradle eat all your performance and after that it will poop it out ok enough jokes lets get to the real stuff
## How to build the project

## Step One
Clone this repo and look for gradlew.bat BUT replace the contents from gradle.properties from this:" # Sets default memory used for gradle commands. Can be overridden by user or command line properties.
# This is required to provide enough memory for the Minecraft decompilation process.
org.gradle.jvmargs=-Xmx14g -Xms14g -Xss2m
org.gradle.daemon=true
"
and replace it with this: "# Sets default memory used for gradle commands. Can be overridden by user or command line properties.
# This is required to provide enough memory for the Minecraft decompilation process.
org.gradle.jvmargs=-Xmx4g -Xms4g -Xss2m
org.gradle.daemon=flase"
