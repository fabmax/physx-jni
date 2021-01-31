# physx-jni
Java JNI bindings for Nvidia PhysX

This is the Java counterpart to the PhysX bindings for javascript/webassembly provided by
[physx-js-webidl](https://github.com/fabmax/physx-js-webidl).

## What's included in the bindings?
Only the basic stuff + vehicle physics.

## Where's the code?
The Java classes containing the actual bindings are generated during build with
[webidl-util](https://github.com/fabmax/webidl-util), a small piece of software which takes a single
WebIDL interface definition file (the same one as used by `physx-js-webidl`) as input and generates
all Java-related binding code from that.

After build (or after running the corresponding gradle task `generateJniBindings`) the generated Java
classes are located under `physx-jni/src/main/generated`.

## Supported platforms
Only windows for now, linux will follow shortly.

## How to use
TODO... For a (very) basic example you can take a look at
[PhysxTest.java](physx-jni/src/test/java/de/fabmax/physxjni/PhysXTest.java)

## Building
Until this project is published you can build it yourself:
```
# Clone this repo
git clone https://github.com/fabmax/physx-jni.git

# Enter that directory
cd physx-jni

# Download submodule containing the PhysX code
git submodule update --init

# Generate Java/JNI code and build library
./gradlew build
```

The above code doesn't compile the native libraries but relies on the pre-built .dll files located in
`physx-jni-native-win64/src/main/resources/win64`. In order to build the native libs from source you need
python3, cmake and Visual Studio 2019 (Community):
```
# Generate PhysX cmake project files
./gradlew generateNativeProject

# Build PhysX
./gradlew buildNativeProject
```