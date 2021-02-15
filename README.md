# physx-jni
Java JNI bindings for Nvidia PhysX

This is the Java counterpart to the PhysX bindings for javascript/webassembly provided by
[physx-js-webidl](https://github.com/fabmax/physx-js-webidl).

## How to use
The library is available on maven central, so you can easily add this to your build.gradle:
```
dependencies {
    // java bindings
    implementation("de.fabmax:physx-jni:0.4.0")
    
    // native libraries, you can add the one matching your system or both
    runtimeOnly("de.fabmax:physx-jni:0.4.0:native-win64")
    runtimeOnly("de.fabmax:physx-jni:0.4.0:native-linux64")
}
```

### Examples
You can take a look at [HelloPhysX.java](physx-jni/src/test/java/de/fabmax/physxjni/HelloPhysX.java) for a
hello world example of how to use the library. There also are a few
[tests](https://github.com/fabmax/physx-jni/tree/main/physx-jni/src/test/java/de/fabmax/physxjni) with slightly
more advanced examples (custom simulation callbacks, triangle mesh collision, etc.).

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

### Things to consider when working with native objects
Whenever you create an instance of a wrapper class within this library, this also creates an object on the native
side. Native objects are not covered by the garbage collector, so, in order to avoid a memory leak, you have to
clean up these objects yourself when you are done with them.

Here is an example:
```java
// create an object of PxVec3, this also creates a native PxVec3
// object behind the scenes.
PxVec3 vector = new PxVec3(1f, 2f, 3f);

// do something with vector...

// destroy the object once you are done with it
vector.destroy();
```

This approach has two potential problems: First, as mentioned, if you forget to call destroy(), the memory on the
native heap is not released resulting in a memory leak. Second, creating new objects on the native heap comes with
a lot of overhead and is much slower than creating a new object on the Java side.

These issues aren't a big problem for long living objects, which you create on start-up and use until you exit
the program. However, for short-lived objects like, in many cases, `PxVec3` this can have a large impact. Therefore,
there is a second method to allocate these objects: Stack allocation. To use this, you will need some sort of
memory allocator like LWJGL's MemoryStack. With that one the above example could look like this:
```java
try (MemoryStack mem = MemoryStack.stackPush()) {
    // create an object of PxVec3. The native object is allocated in the memory
    // provided by MemoryStack
    PxVec3 vector = PxVec3.malloc(mem, MemoryStack::nmalloc, 1f, 2f, 3f);
    
    // do something with vector...
    // no explicit destroy needed, memory is released when we leave the scope
}
```
While the `PxVec3.malloc()` call looks a bit more complicated, this approach is much faster and comes without the
risk of leaking memory.

### Java Callbacks

At a few places it is possible to register callbacks. For now the only supported callbacks are `PxErrorCallback` and
`PxSimulationEventCallback`. In order to implement a callback, the corresponding Java callback class has to be
extended. The implementing class can then be passed into the corresponding PhysX API.

Here's an example how this might look:

```java
// implement callback
public class CustomErrorCallback extends JavaErrorCallback {
    @Override
    public void reportError(int code, String message, String file, int line) {
        System.out.println(code + ": " + message);
    }
}

// register / use callback
CustomErrorCallback errorCb = new CustomErrorCallback();
PxFoundation foundation = PxTopLevelFunctions.CreateFoundation(PX_PHYSICS_VERSION, new PxDefaultAllocator(), errorCb);
```

## What's included in the bindings?
For now only the basic stuff + vehicle physics.

The detailed list of mapped functions is given by the interface definition file 
[PhysXJs.idl](physx-jni/src/main/webidl/PhysXJs.idl). The Java classes containing the
actual bindings are generated from that file during build.

After build (or after running the corresponding gradle task `generateJniBindings`) the generated Java
classes are located under `physx-jni/src/main/generated`.

### Supported platforms:
- Windows (64-bit)
- Linux (64-bit x86)

## What's next?
- [x] Triangle mesh shape
- [x] Callbacks from native to Java (e.g. collision callbacks)
- [ ] More joint types
- [ ] Character controllers
- [ ] Include API docs

## Building
You can build the bindings yourself:
```
# Clone this repo
git clone https://github.com/fabmax/physx-jni.git

# Enter that directory
cd physx-jni

# Generate Java/JNI code and build library
./gradlew build
```

The above code doesn't compile the native libraries but relies on the pre-built libraries located in
the platform subprojects. In order to build the native libs from source you need to clone the submodule containing
the PhysX source code, and you need python3, cmake and Visual Studio 2019 Community (on Windows) or clang (on Linux):
```
# Download submodule containing the PhysX source code
git submodule update --init

# Generate PhysX cmake project files
./gradlew generateNativeProject

# Build PhysX
./gradlew buildNativeProject
```