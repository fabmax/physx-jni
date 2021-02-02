# physx-jni
Java JNI bindings for Nvidia PhysX

This is the Java counterpart to the PhysX bindings for javascript/webassembly provided by
[physx-js-webidl](https://github.com/fabmax/physx-js-webidl).

## How to use
The library is available on jcenter, so you can easily add this to your build.gradle:
```
repositories {
    jcenter()
}

dependencies {
    // java bindings
    implementation("de.fabmax:physx-jni:0.3.0")
    
    // native libraries, you can add the one matching your system or both
    runtimeOnly("de.fabmax:physx-jni:0.3.0:native-win64")
    runtimeOnly("de.fabmax:physx-jni:0.3.0:native-linux64")
}
```

### Examples
You can take a look at [PhysxTest.java](physx-jni/src/test/java/de/fabmax/physxjni/PhysXTest.java) for a
hello world example of how to use the library.

To get a feeling of what can be done with this you can take a look at my [kool](https://github.com/fabmax/kool) demos:

> *__Note:__ These demos run directly in the browser and obviously don't use this library, but the webassembly version mentioned
> above. However, the two are functionally identical, so it shouldn't matter too much. The JNI version is much faster
> though.*

- [Vehicle](https://fabmax.github.io/kool/kool-js/?demo=phys-vehicle): Basic vehicle demo with a few obstacles.
- [Joints](https://fabmax.github.io/kool/kool-js/?demo=phys-joints): A chain running over two gears.
- [Collision](https://fabmax.github.io/kool/kool-js/?demo=physics): Various collision shapes.

### Documentation
Unfortunately, the generated bindings currently don't include any javadoc. However, the Java API
is very close to the original PhysX C++ API, so you can simply use the official
[PhysX API documentation](https://gameworksdocs.nvidia.com/PhysX/4.1/documentation/physxapi/files/index.html) and
[PhysX User's Guide](https://gameworksdocs.nvidia.com/PhysX/4.1/documentation/physxguide/Manual/Index.html).

## What's included in the bindings?
For now only the basic stuff + vehicle physics.

The detailed list of mapped functions is given by the `PhysXJs.idl` in
`PhysX/physx/source/physxwebbindings/src/` (in the PhysX submodule). The Java classes containing the
actual bindings are generated from that file during build.

After build (or after running the corresponding gradle task `generateJniBindings`) the generated Java
classes are located under `physx-jni/src/main/generated`.

### Supported platforms:
- Windows (64-bit)
- Linux (64-bit x86)

## What's next?
- Triangle mesh shape
- More joint types
- Callbacks from native to Java (e.g. collision callbacks)
- Character controllers
- Include API docs

## Building
You can build the bindings yourself:
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

The above code doesn't compile the native libraries but relies on the pre-built libraries located in
the platform subprojects. In order to build the native libs from source you need
python3, cmake and Visual Studio 2019 Community (on Windows) or clang (on Linux):
```
# Generate PhysX cmake project files
./gradlew generateNativeProject

# Build PhysX
./gradlew buildNativeProject
```